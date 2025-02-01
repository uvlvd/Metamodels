package org.xtext.lua.evaluation;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.evaluation.SyntheticReferenceEvalData.SyntheticReferenceCauseEvalData;
import org.xtext.lua.evaluation.SyntheticReferenceInfo.Cause;
import org.xtext.lua.evaluation.SyntheticReferenceInfo.Type;
import org.xtext.lua.lua.Arg;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.GroupedExp;
import org.xtext.lua.lua.MemberAccess;
import org.xtext.lua.lua.MethodCall;
import org.xtext.lua.lua.NamedFeature;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.lua.Var;
import org.xtext.lua.mocking.FeaturePath;
import org.xtext.lua.scoping.LuaGlobalScopeProvider;
import org.xtext.lua.utils.FeatureUtil;
import org.xtext.lua.utils.LuaRequireUtil;
import org.xtext.lua.utils.MockUtil;

public class SyntheticReferenceInfoCollector {
	private static final Logger LOGGER = Logger.getLogger(SyntheticReferenceInfoCollector.class);
	
	private Map<EObject, SyntheticReferenceInfo> infos = new HashMap<>();
	
	public Map<Type, SyntheticReferenceEvalData> getSyntheticReferenceTypeEvalData(final ResourceSet codeModel, final int totalReferences) {
		clear();
		collect(codeModel);
		
		var result = new EnumMap<Type, SyntheticReferenceEvalData>(Type.class);
		Stream.of(Type.values()).forEach(type -> result.put(type, new SyntheticReferenceEvalData()));
		
		final var typeToCauseToCount = getSyntheticReferenceTypeCountByCause();
		final var totalSyntheticReferences = typeToCauseToCount.values()
				.stream()
				.flatMap(causes -> causes.values().stream())
				.mapToInt(Integer::intValue)
				.sum();
		
		for (final var entry : typeToCauseToCount.entrySet()) {
			
			final var type = entry.getKey();
			final var causes = entry.getValue();
			
			final var totalForType = causes.values().stream().mapToInt(Integer::intValue).sum();
			final var percentageOfSyntheticReferences = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(totalForType, totalSyntheticReferences));
			final var percentageOfAllReferences = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(totalForType, totalReferences));
			
			var syntheticReferenceEvalData = result.get(type);
			syntheticReferenceEvalData.setType(type);
			syntheticReferenceEvalData.setNumberTotal(totalForType);
			syntheticReferenceEvalData.setPercentOfTotalSyntheticReferences(percentageOfSyntheticReferences);
			syntheticReferenceEvalData.setPercentOfAllReferences(percentageOfAllReferences);
			
