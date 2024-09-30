package org.xtext.lua.scoping;

import java.util.Collections;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.util.PolymorphicDispatcher;
import org.eclipse.xtext.util.Strings;
import org.xtext.lua.linking.LuaLinkingService;
import org.xtext.lua.lua.Arg;
import org.xtext.lua.lua.ExpList;
import org.xtext.lua.lua.ExpStringLiteral;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.FunctionDeclaration;
import org.xtext.lua.lua.LuaPackage.Literals;
import org.xtext.lua.utils.LinkingAndScopingUtils;
import org.xtext.lua.lua.MemberAccess;
import org.xtext.lua.lua.ParamArgs;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.lua.Var;

import com.google.inject.Inject;


public class LuaQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {
	private static final Logger LOGGER = Logger.getLogger(LuaQualifiedNameProvider.class);
	
	@Inject 
	private LinkingHelper linkingHelper;
	
	@Override
    protected QualifiedName computeFullyQualifiedName(final EObject obj) {
		QualifiedName name = null;
		
		name = super.computeFullyQualifiedName(obj);
        if (name == null) { // TODO: only used for debug
        	//System.out.println(obj);
        } else {
        	//System.out.println("FQN: " + name);
        }
        
        return name;

    }
	
	
	@Override
	protected QualifiedName computeFullyQualifiedNameFromNameAttribute(EObject obj) {
		String name = getNameStr(obj);
		if (Strings.isEmpty(name)) {
			name = getResolver().apply(obj);
		}
		if (Strings.isEmpty(name))
			return null;
		
		QualifiedName qualifiedNameFromConverter = getConverter().toQualifiedName(name);
		
		// we do not consider parent names for function call arguments
		var isParamArg = EcoreUtil2.getContainerOfType(obj, ParamArgs.class) != null;
		if (isParamArg) {
			// TODO: need to return full path of param, not just the last part of name 
			// (equal to the stopAtTableAccess)
			//return qualifiedNameFromConverter;
		} 
	
		
		var isReferencing = obj instanceof Referencing;
		while (obj.eContainer() != null 
				// TODO: document! We use names separated by "." as fqn, but for variables in TableAccesses we need to stop at the TableAccess "border"
				// so a["member"] is a.member but a[str] is a.str_content (i.e. content of the str var)
				&& !(isReferencing && (obj.eContainer() instanceof TableAccess))
				// stop at ExpList for paramArgs
				&& !(isParamArg && (obj.eContainer() instanceof ExpList))) {
			obj = obj.eContainer();
			QualifiedName parentsQualifiedName = getFullyQualifiedName(obj);
			if (parentsQualifiedName != null)
				return parentsQualifiedName.append(qualifiedNameFromConverter);
		}
		return qualifiedNameFromConverter;
	}
	
	private String getNameStr(EObject obj) {
		if (obj instanceof MemberAccess ma) 
			return getMemberAccessQualifiedName(ma);
		if (obj instanceof TableAccess ta)
			return getTableAccessQualifiedName(ta);
		if (obj instanceof FunctionDeclaration decl)
			return getFunctionDeclarationQualifiedName(decl);
		return "";
	}
	
	private String getMemberAccessQualifiedName(MemberAccess ma) {
		return "[\"" + ma.getName() + "\"]";
	}
	
	private String getFunctionDeclarationQualifiedName(FunctionDeclaration decl) {
		var name = decl.getName();
		var qn = getConverter().toQualifiedName(name);
		if (qn.getSegmentCount() > 1) {
			var last = qn.getLastSegment();
			last = "[\"" + last + "\"]";
			var result = qn.skipLast(1);
			result = result.append(last);
			return result.toString();
		}
		return name;
	}
	
	private String getTableAccessQualifiedName(TableAccess ta) {
		//var name = LinkingAndScopingUtils.tryResolveExpressionToString(ta.getIndexExp());
		var name = ta.getName();
		if (name != null) {
			return "[" +  name + "]";
		}
		return ""; // TODO: might want to return dummy name here, instead of in LinkingAndScopingUtils.tryResolveExpressionToString
	}
    	
}
