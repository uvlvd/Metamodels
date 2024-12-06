package org.xtext.lua.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.^extension.ExtendWith
import org.xtext.lua.lua.Chunk
import org.eclipse.emf.ecore.resource.ResourceSet

@ExtendWith(InjectionExtension)
@InjectWith(LuaInjectorProvider)
class DefaultTestingParserHelper {
	
	@Inject
	ParseHelper<Chunk> parseHelper
	
	/**
	 * Performs the same tests as {@link #parseAndPerformBaseTest} and additional basic scoping tests,
	 * i.e. testing for the absence of unresolved or mocked references.
	 */
	def Chunk parseAndPerformBaseScopingTest(String SUT) {
		return parseAndPerformBaseScopingTest(SUT, null)
	}
	
	/**
	 * Performs the same tests as {@link #parseAndPerformBaseTest} and additional basic scoping tests,
	 * i.e. testing for the absence of unresolved or mocked references. If a resourceSet is given,
	 * it is used to resolve external references.
	 */
	def Chunk parseAndPerformBaseScopingTest(String SUT, ResourceSet rs) {
		val result = parseAndPerformBaseTest(SUT, false, rs)
		TestUtil.assertAllReferencesResolved(result)
		return result
	}
	
	/**
	 * Parses the given SUT (snippet under test), tests if the parsed and serialized result
	 * equals the original SUT and returns the parsing result.
	 * 
	 * See {@link TestUtil#parsedAndSerializedEqualsOriginal} for details on the equality test.
	 */
	def Chunk parseAndPerformBaseTest(String SUT) {
		return parseAndPerformBaseTest(SUT, false)
	}
	
	/**
	 * Parses the given SUT (snippet under test), tests if the parsed and serialized result
	 * equals the original SUT and returns the parsing result.
	 * 
	 * See {@link TestUtil#parsedAndSerializedEqualsOriginal} for details on the equality test.
	 */
	def Chunk parseAndPerformBaseTest(String SUT, boolean expectedToFail) {
		// base test does not test scoping and thus never needs the resourceSet when called publicly
		return parseAndPerformBaseTest(SUT, expectedToFail, null)
	}
	
	/**
	 * Private helper function to hide resourceSet functionality.
	 */
	private def Chunk parseAndPerformBaseTest(String SUT, boolean expectedToFail, ResourceSet rs) {
		val result = parse(SUT, rs) // base test does not test scoping and thus never needs the resourceSet
		TestUtil.printModelToConsole(result)
		TestUtil.assertSerializedEqualsOriginal(result, SUT, expectedToFail)
		return result
	}
	
	/**
	 * Helper function to return a parsing result with or without resourceSet.
	 */
	private def parse(String SUT, ResourceSet rs) {
		if (rs !== null) {
			return parseHelper.parse(SUT, rs)
		}
		return parseHelper.parse(SUT)
	}
	
}