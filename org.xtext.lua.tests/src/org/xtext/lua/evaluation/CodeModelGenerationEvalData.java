package org.xtext.lua.evaluation;

import java.util.Map;

import org.xtext.lua.evaluation.SyntheticReferenceInfo.Type;

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
	
	// data generated based on SyntheticReferenceInfoCollector
	private long numberReferencesTotal = -1;
	private long numberNonSyntheticReferencesTotal = -1;
	private long numberSyntheticReferencesTotal = -1;
	private double percentageSyntheticReferences = -1;
	private long numberSyntheticReferencesFiltered = -1;
	private double percentageSyntheticReferencesFiltered = -1;
	private Map<Type, SyntheticReferenceEvalData> syntheticReferenceEvalDatas = null;

	
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

	public long getNumberMockedElements() {
		return numberMockedElements;
	}

	public void setNumberMockedElements(long numberMockedElements) {
		this.numberMockedElements = numberMockedElements;
	}


	public Map<Type, SyntheticReferenceEvalData> getSyntheticReferenceEvalDatas() {
		return syntheticReferenceEvalDatas;
	}

	public void setSyntheticReferenceEvalDatas(Map<Type, SyntheticReferenceEvalData> syntheticReferenceEvalDatas) {
		this.syntheticReferenceEvalDatas = syntheticReferenceEvalDatas;
	}

	public long getNumberNonSyntheticReferencesTotal() {
		return numberNonSyntheticReferencesTotal;
	}

	public void setNumberNonSyntheticReferencesTotal(long numberNonSyntheticReferencesTotal) {
		this.numberNonSyntheticReferencesTotal = numberNonSyntheticReferencesTotal;
	}

	public long getNumberReferencesTotal() {
		return numberReferencesTotal;
	}

	public void setNumberReferencesTotal(long numberReferencesTotal) {
		this.numberReferencesTotal = numberReferencesTotal;
	}

	public long getNumberSyntheticReferencesTotal() {
		return numberSyntheticReferencesTotal;
	}

	public void setNumberSyntheticReferencesTotal(long numberSyntheticReferencesTotal) {
		this.numberSyntheticReferencesTotal = numberSyntheticReferencesTotal;
	}

	public double getPercentageSyntheticReferences() {
		return percentageSyntheticReferences;
	}

	public void setPercentageSyntheticReferences(double percentageSyntheticReferences) {
		this.percentageSyntheticReferences = percentageSyntheticReferences;
	}

	public long getNumberSyntheticReferencesFiltered() {
		return numberSyntheticReferencesFiltered;
	}

	public void setNumberSyntheticReferencesFiltered(long numberSyntheticReferencesFiltered) {
		this.numberSyntheticReferencesFiltered = numberSyntheticReferencesFiltered;
	}

	public double getPercentageSyntheticReferencesFiltered() {
		return percentageSyntheticReferencesFiltered;
	}

	public void setPercentageSyntheticReferencesFiltered(double percentageSyntheticReferencesFiltered) {
		this.percentageSyntheticReferencesFiltered = percentageSyntheticReferencesFiltered;
	}
	
	

}
