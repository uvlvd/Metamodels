package org.xtext.lua;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.xtext.lua.scoping.LuaGlobalScopeProvider;

import com.google.common.base.Preconditions;

public class LuaParser {
	public ResourceSet parse(Path directory) throws IOException {
		if (!Files.isDirectory(directory)) {
			throw new IllegalStateException("The path '" + directory.toString() + "' is not a directory.");
		}
		
		var resourceSet = new LuaStandaloneSetup().createInjectorAndDoEMFRegistration().getInstance(XtextResourceSet.class);

		// parse lua packages and libraries
		registerAndParseImplicitImports(resourceSet);
		
		try (var fileStream = Files.walk(directory)) {
			fileStream
				.filter((path) -> Files.isRegularFile(path) && path.toString().endsWith(".lua"))
				.forEach((path) -> {
					var uri = URI.createFileURI(path.toAbsolutePath().toString());
					
					var r = resourceSet.getResource(uri, true);
					
					
					final var isErrorsOrWarnings = !(r.getErrors().isEmpty() && r.getWarnings().isEmpty());
					if (isErrorsOrWarnings) {
						printErrorsAndWarnings(path, r);
					}
				});
		}
		
        return resourceSet;
	}
	
	private static void registerAndParseImplicitImports(XtextResourceSet resourceSet) {
		registerURIMappingsForImplicitImports(resourceSet);
		var uriMap = resourceSet.getURIConverter().getURIMap();
		for (var uri : uriMap.values()) {
			resourceSet.getResource(uri, true);
		}
	}
	
	// see https://www.davidpace.de/library-bundles-for-your-xtext-dsl/
	// and LuaGlobalScopeProvider for implicit library imports
	private static void registerURIMappingsForImplicitImports(XtextResourceSet resourceSet) {
		final var uriConverter = resourceSet.getURIConverter();
		final var uriMap = uriConverter.getURIMap();
		for (var uri : LuaGlobalScopeProvider.LIBRARY_URIS) {
			registerPlatformToFileURIMapping(uri, uriMap);
		}
		
	}

	private static void registerPlatformToFileURIMapping(URI uri, Map<URI, URI> uriMap) {
		final URI fileURI = createFileURIForHeaderFile(uri);
		final File file = new File(fileURI.toFileString());
		Preconditions.checkArgument(file.exists());
		uriMap.put(uri, fileURI);
	}

	private static URI createFileURIForHeaderFile(URI uri) {
		return URI.createFileURI(deriveFilePathFromURI(uri));
	}

	private static String deriveFilePathFromURI(URI uri) {
		return "../" + uri.path().substring(7);
	}
	
	private void printErrorsAndWarnings(final Path path, final Resource r) {
		System.out.println("Errors or warnings for file: " + path);
		r.getErrors().forEach(d -> System.out.println(d));
		r.getWarnings().forEach(d -> System.out.println(d));
	}
}
