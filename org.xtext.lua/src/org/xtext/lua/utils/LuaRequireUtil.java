package org.xtext.lua.utils;

import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.xtext.lua.lua.ExpStringLiteral;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.LiteralStringArg;
import org.xtext.lua.lua.ParamArgs;
import org.xtext.lua.lua.Var;
import org.xtext.lua.scoping.LuaImportUriResolver;

public class LuaRequireUtil {
	public static final String REQUIRE_FUNC_NAME = "require";
	
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
		if (var.getName().equals(REQUIRE_FUNC_NAME)) {
    		if (FeatureUtil.hasNextFeature(var)) {
    			var next = FeatureUtil.getNextFeature(var);
    			if (next instanceof FunctionCall funcCall) {
    				return true;
    			}
    		}
    	}
		return false;
	}
	
	public static Optional<String> getImportUri(EObject obj) {
    	if (obj instanceof Var var && REQUIRE_FUNC_NAME.equals(var.getName())) {
    		// check if var is a function call
    		if (var.getSuffixExp() instanceof FunctionCall funcCall) {
    			
    			// return literal String argument
    			if (funcCall.getArgs() instanceof LiteralStringArg literalStringArg) {
    				var importUri = ExpUtil.removeQuotesFromString(literalStringArg.getStr());
    				return Optional.of(importUri);
    			}
    			// check if has paramargs
    			else if (funcCall.getArgs() instanceof ParamArgs paramArgs) {
    				// return optional containing String argument if first argument is a String literal
    				return paramArgs.getParams().getExps().stream()
		    					.findFirst()
		    					.map(arg -> {
		    						if (arg instanceof ExpStringLiteral stringLiteral) {
		    							return ExpUtil.removeQuotesFromString(stringLiteral.getValue());
		    						}
		    						return null;
		    					});
    			}
    			
    		}
    	}
    	// not an import object
    	return Optional.empty();
    }

}