			var causesEvalData = new EnumMap<Cause, SyntheticReferenceCauseEvalData>(Cause.class);
			causes.entrySet().forEach(causeEntry -> {
				final var cause = causeEntry.getKey();
				final var count = causeEntry.getValue();
				final var percentageForType = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(count, totalForType));
				
				var causeEvalData = syntheticReferenceEvalData.new SyntheticReferenceCauseEvalData();
				causeEvalData.setNumberTotal(count);
				causeEvalData.setPercentOfType(percentageForType);
				
				causesEvalData.put(cause, causeEvalData);
			});
			
			syntheticReferenceEvalData.setCauses(causesEvalData);
		}
		
		return result;
	}
	
	private void collect(ResourceSet codeModel) {
		for (final var res : codeModel.getResources()) {
			var root = res.getContents().get(0);
			final var syntheticReferences = EcoreUtil2.getAllContentsOfType(root, Referencing.class)
					.stream()
					.filter(MockUtil::referencesMocked)
					.toList();
			
			for (final var referencing: syntheticReferences) {
				getInfoFor(referencing);
			}
		}
	}
	
	public Map<Type, Map<Cause, Integer>> getSyntheticReferenceTypeCountByCause() {
		var result = new EnumMap<Type, Map<Cause, Integer>>(Type.class);
		Stream.of(Type.values()).forEach(type -> result.put(type, getEmptyCauseMap()));
		
		for (var info : infos.values()) {
			final var type = info.getType();
			final var cause = info.getCause();
			final var currentCount = result.get(type).get(cause);
			result.get(type).put(cause, currentCount + 1);			
		}
		
		return result;
	}
	
	private Map<Cause, Integer> getEmptyCauseMap() {
		var result = new EnumMap<Cause, Integer>(Cause.class);
		Stream.of(Cause.values()).forEach(cause -> result.put(cause, 0));
		return result;
	}
	
	public void clear() {
		infos.clear();
	}
	
	/**
	 * Returns the reference info and triggers its computation if it is not yet available in the cache.
	 * @param referencing
	 * @return
	 */
	private SyntheticReferenceInfo getInfoFor(Referencing referencing) {
		if (infos.get(referencing) == null) {
			addSyntheticReferenceInfo(referencing);
		}
		return infos.get(referencing);
	}
	
	private void addSyntheticReferenceInfo(Referencing referencing) {
		if (referencing instanceof NamedFeature feature) {//Feature feature) { // probably always instanceof NamedFeature
			final var type = inferType(feature);
			final var cause = inferCause(feature);
			final var info = new SyntheticReferenceInfo(feature, type, cause);
			infos.put(feature, info);
		} else {
			LOGGER.error("Unexpected non-Feature type synthetic reference found for referencing: " + referencing);
		}
	}
	

	private Type inferType(final Feature feature) {
		if (feature instanceof FunctionCall || feature instanceof MethodCall) {
			return Type.FUNCTION_CALL;
		}
		if (feature instanceof TableAccess || feature instanceof MemberAccess) {
			return Type.TABLE_ACCESS;
		}
		if (feature instanceof Var || feature instanceof GroupedExp) {
			return Type.ROOT_FEATURE;
		}
		return Type.UNKNOWN;
	}
	
	// assumes the contextFeature references a synthetic element (via contextFeature.getRef())
	private Cause inferCause(final Feature contextFeature) {
		final var featurePath = new FeaturePath(contextFeature);
		
		// for each Feature along the path, we check if it's resolvable.
		// if it is not, the cause for the given feature is the same as the cause for the feature along the path
		for (final var feature: featurePath.getContextFeatures()) {
			// handle root feature being GroupedExp
			if (feature instanceof GroupedExp) {
				return Cause.GROUPED_EXP;
			}
			
			
			// we check if any features along the path (previous to the contextFeature) reference a synthetic element
			// the first feature along the path that does so defines the cause.
			if (feature != contextFeature && feature instanceof NamedFeature namedFeature) {
				if (MockUtil.referencesMocked(namedFeature)) {
					return getInfoFor(namedFeature).getCause();
				}
//				final var referenced = namedFeature.getRef();
//				if (MockUtil.isMocked(referenced)) {
//					return getInfoFor(namedFeature).getCause();
//				}
			}
			
			// If no referencing features (NamedFeatures) along the path previous to the contextFeature
			// referenced a synthetic element, the contextFeature itself must contain the cause.
			// The condition feature == contextFeature is needed to skip non-named features like FunctionCall
			if (feature == contextFeature) {
				return getCauseForFeature(feature);
			}
		}
		
		LOGGER.error("Unexpectedly could not infer the cause for synthetic reference for: " + contextFeature);
		return Cause.UNEXPECTED;
	}
	
	private Cause getCauseForFeature(final Feature unresolvableFeature) {
		if (unresolvableFeature instanceof NamedFeature feature) {
			final var referenced = feature.getRef();
			
			// handle implicit imports: check if feature path starts with "require" etc.
			if (originatesInImplicitImport(new FeaturePath(feature))) {
				return Cause.IMPLICIT_IMPORT;
			}
			
			if (referenced instanceof Arg) {
				return Cause.ARG_ACCESS;
			} else if (feature instanceof Var) {
				return Cause.VAR_NOT_FOUND;
			}
			
			final var previous = FeatureUtil.getPreviousFeature(unresolvableFeature);
			// previous feature should be available here
			if (previous == null) {
				LOGGER.error("Could not determine previous Feature for unresolvable feature: " + unresolvableFeature);
				return Cause.UNIDENTIFIED;
			}
			// assumption: return values of previous function call feature could not be computed => cause is function resolution
			if (previous instanceof FunctionCall || previous instanceof MethodCall) {
				return Cause.FUNCTION_RESOLUTION;
			}
			// assumption: table access index expression of previous access feature could not be computed => cause is index expression resolution
			// this includes member accesses of the form table.member, for example when the field was declared by table["mem" .. "ber"] = x.
			if (previous instanceof TableAccess || previous instanceof MemberAccess) {
				return Cause.TABLE_INDEX_EXP;
			}
			// Fallback: could not identify cause
			return Cause.UNIDENTIFIED;
		}
		
		LOGGER.error("Unexpectedly found non-named feature: " + unresolvableFeature);
		return Cause.UNEXPECTED;
	}
	
	
	private boolean originatesInImplicitImport(final FeaturePath featurePath) {
		final var root = featurePath.getRoot();
		if (root instanceof Var var) {
			return originatesInImplicitImport(var);
		}
		return false;
	}
	
	/**
	 * Check if the given Feature path originates in an implicit import (i.e. a Lua language library), by checking if any
	 * feature path root in the reference chain is part of an implicit resource.
	 */
	private boolean originatesInImplicitImport(final Var var) {
		
		if (LuaRequireUtil.isRequireFunctionCall(var)) {
			final var uriStringOpt = LuaRequireUtil.getImportUri(var);
			if (uriStringOpt.isPresent()) {
				final var uriString = uriStringOpt.get();
				return LuaGlobalScopeProvider.getImplicitLibraryUris()
						.stream()
						.map(uri -> uri.toFileString())
						.anyMatch(implicitUriString -> implicitUriString.contains(uriString));
			}
			
		}
		return false;
	}
	
	
	
	
	
	
	
	
	
	
