# Lua Parsing using Xtext 2.34

Lua parser and serializer implemented with xText implementing the syntax described in the [Lua 5.2 Reference Manual](https://www.lua.org/manual/5.2/manual.html#9). 
This project was initially based on the grammar from the [Melange Project](http://melange.inria.fr/), but has since been completely overhauled according to the reference manual linked above as well as some ideas from the [Ometa Lua-grammar implementation](https://github.com/progranet/OMeta.Lua).

The grammar was tested using the [lua 5.2 test suite](https://www.lua.org/tests/).


## Limitations
 - Lua allows for a special comment if the first line starts with a `#`. This can currently not be parsed by this grammar and will lead to an error being shown in the console.
 - Lua allows for multi-level comments, denoted by an opening long bracket `[`, followed by an arbitrary number of `=`s, followed by another opening long bracket. The comment ends when with two closing long brackets separated by an equal amount of `=`s. This can currently only be parsed for up to 7 `=`s by this grammar.
## Setup

In order to run this project, you'll need Eclipse Modeling Tools version 2022-09 with xText installed from the marketplace. Java 11 needs to be selected via "Window - Preferences - Java - Installed JREs".
1. Clone the repository.
2. Import the projects via "File - Import... - General - Existing projects from workspace".
3. Execute the file `org.xtext.lua52/src/GenerateLua52.mwe2` by right-clicking it and selecting "Run - Run as - MWE2 Workflow". This leads to the generation of the xText artifacts.
4. You may now execute the tests in `org.xtext.lua52.tests/src/org/xtext/lua52/tests/Lua52ParsingTest.xtend`. 
5. If you want to test the grammar on a Lua project, set the absolute path for the `SUT_PATH` variable in `org.xtext.lua52.tests/src/org/xtext/lua52/tests/Lua52ParserTest.java` and execute the test in that file. Eventual errors are shown in the console.


## TODOs
 - Fix the limitations mentioned above if possible.
 - Add more documentation
