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
import org.xtext.lua.lua.Exp;
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
					// Effectively makes tableAccess Referenceable and Referencing
					setTableAccessNameAndRef(tableAccess);
				} else {
					// Efectively makes tableAccess Referenceable
					setTableAccessName(tableAccess);
				}
				
			}
			// set "name" attribute for all other Referenceables: 
			else if (obj instanceof Referenceable refble) {
				//setLinkTextAsName(refble);
				setNameAsRef(refble);
			}
			
			
		});

	}
	
	/**
	 * Sets the TableAccess' "name" attribute if it's indexExp can be resolved.
	 * @param tableAccess the TableAccess.
	 */
	private void setTableAccessName(TableAccess tableAccess) {
		final var name = LinkingAndScopingUtils.tryResolveExpressionToString(tableAccess.getIndexExp());
		tableAccess.setName(name);
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
	private void setNameAsRef(Referenceable refble) {
		String name = refble.getName();
		if (name == null && !(refble instanceof Exp)) {
			throw new RuntimeException("Attempting to create 'ref' cross-reference from 'name' attribute for " + refble + ", but name is null.");
		}
		
		if (refble instanceof Referencing referencing) {
			// TODO: The grammar should never produce Referenceables that already have their "ref" cross-reference set.
			//       => This check could be removed once the grammar is not changed anymore for some optimization.
			var refNodes = NodeModelUtils.findNodesForFeature(referencing, Literals.REFERENCING__REF);
			if (!refNodes.isEmpty()) {
				LOGGER.warn("Attempting to create 'ref' cross-reference from 'name' attribute for for " + refble + ", but ref node is not present.");
			}
			
			linkingSupport.createAndSetProxy(referencing, Literals.REFERENCING__REF, name);
		} else {
			//LOGGER.warn("Attempting to create 'ref' cross-reference from 'name' attribute for for " + refble + ", which is not Referencing.");
			return;
		}
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
			if (obj instanceof TableAccess tableAccess && Config.TABLE_ACCESS_REFERENCES) {
				tableAccess.setRef(null);
			}
		});
	}

}
