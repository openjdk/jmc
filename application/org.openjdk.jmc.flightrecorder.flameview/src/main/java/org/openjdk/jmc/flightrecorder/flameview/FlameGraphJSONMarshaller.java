package org.openjdk.jmc.flightrecorder.flameview;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.flameview.tree.TraceTreeUtils;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

public class FlameGraphJSONMarshaller {

	public static String toJSON(StacktraceTreeModel model) {
		return toJSON(model, model.getRoot());
	}

	private static String toJSON(StacktraceTreeModel model, Node node) {
		Map<Integer, Set<Integer>> childrenLookup = model.getChildrenLookup();
		Map<Integer, Node> nodes = model.getNodes();

		StringBuilder sb = new StringBuilder();
		sb.append("{");

		if (node == model.getRoot()) {
			AtomicInteger totalEvents = new AtomicInteger(0);
			Map<String, Integer> eventCountsByType = TraceTreeUtils.eventTypeNameWithCountSorted(model.getItems(),
					totalEvents);
			String selectionText = TraceTreeUtils.createSelectionText(totalEvents.get(), eventCountsByType.size());
			StringBuilder rootTitle = new StringBuilder(selectionText);
			StringBuilder rootDescription = new StringBuilder();

			TraceTreeUtils.createNodeTitleAndDescription(rootTitle, rootDescription, eventCountsByType);
			sb.append(JSONProps(rootTitle.toString(), rootDescription.toString(), 0));
		} else {
			sb.append(JSONProps(node.getFrame(), node.getCumulativeWeight()));
		}

		Set<Integer> childIds = childrenLookup.get(node != null ? node.getNodeId() : null);

		sb.append(", ").append(addQuotes("c"));
		sb.append(": [ ");
		boolean first = true;
		// we sort the nodes so that the output is deterministic
		// TODO: remove once we validate the output
		List<Integer> sortedIds = new ArrayList<>();
		sortedIds.addAll(childIds);
		sortedIds.sort(Comparator.comparing((id) -> {
			return nodes.get(id).getFrame().getHumanReadableShortString();
		}));
		for (int childId : sortedIds) {
			if (!first) {
				sb.append(",");
			}
			sb.append(toJSON(model, nodes.get(childId)));
			first = false;
		}
		sb.append("]");
		sb.append("}");
		return sb.toString();
	}

	private static String JSONProps(AggregatableFrame frame, double value) {
		StringBuilder sb = new StringBuilder();
		sb.append(addQuotes("n")).append(": ").append(addQuotes(frame.getHumanReadableShortString()));
		sb.append(",");
		sb.append(addQuotes("p")).append(": ")
				.append(addQuotes(FormatToolkit.getPackage(frame.getMethod().getType().getPackage())));
		sb.append(",");
		sb.append(addQuotes("v")).append(": ").append(addQuotes(String.valueOf((int) value)));
		return sb.toString();
	}

//	private static String JSONProps(String frameName, double value) {
//		StringBuilder sb = new StringBuilder();
//		sb.append(addQuotes("n")).append(": ").append(addQuotes(frameName));
//		sb.append(", ");
//		sb.append(addQuotes("v")).append(": ").append(addQuotes(String.valueOf((int) value)));
//		return sb.toString();
//	}

	private static String JSONProps(String frameName, String description, double value) {
		StringBuilder sb = new StringBuilder();
		sb.append(addQuotes("n")).append(": ").append(addQuotes(frameName));
		sb.append(",");
		sb.append(addQuotes("p")).append(": ").append(addQuotes(""));
		sb.append(",");
		sb.append(addQuotes("d")).append(": ").append(addQuotes(description));
//		sb.append(",");
//		sb.append(addQuotes("v")).append(": ").append(addQuotes(String.valueOf((int) value)));
		return sb.toString();
	}

	private static String addQuotes(String str) {
		return String.format("\"%s\"", str);
	}

}
