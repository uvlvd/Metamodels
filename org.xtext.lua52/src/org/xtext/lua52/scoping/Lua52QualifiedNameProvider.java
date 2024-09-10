package org.xtext.lua52.scoping;

import java.sql.Ref;
import java.util.Collections;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.util.PolymorphicDispatcher;
import org.eclipse.xtext.util.SimpleAttributeResolver;
import org.eclipse.xtext.util.Strings;
import org.xtext.lua52.lua52.Exp;
import org.xtext.lua52.lua52.Referenceable;
import org.xtext.lua52.lua52.Var;
import org.xtext.lua52.lua52.VarMemberAccessLhs;
import org.xtext.lua52.utils.LinkingAndScopingUtils;


public class Lua52QualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {
	

	
	private String getStrQNForVar(final EObject obj, final Var v) {
		return getStrQN(v.getName(), obj, v);
	}
	
	private String getStrQN(final String acc, final EObject obj, final EObject r) {
		var parent = r.eContainer();
		if (parent == null) {
			// should never happen
			throw new RuntimeException("Encountered null parent in getStrQN");
		}

		if (parent != obj && parent instanceof VarMemberAccessLhs memberAccess) {
			return getStrQN(acc + "." + memberAccess.getName(), obj, memberAccess);
		}
		
		if (parent == obj && parent instanceof VarMemberAccessLhs memberAccess) {
			return acc + "." + memberAccess.getName();
		}
		
		return acc;
    }
	
	@Override
    protected QualifiedName computeFullyQualifiedName(final EObject obj) {

		QualifiedName name = null;
		
		if (obj instanceof VarMemberAccessLhs) {
	    	var vars = EcoreUtil2.getAllContentsOfType(obj, Var.class);
	    	
	    	if (vars.size() > 1) {
	    		//System.out.println(vars);
	    		// TODO
	    		throw new RuntimeException("Should not have more than one Var content.");
	    	}
	    	
	    	for (var v : vars ) {
	    		var qnStr = getStrQNForVar(obj, v);
	    		name = this.getConverter().toQualifiedName(qnStr);
	    	}
		}
		
		if (obj instanceof Var var) {
			name = this.getConverter().toQualifiedName(var.getName());
		}

    	//children.forEach(System.out::println);
    	/*
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
        */

		if (name != null) {
    		//System.out.println("QN: '" + name +"' for obj: " + obj);
    		return name;
		}
    	
		name = super.computeFullyQualifiedName(obj);
    	if (name != null) {
    		//System.out.println("Falling back on base qn: '" + name + "' for resolution of qn for obj: " + obj);
    	}
    	
    	/*
    	if (LinkingAndScopingUtils.hasRef(obj)) {
    		
    		var str = "";
    		if (obj instanceof MemberFuncCall memberFuncCall) {
    			//test= this.getConverter().toQualifiedName(memberFuncCall.getName().getName());
    			System.out.println(memberFuncCall);
    			//str = memberFuncCall.getName().getName();
    		}
    		
    		if (obj instanceof VarMemberAccessImpl varMemberAccess) {
    			//test= this.getConverter().toQualifiedName(varMemberAccess.getName().getName());
    			System.out.println(varMemberAccess);
    			var test = varMemberAccess.basicGetName();
    			System.out.println("test: " + test);
    		}
    		
    		if (obj instanceof Var var) {
    			//test= this.getConverter().toQualifiedName(var.getName().getName());
    			//str = var.getName().getName();
    		}
    		
    		
    		//newResolver(obj.eClass(), "name");
    		System.out.println("FullyQualifiedName:");
    		//System.out.println(test != null ? test : "null");
    		System.out.println("n: " + str);
    		System.out.println(obj);
    	}
	*/
    	
    	//this.getConverter(obj);
        return name;
    }
    
	private boolean hasName(final EClass type, final String attributeName) {
		System.out.println("structureal feature: " + type.getEStructuralFeature(attributeName));
		
		final EStructuralFeature structuralFeature = type.getEStructuralFeature(attributeName);
		if (structuralFeature != null && structuralFeature instanceof EReference && !structuralFeature.isMany()) {
			return true;

		}
		
		return false;
	}
	
}
