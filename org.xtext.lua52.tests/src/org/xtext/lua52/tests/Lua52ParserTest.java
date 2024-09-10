package org.xtext.lua52.tests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xtext.lua52.Lua52Parser;
import org.xtext.lua52.PreprocessingUtils;

public class Lua52ParserTest {
	@Test
	public void testMinimalParseTest() throws IOException {
		final var apisix = "D:\\MA\\apisix\\apisix";
		final var lua_test_suite_51 = "D:\\MA\\lua5.1-tests";
		final var lua_test_suite_52 = "D:\\MA\\lua-5.2.0-tests";
		
		// neovim plugins
		final var nvimDbee = "D:\\MA\\repos\\nvim_plugins\\nvim-dbee"; // has lots of go code
		final var mason = "D:\\MA\\repos\\nvim_plugins\\mason.nvim"; // at least Lua 5.3
		final var lualine = "D:\\MA\\repos\\nvim_plugins\\lualine.nvim";
		final var telescope = "D:\\MA\\repos\\nvim_plugins\\telescope.nvim";
		final var trouble = "D:\\MA\\repos\\nvim_plugins\\trouble.nvim";
		
		final var temp_testfolder = "D:\\MA\\repos\\temp";
		
		var resourceSet = new Lua52Parser().parse(Paths.get(lua_test_suite_52));
		
		var counter = new AtomicInteger();
		resourceSet.getResources().forEach(r -> 
			r.getAllContents().forEachRemaining(
					modelElement -> counter.getAndIncrement()
			)
		);
		

		//System.out.println(resourceSet);
		System.out.println(counter.get());
	}
	
	@Test
	public void luaTestSuiteTest() throws IOException {
		final var lua_test_suite_52 = "D:\\MA\\lua-5.2.0-tests"; // TODO: situate in project
		final var apisix = "D:\\MA\\apisix\\apisix";
		final var temp_testfolder = "D:\\MA\\repos\\temp";
		var resourceSet = new Lua52Parser().parse(Paths.get(apisix));
		
		for (var r : resourceSet.getResources()) {
			var outputStream = new ByteArrayOutputStream();
			r.save(outputStream, new HashMap<>());
			var parsedAndSerialized = outputStream.toString();
			
			var originalPath = r.getURI().toFileString();
			String original = Files.readString(Paths.get(originalPath), Charset.forName("UTF-8"));
			
			Assertions.assertTrue(compareNormalizedStrings(original, parsedAndSerialized));
		}
	}
	
	private boolean compareNormalizedStrings(String str1, String str2) {
		var s1 = PreprocessingUtils.removeCommentsAndWhiteSpacesAndNewLines(str1);
		var s2 = PreprocessingUtils.removeCommentsAndWhiteSpacesAndNewLines(str2);
		return s1.equals(s2);
	}
}
