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
package org.openjdk.jmc.flightrecorder.internal.parser;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.common.collection.SimpleArray;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.StructContentType;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.internal.parser.ItemBuilder.IItemFactory;
import org.openjdk.jmc.flightrecorder.internal.util.DisjointBuilder;
import org.openjdk.jmc.flightrecorder.internal.util.DisjointBuilder.ArrayFactory;
import org.openjdk.jmc.flightrecorder.parser.IEventSink;
import org.openjdk.jmc.flightrecorder.parser.IEventSinkFactory;
import org.openjdk.jmc.flightrecorder.parser.ValueField;

/**
 * Bridge class from event sink to repository. Since this class does not pass on events to another
 * event sink, it must always be the last sink in a chain.
 */
class RepositoryBuilder implements IEventSinkFactory {
	private static final Logger LOGGER = Logger.getLogger(RepositoryBuilder.class.getName());
	private final Map<String, EventTypeEntry> eventTypes = new HashMap<>();

	@Override
	public IEventSink create(
		String identifier, String label, String[] category, String description, List<ValueField> dataStructure) {
		synchronized (eventTypes) {
			EventTypeEntry eventTypeEntry = eventTypes.get(identifier);
			if (eventTypeEntry == null) {
				eventTypeEntry = createEventTypeEntry(identifier, label, category, description, dataStructure);
				eventTypes.put(identifier, eventTypeEntry);
				return eventTypeEntry.createSink();
			} else {
				while (!eventTypeEntry.isCompatibleWith(dataStructure)) {
					if (eventTypeEntry.next == null) {
						eventTypeEntry.next = createEventTypeEntry(identifier + UUID.randomUUID().toString(), label,
								category, description, dataStructure);
						LOGGER.log(Level.WARNING, MessageFormat.format(
								"Created new event type entry for {0} because the fields did not match those of the previously created one. New identifier is {1}", //$NON-NLS-1$
								identifier, eventTypeEntry.next.eventType.getIdentifier()));
						return eventTypeEntry.next.createSink();
					}
					eventTypeEntry = eventTypeEntry.next;
				}
				return eventTypeEntry.createSink();
			}
		}
	}

	private static EventTypeEntry createEventTypeEntry(
		String identifier, String label, String[] category, String description, List<ValueField> dataStructure) {
		StructContentType<IItem> eventType = new StructContentType<>(identifier, label, description);
		IItemFactory itemFactory = ItemBuilder.createItemFactory(eventType, dataStructure);
		IMemberAccessor<IQuantity, IItem> stAccessor = JfrAttributes.START_TIME.getAccessor(eventType);
		IMemberAccessor<IQuantity, IItem> etAccessor = JfrAttributes.END_TIME.getAccessor(eventType);
		if (stAccessor != null && stAccessor != etAccessor) {
			return new DurationEventTypeEntry(eventType, category, itemFactory, dataStructure, stAccessor, etAccessor);
		} else {
			return new InstantEventTypeEntry(eventType, category, itemFactory, dataStructure, etAccessor);
		}
	}

	@Override
	public void flush() {

	}

