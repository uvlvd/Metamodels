package org.xtext.lua.evaluation;

import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.xtext.lua.lua.Arg;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.GroupedExp;
import org.xtext.lua.lua.MethodCall;
import org.xtext.lua.lua.NamedFeature;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.Stat;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.lua.Var;
import org.xtext.lua.mocking.FeaturePath;
import org.xtext.lua.mocking.SyntheticVar;
import org.xtext.lua.scoping.LuaGlobalScopeProvider;
import org.xtext.lua.utils.AssignmentUtil;
import org.xtext.lua.utils.ExpUtil;
import org.xtext.lua.utils.FeatureUtil;
import org.xtext.lua.utils.LuaConstants;
import org.xtext.lua.utils.MockUtil;
import org.xtext.lua.utils.StatUtil;



public class MockInfo {
	
	public enum Cause {
		FUNCTION_RESOLUTION, // Function return values could not be resolved
		TABLE_INDEX_EXP, // TableAccess indexExp could not be resolved
		GROUPED_EXP, // feature path started with grouped exp
		IMPLICIT_IMPORT, // the implicit import files are not complete (e.g. fields are not defined in .lua files, see e.g. io.stderr)
		ARG_ACCESS, // access of an Arg (e.g. MemberAccess on member in function func(arg) arg.member end)
		VAR_NOT_FOUND,
		UNKNOWN
	}
	
	private final EObject context;
	private final Stat parentStat;
	private final String resourceUri;
	//private final Cause cause;
	private Cause cause;
	/**
	 * Whether the mocking of this {@link MockInfo} is a consequence from a previous {@link Feature} of it's 
	 * {@link FeaturePath} being mocked.
	 */
	private boolean isCausedByPreviousFeature = false;
	
	/**
	 * Creates a mock info for a given referencing element with getRef() returning a synthetic element (i.e. a mocked element)
	 * @param context the referencing element with getRef() returning a synthetic model element.
	 */
	// TODO: should probabl get a Referencing as param, whith MockUtil.isMocked(Referencing.getRef()) == true
	public MockInfo(final EObject context) {
//		if (!(context instanceof Referencing refing && refing.getRef() instanceof SyntheticVar)) {
//			// TODO: log error instead
//			assert false;
//		}
		this.context = context;
		final var parentStatOpt = StatUtil.getParentStatement(context);
		if (parentStatOpt.isPresent()) {
			parentStat = parentStatOpt.get();
		} else {
			parentStat = null;
		}
		resourceUri = context.eResource().getURI().toString();

		inferCauseType(context);
	}
	
	
	private void inferCauseType(EObject context) {
		if (context instanceof NamedFeature namedFeature) {
			final var featurePath = new FeaturePath(namedFeature);
			
			for (var feature : featurePath.getContextFeatures()) {
				final var causeOpt = inferCauseTypeForFeature(feature);
				if (causeOpt.isPresent()) {
					cause = causeOpt.get();
					isCausedByPreviousFeature = feature != context;
					return;
				}
			}
			
//			final var firstMockedFeature = findFirstMockedFeature(featurePath);
			
			// if a feature path starts with a GroupedExp, no Features of it path can be resolved
			// (at least as long as GroupedExp resolution is not implemented)
//			if (featurePath.getRoot() instanceof GroupedExp) {
//				return Cause.GROUPED_EXP;
//			}
			
			// TODO: CHECK SECOND ATTENTION BELOW, START FROM ROOT
			// probably should tackle this in a different way:
			// We start at the given context feature, traversing its path in direction of the feature path root.
			//  - for each feature on the path, we need to check why it could have caused the context to not be resolved.
			//  - if it is a
			//   	- function/methodCall: assume that the return values could not be computed
			//  	- if it is a TableAccess assume that the indexExp of the table acces could not be resolved 
		//		  (because it is a function call that could not be resolved or an expression that could not be resolved)
			//   	- MemberAccess or Var:
			//          check if the reference chain contains a mocked Referencing, if so
			//          probably return a "reulting-from-other-mocking" case (maybe with the case type of the other mocking)
			// the first feature along the reverse path encountered is assumed to be the culprit
			// ATTENTION: we should skip all Features along the path that are themselves mocked, since the cause
			//  might lie further along the path
			// ATTENTION: should contemplate wheter it would be better to just start from the root and traverse
			//  the feature path in direction of the context, since any feature along the path from root not being
			//  resolved would lead to a more deeply nested feature not being resolvable
			
//			if (originatesInImplicitImport(featurePath)) {
//				return Cause.IMPLICIT_IMPORT;
//			}
//			
//			final var previousFeature = FeatureUtil.getPreviousFeature(firstMockedFeature);
//			if (previousFeature != null) {
//				//TODO: this is based on the assumption that if a mocked feature is preceded by a fuction/methodCall
//				//      it was mocked because the call could not be resolved
//				if (previousFeature instanceof FunctionCall || previousFeature instanceof MethodCall) {
//					return Cause.FUNCTION_RESOLUTION;
//				}
//				
//				if (isIndexExpWithDummyName(previousFeature)) {
//					return Cause.TABLE_INDEX_EXP;
//				}
//			}
		}
		cause = Cause.UNKNOWN;
	}
	
