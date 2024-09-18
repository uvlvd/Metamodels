package org.xtext.lua.utils;

import java.util.Optional;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.xtext.lua.lua.Assignment;
import org.xtext.lua.lua.Exp;
import org.xtext.lua.lua.ExpStringLiteral;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.Var;

public final class LinkingAndScopingUtils {
	private static final Logger LOGGER = Logger.getLogger(LinkingAndScopingUtils.class);
	
	//TODO: isAssignable function (replaces isOnLhsOfAssignment): refble needs to be leaf
	//      of Featue path
	/**
	 * Returns true if the given object is part of the lhs of an assignment, i.e. gets assigned a value. </br>
	 * E.g. a.member = 10 returns true for member, false for a
	 * @param obj the object.
	 */
	public static boolean isAssignable(EObject obj) {
		// only features that are Referenceable can be assignables (i.e. Vars, MemberAccess, TableAccess, ... but not FunctionCalls etc.)
		if (obj instanceof Feature feature && obj instanceof Referenceable) {
			final var isLeaf = !feature.eContents().stream().anyMatch(child -> child instanceof Feature);
			if (isLeaf) { // only leafs of a feature path are assignable
				return findParentAssignment(feature).isPresent();
			}
		}
		return false;
	}
	
	/**
	 * Returns the expression assigned to this feature. Use {@link #isAssignable(EObject)} to ensure
	 * the given feature is an Assignable before calling this function.
	 * @param feature the Feature (must be a Referenceable).
	 * @return the value expression assigned to the feature.
	 */
	public static Exp findAssignedExp(Feature feature) {
		if (!(feature instanceof Referenceable)) {
			throw new RuntimeException("Cannot find assigned expression for non-referenceable feature!");
		}
		
		final var featurePathRootOpt = findFeaturePathRoot(feature);
		if (featurePathRootOpt.isEmpty()) {
			return null;
		}
		final var featurePathRoot = featurePathRootOpt.get();
		
		final var assignmentOpt = findParentAssignment(featurePathRoot);
		if (assignmentOpt.isPresent()) {
			final var assignment = assignmentOpt.get();
			// TODO: need to know root of feature path for refble to find it in vars
            var index = assignment.getVars().indexOf(featurePathRoot);
            if (index < 0) // should never happen
            	throw new RuntimeException("Could not find feature path root (Variable) in assignment!");

            final var exps = assignment.getExpList().getExps();
            if (exps.size() > index)
            	return assignment.getExpList().getExps().get(index);
		}
		// fall-through, e.g. if explist does not contain an exp for every declared var (i.e. value is 'nil')
		return null;
	}
	
	/**
	 * Returns the Assignment object this feature is contained in.
	 */
	private static Optional<Assignment> findParentAssignment(Feature feature) {
		var parent = feature.eContainer();
		
		if (parent == null) { // no parent
			return Optional.empty();
		}
		
		if (parent instanceof Assignment assignment) {
			return Optional.of(assignment);
		}
		
		if (parent instanceof Feature featureParent) {
			return findParentAssignment(featureParent);
		} 
		
		// object is on rhs or not part of an assignment/feature path.
		return Optional.empty();
	}
	
	// TODO: could conceivably also be a parenthesized Expression (exp).member...
	private static Optional<Var> findFeaturePathRoot(Feature feature) {
		var parent = feature.eContainer();
		
		if (parent == null) { // no parent
			return Optional.empty();
		}
		
		if (parent instanceof Assignment assignment) {
			if (feature instanceof Var var) {
				return Optional.of(var);
			} else {
				throw new RuntimeException("Feature path root is not of type Var.");
			}
		}
		
		if (parent instanceof Feature parentFeature) {
			return findFeaturePathRoot(parentFeature);
		} 
		
		// object is on rhs or not part of an assignment/feature path.
		return Optional.empty();
	}
	
	
	
	//TODO: update doc, how String representation looks for e.g. different types
	/**
	 * Attempts to resolve the given expression to a String. 
	 * If successful, returns a String representation for the resolved expression.</br>
	 * E.g. "str" -> "str", 1+1 -> "N_2"
	 * @param tableAccess the TableAccess.
	 * @return a String representation of the resolved index-expression, if any.
	 */
	public static String tryResolveExpressionToString(Exp exp) {
		String name = null;
		// A StringLiteral is returned as a String
		if (exp instanceof ExpStringLiteral stringLiteral) {
			name = stringLiteralToString(stringLiteral);
		} else if (exp instanceof Referencing ref) {
			if (ref.getRef() == null) {
				throw new RuntimeException("Attempting to resolve value expression, but a ref " + ref + " is not yet resolved!");
			} else {
				if (!ref.getRef().eIsProxy()) {
					var assignedValue = ((Referencing) ref.getRef()).getRef();
					if (assignedValue instanceof ExpStringLiteral stringLiteral) {
						System.out.println("HELLLOOO: " + stringLiteral);
						name = stringLiteralToString(stringLiteral);
					}
				}

			}

		}
		else {
			// TODO
			LOGGER.warn("TableAccess is not (yet) implemented for non-string indexExps!");
			//throw new RuntimeException("TableAccess is not (yet) implemented for non-string indexExps!");
		}
		if (name == null) {
			return "testName";
		}
		return name;
	}
	
	private static String stringLiteralToString(ExpStringLiteral stringLiteral) {
		return removeQuotesFromString(stringLiteral.getValue());
	}
	
	private static String removeQuotesFromString(String str) {
		return str.substring(1, str.length() - 1);
	}
	
	
}
