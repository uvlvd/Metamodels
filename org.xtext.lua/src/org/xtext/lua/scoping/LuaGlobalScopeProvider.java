package org.xtext.lua.scoping;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;
import org.eclipse.xtext.scoping.impl.ImportUriResolver;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import lua_libraries.LuaLibraryFiles;

public class LuaGlobalScopeProvider extends ImportUriGlobalScopeProvider {
	
	@Inject
	ImportUriResolver uriResolver;
	
	public static final boolean isImplicitResource(final Resource resource) {
		if (resource == null) return false;
		
		final var resourceUri = resource.getURI(); 
		return getImplicitLibraryUris().stream()
				.map(uri -> uri.toFileString())
				.anyMatch(fileStr -> fileStr.equals(resourceUri.toFileString()));

	}
	
	public static List<URI> getImplicitLibraryUris() {
		return LuaLibraryFiles.getAbsolutePaths().stream()
				.map(pathStr ->  URI.createURI(pathStr))
				.toList();
	}
	
	@Override
	protected LinkedHashSet<URI> getImportedUris(Resource resource) {
		LinkedHashSet<URI> importedURIs = super.getImportedUris(resource);
		importedURIs.addAll(getImplicitLibraryUris());
		return importedURIs;
	}
	
	@Override
	protected IScope getScope(Resource resource, boolean ignoreCase, EClass type, Predicate<IEObjectDescription> filter) {
		
		final LinkedHashSet<URI> uniqueImportURIs = getImportedUris(resource);
		IResourceDescriptions descriptions = getResourceDescriptions(resource, uniqueImportURIs);
		List<URI> urisAsList = Lists.newArrayList(uniqueImportURIs);
		Collections.reverse(urisAsList);
		IScope scope = IScope.NULLSCOPE;
		for (URI uri : urisAsList) {
			scope = createLazyResourceScope(scope, uri, descriptions, type, filter, ignoreCase);
		}
		return scope;

	}
	
	// TODO: maybe extract to LuaResourceDescriptionStrategy
	public static Predicate<IEObjectDescription> returnedExpAtIndexFilter(int index) {
		return LuaResourceDescriptionStrategy.isReturnedExpAtIndex(index);
	}
	// TODO: rename this, corresponding method in LuaResourceDescriptionStrategy
	//   and corresponding static strings in LuaResourceDescriptionStrategy
	public static Predicate<IEObjectDescription> returnedExpAtIndexFilter(int index, String uriString) {
		return LuaResourceDescriptionStrategy.isReturnedExpAtIndex(index, uriString);
	}

}
