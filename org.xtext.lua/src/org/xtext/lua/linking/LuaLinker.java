package org.xtext.lua.linking;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.diagnostics.IDiagnosticConsumer;
import org.eclipse.xtext.linking.lazy.LazyLinker;

public class LuaLinker extends LazyLinker {

	@Override
	protected void doLinkModel(final EObject model, IDiagnosticConsumer consumer) {

		super.doLinkModel(model, consumer);
	}
}
