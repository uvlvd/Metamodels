package org.xtext.lua.tests;

/**
 * Class used to configure projects for the evaluation tests executed in {@link LuaParserTest} (see also {@link TestConfig#EVAL_PROJECT_CONFIGS}).
 * @author jsaenz
 *
 */
public class EvalProjectConfig {
	
	private final String path;
	private final boolean verbose;
	
	/**
	 * The config for a project that should be tested/evaluated in {@link LuaParserTest}.
	 * @param path the path of the project.
	 * @param verbose whether to log additional information to the console.
	 */
	public EvalProjectConfig(final String path, final boolean verbose) {
		this.path = path;
		this.verbose = verbose;
	}

	public String getPath() {
		return path;
	}

	public boolean isVerbose() {
		return verbose;
	}

}
