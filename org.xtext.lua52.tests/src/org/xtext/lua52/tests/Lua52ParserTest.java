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
	
	/**
	 * The path to the project folder that is to be tested, e.g. "C:\\lua-5.2.0-tests".
	 */
	private final static String SUT_PATH = "C:\\path\\to\\folder"; // subject under test, i.e. a Lua project (e.g. lua 5.2 test suite https://www.lua.org/tests/)
	
	@Test
	/**
	 * Tests the parsing of the project from SUT_PATH.
	 * @throws IOException
	 */
	public void luaTestSuiteTest() throws IOException {
		var resourceSet = new Lua52Parser().parse(Paths.get(SUT_PATH));
		
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
