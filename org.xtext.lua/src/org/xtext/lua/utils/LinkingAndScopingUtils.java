package org.xtext.lua.utils;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.xtext.lua.lua.Assignment;
import org.xtext.lua.lua.ExpList;

public final class LinkingAndScopingUtils {
	
	// TODO:this should be documented clearer. Furthermore
	// ExpList appears in other contexts (e.g. GenericFor), we need to take this into account.
	public static boolean isOnLhsOfAssignment(EObject o) {
		var container = o.eContainer();
		
		if (container == null) { // no parent
			return false;
		}
		
		if (container instanceof ExpList) {
			return false;
		}
		
		if (container instanceof Assignment) {
			return true;
		}
		
		return isOnLhsOfAssignment(container);
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
