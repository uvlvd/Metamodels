package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.xtext.lua.lua.ExpStringLiteral;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.LuaPackage.Literals;
import org.xtext.lua.lua.MemberAccess;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.lua.Var;

import com.google.inject.Inject;


public class LuaQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {
	
  	
	@Inject 
	private LinkingHelper linkingHelper;
	
	@Override
    protected QualifiedName computeFullyQualifiedName(final EObject obj) {

		QualifiedName name = null;

        name = super.computeFullyQualifiedName(obj);
        if (name == null) {
        	//System.out.println(obj);
        }
        
        
        //System.out.println("FQN for obj " + obj + ": " + name);
        return name;
		//System.out.println(obj);
		//return super.computeFullyQualifiedName(obj);
		//return this.getConverter().toQualifiedName("testName"); // TODO: remove
    }
    	
}
