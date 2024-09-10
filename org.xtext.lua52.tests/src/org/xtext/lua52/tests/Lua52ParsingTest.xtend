/*
 * generated by Xtext 2.34.0
 */
package org.xtext.lua52.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import org.xtext.lua52.lua52.Chunk
import java.io.ByteArrayOutputStream
import org.eclipse.emf.ecore.EObject
import org.xtext.lua52.PreprocessingUtils

@ExtendWith(InjectionExtension)
@InjectWith(Lua52InjectorProvider)
class Lua52ParsingTest {
	@Inject
	ParseHelper<Chunk> parseHelper

	def static String dump(EObject mod_, String indent) {
	    var res = indent + mod_.toString.replaceFirst ('.*[.]impl[.](.*)Impl[^(]*', '$1 ')
	
	    for (a :mod_.eCrossReferences)
	        res += ' ->' + a.toString().replaceFirst ('.*[.]impl[.](.*)Impl[^(]*', '$1 ')
	    res += "\n"
	    for (f :mod_.eContents) {
	        res += f.dump (indent+"    ")
	    }
	    return res
	}
	
	def void check(Chunk chunk, String original, Boolean expectedToFail) {
		if (!expectedToFail) {
			check(chunk, original)
			return
		}
		
		Assertions.assertNotNull(chunk)
		val errors = chunk.eResource.errors
		Assertions.assertFalse(errors.isEmpty)
	}	

	def void check(Chunk chunk, String original) {
		Assertions.assertNotNull(chunk)
		val errors = chunk.eResource.errors
		Assertions.assertTrue(errors.isEmpty, '''Unexpected errors: «errors.join(", ")»''')
		checkEquality(chunk, original)
	}
	
