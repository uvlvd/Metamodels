package org.xtext.lua.postprocessing;

import org.apache.log4j.Logger;
import org.eclipse.xtext.linking.lazy.SyntheticLinkingSupport;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.resource.IDerivedStateComputer;
import org.xtext.lua.lua.Exp;
import org.xtext.lua.lua.Field;
import org.xtext.lua.lua.LuaPackage.Literals;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.utils.ExpUtil;
import org.xtext.lua.utils.FieldUtil;
import org.xtext.lua.utils.LuaConstants;

import com.google.inject.Inject;

public class LuaDerivedStateComputer implements IDerivedStateComputer {
	private static final Logger LOGGER = Logger.getLogger(LuaDerivedStateComputer.class);
	
	@Inject
	private SyntheticLinkingSupport linkingSupport;
	
	@Override
	public void installDerivedState(DerivedStateAwareResource resource, boolean preLinkingPhase) {
		resource.getAllContents().forEachRemaining(obj -> {
			// handle table access
			if (obj instanceof TableAccess tableAccess) {
				// Effectively makes tableAccess Referenceable and Referencing
				setTableAccessNameAndRef(tableAccess);
			} else if (obj instanceof Field field) {
				setFieldNameAndRef(field);
			}
			// set "name" attribute for all other Referenceables: 
			else if (obj instanceof Referenceable refble) {
				//setLinkTextAsName(refble);
				setNameAsRef(refble);
			}
		});

	}
	
	/**
	 * Sets the TableAccess' "name" attribute and cross-reference text if its index-expression can be
	 * resolved.
	 * @param tableAccess the TableAccess.
	 */
	private void setTableAccessNameAndRef(TableAccess tableAccess) {
		final var name = ExpUtil.tryResolveExpressionToString(tableAccess.getIndexExp(), LuaConstants.DERIVED_DUMMY_NAME);
		//set name attribute
		tableAccess.setName(name);
		//set cross-reference linkText 
		linkingSupport.createAndSetProxy(tableAccess, Literals.REFERENCING__REF, name);
	}
	
	/**
	 * Sets the Field's "name" attribute and the cross-reference linkText to this name (see {@link LinkingAndScopingUtils#setFieldNameAndRef(Field, String)}.) </br>
	 * @param field the Field.
	 */
	private void setFieldNameAndRef(Field field) {
		var name = FieldUtil.tryGetNameForField(field, LuaConstants.DERIVED_DUMMY_NAME);
		//set name attribute
		field.setName(name);
		//set cross-reference linkText  to name
		linkingSupport.createAndSetProxy(field, Literals.REFERENCING__REF, name);
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
			linkingSupport.createAndSetProxy(referencing, Literals.REFERENCING__REF, name);
		} else {
			//LOGGER.warn("Attempting to create 'ref' cross-reference from 'name' attribute for for " + refble + ", which is not Referencing.");
			return;
		}
	}
	
	@Override
	public void discardDerivedState(DerivedStateAwareResource resource) {
		LOGGER.warn("Discarding derived state, is this working correctly...?");
		resource.getAllContents().forEachRemaining(obj -> {
			// discard "name" attribute
			if (obj instanceof Referenceable refble && obj instanceof Referencing) {
				refble.setName(null);
			}
		});
	}

}
