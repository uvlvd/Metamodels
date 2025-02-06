package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.scoping.impl.ImportUriResolver;
import org.xtext.lua.utils.LuaRequireUtil;

public class LuaImportUriResolver extends ImportUriResolver {
		
    @Override
    public String apply(EObject from) {
    	var importUriOpt = LuaRequireUtil.getImportUri(from);
    	if (importUriOpt.isPresent()) {
    		var importUri = importUriOpt.get();
    		if (!importUri.endsWith(".lua")) {
    			importUri += ".lua";
            }
    		return importUri;
    	}
        return null;
    }

    
}
