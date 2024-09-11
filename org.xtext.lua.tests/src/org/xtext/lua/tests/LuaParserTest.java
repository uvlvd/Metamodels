package org.xtext.lua.tests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xtext.lua.LuaParser;
import org.xtext.lua.PreprocessingUtils;


public class LuaParserTest {
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
		
		var resourceSet = new LuaParser().parse(Paths.get(lua_test_suite_52));
		
		printNumberOfModelElements(resourceSet);
	}
	
	@Test
	public void luaTestSuiteTest() throws IOException {
		final var lua_test_suite_52 = "D:\\MA\\lua-5.2.0-tests"; // TODO: situate in project
		final var apisix = "D:\\MA\\apisix\\apisix";
		final var temp_testfolder = "D:\\MA\\repos\\temp";
		var resourceSet = new LuaParser().parse(Paths.get(temp_testfolder));
		
		for (var r : resourceSet.getResources()) {
			var outputStream = new ByteArrayOutputStream();
			r.save(outputStream, new HashMap<>());
			var parsedAndSerialized = outputStream.toString();
			
			var originalPath = r.getURI().toFileString();
			String original = Files.readString(Paths.get(originalPath), Charset.forName("UTF-8"));
			
			var strsEqual = compareNormalizedStrings(original, parsedAndSerialized);
			if (!strsEqual) {
				System.out.println(originalPath);
			}
			
			Assertions.assertTrue(strsEqual);
		}
		
		printNumberOfModelElements(resourceSet);

	}
	
	private boolean compareNormalizedStrings(String str1, String str2) {
		var s1 = PreprocessingUtils.removeCommentsAndWhiteSpacesAndNewLines(str1);
		var s2 = PreprocessingUtils.removeCommentsAndWhiteSpacesAndNewLines(str2);
		var result = s1.equals(s2);
		
		if (!result) {
			var diffStr = StringUtils.difference(s2, s1);
			System.out.println(s1);
			System.out.println(s2);
			System.out.println(diffStr.substring(0, diffStr.length() < 101 ? diffStr.length() : 100));
		}
		
		return result;
	}
	
	private void printNumberOfModelElements(ResourceSet resourceSet) {
		var counter = new AtomicInteger();
		resourceSet.getResources().forEach(r -> 
			r.getAllContents().forEachRemaining(
					modelElement -> counter.getAndIncrement()
			)
		);

		System.out.println("Total number of resource elements: " + counter.get());
	}
}
