package org.xtext.lua.postprocessing;

import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.resource.IDerivedStateComputer;
import org.xtext.lua.lua.Var;
import org.xtext.lua.lua.LuaPackage.Literals;
import org.xtext.lua.lua.MemberAccess;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.eclipse.xtext.linking.lazy.LazyLinker;
import com.google.inject.Inject;

public class LuaDerivedStateComputer implements IDerivedStateComputer {
	@Inject 
	private LinkingHelper linkingHelper;
	
	@Override
	public void installDerivedState(DerivedStateAwareResource resource, boolean preLinkingPhase) {
		resource.getAllContents().forEachRemaining(obj -> {
			if (obj instanceof Referenceable refble && obj instanceof Referencing) {
				// TODO: could be extracted
				var refNode = NodeModelUtils.findNodesForFeature(refble, Literals.REFERENCING__REF).get(0);
		    	var linkText = linkingHelper.getCrossRefNodeAsString(refNode, true);
		    	refble.setName(linkText);
			}
		});
	}

	@Override
	public void discardDerivedState(DerivedStateAwareResource resource) {
		resource.getAllContents().forEachRemaining(obj -> {
			if (obj instanceof Referenceable refble && obj instanceof Referencing) {
				refble.setName(null);
			}
		});
	}

}
