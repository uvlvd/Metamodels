package org.xtext.lua.wrappers;

import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.PrefixExp;

/**
 * Abstract representation of a feature path.
 * @author jsaenz
 *
 */
public abstract class AbstractFeaturePath {
	/**
	 * The feature this path was created from.
	 */
	private Feature origin;
	/**
	 * The first feature of this FeaturePath.
	 */
	private PrefixExp prefix;
	/**
	 * The last feature of this FeaturePath.
	 */
	private Feature leaf;
	
	protected AbstractFeaturePath(final Feature origin) {
		this.origin = origin;
	}
	
	public Feature getOrigin() {
		return origin;
	}
	
	/**
	 * Returns the root {@link #PrefixExp} of this FeaturePath.
	 */
	public PrefixExp getPrefix() {
		if (prefix == null) {
			prefix = FeaturePathUtil.getFeaturePathPrefix(origin);
		}
		return prefix;
	}

	public Feature getLeaf() {
		if (leaf == null) {
			leaf = FeaturePathUtil.getFeaturePathLeaf(origin);
		}
		return leaf;
	}
}
