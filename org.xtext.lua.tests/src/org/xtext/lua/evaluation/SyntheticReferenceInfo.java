package org.xtext.lua.evaluation;

import org.xtext.lua.lua.Feature;

/**
 * POJO used to store evaluation data regarding synthetic references, i.e. references that could not be
 * resolved by the CMoGS resulting in a reference to a synthetic model element created through 
 * a recovery mechanism (currently trivial recovery).
 * @author jsaenz
 *
 */
public class SyntheticReferenceInfo {
	
	public enum Type {
		/**
		 * Unresolved Feature is a function call feature, the called function could not be resolved.
		 * This includes FunctionCall and MethodCall.
		 */
		FUNCTION_CALL,
		/**
		 * TableAccess indexExp could not be resolved, i.e. unresolved access Feature. 
		 * This includes TableAccess and MemberAccess
		 */
		TABLE_ACCESS, 
		/**
		 *  Root Feature could not be resolved. Includes GroupedExp and Var.
		 */
		ROOT_FEATURE,
		/**
		 * Should never occur, used only as fallback but implies some error in the computation.
		 */
		UNKNOWN 
	}
	
	public enum Cause {
		FUNCTION_RESOLUTION, // Function return values could not be resolved
		TABLE_INDEX_EXP, // TableAccess indexExp could not be resolved
		GROUPED_EXP, // feature path started with grouped exp
		IMPLICIT_IMPORT, // the implicit import files are not complete (e.g. fields are not defined in .lua files, see e.g. io.stderr)
		OTHER_IMPORT, // The import Uri was not correctly resolved, or references an external library
		ARG_ACCESS, // access of an Arg (e.g. MemberAccess on member in function func(arg) arg.member end)
		VAR_NOT_FOUND,
		UNIDENTIFIED, // could not identify the cause
		UNEXPECTED // fallback
	}
	
	/**
	 * The Feature this {@link SyntheticReferenceInfo} corresponds to, i.e. an unresolved Feature.
	 */
	private final Feature context;
	private final Type type;
	private final Cause cause;
	
	public SyntheticReferenceInfo(final Feature feature, final Type type, final Cause cause) {
		this.context = feature;
		this.type = type;
		this.cause = cause;
	}

	public Feature getContext() {
		return context;
	}

	public Type getType() {
		return type;
	}

	public Cause getCause() {
		return cause;
	}
	
	
}