	/**
	 * Used when traversing a feature path from PrefixExp to context feature. This method
	 * assumes that it is called while traversing a feature path from the {@link PrefixExp} to
	 * any feature along the path, i.e. it has already been called for previous features 
	 * in the given {@link Feature}'s feature path.
	 */
	private Optional<Cause> inferCauseTypeForFeature(Feature feature) {
		Cause cause = null;
		
		// TODO: check MockInfo examples with this cause to see if this
		//     is corret (i.e. the var is really not found anywhere, not another cause)
		if (feature instanceof Var var && MockUtil.referencesMocked(var)) {
			cause = Cause.VAR_NOT_FOUND;
		}
		
		else if (originatesInArg(feature)) {
			cause = Cause.ARG_ACCESS;
		}
		
		else if (feature instanceof NamedFeature namedFeature && originatesInImplicitImport(namedFeature)) {
			cause = Cause.IMPLICIT_IMPORT;
		}
		
		else if (feature instanceof GroupedExp) {
			cause = Cause.GROUPED_EXP;
		}
		
		else if(feature instanceof FunctionCall || feature instanceof MethodCall) {
			cause = Cause.FUNCTION_RESOLUTION;
		}
		
		else if (isIndexExpWithDummyName(feature)) {
			cause = Cause.TABLE_INDEX_EXP;
		}
		
		if (cause != null) {
			return Optional.of(cause);
		}
		return Optional.empty();
//		if(feature instanceof Referencing referencing && MockUtil.referencesMocked(referencing)) {
//			return Optional.of(cause);
//		} 
//		
//		return Optional.empty();
	}
	
	private boolean isIndexExpWithDummyName(Feature feature) {
		// TODO: return the part in the if!
//		if (feature instanceof TableAccess ta && ExpUtil.isTableAccessWithDerivedDummyName(ta)) {
//			return ta.getIndexExp().getName().equals(LuaConstants.LINKING_DUMMY_NAME);
//		}
		return feature instanceof TableAccess ta && ExpUtil.isTableAccessWithLinkingDummyName(ta);
	}
	
	private Feature findFirstMockedFeature(FeaturePath featurePath) {
		for (final var feature : featurePath.getContextFeatures()) {
			if (feature instanceof NamedFeature namedFeature && MockUtil.isMocked(namedFeature.getRef())) {
				return feature;
			}
		}
		throw new RuntimeException("Could not find mocked feature while creating mock info for feature " + context);
	}
	
	private boolean originatesInImplicitImport(final FeaturePath featurePath) {
		final var root = featurePath.getRoot();
		if (root instanceof NamedFeature namedFeature) {
			return originatesInImplicitImport(namedFeature);
		}
		return false;
	}
	
	/**
	 * Check if the given Feature path originates in an implicit import (i.e. a Lua language library), by checking if any
	 * feature path root in the reference chain is part of an implicit resource.
	 */
	private boolean originatesInImplicitImport(final Referencing referencing) {
		// can only originate in implicit import if not testing for itself
		// all references in implicit library files should be resolved
		if (referencing.equals(getContext())) {
			return false;
		}
		
		final var ref = referencing.getRef();
		if (MockUtil.isMocked(ref)) {
			// If the ref is mocked, we followed the reference chain to another
			// mocked object, which has its own cause for being mocked.
			
			//TODO: we should mark this as a follows-from-other-mocked case when
			// the referencing does not equal the 
			//cause = Cause.TEMP_NULL;
			return false;
		}
//		if (ref == null) {
//			cause = Cause.TEMP_NULL;
//			return false;
//		}
		final var resource = ref.eResource();
		final var isImplicit = LuaGlobalScopeProvider.isImplicitResource(resource);
		if (isImplicit) {
			return true;
		} else if (ref instanceof NamedFeature referencedFeature) {
			// check referenced object's root
			return originatesInImplicitImport(new FeaturePath(referencedFeature));
		} else if (ref instanceof Referencing referencedReferencing) {
			return originatesInImplicitImport(referencedReferencing);
		}
		return false;
		
//		final var root = featurePath.getRoot();
//		// check if root references a non-assigned Referenceable
//		if (root instanceof NamedFeature rootFeature) {
//			final var ref = rootFeature.getRef();
//			if (ref == null) {
//				cause = Cause.TEMP_NULL;
//				return false;
//			}
//			final var resource = ref.eResource();
//			final var isImplicit = LuaGlobalScopeProvider.isImplicitResource(resource);
//			if (isImplicit) {
//				return true;
//			} else if (ref instanceof NamedFeature referencedFeature) {
//				// check referenced object's root
//				return originatesInImplicitImport(new FeaturePath(referencedFeature));
//			}
//		}
//		return false;
	}
	
	private boolean originatesInArg(Feature feature) {
		final var featurePath = new FeaturePath(feature);
		final var namedPrefixOpt = featurePath.findPrefixAsVar();
		if (namedPrefixOpt.isPresent()) {
			final var namedPrefix = namedPrefixOpt.get();
			final var ref = namedPrefix.getRef();
			if (ref == null) {
				return false;
			}
			
			if (ref instanceof Arg) {
				return true;
			}
			
			if (ref instanceof Feature referencedFeature) {
				return originatesInArg(referencedFeature);
			}
		}
		return false;
	}

	public EObject getContext() {
		return context;
	}

	public Stat getParentStat() {
		return parentStat;
	}

	public String getResourceUri() {
		return resourceUri;
	}

	public Cause getCause() {
		return cause;
	}
	
	public boolean isCausedByPreviousFeature() {
		return isCausedByPreviousFeature;
	}
	

}
