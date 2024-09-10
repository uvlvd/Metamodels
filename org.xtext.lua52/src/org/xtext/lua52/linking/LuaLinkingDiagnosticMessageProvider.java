package org.xtext.lua52.linking;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.linking.ILinkingDiagnosticMessageProvider;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.linking.impl.LinkingDiagnosticMessageProvider;
import org.xtext.lua52.lua52.Assignment;
import org.xtext.lua52.lua52.ExpList;
import org.xtext.lua52.utils.LinkingAndScopingUtils;

public class LuaLinkingDiagnosticMessageProvider extends LinkingDiagnosticMessageProvider {

	/**
	 * Based on UnresolvedFeatureCallTypeAwareMessageProvider.getUnresolvedProxyMessage().
	 */
	@Override
	public DiagnosticMessage getUnresolvedProxyMessage(
			ILinkingDiagnosticMessageProvider.ILinkingDiagnosticContext context) {
		
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
		
		// any object that is part of an assignment's left-hand side cannot contain cross-references,
		// ignore those objects.
		if (LinkingAndScopingUtils.hasRef(contextObject) 
				&& LinkingAndScopingUtils.isOnLhsOfAssignment(contextObject)) {
			//return null;
		} else if (LinkingAndScopingUtils.hasRef(contextObject)) { // TODO: just for debug
			//System.out.println(contextObject);
			//System.out.println(linkText);
		}
		
		//return null; // TODO: for debug purposes, remove
		return super.getUnresolvedProxyMessage(context);
	}
	

}
