package org.xtext.lua.scoping

import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider
import org.eclipse.xtext.naming.QualifiedName
import org.xtext.lua.lua.Referenceable

class LuaQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {
	override QualifiedName getFullyQualifiedName(EObject obj) {
	 	val superFQN =super.getFullyQualifiedName(obj)

		if (obj instanceof Referenceable) {
 			if (obj.entryValue === null) {
				 // Other referenceables get simple naming
				 println(superFQN+" => "+obj.name)
				 return converter.toQualifiedName(obj.name)
 			}
		}

		// Only table entries and non Referenceables get qualified naming!
//		println(superFQN)
		return superFQN
	}
}
