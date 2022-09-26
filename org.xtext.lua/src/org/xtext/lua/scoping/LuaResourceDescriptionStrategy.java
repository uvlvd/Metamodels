package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.impl.DefaultResourceDescriptionStrategy;
import org.eclipse.xtext.util.IAcceptor;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Chunk;
import org.xtext.lua.lua.MultiReferenceable;
import org.xtext.lua.lua.Referenceable;

public class LuaResourceDescriptionStrategy extends DefaultResourceDescriptionStrategy {

	@Override
	/**
	 * returns true if children of the eObject should be traversed
	 */
	public boolean createEObjectDescriptions(EObject eObject, IAcceptor<IEObjectDescription> acceptor) {
		if (eObject instanceof Chunk) {
			// chunks always export
			return true;
		} else if (eObject instanceof Block && eObject.eContainer() instanceof Chunk) {
			// root block in a chunk
			return true;
		} else if (eObject instanceof MultiReferenceable) {
			var multiRefble = (MultiReferenceable) eObject;
			if (!multiRefble.isLocal()) {
				multiRefble.getRefbles().forEach(refble -> {
					var fqn = this.getQualifiedNameProvider().apply(refble);
					if (fqn != null) {
						acceptor.accept(EObjectDescription.create(fqn, refble));
					}
				});
			}
		} else if (eObject instanceof Referenceable) {
			var refble = (Referenceable) eObject;
			if (LuaUtil.isGlobalDeclaration(refble)) {
				var fqn = this.getQualifiedNameProvider().apply(refble);
				if (fqn != null) {
					acceptor.accept(EObjectDescription.create(fqn, refble));
				}
			}
		}
		return false;
	}

}
