package org.xtext.lua.scoping;

import java.util.Collection;

import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.xtext.lua.lua.FunctionDeclaration;
import org.xtext.lua.lua.Referenceable;

import com.google.inject.Inject;

public class DescriptionCreator {
	@Inject
    private IQualifiedNameConverter nameConverter;
    
    /**
     * We always need to create descriptions (instead of returning Scopes.scopeFor(candidates)), since the 
     * candidates name might contain ".", which would fail to match the QualifiedName generated from the cross-reference
     * in the DefaultLinkingService.</br>
     * This is because the qualifiedNameConverter uses "." to separate Strings, i.e.
     * the generated cross-reference QualifiedName for "hello.world" would be "hello" "world", which would not match
     * the candidate's name "hello.world".</br></br>
     * 
     * An alternative would be to extend the IQualifiedNameConverter.DefaultImpl and change/remove the delimiter.</br></br>
	 *
     * @param candidates The candidates.
     * @return A list of EObjectDescriptions of the candidates, created by applying the qualifiedNameConverter to the candidate's name attribute.
     */
    protected Collection<IEObjectDescription> createFor(Collection<? extends Referenceable> candidates) {
    	return candidates.stream()
    				.map(c ->{
    					var name = c.getName();
    					if (c instanceof FunctionDeclaration fd) {
    						name = getLastSegmentFromFunctionDeclarationName(fd);
    					}
    					return EObjectDescription.create(nameConverter.toQualifiedName(name), c);
    				})
    				.toList();
    }
    
    private String getLastSegmentFromFunctionDeclarationName(FunctionDeclaration fd) {
    	var qn = nameConverter.toQualifiedName(fd.getName());
    	return qn.getLastSegment();
    } 
	
}
