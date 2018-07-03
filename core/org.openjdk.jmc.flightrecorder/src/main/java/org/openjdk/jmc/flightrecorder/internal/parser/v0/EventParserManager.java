/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.internal.parser.v0;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openjdk.jmc.common.collection.FastAccessNumberMap;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.internal.EventAppearance;
import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;
import org.openjdk.jmc.flightrecorder.internal.parser.LoaderContext;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.DataType;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.EventTypeDescriptor;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.ProducerDescriptor;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.ValueDescriptor;
import org.openjdk.jmc.flightrecorder.internal.util.JfrInternalConstants;
import org.openjdk.jmc.flightrecorder.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.parser.IEventSink;
import org.openjdk.jmc.flightrecorder.parser.IEventSinkFactory;
import org.openjdk.jmc.flightrecorder.parser.ValueField;

class EventParserManager {
	// Event types
	static final int METADATA_EVENT_TYPE_INDEX = 0;
	static final int CHECK_POINT_EVENT_TYPE_INDEX = 1;
	static final int LOST_EVENT_TYPE_INDEX = 2;

	// Content types
	static final int CONTENT_TYPE_POOL_NONE = 0;
	static final int CONTENT_TYPE_MEMORY = 1;
	static final int CONTENT_TYPE_EPOCHMILLIS = 2;
	static final int CONTENT_TYPE_MILLIS = 3;
	static final int CONTENT_TYPE_NANOS = 4;
	static final int CONTENT_TYPE_TICKS = 5;
	static final int CONTENT_TYPE_ADDRESS = 6;
	static final int CONTENT_TYPE_THREADID = 7;
	static final int CONTENT_TYPE_JAVATHREADID = 8;
	static final int CONTENT_TYPE_STACKTRACEID = 9;
//	static final int CONTENT_TYPE_CLASSID = 10;
	static final int CONTENT_TYPE_PERCENTAGE = 11;
//	static final int CONTENT_TYPE_VMTHREAD = 30;
//	static final int CONTENT_TYPE_METHOD = 32;
//	static final int CONTENT_TYPE_GCWHEN = 38;

	private final FastAccessNumberMap<EventTypeEntry> eventTypes = new FastAccessNumberMap<>(100, 5);
	private final ReaderFactory readerFactory;
	private final LoaderContext context;

	public EventParserManager(ReaderFactory readerFactory, LoaderContext context, ProducerDescriptor ... producers)
			throws InvalidJfrFileException {
		this.readerFactory = readerFactory;
		this.context = context;

		// Create event types entries
		for (ProducerDescriptor pd : producers) {
			for (EventTypeDescriptor etd : pd.getEventTypeDescriptors()) {
				String path = etd.getPath();
				String id = pd.getURIString() + path;
				EventTypeBuilder eventSpec = new EventTypeBuilder(id, etd);
				String[] category = EventAppearance.getHumanSegmentArray(path);
				category = Arrays.copyOf(category, category.length - 1);
				IEventSink sink = context.getSinkFactory().create(id, etd.getLabel(), category, etd.getDescription(),
						eventSpec.getValueFields());
				eventTypes.put(etd.getIdentifier(),
						new EventTypeEntry(sink, etd.hasStartTime(), eventSpec.getValueReaders()));
			}
		}
		eventTypes.put(LOST_EVENT_TYPE_INDEX, createBufferLostEntry(context.getSinkFactory()));
	}

