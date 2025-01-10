package org.xtext.lua.evaluation;

import java.util.Collection;

/**
 * POJO containing evaluation data for the code model generation.
 * @author juanj
 *
 */
public class CodeModelGenerationEvalData {
	private String projectPath = "";
	
	// execution time data
	private long parseTime = -1;
	private long referenceResolutionTime = -1;
	private long totalTime = -1;
	
	// parsing process data
	private long totalNumberModelElements = -1;
	private Boolean originalAndSerialisedCodeEqual = null;
	
	// reference resolution process data
	private long numberCrossReferences = -1;
	private long numberUnresolvedCrossReferences = -1;
	private double resolvedPercent = -1;
	// TODO: used to compare to the mocked percentage during debugging, can be removed
	private long numberDirectReferencesToMockedElements = -1;
	private double percentDirectReferencesToMockedElements = -1;
	
	// mock info: mocked objects and references to mocked objects 
	private long numberMockedElements = -1;
	private long numberReferencingObjects = -1;
	private long numberNonMockedReferences = -1;
	private long numberMockedReferences = -1;
	private double mockedReferencesPercentage = -1;

	private Collection<MockedReferenceCategoryEvalData> mockedReferenceCategoryDatas = null;
	
	/**
	 * Use {@link CodeModelEvaluator} to create objects of this class.
	 */
	protected CodeModelGenerationEvalData() { }
	
	public String getProjectPath() {
		return projectPath;
	}
	
	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}
	
	public long getParseTime() {
		return parseTime;
	}
	
	public void setParseTime(long parseTime) {
		this.parseTime = parseTime;
	}
	
	public long getReferenceResolutionTime() {
		return referenceResolutionTime;
	}
	
	public void setReferenceResolutionTime(long referenceResolutionTime) {
		this.referenceResolutionTime = referenceResolutionTime;
	}
	
	public long getTotalNumberModelElements() {
		return totalNumberModelElements;
	}
	
	public void setTotalNumberModelElements(long totalNumberModelElements) {
		this.totalNumberModelElements = totalNumberModelElements;
	}
	
	public long getNumberUnresolvedCrossReferences() {
		return numberUnresolvedCrossReferences;
	}
	
	public void setNumberUnresolvedCrossReferences(long numberUnresolvedCrossReferences) {
		this.numberUnresolvedCrossReferences = numberUnresolvedCrossReferences;
	}
	
	public double getResolvedPercent() {
		return resolvedPercent;
	}
	
	public void setResolvedPercent(double resolvedPercent) {
		this.resolvedPercent = resolvedPercent;
	}


	public Boolean getOriginalAndSerialisedCodeEqual() {
		return originalAndSerialisedCodeEqual;
	}

	public void setOriginalAndSerialisedCodeEqual(Boolean originalAndSerialisedCodeEqual) {
		this.originalAndSerialisedCodeEqual = originalAndSerialisedCodeEqual;
	}

	public long getNumberCrossReferences() {
		return numberCrossReferences;
	}

	public void setNumberCrossReferences(long numberCrossReferences) {
		this.numberCrossReferences = numberCrossReferences;
	}

	public void setTotalTime(long totalTime) {
		this.totalTime = totalTime;
	}

	public long getTotalTime() {
		return totalTime;
	}

	public long getNumberDirectReferencesToMockedElements() {
		return numberDirectReferencesToMockedElements;
	}

	public void setNumberDirectReferencesToMockedElements(long numberDirectReferencesToMockedElements) {
		this.numberDirectReferencesToMockedElements = numberDirectReferencesToMockedElements;
	}

	public double getPercentDirectReferencesToMockedElements() {
		return percentDirectReferencesToMockedElements;
	}

	public void setPercentDirectReferencesToMockedElements(double percentDirectReferencesToMockedElements) {
		this.percentDirectReferencesToMockedElements = percentDirectReferencesToMockedElements;
	}

	public long getNumberReferencingObjects() {
		return numberReferencingObjects;
	}

	public void setNumberReferencingObjects(long numberReferencingObjects) {
		this.numberReferencingObjects = numberReferencingObjects;
	}

	public long getNumberNonMockedReferences() {
		return numberNonMockedReferences;
	}

	public void setNumberNonMockedReferences(long numberNonMockedReferences) {
		this.numberNonMockedReferences = numberNonMockedReferences;
	}

	public long getNumberMockedReferences() {
		return numberMockedReferences;
	}

	public void setNumberMockedReferences(long numberMockedReferences) {
		this.numberMockedReferences = numberMockedReferences;
	}

	public double getMockedReferencesPercentage() {
		return mockedReferencesPercentage;
	}

	public void setMockedReferencesPercentage(double mockedReferencesPercentage) {
		this.mockedReferencesPercentage = mockedReferencesPercentage;
	}

	public long getNumberMockedElements() {
		return numberMockedElements;
	}

	public void setNumberMockedElements(long numberMockedElements) {
		this.numberMockedElements = numberMockedElements;
	}

	public Collection<MockedReferenceCategoryEvalData> getMockedReferenceCategoryDatas() {
		return mockedReferenceCategoryDatas;
	}

	public void setMockedReferenceCategoryDatas(Collection<MockedReferenceCategoryEvalData> mockedReferenceCategoryDatas) {
		this.mockedReferenceCategoryDatas = mockedReferenceCategoryDatas;
	}

}
