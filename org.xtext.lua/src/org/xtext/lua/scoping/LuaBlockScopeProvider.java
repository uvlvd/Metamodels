package org.xtext.lua.scoping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.Goto;
import org.xtext.lua.lua.Label;
import org.xtext.lua.utils.ReferenceableUtil;
import org.xtext.lua.utils.StatUtil;

import com.google.inject.Inject;

public class LuaBlockScopeProvider implements IScopeProvider {
	private static final Logger LOGGER = Logger.getLogger(LuaBlockScopeProvider.class);
	
	@Inject
	private DescriptionCreator descriptionCreator;
	
	@Inject
	private FeatureScopeHelper featureScopeHelper;

	@Override
	public IScope getScope(final EObject context, final EReference reference) {
		// we pass context and reference for calls to "require" (need to call the global scope, see getReferenceablesFromRequireCall)
		return getScopeByTraversingBlocks(context, reference);
	}
    
    private IScope getScopeByTraversingBlocks(final EObject context, final EReference reference) {
    	final var currentBlock = EcoreUtil2.getContainerOfType(context, Block.class);
    	return getScopeByTraversingBlocks(context, reference, currentBlock, null);
    }
    
    private IScope getScopeByTraversingBlocks(final EObject context, final EReference reference, final Block currentBlock, final Block previousBlock) {
    	// search for candidates in current block
    	var scope = getCandidatesFromBlock(context, reference, currentBlock, previousBlock);
    	if (scope != null) {
    		return scope;
    	}
    	
    	// search for candidates in parent block if none were found in current block
    	final var parentBlock = EcoreUtil2.getContainerOfType(currentBlock.eContainer(), Block.class);
		if (parentBlock != null) {
			return getScopeByTraversingBlocks(context, reference, parentBlock, currentBlock);
		}
		
		// no parent block found, try global scope
		return null;
    }
    
    private IScope getCandidatesFromBlock(final EObject context, final EReference reference, final Block currentBlock, final Block previousBlock) {
    	// Get candidate Labels for Goto: Label names need to be unique within one block)
    	if (context instanceof Goto) {
    		// TODO: do not search global scope for GOTO references
    		return getScopeForGoto(currentBlock, previousBlock);
    	}
    	
    	if (context instanceof Feature feature) {
    		return getScopeForFeature(feature, reference, currentBlock);
    	}
    	
    	throw new RuntimeException("Expected to be able to compute candidates from context object " + context + ", but its type " + context.getClass() + " is not supported.");
    }
    
    private IScope getScopeForGoto(final Block contextBlock, final Block previousBlock) {
    	var referenceables = EcoreUtil2.getAllContentsOfType(contextBlock, Label.class)
    			.stream()
    			// we ignore the previous block, since it has been searched before (see getScopeByTraversingBlocks)
    			.filter(block -> block != previousBlock)
    			.collect(Collectors.toCollection(() -> new ArrayList<>()));
    	
    	if (referenceables.isEmpty()) {
    		return null;
    	}
    	// reverse s.t. last defined Referenceable is first candidate
    	Collections.reverse(referenceables);
    	return new SimpleScope(descriptionCreator.createFor(referenceables));
    }
    
 // For functions, this could be a problem here: https://stackoverflow.com/questions/12291203/lua-how-to-call-a-function-prior-to-it-being-defined
    //  (could also affect Assignments)
    private IScope getScopeForFeature(final Feature feature, final EReference reference, final Block contextBlock) {
    	// we use the parentStatement to decide where to stop searching for candidates (i.e. only consider statements before the context's statement)
		final var contextParentStatementOpt = StatUtil.getParentStatement(feature);
    	if (!contextParentStatementOpt.isPresent()) {
    		LOGGER.warn("Found no contextParentStatement for obj " + feature);
    		return null;
    	}
    	final var contextParentStatement = contextParentStatementOpt.get();
    	
    	var referenceables = ReferenceableUtil.getReferenceablesForContextFromBlock(feature, contextBlock, contextParentStatement);

    	return featureScopeHelper.getScopeForFeatureFromReferenceables(feature, reference, referenceables);
    }

}
