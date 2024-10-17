package org.xtext.lua.scoping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;
import org.eclipse.xtext.scoping.impl.ImportUriResolver;
import org.eclipse.xtext.util.IAcceptor;
import org.xtext.lua.lua.Var;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class LuaGlobalScopeProvider extends ImportUriGlobalScopeProvider {
	
	@Inject
	ImportUriResolver uriResolver;
	
	// see https://www.davidpace.de/library-bundles-for-your-xtext-dsl/
	// and LuaParser.java for implicit library imports
	public static final URI LIBRARY_URI_BASE = URI.createURI("platform:/plugin/org.xtext.lua.libraries/src/lua_libraries/");

	// TODO: could get file names dynamically from folder
	public static final List<URI> LIBRARY_URIS = Collections.unmodifiableList(
		Arrays.asList(
			createLibraryUriForFileStr("basic.lua"),
			createLibraryUriForFileStr("bit.lua"),
			createLibraryUriForFileStr("bit32.lua"),
			createLibraryUriForFileStr("builtin.lua"),
			createLibraryUriForFileStr("coroutine.lua"),
			createLibraryUriForFileStr("debug.lua"),
			createLibraryUriForFileStr("ffi.lua"),
			createLibraryUriForFileStr("io.lua"),
			createLibraryUriForFileStr("jit.lua"),
			createLibraryUriForFileStr("math.lua"),
			createLibraryUriForFileStr("os.lua"),
			createLibraryUriForFileStr("package.lua"),
			createLibraryUriForFileStr("string.lua"),
			createLibraryUriForFileStr("table.lua"),
			createLibraryUriForFileStr("utf8.lua")
		)
	);
	
	private static final URI createLibraryUriForFileStr(String fileStr) {
		return URI.createURI(LIBRARY_URI_BASE + fileStr);
	}
	
	@Override
	protected LinkedHashSet<URI> getImportedUris(Resource resource) {
		LinkedHashSet<URI> importedURIs = super.getImportedUris(resource);
		importedURIs.addAll(LIBRARY_URIS);
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
		//System.out.println("resource: " + resource);
		//System.out.println("type: " + type);
		//System.out.println("filter: " + filter);
		//System.out.println("uniqueImportURIs" + uniqueImportURIs);
		//System.out.println("descriptions" + descriptions);
		//System.out.println("urisAsList" + urisAsList);
		//System.out.println("scope " + scope);
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
