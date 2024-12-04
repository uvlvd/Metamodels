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

/**
 * Class containing tests for the local scoping (i.e. reference resolution).
 * @author jsaenz
 */
 // TODO: Most of the tests in this class do not contain testing functionalities other than 
 // the base tests performed by DefaultTestingParserHelper due to the prioritization of other requirements. 
 // Examples of how to extend the test cases can be found e.g. in the first few test cases, where a more detailed
 // testing is already implemented.
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
	def void scopingTableAccessStringLiteralTest() { 
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
			a["hello.world"] = 2
			b = a["hello.world"]
		'''
		// TODO: this currently fails in the last two rows, the TableAccess b = a["hello.world"]
		// does not correctly point to the assignment a["hello.world"]
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void scopingTableAccessNumberLiteralTest() { 
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
	def void scopingTableAccessStringLiteralVariableTest() { 
		val SUT = '''
			a = {}
			a["member"] = 1
			str = "member" 
			d = a[str] 
		'''
		parseHelper.parseAndPerformBaseScopingTest(SUT)
	}
	
	@Test
	def void scopingTableAccessNumberLiteralVariableTest() { 
		val SUT = '''
			a = {}
		    a[0] = 1
		    str = 0 
		    d = a[str] 
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}

	
	@Test
	def void scopingMissingValueInExpressionTest() { 
		val SUT = '''
			a, b = 1 -- b should reference a newly created ExpNil
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void scopingLastAssignmentTest() { 
		val SUT = '''
			a = 1
			a = 2
			a = 3
			b = a -- b should reference a from a=3
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
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
		val result = parseHelper.parseAndPerformBaseTest(SUT)
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
		val result = parseHelper.parseAndPerformBaseTest(SUT)
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
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}
	
	
	@Test
	def void scopingNumericForTest() { 
		val SUT = '''
		a = {}
		for i = 1, 10 do
		   a[i] = i
		end
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
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
		val result = parseHelper.parseAndPerformBaseTest(SUT)
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
		val result = parseHelper.parseAndPerformBaseTest(SUT)
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
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void scopingMethodBodyArgsTest() { 
		val SUT = '''
		table = {}
		function table:func(arg1) 
			a = self
			return arg1
		end
		b = table:func()
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void scopingLocalFuncBodyArgsTest() { 
		val SUT = '''
		local function func(arg1, arg2) 
			a = arg1
			b = a
			c = arg2
		end
		
		local func = function (a, b)
			a = a
			b = b.b -- TODO: we dont know if b is a table, but this kind of access would indicate so.. -> implement trivial recovery?
			b = b
		end	
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void scopingLocalFuncDeclarationTest() { 
		val SUT = '''
		local function func() end
		local function func2() end	
		
		a = func
		b = func2
		
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
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
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void scopingLocalAssignmentToReferencingTest() { 
		val SUT = '''
		local a = {member = "member"}
		local b = a
		local c = b.member
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
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
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}

	@Test
	def void scopingNestedTableConstructorTest() { 
		val SUT = '''
		a = {b = {c = {member = "hello world"}}}
		d = a.b.c.member
		
		m = {{{"hello again"}}}
		n = m[1][1][1]
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
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
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void scopingGotoLabelTest() { 
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
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void scopingVisibilityTest() { 
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
		val result = parseHelper.parseAndPerformBaseTest(SUT)
		// TODO: fix the problem described by the failed assertion below
		//		(see also LinkingAndScopingUtils.getReferenceablesFromStat)
		Assertions.assertTrue(
			false, 
			"The print(x) in the last line should reference the global x, but references a local one (the one in the first block x=x+1)."
		);
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
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}
	
	
	// TODO: never allow a rhs to reference its own lhs, 
	// needs to be lhs of other Assignment
	@Test
	def void scopingTemp3Test() { 
		val SUT = '''
			a = a
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void scopingFunctionCallReturnFeatureTest() { 
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
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}
	
	// TODO: returned function defined outside of returning function
	// does not work, see scopingResolveParentBlockReferencedVarTest
	@Test
	def void scopingDoubleFunctionCallReturnFeatureTest() { 
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
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}
	
	// TODO: we need to include the previously searched scopes when collecting the 
	// referenceables, since this will not work otherwise ("b" is not visible in the 
	// inner block, but a is not visible in the outer block , so the reference cannot be
	// resolved)
	@Test
	def void scopingResolveParentBlockReferencedVarTest() { 
		val SUT = '''
		b = {member = "hello world"}
		do 
		  a = b 
		  c = a.member
		end
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}

	@Test
	def void tempTest() { 
		val SUT = '''
		
		local _M = {version = 0.2}
		local GRAPHQL_DEFAULT_MAX_SIZE       = 1048576               -- 1MiB
		local GRAPHQL_REQ_DATA_KEY           = "query"
		local GRAPHQL_REQ_METHOD_HTTP_GET    = "GET"
		local GRAPHQL_REQ_METHOD_HTTP_POST   = "POST"
		local GRAPHQL_REQ_MIME_JSON          = "application/json"
		
		
		local fetch_graphql_data = {
		    [GRAPHQL_REQ_METHOD_HTTP_GET] = function(ctx, max_size)
		        local body = request.get_uri_args(ctx)[GRAPHQL_REQ_DATA_KEY]
		        if not body then
		            return nil, "failed to read graphql data, args[" ..
		                        GRAPHQL_REQ_DATA_KEY .. "] is nil"
		        end
		
		        if type(body) == "table" then
		            body = body[1]
		        end
		
		        return body
		    end,
		
		    [GRAPHQL_REQ_METHOD_HTTP_POST] = function(ctx, max_size)
		        local body, err = request.get_body(max_size, ctx)
		        if not body then
		            return nil, "failed to read graphql data, " .. (err or "request body has zero size")
		        end
		
		        if request.header(ctx, "Content-Type") == GRAPHQL_REQ_MIME_JSON then
		            local res
		            res, err = json.decode(body)
		            if not res then
		                return nil, "failed to read graphql data, " .. err
		            end
		
		            if not res[GRAPHQL_REQ_DATA_KEY] then
		                return nil, "failed to read graphql data, json body[" ..
		                            GRAPHQL_REQ_DATA_KEY .. "] is nil"
		            end
		
		            body = res[GRAPHQL_REQ_DATA_KEY]
		        end
		
		        return body
		    end
		}
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}

	@Test
	def void temp2Test() { 
		val SUT = '''
		local a = "a"
		table = {
			[a] = 10
		}
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}
	
	@Test
	def void temp3Test() { 
		val SUT = '''
		a = {}
		a.f = function () end
		function func()
		end
		func()
		a.f()
		'''
		val result = parseHelper.parseAndPerformBaseTest(SUT)
	}
	
}
