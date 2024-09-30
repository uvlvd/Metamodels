package org.xtext.lua.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.Config;
import org.xtext.lua.lua.Assignment;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.BlockWrapperWithArgs;
import org.xtext.lua.lua.Exp;
import org.xtext.lua.lua.ExpLiteral;
import org.xtext.lua.lua.ExpNumberLiteral;
import org.xtext.lua.lua.ExpStringLiteral;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.FunctionDeclaration;
import org.xtext.lua.lua.GenericFor;
import org.xtext.lua.lua.LastStat;
import org.xtext.lua.lua.NumericFor;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.Stat;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.lua.Var;

public final class LinkingAndScopingUtils {
	private static final Logger LOGGER = Logger.getLogger(LinkingAndScopingUtils.class);
	
	// dummy name set for TableAccess if the indexExp cannot be resolved in DerivedStateComputer
	public static final String DERIVED_DUMMY_NAME = "derived_dummy_name";
	// dummy name set for TableAccess if the indexExp cannot be resolved during Linking
	public static final String LINKING_DUMMY_NAME = "linking_dummy_name";
	
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
			// a Var that is part of a TableAccess cannot be an Assignable (e.g. the var in table[var])
			if (isPartOfAnyTableAccessIndexExp(feature)) {
				return false;
			}
			
