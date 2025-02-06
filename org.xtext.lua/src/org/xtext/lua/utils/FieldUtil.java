package org.xtext.lua.utils;

import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.lua.Assignment;
import org.xtext.lua.lua.Exp;
import org.xtext.lua.lua.ExpField;
import org.xtext.lua.lua.ExpList;
import org.xtext.lua.lua.Field;
import org.xtext.lua.lua.FieldList;
import org.xtext.lua.lua.IndexExpField;
import org.xtext.lua.lua.LocalAssignment;
import org.xtext.lua.lua.TableConstructor;

public class FieldUtil {
	private static final Logger LOGGER = Logger.getLogger(FieldUtil.class);
	
	private FieldUtil() { }
	
	/**
	 * Attempts to calculate a table field's name, which depends on the field's type: </br>
	 * 	- NameField: "name" attribute  </br>
	 *  - IndexExpField: string representation of indexExp, if indexExp can be resolved, see {@link #tryResolveExpressionToString}</br>
	 *  - ExpField: name to corresponding index for ExpField in FieldList (starting from 1, counting only ExpField types in FieldList)</br>
	 * @param field the Field.
	 * @param fallback the fallback String returned if no name can be resolved.
	 * @return
	 */
	public static String tryGetNameForField(final Field field, final String fallback) {
		var name = field.getName(); // might be null
		if (field instanceof IndexExpField indexExpField) {
			name = ExpUtil.tryResolveExpressionToString(indexExpField.getIndexExp(), fallback);
		} else if (field instanceof ExpField expField) {
			var fieldList = field.eContainer();
			// only ExpField fields affect the counting: https://www.lua.org/manual/5.2/manual.html#3.4.8
			var expFields = EcoreUtil2.getAllContentsOfType(fieldList, ExpField.class);
			var index = expFields.indexOf(expField) + 1; // Lua indexes start with 1
			name = ExpUtil.tableKeyNumberToNameString(index);
		}
		
		if (name == null) {
			LOGGER.debug("Could not set field name for field " + field);
		}
		
		return name;
	}
	
	public static boolean isFieldWithLinkingDummyName(final Field field) {
		return LuaConstants.LINKING_DUMMY_NAME.equals(field.getName());
	}
	
	public static Optional<EObject> findTableForField(Field field) {
		var containingTableConstructor = EcoreUtil2.getContainerOfType(field, TableConstructor.class);
		if (containingTableConstructor.eContainer() instanceof Field table) {
			return Optional.of(table);
		}
		
		var assignmentOpt = getAssignmentForField(field, containingTableConstructor);
		if (assignmentOpt.isPresent()) {
			var assignment = assignmentOpt.get();
			if (assignment instanceof Assignment globalAssignment) {
				return findTableForFieldInGlobalAssignment(field, globalAssignment);
			}
			if (assignment instanceof LocalAssignment localAssignment) {
				return findTableForFieldInLocalAssignment(field, localAssignment);
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Gets the assignment Stat containing the field (global or local assignment).
	 * @param field the field.
	 * @param containingTableConstructor the TableConstructor containing the field.
	 * @return
	 */
	private static Optional<EObject> getAssignmentForField(Field field, TableConstructor containingTableConstructor) {
		// TableConstructor containing field is part of assignment
		if (containingTableConstructor.eContainer() instanceof ExpList expList) {
			if (expList.eContainer() instanceof Assignment assignment) {
				return Optional.of(assignment);
			}
			if (expList.eContainer() instanceof LocalAssignment localAssignment) {
				return Optional.of(localAssignment);
			}
		}
		return Optional.empty();
	}
	
	private static Optional<EObject> findTableForFieldInLocalAssignment(Field field, LocalAssignment localAssignment) {
		var vars = localAssignment.getVars().getNames();
		var expList = localAssignment.getExpList();
		if (expList == null) {
			return Optional.empty();
		}
		return findTableForField(field, vars, expList.getExps());
	}
	
	private static Optional<EObject> findTableForFieldInGlobalAssignment(Field field, Assignment assignment) {
		var vars = assignment.getVars();
		var exps = assignment.getExpList().getExps();
		return findTableForField(field, vars, exps);
	}
	
	private static Optional<EObject> findTableForField(Field field, List<? extends EObject> vars, List<Exp> exps) {
		var fieldList = (FieldList) field.eContainer();
		var tableConstructor = (TableConstructor) fieldList.eContainer();
		
		var index = exps.indexOf(tableConstructor);
		if (index < 0 || vars.size() < index) {
			return Optional.empty();
		} else {
			var table = vars.get(index);
			return Optional.of(table);
		}
	}
}
