package org.openjdk.jmc.flightrecorder.flameview;

import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_HTML_MORE;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_HTML_TABLE_EVENT_PATTERN;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_HTML_TABLE_EVENT_REST_PATTERN;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_ROOT_NODE;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_ROOT_NODE_EVENT;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_ROOT_NODE_EVENTS;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_ROOT_NODE_TYPE;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_ROOT_NODE_TYPES;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_TITLE_EVENT_MORE_DELIMITER;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_TITLE_EVENT_PATTERN;
import static org.openjdk.jmc.flightrecorder.flameview.MessagesUtils.getFlameviewMessage;
import static org.openjdk.jmc.flightrecorder.flameview.MessagesUtils.getStacktraceMessage;
import static org.openjdk.jmc.flightrecorder.stacktrace.Messages.STACKTRACE_UNCLASSIFIABLE_FRAME;
import static org.openjdk.jmc.flightrecorder.stacktrace.Messages.STACKTRACE_UNCLASSIFIABLE_FRAME_DESC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

public class FlameGraphJSONMarshaller {

	private static final String UNCLASSIFIABLE_FRAME = getStacktraceMessage(STACKTRACE_UNCLASSIFIABLE_FRAME);
	private static final String UNCLASSIFIABLE_FRAME_DESC = getStacktraceMessage(STACKTRACE_UNCLASSIFIABLE_FRAME_DESC);
	private final static int MAX_TYPES_IN_ROOT_TITLE = 2;
	private final static int MAX_TYPES_IN_ROOT_DESCRIPTION = 10;

	public static String toJSON(StacktraceTreeModel model) {
		return toJSON(model, model.getRoot());
	}

	private static String toJSON(StacktraceTreeModel model, Node node) {
		Map<Integer, Set<Integer>> childrenLookup = model.getChildrenLookup();
		Map<Integer, Node> nodes = model.getNodes();

		StringBuilder sb = new StringBuilder();
		sb.append("{");
		if (node.equals(model.getRoot())) {
			sb.append(createRootNodeJSON(model.getItems()));
		} else {
			sb.append(JSONProps(node.getFrame(), node.getCumulativeWeight()));
		}

		Set<Integer> childIds = childrenLookup.get(node.getNodeId());
		sb.append(", ").append(addQuotes("c")).append(": [ ");
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
		sb.append("]").append("}");
		return sb.toString();
	}

	private static String JSONProps(AggregatableFrame frame, double value) {
		StringBuilder sb = new StringBuilder();
		if (frame.getType().equals(IMCFrame.Type.UNKNOWN)) {
			// TODO: this is untested
			sb.append(addQuotes("n")).append(": ").append(addQuotes(UNCLASSIFIABLE_FRAME));
			sb.append(",");
			sb.append(addQuotes("d")).append(": ").append(addQuotes(UNCLASSIFIABLE_FRAME_DESC));
			sb.append(",");
			sb.append(addQuotes("p")).append(": ").append(addQuotes(""));
			sb.append(",");
		} else {
			sb.append(addQuotes("n")).append(": ").append(addQuotes(frame.getHumanReadableShortString()));
			sb.append(",");
			sb.append(addQuotes("p")).append(": ")
					.append(addQuotes(FormatToolkit.getPackage(frame.getMethod().getType().getPackage())));
			sb.append(",");
		}
		sb.append(addQuotes("v")).append(": ").append(addQuotes(String.valueOf((int) value)));
		return sb.toString();
	}

	private static String JSONProps(String frameName, String description) {
		StringBuilder sb = new StringBuilder();
		sb.append(addQuotes("n")).append(": ").append(addQuotes(frameName));
		sb.append(",");
		sb.append(addQuotes("p")).append(": ").append(addQuotes(""));
		sb.append(",");
		sb.append(addQuotes("d")).append(": ").append(addQuotes(description));
		return sb.toString();
	}

