package org.xtext.lua.scoping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.scoping.IGlobalScopeProvider;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.ImportUriResolver;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.xtext.lua.lua.Arg;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.ExpLiteral;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.Field;
import org.xtext.lua.lua.FuncBody;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.LocalVar;
import org.xtext.lua.lua.MemberAccess;
import org.xtext.lua.lua.MethodCall;
import org.xtext.lua.lua.NamedFeature;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.Var;
import org.xtext.lua.utils.FeatureUtil;
import org.xtext.lua.utils.FunctionUtil;
import org.xtext.lua.utils.LuaRequireUtil;
import org.xtext.lua.utils.ReferenceableUtil;
import org.xtext.lua.utils.ReturnUtil;
import org.xtext.lua.utils.StatUtil;

import com.google.inject.Inject;

public class LuaFeatureScopeProvider extends LuaAbstractBlockScopeProvider {
	private static final Logger LOGGER = Logger.getLogger(LuaFeatureScopeProvider.class);
	
	@Inject
	private IGlobalScopeProvider globalScopeProvider;
	
	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;
	
  	@Inject
    private IQualifiedNameConverter nameConverter;
  	
  	@Inject
	private DescriptionCreator descriptionCreator;
	
	@Inject
	private FeaturePathCandidateBuilder featurePathCandidateBuilder;
	
	@Inject
	private ImportUriResolver uriResolver;
	
	
	@Override
	protected IScope getScopeFromBlock(final EObject context, final EReference reference, final Block currentBlock, final Block previousBlock) {
		if (context instanceof Feature feature) {
			// we use the parentStatement to decide where to stop searching for candidates (i.e. only consider statements before the context's statement)
			final var contextParentStatementOpt = StatUtil.getParentStatement(feature);
	    	if (!contextParentStatementOpt.isPresent()) {
	    		LOGGER.warn("Found no contextParentStatement for obj " + feature);
	    		return null;
	    	}
	    	
	    	final var contextParentStatement = contextParentStatementOpt.get();
	    	final var referenceables = ReferenceableUtil.getReferenceablesForContextFromBlock(feature, currentBlock, contextParentStatement);

	    	return getScopeForFeatureFromReferenceables(feature, reference, referenceables);
		}
		return null;
	}

	protected IScope getScopeForFeatureFromReferenceables(final Feature feature, final EReference reference, final Collection<? extends Referenceable> referenceables) {
		final var candidates = getCandidatesForFeature(feature, reference, referenceables);
		if (candidates.isEmpty()) {
			return null;
		}
		return new SimpleScope(descriptionCreator.createFor(candidates));
	}

    /**
     * Searches for candidates for the given Feature by traversing its feature path, starting from the root Feature and matching
     * each feature path segment to the respective segment of the fully qualified name of the candidate. Builds new candidates from 
     * candidates that themselves contain references as needed.
     * @param context the feature.
     * @param referenceables the referenceables to be searched.
     * @return the candidates.
     */
    private List<? extends Referenceable> getCandidatesForFeature(
    		final Feature context, 
    		final EReference reference, 
    		final Collection<? extends Referenceable> referenceables) {
    	
    	final var featurePathCandidates = findFeaturePathCandidatesThatMatchUntil(context, reference, referenceables);
    	return featurePathCandidates.stream()
				 .filter(FeaturePathCandidate::isCompletelyMatched)
				 .map(FeaturePathCandidate::getReferenceable)
				 .toList();
    }

