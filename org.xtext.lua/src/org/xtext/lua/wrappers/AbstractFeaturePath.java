package org.xtext.lua.wrappers;

import java.util.ArrayList;
import java.util.List;

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
	private Feature context;
	/**
	 * The first feature of this FeaturePath.
	 */
	private PrefixExp prefix;
	/**
	 * The last feature of this FeaturePath.
	 */
	private Feature leaf;
	/**
	 * The features of this FeaturePath from root to context Feature.
	 */
	private List<Feature> contextFeatures;
	
	protected AbstractFeaturePath(final Feature origin) {
		this.context = origin;
	}
	
	/**
	 * Returns the feature this path was created from.
	 */
	public Feature getContext() {
		return context;
	}
	
	/**
	 * Returns the root {@link #PrefixExp} of this FeaturePath.
	 */
	public PrefixExp getPrefix() {
		if (prefix == null) {
			prefix = FeaturePathUtil.getFeaturePathPrefix(context);
		}
		return prefix;
	}

	public Feature getLeaf() {
		if (leaf == null) {
			leaf = FeaturePathUtil.getFeaturePathLeaf(context);
		}
		return leaf;
	}
	/**
	 * Returns the features of this FeaturePath from root to context Feature, in order.
	 */
	public List<Feature> getContextFeatures() {
		if (contextFeatures == null) {
			contextFeatures = new ArrayList<>();
			Feature next = prefix;
			contextFeatures.add(next);
			
			while (next != context) {
				next = next.getSuffixExp();
				contextFeatures.add(next);
			}		
		}
		return contextFeatures;
	}
	
}
