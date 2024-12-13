package org.xtext.lua.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.lua.Assignment;
import org.xtext.lua.lua.Exp;
import org.xtext.lua.lua.ExpList;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.Field;
import org.xtext.lua.lua.LocalAssignment;
import org.xtext.lua.lua.LocalVar;
import org.xtext.lua.lua.NamedFeature;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.lua.Var;

public class AssignmentUtil {
	private static final Logger LOGGER = Logger.getLogger(AssignmentUtil.class);
	
	private AssignmentUtil() { }
	
	/**
	 * Returns true if the given object get assigned a value, i.e. is a local variable 
	 * or leaf in a feature path on the left-hand side of a global assignment. </br>
	 * @param obj the object.
	 */
	public static boolean isAssignable(EObject obj) {
		if (isLocalAssignable(obj)) {
			return true;
		}
		return isGlobalAssignable(obj);
	}
	
	/**
	 * Returns whether the object is a variable in a local assignment.
	 */
	private static boolean isLocalAssignable(EObject obj) {
		return obj instanceof LocalVar;
	}
	
	/**
	 * Returns true if the given object is part of the lhs of a global assignment, i.e. gets assigned a value. </br>
	 * Only leafs of a feature path (a path described by the PrefixExp grammar rule) can be assignables. </br>
	 * E.g. a.member = 10 returns true for member, false for a.
	 * @param obj the object.
	 */
	private static boolean isGlobalAssignable(EObject obj) {
		// only features that are Referenceable can be assignables (i.e. Vars, MemberAccess, TableAccess, ... but not FunctionCalls etc.)
		if (obj instanceof NamedFeature namedFeature) {
			// a Var that is part of a TableAccess cannot be an Assignable (e.g. the var in table[var])
			if (isPartOfAnyTableAccessIndexExp(namedFeature)) {
				return false;
			}
					
			if (FeatureUtil.isNamedLeafOfFeaturePath(namedFeature)) { // only leafs of a feature path are assignable
				// check if an Assignment is parent and leaf is on lhs
				return findParentAssignmentForAssignable(namedFeature).isPresent();
			}
		}
		return false;
	}
	
	private static boolean isPartOfAnyTableAccessIndexExp(EObject obj) {
		if (obj instanceof TableAccess) return false; // getContainerOfType(obj, type) returns true if obj is instance of type
		
		// check if obj has a parent that is TableAccess 
		var parentTableAccess = EcoreUtil2.getContainerOfType(obj, TableAccess.class);
		if (parentTableAccess == null) return false;
		
		return isPartOfTableAccessIndexExp(obj, parentTableAccess);
	}
	
