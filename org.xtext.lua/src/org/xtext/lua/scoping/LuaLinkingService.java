package org.xtext.lua.scoping;

import java.util.Collections;
import java.util.Iterator;
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
import org.xtext.lua.lua.LuaFactory;
import org.xtext.lua.lua.Referenceable;

public class LuaLinkingService extends DefaultLinkingService {
    private static final Logger LOGGER = Logger.getLogger("LuaLinkingService");
    private static final URI mockUri = URI.createURI("dummy:/stdlibAndCrowns.lua");
    
    /**
     * Creates a dummy resource in the contexts resource set
     */
    private Resource getOrCreateMockResource(EObject context) {
        // create a dummy URI with the DSL's file extension
        ResourceSet resourceSet = context.eResource()
            .getResourceSet();

        Resource resource = resourceSet.getResource(mockUri, false);
        if (resource == null ) {
            resource = resourceSet.createResource(mockUri);
        }
        return resource;
    }

    private EObject getOrCreateMockReferenceable(EObject context, String name) {
        var mockResource = getOrCreateMockResource(context);
        // if we already created a mock refble we return it
        for (Iterator<EObject> i = mockResource.getAllContents(); i.hasNext();) {
            var eObj = i.next();
            var refble = (Referenceable) eObj;
            if (refble.getName()
                .equals(name)) {
                return eObj;
            }
        }

        // if not we create one
        LOGGER.debug(String.format("Mocking Referenceable with name %s", name));
        var refble = LuaFactory.eINSTANCE.createReferenceable();
        refble.setName(name);

        List<EObject> contents = mockResource.getContents();
        contents.add(refble);
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
        return Collections.singletonList(mockReferenceable);
    }
}
