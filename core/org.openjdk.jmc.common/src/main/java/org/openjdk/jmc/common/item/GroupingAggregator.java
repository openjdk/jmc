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
package org.openjdk.jmc.common.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.openjdk.jmc.common.IPredicate;
import org.openjdk.jmc.common.collection.EntryHashMap;
import org.openjdk.jmc.common.collection.EntryHashMap.Entry;
import org.openjdk.jmc.common.item.Aggregators.MergingAggregator;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;

public class GroupingAggregator {

	public interface GroupEntry<K, G> {
		K getKey();

		G getConsumer();
	}

	public interface IGroupsFinisher<V, K, G> {
		IType<? super V> getValueType();

		V getValue(Iterable<? extends GroupEntry<K, G>> groups);
	}

	public interface IQuantityListFinisher<V> {
		IType<? super V> getValueType();

		V getValue(List<IQuantity> values, IQuantity total);
	}

	private static class ObjectEntry<K, V> extends Entry<K> implements GroupEntry<K, V> {
		private V value;

		public ObjectEntry(K key, V value) {
			super(key);
			this.value = value;
		}

		@Override
		public V getConsumer() {
			return value;
		}
	}

	private static class GroupingConsumer<K, G extends IItemConsumer<G>>
			implements IItemConsumer<GroupingConsumer<K, G>> {

		private EntryHashMap<K, ObjectEntry<K, G>> map;
		private final IMemberAccessor<? extends K, IItem> keyAccessor;
		private final IItemConsumerFactory<G> groupAggregator;

		private GroupingConsumer(IMemberAccessor<? extends K, IItem> keyAccessor,
				IItemConsumerFactory<G> groupAggregator) {
			this.keyAccessor = keyAccessor;
			this.groupAggregator = groupAggregator;
		}

		@Override
		public void consume(IItem item) {
			initialize(ItemToolkit.getItemType(item));
			K key = keyAccessor.getMember(item);
			if (key != null) {
				map.get(key, true).value.consume(item);
			}
		}

		private void initialize(final IType<IItem> type) {
			if (map == null) {
				map = new EntryHashMap<K, ObjectEntry<K, G>>(1000, 0.5f) {

					@Override
					protected ObjectEntry<K, G> computeValue(K key) {
						return new ObjectEntry<>(key, groupAggregator.newItemConsumer(type));
					}
				};
			}
		}

		@Override
		public GroupingConsumer<K, G> merge(GroupingConsumer<K, G> other) {
			if (map != null && other != null && other.map != null) {
				for (ObjectEntry<K, G> otherEntry : other.map) {
					ObjectEntry<K, G> thisEntry = map.get(otherEntry.getKey(), true);
					thisEntry.value = thisEntry.value.merge(otherEntry.value);
				}
			} else if (other != null && other.map != null) {
				map = other.map;
			}
			return this;
		}

		Iterator<ObjectEntry<K, G>> getGroups() {
			return map == null ? Collections.<ObjectEntry<K, G>> emptyList().iterator() : map.iterator();
		}
	}

	private static class GroupingAggregatorImpl<V, K, G extends IItemConsumer<G>>
			extends MergingAggregator<V, GroupingConsumer<K, G>> {

		private final IGroupsFinisher<V, K, G> groupsFinisher;
		private final IAccessorFactory<K> keyField;
		private final IItemConsumerFactory<G> consumerFactory;
		private final IPredicate<IType<IItem>> acceptType;

		GroupingAggregatorImpl(String name, String description, IAccessorFactory<K> keyField,
				IItemConsumerFactory<G> consumerFactory, IPredicate<IType<IItem>> acceptType,
				IGroupsFinisher<V, K, G> groupsFinisher) {
			super(name, description, groupsFinisher.getValueType());
			this.consumerFactory = consumerFactory;
			this.acceptType = acceptType;
			this.keyField = keyField;
			this.groupsFinisher = groupsFinisher;
		}

		@Override
		public boolean acceptType(IType<IItem> type) {
			return keyField.getAccessor(type) != null && acceptType.evaluate(type);
		}

		@Override
		public GroupingConsumer<K, G> newItemConsumer(IType<IItem> type) {
			return new GroupingConsumer<>(keyField.getAccessor(type), consumerFactory);
		}

		@Override
		public V getValue(final GroupingConsumer<K, G> consumer) {
			return groupsFinisher.getValue(new Iterable<ObjectEntry<K, G>>() {

				@Override
				public Iterator<ObjectEntry<K, G>> iterator() {
					return consumer.getGroups();
				}
			});
		}
	}

