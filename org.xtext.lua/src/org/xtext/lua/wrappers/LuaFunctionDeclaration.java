package org.xtext.lua.wrappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.xtext.lua.lua.Arg;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Chunk;
import org.xtext.lua.lua.ExpFunctionDeclaration;
import org.xtext.lua.lua.FunctionDeclaration;
import org.xtext.lua.lua.LocalFunctionDeclaration;
import org.xtext.lua.lua.ParList;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Return;
import org.xtext.lua.lua.Stat;
import org.xtext.lua.scoping.LuaQualifiedNameProvider;
import org.xtext.lua.utils.ReturnUtil;
import org.xtext.lua.utils.FunctionUtil;

import com.google.inject.Inject;

public class LuaFunctionDeclaration {
	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;
	
	/**
	 * The name of the function declaration. Note that this may differ from the name of a function call
	 * calling the function declaration, since a function imported via require(...) may get assigned to a variable
	 * with a different name.
	 */
	private String name;
	private List<Arg> args;
	private Block block;
	/**
	 * The root Referenceable from the CM, might be {@link FunctionDeclaration}, {@link LocalFunctionDeclaration},
	 * or {@link ExpFunctionDeclaration}.
	 */
	private Referenceable root;
	private Stat containingStat;
	
	private Boolean isGlobal = null;
	
	private LuaFunctionDeclaration() { }
	
	

	public List<Arg> getArgs() {
		return args;
	}

	public Block getBlock() {
		return block;
	}

	/**
	 * Returns the Referenceable corresponding to this LuaFunctionDeclaration from the CM, might be {@link FunctionDeclaration}, {@link LocalFunctionDeclaration},
	 * or {@link ExpFunctionDeclaration}.
	 */
	public Referenceable getRoot() {
		return root;
	}
	
	/**
	 * Returns the name of the function declaration. Note that this may differ from the name of a function call
	 * calling the function declaration, since a function imported via require(...) may get assigned to a variable
	 * with a different name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the statement containing this function declaration.
	 */
	public Stat getContainingStat() {
		return containingStat;
	}
	
	// TODO: this needs to be tested
	public boolean isGlobal() {
		if (isGlobal == null) {
			if (root instanceof FunctionDeclaration) {
				isGlobal = true;
			}
			
			var containingChunk = EcoreUtil2.getContainerOfType(root, Chunk.class);
			if (containingChunk != null) {
				var isReturnedByChunk = getReferenceablesFromReturnStat(containingChunk)
						.stream()
						.map(ref -> FunctionUtil.getReferencedFunction(ref, 0, 1000))
						.filter(Objects::nonNull)
						.anyMatch(fd -> fd.getRoot().equals(root));
				isGlobal = isReturnedByChunk;
			}

		}
		
		// this should always be set to something other than null here, since
		// any root object should have a containingChunk.
		if (isGlobal == null) {
			throw new RuntimeException("isGlobal should never be null!");
		}
		
		return isGlobal;
	}
	
	private List<Referenceable> getReferenceablesFromReturnStat(final Chunk chunk) {
		var result = new ArrayList<Referenceable>();
		final var lastStat = chunk.getBlock().getLastStat();
		if (lastStat instanceof Return returnStat) {
			// TODO: should avoid using LuaQualifiedNameProvider here
			final var referenceables = ReturnUtil.getReferenceablesFromReturnStat(returnStat, new LuaQualifiedNameProvider());
			referenceables.stream().forEach(result::addAll);
		}
		return result;
	}
	
	/**
	 * Returns a {@LuaFunctionDeclartion} built from the given {@link EObject},
	 * if the <code> eObj </code> is a function declaration type (e.g., {@link FunctionDeclaration},
	 * {@link LocalFunctionDeclaration}, or {@link ExpFunctionDeclaration}.
	 * <p> Returns null if the building failed. </p>
	 * @param eObj the eObj.
	 * @return the {@link LuaFunctionDeclaration} built from the <code> eObj </code>, or null.
	 */
	public static LuaFunctionDeclaration of(EObject eObj) {
		if (eObj instanceof FunctionDeclaration decl) {
			return of(decl);
		}
		if (eObj instanceof LocalFunctionDeclaration decl) {
			return of(decl);
		}
		if (eObj instanceof ExpFunctionDeclaration decl) {
			return of(decl);
		}
		return null;
	}

	public static LuaFunctionDeclaration of(FunctionDeclaration decl) {
		var functionDeclaration = new LuaFunctionDeclaration();
		
		final var name = decl.getName();
		final var args = getArgsFromParList(decl.getBody().getParList());
		final var block = decl.getBody().getBlock();
		
		functionDeclaration.init(decl, name, args, block, decl);
		return functionDeclaration;
	}
	
	public static LuaFunctionDeclaration of(LocalFunctionDeclaration decl) {
		var functionDeclaration = new LuaFunctionDeclaration();
		
		final var name = decl.getName();
		final var args = getArgsFromParList(decl.getBody().getParList());
		final var block = decl.getBody().getBlock();
		
		functionDeclaration.init(decl, name, args, block, decl);
		return functionDeclaration;
	}
	
	public static LuaFunctionDeclaration of(ExpFunctionDeclaration decl) {
		var functionDeclaration = new LuaFunctionDeclaration();
		
		// name of ExpFunctionDeclaration might be null, e.g. for ExpFunctionDeclaration inside of ParamArgs
		final var name = decl.getName();
		final var containingStat = EcoreUtil2.getContainerOfType(decl, Stat.class);
		if (name == null || containingStat == null) {
			return null;
		}
		final var args = getArgsFromParList(decl.getBody().getParList());
		final var block = decl.getBody().getBlock();
		
		functionDeclaration.init(decl, name, args, block, containingStat);
		return functionDeclaration;
	}
	
	private void init(Referenceable root, String name, List<Arg> args, Block block, Stat containingStat) {
		this.root = root;
		this.name = name;
		this.args = args;
		this.block = block;
		this.containingStat = containingStat;
	}
	
	private static List<Arg> getArgsFromParList(ParList parList) {
		var result = new ArrayList<Arg>();
		if (parList != null && parList.getArgsList() != null) {
			result.addAll(parList.getArgsList().getArgs());
		}
		return result;
	}
	
}
