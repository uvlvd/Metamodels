package org.xtext.lua.mocking;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Chunk;
import org.xtext.lua.lua.LuaFactory;
import org.xtext.lua.lua.NamedFeature;
import org.xtext.lua.lua.Var;

public class MockObjectCreator implements IMockObjectCreator {
	private static final URI VAR_MOCK_URI = URI.createURI("dummy:/syntheticVars.lua");
	
	private Map<Integer, Var> varMap = new HashMap<>();
	private Block mockBlock = null;
	
	// TODO: should handle require calls to external libraries differently, e.g. save the synthetic vars to special resources depending on the require
	// calls parameter
	public EObject createMockObjectFor(final EObject context) {
		var mockBlock =  getOrCreateMockBlock(VAR_MOCK_URI, context);
		
		if (context instanceof NamedFeature namedFeature) {
			final var featureName = namedFeature.getName();
			final var block = EcoreUtil2.getContainerOfType(namedFeature, Block.class);
			final var hash = featureName.hashCode() + block.hashCode();
			var syntheticVar = varMap.get(hash);
			if (syntheticVar == null) {
				syntheticVar = createSyntheticVarAndAddToVarMap(featureName, block);
			}
			
			var syntheticExpList = LuaFactory.eINSTANCE.createExpList();
			var syntheticAssignment = LuaFactory.eINSTANCE.createAssignment();
			syntheticAssignment.setExpList(syntheticExpList);
			syntheticAssignment.getVars().add(syntheticVar);
			mockBlock.getStats().add(syntheticAssignment);
			return syntheticVar;
		}
		
		return null;
	}
	
	private SyntheticVar createSyntheticVarAndAddToVarMap(String name, Block containingBlock) {
		var syntheticVar = new SyntheticVar();
		syntheticVar.setName(name);
		
		varMap.put(createVarMapKey(name, containingBlock), syntheticVar);
		return syntheticVar;
		
	}
	
	private int createVarMapKey(String name, Block containingBlock) {
		return name.hashCode() + containingBlock.hashCode();
	}
	
	/**
     * Creates a dummy resource in the contexts resource set
     */
    private Block getOrCreateMockBlock(URI uri, EObject context) {
    	if (mockBlock != null) {
    		return mockBlock;
    	}
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
            mockBlock = ((Chunk) resource.getContents()
                .get(0)).getBlock();
            return mockBlock;
        }
        return null;
    }
}
