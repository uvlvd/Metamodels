/*
 * generated by Xtext 2.34.0
 */
package org.xtext.lua.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import org.xtext.lua.lua.Chunk
import org.eclipse.emf.ecore.EObject
import java.io.ByteArrayOutputStream
import org.xtext.lua.PreprocessingUtils
import org.eclipse.xtext.resource.DerivedStateAwareResource
import org.eclipse.xtext.resource.IResourceServiceProvider
import org.eclipse.xtext.naming.IQualifiedNameConverter
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.testing.util.ResourceHelper

@ExtendWith(InjectionExtension)
@InjectWith(LuaInjectorProvider)
class LuaGlobalScopingTest {
	@Inject
	ParseHelper<Chunk> parseHelper
	
	@Inject 
	IResourceServiceProvider.Registry rspr
	
    @Inject 
    IQualifiedNameConverter converter
    
    @Inject
	ResourceHelper resourceHelper;
     
    def void printExportedObjects(Resource resource) {
        val resServiceProvider = rspr.getResourceServiceProvider(resource.URI)
        val manager = resServiceProvider.getResourceDescriptionManager()
        val description = manager.getResourceDescription(resource)
        for (eod : description.exportedObjects) {
            println(converter.toString(eod.qualifiedName) + ", " + eod.getEObjectOrProxy)
        }
    }
	
	
	def static String dump(EObject mod_, String indent) {
	    //var res = indent + mod_.toString.replaceFirst ('.*[.]impl[.](.*)Impl[^(]*', '$1 ')
	 	var res = indent + mod_.toString.replaceFirst ('.*[.]impl[.](.*)Impl[@](.*)[^(]*', '$1 $2')
	    for (a :mod_.eCrossReferences) 
	        //res += ' ->' + a.toString().replaceFirst ('.*[.]impl[.](.*)Impl[^(]*', '$1 ')
	        res += ' ->' + a.toString().replaceFirst ('.*[.]impl[.](.*)Impl[@](.*)[^(]*', '$1 $2')
	    res += "\n"
	    for (f :mod_.eContents) {
	        res += f.dump (indent+"    ")
	    }
	    return res
	}
	
	def void check(Chunk chunk, String original, Boolean expectedToFail) {
		if (!expectedToFail) {
			check(chunk, original)
			return
		}
		
		Assertions.assertNotNull(chunk)
		val errors = chunk.eResource.errors
		Assertions.assertFalse(errors.isEmpty)
	}	

	def void check(Chunk chunk, String original) {
		Assertions.assertNotNull(chunk)
		val errors = chunk.eResource.errors
		Assertions.assertTrue(errors.isEmpty, '''Unexpected errors: «errors.join(", ")»''')
		checkEquality(chunk, original)
	}
	
	def void checkEquality(Chunk chunk, String original) {
		val outputStream = new ByteArrayOutputStream()
		chunk.eResource.save(outputStream, #{})
		val parsedAndPrinted = outputStream.toString()
		
		val origExtremelyCanonical = removeComments(original)
		val parsedAndPrintedExtremelyCanonical = removeComments(parsedAndPrinted)

		val equivalence = origExtremelyCanonical.equals(parsedAndPrintedExtremelyCanonical)
		if (!equivalence) {
			System.out.println("===== Original: =====")
			System.out.println(original)
			System.out.println("===== Parsed and serialized: =====")
			System.out.println(parsedAndPrinted)
			
			System.out.println("===== Original canonical (extremely so): =====")
			System.out.println(origExtremelyCanonical)
			System.out.println("===== Parsed and serialized canonical: =====")
			System.out.println(parsedAndPrintedExtremelyCanonical)
		}
		Assertions.assertTrue(equivalence)
	}
	
	// TODO: rename, it replaces all whitespace also
	def String removeComments(String string) {
		val withoutComments = PreprocessingUtils.removeComments(string)
		return PreprocessingUtils.removeAllWhiteSpacesAndNewLines(withoutComments)	 
	}
	

	@Test
	def void globalScopingExportTest() { 
		val SUT = '''
			local function localFunc() end
			local localFunc2 = function() end
			local a, b, c = 1, 2, 3
			
			a, b["stringLiteral"] = "hello", "world"
			globalFunc = function() end
			function globalFunc2 () end
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
		printExportedObjects(result.eResource)
	}
	
	@Test
	def void globalScopingRequireTest() { 

		val SUT = '''
			function require(modname) end
		'''
		val result = parseHelper.parse(SUT)
		val resultUri = result.eResource.getURI
		System.out.println("resultUri " + resultUri)
		
		val rs = result.eResource.getResourceSet
		
		val SUT2 = 
		'require(\"' + resultUri + '\") '+
		'''
			local require = require
			local core = require("apisix.core")
			function globalFunc2 () end
		'''
		val result2 = parseHelper.parse(SUT2, rs)
		
		
		printExportedObjects(result.eResource)
		printExportedObjects(result2.eResource)
		System.out.println(dump(result2, ""));
		check(result2, SUT)
	}
	
}
