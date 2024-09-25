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
		/*
		//System.out.println("LuaLinkingDiagnosticMessageProvider");
		String linkText = null;
		try {
			linkText = context.getLinkText();
		} catch (IllegalNodeException e) {
			linkText = e.getNode().getText();
		}
		if (linkText == null) {
			return null;
		}
		EObject contextObject = context.getContext();
		
		// ignore vars that are part of the lhs of an assignment and do not have a suffix (i.e. member call, table access, etc.)
		var parentContext = context.getContext().eContainer();
		//System.out.println(context.getContext());
		if (context.getContext() instanceof Var var && parentContext instanceof Assignment) {
			//System.out.println("ignored: " + var);
			//return super.getUnresolvedProxyMessage(context);
			//return null;
		}
		
		
		//return null; // TODO: for debug purposes, remove
		var msg = super.getUnresolvedProxyMessage(context);
		if (msg != null) {
			//System.out.println("foo: " + context.getContext());
		}
		*/
		
		//return null;
		return super.getUnresolvedProxyMessage(context);
	}
	

}
