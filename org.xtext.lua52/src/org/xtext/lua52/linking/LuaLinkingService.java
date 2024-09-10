package org.xtext.lua52.linking;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.linking.LinkingScopeProviderBinding;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.xtext.lua52.lua52.Lua52Package.Literals;
import org.xtext.lua52.lua52.Var;
import org.xtext.lua52.lua52.VarMemberAccessRhs;
import org.xtext.lua52.lua52.VarRhs;

import com.google.inject.Inject;

public class LuaLinkingService extends DefaultLinkingService {
	private static final Logger LOGGER = Logger.getLogger(LuaLinkingService.class.getPackageName());
	@Inject
	@LinkingScopeProviderBinding
	private IScopeProvider scopeProvider;

	//@Inject
	//private Provider<ImportedNamesAdapter> importedNamesAdapterProvider;
	
	//@Inject 
	//private LinkingHelper linkingHelper;
	
	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;
	
	@Override
	public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) throws IllegalNodeException {
		final EClass requiredType = ref.getEReferenceType();
		if (requiredType == null) {
			return Collections.<EObject>emptyList();
		}
		//final String crossRefString = getCrossRefNodeAsString(node);
		final String crossRefString = getCrossRefString(context, node);
		if (crossRefString == null || crossRefString.equals("")) {
			return Collections.<EObject>emptyList();
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("before getLinkedObjects: node: '" + crossRefString + "'");
		}
		System.out.println("before getLinkedObjects: node: '" + crossRefString + "'");
		final IScope scope = getScope(context, ref);
		if (scope == null) {
			throw new AssertionError(
					"Scope provider " + scopeProvider.getClass().getName() + " must not return null for context "
							+ context + ", reference " + ref + "! Consider to return IScope.NULLSCOPE instead.");
		}
		final QualifiedName qualifiedLinkName = qualifiedNameConverter.toQualifiedName(crossRefString);
		final IEObjectDescription eObjectDescription = scope.getSingleElement(qualifiedLinkName);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("after getLinkedObjects: node: '" + crossRefString + "' result: " + eObjectDescription);
		}
		System.out.println("after getLinkedObjects: node: '" + crossRefString + "' result: " + eObjectDescription);
		if (eObjectDescription == null) {
			return Collections.emptyList();
		}
		final EObject result = eObjectDescription.getEObjectOrProxy();
		return Collections.singletonList(result);
	}
	
	private String getCrossRefString(EObject context, INode node) {
		if (context instanceof VarMemberAccessRhs) {
			var vars = EcoreUtil2.getAllContentsOfType(context, VarRhs.class);
			if (vars.size() != 1) {
				throw new RuntimeException("Exactly on VarRhs expected, found " + vars.size());
			}
			
			var v = vars.get(0);
			var varRefNode = NodeModelUtils.findNodesForFeature(v, Literals.VAR_RHS__REFERENCING).get(0);
			return getCrossRefStringRecursive(getCrossRefNodeAsString(varRefNode), v, context, node);
		}
		
		var result = getCrossRefNodeAsString(node);
		System.out.println("Info: Returning default crossRefString '" + result + "'");
		return result;
	}
	
	private String getCrossRefStringRecursive(String acc, EObject start, EObject end, INode node) {
		var parent = start.eContainer();
		if (parent != null && parent instanceof VarMemberAccessRhs memberAccess) {
			var parentRef = NodeModelUtils.findNodesForFeature(parent, Literals.VAR_MEMBER_ACCESS_RHS__REFERENCING);
			if (parentRef.size() != 1) {
				throw new RuntimeException("Parent contains ref node count != 1!");
			}
			
			if (parent == end) {
				return acc + '.' + getCrossRefNodeAsString(parentRef.get(0));
			}
			return getCrossRefString(acc + '.' + getCrossRefNodeAsString(parentRef.get(0)), parent, parentRef.get(0));
		}

		return acc;
	}
	
	private String getCrossRefString(String acc, EObject context, INode node) {
		var parent = context.eContainer();
		if (parent != null && parent instanceof VarMemberAccessRhs memberAccess) {
			var parentRef = NodeModelUtils.findNodesForFeature(parent, Literals.VAR_MEMBER_ACCESS_RHS__REFERENCING);
			if (parentRef.size() != 1) {
				throw new RuntimeException("Parent contains ref node count != 1!");
			}
			return getCrossRefString(acc + '.' + getCrossRefNodeAsString(parentRef.get(0)), parent, parentRef.get(0));
			
		}
		return acc;
	}
}
