package org.openjdk.jmc.flightrecorder.json;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.FormatToolkit;

import com.alibaba.fastjson.JSONWriter;

/**
 * Converts IItemCollections of events to JSON.
 */
public class FastJsonIItemCollectionJsonMarshaller {

	public static String toJsonString(IItemCollection eventCollections) {
		StringWriter sw = new StringWriter();
		JSONWriter writer = new JSONWriter(sw);

		writer.startArray();
		for (IItemIterable events : eventCollections) {
			for (IItem event : events) {
				writer.writeObject(convertToMap(event));
			}
		}
		writer.endArray();
		try {
			writer.flush();
			sw.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return sw.toString();
	}

	private static String stringifyMethod(IMCMethod method) {
		if (method == null) {
			return null;
		}
		return formatPackage(method.getType().getPackage()) + "." + //$NON-NLS-1$
				method.getType().getTypeName() + "#" + //$NON-NLS-1$
				method.getMethodName() + method.getFormalDescriptor();
	}

	private static String formatPackage(IMCPackage mcPackage) {
		return FormatToolkit.getPackage(mcPackage);
	}

	private static Map<String, Object> convertToMap(IItem event) {
		Map<String, Object> eventMap = new HashMap<>();
		eventMap.put("type", event.getType().getIdentifier());
		List<Map<String, Object>> valuesList = new ArrayList<>();
		IType<IItem> itemType = ItemToolkit.getItemType(event);
		for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> e : itemType.getAccessorKeys().entrySet()) {
			IMemberAccessor<?, IItem> accessor = itemType.getAccessor(e.getKey());
			IAccessorKey<?> attribute = e.getKey();
			Object value = accessor.getMember(event);
			if (value instanceof IMCStackTrace) {
				IMCStackTrace stackTrace = (IMCStackTrace) value;
				List<Map<String, Object>> frames = new ArrayList<>();
				for (IMCFrame frame : stackTrace.getFrames()) {
					Map<String, Object> frameMap = new HashMap<>();
					frameMap.put("type", frame.getType().getName());
					frameMap.put("method", stringifyMethod(frame.getMethod()));
					frameMap.put("line", frame.getFrameLineNumber());
				}
				eventMap.put("frames", frames);
				continue;
			} else if (value instanceof IQuantity) {
				IQuantity quantity = (IQuantity) value;
				Map<String, Object> quantityMap = new HashMap<>();
				if (!quantity.getUnit().getIdentifier().equals("")) {
					quantityMap.put("unit", quantity.getUnit().getIdentifier());
				}
				quantityMap.put("value", quantity.numberValue());
				quantityMap.put("type", attribute.getIdentifier());
				valuesList.add(quantityMap);
			} else {
				Map<String, Object> simpleValueMap = new HashMap<>();
				simpleValueMap.put("type", attribute.getIdentifier());
				simpleValueMap.put("value", value != null ? value.toString() : null);
				valuesList.add(simpleValueMap);
			}
		}
		eventMap.put("values", valuesList);
		return eventMap;
	}

	// Using the fastjson JSONWriter to construct multiple event objects inside an array fails with:
	//
	// com.alibaba.fastjson.JSONException: illegal state : 1003
	//	at com.alibaba.fastjson.JSONWriter.beginStructure(JSONWriter.java:88)
	//  at com.alibaba.fastjson.JSONWriter.startObject(JSONWriter.java:32)
	//	at org.openjdk.jmc.flightrecorder.flameview.RunnableJsonConverter.marshal(RunnableJsonConverter.java:62)
	//
	// This is unexpected, since we are creating the array context correctly, writing a single object works and
	// writing multiple objects with writeObject also works.
	//
	// Because of this we need to convert the event to a map, which is then serialised nicely into a JSON object.

//	private static void marshal(IItem event, JSONWriter writer) {
//		writer.startObject();
//		writer.writeKey("type");
//		writer.writeValue(event.getType().getIdentifier());
//		writer.writeKey("values");
//		writer.startObject();
//		IType<IItem> itemType = ItemToolkit.getItemType(event);
//		for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> e : itemType.getAccessorKeys().entrySet()) {
//			IMemberAccessor<?, IItem> accessor = itemType.getAccessor(e.getKey());
//			IAccessorKey<?> attribute = e.getKey();
//			Object value = accessor.getMember(event);
//			if (value instanceof IMCStackTrace) {
//				marshalStackTrace((IMCStackTrace) value, writer);
//			} else {
//				writer.writeKey(attribute.getIdentifier());
//				writer.writeValue(value.toString());
//			}
//		}
//		writer.endObject();
//	}
//
//	private static void marshalStackTrace(IMCStackTrace stackTrace, JSONWriter writer) {
//		writer.writeKey("frames");
//		writer.startArray();
//		for (IMCFrame frame : stackTrace.getFrames()) {
//			marshalFrame(frame, writer);
//		}
//		writer.endArray();
//
//	}
//
//	private static void marshalFrame(IMCFrame frame, JSONWriter writer) {
//		writer.startObject();
//		writer.writeKey("method");
//		writer.writeValue(stringifyMethod(frame.getMethod()));
//
//		writer.writeKey("line");
//		writer.writeValue(frame.getFrameLineNumber());
//
//		writer.writeKey("type");
//		writer.writeValue(frame.getType());
//		writer.endObject();
//	}
}