	private static String addQuotes(String str) {
		return String.format("\"%s\"", str);
	}

	public static Map<String, Long> countEventsByType(IItemCollection items) {
		final HashMap<String, Long> eventCountByType = new HashMap<>();
		for (IItemIterable eventIterable : items) {
			if (eventIterable.getItemCount() == 0) {
				continue;
			}
			String typeName = eventIterable.getType().getName();
			long newValue = eventCountByType.getOrDefault(typeName, 0L) + eventIterable.getItemCount();
			eventCountByType.put(typeName, newValue);
		}
		// sort the map in ascending order of values
		return eventCountByType.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	private static String createRootNodeJSON(IItemCollection events) {
		Map<String, Long> eventCountsByType = countEventsByType(events);
		String rootTitle = createRootNodeTitle(eventCountsByType);
		String rootDescription = createRootNodeDescription(eventCountsByType);
		return JSONProps(rootTitle, rootDescription);
	}

	private static String createRootNodeTitle(Map<String, Long> eventCountsByType) {
		int eventsInTitle = Math.min(eventCountsByType.size(), MAX_TYPES_IN_ROOT_TITLE);
		long totalEvents = eventCountsByType.values().stream().mapToLong(Long::longValue).sum();
		StringBuilder title = new StringBuilder(createRootNodeTitlePrefix(totalEvents, eventCountsByType.size()));
		int i = 0;
		for (Map.Entry<String, Long> entry : eventCountsByType.entrySet()) {
			String eventType = getFlameviewMessage(FLAMEVIEW_SELECT_TITLE_EVENT_PATTERN, entry.getKey(),
					String.valueOf(entry.getValue()));
			title.append(eventType);
			if (i < eventsInTitle) {
				title.append(getFlameviewMessage(FLAMEVIEW_SELECT_TITLE_EVENT_MORE_DELIMITER));
			} else {
				break;
			}
			i++;
		}
		if (eventCountsByType.size() > MAX_TYPES_IN_ROOT_TITLE) {
			title.append(getFlameviewMessage(FLAMEVIEW_SELECT_HTML_MORE)); // $NON-NLS-1$
		}
		return title.toString();
	}

	private static String createRootNodeTitlePrefix(long events, int types) {
		String eventText = getFlameviewMessage(
				events > 1 ? FLAMEVIEW_SELECT_ROOT_NODE_EVENTS : FLAMEVIEW_SELECT_ROOT_NODE_EVENT);
		String typeText = getFlameviewMessage(
				types > 1 ? FLAMEVIEW_SELECT_ROOT_NODE_TYPES : FLAMEVIEW_SELECT_ROOT_NODE_TYPE);
		return getFlameviewMessage(FLAMEVIEW_SELECT_ROOT_NODE, String.valueOf(events), eventText, String.valueOf(types),
				typeText);
	}

	private static String createRootNodeDescription(Map<String, Long> eventCountsByType) {
		StringBuilder description = new StringBuilder();
		int i = 0;
		long remainingEvents = 0;
		for (Map.Entry<String, Long> entry : eventCountsByType.entrySet()) {
			if (i < MAX_TYPES_IN_ROOT_DESCRIPTION) {
				description.append(getFlameviewMessage(FLAMEVIEW_SELECT_HTML_TABLE_EVENT_PATTERN,
						String.valueOf(entry.getValue()), entry.getKey()));
			} else {
				remainingEvents = Long.sum(remainingEvents, entry.getValue());
			}
			i++;
		}

		if (remainingEvents > 0) {
			int remainingTypes = eventCountsByType.size() - MAX_TYPES_IN_ROOT_DESCRIPTION;
			description.append(getFlameviewMessage(FLAMEVIEW_SELECT_HTML_TABLE_EVENT_REST_PATTERN,
					String.valueOf(remainingEvents), String.valueOf(remainingTypes)));
		}
		return description.toString();
	}
}
