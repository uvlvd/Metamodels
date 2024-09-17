package org.xtext.lua.postprocessing;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.GeneratedMetamodel;
import org.eclipse.xtext.xtext.ecoreInference.IXtext2EcorePostProcessor;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.LuaPackage.Literals;

//TODO: probably not needed anymore, name attributes are now declared in the grammar via super-type rule
// (i.e. null for Referencing objects, since those have a ref cross-reference instead of the name attrib)
// and set later via the derivedStateComputer.
public class LuaXtext2EcorePostProcessor implements IXtext2EcorePostProcessor {
	private static final Logger LOGGER = Logger.getLogger(LuaXtext2EcorePostProcessor.class);

	@Override
	public void process(GeneratedMetamodel metamodel) {
		process(metamodel.getEPackage());
	}

	/*
	 * We add the name attribute to all objects that should be referenceable here.
	 */
	private void process(EPackage p) {
		LOGGER.info("Starting post-processing to add 'name' attribute to Referenceables without 'name'.");
		var eClasses = p.getEClassifiers()
						.stream()
						.filter(classifier -> (classifier instanceof EClass))
						.map(classifier -> (EClass) classifier).toList();

		for (var clazz : eClasses) {
			var className = clazz.getName();

			// add "name" attribute to Referenceable (which is the superclass of any Referenceable via the grammar),
			// thus effectively adding the "name" attribute to any EObject extending Referenceable).
			// TODO: only set the "name" attribute if no "name" attribute exists, e.g. goto-labels should already have one
			if (clazz.getName().equals(Referenceable.class.getSimpleName())) {
				
				if (clazz.getEAllAttributes().stream().anyMatch(attr -> attr.getName().equals("name"))) {
					System.out.println("Has attr name: " + clazz.getName());
				}
				addTransientNameAttributeFor(clazz);
			}

			//TODO: not needed anymore since Referenceable covers all sub-types
			// add name attribute to all subclasses of Referenceable
			/*
			var maybeReferenceable = clazz.getEAllSuperTypes()
										  .stream()
										  .filter(cls -> cls.getName().equals(Referenceable.class.getSimpleName()))
										  .findAny();
			
			if (maybeReferenceable.isPresent()) {
				addTransientNameAttributeFor(clazz);
			}
			*/
		}
	}
	
	private void addTransientNameAttributeFor(EClass clazz) {
		/*
		 * TODO: It might be possible to set a reference here instead of the attribute
		 * and then resolve it with SyntheticLinkingSupport or smth. similar. This would
		 * probably be more efficient, since accessing the name attribute to set the
		 * cross-reference linkText as name in the DerivedStateComputer should be more efficient than finding the
		 * linkText in the reference (to set to the attribute).
		 * 
		 * var testReference = EcoreFactory.eINSTANCE.createEReference();
		 * testReference.setName("testRef"); testReference.setEType(Literals.EXP);
		 * testReference.setTransient(true); // ignore on serialization
		 * clazz.getEStructuralFeatures().add(testReference);
		 * 
		 * This would then need to be implemented for the Referencing superclass in this.process().
		 */
		
		
		
		LOGGER.info("Adding 'name' attribute to " + clazz.getName() + "...");
		
		var nameAttribute = EcoreFactory.eINSTANCE.createEAttribute();
		nameAttribute.setName("name");
		nameAttribute.setEType(EcorePackage.eINSTANCE.getEString());
		nameAttribute.setTransient(true); // ignore on serialization
		LOGGER.info("Added attribute: " + nameAttribute + ".");
		clazz.getEStructuralFeatures().add(nameAttribute);
	}

}
