package org.xtext.lua.wrappers;

import java.util.Optional;

import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.GroupedExp;
import org.xtext.lua.lua.NamedFeature;
import org.xtext.lua.lua.PrefixExp;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Var;
import org.xtext.lua.utils.ExpUtil;

public class FeaturePathUtil {
	
	private FeaturePathUtil() { }

	/**
	 * Returns the previous Feature of the given Feature's feature path, or null if the given Feature is the root Feature.
	 */
	protected static Feature getPreviousFeature(final Feature feature) {
		final var parent = feature.eContainer();
		if (parent instanceof Feature previousFeature) {
			return previousFeature;
		}
		return null;
	}
	
	// TODO: might want to return param feature if it is the named leaf?
	/**
	 * Returns an {@link Optional} containing the last {@link NamedFeature} of the given Feature's feature path.
	 */
	protected static Optional<NamedFeature> getFeaturePathNamedLeaf(final Feature feature) {
		var current = getFeaturePathLeaf(feature);
		var next = getPreviousFeature(current);
		
		while (!(current instanceof NamedFeature) && next != null) {
			current = next;
			next = getPreviousFeature(current);
		}
		
		if (current instanceof NamedFeature namedLeaf) {
			return Optional.of(namedLeaf);
		}
		return Optional.empty();
	}
	
	protected static Feature getFeaturePathLeaf(final Feature feature) {
		final var suffix = feature.getSuffixExp();
		if (suffix == null) {
			return feature;
		}
		return getFeaturePathLeaf(suffix);
	}
	
	/**
	 * Returns the root of the given Feature's feature path.
	 * 
	 * @return the root, a Var or GroupedExp.
	 */
	protected static Optional<Feature> getFeaturePathRoot(final Feature feature) {
		final var prefix = getFeaturePathPrefix(feature);
		if (prefix instanceof GroupedExp groupedExp) { // found root GroupedExp
			return ExpUtil.tryResolveGroupedExpToFeature(groupedExp);
		}

		if (prefix instanceof Var var) { // found root Var
			return Optional.of(var);
		}
		
		return Optional.empty();
	}
	
	/**
	 * Returns the {@link PrefixExp} of the given Feature's feature path.
	 */
	protected static PrefixExp getFeaturePathPrefix(final Feature feature) {
		if (feature instanceof PrefixExp prefixExp) {
			return prefixExp;
		}

		final var previousFeature = getPreviousFeature(feature);
		if (previousFeature != null) {
			return getFeaturePathPrefix(previousFeature);
		}
		
		// should never happen
		throw new RuntimeException("Could not find parent while searching for feature path prefix for: " + feature + ".");
	}
	
}
