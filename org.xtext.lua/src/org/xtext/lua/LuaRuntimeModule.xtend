/*
 * generated by Xtext 2.28.0
 */
package org.xtext.lua

import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.scoping.IGlobalScopeProvider
import org.eclipse.xtext.scoping.impl.DefaultGlobalScopeProvider
import org.eclipse.xtext.scoping.impl.SimpleLocalScopeProvider

/** 
 * Use this class to register components to be used at runtime / without the
 * Equinox extension registry.
 */
class LuaRuntimeModule extends AbstractLuaRuntimeModule {
	override Class<? extends IQualifiedNameProvider> bindIQualifiedNameProvider() {
//		LuaQualifiedNameProvider
		super.bindIQualifiedNameProvider
	}

	override Class<? extends IGlobalScopeProvider> bindIGlobalScopeProvider() {
//		LuaGlobalScopeProvider
		DefaultGlobalScopeProvider
	}
	
	override bindIScopeProvider() {
		SimpleLocalScopeProvider
//		LuaScopeProvider
	}
	
}
