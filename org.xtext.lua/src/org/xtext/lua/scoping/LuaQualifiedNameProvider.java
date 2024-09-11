package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.xtext.lua.lua.FuncCall;
import org.xtext.lua.lua.LuaPackage.Literals;
import org.xtext.lua.lua.MemberAccess;
import org.xtext.lua.lua.Var;

import com.google.inject.Inject;


public class LuaQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {
	
  	
	@Inject 
	private LinkingHelper linkingHelper;
	
	@Override
    protected QualifiedName computeFullyQualifiedName(final EObject obj) {

		QualifiedName name = null;
		
		if (obj instanceof Var var) {
	    	var refNode = NodeModelUtils.findNodesForFeature(var, Literals.VAR__NAME).get(0);
	    	var refString = linkingHelper.getCrossRefNodeAsString(refNode, true);
	    	name = this.getConverter().toQualifiedName(refString);
		}
		
		if (obj instanceof MemberAccess memberAccess) {
	    	var refNode = NodeModelUtils.findNodesForFeature(memberAccess, Literals.MEMBER_ACCESS__NAME).get(0);
	    	var refString = linkingHelper.getCrossRefNodeAsString(refNode, true);
	    	name = this.getConverter().toQualifiedName(refString);
		}
		
		// TODO?
		if (obj instanceof FuncCall funcCall) {
			//var refNode = NodeModelUtils.findNodesForFeature(funcCall, Literals.FUNC_CALL__OBJECT).get(0);
	    	//var refString = linkingHelper.getCrossRefNodeAsString(refNode, true);
	    	//System.out.println("TODO: implement FuncCall fqn?");
	    	//System.out.println(this.getConverter().toQualifiedName(refString));
		}
		
		//System.out.println("Returning fqn '"  + name + "' for obj " + obj);
		


        return name;
    }
    	
}
