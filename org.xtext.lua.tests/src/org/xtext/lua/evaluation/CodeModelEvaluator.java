package org.xtext.lua.evaluation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.LuaCodeModel;
import org.xtext.lua.LuaParser;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.mocking.SyntheticVar;
import org.xtext.lua.tests.TestUtil;

public class CodeModelEvaluator {
	private static final Logger LOGGER = Logger.getLogger(CodeModelEvaluator.class);
	
	private HashMap<String, CodeModelGenerationEvalData> evalDatas = new HashMap<>();
	private HashMap<String, CodeModelGenerationDurationEvalData> durationDatas = new HashMap<>();
	private MockInfoCollector mockInfoCollector = new MockInfoCollector();
	private SyntheticReferenceInfoCollector syntheticReferenceInfoCollector = new SyntheticReferenceInfoCollector();
	
	/**
	 * Evaluates the given {@link LuaCodeModel}, computing the {@link CodeModelGenerationEvalData} for the given
	 * <code>projectId</code>.
	 * <p>
	 * Note that the evaluation data has to be set up before performing the evaluation (using one of the <code>setup</code> methods)
	 * and the references of the given {@link LuaCodeModel} must have been resolved (see {@link LuaParser#resolveAll}).
	 * </p>
	 * @param projectId the projectId.
	 * @param codeModel the code model with resolved references.
	 */
	public void evaluate(final String projectId, final LuaCodeModel codeModel) {
		if (verifySetupFor(projectId)) {
			final var evalData = evalDatas.get(projectId);
			final var durationData = durationDatas.get(projectId);
			
			computeAndSetDurationsFor(evalData, durationData);
			computeAndSetParsingProcessDataFor(codeModel, evalData);
			computeAndSetReferenceResolutionDataFor(codeModel, evalData);
			computeAndSetMockedElementsData(codeModel, evalData);
		}
	}

	public MockInfoCollector getMockInfoCollector() {
		return mockInfoCollector;
	}

	public void setupAndStartTimingEvaluationFor(final String projectId) {
		setupEvaluationFor(projectId);
		startTimingParsingProcessFor(projectId);
	}
	
	public void setupEvaluationFor(final String projectId) {
		evalDatas.putIfAbsent(projectId, new CodeModelGenerationEvalData());
		durationDatas.putIfAbsent(projectId, new CodeModelGenerationDurationEvalData());
	}
	
	/**
	 * Sets the project path for the project with the given projectId.
	 * <p>
	 * Ensure that a setup method has been called for the given project first.
	 * </p>
	 * @param projectId the project's id.
	 */
	public void setProjectPathFor(final String projectId, final String projectPath) {
		if (verifySetupFor(projectId)) {
			evalDatas.get(projectId).setProjectPath(projectPath);
		}
	}
	
	/**
	 * Starts the timing of the parsing process for the project with the given id, 
	 * call {@link #stopTimingParsingProcessFor(String)} when parsing is done.
	 * <p>
	 * Ensure that a setup method has been called for the given project first.
	 * </p>
	 * @param projectId the project's id.
	 */
	public void startTimingParsingProcessFor(final String projectId) {
		if (verifySetupFor(projectId)) {
			durationDatas.get(projectId).parsingStart();
		}
	}
	
	/**
	 * Stops the timing of the parsing process the project with the given id. 
	 * <p>Make sure to call {@link #startTimingParsingProcessFor(String)} first.</p>
	 * 
	 * @param projectId the project's id.
	 */
	public void stopTimingParsingProcessFor(final String projectId) {
		if (verifySetupFor(projectId)) {
			durationDatas.get(projectId).parsingEnd();
		}
	}
	
	/**
	 * Starts the timing of the reference resolution process for the project with the given id, 
	 * call {@link #stopTimingEvaluationFor(String)} when the reference resolution process finished.
	 * <p>
	 * Ensure that a setup method has been called for the given project first.
	 * </p>
	 * @param projectId the project's id.
	 */
	public void startTimingReferenceResolutionProcessFor(final String projectId) {
		if (verifySetupFor(projectId)) {
			durationDatas.get(projectId).referenceResolutionStart();
		}
	}
	
