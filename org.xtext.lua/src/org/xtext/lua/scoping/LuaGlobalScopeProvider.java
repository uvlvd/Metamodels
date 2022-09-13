package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.DefaultGlobalScopeProvider;

import com.google.common.base.Predicate;

public class LuaGlobalScopeProvider extends DefaultGlobalScopeProvider {
	@Override
	protected IScope getScope(IScope parent, Resource context, boolean ignoreCase, EClass type,
			Predicate<IEObjectDescription> filter) {
		// TODO Auto-generated method stub
		return super.getScope(parent, context, ignoreCase, type, filter);
	}

	@Override
	protected IScope getScope(Resource context, boolean ignoreCase, EClass type,
			Predicate<IEObjectDescription> filter) {
		// TODO Auto-generated method stub
		return super.getScope(context, ignoreCase, type, filter);
	}
}

//public class LuaGlobalScopeProvider extends ImportUriGlobalScopeProvider {
//	// from: https://www.davidpace.de/library-bundles-for-your-xtext-dsl/
//	public static final URI HEADER_URI = URI.createURI("platform:/plugin/org.xtext.lua.lib/stdlib/base_functions.lua");
//
//	@Override
//	protected LinkedHashSet<URI> getImportedUris(Resource resource) {
//		LinkedHashSet<URI> importedURIs = super.getImportedUris(resource);
//		importedURIs.add(HEADER_URI);
//		return importedURIs;
//	}
//}
