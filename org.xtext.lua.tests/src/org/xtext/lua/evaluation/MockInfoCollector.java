package org.xtext.lua.evaluation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.serializer.ISerializer;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.utils.FeatureUtil;
import org.xtext.lua.utils.MockUtil;
import org.xtext.lua.utils.StatUtil;

/**
 * Used to collect information about mocked references for evaluation. A mocked reference
 * is an object referencing an object with {@link MockUtil#isMocked(EObject)} = true through its reference chain. The reference chain is given
 * by calling {@link Referencing#getRef()} repeatedly, until it returns null.
 * @author jsaenz
 *
 */
public class MockInfoCollector {
	
	private Map<MockInfo.Cause, List<MockInfo>> infoByCause = new EnumMap<>(MockInfo.Cause.class);
	
	public MockInfoCollector() {
		// init infoByCause map
		Stream.of(MockInfo.Cause.values()).forEach(cause -> infoByCause.put(cause, new ArrayList<>()));
	}
	
	public void collect(final EObject context) {
		final var info = new MockInfo(context);
		addToInfoByCause(info);
	}
	
	private void addToInfoByCause(final MockInfo info) {
		final var cause = info.getCause();
		infoByCause.get(cause).add(info);
	}
	
	public Map<MockInfo.Cause, List<MockInfo>> getInfoByCause() {
		return infoByCause;
	}
	
	public long getCount() {
		return infoByCause.values().stream().flatMap(List::stream).count();
	}
	
	public void clear() {
		infoByCause.values().stream().forEach(List::clear);
	}

	// TODO: the print methods are used for debugging and can be removed
	public void print(MockInfo info, ISerializer serializer) {
		System.out.println("Mocked object stat info: ");
		System.out.println("	serialized:   \"" + serializer.serialize(info.getParentStat()).trim() + "\"");
		System.out.println("    context:      " + info.getContext());
		System.out.println("    resource uri: " + info.getResourceUri());
	}
	
	// TODO: remove
	public static void print(TableAccess ta, ISerializer serializer) {
		System.out.println("Mocked object stat info: ");
		var statOpt = StatUtil.getParentStatement(ta);
		if (statOpt.isPresent()) {
			System.out.println("	serialized:   \"" + serializer.serialize(statOpt.get()).trim() + "\"");
		}
		var prev = FeatureUtil.getPreviousFeature(ta);
		System.out.println("    previous feature: " + prev);
		System.out.println("    	previous feature mocked: " + MockUtil.isMocked(prev));
		System.out.println("    context:      " + ta);
		System.out.println("    resource uri: " + ta.eResource().getURI());
	}

	
	//TODO: class unfinished, needs implementation
//	public void printContextInfo(final EObject context) {
//		final var statOpt = StatUtil.getParentStatement(context);
//		if (statOpt.isPresent()) {
//			final var stat = statOpt.get();
//			System.out.println("Mocked object stat info: ");
//			System.out.println("	serialized:   \"" + serializer.serialize(stat).trim() + "\"");
//			System.out.println("    context:      " + context);
//			System.out.println("    resource uri: " + context.eResource().getURI());
//		} else {
//			System.out.println("Mocked object info: ");
//			System.out.println("	serialized:   \"" + serializer.serialize(context).trim() + "\"");
//			System.out.println("    context:      " + context);
//			System.out.println("    resource uri: " + context.eResource().getURI());
//		}
//	}
	
	
	
	
}