	/**
	 * Stops the timing of the reference resolution process the project with the given id. 
	 * <p>Make sure to call {@link #startTimingReferenceResolutionProcessFor(String)} first.</p>
	 * 
	 * @param projectId the project's id.
	 */
	public void stopTimingReferenceResolutionProcessFor(final String projectId) {
		if (verifySetupFor(projectId)) {
			durationDatas.get(projectId).referenceResolutionEnd();
		}
	}
	
	/**
	 * Returns the {@link CodeModelGenerationEvalData} for all processed projects.
	 */
	public Collection<CodeModelGenerationEvalData> getEvalDatas() {
		return evalDatas.values();
	}
	
	private void computeAndSetDurationsFor(CodeModelGenerationEvalData evalData, CodeModelGenerationDurationEvalData durationData) {
		final var parseDuration = durationData.getParsingDuration();
		final var referenceResolutionDuration = durationData.getReferenceResolutionDuration();
		final var totalDuration = parseDuration + referenceResolutionDuration;
		
		evalData.setParseTime(parseDuration);
		evalData.setReferenceResolutionTime(referenceResolutionDuration);
		evalData.setTotalTime(totalDuration);
	}
	
	private void computeAndSetParsingProcessDataFor(final LuaCodeModel codeModel, CodeModelGenerationEvalData evalData) {
		evalData.setTotalNumberModelElements(computeTotalNumberOfModelElements(codeModel));
		try {
			evalData.setOriginalAndSerialisedCodeEqual(isParsedAndSerializedEqualsOriginal(codeModel));
		} catch (IOException e) {
			LOGGER.error("Could not compute similarity between original and serialized code.", e);
		}
	}
	
	private void computeAndSetReferenceResolutionDataFor(final ResourceSet codeModel, CodeModelGenerationEvalData evalData) {
		final var allCrossReferences = EcoreUtil.CrossReferencer.find(codeModel.getResources());
		final var unresolvedCrossReferences = EcoreUtil.UnresolvedProxyCrossReferencer.find(codeModel);
		final var mockedCrossReferences = allCrossReferences.keySet()
											.stream()
											.filter(cr -> cr instanceof SyntheticVar)
											.toList();

		final var allCrossReferencesCount = allCrossReferences.size();
		final var unresolvedCrossReferencesCount = unresolvedCrossReferences.size();
		final var mockedCrossReferencesCount = mockedCrossReferences.size();
		final var resolvedPercent = NumberUtil.roundPercentage(NumberUtil.computePercentage(unresolvedCrossReferencesCount, allCrossReferencesCount));
		final var mockedPercent = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(mockedCrossReferencesCount, allCrossReferencesCount));
		
