package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.scoping.impl.ImportUriResolver;
import org.xtext.lua.utils.LuaRequireUtil;

public class LuaImportUriResolver extends ImportUriResolver {
	
	//public static final char IMPORT_URI_SEPARATOR = '.';
	
	
    @Override
    public String apply(EObject from) {
    	var importUriOpt = LuaRequireUtil.getImportUri(from);
    	if (importUriOpt.isPresent()) {
    		var importUri = importUriOpt.get();
    		// Not sure anymore why this is appended to the import uri, but reference resolution
    		// breaks if we do not append it. (This is connected to the LuaResourceDescriptionStrategy,
    		// which compares the import uri to the file uri. Do not change one without the other)
    		if (!importUri.endsWith(".lua")) {
    			importUri += ".lua";
           }
    		return importUri;
    	}
        return null;
    }
//
//    public boolean isImportFunction(EObject obj) {
//    	return LuaRequireUtil.getImportUri(obj).isPresent();
//    }
    
}
