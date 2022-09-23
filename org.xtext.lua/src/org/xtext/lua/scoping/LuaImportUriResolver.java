package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.scoping.impl.ImportUriResolver;
import org.xtext.lua.lua.Expression_Import;

public class LuaImportUriResolver extends ImportUriResolver {

	@Override
	public String apply(EObject from) {
		if (from instanceof Expression_Import) {
			var exp = (Expression_Import) from;
			var uri = exp.getImportURI();
			if (uri.length() > 1) {
				uri = (String) uri.subSequence(1, uri.length() - 1);
			}
			if (!uri.endsWith(".lua")) {
				uri += ".lua";
			}
			return uri;
		}
//		return super.apply(from);
		return null;
	}

}