	public Iterator<EventTypeEntry> getEventTypes() {
		synchronized (eventTypes) {
			final Iterator<EventTypeEntry> it = eventTypes.values().iterator();
			return new Iterator<EventTypeEntry>() {
				private EventTypeEntry rem;

				@Override
				public boolean hasNext() {
					return rem != null || it.hasNext();
				}

				@Override
				public EventTypeEntry next() {
					EventTypeEntry tmp = (rem == null) ? it.next() : rem;
					rem = tmp.next;
					return tmp;
				}

				@Override
				public final void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

	abstract static class EventTypeEntry {
		final StructContentType<IItem> eventType;
		final String[] category;
		final IItemFactory itemFactory;
		final List<ValueField> dataStructure;
		private EventTypeEntry next; // Linked list of event types with the same original identifier

		public EventTypeEntry(StructContentType<IItem> eventType, String[] category, IItemFactory itemFactory,
				List<ValueField> dataStructure) {
			this.eventType = eventType;
			this.category = category;
			this.itemFactory = itemFactory;
			this.dataStructure = dataStructure;
		}

		private boolean isCompatibleWith(List<ValueField> fields) {
			if (dataStructure.size() == fields.size()) {
				for (int i = 0; i < dataStructure.size(); i++) {
					ValueField vf1 = dataStructure.get(i);
					ValueField vf2 = fields.get(i);
					if (!vf1.getIdentifier().equals(vf2.getIdentifier())
							|| !vf1.getContentType().equals(vf2.getContentType())) {
						return false;
					}
				}
				return true;
			}
			return false;
		}

		abstract Collection<IItem[]> buildSortedArrays();

		abstract IEventSink createSink();
	}

	private static class DurationEventTypeEntry extends EventTypeEntry {

		private final List<DisjointBuilder<IItem>> eventsLanes = new ArrayList<>();
		private final IMemberAccessor<IQuantity, IItem> startAccessor;
		private final IMemberAccessor<IQuantity, IItem> endAccessor;

		public DurationEventTypeEntry(StructContentType<IItem> eventType, String[] category, IItemFactory itemFactory,
				List<ValueField> dataStructure, IMemberAccessor<IQuantity, IItem> startAccessor,
				IMemberAccessor<IQuantity, IItem> endAccessor) {
			super(eventType, category, itemFactory, dataStructure);
			this.startAccessor = startAccessor;
			this.endAccessor = endAccessor;
		}

		@Override
		synchronized Collection<IItem[]> buildSortedArrays() {
			return DisjointBuilder.toArrays(eventsLanes, ARRAY_FACTORY);
		}

		private synchronized DisjointBuilder<IItem> createLane() {
			DisjointBuilder<IItem> lane = new DisjointBuilder<>(startAccessor, endAccessor);
			eventsLanes.add(lane);
			return lane;
		}

		@Override
		public IEventSink createSink() {
			return new IEventSink() {

				private final DisjointBuilder<IItem> events = createLane();

				@Override
				public void addEvent(Object[] values) {
					events.add(itemFactory.createEvent(values));
				}

			};
		}
	}

	private static class InstantEventTypeEntry extends EventTypeEntry {

		private final List<SimpleArray<IItem>> eventsLanes = new ArrayList<>();
		private final IMemberAccessor<IQuantity, IItem> order;

		public InstantEventTypeEntry(StructContentType<IItem> eventType, String[] category, IItemFactory itemFactory,
				List<ValueField> dataStructure, IMemberAccessor<IQuantity, IItem> order) {
			super(eventType, category, itemFactory, dataStructure);
			this.order = order;
		}

		@Override
		synchronized Collection<IItem[]> buildSortedArrays() {
			int eventCount = 0;
			for (SimpleArray<IItem> a : eventsLanes) {
				eventCount += a.size();
			}
			if (eventCount == 0) {
				return Collections.emptyList();
			}
			IItem[] events = new IItem[eventCount];
			int offset = 0;
			for (SimpleArray<IItem> a : eventsLanes) {
				a.copyTo(events, offset);
				offset += a.size();
			}
			if (order != null) {
				Arrays.sort(events, new Comparator<IItem>() {

					@Override
					public int compare(IItem o1, IItem o2) {
						return order.getMember(o1).compareTo(order.getMember(o2));
					}
				});
			}
			return Arrays.asList(new IItem[][] {events});
		}

		private synchronized SimpleArray<IItem> createLane() {
			SimpleArray<IItem> lane = new SimpleArray<>(new IItem[3]);
			eventsLanes.add(lane);
			return lane;
		}

		@Override
		public IEventSink createSink() {
			return new IEventSink() {

				private final SimpleArray<IItem> events = createLane();

				@Override
				public void addEvent(Object[] values) {
					events.add(itemFactory.createEvent(values));
				}

			};
		}
	}

	private static final ArrayFactory<IItem> ARRAY_FACTORY = new ArrayFactory<IItem>() {

		@Override
		public IItem[] createArray(int size) {
			return new IItem[size];
		}
	};

}
