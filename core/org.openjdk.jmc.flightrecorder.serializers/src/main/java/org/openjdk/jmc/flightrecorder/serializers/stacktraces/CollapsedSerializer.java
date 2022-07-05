package org.openjdk.jmc.flightrecorder.serializers.stacktraces;

import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a {@link StacktraceTreeModel} to a collapsed format that can be used
 * as input for the flamegraph perl script from https://github.com/brendangregg/FlameGraph
 */
public class CollapsedSerializer {

	/**
	 * Serializes a {@link StacktraceTreeModel} to collasped format.
	 *
	 * @param model
	 *            the {@link StacktraceTreeModel} to serialize to collapsed format.
	 * @return a String containing the serialized model.
	 */
	public static String toCollapsed(StacktraceTreeModel model) {
		StringBuilder sb = new StringBuilder();
		List<String> lines = new ArrayList<>();
		toCollapsed(sb, lines, model, model.getRoot());
		return String.join("\n", lines);
	}

	private static void toCollapsed(StringBuilder sb, List<String> lines, StacktraceTreeModel model, Node node) {
		if (!node.isRoot()) {
			appendFrame(sb, node.getFrame(), node.getCumulativeWeight());
		}
		if (node.getChildren().isEmpty()) {
			lines.add(sb.toString() + " " + (int) node.getCumulativeWeight());
			return;
		}
		for (Node child : node.getChildren()) {
			toCollapsed(new StringBuilder(sb), lines, model, child);
		}
	}

	private static void appendFrame(StringBuilder sb, AggregatableFrame frame, double value) {
		if (sb.length() > 0) {
			sb.append(";");
		}
		sb.append(frame.getHumanReadableShortString());
	}

	public static void main(String[] args) {

	}
}
