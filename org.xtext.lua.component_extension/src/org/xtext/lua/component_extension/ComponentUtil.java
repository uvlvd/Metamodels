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
    
//    /**
//	 * Returns true if this is a function call to an external function, i.e. the component
//	 * containing the function call differs from the component containing the called function.</br>
//	 * 
//	 * This can only be determined if this {@link LuaFunctionCall} is not mocked, and will
//	 * throw a {@link RuntimeException} otherwise.
//	 */
//	public boolean isExternal() {
//		if (isExternal == null) {
//			if (getCalledFunction() == null) {
//				throw new RuntimeException("Attempting to check if " + this + " is external, but calledFunction is null!");
//			}
//			// note: we are comparing optionals here
//			final var callingComponent = ComponentUtil.getComponent(this);
//			final var calledComponent = ComponentUtil.getComponent(getCalledFunction());
//			isExternal = !(callingComponent.equals(calledComponent));
//		}
//		
//		return isExternal;
//	}
//	
//	public boolean isInternal() {
//		return !isExternal();
//	}
    
}
