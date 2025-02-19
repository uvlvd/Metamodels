package org.xtext.lua.tests

import com.google.inject.Inject
import java.util.ArrayList
import java.util.List
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.naming.IQualifiedNameConverter
import org.eclipse.xtext.resource.IEObjectDescription
import org.eclipse.xtext.resource.IResourceServiceProvider
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import org.xtext.lua.lua.Assignment
import org.xtext.lua.lua.MemberAccess
import org.xtext.lua.lua.Var

/**
 * Tests for global scoping, i.e. resolution of functions and variables declared in another file and imported
 * with Lua's {@code require} function.
 * Since the Lua library functions (implicit imports) are only available when using the StandaloneSetup 
 * (see {@link org.xtext.lua.#LuaParser}), any library function used in the snippets under test (SUTs) needs
 * to be declared in the SUTs themselves (including the {@code require} function).
 */
@ExtendWith(InjectionExtension)
@InjectWith(LuaInjectorProvider)
class LuaGlobalScopingTest {
	@Inject
	DefaultTestingParserHelper parseHelper
	
	@Inject 
	IResourceServiceProvider.Registry rspr
	
    @Inject 
    IQualifiedNameConverter converter
    
	/**
	 * Tests that global definitions are always exported/externally visible.
	 */
	@Test
	def void globalExportTest() { 
		val SUT = '''
			local function localFunc() end
			local localFunc2 = function() end
			local a, b, c = 1, 2, 3
			
			a, b["stringLiteral"] = "hello", "world"
			globalFunc = function() end
			function globalFunc2 () end
		'''
		val result = parseHelper.parseAndPerformBaseScopingTest(SUT)
		val eods = getExportedObjectDescriptions(result.eResource)
		val expected = new ArrayList<String>();
		expected.add("globalFunc")
		expected.add("globalFunc2")
		for (eod : eods) {
			expected.remove(converter.toString(eod.qualifiedName))
		}
		Assertions.assertTrue(expected.isEmpty)
	}
	
	@Test
	def void requireTest() { 

		val PROVIDING_SUT = '''
			function require(modname) end
			function globalFunc1() end
		'''
		val providing = parseHelper.parseAndPerformBaseScopingTest(PROVIDING_SUT)
		val providingUri = providing.eResource.getURI	
		val rs = providing.eResource.getResourceSet
		
		val REQUIRING_SUT = 
		'require(\"' + providingUri + '\") '+
		'''
			globalFunc1()
		'''
		// The base tests are enough here, since globalFunc1 would be mocked/ could not be
		// resolved if not found in the global scope
		parseHelper.parseAndPerformBaseScopingTest(REQUIRING_SUT, rs)
	}
	
	@Test
	def void varDeclRequireTest() { 

		val PROVIDING_SUT = '''
			function require(modname) end -- need to declare this function because the lib registered are not imported in xtend tests
			local _M = {}
			_M.global = "global"
			return _M
		'''
		val providing = parseHelper.parseAndPerformBaseScopingTest(PROVIDING_SUT)
		val providingAssignment = providing.block.stats.get(2) as Assignment
		val expected = (providingAssignment.getVars().get(0)as Var).getSuffixExp() as MemberAccess
		val providingUri = providing.eResource.getURI	
		val rs = providing.eResource.getResourceSet
		
		val REQUIRING_SUT = 
		'import = require(\"' + providingUri + '\") ' +
		'''
			g = import.global
		'''
		val requiring = parseHelper.parseAndPerformBaseScopingTest(REQUIRING_SUT, rs)
		val assignment = requiring.block.stats.get(1) as Assignment
		val reffingGlobal = (assignment.getExpList().getExps().get(0) as Var).getSuffixExp() as MemberAccess
		Assertions.assertTrue(reffingGlobal.getRef() == expected)
	}
	
	@Test
	def void memberAccessRequireTest() { 

		val PROVIDING_SUT = '''
			function require(modname) end
			local _M = {}
			_M.global = {}
			_M.global.member = "member"
			return _M
		'''
		val providing = parseHelper.parseAndPerformBaseScopingTest(PROVIDING_SUT)
		val providingUri = providing.eResource.getURI	
		val rs = providing.eResource.getResourceSet
		
		val REQUIRING_SUT = 
		'import1 = require(\"' + providingUri + '\") ' +
		'import2 = require(\"' + providingUri + '\").global ' +
		'''
			f = import1.global.member
			g = import2.member
		'''
		// The base tests are enough here, since f or g would be mocked/ could not be
		// resolved if not correctly imported
		parseHelper.parseAndPerformBaseScopingTest(REQUIRING_SUT, rs)
	}
	
	@Test
	def void globalScopingRequireFunctionsTest() { 

		val PROVIDING_SUT = '''
			function require(modname) end
			local _M = {func3 = function() end}
			_M.func1 = function() end
			local function func2() end
			_M.func2 = func2
			return _M
		'''
		val result = parseHelper.parseAndPerformBaseScopingTest(PROVIDING_SUT)
		val resultUri = result.eResource.getURI	
		val rs = result.eResource.getResourceSet
		
		val REQUIRING_SUT = 
		'import = require(\"' + resultUri + '\") ' +
		'''
			import.func1()
			import.func2()
			import.func3()
		'''
		parseHelper.parseAndPerformBaseScopingTest(REQUIRING_SUT, rs)
	}
	
	@Test
	def void tempTest() { 

		val PROVIDING_SUT = '''
			local _M = {}
			
			local function external_service_D()
			    print("external_service_D")
			end
			
			_M.external_service_D = external_service_D
			
			return _M
		'''
		val result = parseHelper.parseAndPerformBaseScopingTest(PROVIDING_SUT)
		val resultUri = result.eResource.getURI	
		val rs = result.eResource.getResourceSet
		
		val REQUIRING_SUT = 
		'local external_service = require(\"' + resultUri + '\") ' + '.external_service_D' +
		'''
			
			
			
			local use_external_service = function()
			    local i = 1
			    while i < 10 do
			        external_service()
			        i = i + 1
			    end
			end
			
			use_external_service()
		'''
		parseHelper.parseAndPerformBaseScopingTest(REQUIRING_SUT, rs)
	}
	
	@Test
	def void functionDeclarationTest() { 

		val PROVIDING_SUT = '''
			local _M = {}
			
			function _M.external_function() 
			end

			
			return _M
		'''
		val result = parseHelper.parseAndPerformBaseScopingTest(PROVIDING_SUT)
		val resultUri = result.eResource.getURI	
		val rs = result.eResource.getResourceSet
		
		val REQUIRING_SUT = 
		'local external_service = require(\"' + resultUri + '\") ' +
		'''
			local function temp()
				external_service.external_function()
			end
			
		'''
		parseHelper.parseAndPerformBaseScopingTest(REQUIRING_SUT, rs)
	}
	    
    /**
     * Utility debug function to print the exported objects.
     */
    private def void printExportedObjects(Resource resource) {
        val objects = getExportedObjectDescriptions(resource)
        for (eod : objects) {
            println(converter.toString(eod.qualifiedName) + ", " + eod.getEObjectOrProxy)
        }
    }
    
    private def List<IEObjectDescription> getExportedObjectDescriptions(Resource resource) {
        val resServiceProvider = rspr.getResourceServiceProvider(resource.URI)
        val manager = resServiceProvider.getResourceDescriptionManager()
        val description = manager.getResourceDescription(resource)
        
        val result = new ArrayList<IEObjectDescription>();
        for (eod : description.exportedObjects) {
            result.add(eod)
        }
        return result
    }
}
