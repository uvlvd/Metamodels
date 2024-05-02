package org.xtext.lua;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;

public class LuaParser {
	public ResourceSet parse(Path directory) throws IOException {
		if (!Files.isDirectory(directory)) {
			throw new IllegalStateException("The path '" + directory.toString() + "' is not a directory.");
		}
		
		var resourceSet = new LuaStandaloneSetup().createInjectorAndDoEMFRegistration().getInstance(XtextResourceSet.class);
		resourceSet.getLoadOptions().put(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);
		
		try (var fileStream = Files.walk(directory)) {
			fileStream
				.filter((path) -> Files.isRegularFile(path) && path.toString().endsWith(".lua"))
				.forEach((path) -> {
					var uri = URI.createFileURI(path.toAbsolutePath().toString());
					var r = resourceSet.getResource(uri, true);
					r.getErrors().forEach(d -> System.out.println(d));
					r.getWarnings().forEach(d -> System.out.println(d));
				});
		}
		
        return resourceSet;
	}
}
