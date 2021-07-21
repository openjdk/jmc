package org.openjdk.jmc.flightrecorder.serializers.json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemToolkit;

/**
 * Utility methods to convert an IItemCollection to a JSON object containing the serialised array of
 * events.
 */
public class IItemCollectionJsonSerializer extends JsonWriter {
	private final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.flightrecorder.json");

	public static String toJsonString(IItemCollection items) {
		StringWriter sw = new StringWriter();
		IItemCollectionJsonSerializer marshaller = new IItemCollectionJsonSerializer(sw);
		try {
			marshaller.writeRecording(items);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to serialize recording to JSON", e);
		}
		return sw.getBuffer().toString();
	}

	public static String toJsonString(Iterable<IItem> items) {
		StringWriter sw = new StringWriter();
		IItemCollectionJsonSerializer marshaller = new IItemCollectionJsonSerializer(sw);
		try {
			marshaller.writeEvents(items);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to serialize items to JSON", e);
		}
		return sw.getBuffer().toString();
	}

	private IItemCollectionJsonSerializer(Writer w) {
		super(w);
	}

	private void writeRecording(IItemCollection recording) throws IOException {
		writeObjectBegin();
		nextField(true, "events");
		writeArrayBegin();
		int count = 0;
		for (IItemIterable events : recording) {
			for (IItem event : events) {
				nextElement(count == 0);
				writeEvent(event);
				count++;
			}
		}
		writeArrayEnd();
		writeObjectEnd();
		flush();
	}

	void writeEvents(Iterable<IItem> events) throws IOException {
		writeObjectBegin();
		nextField(true, "events");
		writeArrayBegin();
		int count = 0;
		for (IItem event : events) {
			nextElement(count == 0);
			writeEvent(event);
			count++;
		}
		writeArrayEnd();
		writeObjectEnd();
		flush();
	}

	private void writeEvent(IItem event) {
		writeObjectBegin();
		IType<?> type = event.getType();
		writeField(true, "eventType", type.getIdentifier());
		nextField(false, "attributes");
		writeObjectBegin();
		writeEventAttributes(event);
		writeObjectEnd();
		writeObjectEnd();
	}

	private void writeStackTrace(boolean first, IMCStackTrace trace) {
		nextField(first, "stackTrace");
		writeObjectBegin();
		nextField(true, "frames");
		writeArrayBegin();
		boolean firstFrame = true;
		for (IMCFrame frame : trace.getFrames()) {
			nextElement(firstFrame);
			writeFrame(frame); //$NON-NLS-1$
			firstFrame = false;
		}
		writeArrayEnd();
		writeObjectEnd();
	}

	private void writeFrame(IMCFrame frame) {
		Integer lineNumber = frame.getFrameLineNumber();
		IMCMethod method = frame.getMethod();

		writeObjectBegin();
		writeField(true, "name", method != null ? stringifyMethod(method) : null);
		writeField(false, "line", lineNumber);
		writeField(false, "type", frame.getType());
		writeObjectEnd();
	}

	private void writeEventAttributes(IItem event) {
		IType<IItem> itemType = ItemToolkit.getItemType(event);
		boolean first = true;
		for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> e : itemType.getAccessorKeys().entrySet()) {
			IMemberAccessor<?, IItem> accessor = itemType.getAccessor(e.getKey());
			IAccessorKey<?> attribute = e.getKey();
			Object value = accessor.getMember(event);
			if (value instanceof IMCStackTrace) {
				writeStackTrace(first, (IMCStackTrace) value);
			} else {
				writeField(first, attribute.getIdentifier(), value);
			}
			first = false;
		}
	}
}
