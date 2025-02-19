package org.xtext.lua.component_extension;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.wrappers.LuaFunctionCall;
import org.xtext.lua.wrappers.LuaFunctionDeclaration;

public class ComponentUtil {
	
	private ComponentUtil() { }
	
    public static Component getComponent(final LuaFunctionCall functionCall) {
    	return getComponent(functionCall.getCallingFeature());
    }
    
    public static Component getComponent(final LuaFunctionDeclaration functionDeclaration) {
    	return getComponent(functionDeclaration.getRoot());
    }
    
    public static Component getComponent(final EObject eObj) {
    	return EcoreUtil2.getContainerOfType(eObj, Component.class);
    }
    
}