	private static boolean isPartOfTableAccessIndexExp(EObject obj, TableAccess ta) {
		var indexExp = ta.getIndexExp();
		// check if obj is single index exp of table access
		if (indexExp == obj) return true;
		// check if obj is part of index exp of table access
		return ta.getIndexExp().eContents().contains(obj);
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
	
	/**
	 * Attempts to find the referenced {@link Exp} by traversing the reference chain, i.e. repeatedly accessing 
	 * {@link Referencing#getRef} until an {@link Exp}.
	 * @param ref the Referencing
	 * @return the Exp, or null if none is found.
	 */
	public static Exp tryGetReferencedExp(Referencing ref) {
		return tryGetReferencedExp(ref, 0, 1000);
	}
	
	// TODO: may lead to StackOverflow, limit depth?
	private static Exp tryGetReferencedExp(Referencing ref, int currDepth, final int maxDepth) {
		if (currDepth > maxDepth) {
			LOGGER.error("Reached max depth while attempting to get assigned value from " + ref);
			return null;
		}
		if (ref.getRef() == null || ref.getRef().eIsProxy()) {
			return null;
		}
		// TODO: this can probably be removed, since now synthetic NIL exps are assigned 
//		if (ref.getRef().equals(ref)) { // reference to self means no value was assigned (i.e. the value is 'nil')
//			return null;
//		}
		if (ref.getRef() instanceof Referencing refsRef) {
			return tryGetReferencedExp(refsRef, ++currDepth, maxDepth);
		}
		if (ref.getRef() instanceof Exp exp) {
			return exp;
		}
		// TODO: could we just stop when getRef() does not return a Referencing? That would be the end of the reference chain
		return null;
	}
	
	/**
	 * Returns the expression assigned to this feature. Use {@link #isAssignable(EObject)} to ensure
	 * the given feature is an Assignable before calling this function.
	 * @param feature the Feature (must be a Referenceable).
	 * @return the value expression assigned to the feature.
	 */
	public static Exp findAssignedExp(EObject obj) {
		if (!isAssignable(obj)) {
			throw new RuntimeException("Cannot find assigned expression for non-assignable, did you forget to call isAssignable()?");
		}
		
		if (isLocalAssignable(obj)) {
			return findAssignedExpForLocalAssignment((LocalVar) obj);
		}
		
		if (obj instanceof NamedFeature namedFeature) {
			return findAssignedExpForFeature(namedFeature);
		}
		
		throw new RuntimeException("Could not find assigned exp for " + obj);

	}
	
	private static Exp findAssignedExpForLocalAssignment(final LocalVar var) {
		// should never be null
		final var localAssignment = EcoreUtil2.getContainerOfType(var, LocalAssignment.class);
		final var varIndex = localAssignment.getVars().getNames().indexOf(var);
		if (varIndex < 0) // should never happen
			throw new RuntimeException("Could not find local variable in assignment!");
	
		if (localAssignment.getExpList() == null) 
			return null;
		
		final var exps = localAssignment.getExpList().getExps();
        if (exps.size() > varIndex)
        	return localAssignment.getExpList().getExps().get(varIndex);
        
     // fall-through, e.g. if explist does not contain an exp for every declared var (i.e. value is 'nil')
		return null;
	}
	
	private static Exp findAssignedExpForFeature(final Feature feature) {
		final var featurePathRootOpt = FeatureUtil.findFeaturePathRootAsVar(feature);
		if (featurePathRootOpt.isEmpty()) {
			return null;
		}
		final var featurePathRoot = featurePathRootOpt.get();
		
		final var assignmentOpt = findParentAssignmentForAssignable(featurePathRoot);
		if (assignmentOpt.isPresent()) {
			final var assignment = assignmentOpt.get();
			// TODO: need to know root of feature path for refble to find it in vars
            final var index = assignment.getVars().indexOf(featurePathRoot);
            if (index < 0) // should never happen
            	throw new RuntimeException("Could not find feature path root (Variable) in assignment vars!");

            final var exps = assignment.getExpList().getExps();
            if (exps.size() > index)
            	return assignment.getExpList().getExps().get(index);
		}
		// fall-through, e.g. if explist does not contain an exp for every declared var (i.e. value is 'nil')
		return null;
	}
	
	public static Optional<? extends Referenceable> findAssignableForExp(final Exp exp) {
		// handle expressions assigned to fields
		final var expContainer = exp.eContainer();
		if (expContainer instanceof Field field) {
			// TODO: cases: IndexExpField, NameField, ExpField
			return Optional.of(field);
		}
		// handle Assignment
		if (expContainer instanceof ExpList expList) {
			final var expListContainer = expList.eContainer();
			final var expIndex = expList.getExps().indexOf(exp);
			if (expIndex == -1) throw new RuntimeException("Could not find Exp " + exp + " in its parent ExpList " + expList +"!");
			
			if (expListContainer instanceof Assignment assignment) {
				final var assignmentFeatureRoot = assignment.getVars().get(expIndex);
				if (assignmentFeatureRoot instanceof Var featureRoot) {
					return FeatureUtil.findFeaturePathNamedLeaf(featureRoot);
				}
			}
			
			if (expListContainer instanceof LocalAssignment localAssignment) {
				final var vars = localAssignment.getVars().getNames();
				return Optional.of(vars.get(expIndex));
			}
		}
		LOGGER.warn("Cannot find Assignable for exp " + exp + " with container " + expContainer +" and container's container " + expContainer.eContainer() + "!");
		return Optional.empty();
	}
	
	// TODO: with how isAssignable() is implemented, this is probably not very
	// efficient
	// TODO: check if other parts of the program need a
	// "getAssignablesFromAssignment" functionality and use this method
	public static List<? extends Referenceable> getAssignablesFromAssignment(Assignment assignment) {
		return assignment.getVars().stream()
			.flatMap(var -> {
				ArrayList<Referenceable> result = new ArrayList<>();
				result.add(var);
				result.addAll(EcoreUtil2.getAllContentsOfType(var, Referenceable.class));
				return result.stream();
			})
			.filter(referenceable -> isAssignable(referenceable))
			.toList();
	}
	
}
