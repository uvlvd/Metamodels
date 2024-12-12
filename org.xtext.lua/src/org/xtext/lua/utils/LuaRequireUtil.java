package org.xtext.lua.utils;

import org.eclipse.emf.ecore.EObject;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.Var;
import org.xtext.lua.scoping.LuaImportUriResolver;

public class LuaRequireUtil {
	
	private LuaRequireUtil() { }
	
	/**
	 * Returns true if the given EObject is a Feature, the Var at the start of its feature path has
	 * the require name {@link LuaImportUriResolver#REQUIRE_FUNC_NAME}, and that Var is followed by a 
	 * FuncCall.
	 * @param context the object.
	 * @return true if the object's feature path starts with a require call: require(...). ... .context
	 */
	public static boolean isPartOfRequireFunctionCallFeaturePath(EObject context) {
		if (context instanceof Feature feature) {
        	var featurePathRootOpt = FeatureUtil.findFeaturePathPrefixAsVar(feature);
        	if (featurePathRootOpt.isPresent()) {
        		// TODO: log some warning?
        		return isRequireFunctionCall(featurePathRootOpt.get());
        	}
		}
		return false;
	}
	
	/**
	 * Returns true if the given Var has the require name {@link LuaImportUriResolver#REQUIRE_FUNC_NAME} and
	 * is followed by a FuncCall Feature: require(...).
	 * @param var
	 * @return
	 */
	public static boolean isRequireFunctionCall(Var var) {
		if (var.getName().equals(LuaImportUriResolver.REQUIRE_FUNC_NAME)) {
    		if (FeatureUtil.hasNextFeature(var)) {
    			var next = FeatureUtil.getNextFeature(var);
    			if (next instanceof FunctionCall funcCall) {
    				return true;
    			}
    		}
    	}
		return false;
	}

}
