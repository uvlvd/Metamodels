package org.xtext.lua.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import org.xtext.lua.lua.Assignment
import org.xtext.lua.lua.Referencing
import org.xtext.lua.lua.Var
import org.xtext.lua.lua.ExpNumberLiteral
import org.xtext.lua.lua.MemberAccess
import org.xtext.lua.lua.ExpNil
import org.xtext.lua.lua.ExpFunctionDeclaration
import org.xtext.lua.lua.LocalAssignment

/**
 * Class containing tests for the local scoping (i.e. reference resolution). These tests together
 * with the tests contained in LuaGlobalScopingTest.xted form the test suite T_RR referenced in the thesis.
 * @author jsaenz
 */
 // TODO: Most of the tests in this class do not contain testing functionalities other than 
 // the base tests performed by DefaultTestingParserHelper due to the prioritisation of other requirements. 
 // Examples of how to extend the test cases can be found e.g. in the first few test cases, where a more detailed
 // testing was implemented.
@ExtendWith(InjectionExtension)
@InjectWith(LuaInjectorProvider)
class LuaLocalScopingTest {
	@Inject
	DefaultTestingParserHelper parseHelper
	
	@Test
	def void variableAssignmentTest() { 
		val SUT = '''
			a = 1
			b = a
		'''
		val result = parseHelper.parseAndPerformBaseScopingTest(SUT)
		
		val firstAssignment = result.block.stats.get(0) as Assignment
		val secondAssignment = result.block.stats.get(1) as Assignment
		
		val a = firstAssignment.getVars.get(0) as Var
		val b = secondAssignment.getVars.get(0) as Var
		val secondA = secondAssignment.getExpList.getExps.get(0) as Referencing
		
		Assertions.assertEquals(b.getRef, secondA)
		Assertions.assertEquals(secondA.getRef, a)
	}
	
	// Example from Feature Path discussion in thesis
	@Test
	def void TX_featurePathTest() {
		val SUT = '''
			local get_A = function()
			    local t = {}
			    t.x = 10
			    return t
			end
			
			local a = get_A()
			local b = a
			local x = b.x
		'''
		val result = parseHelper.parseAndPerformBaseScopingTest(SUT)
		val firstAssignment = result.block.stats.get(0) as LocalAssignment
		val decl = firstAssignment.getExpList.getExps.get(0) as ExpFunctionDeclaration
		val txAssignment = decl.getBody().getBlock().getStats().get(1) as Assignment
		val referencedXPrefix = txAssignment.getVars.get(0) as Var
		val referencedX = referencedXPrefix.getSuffixExp() as MemberAccess
		
		val lastAssignment = result.block.stats.get(3) as LocalAssignment
		val b = lastAssignment.getExpList().getExps().get(0) as Var
		val referencingX = b.getSuffixExp() as MemberAccess
		Assertions.assertEquals(referencingX.getRef, referencedX)
	}
	
