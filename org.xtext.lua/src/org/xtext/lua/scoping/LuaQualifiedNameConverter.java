package org.xtext.lua.scoping;

import org.eclipse.xtext.naming.IQualifiedNameConverter;

public class LuaQualifiedNameConverter extends IQualifiedNameConverter.DefaultImpl {
	/**
	 * We override the default delimiter for qualified names, since Strings containing "."
	 * would be split otherwise, which leads to problems with the DefaultLinkingService
	 * when comparing cross-reference linkTexts (e.g. "hello.world", which is split into "hello" "world")
	 * with the name of the candidate (which would be "hello.world").
	 * 
	 * The alternative would be to always create IEObjectDescriptions when creating Scopes,
	 * using EObjectDescription.create(QualifiedName, EObject) and the qualifiedNameConverter
	 * to compute the QualifiedName from the candidate name.
	 */
	//@Override
	//public String getDelimiter() {
	//	return "";
	//}
}