    /**
     * Returns all FeaturePathCandidates that match the given feature's featurePath until the feature itself, i.e.
     * the returned candidates contain candidates with a path that continues after the match of the given feature.</br>
     * The returned candidates can be filtered via {@link FeaturePathCandidate#isCompletelyMatched} to get the matching candidates for the given feature
     * (see {@link #getCandidatesForFeature}).
     * @param feature.
     * @param referenceables.
     * @return the FeaturePathCandidates.
     */
    private List<FeaturePathCandidate> findFeaturePathCandidatesThatMatchUntil(
    		final Feature context, 
    		final EReference reference, 
    		final Collection<? extends Referenceable> referenceables) {
    	// TODO: we use features here to be able to distinguish between function/methodCalls and other feature types
    	// i.e. can't use a FeaturePathCandidate for the current feature, but might want to implement a FeaturePath class.
    	final var featurePathPrefixOpt = FeatureUtil.findFeaturePathPrefixAsVar(context);
    	
    	if (featurePathPrefixOpt.isPresent()) {
    		final var featurePathRoot = featurePathPrefixOpt.get();
        	final var featurePathCandidates = featurePathCandidateBuilder.buildFeaturePathCandidates(referenceables);
        	return filterAndExtendFeaturePathCandidatesFromFeatureToFeature(context, reference, featurePathRoot, context, featurePathCandidates);
    	} else {
    		LOGGER.warn("Cannot find feature path candidates for " + context + " because feature path does not start with a Var.");
    		return Collections.emptyList();
    	}
    }

	private List<FeaturePathCandidate> filterAndExtendFeaturePathCandidatesFromFeatureToFeature(
			final EObject context,
			final EReference reference, 
			final Feature startFeature, 
			final Feature endFeature,
			final Collection<FeaturePathCandidate> initialFeaturePathCandidates) {
		var candidates = filterAndExtendFeaturePathCandidatesByFeature(context, reference, startFeature,
				initialFeaturePathCandidates);
		// iterate over all path elements until the feature we started from
		var previousFeature = startFeature;
		while (previousFeature != endFeature && FeatureUtil.hasNextFeature(previousFeature)) {
			final var currentFeature = FeatureUtil.getNextFeature(previousFeature);
			candidates = filterAndExtendFeaturePathCandidatesByFeature(context, reference, currentFeature, candidates);
			previousFeature = currentFeature;

		}
		return candidates;
	}
    
	/**
     * Filters and extends the given FeaturePathCandidates by the given Feature ({@code current}). The given FeaturePathCandidates
     * are filtered according to {@link FeaturePathCandidate#checkAndIncrementIndex} applied to the current Feature's name and extended
     * if a candidate references a Referencing object. Used by {@link filterAndExtendFeaturePathCandidatesFromFeatureToFeature} to
     * find candidates from feature paths that match another feature path.
     * 
     * @param current the current feature in the path.
     * @param previous the next feature in the path.
     * @param candidates the candidates from a previous call of this method. Candidates correspond to allReferenceables on initial call.
     * @param allReferenceables all referenceables that should be considered for the search.
     * @return
     */
    private List<FeaturePathCandidate> filterAndExtendFeaturePathCandidatesByFeature(
    		final EObject context,
    		final EReference reference,
    		final Feature current, 
    		final Collection<FeaturePathCandidate> candidates) {
    
    	var result = new ArrayList<FeaturePathCandidate>();

    	// handle current is function/methodCall feature
    	if (current instanceof FunctionCall) {
    		// We have two possibilities here: 1. previous is named feature or 2. previous itself was functionCallFeature:
    		// In both cases the remaining candidates should already contain only viable candidates from a previous call of this function.
    		for (var candidate : candidates) {
    			result.addAll(getCandidatesFromCall(candidate));
    		}
    		return result;
    	}
    	
    	//TODO: probably need to handle methodCall differntly to function call, since method call contains a name
    	if (current instanceof MethodCall methodCall) {
    		// In contrast to the FunctionCall case, a MethodCall has a name which we need to check before computing the candidates from the 
    		// referenced function's body
    		final var currentFqn = qualifiedNameProvider.getFullyQualifiedName(methodCall);
        	final var currentName = currentFqn.getLastSegment();
    		for (var candidate : candidates) {
    			if (candidate.checkAndIncrementIndex(currentName)) {
    				result.add(candidate); // add the candidate itself (since it had a matching name)
    				result.addAll(getCandidatesFromCall(candidate)); // add the candidates resulting from the call
    			}
    		}
    		return result;
    	}
    	
    	// handle current is non-call named feature (i.e. Var, MemberAccess or TableAccess)
    	assert(current instanceof NamedFeature); // TODO: throw exception instead
    	final var currentFqn = qualifiedNameProvider.getFullyQualifiedName(current);
    	final var currentName = currentFqn.getLastSegment();
    	// check for all candidates if they match, build new candidates from function calls and candidates that reference other Referenceables
    	for (var candidate : candidates) {
    		// TODO: this appears twice here (and I think it may need to?), should overhaul this whole providr
    		// to be closer to the algorithm described in the thesis
    		if (candidate.referencesReferencing()) {	
				// this is the rhs expression the candidate points at if it is an assignable:
				// -> points at the start of a featurePath if it is a feature
				var assignedReferencing = candidate.getReferencedReferencing(); // TODO: is this always a feature if this is referencing?
				result.addAll(buildFeaturePathCandidatesFromAssignedReferencing(context, reference, assignedReferencing));		
			}
    		if (candidate.checkAndIncrementIndex(currentName)) {
    			result.add(candidate); // candidate matches, keep as part of result
    				
    			// find new candidates from references and add them to the result
    			if (candidate.referencesReferencing()) {	
    				// this is the rhs expression the candidate points at if it is an assignable:
    				// -> points at the start of a featurePath if it is a feature
    				var assignedReferencing = candidate.getReferencedReferencing(); // TODO: is this always a feature if this is referencing?
    				result.addAll(buildFeaturePathCandidatesFromAssignedReferencing(context, reference, assignedReferencing));		
    			}
    		}
    	}

    	return result;
    }
    
