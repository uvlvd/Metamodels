package org.xtext.lua.evaluation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.serializer.ISerializer;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.utils.FeatureUtil;
import org.xtext.lua.utils.MockUtil;
import org.xtext.lua.utils.StatUtil;

/**
 * Can be used for debugging, prints information about statements for mocked/synthetic elements.
 * @author jsaenz
 *
 */
public class MockInfoCollector {
	
	private Map<MockInfo.Cause, List<MockInfo>> infoByCause = new EnumMap<>(MockInfo.Cause.class);
	
	public MockInfoCollector() {
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
	
	public Map<MockInfo.Cause, List<SerializableMockInfo>> getStatementMockInfosByCause(ISerializer serializer) {
		Map<MockInfo.Cause, List<SerializableMockInfo>> result = new EnumMap<>(MockInfo.Cause.class);
		Stream.of(MockInfo.Cause.values()).forEach(cause -> result.put(cause, new ArrayList<>()));
		getInfoByCause().forEach((cause, infoList) -> {
			infoList.forEach(info -> {
				var statementMockInfo = new SerializableMockInfo(createStatementCodeString(info, serializer), info);
				result.get(cause).add(statementMockInfo);
			});
		});
		return result;
	}
	
	private String createStatementCodeString(MockInfo info, ISerializer serializer) {
		var stat = info.getParentStat();
		if (stat != null) {
			return serializer.serialize(stat).trim();
		}
		return null;
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
	
	
	
}
