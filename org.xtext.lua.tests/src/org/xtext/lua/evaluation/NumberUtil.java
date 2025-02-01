package org.xtext.lua.evaluation;

public class NumberUtil {
	public static double roundPercentage(final double percentage) {
		return Math.round(percentage * 100.0)/100.0;
	}
	
	public static double computePercentage(final double of, final double from) {
		final var rel = (from - of)/from;
		return Math.round(rel*10000.0)/100.0;
	}
}
