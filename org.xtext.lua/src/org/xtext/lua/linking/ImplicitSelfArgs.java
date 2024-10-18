package org.xtext.lua.linking;

import java.util.HashMap;
import java.util.Map;

import org.xtext.lua.lua.Arg;
import org.xtext.lua.lua.FuncBody;
import org.xtext.lua.lua.LuaFactory;
import org.xtext.lua.lua.impl.ArgImpl;
import org.xtext.lua.utils.LinkingAndScopingUtils;

public class ImplicitSelfArgs {
	private Map<FuncBody, Arg> funcBodyToSelfArg = new HashMap<>();
	
	public Arg getSelfArgFor(FuncBody funcBody) {
		if (funcBodyToSelfArg.containsKey(funcBody)) {
			return funcBodyToSelfArg.get(funcBody);
		}
		var selfArg = LuaFactory.eINSTANCE.createArg();
		selfArg.setName(LinkingAndScopingUtils.SELF_PARAM_NAME);
		funcBodyToSelfArg.put(funcBody, selfArg);
		return selfArg;
	}
}
