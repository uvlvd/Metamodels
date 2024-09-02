package org.xtext.lua52.converters;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.nodemodel.INode;

public class NumberValueConverter implements IValueConverter<Double> {
	private static final String[] HEX_MARKERS = new String[]{"0x", "0X"}; // strings starting with HEX_MARKER are hex strings
	private static final String[] HEX_EXPONENT_MARKERS = new String[]{"p", "P"}; // denote binary exponent part of hex value in Lua

    @Override
    public String toString(Double value) throws ValueConverterException {
        return Double.toString(value);
    }
    
    @Override
    public Double toValue(String string, INode node) throws ValueConverterException {
        if (isHexString(string)) {
        	return toValueFromHex(string);
            //var hexLong = Long.valueOf(Long.parseLong(string.substring(2), 16));
            //return Double.valueOf(hexLong.doubleValue());
        }

        return Double.valueOf(string);
    }
    
    /**
     * Converts a Lua5.2-type hex value string to a double.
     * @param string the hex string, starting with "0x" or "0X".
     * @return the hex value as a double.
     */
    private Double toValueFromHex(String string) {
    	if (string.length() <= 2) {
    		return 0d;
    	}
    	
    	var hexString = string.substring(2); // remove "0x" or "0X" from the start of the string
    	
    	// Lua hex string can contain fractional part after a '.', and binary exponent after a 'p' or 'P'.
    	var integerPartHex = "";
    	var fractionalPartHex = "";
    	var binaryExponentHex = "";
    	
    	// first, we get split the hexString into its integerPart, fractionalPart and binary exponent
    	var hexParts = hexString.split("[.pP]");
    	if (hexString.contains(".") && (containsBinaryExponent(string)) ) { // check if contains "." and "p" or "P"
    		integerPartHex = hexParts[0];
    		fractionalPartHex = hexParts[1];
    		binaryExponentHex = hexParts[2];
    	} else if (hexString.contains(".")) { // only contains "."
    		integerPartHex = hexParts[0];
    		fractionalPartHex = hexParts[1];
    	} else if (containsBinaryExponent(string)) { // only contains "p" or "P"
    		integerPartHex = hexParts[0];
    		binaryExponentHex = hexParts[1];
    	} else { // only contains integer part
    		integerPartHex = hexString;
    	}
    	
    	// then, we convert the integer and fractional parts to base10 string representations
    	var integerPartBase10 = "";
    	var fractionalPartBase10 = "";
    	var binaryExponentBase10 = "";
    	
    	// convert integer part to base10 representation string
    	if (!integerPartHex.isBlank()) {
    		// use BigInteger and BigDecimal to handle large numbers (e.g. from the Lua5.2 test suite: fffffffffffff800)
    		integerPartBase10 = new BigInteger(integerPartHex, 16).toString(); 
    	}
    	// convert fractional part to base10 representation string
    	if (!fractionalPartHex.isBlank()) {
    		// add the "." to the fractional part if it is not empty
    		fractionalPartBase10 = "." + new BigInteger(fractionalPartHex, 16).toString();
    	}
    	// the exponent is given in decimal format, so no conversion is needed
    	if (!binaryExponentHex.isBlank()) {
    		// add the "e" to the exponent part if it is not empty
    		binaryExponentBase10 = "e" + binaryExponentHex;
    	}
    	
    	// now we build a base10 String representation of the original hex.
    	// If parts were not available, they are represented by an empty string, else the separators ("." or "e") are already contained
    	// in their respective string representations.
    	var base10String = integerPartBase10 + fractionalPartBase10 + binaryExponentBase10;
    	// and parse and return this string as a double
    	return new BigDecimal(base10String).doubleValue();
    }
    
    private boolean containsBinaryExponent(String string) {
    	return Arrays.stream(HEX_EXPONENT_MARKERS)
    				 .anyMatch(marker -> string.contains(marker));
    }
    
    private boolean isHexString(String string) {
    	return Arrays.stream(HEX_MARKERS)
    				 .anyMatch(marker -> string.startsWith(marker));
    }


}