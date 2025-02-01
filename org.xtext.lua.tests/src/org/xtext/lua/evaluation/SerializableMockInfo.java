package org.xtext.lua.evaluation;

import org.eclipse.emf.ecore.EObject;
import org.xtext.lua.lua.NamedFeature;
import org.xtext.lua.lua.Stat;

/**
 * POJO class used to store an {@link MockInfo} with the corresponding statement lua code string 
 * for debugging.
 * @author juanj
 *
 */
public class SerializableMockInfo {
	
	private String containingStatement;
	//private MockInfo mockInfo;
	private final String context;
	private final String parentStat;
	private final String resourceUri;
	
	public SerializableMockInfo(String containingStatement, MockInfo mockInfo) {
		this.containingStatement = containingStatement;
		this.context = mockInfo.getContext().toString();
		this.parentStat = mockInfo.getParentStat() != null ? mockInfo.getParentStat().toString() : null;
		this.resourceUri = mockInfo.getResourceUri().toString();
	}
	
	public String getContainingStatement() {
		return containingStatement;
	}

	public void setContainingStatement(String containingStatement) {
		this.containingStatement = containingStatement;
	}

	public String getContext() {
		return context;
	}

	public String getParentStat() {
		return parentStat;
	}

	public String getResourceUri() {
		return resourceUri;
	}

}
