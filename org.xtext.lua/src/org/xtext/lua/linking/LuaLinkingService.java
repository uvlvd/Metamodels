package org.xtext.lua.linking;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.scoping.IScope;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Chunk;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.LuaFactory;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.utils.LinkingAndScopingUtils;
import org.eclipse.emf.common.util.URI;

import com.google.inject.Inject;

public class LuaLinkingService extends DefaultLinkingService {
	private static final Logger LOGGER = Logger.getLogger(LuaLinkingService.class);
	public static final URI NIL_MOCK_URI = URI.createURI("dummy:/syntheticNilValues.lua");
	
  	@Inject
    private IQualifiedNameConverter nameConverter;
  	
	@Override
	public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) throws IllegalNodeException {		
		var linkedObjects = super.getLinkedObjects(context, ref, node);
		return linkedObjects;
	}
	
	private List<EObject> createOrGetSyntheticLinkedObjects(EObject context) {
		LOGGER.debug("Creating synthetic linked objects for " + context);
    	
		if (LinkingAndScopingUtils.isAssignable(context)) {
			final var name = ((Referenceable) context).getName();
			if (name == null) { // name might be null, e.g. for TableAccess with unresolvable indexExpression
				// TODO: return placeholder object or implement trivial recovery
				return Collections.emptyList();
			}
			
			final var fqn = nameConverter.toQualifiedName(name);
	    	final var value = LinkingAndScopingUtils.findAssignedExp((Feature) context);
			if (value == null) {
				var temp = createOrGetSyntheticNilValue(context);
				if (!(temp == null)) {
					return Collections.singletonList(temp);
				}
			}
		}
		
    	
		
		return Collections.emptyList();
	}
	
	private SyntheticExpNil createOrGetSyntheticNilValue(EObject context) {
		var mockBlock = getOrCreateMockBlock(NIL_MOCK_URI, context);
		// TODO: return existing syntheticExpNil if exists
		var syntheticExpList = LuaFactory.eINSTANCE.createExpList();
		var syntheticAssignment = LuaFactory.eINSTANCE.createAssignment();
		syntheticAssignment.setExpList(syntheticExpList);
		var synthetic = new SyntheticExpNil();
		syntheticAssignment.getExpList().getExps().add(synthetic);
		mockBlock.getStats().add(syntheticAssignment);
		return synthetic;
		//return null;
	}
    

    /**
     * Creates a dummy resource in the contexts resource set
     */
    private Block getOrCreateMockBlock(URI uri, EObject context) {
        // create a dummy URI with the DSL's file extension
        ResourceSet resourceSet = context.eResource()
            .getResourceSet();

        Resource resource = resourceSet.getResource(uri, false);
        if (resource == null) {
            resource = resourceSet.createResource(uri);
            var chunk = LuaFactory.eINSTANCE.createChunk();
            resource.getContents()
                .add(chunk);
            var block = LuaFactory.eINSTANCE.createBlock();
            chunk.setBlock(block);
            return block;
        }
        if (resource.getContents()
            .size() > 0
                && resource.getContents()
                    .get(0) instanceof Chunk) {
            return ((Chunk) resource.getContents()
                .get(0)).getBlock();
        }
        return null;
    }
}
