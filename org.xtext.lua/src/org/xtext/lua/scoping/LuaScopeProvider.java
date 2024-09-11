/*
 * generated by Xtext 2.34.0
 */
package org.xtext.lua.scoping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.Scopes;
import org.eclipse.xtext.scoping.impl.SimpleLocalScopeProvider;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.xtext.lua.lua.Assignment;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Refble;
import org.xtext.lua.lua.Stat;
import org.xtext.lua.lua.Var;
import org.xtext.lua.lua.VarMemberAccessLhs;
import org.xtext.lua.lua.VarMemberAccessRhs;
import org.xtext.lua.lua.VarRhs;
import org.xtext.lua.utils.LinkingAndScopingUtils;
import org.xtext.lua.lua.LuaPackage.Literals;

import com.google.inject.Inject;

/**
 * This class contains custom scoping description.
 * 
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#scoping
 * on how and when to use it.
 */
//public class LuaScopeProvider extends AbstractLuaScopeProvider {
public class LuaScopeProvider extends SimpleLocalScopeProvider {
	
	  	@Inject
	    private IQualifiedNameConverter nameConverter;
	  	
		@Inject 
		private LinkingHelper linkingHelper;
	    
	    @Override
	    public IScope getScope(final EObject context, final EReference reference) {
	        if (context == null) {
	            // nothing todo without context
	            return IScope.NULLSCOPE;
	        }
	        
	        if (context instanceof VarMemberAccessRhs memberAccess) {
	        	return getScopeForVarMemberAccess(memberAccess, reference);
	        }
	        

	        var parentBlock = EcoreUtil2.getContainerOfType(context.eContainer(), Block.class);
	        if (parentBlock == null) {
	            // if we have no parent anymore we delegate to the global scope
	        	//System.out.println("TODO: Implement global scope");
	        	//System.out.println(context);
	        	return super.getScope(context, reference); // TODO: just for debug, remove
	            //return super.getGlobalScope(context.eResource(), reference);
	        }
	        
	        //System.out.println("current context: " + context);
	        //System.out.println("context parent: " + context.eContainer());
	        var parentStatement = EcoreUtil2.getContainerOfType(context, Stat.class);
	        var blockScope = getScopeOfBlock(parentBlock, context, parentStatement, reference);
	        return blockScope;
	        //return super.getScope(context, reference); // TODO: just for debug, remove
	    }
	    
	    	private IScope getScopeForVarMemberAccess(final VarMemberAccessRhs memberAccess, final EReference reference) {
	    	
	    	// get leaf var
	    	//var v = getVar(memberAccess);
	    	//var varRefNode = NodeModelUtils.findNodesForFeature(v, Literals.VAR_RHS__REFERENCING).get(0);
	    	//var varRefString = linkingHelper.getCrossRefNodeAsString(varRefNode, true);
	    	// TODO: function to get scope for vars, then replace the above with it?
	    	
	    	// get candidates, i.e. all lhs-objects of the same type and with a name matching the reference
	    		 // TODO: only traverse parent scope, should not need to traverse whole tree
	    		//        maybe even only those VarMemberAccessLhs for which eContainer() is of an appropriate type? (i.e. if there type of candidate.eContainer and memberAccess.eContainer differ
	    		//		  the candidate cant be a candidate?
	    	var rootElement = EcoreUtil2.getRootContainer(memberAccess);
	    	var candidates = EcoreUtil2.getAllContentsOfType(rootElement, VarMemberAccessLhs.class)
	    							   .stream()
	    							   .filter(candidate -> isPathsEqual(candidate, memberAccess))
	    							   .toList();
	    	
	    	
	    	if (candidates.size() != 0) {
	    		return Scopes.scopeFor(candidates);
	    	} else {
	    		System.out.println(candidates);
	    	}
	    	
	    	return IScope.NULLSCOPE;
	    }
	    	
	    	
	    private boolean isPathsEqual(final VarMemberAccessLhs lhs, final VarMemberAccessRhs rhs) {
	    	var refNode = NodeModelUtils.findNodesForFeature(rhs, Literals.VAR_MEMBER_ACCESS_RHS__REFERENCING).get(0);
	    	var refString = linkingHelper.getCrossRefNodeAsString(refNode, true);
	    	System.out.println("	Comparing " + lhs.getName() + " and " + refString);
	    	// early out 1: currently compared path elements are not equal
	    	if (!lhs.getName().equals(refString)) { 
	    		System.out.println("	1:" + lhs.getName() + ", " + refString);
	    		return false;
	    	}
	    	
	    	var lhsChild = getSingleChild(lhs);
	    	var rhsChild = getSingleChild(rhs);
	    	
	    	// end: found leaf elements for both paths, return comparison of leaf elements
	    	if (lhsChild instanceof Var lhsVar && rhsChild instanceof VarRhs rhsVar) {
	    		var refChildNode = NodeModelUtils.findNodesForFeature(rhsVar, Literals.VAR_RHS__REFERENCING).get(0);
		    	var refChildString = linkingHelper.getCrossRefNodeAsString(refChildNode, true);
		    	System.out.println("	2:" + lhsVar.getName() + ", " + refChildString);
	    		return lhsVar.getName().equals(refChildString);
	    	} // recurse further down the trees (which are sequences in this case)
	    	else if (lhsChild instanceof VarMemberAccessLhs l && rhsChild instanceof VarMemberAccessRhs r) {
	    		return isPathsEqual(l, r);
	    	}
	    	// fall-through
	    	System.out.println("	3");
	    	return false;
	    }

	    
	    private EObject getSingleChild(EObject obj) {
	    	var children = obj.eContents();
    		if (children.size() != 1) { // should not happen unless the grammar is changed somehow
    			throw new RuntimeException("Exactly one child expected, for object " + obj + ", but got " + children.size() + ". Did you change the grammar?");
    		}
    		return children.get(0);
	    }
	    
