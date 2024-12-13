package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.xtext.lua.lua.Block;

/**
 * Abstract implementation of a {@link IScopeProvider} that traverses Lua blocks, starting from the parent block of the given context objects.
 * When no candidates are found inside of a block, it's parent block is considered, returning the scope as soon as any candidates are found.
 * @author jsaenz
 */
public abstract class LuaAbstractBlockScopeProvider implements IScopeProvider {

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
    	var scope = getScopeFromBlock(context, reference, currentBlock, previousBlock);
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
    
    /**
     * Computes the scope for the given block. The traversal of contained blocks as well as the given previous block is subject to implementation.
     * @param context the context object.
     * @param reference the reference.
     * @param currentBlock the given block.
     * @param previousBlock the previously traversed block.
     * @return the scope of the block, null if no candidates for the context where found in the block.
     */
    protected abstract IScope getScopeFromBlock(final EObject context, final EReference reference, final Block currentBlock, final Block previousBlock);

}
