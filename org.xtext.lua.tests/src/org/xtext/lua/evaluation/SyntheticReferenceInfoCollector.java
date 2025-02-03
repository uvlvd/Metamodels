package org.xtext.lua.evaluation;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
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
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.lua.Var;
import org.xtext.lua.mocking.FeaturePath;
import org.xtext.lua.scoping.LuaGlobalScopeProvider;
import org.xtext.lua.scoping.LuaResourceDescriptionStrategy;
import org.xtext.lua.utils.AssignmentUtil;
import org.xtext.lua.utils.ExpUtil;
import org.xtext.lua.utils.FeatureUtil;
import org.xtext.lua.utils.LuaRequireUtil;
import org.xtext.lua.utils.MockUtil;
import org.xtext.lua.utils.ReferenceUtil;

public class SyntheticReferenceInfoCollector {
	private static final Logger LOGGER = Logger.getLogger(SyntheticReferenceInfoCollector.class);
	private static final List<Cause> FILTER_CAUSES = List.of(
			Cause.ARG_ACCESS, 
			Cause.GROUPED_EXP,
			Cause.IMPLICIT_IMPORT,
			Cause.VAR_NOT_FOUND);
	
	private Map<EObject, SyntheticReferenceInfo> infos = new HashMap<>();
	
	
	public Map<Type, SyntheticReferenceEvalData> getSyntheticReferenceTypeEvalData(final ResourceSet codeModel, final int totalReferences) {
		clear();
		collect(codeModel);
		
		var result = new EnumMap<Type, SyntheticReferenceEvalData>(Type.class);
		Stream.of(Type.values()).forEach(type -> result.put(type, new SyntheticReferenceEvalData()));
		
		final var typeToTotalReferences = getTypeToTotalReferences(codeModel);
		
		final var typeToCauseToCount = getSyntheticReferenceTypeCountByCause();
		final var totalSyntheticReferences = typeToCauseToCount.values()
				.stream()
				.flatMap(causes -> causes.values().stream())
				.mapToInt(Integer::intValue)
				.sum();
		
		for (final var entry : typeToCauseToCount.entrySet()) {
			
			final var type = entry.getKey();
			final var causes = entry.getValue();
			final var totalReferencesOfType = typeToTotalReferences.get(type);
			
			final var totalSyntheticForType = causes.values().stream().mapToInt(Integer::intValue).sum();
			final var percentageOfSyntheticReferences = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(totalSyntheticForType, totalSyntheticReferences));
			final var percentageOfAllReferences = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(totalSyntheticForType, totalReferences));
			final var percentageOfReferencesOfType = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(totalSyntheticForType, totalReferencesOfType));
			
			var syntheticReferenceEvalData = result.get(type);
			syntheticReferenceEvalData.setType(type);
			syntheticReferenceEvalData.setNumberTotalSyntheticOfType(totalSyntheticForType);
			syntheticReferenceEvalData.setPercentOfTotalSyntheticReferences(percentageOfSyntheticReferences);
			syntheticReferenceEvalData.setPercentOfAllReferences(percentageOfAllReferences);
			syntheticReferenceEvalData.setNumberReferencesOfType(totalReferencesOfType);
			syntheticReferenceEvalData.setPercentOfReferencesOfType(percentageOfReferencesOfType);
			
			final var totalForTypeFitlered = getFilteredNumberOfSyntheticReferences(causes);
			final var numberFilteredSyntheticOfType = totalForTypeFitlered;
			final var percentFilteredOfReferencesOfType = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(totalForTypeFitlered, totalReferencesOfType));
			final var percentFilteredOfSyntheticReferencesOfType = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(totalForTypeFitlered, totalSyntheticForType));
			final var percentFilteredOfTotalSyntheticReferences = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(totalForTypeFitlered, totalSyntheticReferences));
			final var percentFilteredOfAllReferences = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(totalForTypeFitlered, totalReferences));
			
			syntheticReferenceEvalData.setNumberFilteredSyntheticOfType(numberFilteredSyntheticOfType);
			syntheticReferenceEvalData.setPercentFilteredOfReferencesOfType(percentFilteredOfReferencesOfType);
			syntheticReferenceEvalData.setPercentFilteredOfSyntheticReferencesOfType(percentFilteredOfSyntheticReferencesOfType);
			syntheticReferenceEvalData.setPercentFilteredOfTotalSyntheticReferences(percentFilteredOfTotalSyntheticReferences);
			syntheticReferenceEvalData.setPercentFilteredOfAllReferences(percentFilteredOfAllReferences);
			
			var causesEvalData = new EnumMap<Cause, SyntheticReferenceCauseEvalData>(Cause.class);
			causes.entrySet().forEach(causeEntry -> {
				final var cause = causeEntry.getKey();
				final var count = causeEntry.getValue();
				final var percentageForType = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(count, totalReferencesOfType));
				final var percentageSyntheticForType = NumberUtil.roundPercentage(100d - NumberUtil.computePercentage(count, totalSyntheticForType));
				
				var causeEvalData = syntheticReferenceEvalData.new SyntheticReferenceCauseEvalData();
				causeEvalData.setNumberTotal(count);
				causeEvalData.setPercentOfType(percentageForType);
				causeEvalData.setPercentSyntheticOfType(percentageSyntheticForType);
				
				causesEvalData.put(cause, causeEvalData);
			});
			
			syntheticReferenceEvalData.setCauses(causesEvalData);
		}
		
		return result;
	}
	
	private Map<Type, Integer> getTypeToTotalReferences(ResourceSet codeModel) {
		var result = new EnumMap<Type, Integer>(Type.class);
		Stream.of(Type.values()).forEach(type -> result.put(type, 0));
		
		for (final var res : codeModel.getResources()) {
			var root = res.getContents().get(0);
			EcoreUtil2.getAllContentsOfType(root, Referencing.class)
					.stream()
					.forEach(referencing -> {
						if (referencing instanceof NamedFeature feature) {
							final var type = inferType(feature);
							final var currentCount = result.get(type);
							result.put(type, currentCount + 1);	
						}	
					});
		}
		
		return result;
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
	
	/**
	 * Computes the total number of filtered synthetic references, filtering by cause.
	 * The filtered Causes are defined in {@link FILTER_CAUSE}.
	 * @param causeToCount map containing the cause-to-count information for a specific {@link Type}.
	 * @return the filtered count.
	 */
	private int getFilteredNumberOfSyntheticReferences(final Map<Cause, Integer> causeToCount) {
		var result = 0;
		for (final var entry : causeToCount.entrySet()) {
			final var cause = entry.getKey();
			final var count = entry.getValue();
			if (!FILTER_CAUSES.contains(cause)) {
				result += count;
			}
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
	 * Collect all infos for the given codeModel.
	 * @param codeModel
	 */
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
	

	private Type inferType(final NamedFeature feature) {
		// first, handle all function calls (i.e. MethodCall or NamedFeature followed by FunctionCall
		final var nextFeature = FeatureUtil.getNextFeature(feature);
		final var isFunctionCall = nextFeature instanceof FunctionCall;
		if (isFunctionCall || feature instanceof MethodCall) {
			return Type.FUNCTION_CALL;
		}
		// if not function call, we assume that the feature is an access to a table field or a varialbe
		if (feature instanceof TableAccess || feature instanceof MemberAccess) {
			return Type.TABLE_ACCESS;
		}
		if (feature instanceof Var || feature instanceof GroupedExp) {
			return Type.ROOT_FEATURE;
		}
		return Type.UNKNOWN;
	}
	
	// assumes the contextFeature references a synthetic element (via contextFeature.getRef())
	private Cause inferCause(final NamedFeature contextFeature) {
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
			}
			
			// If no referencing features (NamedFeatures) along the path previous to the contextFeature
			// referenced a synthetic element, the contextFeature itself must contain the cause.
			// The condition feature == contextFeature is needed to skip non-named features like FunctionCall
			if (feature == contextFeature) {
				return getCauseForFeature(contextFeature);
			}
		}
		
		LOGGER.error("Unexpectedly could not infer the cause for synthetic reference for: " + contextFeature);
		return Cause.UNEXPECTED;
	}
	
	// 1. pass through feature path to check if a previous feature directly references synthetic element
	// 2. for first feature that directly references a synthetic element, the PREVIOUS feature is the cause
	//     - if a var is synthetic (i.e. no previous feature):
	//              - probably external library, or some global value that could not be resolved
	//              - ???
	// 3. pass through reference chain of PREVIOUS feature, for all features check if
	//     - direct reference to synthetic element -> cause is cause of that feature
	//     - end in function call -> cause is function call resolution
	//     - check for implicit imports along the way (which are function resolutions!)
	private Cause getCauseForFeature(final NamedFeature unresolvableFeature) {
		// no previous feature
		if (unresolvableFeature instanceof Var) {
			// probably defined in external library or not found by global scope provider
			return Cause.VAR_NOT_FOUND;
		}
		
		if (ExpUtil.isTableAccessWithLinkingDummyName(unresolvableFeature)) {
			// probably defined in external library or not found by global scope provider
			return Cause.TABLE_INDEX_EXP;
		}
		
		
		
		// check for causes in the previous feature
		final var previous = FeatureUtil.getPreviousFeature(unresolvableFeature);
		// previous feature should be available here
		if (previous == null) {
			LOGGER.error("Could not determine previous Feature for unresolvable feature: " + unresolvableFeature);
			return Cause.UNIDENTIFIED;
		}
		
		if (previous instanceof FunctionCall functionCall) {
			// TODO: check if require call, else Function Resolution
			return getCauseForFunctionCallFeature(functionCall);
		}
		
		// since we know the previous methodCall itself does not reference a synthetic element
		// if this function was called, we assume it could not be resolved
		if (previous instanceof MethodCall methodCall) {
			// TODO: check if require call, else Function Resolution
			return getCauseForMethodCallFeature(methodCall);
		}
		
		// if previous is namedFeature (MemberAccess, TableAccess, or MethodCall
		if (previous instanceof NamedFeature namedPrevious) {
			// if direct reference to Arg -> Cause is Arg access
//			if (namedPrevious.getRef() instanceof Arg) {
//				return Cause.ARG_ACCESS;
//			}
			// TODO: this is probably never called because this method is only called with the first
			// feature in a path that has MockUtil.referencesMocked == true (i.e. the previous feature
			// cannot return true for this referencesMocked
			// if the previous feature directly references a synthetic element, it defines the cause
			if (MockUtil.referencesMocked(namedPrevious)) {
				//return getCauseByFeatureType(namedPrevious);
			}
			// else we search the cause along the previous feature's reference chain
			return tryInferCauseAlongReferenceChain(namedPrevious, unresolvableFeature);
		}
		

		
		LOGGER.error("Unexpectedly found non-named feature: " + unresolvableFeature);
		return Cause.UNEXPECTED;
	}
	
	// TODO: should getInfo for first named feature prefix to the functionCall
	private Cause getCauseForFunctionCallFeature(FunctionCall functionCall) {
		final var calledFunctionName = FeatureUtil.getFirstNamedPrefix(functionCall);
		
		if (calledFunctionName != null) {
			final var referenced = ReferenceUtil.getReferencedElement(calledFunctionName);
			if (referenced instanceof Arg) {
				return Cause.ARG_ACCESS;
			}
			if (isInImplicitResource(referenced)) {
				return Cause.IMPLICIT_IMPORT;
			}
		}
		
		var featurePath = new FeaturePath(functionCall);
		if (importFailed(featurePath)) {
			return Cause.OTHER_IMPORT;
		}
		
		return Cause.FUNCTION_RESOLUTION;
	}
	
	private Cause getCauseForMethodCallFeature(MethodCall methodCall) {		

		final var referenced = ReferenceUtil.getReferencedElement(methodCall);
		if (referenced instanceof Arg) {
			return Cause.ARG_ACCESS;
		}
		if (isInImplicitResource(referenced)) {
			return Cause.IMPLICIT_IMPORT;
		}
		
		
		var featurePath = new FeaturePath(methodCall);
		if (importFailed(featurePath)) {
			return Cause.OTHER_IMPORT;
		}
		
		return Cause.FUNCTION_RESOLUTION;
	}
	
	private Cause getCauseByFeatureType(NamedFeature feature) {
		if (isInImplicitResource(feature)) {
			return Cause.IMPLICIT_IMPORT;
		}
		if (feature instanceof TableAccess || feature instanceof MemberAccess) {
			return Cause.TABLE_INDEX_EXP;
		}
		if (feature instanceof MethodCall) {
			return Cause.FUNCTION_RESOLUTION;
		}
		LOGGER.error("Could not determine cause by feature type: " + feature);
		return Cause.UNEXPECTED;
	}
	
	private boolean isInImplicitResource(EObject obj) {
		if (obj == null) {
			return false;
		}
		final var resource = obj.eResource();
		return LuaGlobalScopeProvider.isImplicitResource(resource);
	}
	

	/**
	 * Tries to infer the cause along the reference chain of the given feature. This
	 * assumes that the given feature is the previous feature to an unresolvable feature.
	 * 
	 * <p>
	 * For example, if "insert" in "table.insert" cannot be resolved, then this function
	 * is called with "table" and searches for a cause along the reference chain from 
	 * the feature "table".
	 * </p>
	 * @param previousFeature the previous feature to an unresolvable feature.
	 * @return
	 */
	private Cause tryInferCauseAlongReferenceChain(NamedFeature previousFeature, NamedFeature unresolvable) {
		final var referenceChain = ReferenceUtil.getReferenceChain(previousFeature);
		
		//...
		// - TODO: need to think about how this should be resolved,
		//    -> 
		
		// new: for referencing in reference chain
		// - check if feature 
		// 		-> check if any feature is mocked -> return cause
		// - for last element in reference chain: -> check if followed by FunctionCall -> return Cause.Function_Resolution
		
		Referencing previous = previousFeature;
		for (final var referencing : referenceChain) {
			// if any along the reference chain directly reference a synthetic element, compute and return
			// the corresponding cause
			if (referencing instanceof NamedFeature named && MockUtil.referencesMocked(named)) {
				return getInfoFor(named).getCause();
			}
			
			//if (AssignmentUtil.isAssignable(referencing))
		
			
			
			if (referencing instanceof Feature referencedFeature) {
				final var featurePath = new FeaturePath(referencedFeature);
				
				for (var fpFeature : featurePath.getContextFeatures()) {
					if (fpFeature instanceof NamedFeature named && MockUtil.referencesMocked(named)) {
						return getInfoFor(named).getCause();
					}
				}
			}
			previous = referencing;
		}
		
		final var referenced = ReferenceUtil.getReferencedElement(previousFeature);
		if (referenced instanceof Arg) {
			return Cause.ARG_ACCESS;
		}
		
		if (isInImplicitResource(referenced)) {
			return Cause.IMPLICIT_IMPORT;
		}
		
		if (ExpUtil.isTableAccessWithLinkingDummyName(referenced)) {
			return Cause.TABLE_INDEX_EXP;
		}
		
		// check if last named feature (last element of reference chain)
		// is followed by a function call. If so, we assume that it could not be resolved.
		if (referenced instanceof NamedFeature named) {
			final var next = FeatureUtil.getNextFeature(named);
			if (next instanceof FunctionCall functionCall) {
				return getCauseForFunctionCallFeature(functionCall);
			}
		}
		
		// Fallback: could not identify cause, e.g. unresolved table access on previous
		//  feature in Assignment ("b" in a.b is not resolved for a[func()] = 1 with func() returning "b")
		return Cause.UNIDENTIFIED;
	}
	
	
	
	private boolean importFailed(final FeaturePath featurePath) {
		final var root = featurePath.getPrefix();
		return root instanceof Var var && LuaRequireUtil.isRequireFunctionCall(var);
	}

	
//	
//	
//	
//	private Cause getCauseForFeature2(final Feature unresolvableFeature) {
//		if (unresolvableFeature instanceof NamedFeature feature) {
//			final var referenced = feature.getRef();
//			final var featurePath = new FeaturePath(feature);
//			
//			// handle implicit imports: check if feature path starts with "require" etc.
//			if (originatesInImplicitImport(featurePath)) {
//				return Cause.IMPLICIT_IMPORT;
//			}
//			
//			if (importFailed(featurePath)) {
//				return Cause.OTHER_IMPORT;
//			}
//			
//			if (referenced instanceof Arg) {
//				return Cause.ARG_ACCESS;
//			} else if (feature instanceof Var) {
//				return Cause.VAR_NOT_FOUND;
//			}
//			
//			// check for causes in the previous feature
//			final var previous = FeatureUtil.getPreviousFeature(unresolvableFeature);
//			// previous feature should be available here
//			if (previous == null) {
//				LOGGER.error("Could not determine previous Feature for unresolvable feature: " + unresolvableFeature);
//				return Cause.UNIDENTIFIED;
//			}
//			// assumption: return values of previous function call feature could not be computed => cause is function resolution
//			if (previous instanceof FunctionCall || previous instanceof MethodCall) {
//				return Cause.FUNCTION_RESOLUTION;
//			}
//			// assumption: table access index expression of previous access feature could not be computed => cause is index expression resolution
//			// this includes member accesses of the form table.member, for example when the field was declared by table["mem" .. "ber"] = x.
//			if (previous instanceof TableAccess || previous instanceof MemberAccess) {
//				return Cause.TABLE_INDEX_EXP;
//			}
//			
//			
//			
//			if (previous instanceof NamedFeature previousRef) {
//				if (previousRef.getRef() instanceof Arg) {
//					return Cause.ARG_ACCESS;
//				}
//				return inferCauseAlongReferenceChain(previousRef);
//			}
//			
//			else if (previous instanceof Var) {
//				return Cause.VAR_NOT_FOUND;
//			}
//			
//			// Fallback: could not identify cause
//			return Cause.UNIDENTIFIED;
//		}
//		
//		LOGGER.error("Unexpectedly found non-named feature: " + unresolvableFeature);
//		return Cause.UNEXPECTED;
//	}
	
	
//	private boolean originatesInImplicitImport(final FeaturePath featurePath) {
//		final var root = featurePath.getRoot();
//		if (root instanceof Var var) {
//			return originatesInImplicitImport(var);
//		}
//		return false;
//	}

//	
//	/**
//	 * Check if the given Feature path originates in an implicit import (i.e. a Lua language library), by checking if any
//	 * feature path root in the reference chain is part of an implicit resource.
//	 */
//	private boolean originatesInImplicitImport(final Var var) {
//		
//		if (LuaRequireUtil.isRequireFunctionCall(var)) {
//			final var importUriStrOpt = LuaRequireUtil.getImportUri(var);
//			if (importUriStrOpt.isPresent()) {
//				var importUri = importUriStrOpt.get();
//				if (!importUri.endsWith(".lua")) {
//					importUri += ".lua";
//				}
//				
//				final var matchUri = importUri;
//				final var implicitUris = LuaGlobalScopeProvider.getImplicitLibraryUris()
//						.stream()
//						.map(uri -> uri.toFileString())
//						.toList();
//				return implicitUris.stream()
//						.anyMatch(implicitUri -> LuaResourceDescriptionStrategy.importUriEqualsFileUri(matchUri, implicitUri));
//				
////				return LuaGlobalScopeProvider.getImplicitLibraryUris()
////						.stream()
////						.map(uri -> uri.toFileString())
////						.anyMatch(implicitUriString -> implicitUriString.contains(uriString));
//			}
//			
//		}
//		return false;
//	}
	
	
	
	
	
	
	
	
	
	
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