	    private VarRhs getVar(final VarMemberAccessRhs memberAccess) {
	    	var vars = EcoreUtil2.getAllContentsOfType(memberAccess, VarRhs.class);
			if (vars.size() != 1) {
				throw new RuntimeException("Exactly on VarRhs expected, found " + vars.size());
			}
			
			return vars.get(0);
	    }
	    
	    private IScope getScopeOfBlock(Block block, EObject context, Stat filterStatement, EReference reference) {
	        var parentScope = getScope(block, reference);

	        var refblesInBlock = getRefblesInBlock(block, context, filterStatement);

	        List<IEObjectDescription> descriptions = refblesInBlock.stream()
	            .map(refble -> describeRefble(refble))
	            .flatMap(List::stream)
	            .toList();

	        
	        //var thisScope = new SimpleScope(parentScope, descriptions, isIgnoreCase(reference));
	        //return thisScope;
	        var scope = new SimpleScope(parentScope, descriptions);
	        //return IScope.NULLSCOPE;
	        if (!descriptions.isEmpty()) {
	            System.out.println("descs for " + block);
	            descriptions.forEach(System.out::println);
	        }

	        return scope;
	    }
	    
	    private List<Refble> getRefblesInBlock(Block block, EObject context, Stat filterStatement) {
	        List<Refble> refbles = new ArrayList<>();

	        // add refbles contained inside of for loops, function declarations, etc.
	       // if (block.eContainer() instanceof BlockWrapperWithArgs blockWrapperWithArgs) {
	            // add arguments to refbles
	        //	refbles.addAll(LinkingAndScopingUtils.getArgs(blockWrapperWithArgs));
	        //}
	        
	        for (var statement : block.getStats()) {
	            if (statement.equals(filterStatement)) {
	            	System.out.println("Filterstatement: " + filterStatement);
	                continue;
	            } else if (statement instanceof Assignment assignment) {
	                //refbles.addAll(getRefblesInAssignment((Statement_Assignment) statement));
	            	
	            	//System.out.println("test vars: " + test);
	            	//System.out.println("assignment vars: " + assignment.getVars());
	            	//refbles.addAll(assignment.getVars());
	            	//refbles.addAll(LinkingAndScopingUtils.getAssignmentVarsFilteredByContext(context, assignment));
	            } else if (statement instanceof Refble) {
	                refbles.add((Refble) statement);
	            }
	        }
	        
	        //System.out.println("refbles:");
	        //refbles.forEach(System.out::println);
	        
	        return refbles;
	    }
	    
