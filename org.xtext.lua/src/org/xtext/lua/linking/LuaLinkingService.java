package org.xtext.lua.linking;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.diagnostics.IDiagnosticConsumer;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.linking.lazy.SyntheticLinkingSupport;
import org.eclipse.xtext.linking.lazy.LazyLinker;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.resource.CompilerPhases;
import org.xtext.lua.lua.ExpStringLiteral;
import org.xtext.lua.lua.LuaPackage.Literals;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.TableAccess;

import com.google.inject.Inject;

public class LuaLinkingService extends DefaultLinkingService {
	private static final Logger LOGGER = Logger.getLogger(LuaLinkingService.class);
	
	@Inject
	private SyntheticLinkingSupport linkingSupport;
	
	@Inject
	private CompilerPhases compilerPhases;
	
	
	@Override
	public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) throws IllegalNodeException {
		// breaks serialization
		/*
		if (LinkingAndScopingUtils.isAssignable(context)) {
			final var value = LinkingAndScopingUtils.findAssignedExp((Feature) context);
			
			if (value == null) {
				// TODO: create transient "nil" eObject
			} else {
				return Collections.singletonList(value);
			}
		}
		*/
		//System.out.println("linking service call");

		return super.getLinkedObjects(context, ref, node);
	}
}
