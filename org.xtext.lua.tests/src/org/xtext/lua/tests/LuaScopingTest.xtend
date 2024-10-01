/*
 * generated by Xtext 2.34.0
 */
package org.xtext.lua.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import org.xtext.lua.lua.Chunk
import org.eclipse.emf.ecore.EObject
import java.io.ByteArrayOutputStream
import org.xtext.lua.PreprocessingUtils
import org.eclipse.xtext.resource.DerivedStateAwareResource

@ExtendWith(InjectionExtension)
@InjectWith(LuaInjectorProvider)
class LuaScopingTest {
	@Inject
	ParseHelper<Chunk> parseHelper
	
	
	def static String dump(EObject mod_, String indent) {
	    //var res = indent + mod_.toString.replaceFirst ('.*[.]impl[.](.*)Impl[^(]*', '$1 ')
	 	var res = indent + mod_.toString.replaceFirst ('.*[.]impl[.](.*)Impl[@](.*)[^(]*', '$1 $2')
	    for (a :mod_.eCrossReferences) 
	        //res += ' ->' + a.toString().replaceFirst ('.*[.]impl[.](.*)Impl[^(]*', '$1 ')
	        res += ' ->' + a.toString().replaceFirst ('.*[.]impl[.](.*)Impl[@](.*)[^(]*', '$1 $2')
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
	
	
	
	
	
	@Test
	def void scopingTest() { 
		val SUT = '''
			a = 1
			b = a
			
			--for num = 1, 10 do
			--	a = a + num
			--end
			
			--for i, j in {1,2,3}, {4,5,6} do
			--	b = i+j
			--end
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
	
	@Test
	def void scopingTableAccessStringLiteralTest() { 
		val SUT = '''
			a = {}
		    a["member"] = 1
		    b = a["member"]
		    c = a.member
		    str = "member" 
		    --a[str] = 1 --TODO
		    --f = a.member
		    --str2 = "2"
		    d = a[str]
		   -- a["hello.world"] = 2
		   -- b = a["hello.world"]
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	@Test
	def void scopingTableAccessNumberLiteralTest() { 
		val SUT = '''
			a = {}
		    a[0] = 2
		    b = a[0]
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	
	/**
	 *  d should be resolvable to 1, e.g.
	 *  d -> a (in line 4), which contains [str], which points to a["member"], which points to 1.
	 */
	@Test
	def void scopingTableAccessStringLiteralVariableTest() { 
		val SUT = '''
			a = {}
		    a["member"] = 1
		    str = "member" 
		    d = a[str] 
		'''
		val result = parseHelper.parse(SUT)

		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	@Test
	def void scopingTableAccessNumberLiteralVariableTest() { 
		val SUT = '''
			a = {}
		    a[0] = 1
		    str = 0 
		    d = a[str] 
		'''
		val result = parseHelper.parse(SUT)

		System.out.println(dump(result, ""));
		check(result, SUT)
	}

	
	@Test
	def void scopingMissingValueInExpressionTest() { 
		val SUT = '''
			a, b = 1 -- b should reference a newly created ExpNil
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	@Test
	def void scopingLastAssignmentTest() { 
		val SUT = '''
			a = 1
			a = 2
			a = 3
			b = a -- b should reference a from a=3
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	@Test
		def void scopingPartialTableAccessTest() { 
		val SUT = '''
			b = {}
			b.temp = 1 
			a = {}
			a.b = b
			c = a.b.temp
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
		
	// TODO: the result of this test should correspond to its goal, i.e.
	// it should fail if the the second assignment is not referenced by the b = a
	@Test
	def void scopingAssignmentPrecedenceTest() { 
		val SUT = '''
		a = "this is a candidate"
		a = "this should be the chosen candidate"
		b = a
		a = "this is not a candidate"
		      
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
		
	@Test
	def void scopingFunctionDeclarationTest() { 
		val SUT = '''
			a = {}
			function a.f() end
			
			c = a.f
			
			b = {}
			function b.memberFunc() return {["member"] = 1} end
			
			a.x = b
			a.x.memberFunc()
			
			--a.x.memberFunc()["member"]
			
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	
	@Test
	def void scopingNumericForTest() { 
		val SUT = '''
		a = {}
		for i = 1, 10 do
		   a[i] = i
		end
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	@Test
	def void scopingGenericForTest() { 
		val SUT = '''
		a = {}
		b = {"hello", "world"}
		
		function pairs(arr) 
			-- avoids error from library functions not yet being supported by scoping
		end
		
		for k, v in pairs(b) do
		   a[k] = v
		end
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	@Test
	def void scopingParamArgsTest() { 
		val SUT = '''
		function func(arg)
		end
		
		a = {}
		a.b = "test"
		
		func(a)
		func(a.b)
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}

	@Test
	def void scopingFuncBodyArgsTest() { 
		val SUT = '''
		function func(arg1, arg2) 
			a = arg1
			b = arg2
		end
		
		func = function (a, b)
			a = a
			-- b = b.b -- TODO: we dont know if b is a table, but this kind of access would indicate so.. -> implement trivial recovery?
			b = b
		end	
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	@Test
	def void scopingLocalFuncBodyArgsTest() { 
		val SUT = '''
		local function func(arg1, arg2) 
			a = arg1
			b = arg2
		end
		
		local func = function (a, b)
			a = a
			-- b = b.b -- TODO: we dont know if b is a table, but this kind of access would indicate so.. -> implement trivial recovery?
			b = b
		end	
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	@Test
	def void scopingLocalFuncDeclarationTest() { 
		val SUT = '''
		local function func() end
		local function func2() end	
		
		a = func
		b = func2
		
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	@Test
	def void scopingLocalAssignmentTest() { 
		val SUT = '''
		local a
		local b,c = 1
		local func = function () end	
		
		l = b
		k = a
		m = c
		f = func
		
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	@Test
	def void scopingTableConstructorTest() { 
		val SUT = '''
		a = {["one"] = 1, [2] = 2, 3, four = 4, 5}
		local a = {["one"] = 1, [2] = 2, 3, four = 4, 5}
		one = a.one
		two = a[2] --should be 5, since the "2" index is overwritten by the seconde ExpField (5)
		three = a[1]
		four = a.four
		four2 = a["four"]
		five = a[2]
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}

	@Test
	def void scopingNestedTableConstructorTest() { 
		val SUT = '''
		--a = {b = {c = {member = "hello world"}}}
		--d = a.b.c.member
		
		m = {{{"hello again"}}}
		n = m[1][1][1]
		--print(m[1][1][1])
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	@Test
	
	def void scopingtemptempTest() { 
		val SUT = '''
		a = {[1] = {[2] = {member = "hello world"}}}
		d = a[1][2]["member"]
		
		--m = {{{"hello again"}}}
		--n = m[1][1][1]
		--print(m[1][1][1])
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	
	// TODO: this should test that the TableConstructor fields in the functioncall are not candidates for the 
	// Assignment b=a.
	@Test
	def void scopingNonReferenceableTableConstructorTest() { 
		val SUT = '''
		func = function(a) end
		func{1,2,3}
		a = 1
		b = a
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	@Test
	def void scopingTest2() { 
		val SUT = '''
		b = {}
		b.member = 1
		b["member2"] = 2
		a = b["member2"]
		a = b.member2
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	@Test
	def void scopingTempTest() { 
		val SUT = '''
			a = {}
			--a["member"] = {}
			--a["member"].b = "b"
			--a[1+1] = "temps" --TODO
			--b = a["member"]
			--c = a.member
			str = "member" 
			a[str] = 1 --TODO
			f = a.member
			--d = a[str]
		
			--b = {}
			--b.temp = 1 
			--a = {}
			--a.b = b
			--c = a.b.temp
			
			
			--func = function () return 0 end
			--a = {}
		   -- a[0] = 2
		   -- b = a[func()]
		   -- a[func()] = 1
		   -- str = "member"
		   -- a[str] = 1
		   -- c = a.member
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}
	
	
	// TODO: never allow a rhs to reference its own lhs, 
	// needs to be lhs of other Assignment
	@Test
	def void scopingTemp3Test() { 
		val SUT = '''
			a = a
		'''
		val result = parseHelper.parse(SUT)
		System.out.println(dump(result, ""));
		check(result, SUT)
	}

	
}
