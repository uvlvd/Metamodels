package org.xtext.lua.postprocessing;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.resource.IDerivedStateComputer;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.xtext.lua.lua.Var;
import org.xtext.lua.utils.LinkingAndScopingUtils;
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
	
	@Override
	public void installDerivedState(DerivedStateAwareResource resource, boolean preLinkingPhase) {
		resource.getAllContents().forEachRemaining(obj -> {
			// handle table accesses
			if (obj instanceof TableAccess tableAccess) {
				// TODO: handle both setps in one function
				setTableAccessName(tableAccess);
				//set cross-reference linkText 
				linkingSupport.createAndSetProxy(tableAccess, Literals.REFERENCING__REF, createTableAccessName(tableAccess));
			} 
			// handle other referenceables: 
			else if (obj instanceof Referenceable refble) {
				setLinkTextAsName(refble);
			}
			
			// This does not work as intended, the serialization breaks if the reference is set heres
			// Maybe if we do it similar to the tableAccess, using the linkingSupport to populate the proxy unsing a 
			// UUID or some such as name for Values?
			if (false) {
				if (obj instanceof Feature feature) {
					//TODO: set the rhs value as reference for lhs assignments
					// only features that are Referencing can be assignables
					if (LinkingAndScopingUtils.isAssignable(feature) && feature instanceof Referencing referencing) {
						final var value = LinkingAndScopingUtils.findAssignedExp(feature);
						
						if (value == null) {
							// TODO: create transient "nil" eObject
						} else {
							referencing.setRef(value);
						}
					}
				}
			}
			

		});
	}
	
	private void setTableAccessName(TableAccess tableAccess) {		
		tableAccess.setName(createTableAccessName(tableAccess));
	}
	
	private String createTableAccessName(TableAccess tableAccess) {
		final var indexExp = tableAccess.getIndexExp();
		
		String name = null;
		// A tableAccess tab["member"] gets the same name as tab.member
		if (indexExp instanceof ExpStringLiteral stringLiteral) {
			name = stringLiteral.getValue();
			name = name.substring(1, name.length()-1); // remove leading and trailing '"'
		} else {
			// TODO
			throw new RuntimeException("TableAccess is not (yet) implemented for non-string indexExps!");
		}
		
		return name;
	}
	
	/**
	 * Sets the "name" attribute of the given Referenceable to the link text from its cross-reference
	 * if the "name" attribute is null.
	 * @param refble the Referenceable.
	 */
	private void setLinkTextAsName(Referenceable refble) {
		String linkText = refble.getName();
		if (linkText == null && refble instanceof Referencing referencing) {
			// use cross-reference linkText as name if available (e.g. for MemberAccess, Var)
			//if (referencing.getRef() != null) {
				var refNode = NodeModelUtils.findNodesForFeature(referencing, Literals.REFERENCING__REF).get(0);
		    	linkText = linkingHelper.getCrossRefNodeAsString(refNode, true);
			//}
			// handle table access names
			//if (referencing instanceof TableAccess tableAccess) {
				//TODO
			//	linkText = UUID.randomUUID().toString();
			//}
			
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
			// discard references to assignment values
			/*
			if (obj instanceof Feature feature) {
				if (LinkingAndScopingUtils.isAssignable(feature) && feature instanceof Referencing referencing) {
					final var value = LinkingAndScopingUtils.findAssignedExp(feature);
					
					//if (value == null) {
						// TODO: create transient "nil" eObject
					//} else {
					referencing.setRef(referencing);
					//}
				}
			}
			*/
		});
	}

}
