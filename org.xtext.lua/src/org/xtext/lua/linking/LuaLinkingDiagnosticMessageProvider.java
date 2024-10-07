package org.xtext.lua.linking;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.linking.ILinkingDiagnosticMessageProvider;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.linking.impl.LinkingDiagnosticMessageProvider;
import org.eclipse.xtext.linking.lazy.SyntheticLinkingSupport;
import org.eclipse.xtext.linking.lazy.LazyLinker;
import org.xtext.lua.lua.Assignment;
import org.xtext.lua.lua.Var;

public class LuaLinkingDiagnosticMessageProvider extends LinkingDiagnosticMessageProvider {

	/**
	 * Based on UnresolvedFeatureCallTypeAwareMessageProvider.getUnresolvedProxyMessage().
	 */
	@Override
	public DiagnosticMessage getUnresolvedProxyMessage(
			ILinkingDiagnosticMessageProvider.ILinkingDiagnosticContext context) {

		//return null;
		return super.getUnresolvedProxyMessage(context);
	}
	

}
