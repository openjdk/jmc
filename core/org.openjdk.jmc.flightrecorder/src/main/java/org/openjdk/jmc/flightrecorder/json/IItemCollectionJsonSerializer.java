package org.openjdk.jmc.flightrecorder.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

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
 * <ol>
 * <li>Serialize JFR event collections (i.e. JMC {@link IItemCollection}) to JSON.</li>
 * <li>???</li>
 * <li>Profit!</li>
 * </ol>
 */
public class IItemCollectionJsonSerializer extends JsonWriter {
	public IItemCollectionJsonSerializer(Writer w) {
		super(w);
	}

	public void writeEventCollection(IItemCollection eventCollection) throws IOException {
		writeObjectBegin();
		nextField(true, "recording");
		writeObjectBegin();
		nextField(true, "events");
		writeArrayBegin();
		int count = 0;
		for (IItemIterable events : eventCollection) {
			for (IItem event : events) {
//				if (count > 100) {
//					break;
//				}
//				if (!event.getType().getIdentifier().equals("jdk.JavaMonitorWait")) {
//					continue;
//				}
				nextElement(count == 0);
				writeEvent(event);
				count++;
			}
		}
		writeArrayEnd();
		writeObjectEnd();
		writeObjectEnd();
		flush();
	}

	public void writeEvent(IItem event) {
		writeObjectBegin();
		IType<?> type = event.getType();
		writeField(true, "type", type.getIdentifier());
//            printValue(false, false, "startTime", e.getStartTime());
//            printValue(false, false, "duration", e.getDuration());
		nextField(false, "values");
		writeObjectBegin();
		writeValues(event);
		writeObjectEnd();
		writeObjectEnd();
	}

	private void writeTrace(boolean first, IMCStackTrace trace) {
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

	private void writeValues(IItem event) {
		IType<IItem> itemType = ItemToolkit.getItemType(event);
		boolean first = true;
		for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> e : itemType.getAccessorKeys().entrySet()) {
			IMemberAccessor<?, IItem> accessor = itemType.getAccessor(e.getKey());
			IAccessorKey<?> attribute = e.getKey();
			Object value = accessor.getMember(event);
			if (value instanceof IMCStackTrace) {
				writeTrace(first, (IMCStackTrace) value);
			} else {
				writeField(first, attribute.getIdentifier(), value);
			}
			first = false;
		}
	}
}
