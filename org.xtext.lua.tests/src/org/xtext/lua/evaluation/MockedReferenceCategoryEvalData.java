package org.xtext.lua.evaluation;

/**
 * POJO containing evaluation data regarding categories of mock references.
 * @author juanj
 *
 */
public class MockedReferenceCategoryEvalData {
	
	private MockInfo.Cause category;
	
	private long numberTotal = -1;
	
	/**
	 * The number of mocked references in this category, that were caused by previous mocked reference
	 * in their reference chain (e.g.: if <code>func</code> in <code>func.member</code> cannot be resolved,
	 * as a consequence, <code>member</code> cannot be resolved).
	 */
	private long numberCausedByOther = -1;
	
	private double percentageOfMockedReferences = -1;
	
	private double percentageOfAllReferences = -1;
	
	/**
	 * Use {@link CodeModelEvaluator} to create objects of this class.
	 */
	protected MockedReferenceCategoryEvalData() { }
	
	public MockInfo.Cause getCategory() {
		return category;
	}
	
	public void setCategory(MockInfo.Cause category) {
		this.category = category;
	}
	public long getNumberTotal() {
		return numberTotal;
	}
	
	public void setNumberTotal(long numberTotal) {
		this.numberTotal = numberTotal;
	}
	
	public long getNumberCausedByOther() {
		return numberCausedByOther;
	}
	
	public void setNumberCausedByOther(long numberCausedByOther) {
		this.numberCausedByOther = numberCausedByOther;
	}
	
	public double getPercentageOfMockedReferences() {
		return percentageOfMockedReferences;
	}
	
	public void setPercentageOfMockedReferences(double percentageOfMockedReferences) {
		this.percentageOfMockedReferences = percentageOfMockedReferences;
	}
	
	public double getPercentageOfAllReferences() {
		return percentageOfAllReferences;
	}
	
	public void setPercentageOfAllReferences(double percentageOfAllReferences) {
		this.percentageOfAllReferences = percentageOfAllReferences;
	}
	
}
