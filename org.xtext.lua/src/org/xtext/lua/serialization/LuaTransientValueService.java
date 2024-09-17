package org.xtext.lua.serialization;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.parsetree.reconstr.impl.DefaultTransientValueService;
import org.xtext.lua.lua.LuaPackage.Literals;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.TableAccess;

public class LuaTransientValueService extends DefaultTransientValueService {
	@Override
	public boolean isTransient(EObject owner, EStructuralFeature feature, int index) {
		// only EObjects with type Referencing should have derived "name" attributes which need to be ignored
		// on serialization.
		if (owner instanceof Referencing && feature == Literals.REFERENCEABLE__NAME)
			return true;
		
		// ignore TableAccess references since they are synthetically added via the DerivedStateComputer
		if (owner instanceof TableAccess && feature == Literals.REFERENCING__REF)
			return true;

		return super.isTransient(owner, feature, index);
	}
}
