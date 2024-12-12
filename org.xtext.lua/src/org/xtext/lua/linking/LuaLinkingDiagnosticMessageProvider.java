package org.xtext.lua.linking;

import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.linking.ILinkingDiagnosticMessageProvider;
import org.eclipse.xtext.linking.impl.LinkingDiagnosticMessageProvider;

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
