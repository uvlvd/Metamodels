package org.xtext.lua.mocking;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.lua.Arg;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Chunk;
import org.xtext.lua.lua.FuncBody;
import org.xtext.lua.lua.LuaFactory;
import org.xtext.lua.lua.NamedFeature;
import org.xtext.lua.lua.Var;
import org.xtext.lua.utils.LuaConstants;

import com.google.inject.Inject;

public class MockObjectCreator implements IMockObjectCreator {
	public static final URI MOCKED_RESOURCE_URI = URI.createURI("mock:/mocked_objects.lua");

	private Map<Integer, Var> varMap = new HashMap<>();
	private Block mockBlock = null;
	// used as reference for Assignment with less expressions than vars
	private SyntheticExpNil expNilForAssignable = null; 
	// used as references for implicit self parameters of methods
	private Map<FuncBody, Arg> funcBodyToSelfArg = new HashMap<>();
	
	// TODO: should handle require calls to external libraries differently, e.g. save the synthetic vars to special resources depending on the require
	// calls parameter
	public EObject createMockObjectFor(final EObject context) {
		var mockBlock =  getOrCreateMockBlock(MOCKED_RESOURCE_URI, context);
		
		if (context instanceof NamedFeature namedFeature) {
			final var featureName = namedFeature.getName();
			final var block = EcoreUtil2.getContainerOfType(namedFeature, Block.class);
			final var hash = featureName.hashCode() + block.hashCode();
			var syntheticVar = varMap.get(hash);
			if (syntheticVar == null) {
				syntheticVar = createSyntheticVarAndAddToVarMap(featureName, block);
			}
			
			var syntheticExpList = LuaFactory.eINSTANCE.createExpList();
			syntheticExpList.getExps().add(LuaFactory.eINSTANCE.createExpNil());
			var syntheticAssignment = LuaFactory.eINSTANCE.createAssignment();
			syntheticAssignment.setExpList(syntheticExpList);
			syntheticAssignment.getVars().add(syntheticVar);
			mockBlock.getStats().add(syntheticAssignment);
			return syntheticVar;
		}
		
		return null;
	}
	
	/**
	 * Returns a synthetic ExpNil for use as referenced value in assignments with less expressions then vars.
	 */
	public SyntheticExpNil getSyntheticExpNilForAssignable(EObject context) {
		if (expNilForAssignable == null) {
			var mockBlock =  getOrCreateMockBlock(MOCKED_RESOURCE_URI, context);
			var syntheticExpList = LuaFactory.eINSTANCE.createExpList();
			expNilForAssignable = new SyntheticExpNil();
			syntheticExpList.getExps().add(expNilForAssignable);
			var syntheticAssignment = LuaFactory.eINSTANCE.createAssignment();
			syntheticAssignment.setExpList(syntheticExpList);
			var syntheticVar = new SyntheticVar();
			syntheticVar.setName("synthetic_exp_list_var");
			syntheticAssignment.getVars().add(syntheticVar);
			mockBlock.getStats().add(syntheticAssignment);
		}
		return expNilForAssignable;
	}
	
	public Arg getSelfArgFor(FuncBody funcBody) {
		if (funcBodyToSelfArg.containsKey(funcBody)) {
			return funcBodyToSelfArg.get(funcBody);
		}
		
		var selfArg = LuaFactory.eINSTANCE.createArg();
		selfArg.setName(LuaConstants.SELF_PARAM_NAME);
		
		var mockedFunctionBlock = LuaFactory.eINSTANCE.createBlock();
		var mockedParList = LuaFactory.eINSTANCE.createParList();
		var mockedArgList = LuaFactory.eINSTANCE.createArgList();
		mockedArgList.getArgs().add(selfArg);
		mockedParList.setArgsList(mockedArgList);
		
		
		var mockedFunction = LuaFactory.eINSTANCE.createLocalFunctionDeclaration();
		mockedFunction.setName("self_arg_mocked_func_"  + funcBody.hashCode());
		var mockedFunctionBody = LuaFactory.eINSTANCE.createFuncBody();
		mockedFunctionBody.setBlock(mockedFunctionBlock);
		mockedFunctionBody.setParList(mockedParList);
		mockedFunction.setBody(mockedFunctionBody);
		
		var mockBlock =  getOrCreateMockBlock(MOCKED_RESOURCE_URI, funcBody);
		mockBlock.getStats().add(mockedFunction);
		funcBodyToSelfArg.put(funcBody, selfArg);
		return selfArg;
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
