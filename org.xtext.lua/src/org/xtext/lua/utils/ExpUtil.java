package org.xtext.lua.utils;

import java.util.Optional;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.xtext.lua.lua.Exp;
import org.xtext.lua.lua.ExpLiteral;
import org.xtext.lua.lua.ExpNumberLiteral;
import org.xtext.lua.lua.ExpStringLiteral;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.GroupedExp;
import org.xtext.lua.lua.IndexExpField;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.TableAccess;

public class ExpUtil {
	private static final Logger LOGGER = Logger.getLogger(ExpUtil.class);
	
	private ExpUtil() { }
	
	/**
	 * UNIMPLEMENTED METHOD. This method could be used to implement the resolution of grouped expressions
	 * e.g. to resolve the root of a FeaturePath.
	 */
	public static Optional<Feature> tryResolveGroupedExpToFeature(final GroupedExp groupedExp) {
		// TODO: implement resolution of grouped expressions
		LOGGER.warn("Resolution of GroupedExp is not (yet) supported.");
		return Optional.empty();
	}
	

	//TODO: update doc, how String representation looks for e.g. different types
	//TODO: split into two methods, one for derived state computer (resolving only literals)
	// and another for linkingservice, resolving using references (e.g. variables that contain literals)
	/**
	 * Attempts to resolve the given expression to a String. 
	 * If successful, returns a String representation for the resolved expression.</br>
	 * E.g. "str" -> "str", 0 -> "0.0"
	 * @param exp the Exp.
	 * @param fallback the fallback String returned if the expression cannot be resolved.
	 * @return a String representation of the resolved expression, or the fallback if the expression can not be resolved.
	 */
	public static String tryResolveExpressionToString(final Exp exp, final String fallback) {
		String name = null;
		// A StringLiteral is returned as a String
		if (exp instanceof ExpLiteral literal) {
			name = resolveExpLiteralToNameString(literal);
		} else if (exp instanceof Referencing ref) {
			var assignedValue = AssignmentUtil.tryGetReferencedExp(ref);
			if (assignedValue != null) {
				if (assignedValue instanceof ExpLiteral literal) {
					name = resolveExpLiteralToNameString(literal);
				}
			}
		}
		else {
			// TODO
			LOGGER.warn("TableAccess is not (yet) implemented for non-string indexExps like " + exp);
			//throw new RuntimeException("TableAccess is not (yet) implemented for non-string indexExps!");
		}
		
		if (name == null) {
			return fallback;
		}

		return name;
	}
	
	private static String resolveExpLiteralToNameString(ExpLiteral expLiteral) {
		if (expLiteral instanceof ExpStringLiteral stringLiteral) {
			return tableKeyStringLiteralToNameString(stringLiteral);
		}
		if (expLiteral instanceof ExpNumberLiteral numberLiteral) {
			// TODO: this could make use of the NumberValueConverter, but then the whole
			// expression resolution logic would need to be extracted to an injected bean
			// (in order to inject the valueConverterService).
			
			// we remove the "." separators from the double String representation to avoid
			// problems with the qualifiedNameConverter, which splits Strings on "."
			return tableKeyNumberToNameString(numberLiteral.getValue());
		}
		throw new RuntimeException("Error while resolving ExpLiteral to String.");
	}
	/**
	 * Returns a name string used for the "name" attribute of a TableAccess or Field wich contains a Lua number value.
	 * @param d the double value of the Lua number.
	 * @return the string representation, starting with {@link #NUMBER_NAME_STRING_PREFIX} to 
	 * avoid equality with "name" attributes generated from a string, e.g. a["1"] and a[1] define different fields in table a.
	 */
	public static String tableKeyNumberToNameString(double d) {
		// use , instead of . to avoid clash with qualifiedName separator
		return LuaConstants.NUMBER_NAME_STRING_PREFIX + Double.toString(d).replace(".", ",");
	}
	
	
	public static boolean isTableAccessWithDummyName(EObject o) {
		return o instanceof TableAccess ta && ta.getName().equals(LuaConstants.DERIVED_DUMMY_NAME);
	}
	
	public static boolean isIndexExpFieldWithDummyName(EObject o) {
		return o instanceof IndexExpField ief && ief.getName().equals(LuaConstants.DERIVED_DUMMY_NAME);
	}
	
	private static String tableKeyStringLiteralToNameString(ExpStringLiteral stringLiteral) {
		if (stringLiteral.getValue().startsWith(LuaConstants.NUMBER_NAME_STRING_PREFIX)) {
			throw new RuntimeException("The parsed content contains a String literal");
		}
		return removeQuotesFromString(stringLiteral.getValue());
		//return stringLiteral.getValue();
	}
	
	public static String removeQuotesFromString(String str) {
		if (str != null && str.startsWith("\"") && str.endsWith("\"")) {
			return str.substring(1, str.length() - 1);
		}
		return str;
	}
}
