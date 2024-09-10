package org.xtext.lua52.converters;

import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.nodemodel.INode;

public class LuaNameConverter implements IValueConverter<String> {

	@Override
	public String toValue(String string, INode node) throws ValueConverterException {
		//if (string.contains(".")) {
		//	System.out.println("foo: "+string);
		//	var parts= string.split("\\.");
		//	return parts[parts.length];
		//}
		return string;
	}

	/**
	 * We need to split the string value since it might have been altered by the LuaLinkingService
	 * (not sure why). E.g. for a member call a.foo the grammar cross-reference is "foo" (a LUA_NAME), which is
	 * extended by the custom linking service to "a.foo", this leads to problems with the stringification
	 * by the default value converter for LUA_NAME.
	 */
	@Override
	public String toString(String value) throws ValueConverterException {
		if (value.contains(".")) {
			var parts = value.split("\\.");
			if (parts.length > 0) {
				return parts[parts.length - 1];
			}
			
		}
		return value;
	}

}
