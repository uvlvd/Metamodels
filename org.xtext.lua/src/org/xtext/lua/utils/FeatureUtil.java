package org.xtext.lua.utils;

import java.util.Optional;

import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.MethodCall;
import org.xtext.lua.lua.NamedFeature;
import org.xtext.lua.lua.PrefixExp;
import org.xtext.lua.lua.Var;

public class FeatureUtil {
	
	private FeatureUtil() { }
	/**
	 * Returns true if the given feature is a FunctionCall or MethodCall-
	 */
	public static boolean isFunctionOrMethodCallFeature(Feature feature) {
		return feature instanceof FunctionCall || feature instanceof MethodCall;

	}
	
	public static boolean isNamedLeafOfFeaturePath(Feature feature) {
		final var namedLeafOpt = findFeaturePathNamedLeaf(feature);
		if (namedLeafOpt.isPresent()) {
			return namedLeafOpt.get() == feature;
		}
		return false;
	}
	
	// TODO: might want to return param feature if it is the named leaf?
	/**
	 * Returns an {@link Optional} containing the last {@link NamedFeature} of the given Feature's feature path.
	 */
	public static Optional<NamedFeature> findFeaturePathNamedLeaf(final Feature feature) {
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
	
	/**
	 * Returns the last {@link Feature} of the given Features feature path.
	 */
	public static Feature getFeaturePathLeaf(final Feature feature) {
		final var suffix = feature.getSuffixExp();
		if (suffix == null) {
			return feature;
		}
		return getFeaturePathLeaf(suffix);
	}
	
	/**
	 * Returns the root of the given Feature's feature path if it is a {@link Var}.
	 */
	public static Optional<Var> findFeaturePathRootAsVar(final Feature feature) {
		final var featurePathRoot = findFeaturePathRoot(feature);
		if (featurePathRoot instanceof Var var) {
			return Optional.of(var);
		}
		return Optional.empty();
	}
	
	/**
	 * Returns the root of the given Feature's feature path.
	 * 
	 * @return the root, a Var or GroupedExp.
	 */
	public static PrefixExp findFeaturePathRoot(final Feature feature) {
		final var previous = getPreviousFeature(feature);
		if (previous != null) {
			return findFeaturePathRoot(previous);
		}
		return (PrefixExp) feature;
	}
	
	/**
	 * Returns the [@link PrefixExp} of the given Feature's feature path if it is a {@link Var}.
	 * In contrast to {@link #findFeaturePathRootAsVar}, this returns the first {@link PrefixExp} found,
	 * wich may not necessarily equal the root of the feature path (e.g. for a feature path inside a TableAccess index expression).
	 * 
	 */
	public static Optional<Var> findFeaturePathPrefixAsVar(final Feature feature) {
		final var featurePathPrefix = findFeaturePathPrefix(feature);
		if (featurePathPrefix instanceof Var var) {
			return Optional.of(var);
		}
		return Optional.empty();
	}
	
	/**
	 * Returns the first {@link PrefixExp} of the given Feature's feature path before the given Feature.
	 * 
	 * @return the root, a Var or GroupedExp.
	 */
	public static PrefixExp findFeaturePathPrefix(final Feature feature) {
		if (feature instanceof PrefixExp prefixExp) {
			return prefixExp;
		}
		return findFeaturePathPrefix(getPreviousFeature(feature));
	}
	
	/**
	 * Returns the previous Feature of the given Feature's feature path, or null if the given Feature is the root Feature.
	 */
	public static Feature getPreviousFeature(final Feature feature) {
		final var parent = feature.eContainer();
		if (parent instanceof Feature previousFeature) {
			return previousFeature;
		}
		return null;
	}
	
	public static boolean hasNextFeature(Feature feature) {
		return feature.getSuffixExp() != null;
	}
	
	/**
	 * May return null, call {@link #hasNextFeature} first.
	 */
	public static Feature getNextFeature(Feature feature) {
		return feature.getSuffixExp();
	}
	
	/**
	 * Returns the first {@link NamedFeature} found by traversing the given {@link Feature}'s {@link FeaturePath}
	 * upwards (in direction of the {@link FeaturePath}'s root).
	 * @return the first {@linked NamedFeature} found along the path in direction of the root, the given {@Feature} itself if it is a {@link NamedFeature}.
	 */
	public static NamedFeature getFirstNamedPrefix(final Feature feature) {
		if (feature instanceof NamedFeature named) {
			return named;
		}
		return getFirstNamedPrefix(getPreviousFeature(feature));
	}
}
