package org.xtext.lua.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
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
import org.eclipse.xtext.EcoreUtil2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xtext.lua.LuaParser;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.mocking.MockInfo;
import org.xtext.lua.mocking.MockInfoCollector;
import org.xtext.lua.mocking.SyntheticVar;
import org.xtext.lua.utils.ExpUtil;
import org.xtext.lua.utils.StatUtil;

import com.google.inject.Inject;


public class LuaParserTest {
	private static final Logger LOGGER = Logger.getLogger(LuaParserTest.class);
	private static final String EVAL_FOLDER_PATH = "evaluation_results\\";
	
	
	private MockInfoCollector mockInfoCollector = new MockInfoCollector();
	
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
			mockInfoCollector.clear();
			final var path = config.getPath();
			evaluationResults.add("Results for project with path '" + path + "'...");
			
			final var verbose = config.isVerbose();
			
			var start = Instant.now();
			
			var luaParser = new LuaParser();
			var resourceSet = luaParser.parse(Paths.get(path));
			
			assertParsedAndSerializedEqualsOriginal(resourceSet);
			printNumberOfModelElements(evaluationResults, resourceSet);
			evaluateResolvedProxies(evaluationResults, resourceSet, verbose);
			
			var end = Instant.now();
			evaluationResults.add(" - Duration: " + Duration.between(start, end));
			
			
			var indexExpsWithDummyName  = new ArrayList<TableAccess>();
			//TODO: extract
			var mockedObjectCount = 0;
			var referencesCount = 0;

			for (final var res : resourceSet.getResources()) {
				var root = res.getContents().get(0);
				mockedObjectCount += EcoreUtil2.getAllContentsOfType(root, SyntheticVar.class).size();
				
				var allReferencings = EcoreUtil2.getAllContentsOfType(root, Referencing.class);
				referencesCount += allReferencings.size();
				
				allReferencings
					.stream()
					.forEach( refing -> {
						if (refing.getRef() instanceof SyntheticVar) {
							mockInfoCollector.collect(refing);
						}
						if (refing instanceof TableAccess ta && ExpUtil.isTableAccessWithLinkingDummyName(ta)) {
							indexExpsWithDummyName.add(ta);
						}
					});
			}
			
			
			var infoByCause = mockInfoCollector.getInfoByCause();
			var i = 0;
			System.out.println("================ UNKNOWN ===============");
			for (var unknown : infoByCause.get(MockInfo.Cause.UNKNOWN)) {
				mockInfoCollector.print(unknown, luaParser.getSerializer());
				System.out.println();
				i++;
				if (i == 10) {
					break;
				}
			}
			
			i = 0;
			System.out.println("================ VAR_NOT_FOUND ===============");
			for (var unknown : infoByCause.get(MockInfo.Cause.VAR_NOT_FOUND)) {
				mockInfoCollector.print(unknown, luaParser.getSerializer());
				System.out.println();
				i++;
				if (i == 10) {
					break;
				}
			}
			
			i = 0;
			System.out.println("================ TABLE_ACCESS ===============");
			for (var unknown : infoByCause.get(MockInfo.Cause.TABLE_INDEX_EXP)) {
				mockInfoCollector.print(unknown, luaParser.getSerializer());
				System.out.println();
				i++;
				if (i == 10) {
					break;
				}
			}
			
//			System.out.println("================ dummy name tas ===============");
//			for (var ta : indexExpsWithDummyName) {
//				MockInfoCollector.print(ta, luaParser.getSerializer());
//				System.out.println();
//			}
			
			var mockedCount = mockInfoCollector.getCount();
			var mockedPercentage = 100d - getPercentage(mockedCount, referencesCount);
			evaluationResults.add(" - Mocked references percentage: " + mockedPercentage + "%");
			evaluationResults.add(" - Mocked object (objects referencing a mocked object) categorization (total " + mockedCount +  "):");
			infoByCause.keySet().stream().forEach(cause -> {
				
				final var count = infoByCause.get(cause).size();
				final var countCausedByPrevious = infoByCause.get(cause)
					.stream()
					.filter(MockInfo::isCausedByPreviousFeature)
					.toList()
					.size();
				var percentage = 100d - getPercentage(count, mockedCount);
				percentage = Math.round(percentage * 100.0)/100.0;
				var str = cause + ": " + count + " (" + countCausedByPrevious + ")" + ", " + percentage + "%";
				evaluationResults.add("    - " + str);
				
				System.out.println(str);
					
			});
			
			System.out.println("dummy name ta count: " + indexExpsWithDummyName.size());
			System.out.println("mocked object count: " + mockedObjectCount);
			
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
	
	private void assertParsedAndSerializedEqualsOriginal(final ResourceSet resourceSet) throws IOException {
		for (var r : resourceSet.getResources()) {
			var outputStream = new ByteArrayOutputStream();
			var options = new HashMap<>();
			r.save(outputStream, options);
			var parsedAndSerialized = outputStream.toString();
			
			var originalPath = r.getURI().toFileString();
			String original = Files.readString(Paths.get(originalPath));
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
		evaluationResults.add(" - Mocked reference objects: " + mockedPercent + "%");
		
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
