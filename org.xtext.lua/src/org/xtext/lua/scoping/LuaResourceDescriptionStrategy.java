package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.impl.DefaultResourceDescriptionStrategy;
import org.eclipse.xtext.util.IAcceptor;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Chunk;
import org.xtext.lua.lua.Expression_TableConstructor;
import org.xtext.lua.lua.Field_AddEntryToTable;
import org.xtext.lua.lua.Refble;
import org.xtext.lua.lua.Statement_Assignment;
import org.xtext.lua.lua.Statement_Function_Declaration;

public class LuaResourceDescriptionStrategy extends DefaultResourceDescriptionStrategy {

    private void tryAddFQN(IAcceptor<IEObjectDescription> acceptor, Refble refble) {
        var fqn = this.getQualifiedNameProvider()
            .apply(refble);
        if (fqn != null) {
            acceptor.accept(EObjectDescription.create(fqn, refble));
        }
    }

    @Override
    /**
     * returns true if children of the eObject should be traversed
     */
    public boolean createEObjectDescriptions(EObject eObject, IAcceptor<IEObjectDescription> acceptor) {
        if (eObject instanceof Chunk) {
            // chunks always export
            return true;
        } else if (eObject instanceof Block && eObject.eContainer() instanceof Chunk) {
            // root block in a chunk
            return true;
        } else if (eObject instanceof Statement_Assignment) {
            var assignment = (Statement_Assignment) eObject;
            if (!assignment.isLocal()) {
                return true;
            }
        } else if (eObject instanceof Expression_TableConstructor) {
            // tables constructors are traversed so we add their fields
            return true;
        } else if (eObject instanceof Statement_Function_Declaration) {
            var declaration = (Statement_Function_Declaration) eObject;
            if (!declaration.isLocal()) {
                // add global function and strop traversal
                tryAddFQN(acceptor, declaration);
            }
        } else if (eObject instanceof Refble) {
            // refbles are directly added
            tryAddFQN(acceptor, (Refble) eObject);

            if (eObject instanceof Field_AddEntryToTable) {
                // continue traversal in tables
                return true;
            }
        }
        return false;
    }

}
