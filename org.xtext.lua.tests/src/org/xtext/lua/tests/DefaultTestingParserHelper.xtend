package org.xtext.lua.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.^extension.ExtendWith
import org.xtext.lua.lua.Chunk

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
		val result = parseAndPerformBaseTest(SUT, false)
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
		val result = parseHelper.parse(SUT)
		TestUtil.printModelToConsole(result)
		TestUtil.assertSerializedEqualsOriginal(result, SUT, expectedToFail)
		return result
	}
	
}