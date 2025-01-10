package org.xtext.lua.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.lua.Arg;
import org.xtext.lua.lua.ExpFunctionDeclaration;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.FuncBody;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.FunctionCallStat;
import org.xtext.lua.lua.FunctionDeclaration;
import org.xtext.lua.lua.LocalFunctionDeclaration;
import org.xtext.lua.lua.MethodCall;
import org.xtext.lua.lua.NamedFeature;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.mocking.FeaturePath;
import org.xtext.lua.wrappers.LuaFunctionCall;
import org.xtext.lua.wrappers.LuaFunctionDeclaration;

public class FunctionUtil {
	
	private FunctionUtil() { }
	
	public static Optional<FuncBody> findFuncBodyFromFuncObject(EObject funcObject) {
		if (funcObject instanceof FunctionDeclaration funcDecl) {
			return Optional.of(funcDecl.getBody());
		} else if (funcObject instanceof LocalFunctionDeclaration funcDecl) {
			return Optional.of(funcDecl.getBody());
		} else if (funcObject instanceof ExpFunctionDeclaration funcDecl) {
			return Optional.of(funcDecl.getBody());
		} else if (funcObject instanceof Referencing referencing) {
			var value = AssignmentUtil.tryGetReferencedExp(referencing);
			if (value instanceof ExpFunctionDeclaration funcDecl) {
				return Optional.of(funcDecl.getBody());
			}
		}
		return Optional.empty();
	}
	
	public static boolean isFunctionDeclaration(Referenceable referenceable) {
		return findFuncBodyFromFuncObject(referenceable).isPresent();
	}
	
	public static List<Arg> getArgsFromFuncBody(final FuncBody body) {
		var result = new ArrayList<Arg>();
		final var parList = body.getParList();
		if (parList == null) {
			return result;
		}
		final var argList = parList.getArgsList();
		if (argList == null) {
			return result;
		}
		return argList.getArgs();
	}
	
	/**
	 * Recurses through the reference chain to find the function declaration referenced by the given {@link Referenceable}.
	 * Returns null if the referenced function declaration could not be found.
	 * @param ref the referenceable that references the function.
	 * @param currDepth current recursion depth.
	 * @param maxDepth max recursion depth.
	 * @return the referenced {@link LuaFunctionDeclaration}, or null.
	 */
	public static LuaFunctionDeclaration getReferencedFunction(Referenceable ref, int currDepth, final int maxDepth) {
		if (currDepth > maxDepth) {
			throw new RuntimeException("Reached max depth while attempting to get called function value from " + ref);
		}
		
		if (ref instanceof FunctionDeclaration decl) {
			return LuaFunctionDeclaration.of(decl);
		}

		if (ref instanceof LocalFunctionDeclaration decl) {
			return LuaFunctionDeclaration.of(decl);
		}
		
		if (ref instanceof ExpFunctionDeclaration decl) {
			return LuaFunctionDeclaration.of(decl);
		}
		
		if (MockUtil.isMocked(ref)) {
			return null;
		}
		
		if (ref instanceof Referencing referencing) {
			var refsRef = referencing.getRef();
			
			// if ref references a feature, use the feature path leaf as the next ref
			if (refsRef instanceof Feature feature) {
				final var namedLeafOpt = FeatureUtil.findFeaturePathNamedLeaf(feature);
				if (namedLeafOpt.isPresent()) {
					refsRef = namedLeafOpt.get();
				}
			}
			return getReferencedFunction(refsRef, ++currDepth, maxDepth);
		}
		
		// TODO: this fails in certain cases, e.g.g a = b and load(b) in lua 5.2 test suite api.lua
		//throw new RuntimeException("Could not find called function!");
		return null;
	}
	
    
    // TODO: create superclass for Lua FunctionCallStat,FunctionCall,MethodCall s.t. they can be handled
    // together.. either in org.xtext.lua (maybe in lua.xtext?) or in this package as a helper class
    public static List<LuaFunctionCall> getFunctionCallsContainedIn(final EObject root) {
    	var result = new ArrayList<LuaFunctionCall>();
    	EcoreUtil2.getAllContentsOfType(root, FunctionCallStat.class)
    		.stream()
    		.map(LuaFunctionCall::of)
    		.filter(Objects::nonNull)
    		.forEach(result::add);
    	EcoreUtil2.getAllContentsOfType(root, FunctionCall.class)
			.stream()
			.map(LuaFunctionCall::of)
			.filter(Objects::nonNull)
			.forEach(result::add);
    	EcoreUtil2.getAllContentsOfType(root, MethodCall.class)
			.stream()
			.map(LuaFunctionCall::of)
			.filter(Objects::nonNull)
			.forEach(result::add);
    	return result;
    }

    // TODO: comment copied from getFunctionCallscontainedIn: create superclass for Lua FunctionCallStat,FunctionCall,MethodCall s.t. they can be handled
    // together.. either in org.xtext.lua (maybe in lua.xtext?) or in this package as a helper class 
    public static List<LuaFunctionDeclaration> getAllFunctionDeclarationsContainedIn(final EObject root) {
    	var result = new ArrayList<LuaFunctionDeclaration>();
    	EcoreUtil2.getAllContentsOfType(root, FunctionDeclaration.class)
    		.stream()
    		.map(LuaFunctionDeclaration::of)
    		.filter(Objects::nonNull)
    		.forEach(result::add);
    	EcoreUtil2.getAllContentsOfType(root, LocalFunctionDeclaration.class)
			.stream()
			.map(LuaFunctionDeclaration::of)
			.filter(Objects::nonNull)
			.forEach(result::add);
    	EcoreUtil2.getAllContentsOfType(root, ExpFunctionDeclaration.class)
			.stream()
			.map(LuaFunctionDeclaration::of)
			.filter(Objects::nonNull)
			.forEach(result::add);
    	return result;
    }
    

}
