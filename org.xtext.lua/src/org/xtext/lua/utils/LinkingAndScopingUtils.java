package org.xtext.lua.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.xtext.lua.Config;
import org.xtext.lua.lua.Assignment;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.BlockWrapperWithArgs;
import org.xtext.lua.lua.Exp;
import org.xtext.lua.lua.ExpField;
import org.xtext.lua.lua.ExpFunctionDeclaration;
import org.xtext.lua.lua.ExpList;
import org.xtext.lua.lua.ExpLiteral;
import org.xtext.lua.lua.ExpNumberLiteral;
import org.xtext.lua.lua.ExpStringLiteral;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.Field;
import org.xtext.lua.lua.FieldList;
import org.xtext.lua.lua.FuncBody;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.FunctionDeclaration;
import org.xtext.lua.lua.GenericFor;
import org.xtext.lua.lua.IndexExpField;
import org.xtext.lua.lua.LastStat;
import org.xtext.lua.lua.LocalAssignment;
import org.xtext.lua.lua.LocalFunctionDeclaration;
import org.xtext.lua.lua.LocalVar;
import org.xtext.lua.lua.MethodCall;
import org.xtext.lua.lua.NumericFor;
import org.xtext.lua.lua.PrefixExp;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.Return;
import org.xtext.lua.lua.Stat;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.lua.TableConstructor;
import org.xtext.lua.lua.Var;

public final class LinkingAndScopingUtils {
	private static final Logger LOGGER = Logger.getLogger(LinkingAndScopingUtils.class);
	
	// dummy name set for TableAccess if the indexExp cannot be resolved in DerivedStateComputer
	public static final String DERIVED_DUMMY_NAME = "derived_dummy_name";
	// dummy name set for TableAccess if the indexExp cannot be resolved during Linking
	public static final String LINKING_DUMMY_NAME = "linking_dummy_name";
	
	private static final String NUMBER_NAME_STRING_PREFIX = "N__";
	
	//TODO: isAssignable function (replaces isOnLhsOfAssignment): refble needs to be leaf
	//      of Featue path
	/**
	 * Returns true if the given object get assigned a value, i.e. is a local variable 
	 * or leaf in a feature path on the left-hand side of a global assignment. </br>
	 * @param obj the object.
	 */
	public static boolean isAssignable(EObject obj) {
		if (isLocalAssignable(obj)) {
			return true;
		}
		return isGlobalAssignable(obj);
	}
	
	/**
	 * Returns whether the object is a variable in a local assignment.
	 */
	private static boolean isLocalAssignable(EObject obj) {
		return obj instanceof LocalVar;
	}
	
	/**
	 * Returns true if the given object is part of the lhs of a global assignment, i.e. gets assigned a value. </br>
	 * Only leafs of a feature path (a path described by the PrefixExp grammar rule) can be assignables. </br>
	 * E.g. a.member = 10 returns true for member, false for a.
	 * @param obj the object.
	 */
	private static boolean isGlobalAssignable(EObject obj) {
		// only features that are Referenceable can be assignables (i.e. Vars, MemberAccess, TableAccess, ... but not FunctionCalls etc.)
		if (obj instanceof Feature feature && obj instanceof Referenceable) {
			// a Var that is part of a TableAccess cannot be an Assignable (e.g. the var in table[var])
			if (isPartOfAnyTableAccessIndexExp(feature)) {
				return false;
			}
					
			if (isLeafOfFeaturePath(feature)) { // only leafs of a feature path are assignable
				// check if an Assignment is parent and leaf is on lhs
				return findParentAssignmentForAssignable(feature).isPresent();
			}
		}
		return false;
	}
	
	private static boolean isLeafOfFeaturePath(Feature feature) {
		var featureChildren = feature.eContents().stream().filter(child -> child instanceof Feature).toList();
		if (feature instanceof TableAccess ta) {
			// since a table access itself can be a leaf, but may also contain Features in its indexExp,
			// we may only consider children that are not part of the indexExp as a continuation of the feature path.
			return !featureChildren.stream().anyMatch(child -> !isPartOfTableAccessIndexExp(child, ta));
		}
		return featureChildren.isEmpty();
	}
	
