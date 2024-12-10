package org.xtext.lua.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith

/**
 * Class containing tests for the parsing of the Lua syntax by the xText grammar.
 * @author jsaenz
 */
 @ExtendWith(InjectionExtension)
 @InjectWith(LuaInjectorProvider)
class LuaParsingTest {
	
	@Inject
	DefaultTestingParserHelper parserHelper

	@Test
	def void ifThenElseTest() {
		// SUT = Snippet Under Test 
		val SUT = '''
			num = 666   

			if num < 667 then
				num = 2
			elseif num < 667 then
				num = 3
			else
				num = 4
			end
			
			if not x then x = v end
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	// Test variable names
	@Test
	def void simpleVariableNamesTest() {
		val SUT = '''
			a, b, c = 1, nil, "string"
			a = nil
			a1 = nil
			camelCase = nil
			snake_case = nil
			a,b,c = 1
			abc, def = 1, 1
			c = a
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void memberAccessAssignmentTest() {
		val SUT = '''
			a.x = 1; 
			a.y = 0
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void tableAccessAssignmentTest() {
		val SUT = '''
			a[x] = 1; 
			a["y"] = 0
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}

	@Test
	def void tableConstructorTest() {
		val SUT = '''
			b = {};  
			b = {1,2,3}
			b = {1,2;3}
			b = {1,2;3;}
			b = {1,2,3,}
			b = {a=1,b=2,c=3}
			b = {a == nil, b > 2}
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}	

	@Test
	def void expTest() {
		val SUT = '''
			a = 1 + 2 * 3 / (100-x)^2 %y
			
			concat = "hello" .. " world"
			
			isFalse = a or 1
			test = a and 1
			l = 1 < 2
			lt = 1 <= 2
			g = 1 > 2
			gt = 1 >= 2
			
			eq = a == a
			neq = a ~= b
			
			n = not a
			
			-- function expression/assignment
			func = function (x) end
			
			assert(2^-2 == 1/4 and -2^- -2 == - - -4);

		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void functionCallTest() {
		val SUT = '''
			-- tests function call without assignment
			
			f(a, b, c)
			-- test with assignment
			result = f(a, b, c)
			
			a["hello"]()()
			
			a = b or c and d
			otherResult = a.t:x(2,3)

			a = f(x).y
			
			f(x).y["test"]:func() = "hello" -- should fail
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void doBlockEndTest() {
		val SUT = '''
			do end
			do
				a = 4
				b = a^a
			end
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void whileTest() {
		val SUT = '''
			while arg[i] do i=i-1 end
			while true do
				print("Endless loop")
			end
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void repeatUntilTest() {
		val SUT = '''
			-- silly loops
			repeat until 1; repeat until true;
			
			x = 1;
			repeat
			    a;
			    if b==1 then local b=1; x=10; break
			    elseif b==2 then x=20; break;
			    elseif b==3 then x=30;
			    else local a,b,c,d=math.sin(1); x=x+1;
			    end
			 until x>=12;
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void numericForTest() {
		val SUT = '''
			-- without step
			for i=1,#msgs do
				print(msgs[i])
			end
			
			-- with step
			for i=1,#msgs,2 do
				print(msgs[i])
			end
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void genericForTest() {
		val SUT = '''
			for n in pairs(_G) do a[n] = 1 end
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void functionDeclarationTest() {
		val SUT = '''
			function functionName(x) end
			
			function f(x)
				return function (y)
					return function (z) return w+x+y+z end
				end
			end
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}

	@Test
	def void localFunctionDeclarationTest() {
		val SUT = '''
			local function formatmem (m)
			  if m < 1024 then return m
			  else
			    m = m/1024 - m/1024%1
			    if m < 1024 then return m.."K"
			    else
			      m = m/1024 - m/1024%1
			      return m.."M"
			    end
			  end
			end
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void localNaemListDeclarationTest() {
		val SUT = '''
			local a, b, c = 1, 2, "string" or num

		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	
	@Test
	def void longStringLiteralTest() {
		val SUT = '''
			b = [[abcde12 or 12]]
			a = [==[ lorem ipsum dolor sit amet \\2 ]==]
			a = [==[]=]==]
			a = "]="
			
			a = [==[[=[]]=][====[]]===]===]==]
			a = [==[[===[[=[]]=][====[]]===]===]==]
			a = "[===[[=[]]=][====[]]===]==="
			
			a = [====[[===[[=[]]=][====[]]===]===]====]
			a = "[===[[=[]]=][====[]]===]==="
			
			a = [=[]]]]]]]]]=]
			a = "]]]]]]]]"
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void commentTest() {
		val SUT = '''
			do --[
			end
			
			do --[======
			end
			
			--[[
			do
			]]
			
			--[=[==========[]]
			do
			]=]
			
			-- test comment
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void numberTest() { 
		val SUT = '''
			a = 10
			a = .7
			a = 1.1243
			a = 10e3
			a = 10.3e-2
			a = 0xfff
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	
	// TODO: This test should fail because the SUT contains invalid Lua syntax.
	//       However, for our use-case (parsing existing projects to code-models) we can
	//       assume that only valid Lua syntax is parsed (since the existing projects would
	//       otherwise not be executable).
	//@Test
	def void invalidNumberTest() { // should fail
		val SUT = '''
			a = 10r30
			a = 10f(x)
		'''
		parserHelper.parseAndPerformBaseTest(SUT, true)
	}
	
	@Test
	def void invalidExpTest() { // should fail
		val SUT = '''
			a = 2^^3
		'''
		parserHelper.parseAndPerformBaseTest(SUT, true)
	}
	
	@Test
	def void requireTest() { 
		val SUT = '''
			require("hello.world")
			require"hello.world"
			require("hello.world").subFunction
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	
	// Tests for Lua5.2 syntax
	@Test
	def void lua52NumberTest() { 
		val SUT = '''
			a = 3
			a = 3.0
			a = 3.1416
			a = 314.16e-2
			a = 0.31416E1
			a = 0xff
			a = 0x0.1E
			a = 0xA2.3
			a = 0xA23p-4
			a = 0X1.921FB54442D18P+1
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void gotoTest() { 
		val SUT = '''
			do
			  goto l1
			  local a = 23
			  x = a
			  ::l1::;
			end
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	/*
	 * From lua reference: 
	 * "Function calls and assignments can start with an open parenthesis. 
	    This possibility leads to an ambiguity in Lua's grammar. Consider the following fragment:
    		  a = b + c
    		 (print or io.write)('done')
		The grammar could see it in two ways:
     		 a = b + c(print or io.write)('done')
    		 a = b + c; (print or io.write)('done')
    	The current parser always sees such constructions in the first way, 
    	interpreting the open parenthesis as the start of the arguments to a call."
	 */
	@Test
	def void ambiguityTest() { 
		val SUT = '''
			a = b + c
			(print or io.write)('done')
			
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	
	
	// Test for special cases/bugs found during development
	
	/*
	 * Test for a specific bug encountered during development:
	 * When the parenthesized expression alternative does not have an attribute assignment: '(' Exp ')',
	 * the "false" in this code block was serialized in parentheses. (Fix: '(' exp=Exp ')') 
	 */
	@Test
	def void bugTest() {
		val SUT = '''
		if res_put:find("error", 1, true) then
		    is_success = false
		    if (index == host_count) then
		       errmsg = str_format("got malformed key-put message: \"%s\" from etcd \"%s\"\n",
		                            res_put, put_url)
		       util.die(errmsg)
		    end
		    break
		end

		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}	
	
	@Test
	def void firstlineShebangTest() {
		val SUT = '''
			#! line allowing the use of Lua as a script interpreter in Unix systems
			a = 1
			
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
	/**
	 * This code leads to an error in LuaParserTest.java (code is part of all.lua from the Lua 5.2 test suite), but not here.
	 * // TODO
	 */
	@Test
	def void commentIssueTest() {
		val SUT = '''
			  --
			  -- redefine dofile to run files through dump/undump
			  --
			  local function report (n) print("\n***** FILE '"..n.."'*****") end
			  local olddofile = dofile
		'''
		parserHelper.parseAndPerformBaseTest(SUT)
	}
	
}
