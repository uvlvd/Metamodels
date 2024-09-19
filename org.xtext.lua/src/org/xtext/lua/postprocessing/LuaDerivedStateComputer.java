package org.xtext.lua.postprocessing;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.linking.ILinkingService;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.resource.IDerivedStateComputer;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.xtext.lua.lua.Var;
import org.xtext.lua.utils.LinkingAndScopingUtils;
import org.xtext.lua.Config;
import org.xtext.lua.lua.ExpStringLiteral;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.LuaPackage.Literals;
import org.xtext.lua.lua.MemberAccess;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.TableAccess;
import org.eclipse.xtext.linking.lazy.LazyLinker;
import org.eclipse.xtext.linking.lazy.SyntheticLinkingSupport;
import org.eclipse.xtext.naming.IQualifiedNameProvider;

import com.google.inject.Inject;

public class LuaDerivedStateComputer implements IDerivedStateComputer {
	private static final Logger LOGGER = Logger.getLogger(LuaXtext2EcorePostProcessor.class);
	
	@Inject 
	private LinkingHelper linkingHelper;
	
	@Inject
	private SyntheticLinkingSupport linkingSupport;
	
	@Inject
	private ILinkingService linkingService;
	
	@Override
	public void installDerivedState(DerivedStateAwareResource resource, boolean preLinkingPhase) {		
		resource.getAllContents().forEachRemaining(obj -> {
			
			// handle table access
			if (obj instanceof TableAccess tableAccess) {
				if (Config.TABLE_ACCESS_REFERENCES) {
					// TODO: Cannot compute name for tableAccess at this point, since the other references need to be
					// resolved first
					setTableAccessNameAndRef(tableAccess);
				}
			}
			// set "name" attribute for all other Referenceables: 
			else if (obj instanceof Referenceable refble) {
				setLinkTextAsName(refble);
			}
			
			
		});

	}
	
	/**
	 * Sets the TableAccess' "name" attribute and cross-reference text if its index-expression can be
	 * resolved.
	 * @param tableAccess the TableAccess.
	 */
	private void setTableAccessNameAndRef(TableAccess tableAccess) {
		final var name = LinkingAndScopingUtils.tryResolveExpressionToString(tableAccess.getIndexExp());
		//set name attribute
		tableAccess.setName(name);
		//set cross-reference linkText 
		linkingSupport.createAndSetProxy(tableAccess, Literals.REFERENCING__REF, name);
	}
	
	/**
	 * Sets the "name" attribute of the given Referenceable to the link text from its cross-reference
	 * if the "name" attribute is null. </br>
	 * This assumes that the cross-reference is not null.
	 * @param refble the Referenceable.
	 */
	private void setLinkTextAsName(Referenceable refble) {
		String linkText = refble.getName();
		if (linkText == null && refble instanceof Referencing referencing) {
			var refNodes = NodeModelUtils.findNodesForFeature(referencing, Literals.REFERENCING__REF);
			if (refNodes.isEmpty()) {
				refble.setName(null);
				LOGGER.warn("Attempting to create 'name' attribute from ref node, but ref node is not present.");
				return;
				//throw new RuntimeException("Attempting to create 'name' attribute from ref node, but ref node is not present.");
			}
			var refNode = refNodes.get(0);
		    linkText = linkingHelper.getCrossRefNodeAsString(refNode, true);
		}
		
		if (linkText == null) {
			throw new RuntimeException("Could not set value for 'name' attribute for Referenceable " + refble + ".");
		}
		
    	refble.setName(linkText);
	}

	@Override
	public void discardDerivedState(DerivedStateAwareResource resource) {
		LOGGER.warn("Discarding derived state, is this working correctly...?");
		resource.getAllContents().forEachRemaining(obj -> {
			// discard "name" attribute
			if (obj instanceof Referenceable refble && obj instanceof Referencing) {
				refble.setName(null);
			}
			// discard synthetic reference from TableAccesses
			if (obj instanceof TableAccess tableAccess) {
				tableAccess.setRef(null);
			}
		});
	}

}
