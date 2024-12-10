# Lua Parsing using Xtext 2.37
Lua parser and serializer implemented with Xtext implementing the syntax described in the [Lua 5.2 Reference Manual](https://www.lua.org/manual/5.2/manual.html#9). 
This project was originally based on the grammar from the [Melange Project](http://melange.inria.fr/), but has since been completely overhauled according to the reference manual linked above as well as some ideas from the [Ometa Lua-grammar implementation](https://github.com/progranet/OMeta.Lua).

The grammar was tested using the [lua 5.2 test suite](https://www.lua.org/tests/).

## Setup
In order to run this project, you'll need Eclipse Modeling Tools version 2022-09 with Xtext (tested with version 2.37.0) installed from the marketplace. Java 11 needs to be selected via "Window - Preferences - Java - Installed JREs".
1. Clone the repository.
2. Import the projects via "File - Import... - General - Existing projects from workspace".
3. Execute the file `org.xtext.lua52/src/GenerateLua52.mwe2` by right-clicking it and selecting "Run - Run as - MWE2 Workflow". This leads to the generation of the xText artifacts.

## General Approach
The following gives an overview over the project idea and structure.

### Xtext grammar (Lua.xtext)
The goal in designing the grammar was to represent the grammar from the Lua reference manual (see link above) as closely as possible. 
Since the reference grammar is given in extended BNF while Xtext requires LL* grammars, left recursion and ambiguities had to be removed, along with other adjustments.

### References
References are implemented using the types `Referenceable` and `Referencing`, where a `Referencing` references a `Referenceable`. A `Referencing` always references the next element in the reference chain, which ends with a reference to e.g. a function declaration or a value in an assignment.
Examples:
a) Comments in code snippet
```
    var = {} -- var references the table constructor on the rhs
    a = var -- a references var on the rhs, var references var in first assignment
```
b) `f` references `func` on the rhs, `func` being of type `Var` (also a `Referenceable`) which references the function declaration in the first statement.
```
    function func() end
    f = func 
```

## Tests & Evaluation
More details can be found in the README of the test package `org.xtext.lua.tests`.
1. The tests in `org.xtext.lua52.tests` can be executed with right click -> Run As -> JUnit Test:
    - `LuaParsingTest.xtend` for syntactical tests
    - `LuaLocalScopingTest.xtend` for local scoping/reference resolution tests.
    - `LuaGlobalScopingTest.xtend` for global scoping/reference resolution tests.
2. To test the grammar on a Lua project, add a `EvalProjectConfig` to `TestConfig.EVAL_PROJECT_CONFIGS` and execute the `LuaParserTest.evaluationTest`.

## Known limitations
 - Lua allows for a special comment if the first line starts with a `#`. This can currently not be parsed by this grammar and will lead to an error being shown in the console.
 - Lua allows for multi-level comments, denoted by an opening long bracket `[`, followed by an arbitrary number of `=`s, followed by another opening long bracket. The comment ends with two closing long brackets separated by an equal amount of `=`s. This is currently hard coded for the different amounts of `=` and can currently only be parsed for up to 7 `=`s by this grammar.
 - Some illegal Lua syntax will still successfully be parsed, e.g. `a=10r30`
 - The file `all.lua` from the original lua 5.2 test suite fails the tests because of an empty single line comment.

## TODOs
 - Fix the limitations mentioned above if possible.
 - Add more documentation
 - Grammar cleanup
 - Tests cleanup
