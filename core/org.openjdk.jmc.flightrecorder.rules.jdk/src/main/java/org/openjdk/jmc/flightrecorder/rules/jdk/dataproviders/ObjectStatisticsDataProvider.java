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
package org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemConsumer;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.jdk.combine.Combinable;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;

/**
 * Helper class with methods for calculations relating to heap objects.
 */
public class ObjectStatisticsDataProvider {

	private static class IncreaseCalculator
			implements IItemConsumer<IncreaseCalculator>, Combinable<IncreaseCalculator> {

		private final IMemberAccessor<IQuantity, IItem> xAccessor;
		private final IMemberAccessor<IQuantity, IItem> yAccessor;
		private IQuantity minX;
		private IQuantity yAtMinX;
		private IQuantity maxX;
		private IQuantity yAtMaxX;

		IncreaseCalculator(IMemberAccessor<IQuantity, IItem> xAccessor, IMemberAccessor<IQuantity, IItem> yAccessor) {
			this.xAccessor = xAccessor;
			this.yAccessor = yAccessor;
		}

		@Override
		public void consume(IItem item) {
			add(xAccessor.getMember(item), yAccessor.getMember(item));
		}

		private void add(IQuantity x, IQuantity y) {
			if (x != null && y != null) {
				if (minX == null) {
					minX = maxX = x;
					yAtMinX = yAtMaxX = y;
				} else if (x.compareTo(minX) < 0) {
					minX = x;
					yAtMinX = y;
				} else if (x.compareTo(maxX) > 0) {
					maxX = x;
					yAtMaxX = y;
				}
			}
		}

		@Override
		public IncreaseCalculator merge(IncreaseCalculator other) {
			combineWith(other);
			return this;
		}

		@Override
		public IncreaseCalculator combineWith(IncreaseCalculator other) {
			add(other.minX, other.yAtMinX);
			add(other.maxX, other.yAtMaxX);
			return this;
		}

	}

	/**
	 * @return an aggregator for the increase in the live set between the first and last garbage
	 *         collections
	 */
	public static IAggregator<IQuantity, ?> getIncreaseAggregator() {
		return new Aggregators.MergingAggregator<IQuantity, IncreaseCalculator>(
				Messages.getString(Messages.ObjectStatisticsDataProvider_AGGR_LIVE_SIZE_INCREASE),
				Messages.getString(Messages.ObjectStatisticsDataProvider_AGGR_LIVE_SIZE_INCREASE_DESC),
				UnitLookup.MEMORY) {

			@Override
			public boolean acceptType(IType<IItem> type) {
				return JdkTypeIDs.OBJECT_COUNT.equals(type.getIdentifier());
			}

			@Override
			public IncreaseCalculator newItemConsumer(IType<IItem> type) {
				IType<IItem> iType = type;
				return new IncreaseCalculator(JfrAttributes.END_TIME.getAccessor(iType),
						JdkAttributes.HEAP_TOTAL.getAccessor(iType));
			}

			@Override
			public IQuantity getValue(IncreaseCalculator consumer) {
				return consumer == null || consumer.maxX == null ? null : consumer.yAtMaxX.subtract(consumer.yAtMinX);

			}
		};
	}
}
