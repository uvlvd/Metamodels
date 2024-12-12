package org.xtext.lua.scoping;

import java.util.Collection;
import java.util.List;

import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.xtext.lua.lua.Referenceable;

import com.google.inject.Inject;

public class FeaturePathCandidateBuilder {
	
	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;
	
	/**
	 * Returns {@link FeaturePathCandidate}s built from the given {@link Referenceable}s with {@link FeaturePathCandidate#indexToCheck} = 0.
	 */
	protected List<FeaturePathCandidate> buildFeaturePathCandidates(final Collection<? extends Referenceable> referenceables) {
    	return buildFeaturePathCandidates(referenceables, 0);
    }
    
	/**
	 * Returns {@link FeaturePathCandidate}s built from the given {@link Referenceable}s with {@link FeaturePathCandidate#indexToCheck} set to the  given startIndex.
	 */
    protected List<FeaturePathCandidate> buildFeaturePathCandidates(final Collection<? extends Referenceable> referenceables, final int startIndex) {
    	return referenceables.stream()
    						 .map(referenceable -> buildFeaturePathCandidate(referenceable, startIndex))
    						 .toList();
    }

    /**
	 * Returns the {@link FeaturePathCandidate} built from the given {@link Referenceable} with {@link FeaturePathCandidate#indexToCheck} set to the  given startIndex.
	 */
    protected FeaturePathCandidate buildFeaturePathCandidate(final Referenceable referenceable, final int startIndex) {
    	final var fqn = qualifiedNameProvider.getFullyQualifiedName(referenceable);
    	return new FeaturePathCandidate(referenceable, fqn, startIndex);
    }    
}
