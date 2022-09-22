package org.xtext.lua.scoping;

import java.util.LinkedHashSet;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;
import org.eclipse.xtext.scoping.impl.ImportUriResolver;

public class LuaGlobalScopeProvider extends ImportUriGlobalScopeProvider {
	@Override
	public ImportUriResolver getImportUriResolver() {
		return super.getImportUriResolver();
	}

	@Override
	protected LinkedHashSet<URI> getImportedUris(final Resource resource) {
		return super.getImportedUris(resource);
	}
}
