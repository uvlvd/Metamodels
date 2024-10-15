package org.xtext.lua.scoping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
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
import org.xtext.lua.lua.Return;
import org.xtext.lua.utils.LinkingAndScopingUtils;

import com.google.common.base.Predicate;
import com.google.inject.Inject;


public class LuaResourceDescriptionStrategy extends DefaultResourceDescriptionStrategy {
	private static final Logger LOGGER = Logger.getLogger(LuaResourceDescriptionStrategy.class);
	
	private static final String GLOBAL_RETURN_USERDATA_KEY = "globalReturn";
	private static final String GLOBAL_RETURN_URI_USERDATA_KEY = "uriString";
	
    private void createEObjectDescription(IAcceptor<IEObjectDescription> acceptor, Referenceable referenceable) {
    	createEObjectDescription(acceptor, referenceable, null);
    }

	// adapted from DefaultResourceDescriptionStrategy.createEObjectDescriptions
    private void createEObjectDescription(IAcceptor<IEObjectDescription> acceptor, Referenceable referenceable, Map<String, String> userData) {
    	try {
			QualifiedName qualifiedName = getQualifiedNameProvider().getFullyQualifiedName(referenceable);
			if (qualifiedName != null) {
				if (userData != null) {
					acceptor.accept(EObjectDescription.create(qualifiedName, referenceable, userData));
				} else {
					acceptor.accept(EObjectDescription.create(qualifiedName, referenceable));
				}
				
			}
		} catch (Exception exc) {
			LOGGER.error(exc.getMessage(), exc);
		}
    }

    @Override
    public boolean createEObjectDescriptions(EObject eObject, IAcceptor<IEObjectDescription> acceptor) {
    	if (getQualifiedNameProvider() == null)
			return false;
    	
    	// Simplified version: we return all externally visible referenceables from the root block
    	// as well as the referenceables from its return statement (if any).
    	// This is not how it actually works in Lua, since globals defined inside child-blocks would
    	// also be visible from another file.
    	if (eObject instanceof Chunk) {
            // always traverse Chunk's children
            return true;
        } else if (eObject instanceof Block block && eObject.eContainer() instanceof Chunk) {
            // always traverse root block in a chunk
        	LinkingAndScopingUtils.streamExternallyVisibleReferenceablesFromBlock(block)
        		.forEach(assignable -> createEObjectDescription(acceptor, assignable));
            return true;
        } else if (eObject instanceof Return returnStat) {
        	final var referenceablesFromReturnExps = LinkingAndScopingUtils.getReferenceablesFromReturnStat(returnStat, getQualifiedNameProvider());
        	final var returnExpsCount = referenceablesFromReturnExps.size();
        	
        	for (int i = 0; i < returnExpsCount; i++) {
        		final var expIdentifier = Integer.toString(i);
        		for (final var referenceable : referenceablesFromReturnExps.get(i)) {
        			var userData = new HashMap<String, String>();
        			var uriString = eObject.eResource().getURI().toString(); // TODO: add resource uri to userdata map for easy return value of require calc?
        			userData.put(GLOBAL_RETURN_USERDATA_KEY, expIdentifier);
        			userData.put(GLOBAL_RETURN_URI_USERDATA_KEY, uriString);
        			createEObjectDescription(acceptor, referenceable, userData);
        		}
        	}

        	// we traversed the Chunk as well as the root block to get to the return statement of the root block.
        	//LinkingAndScopingUtils.getReferenceablesFromReturnStat(returnStat, getQualifiedNameProvider())
        	//	.forEach(assignable -> createEObjectDescription(acceptor, assignable));
        	return false;
        }
    	return false;
    	
    	
    	
    	/*
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
        */
    }
    
    public static Predicate<IEObjectDescription> isReturnedExpAtIndex(int index) {
    	return description -> {
			var userDataEntry = description.getUserData(GLOBAL_RETURN_USERDATA_KEY);
			if (userDataEntry != null) {
				return userDataEntry.equals(Integer.toString(index));
			}
			return false;
		};
    }
    
    public static Predicate<IEObjectDescription> isReturnedExpAtIndex(int index, String uriString) {
    	return description -> {
			var userDataReturnIndex = description.getUserData(GLOBAL_RETURN_USERDATA_KEY);
			var userDataReturnURI = description.getUserData(GLOBAL_RETURN_URI_USERDATA_KEY);
			if (userDataReturnIndex != null && userDataReturnURI != null) {
				return userDataReturnIndex.equals(Integer.toString(index)) 
						&& userDataReturnURI.equals(uriString);
			}
			return false;
		};
    }
    

}
