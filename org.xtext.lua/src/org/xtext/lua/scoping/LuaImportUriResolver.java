package org.xtext.lua.scoping;

import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.scoping.impl.ImportUriResolver;
import org.xtext.lua.lua.ExpStringLiteral;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.LiteralStringArg;
import org.xtext.lua.lua.ParamArgs;
import org.xtext.lua.lua.Var;
import org.xtext.lua.utils.LinkingAndScopingUtils;

public class LuaImportUriResolver extends ImportUriResolver {
	private static final String REQUIRE_FUNC_NAME = "require";
	
    @Override
    public String apply(EObject from) {
    	var importUriOpt = getImportUri(from);
    	if (importUriOpt.isPresent()) {
    		var importUri = importUriOpt.get();
    		
    		if (!importUri.endsWith(".lua")) {
    			importUri += ".lua";
            }
    		
    		System.out.println("ImportUri: " + importUri);
    		return importUri;
    	}
/*
        if (isImportFunction(from)) {
            var exp = (Expression_Import) from;
            var uri = exp.getImportURI();
            if (uri.length() > 1) {
                uri = (String) uri.subSequence(1, uri.length() - 1);
            }
            if (!uri.endsWith(".lua")) {
                uri += ".lua";
            }
            return uri;
        }*/
        return null;
    }

    public boolean isImportFunction(EObject obj) {
    	return getImportUri(obj).isPresent();
    }
    
    private Optional<String> getImportUri(EObject obj) {
    	if (obj instanceof Var var && REQUIRE_FUNC_NAME.equals(var.getName())) {
    		// check if var is a function call
    		if (var.getSuffixExp() instanceof FunctionCall funcCall) {
    			
    			// return literal String argument
    			if (funcCall.getArgs() instanceof LiteralStringArg literalStringArg) {
    				var importUri = LinkingAndScopingUtils.removeQuotesFromString(literalStringArg.getStr());
    				return Optional.of(importUri);
    			}
    			// check if has paramargs
    			else if (funcCall.getArgs() instanceof ParamArgs paramArgs) {
    				// return optional containing String argument if first argument is a String literal
    				return paramArgs.getParams().getExps().stream()
		    					.findFirst()
		    					.map(arg -> {
		    						if (arg instanceof ExpStringLiteral stringLiteral) {
		    							return LinkingAndScopingUtils.removeQuotesFromString(stringLiteral.getValue());
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
