package org.xtext.lua.scoping;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.utils.LinkingAndScopingUtils;

// Contract:
//   1. The FeaturePathCandidate.indexToCheck is always the index that is to be checked next for the candidate
//       -> previous indexes have already been checked
//   2. When the FeaturePathCandidate is completely matched, the indexToCheck is at fqnSegments.size
public class FeaturePathCandidate {
	private final QualifiedName qualifiedName;
	private final List<String> fqnSegments;
	private final Referenceable context;
	
	private int indexToCheck = 0;
	
	public FeaturePathCandidate(final Referenceable context, final QualifiedName fqn) {
		this.context = context;
		this.qualifiedName = fqn;
		this.fqnSegments = fqn.getSegments();
	}
	
	public FeaturePathCandidate(final Referenceable context, final QualifiedName fqn, final int startIndex) {
		this.context = context;
		this.qualifiedName = fqn;
		this.fqnSegments = fqn.getSegments();
		if (startIndex > fqnSegments.size()) {
			throw new RuntimeException("Setting startIndex which is > fqnSegments.size() for FeaturePathCandidate is not allowed.");
		}
		this.indexToCheck = startIndex;
	}
	
	// TODO: remove if unused
	public void setIndexToCheck(int index) {
		this.indexToCheck = index;
	}
	
	// TODO: rename to checkNameAtAndIncrementIndex or smth.
	public boolean checkAndIncrementIndex(String featureName) {
		if (indexToCheck < fqnSegments.size() ) {
			var result = fqnSegments.get(indexToCheck).equals(featureName);
			this.indexToCheck++;
			return result;
		}
		return false; // candidate has no more segments/is completely matched
		//throw new RuntimeException("Cannot check FeaturePathCandidate for " + context + " with fqn " + qualifiedName + " at index " + indexToCheck);
	}
	
	public Referenceable getReferenceable() {
		return context;
	}
	
	/**
	 * Checks if the contained context has an assigned exp, i.e. checks: </ br>
	 * 1. If the candidate's segments were all matched ( => no further segments)
	 * 2. If the candidate's context object is an assignable ( => appears on rhs of an assignment).
	 * @return
	 */
	public boolean referencesReferencing() {
		//return !hasNextUncheckedIndex()  // can only have a value assigned if current index is last
		return isCompletelyMatched() 	
				&& context instanceof Referencing referencing
				&& referencing.getRef() instanceof Referencing
				&& LinkingAndScopingUtils.isAssignable(context);
	}
	
	/**
	 * Use {@link #referencesReferencing()} first!
	 * @return the Referencing this FeaturePathCandidate's context is assigned to.
	 */
	public Referencing getReferencedReferencing() {
		if (context instanceof Referencing referencing) {
			var assigned = referencing.getRef();
			if (assigned instanceof Referencing referencedReferencing) {
				return referencedReferencing;
			}
		} 
		throw new RuntimeException("Cannot get Reference from non-referencing FeaturePathCandidate with context " + context + " and fqn " + qualifiedName);	
	}
	
	/**
	 * Returns true if all segments of this candidate were matched.
	 */
	public boolean isCompletelyMatched() {
		return fqnSegments.size() == indexToCheck;
	}
	
	public boolean hasNextUncheckedIndex() {
		return indexToCheck < fqnSegments.size();
	}
	
	
}