			final var isLeaf = !feature.eContents().stream().anyMatch(child -> child instanceof Feature);
			if (isLeafOfFeaturePath(feature)) { // only leafs of a feature path are assignable
			//if (isLeaf) {
				// check if an Assignment is parent and leaf is on lhs
				return findParentAssignmentForAssignable(feature).isPresent();
			}
		}
		return false;
	}
	
	private static boolean isLeafOfFeaturePath(Feature feature) {
		var featureChildren = feature.eContents().stream().filter(child -> child instanceof Feature).toList();
		if (feature instanceof TableAccess ta) {
			// since a table access itself can be a leaf, but may also contain Features in its indexExp,
			// we may only consider children that are not part of the indexExp as a continuation of the feature path.
			return !featureChildren.stream().anyMatch(child -> !isPartOfTableAccessIndexExp(child, ta));
		}
		return featureChildren.isEmpty();
	}
	
	private static boolean isPartOfAnyTableAccessIndexExp(EObject obj) {
		if (obj instanceof TableAccess) return false; // getContainerOfType(obj, type) returns true if obj ist instance of type
		
		// check if obj has a parent that is TableAccess 
		var parentTableAccess = EcoreUtil2.getContainerOfType(obj, TableAccess.class);
		if (parentTableAccess == null) return false;
		
		return isPartOfTableAccessIndexExp(obj, parentTableAccess);
		/*
		// check if obj is single index exp of closest parent table access
		var indexExp = parentTableAccess.getIndexExp();
		if (indexExp == obj) return true;
		
		// check if obj is part of index exp of closest parent table access
		return parentTableAccess.getIndexExp().eContents().contains(obj);
		*/
	}
	
	private static boolean isPartOfTableAccessIndexExp(EObject obj, TableAccess ta) {
		var indexExp = ta.getIndexExp();
		// check if obj is single index exp of table access
		if (indexExp == obj) return true;
		// check if obj is part of index exp of table access
		return ta.getIndexExp().eContents().contains(obj);
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
		
		final var assignmentOpt = findParentAssignmentForAssignable(featurePathRoot);
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
	 * Returns the Assignment object this feature is contained in, if the feature is part
	 * of the lhs of an Assignment.
	 */
	public static Optional<Assignment> findParentAssignmentForAssignable(Feature feature) {
		var parent = feature.eContainer();
		
		if (parent == null) { // no parent
			return Optional.empty();
		}
		
		if (parent instanceof Assignment assignment) {
			return Optional.of(assignment);
		}
		
		if (parent instanceof Feature featureParent) {
			return findParentAssignmentForAssignable(featureParent);
		} 
		
		// object is on rhs (i.e. the parent is an ExpList) or not part of an assignment/feature path.
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
		if (exp instanceof ExpLiteral literal) {
			name = resolveExpLiteralToString(literal);
		} else if (exp instanceof Referencing ref) {
			if (ref.getRef() == null && !Config.TABLE_ACCESS_REFERENCES) {
				throw new RuntimeException("Attempting to resolve value expression, but a ref " + ref + " is not yet resolved!");
			} else {
				var assignedValue = tryGetAssignedValueFrom(ref);
				if (assignedValue != null) {
					if (assignedValue instanceof ExpLiteral literal) {
						name = resolveExpLiteralToString(literal);
					}
				}
			}
		}
		else {
			// TODO
			LOGGER.warn("TableAccess is not (yet) implemented for non-string indexExps like " + exp);
			//throw new RuntimeException("TableAccess is not (yet) implemented for non-string indexExps!");
		}
		if (Config.TABLE_ACCESS_REFERENCES) {
			if (name == null) {
				return DERIVED_DUMMY_NAME;
			}
		}

		return name;
	}
	
	private static String resolveExpLiteralToString(ExpLiteral expLiteral) {
		if (expLiteral instanceof ExpStringLiteral stringLiteral) {
			return stringLiteralToString(stringLiteral);
		}
		if (expLiteral instanceof ExpNumberLiteral numberLiteral) {
			// TODO: this could make use of the NumberValueConverter, but then the whole
			// expression resolution logic would need to be extracted to an injected bean
			// (in order to inject the valueConverterService).
			
			// we remove the "." separators from the double String representation to avoid
			// problems with the qualifiedNameConverter, which splits Strings on "."
			return Double.toString(numberLiteral.getValue());
		}
		throw new RuntimeException("Error while resolving ExpLiteral to String.");
	}
	
	public static Exp tryGetAssignedValueFrom(Referencing ref) {
		return tryGetAssignedValueFrom(ref, 0, 1000);
	}
	
	
	// TODO: may lead to StackOverflow, limit depth?
	private static Exp tryGetAssignedValueFrom(Referencing ref, int currDepth, final int maxDepth) {
		if (currDepth > maxDepth) {
			LOGGER.error("Reached max depth while attempting to get assigned value from " + ref);
			return null;
		}
		if (ref.getRef() == null || ref.getRef().eIsProxy()) {
			//System.out.println("ref's "+ ref + " ref null?: " + ref.getRef());
			return null;
		}
		if (ref.getRef().equals(ref)) { // reference to self means no value was assigned (i.e. the value is 'nil')
			//System.out.println("ref to self?: " + ref.getRef());
			return null;
		}
		if (ref.getRef() instanceof Referencing refsRef) {
			//System.out.println("get next ref: " + ref.getRef());
			return tryGetAssignedValueFrom(refsRef, ++currDepth, maxDepth);
			//return tryGetAssignedValueFrom(refsRef, 0, maxDepth);
		}
		//System.out.println("ref found: " + ref.getRef());
		if (ref.getRef() instanceof Exp exp) {
			return exp;
		}
		return null;
	}
	
	public static boolean isTableAccessWithDummyName(EObject o) {
		return o instanceof TableAccess ta && ta.getName().equals(DERIVED_DUMMY_NAME);
	}
	
	private static String stringLiteralToString(ExpStringLiteral stringLiteral) {
		//return removeQuotesFromString(stringLiteral.getValue());
		return stringLiteral.getValue();
	}
	
	public static String removeQuotesFromString(String str) {
		if (str != null && str.startsWith("\"") && str.endsWith("\"")) {
			return str.substring(1, str.length() - 1);
		}
		return str;
	}
	
	public static Optional<Stat> getParentStatement(EObject obj) {
		// since all PrefixExps extend Stat, we need to return the Stat from the Block, not
		// the direct parent of the object
		var parentBlock = EcoreUtil2.getContainerOfType(obj, Block.class);
		return EcoreUtil2.getAllContentsOfType(parentBlock, Stat.class).stream()
				.filter(stat -> EcoreUtil2.isAncestor(stat, obj))
				.findAny();
	}
	
	public static Collection<Referenceable> getReferenceablesFromStat(Stat stat, EObject context) {
		if (stat instanceof Assignment assignment) {
			return EcoreUtil2.getAllContentsOfType(assignment, Referenceable.class);
		} else if (stat instanceof BlockWrapperWithArgs bwwa && EcoreUtil2.isAncestor(bwwa, context)) {
			return getReferenceablesFromBlockWrapperWithArgs(bwwa);
		}
		else if (stat instanceof Referenceable ref) { // e.g. FunctionDeclaration
			return Collections.singletonList(ref);
		}
		return Collections.emptyList();
	}
	
	private static Collection<Referenceable> getReferenceablesFromBlockWrapperWithArgs(BlockWrapperWithArgs bwwa) {
		if (bwwa instanceof NumericFor numericFor) {
			return Collections.singletonList(numericFor.getArg());
		} else if (bwwa instanceof GenericFor genericFor) {
			return new ArrayList<Referenceable>(genericFor.getArgs().getArgs());
		}
		return Collections.emptyList();
	}
	
	
	
}
