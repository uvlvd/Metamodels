package org.xtext.lua.evaluation;

import java.util.EnumMap;
import java.util.Map;

import org.xtext.lua.evaluation.SyntheticReferenceInfo.Cause;
import org.xtext.lua.evaluation.SyntheticReferenceInfo.Type;

public class SyntheticReferenceEvalData {
	
	protected class SyntheticReferenceCauseEvalData {
		private int numberTotal = -1;
		private double percentOfType = -1;
		
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

	}

	private Type type;
	private int numberTotal = -1;
	private double percentOfTotalSyntheticReferences = -1;
	private double percentOfAllReferences = -1;
	private Map<Cause, SyntheticReferenceCauseEvalData> causes = new EnumMap<>(Cause.class);
	
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public int getNumberTotal() {
		return numberTotal;
	}
	public void setNumberTotal(int numberTotal) {
		this.numberTotal = numberTotal;
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
	
	
}
