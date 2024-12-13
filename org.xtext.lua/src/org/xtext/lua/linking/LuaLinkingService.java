package org.xtext.lua.linking;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.linking.lazy.SyntheticLinkingSupport;
import org.eclipse.xtext.nodemodel.INode;
import org.xtext.lua.lua.ExpFunctionDeclaration;
import org.xtext.lua.lua.FuncBody;
import org.xtext.lua.lua.IndexExpField;
import org.xtext.lua.lua.LuaPackage.Literals;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.utils.AssignmentUtil;
import org.xtext.lua.utils.ExpUtil;
import org.xtext.lua.utils.LuaConstants;
import org.xtext.lua.utils.ReferenceableUtil;

import com.google.inject.Inject;

public class LuaLinkingService extends DefaultLinkingService {
	
	public static final URI NIL_MOCK_URI = URI.createURI("dummy:/syntheticNilValues.lua");
	
	private ImplicitSelfArgs implicitSelfArgs = new ImplicitSelfArgs();
	
	@Inject
	private SyntheticLinkingSupport linkingSupport;
	
	@Inject
	private IMockObjectCreator mockObjectCreator;
	
	private AtomicBoolean isTableAccessNamesResolved = new AtomicBoolean(false);
  	
	@Override
	public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) throws IllegalNodeException {
		
		// first, try to resolve all table access names and references to avoid cyclic reference resolution
		// (could probably also be handled in some other way in the scopeProvider implementation)
		// TODO: would probably be better is this could somehow be executed as a "first step" of the linking via an api method
		if (isTableAccessNamesResolved.compareAndSet(false, true)) {
			resolveAllTableAccessNamesAndRefsInContextRoot(context);
			resolveAllFieldNamesAndRefsInContextRoot(context);
			// only call after other names have been resolved, makes use of TableAccess and Field names
			resolveAllExpFunctionDeclarationNamesInContextRoot(context);
		}

		var linkedObjects = super.getLinkedObjects(context, ref, node);
		
		if (linkedObjects.isEmpty()) {
			
        	// handle implicit self parameters for method declarations
        	if (ReferenceableUtil.referencesImplicitSelfParam(context)) {
        		// referencesImplicitSelfParam ensures that funcBody is present
        		var containingFuncBody = EcoreUtil2.getContainerOfType(context, FuncBody.class);
        		var selfArg = implicitSelfArgs.getSelfArgFor(containingFuncBody);
        		return Collections.singletonList(selfArg);
        	}
        	
        	var mockedObject = mockObjectCreator.createMockObjectFor(context);
        	if (mockedObject != null) {
        		return Collections.singletonList(mockedObject);
        	}
		}
		
		return linkedObjects;
	}
	
	private void resolveAllTableAccessNamesAndRefsInContextRoot(EObject context) {
		var scopeRoot = EcoreUtil2.getRootContainer(context);
		var tas = EcoreUtil2.getAllContentsOfType(scopeRoot, TableAccess.class);
		for (var ta : tas) {
			if (ExpUtil.isTableAccessWithDummyName(ta)) {
				ta.setName(LuaConstants.LINKING_DUMMY_NAME);
				var name = ExpUtil.tryResolveExpressionToString(ta.getIndexExp(), LuaConstants.LINKING_DUMMY_NAME);
				if (name != null) {
					ta.setName(name);
					linkingSupport.createAndSetProxy(ta, Literals.REFERENCING__REF, name);
					ta.setRef(ta.getRef());
				} else {
					// reference is resolved later or mocked via mockObjectCreator
				}
			}
		}
	}
	
	private void resolveAllFieldNamesAndRefsInContextRoot(EObject context) {
		var scopeRoot = EcoreUtil2.getRootContainer(context);
		var indexExpFields = EcoreUtil2.getAllContentsOfType(scopeRoot, IndexExpField.class);
		for (var indexExpField : indexExpFields) {
			if (ExpUtil.isIndexExpFieldWithDummyName(indexExpField)) {
				indexExpField.setName(LuaConstants.LINKING_DUMMY_NAME);
				var name = ExpUtil.tryResolveExpressionToString(indexExpField.getIndexExp(), LuaConstants.LINKING_DUMMY_NAME);
				if (name != null) {
					indexExpField.setName(name);
					linkingSupport.createAndSetProxy(indexExpField, Literals.REFERENCING__REF, name);
					indexExpField.setRef(indexExpField.getRef());
				} else {
					// reference is resolved later or mocked via mockObjectCreator
				}
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
	private void resolveAllExpFunctionDeclarationNamesInContextRoot(final EObject context) {
		final var scopeRoot = EcoreUtil2.getRootContainer(context);
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
