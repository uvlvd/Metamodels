package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;

public class FeaturePathCandidate {
	private int indexToCheck = 0;
	private QualifiedName qualifiedName;
	private EObject context;
	
	public FeaturePathCandidate(final EObject context, final QualifiedName fqn) {
		this.context = context;
		this.qualifiedName = fqn;
	}
	
	public void setIndexToCheck(int index) {
		this.indexToCheck = index;
	}
	
	public boolean check(String featureName) {
		final var fqnSegments = qualifiedName.getSegments();
		if (fqnSegments.size() < indexToCheck) {
			throw new RuntimeException("Cannot check FeaturePathCandidate for " + context + " with fqn" + qualifiedName + " at index " + indexToCheck);
		}
		return fqnSegments.get(indexToCheck).equals(featureName);
	}
	
}
