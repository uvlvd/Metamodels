/*
 * generated by Xtext 2.28.0
 */
package org.xtext.lua.scoping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.ISelectable;
import org.eclipse.xtext.resource.impl.AliasedEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.MultimapBasedSelectable;
import org.eclipse.xtext.scoping.impl.SimpleLocalScopeProvider;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.xtext.lua.LuaUtil;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.BlockWrapperWithArgs;
import org.xtext.lua.lua.Expression_Function;
import org.xtext.lua.lua.Expression_TableConstructor;
import org.xtext.lua.lua.Expression_VariableName;
import org.xtext.lua.lua.Field_AddEntryToTable;
import org.xtext.lua.lua.Refble;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Statement;
import org.xtext.lua.lua.Statement_Assignment;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * This class contains custom scoping description.
 * 
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#scoping on how and when
 * to use it.
 */
public class LuaScopeProvider extends SimpleLocalScopeProvider {
    private static final Logger LOGGER = Logger.getLogger(LuaScopeProvider.class.getPackageName());

    @Inject
    private IQualifiedNameConverter nameConverter;

    private List<Refble> getRefblesInAssignment(Statement_Assignment assignment) {
        var refbles = new ArrayList<Refble>();

        for (var dest : assignment.getDests()) {
            if (dest instanceof Referenceable)
                refbles.add((Referenceable) dest);
        }

        for (var expr : assignment.getValues()) {
            if (expr instanceof Expression_TableConstructor) {
                // if we assign a table we also pull in its fields
                var tableFields = EcoreUtil2.getAllContentsOfType(expr, Field_AddEntryToTable.class);
                refbles.addAll(tableFields);
            }
        }
        return refbles;
    }

//     TODO is this complete?
    private List<Refble> getRefblesInBlock(Block block, Statement filterStatement) {
        List<Refble> refbles = new ArrayList<Refble>();

        if (block.eContainer() instanceof BlockWrapperWithArgs) {
            // add arguments to refbles
            var argsOfBlock = ((BlockWrapperWithArgs) block.eContainer()).getArguments();
            argsOfBlock.forEach(arg -> refbles.add(arg));
        }
        for (var statement : block.getStatements()) {
            if (statement.equals(filterStatement)) {
                continue;
            } else if (statement instanceof Statement_Assignment) {
                refbles.addAll(getRefblesInAssignment((Statement_Assignment) statement));
            } else if (statement instanceof Refble) {
                refbles.add((Refble) statement);
            }
        }
        return refbles;
    }

    private IScope getScopeOfBlock(Block block, Statement filterStatement, EReference reference) {
        var parentScope = getScope(block, reference);

        var refblesInBlock = getRefblesInBlock(block, filterStatement);

        List<IEObjectDescription> descriptions = refblesInBlock.stream()
            .map(refble -> describeRefble(refble))
            .flatMap(List::stream)
            .collect(Collectors.toList());

        var thisScope = new SimpleScope(parentScope, descriptions, isIgnoreCase(reference));
        return thisScope;
    }

    @Override
    public IScope getScope(final EObject context, final EReference reference) {
        if (context == null) {
            // nothing todo without context
            return IScope.NULLSCOPE;
        }
        

        var parentBlock = EcoreUtil2.getContainerOfType(context.eContainer(), Block.class);
        if (parentBlock == null) {
            // if we have no parent anymore we delegate to the global scope
            return super.getGlobalScope(context.eResource(), reference);
        }
        
        var parentStatement = EcoreUtil2.getContainerOfType(context, Statement.class);
        var blockScope = getScopeOfBlock(parentBlock, parentStatement, reference);
        return blockScope;
    }

    private List<IEObjectDescription> describeRefble(Refble refble) {
        var descriptions = new ArrayList<IEObjectDescription>();
        var fqn = getNameProvider().apply(refble);
        if (fqn != null) {

            // create description
            var description = EObjectDescription.create(fqn, refble);
            descriptions.add(description);

            // Add alias for functions in tables because of the member syntactic sugar
            // E.g. Foo:bar(...)
            if (refble instanceof Field_AddEntryToTable
                    && ((Field_AddEntryToTable) refble).getValue() instanceof Expression_Function) {
                var aliasString = fqn.skipLast(1)
                    .toString() + ":"
                        + fqn.skipFirst(fqn.getSegmentCount() - 1)
                            .toString();
                var aliasQn = nameConverter.toQualifiedName(aliasString);
                descriptions.add(new AliasedEObjectDescription(aliasQn, description));
            }

            // Check if this is an aliasing assignment
//            var value = LuaUtil.resolveRefToValue(refble);
//            if (value instanceof Expression_VariableName) {
//                // extract the reference name from the node model
//                var node = NodeModelUtils.getNode(value);
//                var aliasTarget = NodeModelUtils.getTokenText(node);
//                LOGGER.debug(String.format("Aliasing assignment: %s -> %s", refble.getName(), aliasTarget));
//                aliases.put(aliasTarget, description);
//            }
        }
        return descriptions;
    }

