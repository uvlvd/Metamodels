package org.xtext.lua.tests;

import java.util.List;

public class TestConfig {
	private TestConfig() { }
	
	/**
	 * Whether to print the models resulting from the code snippets (SUTs) in the 
	 * {@link LuaLocalScopingTest} and {@link LuaGlobalScopingTest} test classes.
	 */
	public static final boolean PRINT_SNIPPET_MODELS = false;
	
	/**
	 * List containing the project configs for all projects that are evaluated in
	 * {@link LuaParserTest}.
	 */
	public static final List<EvalProjectConfig> EVAL_PROJECT_CONFIGS = List.of(
				new EvalProjectConfig(
					"test_data\\lua-5.2.0-tests_utf8",
					false
				)
			);
}
