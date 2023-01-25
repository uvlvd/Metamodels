package org.xtext.lua;

import java.util.Optional;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.xtext.lua.lua.Assignment_Destination;
import org.xtext.lua.lua.Chunk;
import org.xtext.lua.lua.Component;
import org.xtext.lua.lua.Expression;
import org.xtext.lua.lua.Expression_String;
import org.xtext.lua.lua.NamedChunk;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Statement_Assignment;
import org.xtext.lua.lua.Statement_Declaration;
import org.xtext.lua.lua.Statement_Function_Declaration;

public class LuaUtil {
    private static final Logger LOGGER = Logger.getLogger(LuaUtil.class.getPackageName());

    /**
     * 
     * @param chunk
     * @return The component which contains chunk
     */
    public static Component getComponentOfChunk(Chunk chunk) {
        var parent = chunk.eContainer();
        if (parent instanceof NamedChunk) {
            var grandparent = parent.eContainer();
            if (grandparent instanceof Component) {
                return (Component) grandparent;
            }
        }
        return null;
    }

    /**
     * Return the source code representation of the given EObject
     * 
     * @param eObj
     * @return
     */
    public static String eObjectToTokenText(EObject eObj) {
        var node = NodeModelUtils.getNode(eObj);
        if (node != null) {
            return NodeModelUtils.getTokenText(node);
        }
        return "";
    }

    /**
     * Extract the string from a string expression as parsed by the grammar
     * 
     * @param expString
     * @return The extracted string
     */
    // TODO this could be implemented differently in the grammar, so we don't
    // need to strip here
    public static String expressionStringToString(Expression_String expString) {
        // this string still contains quotes
        var rawString = expString.getValue();
        if (rawString.length() > 2) {
            return rawString.substring(1, rawString.length() - 1);
        }
        return "";
    }

    /**
     * Find a function declaration by name in a chunk
     * 
     * @param declarationName
     * @param chunk
     * @return
     */
    public static Optional<Statement_Function_Declaration> getFunctionDeclarationByName(String declarationName,
            Chunk chunk) {
        var declarations = EcoreUtil2.getAllContentsOfType(chunk, Statement_Function_Declaration.class);
        return declarations.stream()
            .filter((decl) -> decl.getName()
                .equals(declarationName))
            .findFirst();
    }

    public static Statement_Declaration getContainingDeclaration(final EObject obj) {
        return EcoreUtil2.<Statement_Declaration> getContainerOfType(obj, Statement_Declaration.class);
    }

    /**
     * This method can be used to resolve a value expression to its corresponding Referenceable in
     * an assignment
     * 
     * ref1, ref2 = val1, val2
     * 
     * This method can be used to e.g. resolve val2 to ref2
     */
    public static Assignment_Destination resolveValueToDest(Expression expression) {
        if (expression.eContainer() instanceof Statement_Assignment) {
            var assignment = (Statement_Assignment) expression.eContainer();

            if (assignment != null) {
                var index = assignment.getValues()
                    .indexOf(expression);
                if (index < 0)
                    return null; // expression did not exist

                return assignment.getDests()
                    .get(index);
            }
        }
        return null;
    }

    /**
     * This method can be used to resolve a Referenceable to its corresponding value expression in
     * an assignment
     * 
     * Having an assignment like ref1, ref2 = val1, val2
     * 
     * This method can be used to e.g. resolve ref1 to val1
     */
    public static Expression resolveRefToValue(Referenceable refble) {
        if (!(refble.eContainer() instanceof Statement_Assignment)) {
            LOGGER.debug("resolveRefToValue: Cannot resolve Referenceable that is outside a MultiReferenceable");
            return null;
        }

        var assignment = (Statement_Assignment) refble.eContainer();
        if (assignment == null) {
            LOGGER.error("resolveRefToValue: Container is null");
            return null; // expression did not exist
        }

        var index = assignment.getDests()
            .indexOf(refble);
        if (index < 0) {
            LOGGER.debug("resolveRefToValue: expression does not exist");
            return null; // expression did not exist
        } else if (index >= assignment.getValues()
            .size()) {
            LOGGER.debug("resolveRefToValue: Cannot determine value for ref as there are not enough values");
            return null; // more names than value -> we cannot decide what value was intended
        }

        return assignment.getValues()
            .get(index);
    }
}