    @Override
    protected ISelectable getAllDescriptions(Resource resource) {
//		System.out.println("CALL: LuaScopeProvider.getAllDescriptions(...)");
        Iterable<EObject> allContents = new Iterable<EObject>() {
            @Override
            public Iterator<EObject> iterator() {
                return EcoreUtil.getAllContents(resource, false);
            }
        };
        // aliased object are tracked here during a first pass and expanded in a second
        var aliases = new HashMap<String, IEObjectDescription>();

        /*
         * First Pass: Add aliases for member syntactic sugar (Foo.bar -> Foo:bar) Track aliasing
         * assignments in `aliases`
         */
        var identifyAliases = new Function<EObject, List<IEObjectDescription>>() {
            @Override
            public List<IEObjectDescription> apply(EObject eObject) {
                var descriptions = new ArrayList<IEObjectDescription>();
                if (eObject instanceof Refble) {
                    var refble = (Refble) eObject;
                    var fqn = getNameProvider().apply(refble);
                    if (fqn != null) {

                        // create description
                        var description = EObjectDescription.create(fqn, refble);
                        descriptions.add(description);

                        // Add alias for functions in tables because of the member syntactic sugar
                        // E.g. Foo:bar(...)
                        if (eObject instanceof Field_AddEntryToTable
                                && ((Field_AddEntryToTable) eObject).getValue() instanceof Expression_Function) {
                            var aliasString = fqn.skipLast(1)
                                .toString() + ":"
                                    + fqn.skipFirst(fqn.getSegmentCount() - 1)
                                        .toString();
                            var aliasQn = nameConverter.toQualifiedName(aliasString);
                            descriptions.add(new AliasedEObjectDescription(aliasQn, description));
                        }

                        // Check if this is an aliasing assignment
                        if (refble instanceof Referenceable) {
                            var value = LuaUtil.resolveRefToValue((Referenceable) refble);
                            if (value instanceof Expression_VariableName) {
                                // extract the reference name from the node model
                                var node = NodeModelUtils.getNode(value);
                                var aliasTarget = NodeModelUtils.getTokenText(node);
                                LOGGER.debug(
                                        String.format("Aliasing assignment: %s -> %s", refble.getName(), aliasTarget));
                                aliases.put(aliasTarget, description);
                            }
                        }
                    }
                }
                return descriptions;
            }
        };
        var handleAliases = new Function<IEObjectDescription, List<IEObjectDescription>>() {
            @Override
            public List<IEObjectDescription> apply(IEObjectDescription description) {
                var descriptions = new ArrayList<IEObjectDescription>();
                descriptions.add(description);
                // add an alias of the described object is part of an aliased assignment
                var originalName = description.getQualifiedName();
                var aliasingDescription = aliases.get(originalName.getFirstSegment());
                if (aliasingDescription != null) {

                    if (originalName.getSegmentCount() > 1) {
                        // Design decision: The alias points to the original declaration, not the
                        // aliasing declaration
                        // Given Foo = { bar = 42 }; foo = foo
                        // foo.bar points to the refble assigned to 42
//						var aliasTarget = aliasingDescription;
                        var aliasTarget = description;

                        /*
                         * We only join using a ':' if the name is e.g.: Foo:bar -> foo:bar
                         * 
                         * If there are more segments its like: Foo.bar:baz -> foo.bar:baz
                         */
                        var targetString = aliasTarget.getName()
                            .toString();
                        var isMemberAlias = targetString.contains(":") && !targetString.contains(".");

                        var aliasString = aliasingDescription.getName() + (isMemberAlias ? ":" : ".")
                                + originalName.skipFirst(1)
                                    .toString();

//						System.out.printf("Adding description for alias: %s -> %s\n", aliasString,
//								aliasTarget.getName());
                        var aliasQn = nameConverter.toQualifiedName(aliasString);

                        descriptions.add(new AliasedEObjectDescription(aliasQn, aliasTarget));
                    }
                }
                return descriptions;
            }
        };

        // Converting the iterator to a list to synchronize the two passes. This is
        // probably not
        // the correct way of doing this
        var firstPassResult = Lists
            .newArrayList(IterableExtensions.flatten(Iterables.transform(allContents, identifyAliases)));
        var secondPassResult = IterableExtensions.flatten(Iterables.transform(firstPassResult, handleAliases));
        var selectable = new MultimapBasedSelectable(secondPassResult);
        return selectable;
    }
}
