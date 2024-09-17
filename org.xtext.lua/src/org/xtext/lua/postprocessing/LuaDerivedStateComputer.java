package org.xtext.lua.postprocessing;

import org.apache.log4j.Logger;
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
	private static final Logger LOGGER = Logger.getLogger(LuaXtext2EcorePostProcessor.class);
	
	@Inject 
	private LinkingHelper linkingHelper;
	
	@Override
	public void installDerivedState(DerivedStateAwareResource resource, boolean preLinkingPhase) {
		resource.getAllContents().forEachRemaining(obj -> {
			// TODO: should probably check if name is already set, e.g. goto-labels should already have a name
			//       given by the grammar
			// TODO: throw exception if a name could not be set, we expect that names can be set
			//LOGGER.info("Installing derived state...");
			
			if (obj instanceof Referenceable refble) {
				String linkText = refble.getName();
				if (linkText == null && refble instanceof Referencing referencing) {
					// TODO: could be extracted
					//System.out.println(refble);
					var refNode = NodeModelUtils.findNodesForFeature(referencing, Literals.REFERENCING__REF).get(0);
			    	linkText = linkingHelper.getCrossRefNodeAsString(refNode, true);
			    	
				}
				
				if (linkText == null) {
					throw new RuntimeException("Could not set value for 'name' attribute for obj " + obj + ".");
				}
		    	refble.setName(linkText);
			}
		});
	}

	@Override
	public void discardDerivedState(DerivedStateAwareResource resource) {
		LOGGER.warn("Discarding derived state, is this working correctly...?");
		resource.getAllContents().forEachRemaining(obj -> {
			if (obj instanceof Referenceable refble && obj instanceof Referencing) {
				refble.setName(null);
			}
		});
	}

}
