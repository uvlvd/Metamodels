package org.xtext.lua.utils;

import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Stat;

/**
 * Utility class for Lua Statements ({@link Stat}).
 * @author jsaenz
 *
 */
public class StatUtil {
	private StatUtil() { }
	
	/**
	 * Returns the {@link Stat} that contains the given {@link EObject}.
	 */
	public static Optional<Stat> getParentStatement(EObject obj) {
		// since all PrefixExps extend Stat, we need to return the Stat from the Block, not
		// the direct parent of the object
		var parentBlock = EcoreUtil2.getContainerOfType(obj, Block.class);
		return EcoreUtil2.getAllContentsOfType(parentBlock, Stat.class).stream()
				.filter(stat -> EcoreUtil2.isAncestor(stat, obj))
				.findAny();
	}
}