	def void checkEquality(Chunk chunk, String original) {
		val outputStream = new ByteArrayOutputStream()
		chunk.eResource.save(outputStream, #{})
		val parsedAndPrinted = outputStream.toString()
		
		// strip things like unimportant whitespace, duplicate newlines, etc.
		val origCanonical = bringIntoCanonicalForm(original)
		val parsedAndPrintedCanonical = bringIntoCanonicalForm(parsedAndPrinted)
		
		// further also strip _all_ newlines and whitespace
		// this definitely breaks the file, but that does not matter for the string comparision
		//val origExtremelyCanonical = bringIntoExtremelyCanonicalForm(origCanonical)
		//val parsedAndPrintedExtremelyCanonical = bringIntoExtremelyCanonicalForm(parsedAndPrintedCanonical)
		
		val origExtremelyCanonical = removeComments(original)
		val parsedAndPrintedExtremelyCanonical = removeComments(parsedAndPrinted)

		val equivalence = origExtremelyCanonical.equals(parsedAndPrintedExtremelyCanonical)
		if (!equivalence) {
			System.out.println("===== Original: =====")
			System.out.println(original)
			System.out.println("===== Parsed and serialized: =====")
			System.out.println(parsedAndPrinted)
			
			System.out.println("===== Original canonical (extremely so): =====")
			System.out.println(origExtremelyCanonical)
			System.out.println("===== Parsed and serialized canonical: =====")
			System.out.println(parsedAndPrintedExtremelyCanonical)
		}
		Assertions.assertTrue(equivalence)
	}
	
	// TODO: rename, it replaces all whitespace also
	def String removeComments(String string) {
		val withoutComments = PreprocessingUtils.removeComments(string)
		return PreprocessingUtils.removeAllWhiteSpacesAndNewLines(withoutComments)	 
	}
	
	/**
	 * This brings a string into a form where we can compare it to another of the same form
	 * This prevents false positives from whitespace, comment which would otherwise create
	 * differences between the two strings.
	 */
	def String bringIntoCanonicalForm(String luaCode) {
		// strip comments
		var striped = luaCode.replaceAll("(?m)--[^\n]*\n?", "")

		// to leading and trailingwhite space characters
		striped = striped.replaceAll("(?m)^[\t ]*", "")
		striped = striped.replaceAll("(?m)[\t ]*$", "")

		// only one newline between lines
		striped = striped.replaceAll("[\r\n]+", "\n")

		// no newlines in first and last line 
		striped.trim()
	}

	def String bringIntoExtremelyCanonicalForm(String canonicalForm) {
		// spaces my occur in the original file
		// this breaks strings, but that doesn't matter here
		var canonical = canonicalForm.replaceAll("[\t ]+", "")

		// no newlines in first and last line 
		canonical.replaceAll("\n","")
	}
	
	@Test
	def void ifThenElseTest() {
		// SUT = Snippet Under Test 
		val SUT = '''
			--[[test]]
			num = 666   
			-- comment

			if num < 667 then
				num = 2
			elseif num < 667 then -- comment
				num = 3
			else
				num = 4
			end
			
			if not x then x = v end

		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""))
		check(result, SUT)
	}
	
	// Test variable names
	@Test
	def void varTest_simpleNames() {
		// SUT = Snippet Under Test 
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
		val result = parseHelper.parse(SUT)
		check(result, SUT)
	}
	
	@Test
	def void varTest_dotNotation() {
		val SUT = '''
			a.x = 1; 
			a.y = 0
		'''
		val result = parseHelper.parse(SUT)
		check(result, SUT)
	}
	
	@Test
	def void varTest_squareBracketNotation() {
		val SUT = '''
			a[x] = 1; 
			a["y"] = 0
		'''
		val result = parseHelper.parse(SUT)
		check(result, SUT)
	}
	
	// Test Expressions	
	@Test
	def void expTest_tableConstructor() {
		val SUT = '''
			b = {};  
			b = {1,2,3}
			b = {1,2;3}
			b = {1,2;3;}
			b = {1,2,3,}
			b = {a=1,b=2,c=3}
			b = {a == nil, b > 2}
		'''
		val result = parseHelper.parse(SUT)
		check(result, SUT)
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
		val result = parseHelper.parse(SUT)
		check(result, SUT)
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
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""))
		check(result, SUT)
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
		val result = parseHelper.parse(SUT)
		check(result, SUT)
	}
	
	@Test
	def void whileTest() {
		val SUT = '''
			while arg[i] do i=i-1 end
			while true do
				print("Endless loop")
			end
		'''
		val result = parseHelper.parse(SUT)
		check(result, SUT)
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
		val result = parseHelper.parse(SUT)
		check(result, SUT)
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
		val result = parseHelper.parse(SUT)
		check(result, SUT)
	}
	
	@Test
	def void genericForTest() {
		val SUT = '''
			for n in pairs(_G) do a[n] = 1 end
		'''
		val result = parseHelper.parse(SUT)
		check(result, SUT)
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
		val result = parseHelper.parse(SUT)
		check(result, SUT)
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
		val result = parseHelper.parse(SUT)
		check(result, SUT)
	}
	
	@Test
	def void localNaemListDeclarationTest() {
		val SUT = '''
			local a, b, c = 1, 2, "string" or num

		'''
		val result = parseHelper.parse(SUT)
		check(result, SUT)
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
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ''))
		check(result, SUT)
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
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ''))
		check(result, SUT)
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
		val result = parseHelper.parse(SUT)
		check(result, SUT)
	}
	
	//@Test
	def void invalidNumberTest() { // should fail
		val SUT = '''
			a = 10r30
			a = 10f(x)
		'''
		val result = parseHelper.parse(SUT)
		check(result, SUT, true)
	}
	
	@Test
	def void invalidExpTest() { // should fail
		val SUT = '''
			a = 2^^3
		'''
		val result = parseHelper.parse(SUT)
		check(result, SUT, true)
	}
	
	@Test
	def void requireTest() { 
		val SUT = '''
			require("hello.world")
			require"hello.world"
			require("hello.world").subFunction
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ''))
		check(result, SUT)
	}
	
	// Test for stuff that does not yet work
	@Test
	def void todoest() {
		val SUT = '''
			--assert(debug.getmetatable(x).__gc == F)
			
			--#! shebang line
			--a, b = 10, 11
			--print("hello world")
			--function func(a) return a+a end
			--1f(x)
			
			--do --[
			--end
			
			--assert(2^-2 == 1/4 and -2^- -2 == - - -4); 
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ''))
		check(result, SUT)
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
		val result = parseHelper.parse(SUT)
		check(result, SUT)
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
		val result = parseHelper.parse(SUT)
		check(result, SUT)
	}
	
	// Tests for scoping
	
	@Test
	def void scopingTest() { 
		val SUT = '''
			a = 1
			b = a
			
			for num = 1, 10 do
				a = a + num
			end
			
			for i, j in {1,2,3}, {4,5,6} do
				b = i+j
			end
			--test.member[1].func()[2].field = 1
			--var = 1
			--a = var
			--a.b, a = 2, 3
			--a = 10
			--b = a
			--test.member[1] = t.member[1]
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	@Test
	def void scopingMemberTest() { 
		val SUT = '''
			l = {}
		    l.member, k, j = 10, 11, 12
			a = {}
			a.member = {}
			a.member.secondMember = 10
			b = a
			c = a.member
			d = a.member.secondMember
			x, y = l.member, a.member
			
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
}
