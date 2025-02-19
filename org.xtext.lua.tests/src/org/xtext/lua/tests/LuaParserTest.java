package org.xtext.lua.tests;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.xtext.lua.LuaParser;
import org.xtext.lua.evaluation.CodeModelEvaluator;
import org.xtext.lua.evaluation.EvalDataWriter;
import org.xtext.lua.scoping.LuaGlobalScopeProvider;
import org.xtext.lua.utils.FunctionUtil;
import org.xtext.lua.wrappers.LuaFunctionCall;

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
			
			// write mock infos to json for debugging
			//final var statementMockInfos = evaluator.getMockInfoCollector().getStatementMockInfosByCause(luaParser.getSerializer());
			//EvalDataWriter.writeAll(statementMockInfos);
			var allFunctionCalls = new ArrayList<LuaFunctionCall>();
			for (var res: codeModel.getResources()) {
				allFunctionCalls.addAll(FunctionUtil.getFunctionCallsContainedIn(res.getContents().get(0)));
			}
			var totalExternal = 0;
			for (var fc : allFunctionCalls) {
				if (!fc.isMocked()) {
					var declResource = fc.getCalledFunction().getRoot().eResource();
					if (LuaGlobalScopeProvider.isImplicitResource(declResource)) {
						continue;
					}
					if (fc.getCallingFeature().eResource() != declResource) {
						totalExternal++;
					}
				}
			}
			System.out.println("total calls to other files without implicit: " + totalExternal);
		}
		
		EvalDataWriter.writeAll(evaluator.getEvalDatas());
	}
	
	// does not provide timing information
	@Test
	public void evaluateGenerationTest() throws IOException {
		
		final var evaluator = new CodeModelEvaluator();
		
		for (final var config : TestConfig.EVAL_PROJECT_CONFIGS) {
			final var projectPath = config.getPath();
			evaluator.setupEvaluationFor(projectPath);
			evaluator.setProjectPathFor(projectPath, projectPath);
			var luaParser = new LuaParser();
			
			var codeModel = luaParser.generate(Paths.get(projectPath));
			
			evaluator.evaluate(projectPath, codeModel);
		}
		
		EvalDataWriter.writeAll(evaluator.getEvalDatas());
	}
	
}
