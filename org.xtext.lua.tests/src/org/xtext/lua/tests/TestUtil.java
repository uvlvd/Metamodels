package org.xtext.lua.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.Assertions;
import org.xtext.lua.lua.Chunk;
import org.xtext.lua.mocking.SyntheticVar;

/**
 * Util class for test cases.
 * @author jsaenz
 *
 */
public class TestUtil {
	private static final Logger LOGGER = Logger.getLogger(TestUtil.class);
	
	private TestUtil() { }
	
	/**
	 * Prints the given model to the console, if {@link TestConfig#PRINT_SNIPPET_MODELS} is true.
	 * @param model
	 */
	public static void printModelToConsole(EObject model) {
		if (TestConfig.PRINT_SNIPPET_MODELS) {
			System.out.println(dump(model, ""));
		}
	}
	
	public static void assertSerializedEqualsOriginal(final Chunk chunk, final String original, final Boolean expectedToFail) {
		if (!expectedToFail) {
			assertSerializedEqualsOriginal(chunk, original);
			return;
		}
		
		Assertions.assertNotNull(chunk);
		final var errors = chunk.eResource().getErrors();
		Assertions.assertFalse(errors.isEmpty());
	}	

	public static void assertSerializedEqualsOriginal(final Chunk chunk, final String original) {
		Assertions.assertNotNull(chunk);
		final var errors = chunk.eResource().getErrors();
		Assertions.assertTrue(errors.isEmpty(), "Unexpected errors: " + StringUtils.join(errors, ", "));
		parsedAndSerializedEqualsOriginal(chunk, original);
	}
	
	/**
	 * Resolves all references in the given chunk and tests that no unresolved references (i.e. proxies) or mocked references
	 * are present after the reference resolution.
	 * @param chunk
	 */
	public static void assertAllReferencesResolved(final Chunk chunk) {
		EcoreUtil.resolveAll(chunk);
		final var allCrossReferences = EcoreUtil.CrossReferencer.find(Collections.singleton(chunk));
		final var unresolvedCrossReferences = EcoreUtil.UnresolvedProxyCrossReferencer.find(chunk);
		final var mockedCrossReferences = allCrossReferences.keySet().stream()
				.filter(cr -> cr instanceof SyntheticVar)
				.toList();
		Assertions.assertTrue(unresolvedCrossReferences.isEmpty(), 
							  "Unexpected unresolved cross-references found:\n   " + unresolvedCrossReferences);
		Assertions.assertTrue(mockedCrossReferences.isEmpty(), "Unexpected mock-references found:\n  " + mockedCrossReferences);
	}
	
	/**
	 * Parses and serializes the given chunk, removes all comments and whitespace characters from the resulting
	 * serialized code and the given original, and compares the resulting strings.
	 * @param chunk the chunk.
	 * @param original the original code from which the chunk was parsed.
	 */
	private static void parsedAndSerializedEqualsOriginal(final Chunk chunk, final String original) {
		try (var outputStream = new ByteArrayOutputStream()){
			chunk.eResource().save(outputStream, new HashMap<>());
			final var parsedAndSerialized = outputStream.toString();
			compareNormalizedStrings(original, parsedAndSerialized);
		} catch (IOException e) {
			Assertions.fail("Unexpected IOException thrown during tests:\n  " + e.getLocalizedMessage());
		}
		
	}
	
	public static boolean compareNormalizedStrings(final String original, final String parsedAndSerialized) {
		final var s1 = PreprocessingUtils.removeCommentsAndWhiteSpacesAndNewLines(original);
		final var s2 = PreprocessingUtils.removeCommentsAndWhiteSpacesAndNewLines(parsedAndSerialized);
		final var result = s1.equals(s2);
		if (!result) {
			var diffStr = StringUtils.difference(s2, s1);
			diffStr = diffStr.substring(0, diffStr.length() < 101 ? diffStr.length() : 100);
			LOGGER.error("Original and parsed and serialized Strings differ!\n"
					+ "    Original: " + s1 + "\n"
					+ "    Parsed  : " + s2 + "\n"
					+ "    Diff at : " + diffStr);
		}
		
		return result;
	}

	/**
	 * Creates a String representation of the given EObject. Used to print the parsed code snippets for 
	 * inspection in testing.
	 */
	public static String dump(final EObject model, final String indent) {
		// use commented-out regexes if you wish to hide the eObject IDs in the output
		
	    //var res = indent + model.toString().replaceFirst (".*[.]impl[.](.*)Impl[^(]*", "$1 ");
	    var res = indent + model.toString().replaceFirst(".*[.]impl[.](.*)Impl[@](.*)[^(]*", "$1 $2");
	
	    for (final var a : model.eCrossReferences()) {
	        //res += " ->" + a.toString().replaceFirst(".*[.]impl[.](.*)Impl[^(]*", "$1 ");
	    	res += " ->" + a.toString().replaceFirst (".*[.]impl[.](.*)Impl[@](.*)[^(]*", "$1 $2");
	    }
	    res += "\n";
	    for (final var f : model.eContents()) {
	        res += dump(f, indent+"    ");
	    }
	    return res;
	}

}