	private static boolean isPartOfAnyTableAccessIndexExp(EObject obj) {
		if (obj instanceof TableAccess) return false; // getContainerOfType(obj, type) returns true if obj is instance of type
		
		// check if obj has a parent that is TableAccess 
		var parentTableAccess = EcoreUtil2.getContainerOfType(obj, TableAccess.class);
		if (parentTableAccess == null) return false;
		
		return isPartOfTableAccessIndexExp(obj, parentTableAccess);
		/*
		// check if obj is single index exp of closest parent table access
		var indexExp = parentTableAccess.getIndexExp();
		if (indexExp == obj) return true;
		
		// check if obj is part of index exp of closest parent table access
		return parentTableAccess.getIndexExp().eContents().contains(obj);
		*/
	}
	
	private static boolean isPartOfTableAccessIndexExp(EObject obj, TableAccess ta) {
		var indexExp = ta.getIndexExp();
		// check if obj is single index exp of table access
		if (indexExp == obj) return true;
		// check if obj is part of index exp of table access
		return ta.getIndexExp().eContents().contains(obj);
	}
	
	public static Optional<EObject> findTableForField(Field field) {
		var containingTableConstructor = EcoreUtil2.getContainerOfType(field, TableConstructor.class);
		if (containingTableConstructor.eContainer() instanceof Field table) {
			return Optional.of(table);
		}
		
		var assignmentOpt = getAssignmentForField(field, containingTableConstructor);
		if (assignmentOpt.isPresent()) {
			var assignment = assignmentOpt.get();
			if (assignment instanceof Assignment globalAssignment) {
				return findTableForFieldInGlobalAssignment(field, globalAssignment);
			}
			if (assignment instanceof LocalAssignment localAssignment) {
				return findTableForFieldInLocalAssignment(field, localAssignment);
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Gets the assignment Stat containing the field (global or local assignment).
	 * @param field the field.
	 * @param containingTableConstructor the TableConstructor containing the field.
	 * @return
	 */
	private static Optional<EObject> getAssignmentForField(Field field, TableConstructor containingTableConstructor) {
		// TableConstructor containing field is part of assignment
		if (containingTableConstructor.eContainer() instanceof ExpList expList) {
			if (expList.eContainer() instanceof Assignment assignment) {
				return Optional.of(assignment);
			}
			if (expList.eContainer() instanceof LocalAssignment localAssignment) {
				return Optional.of(localAssignment);
			}
		}
		return Optional.empty();
	}
	
	private static Optional<EObject> findTableForFieldInLocalAssignment(Field field, LocalAssignment localAssignment) {
		var vars = localAssignment.getVars().getNames();
		var expList = localAssignment.getExpList();
		if (expList == null) {
			return Optional.empty();
		}
		return findTableForField(field, vars, expList.getExps());
	}
	
	private static Optional<EObject> findTableForFieldInGlobalAssignment(Field field, Assignment assignment) {
		var vars = assignment.getVars();
		var exps = assignment.getExpList().getExps();
		return findTableForField(field, vars, exps);
	}
	
	private static Optional<EObject> findTableForField(Field field, List<? extends EObject> vars, List<Exp> exps) {
		var fieldList = (FieldList) field.eContainer();
		var tableConstructor = (TableConstructor) fieldList.eContainer();
		
		var index = exps.indexOf(tableConstructor);
		if (index < 0 || vars.size() < index) {
			return Optional.empty();
		} else {
			var table = vars.get(index);
			return Optional.of(table);
		}
	}
	
	/**
	 * Returns the expression assigned to this feature. Use {@link #isAssignable(EObject)} to ensure
	 * the given feature is an Assignable before calling this function.
	 * @param feature the Feature (must be a Referenceable).
	 * @return the value expression assigned to the feature.
	 */
	public static Exp findAssignedExp(EObject obj) {
		if (isLocalAssignable(obj)) {
			return findAssignedExpForLocalAssignment((LocalVar) obj);
		}
		
		if(!(obj instanceof Feature)) {
			throw new RuntimeException("Cannot find assigned expression for non-assignable!");
		}
		
		var feature = (Feature) obj;
		
		if (!(feature instanceof Referenceable)) {
			throw new RuntimeException("Cannot find assigned expression for non-referenceable feature!");
		}
		
		return findAssignedExpForFeature(feature);
	}
	
	private static Exp findAssignedExpForLocalAssignment(final LocalVar var) {
		// should never be null
		final var localAssignment = EcoreUtil2.getContainerOfType(var, LocalAssignment.class);
		final var varIndex = localAssignment.getVars().getNames().indexOf(var);
		if (varIndex < 0) // should never happen
			throw new RuntimeException("Could not find local variable in assignment!");
	
		if (localAssignment.getExpList() == null) 
			return null;
		
		final var exps = localAssignment.getExpList().getExps();
        if (exps.size() > varIndex)
        	return localAssignment.getExpList().getExps().get(varIndex);
        
     // fall-through, e.g. if explist does not contain an exp for every declared var (i.e. value is 'nil')
		return null;
	}
	
	private static Exp findAssignedExpForFeature(final Feature feature) {
		final var featurePathRootOpt = findFeaturePathRoot(feature);
		if (featurePathRootOpt.isEmpty()) {
			return null;
		}
		final var featurePathRoot = featurePathRootOpt.get();
		
		final var assignmentOpt = findParentAssignmentForAssignable(featurePathRoot);
		if (assignmentOpt.isPresent()) {
			final var assignment = assignmentOpt.get();
			// TODO: need to know root of feature path for refble to find it in vars
            final var index = assignment.getVars().indexOf(featurePathRoot);
            if (index < 0) // should never happen
            	throw new RuntimeException("Could not find feature path root (Variable) in assignment!");

            final var exps = assignment.getExpList().getExps();
            if (exps.size() > index)
            	return assignment.getExpList().getExps().get(index);
		}
		// fall-through, e.g. if explist does not contain an exp for every declared var (i.e. value is 'nil')
		return null;
	}
	
	/**
	 * Returns the Assignment object this feature is contained in, if the feature is part
	 * of the lhs of an Assignment.
	 */
	public static Optional<Assignment> findParentAssignmentForAssignable(Feature feature) {
		var parent = feature.eContainer();
		
		if (parent == null) { // no parent
			return Optional.empty();
		}
		
		if (parent instanceof Assignment assignment) {
			return Optional.of(assignment);
		}
		
		if (parent instanceof Feature featureParent) {
			return findParentAssignmentForAssignable(featureParent);
		} 
		
		// object is on rhs (i.e. the parent is an ExpList) or not part of an assignment/feature path.
		return Optional.empty();
	}
	
	// TODO: could conceivably also be a parenthesized Expression (exp).member...
	private static Optional<Var> findFeaturePathRoot(Feature feature) {
		var parent = feature.eContainer();
		
		if (parent == null) { // no parent
			return Optional.empty();
		}
		
		if (parent instanceof Assignment assignment) {
			if (feature instanceof Var var) {
				return Optional.of(var);
			} else {
				throw new RuntimeException("Feature path root is not of type Var.");
			}
		}
		
		if (parent instanceof Feature parentFeature) {
			return findFeaturePathRoot(parentFeature);
		} 
		
		// object is on rhs or not part of an assignment/feature path.
		return Optional.empty();
	}
	
	public static Var getFeaturePathRoot(Feature feature) {	
		if (feature instanceof Var var) { // found root
			return var;
		}
		
		var parent = feature.eContainer();
		if (parent instanceof Feature parentFeature) {
			return getFeaturePathRoot(parentFeature);
		} 
		
		// should never be null
		throw new RuntimeException("Feature path root could not be found, this should not have happened..."); 
	}
	
	public static Feature getFeaturePathNamedLeaf(Feature feature) {
		return getFeaturePathNamedLeaf(feature, null);
	}
	
	private static Feature getFeaturePathNamedLeaf(Feature feature, Feature lastMatch) {
		if (feature instanceof Referenceable referenceable) { // has name
			lastMatch = feature;
		}
		
		Optional<Feature> featureChildOpt = feature.eContents().stream()
								.filter(child -> child instanceof Feature)
								.map(f -> (Feature) f) // cast to Feature type
								.findFirst();
		
		if (!featureChildOpt.isPresent()) {
			return lastMatch; // might be null
		}
		
		var featureChild = featureChildOpt.get();

		
		return getFeaturePathNamedLeaf(featureChild, lastMatch);
	}
	
	public static Var getFeaturePathParentForFilter(Feature feature) {	
		if (feature instanceof Var var) { // found root
			return var;
		}
		
		var parent = feature.eContainer();
		if (parent instanceof Feature parentFeature) {
			return getFeaturePathRoot(parentFeature);
		} 
		
		// should never be null
		throw new RuntimeException("Feature path root could not be found, this should not have happened..."); 
	}
	
	public static boolean hasNextFeature(Feature feature) {
		return feature.eContents().stream().anyMatch(child -> child instanceof Feature);
	}
	
	public static Feature getNextFeature(Feature feature) {
		var nextOpt = feature.eContents().stream().filter(child -> child instanceof Feature).findFirst();
		if (nextOpt.isPresent()) {
			return (Feature) nextOpt.get();
		}
		return null;
	}
	
	/**
	 * Attempts to calculate a table field's name, which depends on the field's type: </br>
	 * 	- NameField: "name" attribute  </br>
	 *  - IndexExpField: string representation of indexExp, if indexExp can be resolved, see {@link #tryResolveExpressionToString}</br>
	 *  - ExpField: name to corresponding index for ExpField in FieldList (starting from 1, counting only ExpField types in FieldList)</br>
	 * @param field the Field.
	 * @param fallback the fallback String returned if no name can be resolved.
	 * @return
	 */
	public static String tryGetNameForField(final Field field, final String fallback) {
		var name = field.getName(); // might be null
		if (field instanceof IndexExpField indexExpField) {
			name = tryResolveExpressionToString(indexExpField.getIndexExp(), fallback);
		} else if (field instanceof ExpField expField) {
			var fieldList = field.eContainer();
			// only ExpField fields affect the counting: https://www.lua.org/manual/5.2/manual.html#3.4.8
			var expFields = EcoreUtil2.getAllContentsOfType(fieldList, ExpField.class);
			var index = expFields.indexOf(expField) + 1; // Lua indexes start with 1
			name = tableKeyNumberToNameString(index);
		}
		
		if (name == null) {
			throw new RuntimeException("Could not set field name for field " + field);
		}
		
		return name;
	}
	
	//TODO: update doc, how String representation looks for e.g. different types
	//TODO: split into two methods, one for derived state computer (resolving only literals)
	// and another for linkingservice, resolving using references (e.g. variables that contain literals)
	/**
	 * Attempts to resolve the given expression to a String. 
	 * If successful, returns a String representation for the resolved expression.</br>
	 * E.g. "str" -> "str", 0 -> "0.0"
	 * @param exp the Exp.
	 * @param fallback the fallback String returned if the expression cannot be resolved.
	 * @return a String representation of the resolved expression, or the fallback if the expression can not be resolved.
	 */
	public static String tryResolveExpressionToString(final Exp exp, final String fallback) {
		String name = null;
		// A StringLiteral is returned as a String
		if (exp instanceof ExpLiteral literal) {
			name = resolveExpLiteralToNameString(literal);
		} else if (exp instanceof Referencing ref) {
			if (ref.getRef() == null && !Config.TABLE_ACCESS_REFERENCES) {
				throw new RuntimeException("Attempting to resolve value expression, but a ref " + ref + " is not yet resolved!");
			} else {
				var assignedValue = tryGetAssignedValueFrom(ref);
				if (assignedValue != null) {
					if (assignedValue instanceof ExpLiteral literal) {
						name = resolveExpLiteralToNameString(literal);
					}
				}
			}
		}
		else {
			// TODO
			LOGGER.warn("TableAccess is not (yet) implemented for non-string indexExps like " + exp);
			//throw new RuntimeException("TableAccess is not (yet) implemented for non-string indexExps!");
		}
		if (Config.TABLE_ACCESS_REFERENCES) {
			if (name == null) {
				return fallback;
			}
		}

		return name;
	}
	
	private static String resolveExpLiteralToNameString(ExpLiteral expLiteral) {
		if (expLiteral instanceof ExpStringLiteral stringLiteral) {
			return tableKeyStringLiteralToNameString(stringLiteral);
		}
		if (expLiteral instanceof ExpNumberLiteral numberLiteral) {
			// TODO: this could make use of the NumberValueConverter, but then the whole
			// expression resolution logic would need to be extracted to an injected bean
			// (in order to inject the valueConverterService).
			
			// we remove the "." separators from the double String representation to avoid
			// problems with the qualifiedNameConverter, which splits Strings on "."
			return tableKeyNumberToNameString(numberLiteral.getValue());
		}
		throw new RuntimeException("Error while resolving ExpLiteral to String.");
	}
	
	private static String tableKeyStringLiteralToNameString(ExpStringLiteral stringLiteral) {
		if (stringLiteral.getValue().startsWith(NUMBER_NAME_STRING_PREFIX)) {
			throw new RuntimeException("The parsed content contains a String literal");
		}
		return removeQuotesFromString(stringLiteral.getValue());
		//return stringLiteral.getValue();
	}
	
	/**
	 * Returns a name string used for the "name" attribute of a TableAccess or Field wich contains a Lua number value.
	 * @param d the double value of the Lua number.
	 * @return the string representation, starting with {@link #NUMBER_NAME_STRING_PREFIX} to 
	 * avoid equality with "name" attributes generated from a string, e.g. a["1"] and a[1] define different fields in table a.
	 */
	private static String tableKeyNumberToNameString(double d) {
		// use , instead of . to avoid clash with qualifiedName separator
		return NUMBER_NAME_STRING_PREFIX + Double.toString(d).replace(".", ",");
	}
	
	public static Exp tryGetAssignedValueFrom(Referencing ref) {
		return tryGetAssignedValueFrom(ref, 0, 1000);
	}
	
	
	// TODO: may lead to StackOverflow, limit depth?
	private static Exp tryGetAssignedValueFrom(Referencing ref, int currDepth, final int maxDepth) {
		if (currDepth > maxDepth) {
			LOGGER.error("Reached max depth while attempting to get assigned value from " + ref);
			return null;
		}
		if (ref.getRef() == null || ref.getRef().eIsProxy()) {
			//System.out.println("ref's "+ ref + " ref null?: " + ref.getRef());
			return null;
		}
		// TODO: this can probably be removed, since now synthetic NIL exps are assigned 
		if (ref.getRef().equals(ref)) { // reference to self means no value was assigned (i.e. the value is 'nil')
			//System.out.println("ref to self?: " + ref.getRef());
			return null;
		}
		if (ref.getRef() instanceof Referencing refsRef) {
			//System.out.println("get next ref: " + ref.getRef());
			return tryGetAssignedValueFrom(refsRef, ++currDepth, maxDepth);
			//return tryGetAssignedValueFrom(refsRef, 0, maxDepth);
		}
		//System.out.println("ref found: " + ref.getRef());
		if (ref.getRef() instanceof Exp exp) {
			return exp;
		}
		return null;
	}
	
	public static boolean isTableAccessWithDummyName(EObject o) {
		return o instanceof TableAccess ta && ta.getName().equals(DERIVED_DUMMY_NAME);
	}
	
	public static String removeQuotesFromString(String str) {
		if (str != null && str.startsWith("\"") && str.endsWith("\"")) {
			return str.substring(1, str.length() - 1);
		}
		return str;
	}
	
	public static Optional<Stat> getParentStatement(EObject obj) {
		// since all PrefixExps extend Stat, we need to return the Stat from the Block, not
		// the direct parent of the object
		var parentBlock = EcoreUtil2.getContainerOfType(obj, Block.class);
		return EcoreUtil2.getAllContentsOfType(parentBlock, Stat.class).stream()
				.filter(stat -> EcoreUtil2.isAncestor(stat, obj))
				.findAny();
	}
	


	
	
	
	
	
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
				.filter(referenceable -> isAssignable(referenceable) || referenceable instanceof Field)
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
	
	
	public static Stream<? extends Referenceable> streamExternallyVisibleReferenceablesFromBlock(Block block) {
		return streamAllStatsFromBlock(block)
			//.filter(stat -> !(stat instanceof LocalAssignment || stat instanceof LocalFunctionDeclaration))
			.flatMap(stat -> {
				if (stat instanceof Assignment assignment) {
					return getAssignablesFromAssignment(assignment).stream()
						.filter(referenceable -> !isNamePartOfPreviousLocalAssignment(referenceable, stat, block));
				} else if (stat instanceof FunctionDeclaration funcDecl) {
					if (!isNamePartOfPreviousLocalFunctionDeclaration(funcDecl, block)) {
						return Arrays.asList(funcDecl).stream();
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
			final var featurePathRootOpt = findFeaturePathRoot(feature);
			if (featurePathRootOpt.isPresent()) {
				name = featurePathRootOpt.get().getName();
			}
		}
		return name;
	}
	
	/**
	 * Returns true if the local assignment contains a var with the given name.
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
	
	// TODO: with how isAssignable() is implemented, this is probably not very efficient
	// TODO: check if other parts of the program need a "getAssignablesFromAssignment" functionality and use this method
	private static List<? extends Referenceable> getAssignablesFromAssignment(Assignment assignment) {
		return assignment.getVars().stream()
					.flatMap(var -> {
						ArrayList<Referenceable> result = new ArrayList<>();
						result.add(var);
						result.addAll(EcoreUtil2.getAllContentsOfType(var, Referenceable.class));
						return result.stream();
					})
					.filter(referenceable -> isAssignable(referenceable))
					.toList();
	}

	
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
				final var leaf = getFeaturePathNamedLeaf(feature);
				final var expFqn = qualifiedNameProvider.getFullyQualifiedName(leaf);
				final ArrayList<Referenceable> expReferenceables = new ArrayList<> ();
				getReferenceablesForContextFromBlock(returnStat, containingBlock, null)
					.stream()
					.filter(referenceable -> qualifiedNameProvider.getFullyQualifiedName(referenceable).startsWith(expFqn))
					.forEach(expReferenceables::add);
				result.add(expReferenceables);
			}
		}

		return result;
	}

	

	public static boolean isFunctionDeclaration(Referenceable referenceable) {
		return getFuncBodyFromFuncObject(referenceable) != null;
	}
	

	/**
	 * Returns true if the given feature is a FunctionCall or MethodCall-
	 */
	public static boolean isFunctionCallFeature(Feature feature) {
		return feature instanceof FunctionCall || feature instanceof MethodCall;

	}

	// TODO: is this unused? -> remove
	public static boolean isFunctionCallFeature(Referenceable referenceable) {
		
		if (referenceable instanceof Feature feature) {
			final var children = feature.eContents();
			if (!children.isEmpty()) {
				final var suffix = children.get(0);
				return suffix instanceof FunctionCall || suffix instanceof MethodCall;
			}
		}
		return false;
	}
	
	public static FuncBody getFuncBodyFromFuncObject(EObject funcObject) {
		if (funcObject instanceof FunctionDeclaration funcDecl) {
			return funcDecl.getBody();
		} else if (funcObject instanceof LocalFunctionDeclaration funcDecl) {
			return funcDecl.getBody();
		} else if (funcObject instanceof ExpFunctionDeclaration funcDecl) {
			return funcDecl.getBody();
		} else if (funcObject instanceof Referencing referencing) {
			var value = tryGetAssignedValueFrom(referencing);
			if (value instanceof ExpFunctionDeclaration funcDecl) {
				return funcDecl.getBody();
			}
		}
		return null;
	}
	
	
	public static Optional<Return> findReturnStatInBlock(Block block) {
		var returnStat = block.getLastStat();
		if (returnStat instanceof Return ret) {
			return Optional.of(ret);
		}
		return Optional.empty();
	}
	
	
}
