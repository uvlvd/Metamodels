package org.xtext.lua.tests;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.xtext.lua.LuaParser;

public class LuaParserTest {
	@Test
	public void minimalParseTest() throws IOException {
		var resourceSet = new LuaParser().parse(Paths.get("YOUR PATH TO A DIRECTORY WITH LUA CODE"));
		System.out.println(resourceSet);
	}
}