	// expected to fail
	@Test
	def void TY_unresolvableReferenceTest() {
		val SUT = '''
			get_A = function()
				if math.random() > 0.5 then -- random float in [0,1)
					return {x = 0} -- new table containing field ’x’
				else
					return {y = 0} -- new table containing field ’y’
				end
			end
			y = get_A().y
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT, true) // expected to fail
	}
	
	@Test
	def void TZ_expressionEvaluation() {
		val SUT = '''
			v = "hello"
			v2 = 1
			
			a = {hello = "hello", [1] = 1}

			b = a[v]
			b = a[v2]
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT) 
	}
	
	@Test
	def void multipleAssignmentTest() {
		val SUT = '''
			a, b, c = 10, 11, 12
		'''
		val result = parseHelper.parseAndPerformBaseScopingTest(SUT)
		
		val assignment = result.block.stats.get(0) as Assignment
		
		val a = assignment.getVars.get(0) as Var
		val b = assignment.getVars.get(1) as Var
		val c = assignment.getVars.get(2) as Var
		
		val aValue = assignment.getExpList.getExps.get(0) as ExpNumberLiteral
		val bValue = assignment.getExpList.getExps.get(1) as ExpNumberLiteral
		val cValue = assignment.getExpList.getExps.get(2) as ExpNumberLiteral
		
		Assertions.assertEquals(a.getRef, aValue)
		Assertions.assertEquals(b.getRef, bValue)
		Assertions.assertEquals(c.getRef, cValue)
	}
	
	@Test
	def void memberAssignmentTest() { 
		val SUT = '''
			a = {}
			a.member = {}
			a.member.secondMember = 10
			b = a
			c = a.member
			d = a.member.secondMember
			
		'''
		val result = parseHelper.parseAndPerformBaseScopingTest(SUT)
		
		val firstAssignment = result.block.stats.get(0) as Assignment
		val secondAssignment = result.block.stats.get(1) as Assignment
		val thirdAssignment = result.block.stats.get(2) as Assignment
		
		// TODO: create FeaturePath class and use here to get the different parts
		val a = firstAssignment.getVars.get(0) as Var
		val member = (secondAssignment.getVars.get(0) as Var).getSuffixExp as MemberAccess
		val secondMember = ((thirdAssignment.getVars.get(0) as Var).getSuffixExp as MemberAccess).getSuffixExp as MemberAccess
		
		val dAssignment = result.block.stats.get(5) as Assignment
		val d = dAssignment.getVars.get(0) as Var
		
		val da = d.getRef as Var // a on rhs of d = a.member.secondMember
		val dMember = da.getSuffixExp as MemberAccess // member on rhs of d = a.member.secondMember
		val dSecondMember = dMember.getSuffixExp as MemberAccess // secondMember on rhs of d = a.member.secondMember
		
		Assertions.assertEquals(da.getRef, a)
		Assertions.assertEquals(dMember.getRef, member)
		Assertions.assertEquals(dSecondMember.getRef, secondMember)
	}
	
	@Test
	def void tableAccessStringLiteralTest() { 
		val SUT = '''
			a = {}
			a["member"] = 1
			b = a["member"]
			c = a.member
			str = "member" 
			a[str] = 1 --TODO
			f = a.member
			str2 = "2"
			d = a[str]
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void tableAccessDotInStringLiteralTest() { 
		val SUT = '''
			a = {}
			a["hello.world"] = 2
			b = a["hello.world"]
		'''
		// TODO: fix, dots in String seem to cause problems, does not correctly point to the assignment a["hello.world"]
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void tableAccessNumberLiteralTest() { 
		val SUT = '''
			a = {}
			a[0] = 2
			b = a[0]
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	
	/**
	 *  d should be resolvable to 1, e.g.
	 *  d -> a (in line 4), which contains [str], which points to a["member"], which points to 1.
	 */
	@Test
	def void tableAccessStringLiteralVariableTest() { 
		val SUT = '''
			a = {}
			a["member"] = 1
			str = "member" 
			d = a[str] 
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void tableAccessNumberLiteralVariableTest() { 
		val SUT = '''
			a = {}
			a[0] = 1
			str = 0 
			 d = a[str] 
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}

	
	@Test
	def void missingValueInExpressionTest() { 
		val SUT = '''
			a, b = 1 -- b should reference a newly created ExpNil
		'''
		val result = parseHelper.parseAndPerformBaseScopingTest(SUT)
		val assignment = result.block.stats.get(0) as Assignment
		val b = assignment.getVars.get(1) as Var
		// could also test for SyntheticExpNil, but the general case seems better since it should
		// not matter how b gets assigned the Nil value, as long as it is a Nil value
		Assertions.assertTrue(b.getRef instanceof ExpNil)
	}
	
	@Test
	def void assignmentPrecedenceTest() { 
		val SUT = '''
			a = 1
			a = 2
			b = a -- b should reference a from a=2
			a = 3
		'''
		val result = parseHelper.parseAndPerformBaseScopingTest(SUT)
		val secondAssignment = result.block.stats.get(1) as Assignment
		val thirdAssignment = result.block.stats.get(2) as Assignment
		
		val expectedA = secondAssignment.getVars().get(0) as Var
		val b = thirdAssignment.getVars().get(0) as Var
		val ba = b.getRef() as Var // a on rhs of b = a
		Assertions.assertTrue(ba.getRef() == expectedA)
	}
	
	@Test
		def void partialTableAccessTest() { 
		val SUT = '''
			b = {}
			b.temp = 1 
			a = {}
			a.b = b
			c = a.b.temp
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	// TODO: need to implement references to function return values 
	@Test
	def void functionDeclarationTest() { 
		val SUT = '''
			a = {}
			function a.f() end
			
			c = a.f
			
			b = {}
			function b.memberFunc() return {["member"] = 1} end
			
			a.x = b
			a.x.memberFunc()
			
			a.x.memberFunc()["member"]
			
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT, true)
	}
	
	
	@Test
	def void numericForTest() { 
		val SUT = '''
		a = {}
		for i = 1, 10 do
		   a[i] = i
		end
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void genericForTest() { 
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
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void paramArgsTest() { 
		val SUT = '''
		function func(arg)
		end
		
		a = {}
		a.b = "test"
		
		func(a)
		func(a.b)
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}

	@Test
	def void funcBodyArgsTest() { 
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
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	// TODO: fix test
	@Test
	def void methodBodyArgsTest() { 
		val SUT = '''
		table = {}
		function table:func(arg1) 
			a = self
			return arg1
		end
		b = table:func()
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void methodBodyOnlySelfArgTest() { 
		val SUT = '''
		function table:f () return self end
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void methodReturnTableTest() { 
		val SUT = '''
		table = {}
		function table:func() 
			a = {test = "test"}
			return a
		end
		b = table:func().test
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void localFuncBodyArgsTest() { 
		val SUT = '''
		local function func(arg1, arg2) 
			a = arg1
			b = a
			c = arg2
		end
		
		local func = function (a, b)
			a = a
			--b = b.b -- TODO: we dont know if b is a table, but this kind of access would indicate so.. -> implement trivial recovery?
			b = b
		end	
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void localFuncDeclarationTest() { 
		val SUT = '''
		local function func() end
		local function func2() end	
		
		a = func
		b = func2
		
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void localAssignmentTest() { 
		val SUT = '''
		local a
		local b,c = 1
		local func = function () end	
		
		l = b
		k = a
		m = c
		f = func
		
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void localAssignmentToReferencingTest() { 
		val SUT = '''
		local a = {member = "member"}
		local b = a
		local c = b.member
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void tableConstructorTest() { 
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
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}

	@Test
	def void nestedTableConstructorTest() { 
		val SUT = '''
		a = {b = {c = {member = "hello world"}}}
		d = a.b.c.member
		
		m = {{{"hello again"}}}
		n = m[1][1][1]
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	
	// TODO: this should test that the TableConstructor fields in the functioncall are not candidates for the 
	// Assignment b=a.
	@Test
	def void nonReferenceableTableConstructorTest() { 
		val SUT = '''
		func = function(a) end
		func{1,2,3}
		a = 1
		b = a
		'''
		val result = parseHelper.parseAndPerformBaseScopingTest(SUT)
		val aAssignment = result.block.stats.get(2) as Assignment
		val bAssignment = result.block.stats.get(3) as Assignment
		val a = aAssignment.getVars().get(0) as Var
		val b = bAssignment.getVars().get(0) as Var
		val ba = b.getRef() as Var // a in b = a
		Assertions.assertTrue(ba.getRef() == a)
		
	}
	
	@Test
	def void gotoLabelTest() {
		val SUT = '''
			do
				::label::
				goto label
			end
			::label::
			goto label
			goto label2
			::label2::
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	/**
	 * Test that label outside of block scope is found if other
	 * labels are present in block.
	 */
	@Test
	def void gotoLabelNameTest() {
		val SUT = '''
			do
				::l2:: 
				goto l3
			end
			::l3::
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void scopeVisibilityTest() { 
		val SUT = '''
		function print(str) end
		
		x = 10                -- global variable 2d000e80
		do                    -- new block
			local x = x         -- new 'x', with value 10 21422231
			print(x)            --> 10
			x = x+1                 -- 39e67516
		    do                  -- another block
		    	local x = x+1     -- another 'x' 1d61c6dc
		    	print(x)          --> 12
		    end
			print(x)            --> 11
		end
		print(x)              --> 10  (the global one)
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	// never allow a rhs to reference its own lhs
	@Test
	def void rhsNotReferencingAssignmentTest() { 
		val SUT = '''
			a = a
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
		val aAssignment = result.block.stats.get(0) as Assignment
		val aLhs = aAssignment.getVars().get(0) as Var
		val aRhs = aAssignment.getExpList().getExps().get(0) as Var
		Assertions.assertFalse(aRhs.getRef == aLhs)
	}
	
	@Test
	def void functionCallReturnFeatureTest() { 
		val SUT = '''
		 m = function()
		 	local _M = {}
		 	_M.first = "first"
		 	_M.second = "second"
		 	_M.table = {}
		 	_M.table.member = "member"
		 	return _M
		 end
		 test = m().first
		 test2 = m().table
		 test3 = test2.member
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	// TODO: returned function defined outside of returning function
	// does not work, see scopingResolveParentBlockReferencedVarTest
	@Test
	def void doubleFunctionCallReturnFeatureTest() { 
		val SUT = '''
		 --local n = function ()
		 --	local _M = {}
		 --	_M.first = "first"
		 --	return _M
		 --end
		 m = function()
		 	local n = function ()
		 		 	local _M = {}
		 		 	_M.first = "first"
		 		    return _M
		 	end
		 	--local _M = {}
		 	--_M.first = "hello"
		 	--_M.second = "hello2"
		 	return n
		 end
		 test = m()().first
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void resolveParentBlockReferencedVarTest() { 
		val SUT = '''
		b = {member = "hello world"}
		do 
		  a = b 
		  c = a.member
		end
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void resolveFeatureInTableAccessTest() { 
		val SUT = '''
		a = "a"
		T = {first = a}
		t = {T.first}
		result = t[1]
		b = result
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}


	@Test
	def void resolveTableFieldReferencingVarAccess() { 
		val SUT = '''
		local a = "a"
		table = {
			[a] = 10
		}
		b = table.a
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}

	@Test
	def void functionDeclarationTest2() { 
		val SUT = '''
		local a = {}
		function a.func() 
			local M = {}
			M.a = 10
			return M
		end

		b = a.func()

		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
}
