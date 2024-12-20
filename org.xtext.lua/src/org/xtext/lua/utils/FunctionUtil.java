package org.xtext.lua.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.xtext.lua.lua.Arg;
import org.xtext.lua.lua.ExpFunctionDeclaration;
import org.xtext.lua.lua.FuncBody;
import org.xtext.lua.lua.FunctionDeclaration;
import org.xtext.lua.lua.LocalFunctionDeclaration;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;

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

}
