package org.xtext.lua.wrappers;

import java.util.Optional;

import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.NamedFeature;

/**
 * FeaturePath implementation that ensures created FeaturePaths have a resolvable Feature as a root and a last NamedFeature.
 * @author jsaenz
 */
public class FeaturePath extends AbstractFeaturePath {

	/**
	 * The first feature of this FeaturePath.
	 */
	private Feature root;
	
	/**
	 * The last named feature of this FeaturePath.
	 */
	private NamedFeature namedLeaf;
	
	/**
	 * Use {@link FeaturePath#of} to create FeaturePaths. 
	 */
	private FeaturePath(final Feature origin) {
		super(origin);
	}
	
	/**
	 * Creates a FeaturePath if the given Features feature path contains a resolvable root and a {@link NamedFeature}.
	 * @param origin the origin Feature.
	 */
	public static Optional<FeaturePath> of(final Feature origin) {
		var featurePath = new FeaturePath(origin);
		
		final var rootOpt = FeaturePathUtil.getFeaturePathRoot(featurePath.getContext());
		final var namedLeafOpt = FeaturePathUtil.getFeaturePathNamedLeaf(featurePath.getContext());
		
		if (rootOpt.isEmpty() || namedLeafOpt.isEmpty()) {
			return Optional.empty();
		}
		
		featurePath.root = rootOpt.get();
		featurePath.namedLeaf = namedLeafOpt.get();
		return Optional.of(featurePath);
	}


	/**
	 * Returns the root of this FeaturePath as an {@link #Optional}, since the root may be a {@link #GroupedExp}
	 * which is not always resolvable.
	 */
	public Feature getRoot() {
		return root;
	}
	/**
	 * Returns the last named feature of this FeaturePath.
	 */
	public NamedFeature getNamedLeaf() {
		return namedLeaf;
	}
	
	
}
