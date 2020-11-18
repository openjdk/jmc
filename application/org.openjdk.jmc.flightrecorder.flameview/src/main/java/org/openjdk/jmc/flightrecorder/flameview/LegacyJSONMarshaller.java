package org.openjdk.jmc.flightrecorder.flameview;

import static org.openjdk.jmc.flightrecorder.flameview.MessagesUtils.getStacktraceMessage;
import static org.openjdk.jmc.flightrecorder.stacktrace.Messages.STACKTRACE_UNCLASSIFIABLE_FRAME;
import static org.openjdk.jmc.flightrecorder.stacktrace.Messages.STACKTRACE_UNCLASSIFIABLE_FRAME_DESC;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.flameview.tree.TraceNode;
import org.openjdk.jmc.flightrecorder.flameview.tree.TraceTreeUtils;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel;

public class LegacyJSONMarshaller {
	public static String toLegacyJSON(StacktraceModel statefulModel, IItemCollection recording) {
		TraceNode root = TraceTreeUtils.createRootWithDescription(recording,
				statefulModel.getRootFork().getBranchCount());
		TraceNode traceNode = TraceTreeUtils.createTree(root, statefulModel);
		return toLegacyJSON(traceNode);
	}

	private static final String UNCLASSIFIABLE_FRAME = getStacktraceMessage(STACKTRACE_UNCLASSIFIABLE_FRAME);
	private static final String UNCLASSIFIABLE_FRAME_DESC = getStacktraceMessage(STACKTRACE_UNCLASSIFIABLE_FRAME_DESC);

	private static String toLegacyJSON(TraceNode root) {
		StringBuilder builder = new StringBuilder();
		String rootNodeStart = createJsonRootTraceNode(root);
		builder.append(rootNodeStart);
		renderChildren(builder, root);
		builder.append("]}");
		return builder.toString();
	}

	private static void render(StringBuilder builder, TraceNode node) {
		String start = UNCLASSIFIABLE_FRAME.equals(node.getName()) ? createJsonDescTraceNode(node)
				: createJsonTraceNode(node);
		builder.append(start);
		renderChildren(builder, node);
		builder.append("]}");
	}

	private static void renderChildren(StringBuilder builder, TraceNode node) {
		int i = 0;
		while (i < node.getChildren().size()) {
			render(builder, node.getChildren().get(i));
			if (i < node.getChildren().size() - 1) {
				builder.append(",");
			}
			i++;
		}
	}

	private static String createJsonRootTraceNode(TraceNode rootNode) {
		return String.format("{%s,%s,%s, \"c\": [ ", toJSonKeyValue("n", rootNode.getName()), toJSonKeyValue("p", ""),
				toJSonKeyValue("d", rootNode.getPackageName()));
	}

	private static String createJsonTraceNode(TraceNode node) {
		return String.format("{%s,%s,%s, \"c\": [ ", toJSonKeyValue("n", node.getName()),
				toJSonKeyValue("p", node.getPackageName()), toJSonKeyValue("v", String.valueOf(node.getValue())));
	}

	private static String createJsonDescTraceNode(TraceNode node) {
		return String.format("{%s,%s,%s,%s, \"c\": [ ", toJSonKeyValue("n", node.getName()),
				toJSonKeyValue("p", node.getPackageName()), toJSonKeyValue("d", UNCLASSIFIABLE_FRAME_DESC),
				toJSonKeyValue("v", String.valueOf(node.getValue())));
	}

	private static String toJSonKeyValue(String key, String value) {
		return "\"" + key + "\": " + "\"" + value + "\"";
	}
}
