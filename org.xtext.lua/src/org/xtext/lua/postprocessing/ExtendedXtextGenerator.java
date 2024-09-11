package org.xtext.lua.postprocessing;

import org.eclipse.xtext.XtextRuntimeModule;
import org.eclipse.xtext.XtextStandaloneSetup;
import org.eclipse.xtext.xtext.generator.XtextGenerator;
import org.xtext.lua.LuaRuntimeModule;
import org.xtext.lua.LuaStandaloneSetup;
import org.eclipse.xtext.xtext.ecoreInference.IXtext2EcorePostProcessor;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Used to inject the custom IXtext2EcorePostProcessor to add attributes that cannot be 
 * defined in the grammar to some EObjects.
 */
//@SuppressWarnings("restriction")
public class ExtendedXtextGenerator extends XtextGenerator {
  
	  public ExtendedXtextGenerator() {
	        new XtextStandaloneSetup() {
	            @Override
	            public Injector createInjector() {
	                return Guice.createInjector(new XtextRuntimeModule() {  	
	                    @SuppressWarnings("unused")
						public Class<? extends IXtext2EcorePostProcessor> bindEcorePostProcessor() {
	                        return LuaXtext2EcorePostProcessor.class;
	                    }
	                });
	            }
	        }.createInjectorAndDoEMFRegistration();
	    }
	  
}