package org.xtext.lua;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.serializer.ISerializer;
import org.xtext.lua.scoping.LuaGlobalScopeProvider;

import com.google.inject.Injector;

public class LuaParser {
	
	private Injector injector;
	
	public LuaParser() {
		injector = new LuaStandaloneSetup().createInjectorAndDoEMFRegistration();
	}
	
	public ResourceSet parse(Path directory) throws IOException {
		if (!Files.isDirectory(directory)) {
			throw new IllegalStateException("The path '" + directory.toString() + "' is not a directory.");
		}
		
		var resourceSet = injector.getInstance(XtextResourceSet.class);
		
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
	
	public ISerializer getSerializer() {
		return injector.getInstance(ISerializer.class);
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
		for (var uri : LuaGlobalScopeProvider.getImplicitLibraryUris()) {
			uriMap.put(uri, uri);
		}
	}
	
	private void printErrorsAndWarnings(final Path path, final Resource r) {
		System.out.println("Errors or warnings for file: " + path);
		r.getErrors().forEach(d -> System.out.println(d));
		r.getWarnings().forEach(d -> System.out.println(d));
	}
}
