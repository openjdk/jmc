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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.openjdk.jmc.common.IDescribable;
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

public class ParserStats {
	private short majorVersion;
	private short minorVersion;
	private final AtomicInteger chunkCount = new AtomicInteger();
	private final AtomicLong skippedEventCount = new AtomicLong();
	private final ConcurrentHashMap<String, EventTypeStats> statsByType = new ConcurrentHashMap<>();
	private final ConcurrentLinkedDeque<ConstantPoolInfo> constantPoolInfoList = new ConcurrentLinkedDeque<ConstantPoolInfo>();
	private final ConcurrentHashMap<String, Long> entryPoolSizeByType = new ConcurrentHashMap<>();
	private IItemCollection poolStats;
	private IItemCollection constants;

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

	public void addConstantPool(long id, String name, FastAccessNumberMap<Object> constantPool) {
		constantPoolInfoList.add(new ConstantPoolInfo(id, name, constantPool));
	}

	public void addEntryPoolSize(String typeIdentifier, long size) {
		entryPoolSizeByType.compute(typeIdentifier, (key, value) -> {
			if (value == null) {
				return size;
			}
			return value + size;
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

	public IItemCollection getConstantPools() {
		if (poolStats == null) {
			Map<String, ConstPoolItem> poolStatsByName = new HashMap<>();
			for (ConstantPoolInfo info : constantPoolInfoList) {
				ConstPoolItem poolItem = poolStatsByName.computeIfAbsent(info.name, key -> createPoolItem(info));
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
			return null;
		}

		@Override
		public Map<IAccessorKey<?>, ? extends IDescribable> getAccessorKeys() {
			return null;
		}

		@Override
		public boolean hasAttribute(ICanonicalAccessorFactory<?> attribute) {
			return false;
		}

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
			return null;
		}

		@Override
		public Map<IAccessorKey<?>, ? extends IDescribable> getAccessorKeys() {
			return null;
		}

		@Override
		public boolean hasAttribute(ICanonicalAccessorFactory<?> attribute) {
			return false;
		}

		@Override
		public <M> IMemberAccessor<M, IItem> getAccessor(IAccessorKey<M> attribute) {
			if ("typeName".equals(attribute.getIdentifier())) {
				return ((IMemberAccessor<M, IItem>) MemberAccessorToolkit.<IItem, Object, Object> constant(typeName));
			}
			if ("constant".equals(attribute.getIdentifier())) {
				return ((IMemberAccessor<M, IItem>) MemberAccessorToolkit.<IItem, Object, Object> constant(constant));
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

	private ConstPoolItem createPoolItem(ConstantPoolInfo info) {
		Long totalSize = entryPoolSizeByType.get(info.name);
		long entrySize = totalSize != null ? totalSize.longValue() : 0;
		return new ConstPoolItem(info.name, 0, entrySize);
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

	private static class ConstantPoolInfo {
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