    /**
     * Returns the candidates resulting from a function/method call of a candidate.
     * @param candidate
     * @return
     */
    private List<FeaturePathCandidate> getCandidatesFromCall(final FeaturePathCandidate candidate) {
    	// candidate can only be functionCall/methodCall-Candidate if it is completely matched, if the candidate's feature path
    	// continues, it is not a matching candidate function/methodCall for the currently considered context object
    	if (candidate.isCompletelyMatched()) { 
			final var candidateContext = candidate.getReferenceable();
			final var candidateFuncBodyOpt = FunctionUtil.findFuncBodyFromFuncObject(candidateContext);
			if (candidateFuncBodyOpt.isPresent()) {
				return getFeaturePathCandidatesFromFuncBody(candidateFuncBodyOpt.get());
			}
		}
    	return Collections.emptyList();
    }

    // TODO: this method is too complex, should be split into multiple methods for each case.
    // assignedReferencing is a Referencing Feature on the rhs (first element of a feature path) that was assigned to an assignable
    /**
     * Called by {@link #findCandidatesForFeature} when a Referencing object (the {@code assignedReferencing}) is assigned to a candidate
     * to build and return the FeaturePathCandidates resulting from the reference of the assigned object.
     * @param context the current context object.
     * @param reference the current reference.
     * @param assignedReferencing the object that is assigned to the candidate (i.e. on rhs of Assignment of candidate).
     * @return the FeaturePathCandidates reachable from the assignedReferencing.
     */
    private List<FeaturePathCandidate> buildFeaturePathCandidatesFromAssignedReferencing(final EObject context, final EReference reference, final Referencing assignedReferencing) {
    	if (assignedReferencing instanceof Feature feature) {
    		var leafOpt = FeatureUtil.findFeaturePathNamedLeaf(feature);
    		if (leafOpt.isEmpty()) {
    			return Collections.emptyList();
    		}
    		
			var referencedByLeaf = ((Referencing) leafOpt.get()).getRef();
    		
    		// handle require calls here, since they/what they reference are/is defined by the first part of the featurePath
			if (assignedReferencing instanceof Var var && LuaRequireUtil.isRequireFunctionCall(var)) {
				var requireFuncCall = var;
				return getAllFeaturePathCandidatesFromRequireCallFeaturePath(context, reference, 0, (Var) requireFuncCall);
			}
			
			// get candidates from function calls
			final var candidateFuncBodyOpt = FunctionUtil.findFuncBodyFromFuncObject(referencedByLeaf);
			if (candidateFuncBodyOpt.isPresent()) {
				return getFeaturePathCandidatesFromFuncBody(candidateFuncBodyOpt.get());
			}
			
			// get candidates from referenced feature paths
			else if (referencedByLeaf instanceof Feature referencedFeature) {
				// handle other references to other features here:
				// 1. get Referenceables for the referenced feature from all Referenceables of its 
				//    block until the statement containing the assigned feature
				// 2. filter/extend candidates by calling findFeaturePathCandidatesThatMatchUntil with 
				//    the referenced feature and the Referenceables from 1.
				var referencedBlock = EcoreUtil2.getContainerOfType(referencedFeature, Block.class);
				// parenStatement should be present here, a feature is always part of a Stat
				var parentStatement = StatUtil.getParentStatement(assignedReferencing);
				var referenceablesInReferencedBlock = ReferenceableUtil.getReferenceablesForContextFromBlock(referencedFeature, referencedBlock, parentStatement.get());
				var extendedFeaturePathCandidates = findFeaturePathCandidatesThatMatchUntil(referencedFeature, reference, referenceablesInReferencedBlock);
				return extendedFeaturePathCandidates;
			}
			
			else if (referencedByLeaf instanceof LocalVar localVar) {
				var referencedBlock = EcoreUtil2.getContainerOfType(localVar, Block.class);
				// parenStatement should be present here, a localVar is always part of a Stat
				var parentStatement = StatUtil.getParentStatement(assignedReferencing);
				var referenceablesInReferencedBlock = ReferenceableUtil.getReferenceablesForContextFromBlock(localVar, referencedBlock, parentStatement.get());
				var localVarFqn = nameConverter.toQualifiedName(localVar.getName()); // localVars have a single-element name
				var referenceableCandidatesForLocalVar = referenceablesInReferencedBlock.stream()
											.filter(ref -> qualifiedNameProvider.getFullyQualifiedName(ref)
																.startsWith(localVarFqn)
											 ).toList();
				// TODO: should maybe use something similar to the referencedByLeaf instanceof Feature-case here,
				// i.e. some functionality analogous to findFeaturePathCandidatesThatMatchUntil?
				var extendedFeaturePathCandidates = featurePathCandidateBuilder.buildFeaturePathCandidates(referenceableCandidatesForLocalVar, localVarFqn.getSegmentCount());
				return extendedFeaturePathCandidates;
				
			}	
			
			else if (referencedByLeaf instanceof Arg) {
				// Args should already be part of the Referenceables/featurePathCandidates
				return Collections.emptyList();
			}
			
			else if (referencedByLeaf instanceof Field field) {
				final var valueExp = field.getValueExp();
				if (valueExp instanceof Feature featureValue) {
					var referencedBlock = EcoreUtil2.getContainerOfType(featureValue, Block.class);
					var parentStatement = StatUtil.getParentStatement(assignedReferencing);
					var referenceablesInReferencedBlock = ReferenceableUtil.getReferenceablesForContextFromBlock(featureValue, referencedBlock, parentStatement.get());
					return findFeaturePathCandidatesThatMatchUntil(featureValue, reference, referenceablesInReferencedBlock);
				}
				if (valueExp instanceof ExpLiteral) {
					return Collections.emptyList();
				}
				throw new RuntimeException("Unexpected type: " + valueExp + ", expected Feature for value expression.");
			}
			
			// throw warning if the referenced leaf is a proxy object
			else if (referencedByLeaf != null && referencedByLeaf.eIsProxy()) {
				LOGGER.warn("Found proxy object " + referencedByLeaf + " while attempting to resolve assigned referencing " + assignedReferencing);
				return Collections.emptyList();
			} else {
				// there should be no other options then function calls and feature paths
				throw new RuntimeException("Unexpected type: " + referencedByLeaf + ", expected function declaration or Feature.");
			}
			

		} else {
			throw new RuntimeException("Expected Feature type, but got " + assignedReferencing + ".");
		}
    }
    
    
    private List<FeaturePathCandidate> getFeaturePathCandidatesFromFuncBody(FuncBody funcBody) {
    	if (funcBody == null) {
    		return Collections.emptyList();
    	}
    
		final var containingBlock = funcBody.getBlock();
		List<FeaturePathCandidate> result = new ArrayList<>();
		
		final var returnStatOpt = ReturnUtil.findReturnStatInBlock(containingBlock);
		if (returnStatOpt.isPresent()) {
			var returnStat = returnStatOpt.get();
		
			final var exps = ReturnUtil.getExpsFromReturnStat(returnStat);
			if (!exps.isEmpty()) {
				final var exp = exps.get(0);
				// use the returned exp's named leaf if it is a feature to get it's fqn and compute the candidates in the body
				if (exp instanceof Feature feature) {
					final var leafOpt = FeatureUtil.findFeaturePathNamedLeaf(feature);
					if (leafOpt.isEmpty()) {
						return result;
					}
					
					final var expFqn = qualifiedNameProvider.getFullyQualifiedName(leafOpt.get());
					final var indexToMatch = expFqn.getSegmentCount();
					ReferenceableUtil.getReferenceablesForContextFromBlock(returnStat, containingBlock, null)
						.stream()
						.filter(referenceable -> qualifiedNameProvider.getFullyQualifiedName(referenceable).startsWith(expFqn))
						.map(ref -> featurePathCandidateBuilder.buildFeaturePathCandidate(ref, indexToMatch))
						.forEach(result::add);
				}
			}
		}

		return result;
    }
    
    
    /**
     * Only call this for features of a require func call feature path, e.g. "member" in require(...).member.
     */
    protected IScope getScopeForFeatureOfRequireFuncCallFeaturePath(EObject context, EReference reference) {
    	if (LuaRequireUtil.isPartOfRequireFunctionCallFeaturePath(context)) {
        	final var featureOfRequireFuncCallFeaturePath = (Feature) context;
        	final var featurePathPrefixOpt = FeatureUtil.findFeaturePathPrefixAsVar(featureOfRequireFuncCallFeaturePath);
        	if (featurePathPrefixOpt.isPresent()) {
        		final var featurePathRoot = featurePathPrefixOpt.get();
            	if (featurePathRoot != context) { // the "require" Var itself is resolved using the global scope (standard library)
            		final var featurePathCandidates = getFeaturePathCandidatesFromRequireCallFeaturePathUntil(context, reference, 0, featurePathRoot, featureOfRequireFuncCallFeaturePath);
    		    	final var candidates = featurePathCandidates.stream()
    				 	.filter(FeaturePathCandidate::isCompletelyMatched)
    				 	.map(FeaturePathCandidate::getReferenceable)
    				 	.toList();

    				return new SimpleScope(descriptionCreator.createFor(candidates));
            	}
        	} else {
        		LOGGER.warn("Cannot get Scope for feature of require function call feature path because feature path does not start with a Var.");
        	}
        }
    	return null;
    }

