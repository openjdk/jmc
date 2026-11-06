/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemConsumer;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

class JfrToolkitTest {

	@Test
	void clampUsesDefaultForMissingOrNonPositive() {
		assertEquals(50, JfrToolkit.clamp(null, 50, 200));
		assertEquals(50, JfrToolkit.clamp(0, 50, 200));
		assertEquals(50, JfrToolkit.clamp(-7, 50, 200));
	}

	@Test
	void clampHonoursRequestedValueUpToCap() {
		assertEquals(10, JfrToolkit.clamp(10, 50, 200));
		assertEquals(200, JfrToolkit.clamp(200, 50, 200));
		assertEquals(200, JfrToolkit.clamp(5000, 50, 200));
	}

	@Test
	void formatHandlesNull() {
		assertEquals("", JfrToolkit.format(null));
		assertEquals("N/A", JfrToolkit.formatQuantity(null));
	}

	@Test
	void quantityFormattingPreservesAllNativeLongDigits() {
		IQuantity quantity = UnitLookup.NANOSECOND.quantity(9007199254740993L);
		// Strip localized grouping separators and the unit, retaining every numerical digit.
		assertEquals("9007199254740993", JfrToolkit.formatQuantity(quantity).replaceAll("\\D", ""));
		assertEquals("9007199254740993", JfrToolkit.format(quantity).replaceAll("\\D", ""));
	}

	@Test
	void recordingBoundsUseCollectionAggregatesAndPreserveEpochNanoseconds() {
		long earliest = 1_700_000_000_000_000_001L;
		long latest = earliest + 11;
		String endTime = JfrAttributes.END_TIME.getIdentifier();
		IItemCollection fixture = TestItems.collection("test.Timed",
				List.of(JfrAttributes.START_TIME, JfrAttributes.END_TIME),
				List.of(Map.of("startTime", UnitLookup.EPOCH_NS.quantity(earliest + 3), endTime,
						UnitLookup.EPOCH_NS.quantity(earliest + 7)),
						Map.of("startTime", UnitLookup.EPOCH_NS.quantity(earliest), endTime,
								UnitLookup.EPOCH_NS.quantity(latest)),
						Map.of("startTime", UnitLookup.EPOCH_NS.quantity(earliest + 5), endTime,
								UnitLookup.EPOCH_NS.quantity(earliest + 8)),
						Map.of()));
		IItemCollection items = aggregatesOnly(fixture);
		IQuantity start = JfrToolkit.getRecordingStart(items);
		IQuantity end = JfrToolkit.getRecordingEnd(items);
		assertEquals(UnitLookup.EPOCH_NS, start.getUnit());
		assertEquals(earliest, start.longValue());
		assertEquals(UnitLookup.EPOCH_NS, end.getUnit());
		assertEquals(latest, end.longValue());
	}

	private static IItemCollection aggregatesOnly(IItemCollection delegate) {
		return new IItemCollection() {
			@Override
			public Iterator<IItemIterable> iterator() {
				throw new AssertionError("Recording bounds must use the collection's aggregates.");
			}

			@Override
			public IItemCollection apply(IItemFilter filter) {
				return delegate.apply(filter);
			}

			@Override
			public <V, C extends IItemConsumer<C>> V getAggregate(IAggregator<V, C> aggregator) {
				return delegate.getAggregate(aggregator);
			}

			@Override
			public boolean hasItems() {
				return delegate.hasItems();
			}

			@Override
			@SuppressWarnings("deprecation")
			public Set<IRange<IQuantity>> getUnfilteredTimeRanges() {
				return delegate.getUnfilteredTimeRanges();
			}
		};
	}
}
