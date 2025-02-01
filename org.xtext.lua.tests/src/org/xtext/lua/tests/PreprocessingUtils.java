package org.xtext.lua.tests;

public class PreprocessingUtils {
	/**
	 * Attention! This single-line regex will also match multi-line comments to an extend, thus the multi-line comments
	 * need to be removed first.
	 * It may be possible to use negative lookahead to avoid matching multi-line comments,
	 * but that would lead to a complex regex.
	 */
	// TODO: these regexes do not correctly match all possible comment patterns appearing in strings, for example comment patterns
	// surrounded by strings like "-- this is a comment"
	private static final String NEGATIVE_LOOKAHEAD_NO_QUOTES = "(?<!['\"])"; //"^(?!(['\\\"])).*";
	private static final String LUA_SINGLE_LINE_COMMENT_REGEX = NEGATIVE_LOOKAHEAD_NO_QUOTES + "--.*\\R?"; 
	private static final String LUA_MULTI_LINE_COMMENT_REGEX = NEGATIVE_LOOKAHEAD_NO_QUOTES + "--\\[(=*)\\[(?s).*?\\]\\1\\]"; // use backreference to match multi-line comments with arbitrary number of "="
	
	
	public static final String removeCommentsAndWhiteSpacesAndNewLines(String str) {
		return removeAllWhiteSpacesAndNewLines(removeComments(str));
	}
	
	/**
	 * Removes all Lua comments from the given string.
	 * @param str the string.
	 * @return the string without Lua comments.
	 */
	public static final String removeComments(String str) {
		// remove multi-line comments first, since single-line comment regex also matches multi-line comments
		return str.replaceAll(LUA_MULTI_LINE_COMMENT_REGEX, "") 
					  .replaceAll(LUA_SINGLE_LINE_COMMENT_REGEX, "");
	}
	
	public static final String removeAllWhiteSpacesAndNewLines(String str) {
		return str.replaceAll("\\s+", "");
	}
	

}