    /**
     * Returns the FeaturePathCandidates for a require call feature path, that is returns all candidates from the returned expression
     * at index {@code returnExpIndex} for the given require call, with their {@link FeaturePathCandidate#indexToCheck} set to the index after
     * the last segment of the require call feature feature path.</br>
     * E.g.: a requireFuncCall in require(...).member returns FeaturePathCandidates from the file ... with the indexToCheck 
     * set to the segment after "member".</br>
     * The indexToCheck is always at least 1, since the name of the returned expression itself is always matched by the "require".
     * 
     * @param context the current context object.
     * @param reference the current reference object.
     * @param returnExpIndex the index for which the require return expression referenceables should be returned (e.g. a, b = require(...) for b -> returnExpIndex = 1)
     * @param requireFuncCall the require Var (start of the require call feature path).
     * @return the candidates with the indexToCheck set according to the require call feature path.
     */
    private List<FeaturePathCandidate> getAllFeaturePathCandidatesFromRequireCallFeaturePath(
    		final EObject context, 
    		final EReference reference, 
    		final int returnExpIndex, 
    		final Var requireFuncCall) {
    	if (!LuaRequireUtil.isRequireFunctionCall(requireFuncCall)) {
    		throw new RuntimeException("requireFuncCall must be a require function call!");
    	}
    	
    	final var lastFeature = FeatureUtil.getFeaturePathLeaf(requireFuncCall);
    	return getFeaturePathCandidatesFromRequireCallFeaturePathUntil(context, reference, returnExpIndex, requireFuncCall, lastFeature);
    }
    
