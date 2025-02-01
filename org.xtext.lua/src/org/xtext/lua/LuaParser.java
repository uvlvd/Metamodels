package org.xtext.lua;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.serializer.ISerializer;
import org.xtext.lua.scoping.LuaGlobalScopeProvider;

import com.google.inject.Injector;

// TODO: doc, mention the difference between generate and parse, resolveReferences;
// mention serialize
public class LuaParser {
	private static final Logger LOGGER = Logger.getLogger(LuaParser.class);
	
	private Injector injector;
	
	public LuaParser() {
		injector = new LuaStandaloneSetup().createInjectorAndDoEMFRegistration();
	}
	
	public LuaCodeModel generate(Path directory) throws IOException {
		var luaCodeModel = parse(directory);
		resolveAll(luaCodeModel);
		return luaCodeModel;
	}
	
	public LuaCodeModel parse(Path directory) throws IOException {
		if (!Files.isDirectory(directory)) {
			throw new IllegalStateException("The path '" + directory.toString() + "' is not a directory.");
		}
		
		var resourceSet = injector.getInstance(LuaCodeModel.class);
		
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
	
	/**
	 * Resolves all references in the given <code>codeModel</code>.
	 * 
	 * <p>This operation mutates the <code>codeModel</code>, installing the derived state
	 * and resolving all proxy elements to the actual model elements or mocked model elements.</p>
	 * @param codeModel the codeModel.
	 */
	public void resolveAll(LuaCodeModel codeModel) {
		installDerivedState(codeModel);
		EcoreUtil.resolveAll(codeModel);
	}
	
	private void installDerivedState(LuaCodeModel codeModel) {
		for (final var r : codeModel.getResources()) {
			if (r instanceof DerivedStateAwareResource derivedStateAwareResource) {
				derivedStateAwareResource.installDerivedState(false);
			} else {
				LOGGER.warn("Could not install derived state for resource " + r + ".");
			}
		}
	}
	
	public Map<URI, ByteArrayOutputStream> serialize(final LuaCodeModel codeModel) {
		// note that we do not need to discard the derived state, since it is ignored by the
		// implementation of the LuaTransientValueService.
		var result = new HashMap<URI, ByteArrayOutputStream>();
		for (final var r : codeModel.getSerializableResources()) {
			var outputStream = new ByteArrayOutputStream();
			var options = new HashMap<>();
			try {
				r.save(outputStream, options);
			} catch (IOException e) {
				LOGGER.error("Could not save resource to output stream during Serialization!", e);
			}
			result.put(r.getURI(), outputStream);
		}
		return result;
	}
	
	/**
	 * Returns the serializer used by this {@link LuaParser}. Only use for debugging purposes,
	 * e.g. when some model element should be serialized to the original String to via
	 * {@link ISerializer#serialize(org.eclipse.emf.ecore.EObject)}.
	 */
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
		r.getErrors().forEach(d -> LOGGER.error("Lua parser error in file: '" + path + "': " + d.getMessage()));
		r.getWarnings().forEach(d -> LOGGER.warn("Lua parser warning in file: '" + path + "': " + d.getMessage()));
	}
}
