package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.xtext.lua.LuaUtil;
import org.xtext.lua.lua.Expression_TableConstructor;
import org.xtext.lua.lua.Field_AddEntryToTable;
import org.xtext.lua.lua.Refble;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Statement_Assignment;

public class LuaQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {
    @Override
    protected QualifiedName computeFullyQualifiedName(final EObject obj) {
        // Table field use the qualified name calculation and resolve the parent
        // multirefble
        if (obj instanceof Field_AddEntryToTable) {
            var computedFQN = this.computeFullyQualifiedNameFromNameAttribute(obj);

            if (obj.eContainer() instanceof Expression_TableConstructor) {
                var tableConstructor = (Expression_TableConstructor) obj.eContainer();
                if (tableConstructor.eContainer() instanceof Statement_Assignment) {
                    // find the refble that is associated with us in the parent multirefble
                    var dest = LuaUtil.resolveValueToDest(tableConstructor);
                    if (dest != null && dest instanceof Referenceable) {
                        var myRefble = (Referenceable) dest;
                        var myName = myRefble.getName() + "." + computedFQN.toString();
                        return this.getConverter()
                            .toQualifiedName(myName);
                    }
                }
            }
            return computedFQN;
        } else if (obj instanceof Refble) {
            var refble = (Refble) obj;

            // Othere refbles use simply their name
            var name = refble.getName();
            if (name != null) {
                return this.getConverter()
                    .toQualifiedName(name);
            }
        }
        return null;
    }
}
