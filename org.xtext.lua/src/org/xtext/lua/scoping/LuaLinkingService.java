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

    @Override
    public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) throws IllegalNodeException {
        List<EObject> list = super.getLinkedObjects(context, ref, node);

        // has super resolved the ref?
        if (!list.isEmpty()) {
            return list;
        }

        // we create missing refbles in a dummy resource

        String name = getCrossRefNodeAsString(node);

        // create a dummy URI with the DSL's file extension
        URI uri = URI.createURI("dummy:/stdlibAndCrowns.lua");
        ResourceSet resourceSet = context.eResource()
            .getResourceSet();
        Resource resource = resourceSet.getResource(uri, true);

        // if we already created a mock refble we return it
        for (Iterator<EObject> i = resource.getContents()
            .iterator(); i.hasNext();) {
            var eObj = i.next();
            var refble = (Referenceable) eObj;
            if (refble.getName() == name) {
                return Collections.singletonList(eObj);
            }
        }

        // if not we create one
        LOGGER.debug(String.format("Mocking Referenceable with name %s", name));
        var refble = LuaFactory.eINSTANCE.createReferenceable();
        refble.setName(name);

        List<EObject> contents = resource.getContents();
        contents.add(refble);
        return Collections.singletonList((EObject) refble);

//        // create a dummy URI with the DSL's file extension
////        URI uri = URI.createURI("dummy:/" + name + ".lua");
//        URI uri = URI.createURI("dummy:/stdlibAndCrowns.lua");
//        ResourceSet resourceSet = context.eResource()
//            .getResourceSet();
//        Resource resource = resourceSet.getResource(uri, false);
//        Referenceable refble;
//
//        // if the refble doesn't exist we inject one
//        if (resource == null) {
//            LOGGER.debug(String.format("Injecting Referenceable with name %s", name));
//            refble = LuaFactory.eINSTANCE.createReferenceable();
//            refble.setName(name);
//            resource = resourceSet.createResource(uri);
////            resource = resourceSet.getResource(uri, true);
//            List<EObject> contents = resource.getContents();
//            contents.add(refble);
//        } else {
//            refble = (Referenceable) resource.getContents()
//                .get(0);
//        }

//        return Collections.singletonList((EObject) refble);
    }
}
