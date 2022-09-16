/*
 * generated by Xtext 2.28.0
 */
package org.xtext.lua.ui.labeling;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.ui.label.DefaultEObjectLabelProvider;

import com.google.inject.Inject;

/**
 * Provides labels for EObjects.
 * 
 * See
 * https://www.eclipse.org/Xtext/documentation/310_eclipse_support.html#label-provider
 */
public class LuaLabelProvider extends DefaultEObjectLabelProvider {

	@Inject
	DefaultDeclarativeQualifiedNameProvider qualifiedNameProvider;

	@Inject
	public LuaLabelProvider(AdapterFactoryLabelProvider delegate) {
		super(delegate);
	}

	// Labels and icons can be computed like this:
	String text(EObject ele) {
		var cls = ele.eClass();

//		var attributes = cls.getEAllAttributes().stream().map(a -> {
//			return a.getName();
//		}).collect(Collectors.joining(","));

		var sb = new StringBuilder();
		sb.append(cls.getName());

		var fqn = qualifiedNameProvider.apply(ele);
		if (fqn != null) {
			sb.append(" aka " + fqn);
		}
		return sb.toString();
	}
//
//	String image(Greeting ele) {
//		return "Greeting.gif";
//	}

}
