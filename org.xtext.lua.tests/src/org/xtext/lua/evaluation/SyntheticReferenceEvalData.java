package org.xtext.lua.evaluation;

import java.util.EnumMap;
import java.util.Map;

import org.xtext.lua.evaluation.SyntheticReferenceInfo.Cause;
import org.xtext.lua.evaluation.SyntheticReferenceInfo.Type;

public class SyntheticReferenceEvalData {
	
	protected class SyntheticReferenceCauseEvalData {
		private int numberTotal = -1;
		private double percentOfType = -1;
		private double percentSyntheticOfType = -1;
		
		public int getNumberTotal() {
			return numberTotal;
		}
		public void setNumberTotal(int numberTotal) {
			this.numberTotal = numberTotal;
		}
		public double getPercentOfType() {
			return percentOfType;
		}
		public void setPercentOfType(double percentOfType) {
			this.percentOfType = percentOfType;
		}
		public double getPercentSyntheticOfType() {
			return percentSyntheticOfType;
		}
		public void setPercentSyntheticOfType(double percentSyntheticOfType) {
			this.percentSyntheticOfType = percentSyntheticOfType;
		}
	}

	private Type type;
	private int numberReferencesOfType = -1;
	private double percentOfReferencesOfType = -1;
	private int numberTotalSyntheticOfType = -1;
	private double percentOfTotalSyntheticReferences = -1;
	private double percentOfAllReferences = -1;
	
	/**
	 * The "filtered" values represent synthetic references of the given type that do have
	 * an underlying cause of implicit import or var_not_found
	 */
	private int numberFilteredSyntheticOfType = -1;
	private double percentFilteredOfReferencesOfType = -1;
	private double percentFilteredOfSyntheticReferencesOfType = -1;
	private double percentFilteredOfTotalSyntheticReferences = -1;
	private double percentFilteredOfAllReferences = -1;
	
	
	private Map<Cause, SyntheticReferenceCauseEvalData> causes = new EnumMap<>(Cause.class);
	
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public int getNumberReferencesOfType() {
		return numberReferencesOfType;
	}
	public void setNumberReferencesOfType(int numberReferencesOfType) {
		this.numberReferencesOfType = numberReferencesOfType;
	}
	public double getPercentOfReferencesOfType() {
		return percentOfReferencesOfType;
	}
	public void setPercentOfReferencesOfType(double percentOfReferencesOfType) {
		this.percentOfReferencesOfType = percentOfReferencesOfType;
	}
	public int getNumberTotalSyntheticOfType() {
		return numberTotalSyntheticOfType;
	}
	public void setNumberTotalSyntheticOfType(int numberTotal) {
		this.numberTotalSyntheticOfType = numberTotal;
	}
	public double getPercentOfTotalSyntheticReferences() {
		return percentOfTotalSyntheticReferences;
	}
	public void setPercentOfTotalSyntheticReferences(double percentOfTotalSyntheticReferences) {
		this.percentOfTotalSyntheticReferences = percentOfTotalSyntheticReferences;
	}
	public double getPercentOfAllReferences() {
		return percentOfAllReferences;
	}
	public void setPercentOfAllReferences(double percentOfAllReferences) {
		this.percentOfAllReferences = percentOfAllReferences;
	}
	public Map<Cause, SyntheticReferenceCauseEvalData> getCauses() {
		return causes;
	}
	public void setCauses(Map<Cause, SyntheticReferenceCauseEvalData> causes) {
		this.causes = causes;
	}
	public int getNumberFilteredSyntheticOfType() {
		return numberFilteredSyntheticOfType;
	}
	public void setNumberFilteredSyntheticOfType(int numberFilteredSyntheticOfType) {
		this.numberFilteredSyntheticOfType = numberFilteredSyntheticOfType;
	}
	public double getPercentFilteredOfReferencesOfType() {
		return percentFilteredOfReferencesOfType;
	}
	public void setPercentFilteredOfReferencesOfType(double percentFilteredOfReferencesOfType) {
		this.percentFilteredOfReferencesOfType = percentFilteredOfReferencesOfType;
	}
	public double getPercentFilteredOfTotalSyntheticReferences() {
		return percentFilteredOfTotalSyntheticReferences;
	}
	public void setPercentFilteredOfTotalSyntheticReferences(double percentFilteredOfTotalSyntheticReferences) {
		this.percentFilteredOfTotalSyntheticReferences = percentFilteredOfTotalSyntheticReferences;
	}
	public double getPercentFilteredOfAllReferences() {
		return percentFilteredOfAllReferences;
	}
	public void setPercentFilteredOfAllReferences(double percentFilteredOfAllReferences) {
		this.percentFilteredOfAllReferences = percentFilteredOfAllReferences;
	}
	public double getPercentFilteredOfSyntheticReferencesOfType() {
		return percentFilteredOfSyntheticReferencesOfType;
	}
	public void setPercentFilteredOfSyntheticReferencesOfType(double percentFilteredOfSyntheticReferencesOfType) {
		this.percentFilteredOfSyntheticReferencesOfType = percentFilteredOfSyntheticReferencesOfType;
	}
	
	
	
	
}
