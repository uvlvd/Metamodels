package org.xtext.lua.tests;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.xtext.lua.LuaParser;
import org.xtext.lua.evaluation.CodeModelEvaluator;
import org.xtext.lua.evaluation.EvalDataWriter;

public class LuaParserTest {
	
	/**
	 * Test used for the evaluation of the Lua code model converter. 
	 * All projects configured in {@link TestConfig#EVAL_PROJECT_CONFIGS} are parsed and tested.
	 * Results of the evaluation are written to a newly create evaluation file inside evaluation_results.
	 * @throws IOException
	 */
	@Test
	public void evaluationTest() throws IOException {
		
		final var evaluator = new CodeModelEvaluator();
		
		for (final var config : TestConfig.EVAL_PROJECT_CONFIGS) {
			final var projectPath = config.getPath();
			evaluator.setupEvaluationFor(projectPath);
			evaluator.setProjectPathFor(projectPath, projectPath);
			var luaParser = new LuaParser();
			

			evaluator.startTimingParsingProcessFor(projectPath);
			var codeModel = luaParser.parse(Paths.get(projectPath));
			evaluator.stopTimingParsingProcessFor(projectPath);
			
			evaluator.startTimingReferenceResolutionProcessFor(projectPath);
			luaParser.resolveAll(codeModel);
			evaluator.stopTimingReferenceResolutionProcessFor(projectPath);
			
			evaluator.evaluate(projectPath, codeModel);
		}
		
		EvalDataWriter.writeAll(evaluator.getEvalDatas());
	}
	
}
