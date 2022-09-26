package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.lua.ContainsBlock;
import org.xtext.lua.lua.Expression;
import org.xtext.lua.lua.MultiReferenceable;
import org.xtext.lua.lua.Referenceable;

public class LuaUtil {
	public static boolean isTableField(final Referenceable refble) {
		Expression _entryValue = refble.getEntryValue();
		return (_entryValue != null);
	}

	public static boolean isDeclaration(final Referenceable refble) {
		boolean _isTableField = LuaUtil.isTableField(refble);
		return (!_isTableField);
	}

	/**
	 * Local Declarations are: - local variables - local functions - arguments of
	 * function- and for-blocks
	 */
	public static boolean isLocalDeclaration(final Referenceable refble) {
		final var parent = refble.eContainer();
		// arguments of for- or function-block
		if (parent instanceof ContainsBlock)
			return true;

		// refble inside a multirefble
		if (parent instanceof MultiReferenceable) {
			return ((MultiReferenceable) parent).isLocal();
		}

		return false;
	}

	public static boolean isGlobalDeclaration(final Referenceable refble) {
		return isDeclaration(refble) && !isLocalDeclaration(refble);
	}

	public static Referenceable getContainingDeclaration(final EObject obj) {
		Referenceable _xblockexpression = null;
		{
			Referenceable parent = EcoreUtil2.<Referenceable>getContainerOfType(obj, Referenceable.class);
			while (((parent != null) && (!LuaUtil.isDeclaration(parent)))) {
				EcoreUtil2.<Referenceable>getContainerOfType(obj, Referenceable.class);
			}
			_xblockexpression = parent;
		}
		return _xblockexpression;
	}

	/*
	 * This method can be used to resolve a value expression to its corresponding
	 * Referenceable in an assignment
	 * 
	 * ref1, ref2 = val1, val2
	 * 
	 * This method can be used to e.g. resolve val2 to ref2
	 */
	public static Referenceable resolveValueToRef(Expression expression) {
		if (expression.eContainer() instanceof MultiReferenceable) {
			var multiRefble = (MultiReferenceable) expression.eContainer();

			if (multiRefble != null) {
				var index = multiRefble.getValues().indexOf(expression);
				if (index < 0)
					return null; // expression did not exist

				return multiRefble.getRefbles().get(index);
			}
		}
		return null;
	}

	/*
	 * This method can be used to resolve a Referenceable to its corresponding value
	 * expression in an assignment
	 * 
	 * Having a MultiRefenceable like ref1, ref2 = val1, val2
	 * 
	 * This method can be used to e.g. resolve ref1 to val1
	 */
	public static Expression resolveRefToValue(Referenceable refble) {
		if (refble.eContainer() instanceof MultiReferenceable) {
			var multiRefble = (MultiReferenceable) refble.eContainer();

			if (multiRefble != null) {
				var index = multiRefble.getRefbles().indexOf(refble);
				if (index < 0)
					return null; // expression did not exist

				return multiRefble.getValues().get(index);
			}
		}
		return null;
	}
}
