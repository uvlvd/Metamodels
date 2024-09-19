/*
 * generated by Xtext 2.34.0
 */
package org.xtext.lua.scoping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.linking.lazy.SyntheticLinkingSupport;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.Scopes;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.xtext.lua.Config;
import org.xtext.lua.lua.Assignment;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Exp;
import org.xtext.lua.lua.ExpList;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.LuaPackage.Literals;
import org.xtext.lua.lua.MemberAccess;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.Stat;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.lua.Var;
import org.xtext.lua.utils.LinkingAndScopingUtils;

import com.google.inject.Inject;
import com.google.inject.Scope;

/**
 * This class contains custom scoping description.
 * 
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#scoping
 * on how and when to use it.
 */
public class LuaScopeProvider extends AbstractLuaScopeProvider {
	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;
	
  	@Inject
    private IQualifiedNameConverter nameConverter;
  	
	@Inject 
	private LinkingHelper linkingHelper;
	
	@Inject
	private SyntheticLinkingSupport linkingSupport;
    
    @Override
    public IScope getScope(final EObject context, final EReference reference) {
        if (context == null) {
            // nothing todo without context
            return IScope.NULLSCOPE;
        }
        
        // TODO: write doc for the method instead of the below comment
        // handle scope for assignables, i.e. set their reference to the value they are assigned to
        if (LinkingAndScopingUtils.isAssignable(context)) {
        	return getScopeForAssignable(context);
        }
        
        if (Config.TABLE_ACCESS_REFERENCES) {
	        /**
	         * For now, this is just a proof-of-concept. With this (and setting a name and ref for TableAccess in the DerivedStateComputer), 
	         * TableAccess indexes could be resolved s.t. the TableAccess references a given table field.
	         */
	        if (LinkingAndScopingUtils.isTableAccessWithDummyName(context)) {
	        	var ta = (TableAccess) context;
	        	var name = LinkingAndScopingUtils.tryResolveExpressionToString(ta.getIndexExp());
	        	
	        	var taDummyFqn = qualifiedNameProvider.getFullyQualifiedName(ta);
	        	var candidatesName = taDummyFqn.toString().replace(LinkingAndScopingUtils.DUMMY_NAME, name);
	        	var candidates = getCandidatesFromAssignablesFor(ta, nameConverter.toQualifiedName(candidatesName));
	
	        	return new SimpleScope(candidates.stream()
	        									 .map(c -> EObjectDescription.create(LinkingAndScopingUtils.DUMMY_NAME, c))
	        									 .toList());
	    	}
        }
    	

        if (context instanceof Referencing referencing) {
        //if (context instanceof Referencing referencing && ((context instanceof Var) || (context instanceof MemberAccess))) {
        	final var contextFqn = qualifiedNameProvider.getFullyQualifiedName(context);

        	//TODO: it seems that the lazy linking happens before the DerivedStateComputer
        	// 	installs the derived state, thus the name is null.
        	//  need to change that somewhere...?
        	if (contextFqn == null) {
        		return IScope.NULLSCOPE;
        	}
        	
        	
        	var candidates = getCandidatesFromAssignablesFor(referencing, contextFqn);	
        	return Scopes.scopeFor(candidates);
        }
    

        var parentBlock = EcoreUtil2.getContainerOfType(context.eContainer(), Block.class);
        if (parentBlock == null) {
            // if we have no parent anymore we delegate to the global scope
        	//System.out.println("TODO: Implement global scope");
        	//System.out.println(context);
        	return super.getScope(context, reference); // TODO: just for debug, remove
            //return super.getGlobalScope(context.eResource(), reference);
        }
        
        //System.out.println("current context: " + context);
        //System.out.println("context parent: " + context.eContainer());
        var parentStatement = EcoreUtil2.getContainerOfType(context, Stat.class);
        //var blockScope = getScopeOfBlock(parentBlock, context, parentStatement, reference);
        //return blockScope;
        return IScope.NULLSCOPE;
        
        
        /*
        var scope = super.getScope(context, reference);
        var temp = new ArrayList<IEObjectDescription>();
        scope.getAllElements().forEach(temp::add);
        if (temp.isEmpty()) {
        	System.out.println("Scope empty for obj: " + context);
        }
        return super.getScope(context, reference); // TODO: just for debug, remove
        */
    }
    
    private List<Referenceable> getCandidatesFromAssignablesFor(EObject context, QualifiedName fqn) {
    	var rootElement = EcoreUtil2.getRootContainer(context);
    	var assignments = EcoreUtil2.getAllContentsOfType(rootElement, Assignment.class);
    	
    	return assignments.stream()
				 .map(assignment ->  EcoreUtil2.getAllContentsOfType(assignment, Referenceable.class))
				 .flatMap(List::stream)
				 .filter(obj -> LinkingAndScopingUtils.isAssignable(obj))
				 .filter(refble -> {
					 return fqn.equals(qualifiedNameProvider.getFullyQualifiedName(refble));
				 })
				 .toList();
    }
    
    private IScope getScopeForAssignable(EObject assignable) {
    	final var value = LinkingAndScopingUtils.findAssignedExp((Feature) assignable);
		if (value == null) {
			// TODO: create transient "nil" eObject?
			// For now, reference self when no assigned value is part of the ExpList (= rhs of Assignment)
			return Scopes.scopeFor(Collections.singletonList(assignable));
		} else {
			// any assignable is also a Referenceable which has a name attribute and a
			// cross-reference with linkText == name
			var name = ((Referenceable) assignable).getName();
			var fqn = nameConverter.toQualifiedName(name);
			// we create a description with the name of the assignable and the value expression
			// => the scope for the assignable contains only this description
			var assignedValueDescription = EObjectDescription.create(fqn, value);
			return new SimpleScope(Collections.singletonList(assignedValueDescription));
		}
    }
   
    
}