//	
//	private Cause inferCauseAlongReferenceChain() {
//		final var referenceChain = ReferenceUtil.getReferenceChain(feature);
//	}
//	
//	
//	// TODO: need to consider all FeaturePaths of the reference chain, since they 
//	//  may contain features that are the real cause???
//	private Cause inferCause(final Feature contextFeature) {
//		
//		final var featurePath = new FeaturePath(contextFeature);
//		for (final var feature: featurePath.getContextFeatures()) {
//			// handle root feature being GroupedExp
//			if (feature instanceof GroupedExp) {
//				return Cause.GROUPED_EXP;
//			}
//			
//			if (feature instanceof NamedFeature namedFeature) {
//				final var referenced = ReferenceUtil.getReferencedElement(namedFeature);
//				if (referenced == null) {
//					System.out.println("Unexpected null returned as referencd element for Feature: " + feature);
//					assert(false);
//				}
//				if (MockUtil.isMocked(referenced)) {
//					return inferCauseByPreviousFeature(namedFeature);
//				}
//			}
//		}
//		
//		return Cause.UNIDENTIFIED;
//	}
//	
//	
//	
//	/**
//	 * Infers the cause for the given feature not being resolvable by considering the previous Feature in the Feature Path.
//	 * Assumes that the unresolvedFeature is the first unresolved feature in the corresponding FeaturePath.
//	 * @param unresolvedFeature the first unresolved Feature in the corresponding FeaturePath.
//	 * @return the cause according to the previous feature.
//	 */
//	private Cause inferCauseByPreviousFeature(final NamedFeature unresolvableFeature) {
//		// TODO: handle implicit imports as causes
//		if (unresolvableFeature instanceof Var var) {
//			if (referenceChainContainsReferenceToArg(var)) {
//				return Cause.ARG_ACCESS;
//			}
//			if (referenceChainContainsReferenceToImplicitImport(var)) {
//				return Cause.IMPLICIT_IMPORT;
//			}
//			// TODO: test for arg access
//			return Cause.VAR_NOT_FOUND;
//		}
//		
//		final var previous = FeatureUtil.getPreviousFeature(unresolvableFeature);
//		// previous feature should be available here
//		if (previous == null) {
//			LOGGER.error("Could not determine previous Feature for unresolvable feature: " + unresolvableFeature);
//			return Cause.UNIDENTIFIED;
//		}
//		// assumption: return values of previous function call feature could not be computed => cause is function resolution
//		if (previous instanceof FunctionCall || previous instanceof MethodCall) {
//			return Cause.FUNCTION_RESOLUTION;
//		}
//		// assumption: table access index expression of previous access feature could not be computed => cause is index expression resolution
//		// this includes member accesses of the form table.member, for example when the field was declared by table["mem" .. "ber"] = x.
//		if (previous instanceof TableAccess || previous instanceof MemberAccess) {
//			return Cause.TABLE_INDEX_EXP;
//		}
//		
//		
//		return Cause.UNIDENTIFIED;
//	}
//	
//	private boolean referenceChainContainsReferenceToArg(NamedFeature feature) {
//		final var referenceChain = ReferenceUtil.getReferenceChain(feature);
//		return referenceChain.stream().anyMatch(ref -> {
//			if (ref instanceof Arg) {
//				return true;
//			}
//			
//			// test that no references along the path
//			if (ref instanceof Feature f) {
//				final var featurePath = new FeaturePath(f);
//				final var root = featurePath.getPrefix();
//				if (root instanceof Var varRoot) {
//					final var referencedByFeature = ReferenceUtil.getReferencedElement(varRoot);
//				}
//			}
//			
//			
//			
//		});
//	}

}
