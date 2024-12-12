package org.xtext.lua.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.lua.Assignment;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.BlockWrapperWithArgs;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.Field;
import org.xtext.lua.lua.FuncBody;
import org.xtext.lua.lua.FunctionDeclaration;
import org.xtext.lua.lua.GenericFor;
import org.xtext.lua.lua.LastStat;
import org.xtext.lua.lua.LocalAssignment;
import org.xtext.lua.lua.LocalFunctionDeclaration;
import org.xtext.lua.lua.LocalVar;
import org.xtext.lua.lua.NamedFeature;
import org.xtext.lua.lua.NumericFor;
import org.xtext.lua.lua.PrefixExp;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Stat;

public class ReferenceableUtil {
	private static final Logger LOGGER = Logger.getLogger(ReferenceableUtil.class);
	
	private ReferenceableUtil() { }
	
	/**
	 * 
	 * @param context
	 * @param block
	 * @param stopStat the statement until all statements inside the Block should be searched, null if all statements in the block should be searched.
	 * @return
	 */
	public static List<? extends Referenceable> getReferenceablesForContextFromBlock(final EObject context, final Block block, final Stat stopStat) {
		List<Referenceable> referenceables = new ArrayList<>();
		if (block.eContainer() instanceof BlockWrapperWithArgs bwwa) {
			referenceables.addAll(getArgsFromBlockWrapperWithArgs(bwwa));
		}
		var referenceablesInBlock = streamAllStatsFromBlockUntil(block, stopStat)
						.flatMap(stat -> getReferenceablesFromStat(stat).stream())
						.toList();
		
		referenceables.addAll(referenceablesInBlock);
    	// reverse result s.t. the last assignment before the currently considered context is the first element in the resulting candidate list
    	Collections.reverse(referenceables);
		LOGGER.debug("Searching for: " + context + " in block :" + block + "; found: " + referenceables);

		return referenceables;
	}
	
	private static Stream<? extends Stat> streamAllStatsFromBlock(final Block block) {
		return streamAllStatsFromBlockUntil(block, null);
	}
	
	private static Stream<? extends Stat> streamAllStatsFromBlockUntil(final Block block, final Stat stopStat) {
		return block.eContents().stream()
				// because of how we defined the xtext grammar, PrefixExps extend Stat and need to be filtered here
				.filter(stat -> !(stat instanceof PrefixExp))
				// do not return LastStat
				.filter(stat -> !(stat instanceof LastStat))
				// stop at stopStat (usually the statement the context is contained in, 
				//only statements before the context' statement are referenceable by the context)
				.takeWhile(stat -> !EcoreUtil.isAncestor(stat, stopStat))
				// map to correct return type
				.map(stat -> (Stat) stat); 
	}
	
	public static List<? extends Referenceable> getReferenceablesFromStat(Stat stat) {
		List<Referenceable> result = new ArrayList<>();
		
		if (stat instanceof Assignment assignment) {
			result.addAll(getReferenceablesFromAssignment(assignment));
		}
		
		if (stat instanceof LocalAssignment localAssignment) {
			result.addAll(getReferenceablesFromLocalAssignment(localAssignment));
		}
		// other Referenceables include e.g. FunctionDeclaration, LocalFunctionDeclaration
		if (stat instanceof Referenceable ref) { 
			result.add(ref);
		} 
		
		// statement may contain blocks, add visible Referenceables from these
		final var childBlocks = EcoreUtil2.getAllContentsOfType(stat, Block.class);
		if (!childBlocks.isEmpty()) { 
			result.addAll(childBlocks.stream()
					.flatMap(block -> streamExternallyVisibleReferenceablesFromBlock(block))
					.toList()
			);
		} 

		return result;
	}
	
	private static List<? extends Referenceable> getReferenceablesFromAssignment(Assignment assignment) {
		return EcoreUtil2.getAllContentsOfType(assignment, Referenceable.class)
				.stream()
				// filter for vars on lhs and fields on rhs
				.filter(referenceable -> referenceable instanceof Field || !EcoreUtil2.isAncestor(assignment.getExpList(), referenceable))
				// assignables and fields in table connstructors are Referenceables in Assignments
				.filter(referenceable -> AssignmentUtil.isAssignable(referenceable) || referenceable instanceof Field)
				.toList();
	}
	
