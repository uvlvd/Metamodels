package org.xtext.lua.scoping;

import java.util.Collections;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.xtext.lua.linking.SyntheticExpNil;
import org.xtext.lua.lua.Field;
import org.xtext.lua.lua.Assignment;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.utils.LinkingAndScopingUtils;

import com.google.inject.Inject;

/**
 * {@link IScopeProvider} implementation responsible for resolving assignable-to-value references in {@link Assignment}s or {@link Field}s.
 * @author jsaenz
 *
 */
public class LuaAssignmentScopeProvider implements IScopeProvider {
	
	@Inject
    private IQualifiedNameConverter nameConverter;

	@Override
	public IScope getScope(final EObject context, final EReference reference) {
		return getScopeForAssignableToValue(context);
	}
	
    /**
     * Assignables and Fields reference their assigned value, this returns the respective scope.</br>
     * E.g.: a = {b = 1} returns 1 for context object b, and the right-hand side TableConstructor for context object a.
     * @param context the context object.
     * @return the scope, or null if the context object is not an assignable or Field.
     */
    private IScope getScopeForAssignableToValue(EObject context) {
        // fields (in TableConstructors) also reference their assigned value
        if (context instanceof Field field) {
        	return getScopeForField(field);
        }
    	// assignables (variables that get assigned a value in an Assignment) reference their assigned value
        if (LinkingAndScopingUtils.isAssignable(context)) {
        	return getScopeForAssignable(context);
        }
        
        return null;
    }
    
    
    //TODO: this does not seem like the right location for this method, since the field names are computed here
    //      but the tableAccess names are computed in LuaLinkingService...
    /**
     * Returns the Scope for Fields. Every field has a value, which is the candidate returned in the returned scope.
     * Since not all names for fields can be computed, some fields may get assigned a dummy name.
     */
    private IScope getScopeForField(Field field) {
    	var value = field.getValueExp(); 	
    	if (value == null) {
    		throw new RuntimeException("Could not determine value expression for field " + field);
    	}
    	
    	var name = field.getName();
    	if (name.equals(LinkingAndScopingUtils.DERIVED_DUMMY_NAME)) {
    		name = LinkingAndScopingUtils.tryGetNameForField(field, LinkingAndScopingUtils.LINKING_DUMMY_NAME);
    	}
    	
    	var fqn = nameConverter.toQualifiedName(name);
    	var assignedValueDescription = EObjectDescription.create(fqn, value);
    	return new SimpleScope(Collections.singletonList(assignedValueDescription));
    }

    /**
     * Returns the Scope for assignables. Assignables are Referenceables on the lhs of an Assignment,
     * and the returned Scope contains a single candidate: the corresponding value expression on the rhs of the Assignment. </br>
     * If there is no corresponding value expression (e.g. a, b = 1), a new ExpNil is created and set as reference (in the e.g. b). </br>
     * @param assignable
     * @return
     */
    private IScope getScopeForAssignable(EObject assignable) {
    	final var name = ((Referenceable) assignable).getName();
		if (name == null) { // name might be null, e.g. for TableAccess with unresolvable indexExpression
			// TODO: return placeholder object or implement trivial recovery
			// TODO: name should not be null anymore, since TableAccesses get a dummy name?
			return IScope.NULLSCOPE;
		}
		
    	final var fqn = nameConverter.toQualifiedName(name);
    	final var value = LinkingAndScopingUtils.findAssignedExp(assignable);
		if (value == null) {
			// create synthetic nil value if ExpList does not contains value for assignable
			var nilValue = new SyntheticExpNil();
			var nilValueDescription = EObjectDescription.create(fqn, nilValue);
			return new SimpleScope(Collections.singletonList(nilValueDescription));
		} else {
			// any assignable is also a Referenceable which has a name attribute and a
			// cross-reference with linkText == name

			// we create a description with the name of the assignable and the corresponding value expression on the rhs of the assignment
			// => the scope for the assignable contains only this description
			var assignedValueDescription = EObjectDescription.create(fqn, value);
			return new SimpleScope(Collections.singletonList(assignedValueDescription));
		}
    }
    
}
