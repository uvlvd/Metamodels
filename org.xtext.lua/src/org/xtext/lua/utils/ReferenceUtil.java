package org.xtext.lua.utils;

import java.util.ArrayList;
import java.util.List;

import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;

public class ReferenceUtil {
	private static final int MAX_RECURSION_DEPTH = 1000;
	
	/**
	 * Returns the last element in the given Referencings reference chain by recursively
	 * calling {@link Referencing#getRef()}.
	 * @param referencing
	 * @return the referenceable referenced from the last element in the reference chain, may be null.
	 */
	public static Referenceable getReferencedElement(final Referencing referencing) {
		final var referenceChain = getReferenceChain(referencing);
		if (referenceChain.size() > 0) {
			return referenceChain.get(referenceChain.size() - 1).getRef();
		}
		return referencing.getRef();
	}
	
	/**
	 * Returns the given Referencings reference chain built by recursively
	 * calling {@link Referencing#getRef()}.
	 * @param referencing
	 * @return the reference chain, not containing the given referencing. The last element references the element referenced through the reference chain.
	 */
	public static List<Referencing> getReferenceChain(final Referencing referencing) {
		return collectReferenceChain(referencing, new ArrayList<>(), 0, MAX_RECURSION_DEPTH);
	}
	
	private static List<Referencing> collectReferenceChain(final Referencing referencing, List<Referencing> referenceChain, int currDepth, final int maxDepth) {
		if (currDepth > maxDepth) {
			throw new RuntimeException("Reached max depth while attempting to traverse reference chain for " + referencing);
		}
		
		var referenced = referencing.getRef();
		
		// referenced element is itself Referencing, traverse further
		if (referenced instanceof Referencing referencingsReferencing) {
			
			// Currently, vars on the lhs on an assignment assigned to a FeaturePath on the rhs
			// erroneously reference (point to) the FeaturePath root insted of the FeaturePath leaf.
			// This check can be removed whenever this problem has been refactored.
			if (referencingsReferencing instanceof Feature feature) {
				final var namedLeafOpt = FeatureUtil.findFeaturePathNamedLeaf(feature);
				if (namedLeafOpt.isPresent()) {
					referencingsReferencing = namedLeafOpt.get();
				}
			}
			
			referenceChain.add(referencingsReferencing);
			return collectReferenceChain(referencingsReferencing, referenceChain, ++currDepth, maxDepth);
		}
		
		// end of reference chain reached
		return referenceChain;
	}
	
}
