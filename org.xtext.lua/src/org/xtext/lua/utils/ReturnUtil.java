package org.xtext.lua.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Exp;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Return;

public class ReturnUtil {
	
	private ReturnUtil() { }
	
	public static List<Exp> getExpsFromReturnStat(Return returnStat) {
		final var expList = returnStat.getExpList();
		if (expList == null ) {
			return Collections.emptyList();
		}

		return expList.getExps();
	}
	
	// TODO: this should use the LocalScopeProvider-way of finding referenceables
	public static List<List<Referenceable>> getReferenceablesFromReturnStat(Return returnStat, final IQualifiedNameProvider qualifiedNameProvider) {
		final var containingBlock =  EcoreUtil2.getContainerOfType(returnStat, Block.class);
		List<List<Referenceable>> result = new ArrayList<>();

		final var exps = getExpsFromReturnStat(returnStat);
		for (var exp : exps) {
			if (exp instanceof Feature feature) {
				final var leafOpt = FeatureUtil.findFeaturePathNamedLeaf(feature);
				if (leafOpt.isPresent()) {
					final var expFqn = qualifiedNameProvider.getFullyQualifiedName(leafOpt.get());
					final ArrayList<Referenceable> expReferenceables = new ArrayList<> ();
					ReferenceableUtil.getReferenceablesForContextFromBlock(returnStat, containingBlock, null)
						.stream()
						.filter(referenceable -> qualifiedNameProvider.getFullyQualifiedName(referenceable).startsWith(expFqn))
						.forEach(expReferenceables::add);
					result.add(expReferenceables);
				}
			}
		}

		return result;
	}

	
	public static Optional<Return> findReturnStatInBlock(Block block) {
		var returnStat = block.getLastStat();
		if (returnStat instanceof Return ret) {
			return Optional.of(ret);
		}
		return Optional.empty();
	}

}
