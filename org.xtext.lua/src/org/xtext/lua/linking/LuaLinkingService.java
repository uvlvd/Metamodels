package org.xtext.lua.linking;

import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.nodemodel.INode;
import org.xtext.lua.lua.FuncBody;
import org.xtext.lua.mocking.MockObjectCreator;
import org.xtext.lua.postprocessing.LuaResourcePostProcessor;
import org.xtext.lua.utils.ReferenceableUtil;

import com.google.inject.Inject;

public class LuaLinkingService extends DefaultLinkingService {
	
	@Inject
	private MockObjectCreator mockObjectCreator;
	
	@Inject
	private LuaResourcePostProcessor resourcePostProcessor;
	
	private boolean isTableAccessNamesResolved = false;
  	
	@Override
	public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) throws IllegalNodeException {
		
		// first, try to resolve all table access names and references to avoid cyclic reference resolution
		// (could probably also be handled in some other way in the scopeProvider implementation)
		// TODO: would probably be better is this could somehow be executed as a "first step" of the linking via an api method
		if (!isTableAccessNamesResolved) {
			isTableAccessNamesResolved = true;
			resourcePostProcessor.process(context);
		}

		var linkedObjects = super.getLinkedObjects(context, ref, node);
		
		if (linkedObjects.isEmpty()) {
			
        	// handle implicit self parameters for method declarations
        	if (ReferenceableUtil.referencesImplicitSelfParam(context)) {
        		// referencesImplicitSelfParam ensures that funcBody is present
        		var containingFuncBody = EcoreUtil2.getContainerOfType(context, FuncBody.class);
        		//var selfArg = implicitSelfArgs.getSelfArgFor(containingFuncBody);
        		var selfArg = mockObjectCreator.getSelfArgFor(containingFuncBody);
        		return Collections.singletonList(selfArg);
        	}
        	
        	var mockedObject = mockObjectCreator.createMockObjectFor(context);
        	if (mockedObject != null) {
        		return Collections.singletonList(mockedObject);
        	}
		}
		
		return linkedObjects;
	}
	
}
