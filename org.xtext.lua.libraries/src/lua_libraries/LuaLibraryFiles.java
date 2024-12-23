package lua_libraries;

import java.util.List;

/**
 * This class is used to return the paths to the Lua library files contained in the project.
 * @author jsaenz
 *
 */
public class LuaLibraryFiles {
	
	private LuaLibraryFiles() {}
	
	/**
	 * A list of all library file names contained in the project.
	 */
	private static final List<String> LIBRARY_FILE_NAMES = List.of(
			"basic.lua",
			"bit.lua",
			"bit32.lua",
			"builtin.lua",
			"coroutine.lua",
			"debug.lua",
			"ffi.lua",
			"io.lua",
			"jit.lua",
			"math.lua",
			"os.lua",
			"package.lua",
			"string.lua",
			"table.lua",
			"utf8.lua"
	);
	
	public static List<String> getAbsolutePaths() {
		return LIBRARY_FILE_NAMES.stream()
				.map(fileName -> LuaLibraryFiles.class.getResource(fileName).toString())
				.toList();
	}
	
}