	    private List<IEObjectDescription> describeRefble(Refble refble) {
	        var descriptions = new ArrayList<IEObjectDescription>();
	        var fqn = getNameProvider().apply(refble);
	        //System.out.println("fqn: " + fqn); // TODO: remove
	        
	        if (fqn != null) {
	            // create description
	            var description = EObjectDescription.create(fqn, refble);
	            descriptions.add(description);

	            // Add alias for functions in tables because of the member syntactic sugar
	            // E.g. Foo:bar(...)
	            /* TODO: implement
	            if (refble instanceof Field_AddEntryToTable
	                    && ((Field_AddEntryToTable) refble).getValue() instanceof Expression_Function) {
	                var aliasString = fqn.skipLast(1)
	                    .toString() + ":"
	                        + fqn.skipFirst(fqn.getSegmentCount() - 1)
	                            .toString();
	                var aliasQn = nameConverter.toQualifiedName(aliasString);
	                descriptions.add(new AliasedEObjectDescription(aliasQn, description));
	            }
				*/

	        }
	        return descriptions;
	    }
	    
	    //@Override
	    public IScope getScope_old(EObject context, EReference reference) {
	    	
	    
	    	//EObject rootElement = EcoreUtil2.getRootContainer(context);
	    	//List<Ref> candidates = EcoreUtil2.getAllContentsOfType(rootElement, Ref.class);

	    	
	        // We want to define the Scope for the Element's superElement cross-reference
	        //if (context instanceof ParamArgs paramArgs
	    	
	        if (LinkingAndScopingUtils.hasRef(context)
	                //&& reference == Lua52Package.Literals.PARAM_ARGS__PARAMS) {
	        		//&& (
	        		//		reference == Lua52Package.Literals.VAR_MEMBER_ACCESS__NAME
	        		//		|| reference == Lua52Package.Literals.VAR__NAME
	        		//		|| reference == Lua52Package.Literals.MEMBER_FUNC_CALL__NAME
	        		//	)
	        		) {
	            // Collect a list of candidates by going through the model
	            // EcoreUtil2 provides useful functionality to do that
	            // For example searching for all elements within the root Object's tree
	            EObject rootElement = EcoreUtil2.getRootContainer(context);
	            //System.out.println(rootElement);
	            List<Assignment> candidates = EcoreUtil2.getAllContentsOfType(rootElement, Assignment.class);
	            //System.out.println(candidates);
	            //System.out.println("Prefix exps:");

	            
	            for (var assignment : candidates) {
	            	//System.out.println(assignment);
	            	//for (var v : assignment.getVars()) {
	            		//name = getRefName(name, v.getSuffixExp());
	            		//var name = "NULL";
	            	
	            		//System.out.println(v);
	            		//System.out.println(name);
	            		/*
	            		var ref = v.getRef();
	            		if (ref != null) {
	            			var name = ref.getName();
	            			if (v.getSuffixExp() != null) {
	            				name = getRefName(name, v.getSuffixExp());
	            			} 
	            			System.out.println(name);
	            		}
	            		*/
	            	//}
	            }
	            
	            //candidates.forEach(a -> System.out.println(a.getVars()));
	            
	            //System.out.println(paramArgs.getParams());
	           // for (var exp: paramArgs.getParams().getExps()) {
	           // 	if (exp instanceof PrefixExp prefixExp) {
	           // 		System.out.println(prefixExp);
	           // 	}
	            //}
	            // Create IEObjectDescriptions and puts them into an IScope instance
	            //System.out.println(Scopes.scopeFor(candidates));
	            
	            
	            //return Scopes.scopeFor(candidates);
	        }
	        
	        return super.getScope(context, reference);
	    }
	    /*
	    private String getRefName(String current, SuffixExp suffix) {
	    	if (suffix instanceof FuncCallSuffixExp funcCallSuffix) {
	    		return getRefName(current + ":" + funcCallSuffix.getFuncName().getName(), suffix.getSuffixExp());
	    	}
	    	if (suffix instanceof NameSuffixExp nameSuffixExp) {
	    		return getRefName(current + "." + nameSuffixExp.getIndexName().getName(), suffix.getSuffixExp());
	    	}
	    	// TODO: should probably also use IndexSuffixExp and ArgsSuffixExp to parse name
	    	return current;
	    }
	    */

}
