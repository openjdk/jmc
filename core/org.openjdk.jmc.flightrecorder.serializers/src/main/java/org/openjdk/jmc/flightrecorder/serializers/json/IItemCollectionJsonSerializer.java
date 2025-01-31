/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2025, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.flightrecorder.serializers.json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.function.BooleanSupplier;
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

	public static String toJsonString(IItemCollection items, BooleanSupplier stopFlag) {
		StringWriter sw = new StringWriter();
		IItemCollectionJsonSerializer marshaller = new IItemCollectionJsonSerializer(sw);
		try {
			marshaller.writeRecording(items, stopFlag);
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

	private void writeRecording(IItemCollection recording, BooleanSupplier stopFlag) throws IOException {
		writeObjectBegin();
		nextField(true, "events");
		writeArrayBegin();
		int count = 0;
		for (IItemIterable events : recording) {
			if (stopFlag.getAsBoolean()) {
				return;
			}
			for (IItem event : events) {
				if (stopFlag.getAsBoolean()) {
					return;
				}
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
