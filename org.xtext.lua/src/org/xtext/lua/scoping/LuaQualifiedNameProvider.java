package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.xtext.lua.lua.Referenceable;

public class LuaQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {
	@Override
	protected QualifiedName computeFullyQualifiedName(final EObject obj) {
		if ((obj instanceof Referenceable refble)) {

			// Table field use qualified name calculation
			if (LuaUtil.isTableField((refble))) {
				return this.computeFullyQualifiedNameFromNameAttribute(obj);
			}

			// Othere refbles use simply their name
			return this.getConverter().toQualifiedName(refble.getName());
		}
		return null;
	}
}
