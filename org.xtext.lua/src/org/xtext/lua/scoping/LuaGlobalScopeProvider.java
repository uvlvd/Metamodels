package org.xtext.lua.scoping;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;
import org.eclipse.xtext.scoping.impl.ImportUriResolver;
import org.xtext.lua.lua.Var;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import lua_libraries.LuaLibraryFiles;

public class LuaGlobalScopeProvider extends ImportUriGlobalScopeProvider {
	
	private static final int COMPARE_LIBRARY_URI_SEGMENTS_COUNT = 2;
	
	private final Map<Resource, Collection<URI>> resourceToImportedUris = new HashMap<>();
	
	@Inject
	ImportUriResolver uriResolver;
	
	@Inject
	IResourceServiceProvider resourceServiceProvider;
	
	public static final boolean isImplicitResource(final Resource resource) {
		if (resource == null) return false;
		
		final var resourceUri = resource.getURI(); 
		final var resourceUriSegments = resourceUri.segmentsList();
		if (resourceUriSegments.size() < 2) {
			return false;
		}

		// we cannot check the whole path because it may differ in the CIPM pipeline, i.e. when we are running the second instance
		// for CIPM the resource path will not be a file path
		return getImplicitLibraryUris().stream()
				.map(uri -> uri.segmentsList())
				.anyMatch(librarySegments -> {
					
					if (librarySegments.size() < COMPARE_LIBRARY_URI_SEGMENTS_COUNT) {
						return false;
					}
					// check if last COMPARE_LIBRARY_URI_SEGMENTS_COUNT segments match 
					final var librarySegmentsTail = librarySegments.subList(librarySegments.size() - COMPARE_LIBRARY_URI_SEGMENTS_COUNT, librarySegments.size());
					final var resourceUriSegmentssTail = resourceUriSegments.subList(resourceUriSegments.size() - COMPARE_LIBRARY_URI_SEGMENTS_COUNT, resourceUriSegments.size());
					return librarySegmentsTail.equals(resourceUriSegmentssTail);

				});
	}
	
	public static List<URI> getImplicitLibraryUris() {
		return LuaLibraryFiles.getAbsolutePaths().stream()
				.map(pathStr ->  URI.createURI(pathStr))
				.toList();
	}
	
	private Collection<URI> getImportUrisFor(Resource resource) {
		if (resourceToImportedUris.get(resource) == null) {
			var importUris = EcoreUtil2.getAllContentsOfType(resource.getContents().get(0), Var.class).stream()
					.map(uriResolver::apply)
					.filter(Objects::nonNull)
					.toList();
			
			var importedUris = resource.getResourceSet().getResources().stream()
					.filter(res -> !LuaGlobalScopeProvider.isImplicitResource(res))
					.filter(res -> {
						final var resourceUriStr = res.getURI().toFileString();
						return importUris.stream()
								.anyMatch(importUri -> 
								LuaResourceDescriptionStrategy.importUriEqualsFileUri(importUri, resourceUriStr)
							);
					})
					.map(Resource::getURI)
					.collect(Collectors.toSet());
			
			importedUris.addAll(getImplicitLibraryUris());

			resourceToImportedUris.put(resource, importedUris);
		}
		return resourceToImportedUris.get(resource);
	}
	
	@Override
	protected LinkedHashSet<URI> getImportedUris(Resource resource) {
		return new LinkedHashSet<URI>(getImportUrisFor(resource));
	}
	
	@Override
	protected IScope getScope(Resource resource, boolean ignoreCase, EClass type, Predicate<IEObjectDescription> filter) {		

		final var uniqueImportURIs = getImportedUris(resource);
		IResourceDescriptions descriptions = getResourceDescriptions(resource, uniqueImportURIs);
		List<URI> urisAsList = Lists.newArrayList(uniqueImportURIs);
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