		evalData.setNumberCrossReferences(allCrossReferencesCount);
		evalData.setNumberUnresolvedCrossReferences(unresolvedCrossReferencesCount);
		evalData.setResolvedPercent(resolvedPercent);
		evalData.setNumberDirectReferencesToMockedElements(mockedCrossReferencesCount);
		evalData.setPercentDirectReferencesToMockedElements(mockedPercent);
	}
	
	private void computeAndSetMockedElementsData(final ResourceSet codeModel, CodeModelGenerationEvalData evalData) {
		mockInfoCollector.clear();
		
		var mockedObjectCount = 0;
		var numberReferencingElements = 0;
		for (final var res : codeModel.getResources()) {
			var root = res.getContents().get(0);
			mockedObjectCount += EcoreUtil2.getAllContentsOfType(root, SyntheticVar.class).size();
			
			var allReferencings = EcoreUtil2.getAllContentsOfType(root, Referencing.class);
			numberReferencingElements += allReferencings.size();
			allReferencings
				.stream()
				.forEach( refing -> {
					if (refing.getRef() instanceof SyntheticVar) {
						mockInfoCollector.collect(refing);
					}
				});
		}
		
		final var numberMockedReferences = mockInfoCollector.getCount();
		final var mockedPercentage = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(numberMockedReferences, numberReferencingElements));
		
		evalData.setNumberMockedElements(mockedObjectCount);
		evalData.setNumberReferencingObjects(numberReferencingElements);
		evalData.setNumberNonMockedReferences(numberReferencingElements - numberMockedReferences);
		evalData.setNumberMockedReferences(numberMockedReferences);
		evalData.setMockedReferencesPercentage(mockedPercentage);
		evalData.setMockedReferenceCategoryDatas(
				createMockedCategoriesdata(mockInfoCollector, numberMockedReferences, numberReferencingElements, evalData)
		);
		
		
		// new
		final var numberReferencesTotal = numberReferencingElements;
		final var syntheticReferenceData = syntheticReferenceInfoCollector.getSyntheticReferenceTypeEvalData(codeModel, numberReferencesTotal);
		evalData.setSyntheticReferenceEvalDatas(syntheticReferenceData);
		final var numberSyntheticReferencesTotal = syntheticReferenceData.values()
				.stream()
				.mapToInt(SyntheticReferenceEvalData::getNumberTotalSyntheticOfType)
				.sum();
		final var numberNonSyntheticReferencesTotal = numberReferencesTotal - numberSyntheticReferencesTotal;
		final var percentageSyntheticReferences = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(numberSyntheticReferencesTotal, numberReferencesTotal));;
		evalData.setNumberReferencesTotal(numberReferencesTotal);
		evalData.setNumberNonSyntheticReferencesTotal(numberNonSyntheticReferencesTotal);
		evalData.setNumberSyntheticReferencesTotal(numberSyntheticReferencesTotal);
		evalData.setPercentageSyntheticReferences(percentageSyntheticReferences);
	}
	
	private Collection<MockedReferenceCategoryEvalData> createMockedCategoriesdata(
			final MockInfoCollector mockInfoCollector, 
			final long numberMockedReferences,
			final long numberReferencingElements,
			final CodeModelGenerationEvalData evalData) {
		var infoByCause = mockInfoCollector.getInfoByCause();
		
		var result = new ArrayList<MockedReferenceCategoryEvalData>();
		infoByCause.keySet().stream().forEach(cause -> {
			final var count = infoByCause.get(cause).size();
			final var numberCausedByPrevious = infoByCause.get(cause)
				.stream()
				.filter(MockInfo::isCausedByPreviousFeature)
				.toList()
				.size();
			var categoryData = new MockedReferenceCategoryEvalData();
			final var percentageOfMockedReferences = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(count, numberMockedReferences));
			
			final var percentageOfAllReferences = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(count, numberReferencingElements));
			
			categoryData.setCategory(cause);
			categoryData.setNumberCausedByOther(numberCausedByPrevious);
			categoryData.setNumberTotal(count);
			categoryData.setPercentageOfAllReferences(percentageOfAllReferences);
			categoryData.setPercentageOfMockedReferences(percentageOfMockedReferences);
			
			result.add(categoryData);
		});
		
		return result;
	}
	
	
	private long computeTotalNumberOfModelElements(final ResourceSet codeModel) {
		var counter = new AtomicLong();
		codeModel.getResources().forEach(r -> 
			r.getAllContents().forEachRemaining(
					modelElement -> counter.getAndIncrement()
			)
		);
		return counter.longValue();
	}
	
	private boolean isParsedAndSerializedEqualsOriginal(final LuaCodeModel codeModel) throws IOException {		
		var serialized = new LuaParser().serialize(codeModel);
		
		for (final var r : codeModel.getSerializableResources()) {
			final var parsedAndSerialized = serialized.get(r.getURI()).toString();
			final var originalPath = r.getURI().toFileString();
			final var original = Files.readString(Paths.get(originalPath));
			final var strsEqual = TestUtil.compareNormalizedStrings(original, parsedAndSerialized);
			if (!strsEqual) {
				LOGGER.warn("ParedAndSerialized code and original code are not equal for path: " + originalPath);
				return false;
			}
		}
		
		return true;
	}
	
	private boolean verifySetupFor(final String projectId) {
		var isSetup = evalDatas.containsKey(projectId) && durationDatas.containsKey(projectId);
		if (!isSetup) {
			LOGGER.error("Attempting evaluation operation on Code Model without previous setup of the Code Model evaluation data. Did you call one of the setup methods first?");
		}
		return isSetup;
	}
	

	
}
