package org.xtext.lua.linking;

import org.eclipse.emf.ecore.EObject;

public interface IMockObjectCreator {
	EObject createMockObjectFor(EObject context);
}
