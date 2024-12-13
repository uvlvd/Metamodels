package org.xtext.lua.mocking;

import org.eclipse.emf.ecore.EObject;

public interface IMockObjectCreator {
	EObject createMockObjectFor(EObject context);
}