	private static List<? extends Referenceable> getReferenceablesFromLocalAssignment(LocalAssignment localAssignment) {
		return EcoreUtil2.getAllContentsOfType(localAssignment, Referenceable.class)
				.stream()
				.filter(referenceable -> referenceable instanceof LocalVar || referenceable instanceof Field)
				.toList();
	}
	
	private static List<? extends Referenceable> getArgsFromBlockWrapperWithArgs(BlockWrapperWithArgs bwwa) {
		if (bwwa instanceof NumericFor numericFor) {
			return Collections.singletonList(numericFor.getArg());
		} else if (bwwa instanceof GenericFor genericFor) {
			return genericFor.getArgList().getArgs();
		} else if (bwwa instanceof FuncBody funcBody && funcBody.getParList() != null) {
			var argList = funcBody.getParList().getArgsList();
			if (argList == null) {
				LOGGER.warn("Found ExpVarArgs in FuncBody ParList, scoping is not supported for varargs.");
				return Collections.emptyList();
			}
			return argList.getArgs();
		}
		return Collections.emptyList();
	}
	
	public static boolean referencesImplicitSelfParam(EObject context) {
		if (!(context instanceof Feature)) {
			return false;
		}
		var feature = (Feature) context;
		
		if (!(feature instanceof NamedFeature namedFeature) 
			 || !namedFeature.getName().equals(LuaConstants.SELF_PARAM_NAME)) {
			return false;
		}
		// check if feature is contained in a function
		var containingFuncBody = EcoreUtil2.getContainerOfType(feature, FuncBody.class);
		if (containingFuncBody == null || containingFuncBody.getParList() == null) {
			return false;
		}
		// check if function args already contain a "self" parameter
		var argList = containingFuncBody.getParList().getArgsList();
		if (argList == null) {
			// ExpVarArgs in parlist
			return false;
		}
		return !argList.getArgs().stream()
					  .anyMatch(arg -> arg.getName().equals(LuaConstants.SELF_PARAM_NAME));
	}
	
	public static Stream<? extends Referenceable> streamExternallyVisibleReferenceablesFromBlock(Block block) {
		return streamAllStatsFromBlock(block)
				.flatMap(stat -> {
					if (stat instanceof Assignment assignment) {
						return AssignmentUtil.getAssignablesFromAssignment(assignment).stream().filter(
								referenceable -> !isNamePartOfPreviousLocalAssignment(referenceable, stat, block));
					} else if (stat instanceof FunctionDeclaration funcDecl) {
						if (!isNamePartOfPreviousLocalFunctionDeclaration(funcDecl, block)) {
							if (!isNamePartOfPreviousLocalFunctionDeclaration(funcDecl, block)) {
								return Arrays.asList(funcDecl).stream();
							}
						}
					}
					return new ArrayList<Referenceable>().stream();
				});
	}
	
	private static boolean isNamePartOfPreviousLocalAssignment(Referenceable referenceable, Stat stat, Block block) {
		// check name of featurePathRoot if referenceable is part of featurePath, else the referenceable's own name
		final var name = getNameOrFeaturePathRootName(referenceable);
		return streamAllStatsFromBlockUntil(block, stat)
			.filter(stmt -> stmt instanceof LocalAssignment)
			.anyMatch(localAssignment -> localAssignmentContainsName(name, (LocalAssignment) localAssignment));
	}
	
	private static String getNameOrFeaturePathRootName(Referenceable referenceable) {
		var name = referenceable.getName();
		if (referenceable instanceof Feature feature) {
			final var featurePathRootOpt = FeatureUtil.findFeaturePathRootAsVar(feature);
			if (featurePathRootOpt.isPresent()) {
				name = featurePathRootOpt.get().getName();
			}
		}
		return name;
	}
	
	/**
	 * Returns true if the local assignment contains a {@link Var} with the given name.
	 */
	private static boolean localAssignmentContainsName(String name, LocalAssignment localAssignment) {
		return localAssignment.getVars().getNames()
					.stream()
					.map(localVarName -> localVarName.getName())
					.anyMatch(localVarName -> name.equals(localVarName));
	}
	
	private static boolean isNamePartOfPreviousLocalFunctionDeclaration(FunctionDeclaration funcDecl, Block block) {
		return streamAllStatsFromBlockUntil(block, funcDecl)
			.anyMatch(stat -> 
				stat instanceof LocalFunctionDeclaration localFuncDecl 
					&& funcDecl.getName().equals(localFuncDecl.getName())

			);
	}
	
	
	
}