	void loadEvent(byte[] data, Offset offset, int eventTypeId) throws InvalidJfrFileException {
		EventTypeEntry ep = eventTypes.get(eventTypeId);
		if (ep == null) {
			throw new IllegalArgumentException("Event type " + eventTypeId + " is not described in the file"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		long endTime = readerFactory.readTicksTimestamp(data, offset);
		int valueIndex = 0;
		if (ep.hasStartTime) {
			ep.values[valueIndex++] = UnitLookup.EPOCH_NS.quantity(readerFactory.readTicksTimestamp(data, offset));
		}
		ep.values[valueIndex++] = UnitLookup.EPOCH_NS.quantity(endTime);
		for (int n = 0; n < ep.parsers.length; n++) {
			ep.values[valueIndex++] = ep.parsers[n].readValue(data, offset, endTime);
		}
		ep.sink.addEvent(ep.values);
	}

	private EventTypeEntry createBufferLostEntry(IEventSinkFactory esf) throws InvalidJfrFileException {
		EventTypeBuilder eventReader = new EventTypeBuilder();
		eventReader.addThreadField();
		eventReader.addMemoryDroppedField();
		IEventSink sink = esf.create(JfrInternalConstants.BUFFER_LOST_TYPE_ID,
				Messages.getString(Messages.EventParserManager_TYPE_BUFFER_LOST),
				EventAppearance.getHumanSegmentArray("recordings"), //$NON-NLS-1$
				Messages.getString(Messages.EventParserManager_TYPE_BUFFER_LOST_DESC), eventReader.getValueFields());
		return new EventTypeEntry(sink, false, eventReader.getValueReaders());
	}

	private class EventTypeBuilder {
		private final List<IValueReader> readers = new ArrayList<>();
		private final List<ValueField> valueFields = new ArrayList<>();

		EventTypeBuilder() {
			valueFields.add(new ValueField(JfrAttributes.END_TIME));
		}

		EventTypeBuilder(String typeId, EventTypeDescriptor etd) throws InvalidJfrFileException {
			if (etd.hasStartTime()) {
				valueFields.add(new ValueField(JfrAttributes.START_TIME));
			}
			valueFields.add(new ValueField(JfrAttributes.END_TIME));
			if (etd.hasThread()) {
				addThreadField();
			}
			if (etd.canHaveStacktrace()) {
				addStacktraceField();
			}
			for (ValueDescriptor vd : etd.getDataStructure()) {
				add(typeId, vd);
			}
		}

		void add(String typeId, ValueDescriptor vd) throws InvalidJfrFileException {
			if (vd.getDataType() == DataType.STRUCT) {
				for (ValueDescriptor child : vd.getChildren()) {
					add(typeId, child, vd.getIdentifier() + ":" + child.getIdentifier(), //$NON-NLS-1$
							vd.getName() + " : " + child.getName()); //$NON-NLS-1$
				}
			} else {
				add(typeId, vd, vd.getIdentifier(), vd.getName());
			}
		}

		private void add(String typeId, ValueDescriptor vd, String identifier, String name)
				throws InvalidJfrFileException {
			String valueType = context.getValueInterpretation(typeId, identifier);
			IValueReader r = readerFactory.createReader(vd, valueType);
			readers.add(r);
			valueFields.add(new ValueField(identifier, name, vd.getDescription(), r.getValueType()));
		}

		private void addThreadField() throws InvalidJfrFileException {
			readers.add(readerFactory.createConstantReader(DataType.U4, CONTENT_TYPE_THREADID));
			valueFields.add(new ValueField(JfrAttributes.EVENT_THREAD));
		}

		private void addStacktraceField() throws InvalidJfrFileException {
			readers.add(readerFactory.createConstantReader(DataType.U8, CONTENT_TYPE_STACKTRACEID));
			valueFields.add(new ValueField(JfrAttributes.EVENT_STACKTRACE));
		}

		private void addMemoryDroppedField() throws InvalidJfrFileException {
			readers.add(new QuantityReader(DataType.U4, UnitLookup.BYTE));
			valueFields.add(new ValueField(JfrAttributes.FLR_DATA_LOST));
		}

		IValueReader[] getValueReaders() {
			return readers.toArray(new IValueReader[readers.size()]);
		}

		List<ValueField> getValueFields() {
			return Collections.unmodifiableList(valueFields);
		}
	}

	private static class EventTypeEntry {

		private final Object[] values;
		private final IValueReader[] parsers;
		private final IEventSink sink;
		private final boolean hasStartTime;

		public EventTypeEntry(IEventSink sink, boolean hasStartTime, IValueReader[] valueParsers) {
			parsers = valueParsers;
			this.sink = sink;
			this.hasStartTime = hasStartTime;
			values = new Object[(hasStartTime ? 2 : 1) + parsers.length];
		}
	}

}
