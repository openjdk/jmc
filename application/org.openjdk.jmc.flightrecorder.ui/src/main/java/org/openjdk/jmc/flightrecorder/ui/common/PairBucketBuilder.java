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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemConsumer;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.ItemIterableToolkit;
import org.openjdk.jmc.ui.charts.IQuantitySeries;
import org.openjdk.jmc.ui.charts.SubdividedQuantityRange;
import org.openjdk.jmc.ui.charts.XYQuantities;

// FIXME: Move this class
public class PairBucketBuilder<C extends IItemConsumer<C>, CC extends IItemConsumer<CC>> {
	// FIXME: Do we need a special class for this, or could we move the functionality into the ordinary BucketBuilder?
	int bucketCount;
	IAttribute<IQuantity> xAttribute;
	IAggregator<IQuantity, C> aggregator;
	SubdividedQuantityRange xRange;
	IAggregator<IQuantity, CC> secondAggregator;

	PairBucketBuilder(int bucketCount, IAttribute<IQuantity> xAttribute, IAggregator<IQuantity, C> aggregator,
			IAggregator<IQuantity, CC> secondAggregator, SubdividedQuantityRange xRange) {
		this.bucketCount = bucketCount;
		this.xAttribute = xAttribute;
		this.aggregator = aggregator;
		this.secondAggregator = secondAggregator;
		this.xRange = xRange;
	}

	private List<Pair<C, CC>> build(IType<IItem> type) {
		List<Pair<C, CC>> calculators = new ArrayList<>(bucketCount);
		for (int i = 0; i < bucketCount; i++) {
			calculators.add(new Pair<>(aggregator.newItemConsumer(type), secondAggregator.newItemConsumer(type)));
		}
		return calculators;
	}

	private void consume(List<Pair<C, CC>> list, IItem item, IMemberAccessor<IQuantity, IItem> xAccessor) {
		int xPos = xRange.getFloorSubdivider(xAccessor.getMember(item));
		if (xPos >= 0 && xPos < list.size()) {

			list.get(xPos).left.consume(item);
			list.get(xPos).right.consume(item);
		}
	}

	private List<Pair<C, CC>> combine(List<Pair<C, CC>> l1, List<Pair<C, CC>> l2) {
		for (int i = 0; i < bucketCount; i++) {
			l1.set(i, new Pair<>(l1.get(i).left.merge(l2.get(i).left), l1.get(i).right.merge(l2.get(i).right)));
		}
		return l1;
	}

	private Collector<IItem, List<Pair<C, CC>>, List<Pair<C, CC>>> collector(IType<IItem> type) {
		IMemberAccessor<IQuantity, IItem> xAccessor = xAttribute.getAccessor(type);
		return Collector.of(() -> build(type), (l, i) -> consume(l, i, xAccessor), this::combine,
				Collector.Characteristics.UNORDERED);
	}

	private boolean acceptItems(IItemIterable is) {
		return aggregator.acceptType(is.getType());
	}

	private List<Pair<C, CC>> collectItems(IItemIterable is) {
		return ItemIterableToolkit.parallelStream(is).collect(collector(is.getType()));
	}

	Pair<IQuantity[], IQuantity[]> buildBuckets(IItemCollection items) {
		IQuantity[] q1 = new IQuantity[bucketCount];
		IQuantity[] q2 = new IQuantity[bucketCount];
		List<List<Pair<C, CC>>> collect = ItemCollectionToolkit.parallelStream(items).filter(this::acceptItems)
				.map(this::collectItems).collect(Collectors.toList());
		for (int i = 0; i < bucketCount; i++) {
			int bucketIndex = i;
			List<Pair<C, CC>> pairList = collect.stream().map(list -> list.get(bucketIndex))
					.collect(Collectors.toList());
			List<C> l1 = new ArrayList<>();
			List<CC> l2 = new ArrayList<>();
			for (Pair<C, CC> pair : pairList) {
				l1.add(pair.left);
				l2.add(pair.right);
			}
			q1[i] = aggregator.getValue(l1.iterator());
			q2[i] = secondAggregator.getValue(l2.iterator());
		}
		return new Pair<>(q1, q2);
	}

	public static <C extends IItemConsumer<C>, CC extends IItemConsumer<CC>> IQuantitySeries<IQuantity[]> aggregatorSeries(
		IItemCollection items, IAggregator<IQuantity, C> a, IAggregator<IQuantity, CC> c,
		IAttribute<IQuantity> xAttribute) {
		return new IQuantitySeries<IQuantity[]>() {
			@Override
			public XYQuantities<IQuantity[]> getQuantities(SubdividedQuantityRange xBucketRange) {
				int bucketCount = xBucketRange.getNumSubdividers();
				PairBucketBuilder<C, CC> bb = new PairBucketBuilder<>(bucketCount, xAttribute, a, c, xBucketRange);
				Pair<IQuantity[], IQuantity[]> pairBuckets = bb.buildBuckets(items);
				return XYQuantities.create(pairBuckets.right, Arrays.asList(pairBuckets.left), xBucketRange);
			}
		};
	}
}
