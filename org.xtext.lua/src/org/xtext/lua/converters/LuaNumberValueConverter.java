package org.xtext.lua.converters;

import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.nodemodel.INode;

public class LuaNumberValueConverter implements IValueConverter<Double> {

    @Override
    public Double toValue(String string, INode node) throws ValueConverterException {
        // parse hexadecimals correctly
        if (string.startsWith("0x")) {
            var hexLong = Long.valueOf(Long.parseLong(string.substring(2), 16));
            return Double.valueOf(hexLong.doubleValue());
        }

        return Double.valueOf(string);
    }

    @Override
    public String toString(Double value) throws ValueConverterException {
        return Double.toString(value);
    }

}
