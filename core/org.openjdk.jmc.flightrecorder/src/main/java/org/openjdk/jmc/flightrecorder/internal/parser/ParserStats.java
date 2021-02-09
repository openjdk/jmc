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
