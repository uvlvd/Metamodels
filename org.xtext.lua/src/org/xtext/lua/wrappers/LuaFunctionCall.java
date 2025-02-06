package org.xtext.lua.wrappers;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.FunctionCallStat;
import org.xtext.lua.lua.MethodCall;
import org.xtext.lua.lua.NamedFeature;
import org.xtext.lua.mocking.FeaturePath;
import org.xtext.lua.utils.FeatureUtil;
import org.xtext.lua.utils.FunctionUtil;

public class LuaFunctionCall {
	private static final Logger LOGGER = Logger.getLogger(LuaFunctionCall.class);

	/**
	 * The name of the called function;
	 */
	private String name;
	/**
	 * The called function.
	 */
	private LuaFunctionDeclaration calledFunction;
	/**
	 * The feature calling the function;
	 */
	private Feature callingFeature;
	
	/**
	 * The named feature of this function call, either the name of a {@link MethodCall}, or
	 * the feature preceding the {@link FunctionCall} this {@link LuaFunctionCall} was built from.
	 */
	private NamedFeature namedFeature;
	
	// set on init, contract: calledFunction = null <=> isMocked = true
	private boolean isMocked = false; 
	
	
	private LuaFunctionCall() { }
	
	
	/**
	 * Returns the name of the <i>called</i> function.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the called function, or null if the reference to the called function {@link #isMocked()}.
	 */
	public LuaFunctionDeclaration getCalledFunction() {
		return calledFunction;
	}
	
	public Feature getCallingFeature() {
		return callingFeature;
	}

	public NamedFeature getNamedFeature() {
		return namedFeature;
	}


	/**
	 * Returns true if the reference to the called function is mocked, i.e. {@link #getCalledFunction()} returns null.
	 */
	public boolean isMocked() {
		return isMocked;
	}
	
	/**
	 * Returns a {LuaFunctionCall} built from the given {@link EObject},
	 * if the <code> eObj </code> is a function call type (e.g., {@link FunctionCallStat},
	 * {@link FunctionCall}, or {@link MethodCall}.
	 * <p> Returns null if the building failed. </p>
	 * @param eObj the eObj.
	 * @return the {@link LuaFunctionDeclaration} built from the <code> eObj </code>, or null.
	 */
	public static LuaFunctionCall of(EObject eObj) {
		if (eObj instanceof FunctionCallStat call) {
			return of(call);
		}
		if (eObj instanceof FunctionCall call) {
			return of(call);
		}
		if (eObj instanceof MethodCall call) {
			return of(call);
		}
		return null;
	}

	public static LuaFunctionCall of(final FunctionCallStat functionCallStat) {
		final var featureRoot = (Feature) functionCallStat.getPrefix();
		final var featurePathLeaf = FeatureUtil.getFeaturePathLeaf(featureRoot);
		
		if (featurePathLeaf instanceof FunctionCall fc) {
			return of(fc);
		}
		
		if (featurePathLeaf instanceof MethodCall mc) {
			return of(mc);
		}
		
		return null;
	}
	
	public static LuaFunctionCall of(final FunctionCall functionCall) {
		var result = new LuaFunctionCall();
		
		var previousFeature = FeatureUtil.getPreviousFeature(functionCall);
		if (previousFeature instanceof NamedFeature named) {
			result.initFromNamedFeature(functionCall, named);
		}
		// TODO: a function call of type func()() is currently not initialised by this function
		//       we would need to check if the previous feature is also a functionCall, and, if so, resolve
		//       that function call to the returned function
		
		if (!result.validateConstruction()) {
			return null;
		}
		
		return result;
	}
	
	public static LuaFunctionCall of(final MethodCall methodCall) {
		var result = new LuaFunctionCall();
		
		result.initFromNamedFeature(methodCall, methodCall);
		if (!result.validateConstruction()) {
			return null;
		}
		
		return result;
	}
	
	
	/**
	 * The Lua CM parser contains different kinds of features that form feature paths, e.g. the feature path </br>
	 *  {@code var.func()} </br>
	 * contains the Features {@code var}, {@code func} and {@code ()} (a function call). Thus, the {@link callingFeature} is not always the Feature
	 * referencing the called function, e.g. for a {@link FunctionCall} in a path like {@code func()} the Feature actually containing the reference to the called function
	 * is {@code func}, not {@code ()}. </br>
	 * This initializes the {@link LuaFunctionCall} based on the given named feature, which is</br>
	 * 	- the last named feature of the feature path for {@link FunctionCallStat}s</br>
	 * 	- the first named feature prefix for {@link FunctionCall} Features</br>
	 *  - the {@link MethodCall} for {@link MethodCall}s</br>
	 * 
	 * @param named the NamedFeature this functionCall calls (i.e. the feature referencing the called function).</br>
	 */
	private void initFromNamedFeature(Feature callingFeature, NamedFeature named) {
		this.callingFeature = callingFeature;
		this.name = named.getName();
		this.namedFeature = named;
		this.calledFunction = getCalledFunction(named);
		if (calledFunction == null) {
			// TODO: this is confusing because it is different then MockUtil.isMocked(),
			// here, a LuaFunctionCall is mocked if we cannot trace the reference chain back to the original
			// function declaration, wheres MockUtil.isMocked() returns true when the referenced object is a mock object..
			this.isMocked = true;
		}
	}
	
	private boolean validateConstruction() {
		// cannot create LuaFunctionCall from null.
		if (getCallingFeature() == null) {
			return false;
		}
		
		
		if (name == null || namedFeature == null) {
			var featurePath = new FeaturePath(getCallingFeature());
			var features = featurePath.getContextFeatures();
			LOGGER.error("Expected FunctionCall built from " + getCallingFeature() + " to contain a named feature leaf. FeaturePath context features: " + features);
			return false;
		}
		
		return true;
	}
	
	private LuaFunctionDeclaration getCalledFunction(NamedFeature named) {
		//var ref = named.getRef();
		return FunctionUtil.getReferencedFunction(named);
	}
	

}
