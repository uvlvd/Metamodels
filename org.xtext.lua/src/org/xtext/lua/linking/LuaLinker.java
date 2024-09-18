package org.xtext.lua.linking;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.diagnostics.IDiagnosticConsumer;
import org.eclipse.xtext.linking.impl.LinkingDiagnosticProducer;
import org.eclipse.xtext.linking.lazy.LazyLinker;
import org.eclipse.xtext.linking.lazy.SyntheticLinkingSupport;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.utils.LinkingAndScopingUtils;
import org.xtext.lua.lua.LuaPackage.Literals;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;

public class LuaLinker extends LazyLinker {

	@Override
	protected void doLinkModel(final EObject model, IDiagnosticConsumer consumer) {

		super.doLinkModel(model, consumer);
	}
}
