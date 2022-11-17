package org.xtext.lua.scoping;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.nodemodel.INode;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Chunk;
import org.xtext.lua.lua.LuaFactory;
import org.xtext.lua.lua.Referenceable;

public class LuaLinkingService extends DefaultLinkingService {
    private static final Logger LOGGER = Logger.getLogger(LuaLinkingService.class.getPackageName());
    public static final URI MOCK_URI = URI.createURI("dummy:/stdlibAndCrowns.lua");

    /**
     * Creates a dummy resource in the contexts resource set
     */
    private Block getOrCreateMockBlock(EObject context) {
        // create a dummy URI with the DSL's file extension
        ResourceSet resourceSet = context.eResource()
            .getResourceSet();

        Resource resource = resourceSet.getResource(MOCK_URI, false);
        if (resource == null) {
            resource = resourceSet.createResource(MOCK_URI);
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

    private EObject getOrCreateMockReferenceable(EObject context, String name) {
        var mockBlock = getOrCreateMockBlock(context);
        if (mockBlock == null) {
            return null;
        }

        // if we already created a mock refble we return it
        for (var statement : mockBlock.getStatements()) {
            if (statement instanceof Referenceable) {
                var refble = (Referenceable) statement;
                if (refble.getName()
                    .equals(name)) {
                    return statement;
                }
            }
        }

        // if not we create one
        LOGGER.debug(String.format("Mocking Referenceable with name %s", name));
        var refble = LuaFactory.eINSTANCE.createReferenceable();
        refble.setName(name);

        mockBlock.getStatements()
            .add(refble);
        return refble;
    }

    @Override
    public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) throws IllegalNodeException {
        List<EObject> list = super.getLinkedObjects(context, ref, node);

        // has super resolved the ref?
        if (!list.isEmpty()) {
            return list;
        }

        // If not:
        // we create mock refbles in a dummy resource
        String name = getCrossRefNodeAsString(node);
        var mockReferenceable = getOrCreateMockReferenceable(context, name);
        if (mockReferenceable != null) {
            return Collections.singletonList(mockReferenceable);
        }

        return Collections.emptyList();
    }
}
