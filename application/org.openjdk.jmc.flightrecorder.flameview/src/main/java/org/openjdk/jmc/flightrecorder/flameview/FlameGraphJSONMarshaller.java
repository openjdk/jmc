package org.openjdk.jmc.flightrecorder.flameview;

import java.util.Map;
import java.util.Set;

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

		// TODO: remove this special case, the root node should be identical to other
		// nodes
		if (node == model.getRoot()) {
			sb.append(JSONProps("root", 0));
		} else {
			sb.append(JSONProps(node.getFrame().getHumanReadableShortString(), node.getWeight()));

		}

		Set<Integer> childIds = childrenLookup.get(node != null ? node.getNodeId() : null);
		if (childIds.size() > 0) {
			sb.append(", ").append(addQuotes("children"));
			sb.append(": [");
			boolean first = true;
			// since we're iterating on a set, the order is not guaranteed to be
			// deterministic
			for (int childId : childIds) {
				if (!first) {
					sb.append(", ");
				}
				sb.append(toJSON(model, nodes.get(childId)));
				first = false;
			}
			sb.append("]");
		}
		sb.append("}");
		return sb.toString();
	}

	private static String JSONProps(String name, double value) {
		StringBuilder sb = new StringBuilder();
		sb.append(addQuotes("name")).append(": ").append(addQuotes(name));
		sb.append(", ");
		sb.append(addQuotes("value")).append(": ").append(value);
		return sb.toString();
	}

	private static String addQuotes(String str) {
		return String.format("\"%s\"", str);
	}
}
