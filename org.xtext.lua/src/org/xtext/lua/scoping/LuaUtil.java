package org.xtext.lua.scoping;

import com.google.common.base.Objects;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.lua.ContainsBlock;
import org.xtext.lua.lua.Expression;
import org.xtext.lua.lua.Referenceable;

@SuppressWarnings("all")
public class LuaUtil {
  public static boolean isTableField(final Referenceable refble) {
    Expression _entryValue = refble.getEntryValue();
    return (_entryValue != null);
  }

  public static boolean isDeclaration(final Referenceable refble) {
    boolean _isTableField = LuaUtil.isTableField(refble);
    return (!_isTableField);
  }

  /**
   * Local Declarations are:
   * 	- local variables
   * 	- local functions
   * 	- arguments of function- and for-blocks
   */
  public static boolean isLocalDeclaration(final Referenceable refble) {
    return (Objects.equal(refble.getLocal(), "local") || (refble.eContainer() instanceof ContainsBlock));
  }

  public static Referenceable getContainingDeclaration(final EObject obj) {
    Referenceable _xblockexpression = null;
    {
      Referenceable parent = EcoreUtil2.<Referenceable>getContainerOfType(obj, Referenceable.class);
      while (((parent != null) && (!LuaUtil.isDeclaration(parent)))) {
        EcoreUtil2.<Referenceable>getContainerOfType(obj, Referenceable.class);
      }
      _xblockexpression = parent;
    }
    return _xblockexpression;
  }
}
