package org.xtext.lua.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.util.Strings;
import org.xtext.lua.lua.ExpStringLiteral;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.LuaPackage.Literals;
import org.xtext.lua.lua.MemberAccess;
import org.xtext.lua.lua.Referencing;
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
        if (name == null) { // TODO: only used for debug
        	//System.out.println(obj);
        }
        return name;
    }
	
	@Override
	protected QualifiedName computeFullyQualifiedNameFromNameAttribute(EObject obj) {
		String name = getResolver().apply(obj);
		if (Strings.isEmpty(name))
			return null;
		QualifiedName qualifiedNameFromConverter = getConverter().toQualifiedName(name);
	
		var stopAtTableAccess = obj instanceof Referencing;
		while (obj.eContainer() != null 
				// TODO: document! We use names separated by "." as fqn, but for variables in TableAccesses we need to stop at the TableAccess "border"
				// so a["member"] is a.member but a[str] is a.str_content (i.e. content of the str var)
				&& !(stopAtTableAccess && (obj.eContainer() instanceof TableAccess))) {
			obj = obj.eContainer();
			QualifiedName parentsQualifiedName = getFullyQualifiedName(obj);
			if (parentsQualifiedName != null)
				return parentsQualifiedName.append(qualifiedNameFromConverter);
		}
		return qualifiedNameFromConverter;
	}
    	
}
