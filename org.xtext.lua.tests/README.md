# Overview
Following [this stackoverflow post](https://stackoverflow.com/questions/62162295/why-does-xtext-no-longer-generates-xtend-classes) which references [this blog post](https://blogs.itemis.com/en/xtext-2.20-release),
the Test (util) classes other than the classes containing the test cases are written in Java instead of Xtend.
The exception is LuaParserTest.java, which contains tests for the parsing of complete projects. The parsing of projects
is expected to be executed using the LuaParser class from Java code.

 - `DefaultTestingParserHelper.java` is used to parse the tested code snippets (SUTs) and perform default ("basic") tests performed for many of the tests, e.g. that all references/proxies are resolved in case of scoping tests.
 - `TestUtil.java` contains utility function for testing.
 - `LuaParsingTest.xtend` contains unit tests for the correctness of the syntactical parsing without reference rsolution.
 - `LuaLocalScopingTest.xtend` contains unit tests for resolution of local linking and scoping (i.e. reference resolution).
 - `LuaGlobalScopingTest.xtend` contains unit tests for resolution of global linking and scoping (i.e. reference resolution).
 - `LuaParserTest.java` contains tests on complete Lua projects. These test will output evaluation data like percentage of resolved (non-mocked) references. The evaluated projects can be configuret in `TestConfig.EVAL_PROJECT_CONFIGS`

# Requirements for projects to be parsed
 - `UTF-8` encoding
 - no special first-line comments (first line starting with `#`) in any files

For the evaluation tests in `LuaParserTest.java` to run succesfully the input projects need to be encoded using `UTF-8`.
The Lua test suite files were therefore converted to `UTF-8` using the python script in the `test_data` folder.
Additionally, all special first-line comments (first line starting with a `#` in Lua files) were removed, since these comments can 
currently not be parsed (see limitations of code model in global README and `org.xtext.lua.Lua.xtext`).

There also seems to be a bug where an empty comment breaks the parser. As a result, a line break was inserted at line 123 of the test-suite files `all.lua`:
```
--
-- redefine dofile to run files through dump/undump
--
local function report (n) print("\n***** FILE '"..n.."'*****") end
```
The `local function report [...]` is not parsed (appearently treated as part of the comment) without a line-break separating the comment and the line after the comment. This issue only arises in `LuaParserTest.java`, not for the same code in `LuaParsingTest.xtend`.

# Configuration file
 - `TestUtil.java` may be modified to configure the tests, e.g. to enable/disable the output of a String representation of the Models resulting from parsing the Lua code snippets in the test cases.

# Terminology
 - `SUT` = snippet/subject under test

# TODOS and known problems:
 - `LuaParsingTest.invalidNumberTest` is currently not executed, because the grammar currently allows for invalid numbers (non-conformant to Lua syntax); since this is not problematic for the CIPM use-case, this issue was not yet resolved.
 - Currently, there are test cases in `LuaLocalScopingTest.xtend` that fail because the tested functionality was not yet implemented. These cases are marked with a TODO comment.