	public static <V, K, C extends IItemConsumer<C>> IAggregator<V, ?> build(
		String name, String description, IAccessorFactory<K> keyField, IItemConsumerFactory<C> groupAggregator,
		IPredicate<IType<IItem>> acceptType, IGroupsFinisher<V, K, C> finisher) {
		return new GroupingAggregatorImpl<>(name, description, keyField, groupAggregator, acceptType, finisher);
	}

	public static <V, K, C extends IItemConsumer<C>> IAggregator<V, ?> build(
		String name, String description, IAccessorFactory<K> keyField, final IAggregator<?, C> a,
		IGroupsFinisher<V, K, C> finisher) {
		return build(name, description, keyField, a, new IPredicate<IType<IItem>>() {

			@Override
			public boolean evaluate(IType<IItem> o) {
				return a.acceptType(o);
			}
		}, finisher);
	}

	public static <K, C extends IItemConsumer<C>> IAggregator<Iterable<? extends GroupEntry<K, C>>, ?> build(
		String name, String description, IAccessorFactory<K> keyField, IItemConsumerFactory<C> groupAggregator,
		IPredicate<IType<IItem>> acceptType) {
		return build(name, description, keyField, groupAggregator, acceptType,
				new IGroupsFinisher<Iterable<? extends GroupEntry<K, C>>, K, C>() {

					@Override
					public IType<Object> getValueType() {
						return UnitLookup.UNKNOWN;
					}

					@Override
					public Iterable<? extends GroupEntry<K, C>> getValue(Iterable<? extends GroupEntry<K, C>> groups) {
						return groups;
					}

				});
	}

	public static <V, K, C extends IItemConsumer<C>> IAggregator<V, ?> build(
		String name, String description, IAccessorFactory<K> keyField, final IAggregator<IQuantity, C> a,
		final IQuantityListFinisher<V> groupFinisher) {
		return build(name, description, keyField, a, new IGroupsFinisher<V, K, C>() {

			@Override
			public IType<? super V> getValueType() {
				return groupFinisher.getValueType();
			}

			@Override
			public V getValue(Iterable<? extends GroupEntry<K, C>> groups) {
				Iterator<? extends GroupEntry<K, C>> groupsIterator = groups.iterator();
				if (groupsIterator.hasNext()) {
					List<IQuantity> values = new ArrayList<>();
					while (groupsIterator.hasNext()) {
						values.add(a.getValue(Arrays.asList(groupsIterator.next().getConsumer()).iterator()));
					}
					Collections.sort(values);
					IQuantity total = a.getValue(consumerIterator(groups.iterator()));
					return groupFinisher.getValue(values, total);
				}
				return null;
			}

		});
	}

	public static <V, C extends IItemConsumer<C>> IAggregator<IQuantity, ?> buildMax(
		String name, String description, IAccessorFactory<?> keyField, final IAggregator<IQuantity, C> aggregate) {
		IQuantityListFinisher<IQuantity> gf = new IQuantityListFinisher<IQuantity>() {
			@Override
			public IType<? super IQuantity> getValueType() {
				return aggregate.getValueType();
			}

			@Override
			public IQuantity getValue(List<IQuantity> values, IQuantity total) {
				return values.size() == 0 ? null : values.get(values.size() - 1);
			}
		};
		return build(name, description, keyField, aggregate, gf);
	}

	private static <G> Iterator<G> consumerIterator(final Iterator<? extends GroupEntry<?, G>> it) {
		return new Iterator<G>() {

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public G next() {
				return it.next().getConsumer();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

}
