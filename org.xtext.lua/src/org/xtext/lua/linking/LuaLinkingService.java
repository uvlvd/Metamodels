package org.xtext.lua.linking;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.nodemodel.INode;

public class LuaLinkingService extends DefaultLinkingService {
	private static final Logger LOGGER = Logger.getLogger(LuaLinkingService.class);
		
	@Override
	public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) throws IllegalNodeException {		
		return super.getLinkedObjects(context, ref, node);
	}
}
