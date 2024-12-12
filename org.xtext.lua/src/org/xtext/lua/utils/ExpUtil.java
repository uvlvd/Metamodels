package org.xtext.lua.utils;

import java.util.Optional;

import org.xtext.lua.lua.Exp;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.GroupedExp;

public class ExpUtil {
	
	/**
	 * UNIMPLEMENTED METHOD. This method could be used to implement the resolution of grouped expressions
	 * e.g. to resolve the root of a FeaturePath.
	 */
	public static Optional<Feature> tryResolveGroupedExpToFeature(final GroupedExp groupedExp) {
		// TODO: implement resolution of grouped expressions
		return Optional.empty();
	}
}
