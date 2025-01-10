package org.xtext.lua.postprocessing;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.linking.lazy.SyntheticLinkingSupport;
import org.xtext.lua.lua.ExpFunctionDeclaration;
import org.xtext.lua.lua.IndexExpField;
import org.xtext.lua.lua.LuaPackage.Literals;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.utils.AssignmentUtil;
import org.xtext.lua.utils.ExpUtil;
import org.xtext.lua.utils.LuaConstants;

import com.google.inject.Inject;

/**
 * This class is used to prepare a resource for the linking step. The state installed in the {@link LuaDerivedStateComputer} is
 * processed further to resolve names and references is far as possible before linking.
 * @author jsaenz
 */
public class LuaResourcePostProcessor {
	
	@Inject
	private SyntheticLinkingSupport linkingSupport;

    public void process(Resource resource) {
    	if (resource.getContents().size() > 0) {
    		var root = resource.getContents().get(0);
    		process(root);
    	} 
    }
    
    public void process(EObject model) {
    	performPostProcessing(model);
    }

    private void performPostProcessing(EObject model) {
    	resolveAllTableAccessNamesAndRefsInContextRoot(model);
		resolveAllFieldNamesAndRefsInContextRoot(model);
		// only call after other names have been resolved, makes use of TableAccess and Field names
		resolveAllExpFunctionDeclarationNamesInContextRoot(model);
    }
    

	private void resolveAllTableAccessNamesAndRefsInContextRoot(EObject model) {
		var scopeRoot = EcoreUtil2.getRootContainer(model);
		var tas = EcoreUtil2.getAllContentsOfType(scopeRoot, TableAccess.class);
		for (var ta : tas) {
			if (ExpUtil.isTableAccessWithDerivedDummyName(ta)) {
				ta.setName(LuaConstants.LINKING_DUMMY_NAME);
				var name = ExpUtil.tryResolveExpressionToString(ta.getIndexExp(), LuaConstants.LINKING_DUMMY_NAME);
				// if name resolves to default, reference is resolved later or mocked via
				// mockObjectCreator
				ta.setName(name);
				linkingSupport.createAndSetProxy(ta, Literals.REFERENCING__REF, name);
				ta.setRef(ta.getRef());

			}
		}
	}
	
	private void resolveAllFieldNamesAndRefsInContextRoot(EObject model) {
		var scopeRoot = EcoreUtil2.getRootContainer(model);
		var indexExpFields = EcoreUtil2.getAllContentsOfType(scopeRoot, IndexExpField.class);
		for (var indexExpField : indexExpFields) {
			if (ExpUtil.isIndexExpFieldWithDummyName(indexExpField)) {
				indexExpField.setName(LuaConstants.LINKING_DUMMY_NAME);
				var name = ExpUtil.tryResolveExpressionToString(indexExpField.getIndexExp(),
						LuaConstants.LINKING_DUMMY_NAME);
				// if name resolves to default, reference is resolved later or mocked via
				// mockObjectCreator
				indexExpField.setName(name);
				linkingSupport.createAndSetProxy(indexExpField, Literals.REFERENCING__REF, name);
				indexExpField.setRef(indexExpField.getRef());
			}
		}
	}
	
	/**
	 * Sets the name of all {@link ExpFunctionDeclaration} contained in the given context to the name of their
	 * corresponding assignable, if the assignable can be determined.</br>
	 * E.g. For the Lua snippet: </br>
	 * 		{@code func = function() end}</br> 
	 * the {@code name} attribute of the {@link ExpFunctionDeclaration} {@code function() end} would be set to "func".</br></br>
	 * 
	 * <i>This assumes that the names for the corresponding assignables (e.g. TableAccesses, Fields, etc.) have been set beforehand!</i>
	 * @param context
	 */
	private void resolveAllExpFunctionDeclarationNamesInContextRoot(final EObject model) {
		final var scopeRoot = EcoreUtil2.getRootContainer(model);
		final var expFunctionDecls = EcoreUtil2.getAllContentsOfType(scopeRoot, ExpFunctionDeclaration.class);
		for (var expFunctionDecl : expFunctionDecls) {
			final var assignableOpt = AssignmentUtil.findAssignableForExp(expFunctionDecl);
			if (assignableOpt.isPresent()) {
				final var assignable = assignableOpt.get();
				// we assume that all possible assignables returned here have their name attribute already set
				expFunctionDecl.setName(assignable.getName());
			} else {
				// We assume that all assignables are returned by the findAssignableForExp function
				// and any other ExpFunctionDeclarations are unnamed functions (e.g. as part of the arguments of a function call)
			}
		}
	}
}
