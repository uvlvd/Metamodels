package org.xtext.lua.scoping;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.impl.DefaultResourceDescriptionStrategy;
import org.eclipse.xtext.util.IAcceptor;
import org.xtext.lua.lua.Assignment;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Chunk;
import org.xtext.lua.lua.FunctionDeclaration;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.utils.LinkingAndScopingUtils;


public class LuaResourceDescriptionStrategy extends DefaultResourceDescriptionStrategy {
	private static final Logger LOGGER = Logger.getLogger(LuaResourceDescriptionStrategy.class);
	
	// adapted from DefaultResourceDescriptionStrategy.createEObjectDescriptions
    private void createEObjectDescription(IAcceptor<IEObjectDescription> acceptor, Referenceable referenceable) {
    	try {
			QualifiedName qualifiedName = getQualifiedNameProvider().getFullyQualifiedName(referenceable);
			if (qualifiedName != null) {
				acceptor.accept(EObjectDescription.create(qualifiedName, referenceable));
			}
		} catch (Exception exc) {
			LOGGER.error(exc.getMessage(), exc);
		}
    	
    	// From previous implementation
        /*var fqn = this.getQualifiedNameProvider()
            .apply(refble);
        if (fqn != null) {
            acceptor.accept(EObjectDescription.create(fqn, refble));
        }*/
    }

    @Override
    public boolean createEObjectDescriptions(EObject eObject, IAcceptor<IEObjectDescription> acceptor) {
    	if (getQualifiedNameProvider() == null)
			return false;
    	
    	// we only want (global) FunctionDeclarations and leafs of PrefixExp feature paths 
    	// (i.e. assignables in (global) Assignments) to be accessible from outside a resource. 
    	// => we need to traverse to all (global) FunctionDeclarations and return those as well as
    	//    to all (global) Assignments and return their assignables.
		
    	
    	// TODO: this does currently not traverse Block which are not direct children of Chunks, i.e.
    	//       global definitions in sub-blocks (which should be exported) are not exported as of now.
    	//       For this, we would need to be able to decide which assignables in a block are globally accessible,
    	//       which depends on if a local assignable with the same name exists in previous statements, which is
    	//       not yet implemented (see LinkingAndScopingUtils.getReferenceablesFromStat).
    	// Alternatively we could always traverse EXCEPT for when we hit a FunctionDeclaration or a (global) Assignment
    	// which would lead to all FunctionDeclarations and Assignments being exported regardless of their parent block
    	
    	if (eObject instanceof Chunk) {
            // always traverse Chunk's children
            return true;
        } else if (eObject instanceof Block && eObject.eContainer() instanceof Chunk) {
            // always traverse root block in a chunk
            return true;
        } else if (eObject instanceof FunctionDeclaration funcDecl) {
            // add global function and stop traversal
        	createEObjectDescription(acceptor, funcDecl);
            return false;
        } else if (eObject instanceof Assignment assignment) {
        	// add all assignables from (global) Assignments and stop traversal
        	EcoreUtil2.getAllContentsOfType(assignment, Referenceable.class)
        			.stream()
        			.filter(ref -> LinkingAndScopingUtils.isAssignable(ref))
        			.forEach(assignable -> createEObjectDescription(acceptor, assignable));
            return false;
        }
    	// fall-through: TODO: check if anything non-expected falls through
        return false;
    }

}
