package org.xtext.lua.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xtext.lua.LuaParser;
import org.xtext.lua.linking.SyntheticVar;


public class LuaParserTest {
	private static final Logger LOGGER = Logger.getLogger(LuaParserTest.class);
	
	// TODO: remove
	@Test
	public void testMinimalParseTest() throws IOException {
		final var apisix = "D:\\MA\\apisix\\apisix";
		final var lua_test_suite_51 = "D:\\MA\\lua5.1-tests";
		final var lua_test_suite_52 = "D:\\MA\\lua-5.2.0-tests";
		
		// neovim plugins
		final var nvimDbee = "D:\\MA\\repos\\nvim_plugins\\nvim-dbee"; // has lots of go code
		final var mason = "D:\\MA\\repos\\nvim_plugins\\mason.nvim"; // at least Lua 5.3
		final var lualine = "D:\\MA\\repos\\nvim_plugins\\lualine.nvim";
		final var telescope = "D:\\MA\\repos\\nvim_plugins\\telescope.nvim";
		final var trouble = "D:\\MA\\repos\\nvim_plugins\\trouble.nvim";
		
		final var temp_testfolder = "D:\\MA\\repos\\temp";
		
		var resourceSet = new LuaParser().parse(Paths.get(lua_test_suite_52));
		
		//printNumberOfModelElements(resourceSet);
	}
	
	private static final String EVAL_FOLDER_PATH = "evaluation_results\\";
	
	/**
	 * Test used for the evaluation of the Lua code model. 
	 * All projects configured in {@link TestConfig#EVAL_PROJECT_CONFIGS} are parsed and tested.
	 * Results of the evaluation are written to a newly create evaluation file inside evaluation_results.
	 * @throws IOException
	 */
	@Test
	public void evaluationTest() throws IOException {
		// this list is filled with evaluation results for each project and printed
		// to an evaluation file at the end of the test.
		var evaluationResults = new ArrayList<String>();
		
		for (final var config : TestConfig.EVAL_PROJECT_CONFIGS) {
			final var path = config.getPath();
			evaluationResults.add("Results for project with path '" + path + "'...");
			final var charSet = config.getCharSet();
			final var verbose = config.isVerbose();
			
			var resourceSet = new LuaParser().parse(Paths.get(path));
			
			assertParsedAndSerializedEqualsOriginal(resourceSet, charSet);
			printNumberOfModelElements(evaluationResults, resourceSet);
			evaluateResolvedProxies(evaluationResults, resourceSet, verbose);
		}
		
		writeEvaluationResults(evaluationResults);
	}
	
	private void writeEvaluationResults(List<String> evaluationResults) throws IOException {
		final var now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMuuuu_HHmmss"));
		final var fileName = "eval_" + now + ".txt";
		final var path = Paths.get(EVAL_FOLDER_PATH + fileName);
		System.out.println("Test finished, printing evaluation results to file '" + path + "'...");
		Files.write(path, evaluationResults, StandardCharsets.UTF_8);
		System.out.println("Evaluation test concluded.");
	}
	
	private void assertParsedAndSerializedEqualsOriginal(final ResourceSet resourceSet, final String charSet) throws IOException {
		for (var r : resourceSet.getResources()) {
			var outputStream = new ByteArrayOutputStream();
			r.save(outputStream, new HashMap<>());
			var parsedAndSerialized = outputStream.toString();
			
			var originalPath = r.getURI().toFileString();
			String original = Files.readString(Paths.get(originalPath), Charset.forName(charSet));
			
			var strsEqual = TestUtil.compareNormalizedStrings(original, parsedAndSerialized);
			if (!strsEqual) {
				System.out.println(originalPath);
			}
			
			Assertions.assertTrue(strsEqual, "The original and the parsed and serialized code differ.");
		}
	}

	private void printNumberOfModelElements(List<String> evaluationResults, final ResourceSet resourceSet) {
		var counter = new AtomicInteger();
		resourceSet.getResources().forEach(r -> 
			r.getAllContents().forEachRemaining(
					modelElement -> counter.getAndIncrement()
			)
		);

		evaluationResults.add(" - Total number of resource elements: " + counter.get());
	}

	/**
	 * Calculates the percentages of resolved and mocked proxies/references and prints the results to the console.
	 * @param resourceSet the resourceSet
	 * @param verbose [Attention: currently not working!] whether to print additional information about which reference is being resolved.
	 */
	private void evaluateResolvedProxies(List<String> evaluationResults, final ResourceSet resourceSet, final boolean verbose) {
		// TODO: using verbose here does currently not work, leads to a ConcurrentModificationException
		if (verbose) {
			resolveCrossReferencesVerbose(resourceSet);
		} else {
			EcoreUtil.resolveAll(resourceSet);
		}

		final var allCrossReferences = EcoreUtil.CrossReferencer.find(resourceSet.getResources());
		final var unresolvedCrossReferences = EcoreUtil.UnresolvedProxyCrossReferencer.find(resourceSet);
		final var mockedCrossReferences = allCrossReferences.keySet().stream()
				.filter(cr -> cr instanceof SyntheticVar)
				.toList();

		final var allCrossReferencesCount = allCrossReferences.size();
		final var unresolvedCrossReferencesCount = unresolvedCrossReferences.size();
		final var mockedCrossReferencesCount = mockedCrossReferences.size();
		final var resolvedPercent = getPercentage(unresolvedCrossReferencesCount, allCrossReferencesCount);
		final var mockedPercent = 100d - getPercentage(mockedCrossReferencesCount, allCrossReferencesCount);
		
		evaluationResults.add(" - Total cross references count: " + allCrossReferencesCount + ",\n    - unresolved: " + unresolvedCrossReferencesCount + ",\n    - mocked: " + mockedCrossReferencesCount);
		evaluationResults.add(" - Resolved references: " + resolvedPercent + "% (needs to be 100% for CIPM)");
		evaluationResults.add(" - Mocked references: " + mockedPercent + "%");
		
		Assertions.assertTrue(unresolvedCrossReferences.isEmpty());
	}
	
	
	//TODO: currently not working, see comment/TODO in evaluateResolvedProxies
	/**
	 * This method can be used to debug reference resolution. It logs the currently resolved resource and
	 * EObject during resolution, s.t. the user can identify which parts of the model cause the problem.
	 * @param resourceSet
	 */
	private static void resolveCrossReferencesVerbose(final ResourceSet resourceSet) {
		final var resources = resourceSet.getResources();
		for (final var resource : resources) {			
			LOGGER.info("Resolving all in resource " + resource.getURI());
			for (final var obj : resource.getContents()) {
				LOGGER.info("Resolving all for object " + obj);
				obj.eContainer();
				resolveCrossReferences(obj);
				for (Iterator<EObject> i = obj.eAllContents(); i.hasNext();) {
					EObject childEObject = i.next();
					resolveCrossReferences(childEObject);
				}
			}			
		}
	}
	
	private static void resolveCrossReferences(final EObject eObject) {
		for (Iterator<EObject> i = eObject.eCrossReferences().iterator(); i.hasNext(); i.next()) {
			// The loop resolves the cross references by visiting them.
		}
	}
	
	
	private double getPercentage(final double of, final double from) {
		final var rel = (from - of)/from;
		return Math.round(rel*10000.0)/100.0;
	}

}
