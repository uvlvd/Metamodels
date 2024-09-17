package org.xtext.lua.utils;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.lua.Assignment;
import org.xtext.lua.lua.Exp;
import org.xtext.lua.lua.ExpList;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Var;

public final class LinkingAndScopingUtils {
	
	
	//TODO: isAssignable function (replaces isOnLhsOfAssignment): refble needs to be leaf
	//      of Featue path
	
	public static boolean isAssignable(EObject obj) {
		if (obj instanceof Feature feature) {
			final var isLeaf = !feature.eContents().stream().anyMatch(child -> child instanceof Feature);
			if (isLeaf) { // only leafs of a feature path are assignable
				return findAssignmentParent(feature).isPresent();
			}
		}
		return false;
	}
	
	public static Exp findAssignedExp(Feature feature) {
		final var featurePathRootOpt = findFeaturePathRoot(feature);
		if (featurePathRootOpt.isEmpty()) {
			return null;
		}
		final var featurePathRoot = featurePathRootOpt.get();
		
		final var assignmentOpt = findAssignmentParent(featurePathRoot);
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
	
	private static Optional<Assignment> findAssignmentParent(Feature feature) {
		var parent = feature.eContainer();
		
		if (parent == null) { // no parent
			return Optional.empty();
		}
		
		if (parent instanceof Assignment assignment) {
			return Optional.of(assignment);
		}
		
		if (parent instanceof Feature featureParent) {
			return findAssignmentParent(featureParent);
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
	
	
	/**
	 * Checks if the given object's class contains a method called "getRef".
	 * @param o the object.
	 * @return true, if a method "getRef" was found in the object's class.
	 */
	public static boolean hasRef(final Object o) {
		return Stream.of(o.getClass().getMethods())
					 .map(Method::getName)
					 .anyMatch(name -> name.equals("getName"));
	}
	
	/*
	public static List<? extends Refble> getAssignmentVarsFilteredByContext(final EObject context, final Assignment assignment) {
		var result = new ArrayList<Refble>();
		
		if (context instanceof VarMemberAccess ctxMemberAccess) {
			System.out.println("=== start " + ctxMemberAccess.getName());
			for (var v : assignment.getVars()) {
				if (v instanceof VarMemberAccess memberAccess) {
					System.out.println(v);
					System.out.println(ctxMemberAccess.eContents());
				}
			}
			System.out.println("=== end");
		}

		
		//return result;
		return assignment.getVars();
	}
	
	private boolean checkChildren(final EObject context, final Referenceable v) {
		var x = context.eAllContents();
	}
	*/
	
	/*
	//TODO: refactor, if possible, to not work with strings
	public static List<Refble> getArgs(final BlockWrapperWithArgs wrapper) {
		//System.out.println("foo");
		//System.out.println(wrapper.getArgs());
		var args = new ArrayList<Refble>();
		
		// handle single argument wrapper, e.g. numeric for
		if (wrapper.getArg() != null) { 
			args.add(wrapper.getArg());
		}
		
		// handle multi-argument wrapper, e.g. generic for
		var nameList = wrapper.getArgs();
		if (nameList != null) {
			args.addAll(nameList.getNames());
		}

		return args;
	}
	*/
	
	
}
