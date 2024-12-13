package org.xtext.lua.scoping;

import java.util.Collections;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.xtext.lua.lua.Field;
import org.xtext.lua.utils.FieldUtil;
import org.xtext.lua.utils.LuaConstants;

import com.google.inject.Inject;

/**
 *  {@link IScopeProvider} implementation responsible for resolving assignable-to-value references in {@link Field}s.
 * @author jsaenz
 *
 */
public class LuaFieldScopeProvider implements IScopeProvider {
	
	@Inject
    private IQualifiedNameConverter nameConverter;
	
	@Override
	public IScope getScope(EObject context, EReference reference) {
		// fields (in TableConstructors) reference their assigned value
        if (context instanceof Field field) {
        	return getScopeForField(field);
        }
        throw new RuntimeException("FieldScopeProvider called with non-field object " + context);
	}
	
    
    //TODO: Since Fields contain the value in their valueExp, they can probably be non-referencing but Referenceable.
    //      This change would need to be implemented for all functions that return values for Referencing objects, since
    //      the "getRef" function of the field would not exist anymore, breaking the reference chain.
    //      E.g.: a.getRef().getRef()....getRef() would need to end with getValueExp() for Fields when attempting to find
    //      a's referenced value.
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
    	if (name.equals(LuaConstants.DERIVED_DUMMY_NAME)) {
    		name = FieldUtil.tryGetNameForField(field, LuaConstants.LINKING_DUMMY_NAME);
    	}
    	
    	var fqn = nameConverter.toQualifiedName(name);
    	var assignedValueDescription = EObjectDescription.create(fqn, value);
    	return new SimpleScope(Collections.singletonList(assignedValueDescription));
    }

}
