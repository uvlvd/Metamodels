package org.xtext.lua.utils;

import org.eclipse.emf.ecore.EObject;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.mocking.SyntheticVar;

public class MockUtil {
	
	public static boolean referencesMocked(final Referencing referencing) {
		return isMocked(referencing.getRef());
	}
	
	public static boolean isMocked(final EObject eobj) {
		return eobj instanceof Referenceable ref && isMocked(ref);
	}

	/**
	 * Returns true if the given {@link Referenceable} is mocked. The Lua CM is not able to
	 * resolve all references (e.g. because of the dynamic nature of some
	 * references) and will insert mocks for these references.
	 */
	public static boolean isMocked(final Referenceable ref) {
		return ref instanceof SyntheticVar;
	}
}
