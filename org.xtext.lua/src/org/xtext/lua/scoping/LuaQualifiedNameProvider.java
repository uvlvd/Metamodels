package org.xtext.lua.scoping;

import java.util.Collections;
import java.util.function.Predicate;

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
import org.xtext.lua.lua.Field;
import org.xtext.lua.lua.FuncBody;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.FunctionDeclaration;
import org.xtext.lua.lua.LocalVar;
import org.xtext.lua.lua.LuaPackage.Literals;
import org.xtext.lua.utils.LinkingAndScopingUtils;
import org.xtext.lua.lua.MemberAccess;
import org.xtext.lua.lua.NameField;
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
		
		// Special cases
		// Fields in TableCounstructors that are part of Assignments or nested TableConstructors have their assigned table as part of the fqn
		if (obj instanceof Field field) 
			return getQualifiedNameForField(field, qualifiedNameFromConverter);
		// we do not consider parent names (i.e. feature paths) for function arguments
		if (obj instanceof Arg) {
			return qualifiedNameFromConverter;
		} 

		// Other cases, name is build using all parent names until stop condition is true
		while (obj.eContainer() != null && !isStopConditionFor(obj)) {
			obj = obj.eContainer();
			QualifiedName parentsQualifiedName = getFullyQualifiedName(obj);
			if (parentsQualifiedName != null)
				return parentsQualifiedName.append(qualifiedNameFromConverter);
		}
		return qualifiedNameFromConverter;
	}
	
	private boolean isStopConditionFor(EObject obj) {
		final var isParamArg = EcoreUtil2.getContainerOfType(obj, ParamArgs.class) != null;
		final var isVarInsideTableAcccess = !(obj instanceof TableAccess) 
											&& (obj instanceof Var || obj instanceof LocalVar)
											&& (obj.eContainer() instanceof TableAccess);
		
		// TODO: document! We use names separated by "." as fqn, but for variables in TableAccesses we need to stop at the TableAccess "border"
		// so a["member"] is a.member but a[str] is a.str_content (i.e. content of the str var)
		return isVarInsideTableAcccess
		// stop at ExpList for paramArgs
		|| (isParamArg && (obj.eContainer() instanceof ExpList))
		// always stop at FuncBody (do not consider names outside of FuncBody or func name)
		|| (obj.eContainer() instanceof FuncBody);
	}
	
	private String getNameStr(EObject obj) {
		if (obj instanceof MemberAccess ma) 
			return getMemberAccessNameStr(ma);
		if (obj instanceof TableAccess ta)
			return getTableAccessNameStr(ta);
		if (obj instanceof FunctionDeclaration decl)
			return getFunctionDeclarationNameStr(decl);
		if (obj instanceof Field field)
			return getFieldNameStr(field);
		return "";
	}
	
	private String getFieldNameStr(Field field) {
		if (field instanceof NameField) {
			return getMemberAccessNameStr(field.getName());
		}
		return getTableAccessNameStr(field.getName());
	}
	
	private QualifiedName getQualifiedNameForField(Field field, QualifiedName qualifiedNameFromConverter) {
		var tableOpt = LinkingAndScopingUtils.findTableForField(field);
		if (tableOpt.isPresent()) {
			var tableFqn = getFullyQualifiedName(tableOpt.get());
			var result= tableFqn.append(qualifiedNameFromConverter);
			return result;
		}
		
		return qualifiedNameFromConverter;

	}
	
	private String getMemberAccessNameStr(MemberAccess ma) {
		return getMemberAccessNameStr(ma.getName());
	}
	
	private String getMemberAccessNameStr(String name) {
		//return "[\"" + name + "\"]";
		return "[" +  name + "]";
	}
	
	private String getFunctionDeclarationNameStr(FunctionDeclaration decl) {
		var name = decl.getName();
		var qn = getConverter().toQualifiedName(name);
		if (qn.getSegmentCount() > 1) {
			var last = qn.getLastSegment();
			last = "[" + last + "]";
			var result = qn.skipLast(1);
			result = result.append(last);
			return result.toString();
		}
		return name;
	}
	
	private String getTableAccessNameStr(TableAccess ta) {
		return getTableAccessNameStr(ta.getName());
	}
	
	private String getTableAccessNameStr(String name) {
		if (name != null) {
			return "[" +  name + "]";
		}
		return ""; // TODO: might want to return dummy name here, instead of in LinkingAndScopingUtils.tryResolveExpressionToString
	}
    	
}
