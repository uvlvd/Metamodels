package org.xtext.lua52;

public class PreprocessingUtils {
	/**
	 * Attention! This single-line regex will also match multi-line comments to an extend, thus the multi-line comments
	 * need to be removed first.
	 * It may be possible to use negative lookahead to avoid matching multi-line comments,
	 * but that would lead to a complex regex.
	 */
	private static final String LUA_SINGLE_LINE_COMMENT_REGEX = "--.*[\\s]+"; 
	private static final String LUA_MULTI_LINE_COMMENT_REGEX = "--\\[(=*)\\[(?s).*?\\]\\1\\]"; // use backreference to match multi-line comments with arbitrary number of "="
	
	/**
	 * Returns the given String without leading whitespace characters and the special first-line comment allowed in Lua scripts.
	 * The special first-line comment is denoted by a "#" as the first non-whitespace character in a Lua file.
	 */
	public static final String removeFirstLineSpecialComment(String fileStr) {
		//return fileStr.stripLeading() // remove leading whitespace
		System.out.println("TODO: implement removeFirstLineSpecialComment()");
		return "";
	}
	
	
	// TODO: the below methods are no preprocessing methods, just used for testing stuff
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