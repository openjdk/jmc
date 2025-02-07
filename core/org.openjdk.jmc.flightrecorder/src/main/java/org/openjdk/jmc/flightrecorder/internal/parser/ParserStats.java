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
package org.openjdk.jmc.flightrecorder.internal.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.Consumer;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.collection.FastAccessNumberMap;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.ICanonicalAccessorFactory;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.MemberAccessorToolkit;
import org.openjdk.jmc.flightrecorder.IParserStats.IEventStats;
import org.openjdk.jmc.flightrecorder.parser.IConstantPoolExtension;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFormatToolkit;

public class ParserStats {
	private short majorVersion;
	private short minorVersion;
	private final AtomicInteger chunkCount = new AtomicInteger();
	private final AtomicLong skippedEventCount = new AtomicLong();
	private final ConcurrentHashMap<String, EventTypeStats> statsByType = new ConcurrentHashMap<>();
	private final ConcurrentLinkedDeque<ConstantPoolInfo> constantPoolInfoList = new ConcurrentLinkedDeque<>();
	private final ConcurrentHashMap<String, AtomicLong> entryPoolSizeByType = new ConcurrentHashMap<>();
	private IItemCollection poolStats;
	private IItemCollection constants;
	private final Map<String, IConstantPoolExtension> constantPoolExtensions = new ConcurrentHashMap<>();

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
		statsByType.computeIfAbsent(eventTypeName, EventTypeStats::new).add(size);
	}

	public void addConstantPool(long id, String name, FastAccessNumberMap<Object> constantPool) {
		constantPoolInfoList.add(new ConstantPoolInfo(id, name, constantPool));
	}

	public void addEntryPoolSize(String typeIdentifier, long size) {
		entryPoolSizeByType.computeIfAbsent(typeIdentifier, id -> new AtomicLong()).addAndGet(size);
	}

	public void addConstantPoolExtension(IConstantPoolExtension extension) {
		constantPoolExtensions.put(extension.getId(), extension);
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
		return stats.getCount();
	}

	public long getTotalSize(String eventTypeName) {
		EventTypeStats stats = statsByType.get(eventTypeName);
		if (stats == null) {
			return 0;
		}
		return stats.getTotalSize();
	}

	public IItemCollection getConstantPools() {
		if (poolStats == null) {
			Map<String, ConstPoolItem> poolStatsByName = new HashMap<>();
			for (ConstantPoolInfo info : constantPoolInfoList) {
				ConstPoolItem poolItem = poolStatsByName.computeIfAbsent(info.name, this::createPoolItem);
				poolItem.count += getConstantPoolCount(info.constantPool);
			}
			poolStats = ItemCollectionToolkit.build(poolStatsByName.values().stream());
		}
		return poolStats;
	}

	public IItemCollection getConstants() {
		if (constants == null) {
			List<ConstantItem> items = new ArrayList<>();
			for (ConstantPoolInfo info : constantPoolInfoList) {
				for (Object value : info.constantPool) {
					items.add(new ConstantItem(info.name, value));
				}
			}
			constants = ItemCollectionToolkit.build(items.stream());
		}
		return constants;
	}

	public Map<String, IConstantPoolExtension> getConstantPoolExtensions() {
		return constantPoolExtensions;
	}

	static class ConstPoolItem implements IItem, IType<IItem> {
		private final String name;
		private long count;
		private final long size;

		@Override
		public IType<?> getType() {
			return this;
		}

		public ConstPoolItem(String name, long count, long size) {
			this.name = name;
			this.count = count;
			this.size = size;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getDescription() {
			return "ConstantPoolStats";
		}

		@Override
		public List<IAttribute<?>> getAttributes() {
			return Collections.emptyList();
		}

		@Override
		public Map<IAccessorKey<?>, ? extends IDescribable> getAccessorKeys() {
			return null;
		}

		@Override
		public boolean hasAttribute(ICanonicalAccessorFactory<?> attribute) {
			return false;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <M> IMemberAccessor<M, IItem> getAccessor(IAccessorKey<M> attribute) {
			if ("name".equals(attribute.getIdentifier())) {
				return ((IMemberAccessor<M, IItem>) MemberAccessorToolkit.<IItem, String, String> constant(name));
			}
			if ("count".equals(attribute.getIdentifier())) {
				return (IMemberAccessor<M, IItem>) MemberAccessorToolkit.<IItem, Long, Long> constant(count);
			}
			if ("size".equals(attribute.getIdentifier())) {
				return (IMemberAccessor<M, IItem>) MemberAccessorToolkit
						.<IItem, IQuantity, IQuantity> constant(UnitLookup.BYTE.quantity(size));
			}
			return null;
		}

		@Override
		public String getIdentifier() {
			return "constPoolStatsType";
		}
	}

	static class ConstantItem implements IItem, IType<IItem> {
		private final String typeName;
		private final Object constant;

		public ConstantItem(String typeName, Object constant) {
			this.typeName = typeName;
			this.constant = constant;
		}

		@Override
		public String getName() {
			return typeName;
		}

		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public List<IAttribute<?>> getAttributes() {
			return Collections.emptyList();
		}

		@Override
		public Map<IAccessorKey<?>, ? extends IDescribable> getAccessorKeys() {
			return null;
		}

		@Override
		public boolean hasAttribute(ICanonicalAccessorFactory<?> attribute) {
			return false;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <M> IMemberAccessor<M, IItem> getAccessor(IAccessorKey<M> attribute) {
			if ("typeName".equals(attribute.getIdentifier())) {
				return ((IMemberAccessor<M, IItem>) MemberAccessorToolkit.<IItem, Object, Object> constant(typeName));
			}
			if ("constant".equals(attribute.getIdentifier())) {
				if (constant instanceof IMCStackTrace) {
					IMCStackTrace stackTrace = ((IMCStackTrace) constant);
					if (!stackTrace.getFrames().isEmpty()) {
						IMCFrame imcFrame = (stackTrace).getFrames().get(0);
						String str = StacktraceFormatToolkit.formatFrame(imcFrame,
								new FrameSeparator(FrameCategorization.METHOD, false));
						return ((IMemberAccessor<M, IItem>) MemberAccessorToolkit
								.<IItem, Object, Object> constant(str));
					}
				}
				return ((IMemberAccessor<M, IItem>) MemberAccessorToolkit.<IItem, Object, Object> constant(constant));
			}
			if ("stackTrace".equals(attribute.getIdentifier())) {
				if (constant instanceof IMCStackTrace) {
					return (IMemberAccessor<M, IItem>) MemberAccessorToolkit
							.<IItem, IMCStackTrace, IMCStackTrace> constant((IMCStackTrace) constant);
				}
			}
			return null;
		}

		@Override
		public String getIdentifier() {
			return "constantValueType";
		}

		@Override
		public IType<?> getType() {
			return this;
		}
	}

	private ConstPoolItem createPoolItem(String infoName) {
		AtomicLong totalSize = entryPoolSizeByType.get(infoName);
		long entrySize = totalSize != null ? totalSize.longValue() : 0;
		return new ConstPoolItem(infoName, 0, entrySize);
	}

	private long getConstantPoolCount(FastAccessNumberMap<Object> constantPool) {
		Iterator<Object> iterator = constantPool.iterator();
		int count = 0;
		while (iterator.hasNext()) {
			count++;
			iterator.next();
		}
		return count;
	}

	private static class EventTypeStats implements IEventStats {
		private final String eventTypeName;
		private static final AtomicLongFieldUpdater<EventTypeStats> COUNT_UPDATER = AtomicLongFieldUpdater
				.newUpdater(EventTypeStats.class, "count");
		private volatile long count;
		private static final AtomicLongFieldUpdater<EventTypeStats> TOTAL_SIZE_UPDATER = AtomicLongFieldUpdater
				.newUpdater(EventTypeStats.class, "totalSize");
		private volatile long totalSize;

		public EventTypeStats(String eventTypeName) {
			this.eventTypeName = eventTypeName;
		}

		public void add(long size) {
			COUNT_UPDATER.incrementAndGet(this);
			TOTAL_SIZE_UPDATER.addAndGet(this, size);
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

	private static class ConstantPoolInfo {
		@SuppressWarnings("unused")
		final long id;
		final String name;
		final FastAccessNumberMap<Object> constantPool;

		public ConstantPoolInfo(long id, String name, FastAccessNumberMap<Object> constantPool) {
			this.id = id;
			this.name = name;
			this.constantPool = constantPool;
		}
	}
}
