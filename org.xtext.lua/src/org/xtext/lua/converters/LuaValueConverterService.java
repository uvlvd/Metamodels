package org.xtext.lua.converters;

import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;
import org.eclipse.xtext.conversion.impl.AbstractDeclarativeValueConverterService;

public class LuaValueConverterService extends AbstractDeclarativeValueConverterService {

    @ValueConverter(rule = "LUA_NUMBER")
    public IValueConverter<Double> getLuaNumberConverter() {
        return new LuaNumberValueConverter();
    }
}
