package org.xtext.lua.evaluation;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.xtext.lua.lua.Field;
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
import org.xtext.lua.scoping.LuaResourceDescriptionStrategy;
import org.xtext.lua.utils.ExpUtil;
import org.xtext.lua.utils.FeatureUtil;
import org.xtext.lua.utils.FieldUtil;
import org.xtext.lua.utils.LuaRequireUtil;
import org.xtext.lua.utils.MockUtil;
import org.xtext.lua.utils.ReferenceUtil;
import org.xtext.lua.wrappers.LuaFunctionCall;

/**
 * This class contains the logic for detecting the causes for synthetic references (i.e. "Cause Detection Algorithm" in the thesis).
 * It analyses all synthetic references in a given Lua Code Model and attempts to determine the cause for their synthetic resolution.
 * @author jsaenz
 *
 */
public class SyntheticReferenceInfoCollector {
	private static final Logger LOGGER = Logger.getLogger(SyntheticReferenceInfoCollector.class);
	private static final List<Cause> FILTER_CAUSES = List.of(
			Cause.ARG_ACCESS, 
			Cause.GROUPED_EXP,
			Cause.IMPLICIT_IMPORT,
			Cause.VAR_NOT_FOUND);
	
	private Map<EObject, SyntheticReferenceInfo> infos = new HashMap<>();
	private ResourceSet codeModel;
	
	
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
		codeModel = null;
	}
	
	/**
	 * Collect all infos for the given codeModel.
	 * @param codeModel
	 */
	private void collect(ResourceSet codeModel) {
		this.codeModel = codeModel;
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
				return Cause.GROUPED_EXP; // grouped expressions are never resolved -> special cause
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
				return getCauseForFirstUnresolvableFeatureInFeaturePath(contextFeature);
			}
		}
		
		LOGGER.error("Unexpectedly could not infer the cause for synthetic reference for: " + contextFeature);
		return Cause.UNEXPECTED;
	}
	
	// 1. pass through feature path to check if a previous feature directly references synthetic element
	// 2. for first feature that directly references a synthetic element, the PREVIOUS feature is the cause
	//     - if a var is synthetic (i.e. no previous feature):
	//              - probably external library, or some global value that could not be resolved
	// 3. pass through reference chain of PREVIOUS feature, for all features check if
	//     - direct reference to synthetic element -> cause is cause of that feature
	//     - end in function call -> cause is function call resolution
	//     - check for implicit imports along the way (which are function resolutions!)
	private Cause getCauseForFirstUnresolvableFeatureInFeaturePath(final NamedFeature unresolvableFeature) {
		// no previous feature
		if (unresolvableFeature instanceof Var) {
			// probably defined in external library or not found by global scope provider
			return Cause.VAR_NOT_FOUND;
		}
		
		if (ExpUtil.isTableAccessWithLinkingDummyName(unresolvableFeature)) {
			// Dynamic Access to table field could not be resolved
			return Cause.TABLE_INDEX_EXP;
		}
		
		
		// check for causes in the previous feature
		final var previous = FeatureUtil.getPreviousFeature(unresolvableFeature);
		// previous feature should be available here
		if (previous == null) { // should never happen, previous features should have been handled before
			LOGGER.error("Could not determine previous Feature for unresolvable feature: " + unresolvableFeature);
			return Cause.UNIDENTIFIED;
		}
		
		return tryInferCauseAlongReferenceChain(previous);
	}
	
	private boolean isInImplicitResource(EObject obj) {
		if (obj == null) {
			return false;
		}
		final var resource = obj.eResource();
		return LuaGlobalScopeProvider.isImplicitResource(resource);
	}
	

	// TODO: fix redundancies in this method
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
	private Cause tryInferCauseAlongReferenceChain(Feature previousFeature) {
		
		if (isDoubleFunctionCall(previousFeature)) {
			return Cause.DOUBLE_FUNCTION_CALL;
		}
		
		// handle previous is function call
		final var functionCall = LuaFunctionCall.of(previousFeature);
		if (functionCall != null) {
			final var causeOpt = tryGetCauseForFunctionCall(functionCall);
			if (causeOpt.isPresent()) {
				return causeOpt.get();
			}
		}
		
		// here, the name of the function called by the functionCall could still point
		// to another unresolved feature
		NamedFeature namedPrevious;
		if (functionCall != null) {
			namedPrevious = functionCall.getNamedFeature();
		} else if (previousFeature instanceof NamedFeature named) {
			namedPrevious = named;
		} else {
			LOGGER.error("Unexpected non-named previous feature");
			return Cause.UNEXPECTED;
		}
		
		// here, we know the next feature is resolved synthetically, but the namedPrevious
		// is not, so we need to test if the namedPrevious is a require call. If so, we expect
		// that the next feature could only not be resolved if the require call could not determine
		// a resource, i.e. the import is from an external library for which the code is not available.	
		final var referenceChain = ReferenceUtil.getReferenceChain(namedPrevious);

		
		for (final var referencing : referenceChain) {
			if (referencing instanceof Arg) {
				return Cause.ARG_ACCESS; // arg accesses are not resolved by the Lua CMoGS
			}
			
			if (isInImplicitResource(referencing)) {
				// assume that the next feature could not be resolved because of 
				// missing information in the library code (i.e. C code not represented in Lua
				// standard library files used from sumneko language server)
				return Cause.IMPLICIT_IMPORT; 
			}
			
			if (referencing instanceof Field field && FieldUtil.isFieldWithLinkingDummyName(field)) {
				return Cause.TABLE_INDEX_EXP;
			}
			
			
			// if an element on the reference chain is itself unresolvable, we return it's cause
			if (referencing instanceof NamedFeature feature && MockUtil.referencesMocked(feature)) {
				return getInfoFor(feature).getCause();
			}
		}
		
		// after traversing the complete reference chain, we check the final referenced element
		final var referenced = ReferenceUtil.getReferencedElement(namedPrevious);
		if (referenced instanceof Arg) {
			return Cause.ARG_ACCESS;
		}
		
		if (isInImplicitResource(referenced)) {
			return Cause.IMPLICIT_IMPORT;
		}
		
		if (referenced instanceof Field field && FieldUtil.isFieldWithLinkingDummyName(field)) {
			return Cause.TABLE_INDEX_EXP;
		}
		
		final var referencedFunctionCall = LuaFunctionCall.of(referenced);
		if (referencedFunctionCall != null) {
			final var causeOpt = tryGetCauseForFunctionCall(referencedFunctionCall);
			if (causeOpt.isPresent()) {
				return causeOpt.get();
			}
		}
		
		if (referenced instanceof NamedFeature feature) {			
			if (MockUtil.referencesMocked(feature)) {
				return getInfoFor(feature).getCause();
			}
		}
		
		// Fallback: could not identify cause, e.g. unresolved table access on previous
		//  feature in Assignment ("b" in a.b is not resolved for a[func()] = 1 with func() returning "b")
		//  other e.g.: a,b = func(); b points at synthetic nil expression but func() may return multiple values
		return Cause.UNIDENTIFIED;
	}
	
	private Optional<Cause> tryGetCauseForFunctionCall(LuaFunctionCall functionCall) {
		Cause maybeCause = null;
		if (!functionCall.isMocked()) {
			var calledFunctionName = functionCall.getNamedFeature();
			// fist check if require call (i.e. import)
			if (isRequireCall(calledFunctionName)) {
				// if any match -> import resolution failed (i.e. function resolution)
				// if no match -> external library not available in the resourceSet (i.e. CM)
				if (isExternalImport(calledFunctionName)) { // uri not found in resources
					maybeCause = Cause.EXTERNAL_IMPORT;
				}
				// uri found in resources -> could not resolve an access to the value returned 
				// by the require call, which we regard as a function call resolution failure
				maybeCause = Cause.FUNCTION_RESOLUTION; 
			}
			
			if (calledFunctionName instanceof Arg) {
				maybeCause = Cause.ARG_ACCESS; // arg accesses are not resolved by the Lua CMoGS
			}
			
			// since we already checked for require calls, here only other standard library
			// functions are checked
			if (isInImplicitResource(calledFunctionName)) {
				// assume that the next feature could not be resolved because of 
				// missing information in the library code (i.e. C code not represented in Lua
				// standard library files used from sumneko language server)
				maybeCause = Cause.IMPLICIT_IMPORT; 
			}
			
			// if function call was not mocked and other causes do not apply, the
			// next feature could not be resolved because the function call's return value
			// could not be resolved
			maybeCause = Cause.FUNCTION_RESOLUTION;
		}
		
		if (maybeCause != null) {
			return Optional.of(maybeCause);
		}
		return Optional.empty();
	}
	
	/**
	 * Checks if a {@link FunctionCall} feature is preceded by another function call (FunctionCall or MethodCall)
	 * in its feature path.
	 * @param feature the {link FunctionCall} feature.
	 */
	private boolean isDoubleFunctionCall(Feature feature) {
		if (feature instanceof FunctionCall) {
			final var previousPrevious = FeatureUtil.getPreviousFeature(feature);
			final var isDoubleFunctionCall = previousPrevious != null 
					&& (previousPrevious instanceof FunctionCall || previousPrevious instanceof MethodCall);
			return isDoubleFunctionCall;
		}
		return false;
	}
	
	private boolean isExternalImport(NamedFeature feature) {
		if (isRequireCall(feature)) {
			final var importUriOpt = LuaRequireUtil.getImportUri(feature);
			if (importUriOpt.isPresent()) {
				var importUriPart = importUriOpt.get();
				if (!importUriPart.endsWith(".lua")) {
					importUriPart += ".lua";
	            }
				final var importUri = importUriPart;
				var isInternal = codeModel.getResources().stream()
						.filter(resource -> !LuaGlobalScopeProvider.isImplicitResource(resource))
						.anyMatch(resource -> {
							final var resourceUriStr = resource.getURI().toFileString();
							return LuaResourceDescriptionStrategy.importUriEqualsFileUri(importUri, resourceUriStr);
						});
				return !isInternal;
			}
		}
		return false;
	}
	
	
	private boolean isRequireCall(NamedFeature feature) {
		return feature instanceof Var var && LuaRequireUtil.isRequireFunctionCall(var);
	}

}
