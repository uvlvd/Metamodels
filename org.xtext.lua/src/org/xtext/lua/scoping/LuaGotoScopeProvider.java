package org.xtext.lua.scoping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Goto;
import org.xtext.lua.lua.Label;
import org.xtext.lua.lua.LuaPackage.Literals;

import com.google.inject.Inject;

public class LuaGotoScopeProvider extends LuaAbstractBlockScopeProvider {
	
	@Inject
	private DescriptionCreator descriptionCreator;
	
	@Inject 
	private LinkingHelper linkingHelper;

	@Override
	protected IScope getScopeFromBlock(final EObject context, final EReference reference, final Block currentBlock, final Block previousBlock) {
		if (context instanceof Goto goTo) {
			final var refString = getGotoRefString(goTo);
			final var referenceables = EcoreUtil2.getAllContentsOfType(currentBlock, Label.class)
	    			.stream()
	    			// we ignore the previous block, since it has been searched before (see getScopeByTraversingBlocks)
	    			.filter(block -> block != previousBlock)
	    			.filter(label -> label.getName().equals(refString))
	    			.collect(Collectors.toCollection(() -> new ArrayList<>()));
	    	if (referenceables.isEmpty()) {
	    		return null;
	    	}
	    	// reverse s.t. last defined Referenceable is first candidate
	    	Collections.reverse(referenceables);
	    	return new SimpleScope(descriptionCreator.createFor(referenceables));
		} else {
			throw new RuntimeException("Called LuaGotoScopeProvider with non-Goto object: " + context);
		}
	}
	
	/**
	 * Returns the String of the {@link Goto#getRef()}, i.e. the name of the label this {@link Goto} references.
	 */
	private String getGotoRefString(final Goto goTo) {
		var refNodes = NodeModelUtils.findNodesForFeature(goTo, Literals.REFERENCING__REF);
		if (refNodes.isEmpty()) {
			throw new RuntimeException("Could not find ref string for Goto object: " + goTo);
		}
		var node = refNodes.get(0);
		return linkingHelper.getCrossRefNodeAsString(node, false);
	}

}
