package org.xtext.lua.converters;

import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;
import org.eclipse.xtext.conversion.impl.AbstractDeclarativeValueConverterService;

/**
 * Used to convert Lua values, in particular Lua numbers (which can be hex strings) to double values.
 */
public class LuaValueConverterService extends AbstractDeclarativeValueConverterService {

    @ValueConverter(rule = "NUMBER_LITERAL")
    public IValueConverter<Double> getLuaNumberConverter() {
        return new NumberValueConverter();
    }
    
}
