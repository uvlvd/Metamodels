package org.xtext.lua52.converters;

import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;
import org.eclipse.xtext.conversion.impl.AbstractDeclarativeValueConverterService;

public class LuaValueConverterService extends AbstractDeclarativeValueConverterService {

    @ValueConverter(rule = "NUMBER_LITERAL")
    public IValueConverter<Double> getLuaNumberConverter() {
        return new NumberValueConverter();
    }
}
