/*
 * generated by Xtext 2.28.0
 */
package org.xtext.lua.ide

import com.google.inject.Guice
import com.google.inject.Injector
import org.eclipse.xtext.util.Modules2
import org.xtext.lua.LuaRuntimeModule
import org.xtext.lua.LuaStandaloneSetup

/** 
 * Initialization support for running Xtext languages as language servers.
 */
class LuaIdeSetup extends LuaStandaloneSetup {
	override Injector createInjector() {
		return Guice.createInjector(Modules2.mixin(new LuaRuntimeModule(), new LuaIdeModule()))
	}
}
