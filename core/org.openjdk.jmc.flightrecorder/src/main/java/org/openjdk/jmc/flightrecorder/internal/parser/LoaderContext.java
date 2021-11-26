/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openjdk.jmc.common.collection.FastAccessNumberMap;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.internal.EventArray;
import org.openjdk.jmc.flightrecorder.internal.EventArrays;
import org.openjdk.jmc.flightrecorder.internal.parser.RepositoryBuilder.EventTypeEntry;
import org.openjdk.jmc.flightrecorder.internal.util.CanonicalConstantMap;
import org.openjdk.jmc.flightrecorder.parser.IConstantPoolExtension;
import org.openjdk.jmc.flightrecorder.parser.IEventSinkFactory;
import org.openjdk.jmc.flightrecorder.parser.IParserExtension;

/**
 * The heart of the loading process. Manages a pool of reusable byte buffers to load chunks in.
 * Collects all loaded chunks and combines them to a FlightRecording.
 */
public class LoaderContext {
	private final RepositoryBuilder repositoryBuilder = new RepositoryBuilder();
	private final IEventSinkFactory sinkFactory;
	private final ConcurrentHashMap<Object, CanonicalConstantMap<Object>> constantsByType = new ConcurrentHashMap<>();
	private final boolean hideExperimentals;
	private final List<? extends IParserExtension> extensions;
	private final List<IConstantPoolExtension> constPoolExtensions = new CopyOnWriteArrayList<>();
	private final Set<IRange<IQuantity>> chunkRanges;
	private final ParserStats parserStats = new ParserStats();

	public LoaderContext(List<? extends IParserExtension> extensions, boolean hideExperimentals) {
		this.extensions = extensions;
		this.hideExperimentals = hideExperimentals;
		IEventSinkFactory sinkFactory = repositoryBuilder;
		// Traverse the list in reverse order so that the first element will create outermost sink factory
		for (int i = extensions.size() - 1; i >= 0; i--) {
			sinkFactory = extensions.get(i).getEventSinkFactory(sinkFactory);
		}
		for (IParserExtension extension : extensions) {
			IConstantPoolExtension constantPoolExtension = extension.createConstantPoolExtension();
			if (constantPoolExtension != null) {
				constPoolExtensions.add(constantPoolExtension);
			}
		}
		this.sinkFactory = sinkFactory;
		this.chunkRanges = new HashSet<>();
	}

	public CanonicalConstantMap<Object> getConstantPool(Object poolKey) {
		CanonicalConstantMap<Object> newMap = new CanonicalConstantMap<>();
		CanonicalConstantMap<Object> existing = constantsByType.putIfAbsent(poolKey, newMap);
		return existing == null ? newMap : existing;
	}

	public boolean hideExperimentals() {
		return hideExperimentals;
	}

	public String getValueInterpretation(String eventTypeId, String fieldId) {
		for (IParserExtension m : extensions) {
			String vi = m.getValueInterpretation(eventTypeId, fieldId);
			if (vi != null) {
				return vi;
			}
		}
		return null;
	}

	public Object constantRead(long constantIndex, Object constant, String eventTypeId) {
		Object newConstant = constant;
		for (IConstantPoolExtension m : constPoolExtensions) {
			newConstant = m.constantRead(constantIndex, newConstant, eventTypeId);
		}
		return newConstant;
	}

	public Object constantReferenced(Object constant, String poolName, String eventTypeId) {
		Object newConstant = constant;
		for (IConstantPoolExtension m : constPoolExtensions) {
			newConstant = m.constantReferenced(newConstant, poolName, eventTypeId);
		}
		return newConstant;
	}

	public Object constantResolved(Object constant, String poolName, String eventTypeId) {
		Object newConstant = constant;
		for (IConstantPoolExtension m : constPoolExtensions) {
			newConstant = m.constantResolved(newConstant, poolName, eventTypeId);
		}
		return newConstant;
	}

	public void allConstantPoolsResolved(Map<String, FastAccessNumberMap<Object>> constantPools) {
		for (IConstantPoolExtension m : constPoolExtensions) {
			m.allConstantPoolsResolved(constantPools);
		}
	}

	public IEventSinkFactory getSinkFactory() {
		return sinkFactory;
	}

	public void addChunkRange(IRange<IQuantity> chunkRange) {
		this.chunkRanges.add(chunkRange);
	}

	@SuppressWarnings("deprecation")
	public EventArrays buildEventArrays() throws CouldNotLoadRecordingException {
		sinkFactory.flush();
		Iterator<EventTypeEntry> eventTypes = repositoryBuilder.getEventTypes();
		ArrayList<EventArray> eventArrays = new ArrayList<>();
		while (eventTypes.hasNext()) {
			EventTypeEntry ete = eventTypes.next();
			ete.eventType.addExtraAttribute(0, JfrAttributes.EVENT_TYPE);
			List<IAttribute<?>> attributes = ete.eventType.getAttributes();
			if (attributes.contains(JfrAttributes.START_TIME)) {
				int endTimeIndex = attributes.indexOf(JfrAttributes.END_TIME);
				int durationIndex = attributes.indexOf(JfrAttributes.DURATION);
				if (endTimeIndex >= 0 && durationIndex < 0) {
					ete.eventType.addExtraAttribute(endTimeIndex, JfrAttributes.DURATION); // for pre-JDK9 recordings
				} else if (durationIndex >= 0 && endTimeIndex < 0) {
					ete.eventType.addExtraAttribute(durationIndex + 1, JfrAttributes.END_TIME); // for JDK9 recordings
				}
			}
			Collection<IItem[]> sortedArrays = ete.buildSortedArrays();
			if (sortedArrays.isEmpty()) {
				// include all event types, even if there are no events
				eventArrays.add(new EventArray(new IItem[] {}, ete.eventType, ete.category));
			} else {
				for (IItem[] ea : sortedArrays) {
					eventArrays.add(new EventArray(ea, ete.eventType, ete.category));
				}
			}

		}
		return new EventArrays(eventArrays.toArray(new EventArray[eventArrays.size()]), chunkRanges, parserStats);
	}

	public void incChunkCount() {
		parserStats.incChunkCount();
	}

	public void updateEventStats(String eventTypeName, long size) {
		parserStats.updateEventStats(eventTypeName, size);
	}

	public void addTypeConstantPool(long id, String name, FastAccessNumberMap<Object> constantPool) {
		parserStats.addConstantPool(id, name, constantPool);
	}

	public ParserStats getParserStats() {
		return parserStats;
	}

	public void setVersion(short majorVersion, short minorVersion) {
		parserStats.setVersion(majorVersion, minorVersion);
	}

	public void setSkippedEventCount(long skippedEventCount) {
		parserStats.setSkippedEventCount(skippedEventCount);
	}

	public void addEntryPoolSize(String typeIdentifier, long size) {
		parserStats.addEntryPoolSize(typeIdentifier, size);
	}

	public void addConstantPoolExtensions() {
		for (IConstantPoolExtension ext : constPoolExtensions) {
			ext.eventsLoaded();
			parserStats.addConstantPoolExtension(ext);
		}
	}
}
