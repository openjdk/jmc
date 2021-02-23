/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Datadog, Inc. All rights reserved.
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.swing.text.html.MinimalHTMLWriter;

import org.openjdk.jmc.flightrecorder.IParserStats.IEventStats;

public class ParserStats {
	private short majorVersion;
	private short minorVersion;
	private final AtomicInteger chunkCount = new AtomicInteger();
	private final AtomicLong skippedEventCount = new AtomicLong();
	private final ConcurrentHashMap<String, EventTypeStats> statsByType = new ConcurrentHashMap<>();

	public void setVersion(short majorVersion, short minorVersion) {
		this.majorVersion = majorVersion;
		this.minorVersion = minorVersion;
	}

	public void incChunkCount() {
		chunkCount.incrementAndGet();
	}

	public void setSkippedEventCount(long skippedEventCount) {
		this.skippedEventCount.addAndGet(skippedEventCount);
	}

	public void updateEventStats(String eventTypeName, long size) {
		statsByType.compute(eventTypeName, (key, stats) -> {
			if (stats == null) {
				return new EventTypeStats(eventTypeName, size);
			}
			stats.add(size);
			return stats;
		});
	}

	public void forEachEventType(Consumer<IEventStats> consumer) {
		for (EventTypeStats eventStats : statsByType.values()) {
			consumer.accept(eventStats);
		}
	}

	public short getMajorVersion() {
		return majorVersion;
	}

	public short getMinorVersion() {
		return minorVersion;
	}

	public int getChunkCount() {
		return chunkCount.get();
	}

	public long getSkippedEventCount() {
		return skippedEventCount.get();
	}

	public long getCount(String eventTypeName) {
		EventTypeStats stats = statsByType.get(eventTypeName);
		if (stats == null) {
			return 0;
		}
		return stats.count;
	}

	public long getTotalSize(String eventTypeName) {
		EventTypeStats stats = statsByType.get(eventTypeName);
		if (stats == null) {
			return 0;
		}
		return stats.totalSize;
	}

	private static class EventTypeStats implements IEventStats {
		private final String eventTypeName;
		private long count;
		private long totalSize;

		public EventTypeStats(String eventTypeName, long size) {
			this.eventTypeName = eventTypeName;
			this.count = 1;
			this.totalSize = size;
		}

		public void add(long size) {
			count++;
			totalSize += size;
		}

		@Override
		public String getName() {
			return eventTypeName;
		}

		@Override
		public long getCount() {
			return count;
		}

		@Override
		public long getTotalSize() {
			return totalSize;
		}

		@Override
		public String toString() {
			return "EventTypeStats [eventTypeName=" + eventTypeName + ", count=" + count + ", totalSize=" + totalSize
					+ "]";
		}
	}
}
