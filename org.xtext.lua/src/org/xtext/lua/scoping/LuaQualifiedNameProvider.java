package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.xtext.lua.lua.Expression_TableConstructor;
import org.xtext.lua.lua.MultiReferenceable;
import org.xtext.lua.lua.Referenceable;

public class LuaQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {
	@Override
	protected QualifiedName computeFullyQualifiedName(final EObject obj) {
		if ((obj instanceof Referenceable)) {
			var refble = (Referenceable) obj;

			// Table field use the qualified name calculation and resolve the parent
			// multirefble
			if (LuaUtil.isTableField(refble)) {
				var computedFQN = this.computeFullyQualifiedNameFromNameAttribute(obj);

				if (refble.eContainer() instanceof Expression_TableConstructor) {
					var tableConstructor = (Expression_TableConstructor) refble.eContainer();
					if (tableConstructor.eContainer() instanceof MultiReferenceable) {
						// find the refble that is associated with us in the parent multirefble
						var myRefble = LuaUtil.resolveValueToRef(tableConstructor);
						if (myRefble != null) {
							var myName = myRefble.getName() + "." + computedFQN.toString();
							return this.getConverter().toQualifiedName(myName);
						}
					}
				}
				return computedFQN;
			}

			// Othere refbles use simply their name
			var name = refble.getName();
			if (name != null) {
				return this.getConverter().toQualifiedName(name);
			}
		}
		return null;
	}
}
