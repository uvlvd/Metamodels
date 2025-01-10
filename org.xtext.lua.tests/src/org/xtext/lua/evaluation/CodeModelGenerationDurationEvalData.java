package org.xtext.lua.evaluation;

import java.time.Duration;
import java.time.Instant;

import org.apache.log4j.Logger;

public class CodeModelGenerationDurationEvalData {
	private static final Logger LOGGER = Logger.getLogger(CodeModelGenerationDurationEvalData.class);
	
	private Instant parsingStart;
	private Instant parsingEnd;
	private Instant referenceResolutionStart;
	private Instant referenceResolutionEnd;
	
	
	protected void parsingStart() {
		parsingStart = Instant.now();
	}
	
	protected void parsingEnd() {
		parsingEnd = Instant.now();
	}
	
	protected void referenceResolutionStart() {
		referenceResolutionStart = Instant.now();
	}
	
	protected void referenceResolutionEnd() {
		referenceResolutionEnd = Instant.now();
	}
	
	protected long getParsingDuration() {
		if (parsingStart == null || parsingEnd == null) {
			LOGGER.warn("Could not compute parsing duration for evaluation of Code Model generation. Did you set the start and end times?");
			return -1;
		}
		return Duration.between(parsingStart, parsingEnd).toMillis();
	}
	
	protected long getReferenceResolutionDuration() {
		if (referenceResolutionStart == null || referenceResolutionEnd == null) {
			LOGGER.warn("Could not compute reference resolution duration for evaluation of Code Model generation. Did you set the start and end times?");
			return -1;
		}
		return Duration.between(referenceResolutionStart, referenceResolutionEnd).toMillis();
	}
}