    /**
     * Returns the FeaturePathCandidates for a require call feature path, that is returns all candidates from the returned expression
     * at index {@code returnExpIndex} for the given require call, with their {@link FeaturePathCandidate#indexToCheck} set to the index after
     * the last segment of the require call feature feature path that matches the given lastSegment.</br>
     * E.g.: a requireFuncCall in require(...).member returns FeaturePathCandidates from the file ... with the indexToCheck 
     * set to the segment after "member".</br>
     * The indexToCheck is always at least 1, since the name of the returned expression itself is always matched by the "require".
     * 
     * 
     * @param context the current context object.
     * @param reference the current reference object.
     * @param returnExpIndex the index for which the require return expression referenceables should be returned (e.g. a, b = require(...) for b -> returnExpIndex = 1)
     * @param requireFuncCall the require Var (start of the require call feature path).
     * @param lastFeature the last feature of the require func call feature path that should be included for candidate matching.
     * @return the candidates with the indexToCheck set according to the require call feature path.
     */
    private List<FeaturePathCandidate> getFeaturePathCandidatesFromRequireCallFeaturePathUntil(
    		final EObject context, 
    		final EReference reference, 
    		final int returnExpIndex, 
    		final Var requireFuncCall,
    		final Feature lastFeature) {
    	
    	if (!LuaRequireUtil.isRequireFunctionCall(requireFuncCall)) {
    		throw new RuntimeException("requireFuncCall must be a require function call!");
    	}
    	var requireCallReferenceables = getReferenceablesFromRequireCall(context, reference, returnExpIndex, requireFuncCall);
    	if (FeatureUtil.hasNextFeature(requireFuncCall)) {
    		var funcCallFeature = FeatureUtil.getNextFeature(requireFuncCall);
    		var featurePathCandidates = featurePathCandidateBuilder.buildFeaturePathCandidates(requireCallReferenceables, 1);
    		// only filter and extend candidates if require() is followed by other features
    		if (FeatureUtil.hasNextFeature(funcCallFeature)) { 
    			var nextFeature = FeatureUtil.getNextFeature(funcCallFeature);
        		// we start at indexToStart = 1 with the nextFeature since "require" would not match whatever the return value of the require function is called
        		var filteredCandidates = filterAndExtendFeaturePathCandidatesFromFeatureToFeature(context, reference, nextFeature, lastFeature, featurePathCandidates);
        		return filteredCandidates;
    		}
    		// else return all candidates of require function
    		return featurePathCandidates;
    	}
    	return Collections.emptyList();
    }
    
    
    /**
     * Returns the referenceable objects from the return expression at the given index of a call to the "require" function.
     * @param context
     * @param reference
     * @param returnExpIndex
     * @param requireFuncCall
     * @return
     */
    private List<Referenceable> getReferenceablesFromRequireCall(
    		final EObject context, 
    		final EReference reference, 
    		final int returnExpIndex, 
    		final Var requireFuncCall) {
    	final var uriString = uriResolver.apply(requireFuncCall);
    	if (uriString == null) {
    		LOGGER.warn("Cannot get Referenceables from require func call " + requireFuncCall 
    					+ " with suffix " + requireFuncCall.getSuffixExp() 
    					+ " because the import uri String is null.");
    		LOGGER.error("Implement some workaround or solution for local require = require (see apisix/balancer.lua).");
    		
    		return Collections.emptyList();
    	}
		
    	final var uri = URI.createURI(uriString);
    	var requireCallReturnedScope = globalScopeProvider.getScope(context.eResource(), 
    											reference, 
    											//null
    											LuaGlobalScopeProvider.returnedExpAtIndexFilter(returnExpIndex, uri.toString())
    											);
    	
    	// TODO: the "getAllElements" call here outputs the info:
    	// "ImportedNamesAdapter  - getElements should be called with a QualifiedName during linking."
    	// But we need all elements of the scope here, i.e. cannot filter by any qualified name
    	return StreamSupport.stream(requireCallReturnedScope.getAllElements().spliterator(), false)
    				.map(obj -> obj.getEObjectOrProxy())
    				.filter(obj -> obj instanceof Referenceable)
    				.map(obj -> (Referenceable) obj)
    				.toList();
    }


}
