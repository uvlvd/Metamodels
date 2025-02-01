package org.xtext.lua.mocking;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.lua.Var;
import org.xtext.lua.lua.PrefixExp;
import org.xtext.lua.utils.FeatureUtil;

public class FeaturePath {
	/**
	 * The feature this path was created from.
	 */
	private Feature context;
	/**
	 * The first {@link PrefixExp} of this FeaturePath (might not be root, e.g. for context inside {@link TableAccess#getIndexExp}.
	 */
	private PrefixExp prefix;
	/**
	 * The first root of this FeaturePath.
	 */
	private PrefixExp root;
	/**
	 * The last feature of this FeaturePath.
	 */
	private Feature leaf;
	/**
	 * The features of this FeaturePath from root to context Feature.
	 */
	private List<Feature> contextFeatures;
	
	public FeaturePath(final Feature origin) {
		this.context = origin;
	}
	
	/**
	 * Returns the feature this path was created from.
	 */
	public Feature getContext() {
		return context;
	}
	
	/**
	 * Returns the first {@link PrefixExp} relative to the context of this FeaturePath, i.e. the first on the left of the context Feature (might not be root, e.g. for context inside {@link TableAccess#getIndexExp}.
	 */
	// TODO: this is highly confusing: there should be no FeaturePaths containing stuff outside of the feature's FeaturePath,
	// i.e. a FeaturePaths root is ALWAYS the first prefixExp preceding the context feature, and FeaturePaths never contain
	//  features outside their context, e.g. a[b.c.d]: a[...] is one FeaturePath and b.c.d is another, the two paths
	//  have no overlapping Features!
	public PrefixExp getPrefix() {
		if (prefix == null) {
			prefix = FeatureUtil.findFeaturePathPrefix(context);
		}
		return prefix;
	}

	/**
	 * Returns the first {@link PrefixExp} of this FeaturePath.
	 */
	public PrefixExp getRoot() {
		if (root == null) {
			root = FeatureUtil.findFeaturePathRoot(context);
		}
		return root;
	}

	public Feature getLeaf() {
		if (leaf == null) {
			leaf = FeatureUtil.getFeaturePathLeaf(context);
		}
		return leaf;
	}
	/**
	 * Returns the features of this FeaturePath from its PrefixExp to context Feature (the {@link Feature} this path was build from), in order.
	 */
	public List<Feature> getContextFeatures() {
		if (contextFeatures == null) {
			contextFeatures = new ArrayList<>();
			Feature next = getPrefix();
			contextFeatures.add(next);
			
			while (next != context) {
				next = next.getSuffixExp();
				contextFeatures.add(next);
			}		
		}
		return contextFeatures;
	}
	
	public Optional<Var> findPrefixAsVar() {
		if (getPrefix() instanceof Var var) {
			return Optional.of(var);
		}
		return Optional.empty();
	}
	
}
