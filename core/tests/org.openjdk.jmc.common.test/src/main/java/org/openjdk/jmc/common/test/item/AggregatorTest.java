/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.common.test.item;

import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.Aggregators.AdvancedMaxAggregator;
import org.openjdk.jmc.common.item.Aggregators.AdvancedMinAggregator;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.test.MCTestCase;
import org.openjdk.jmc.test.mock.item.MockAggregators;
import org.openjdk.jmc.test.mock.item.MockAttributes;
import org.openjdk.jmc.test.mock.item.MockCollections;
import org.openjdk.jmc.test.mock.item.MockItem;
import org.openjdk.jmc.test.mock.item.MockItemCollection;
import org.openjdk.jmc.test.mock.item.MockNumberType;

@SuppressWarnings("nls")
public class AggregatorTest extends MCTestCase {
	private final static double EPSILON = 1e-9;
	private static final IAttribute<IQuantity> DURATION = Attribute.attr("duration", "Duration", UnitLookup.TIMESPAN);

	@Test
	public void testSumAggregator() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(400, 999));
		IQuantity aggregate = mockDoubleCollection.getAggregate(MockAggregators.SUM);
		// Reference value from excel
		Assert.assertEquals(195905.039483473, aggregate.doubleValue(), EPSILON);
	}

	@Test
	public void testVarianceAggregator() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(400, 999));
		IQuantity aggregate = mockDoubleCollection.getAggregate(MockAggregators.VARIANCE);
		// Reference value from excel
		Assert.assertEquals(74963.393247156, aggregate.doubleValue(), EPSILON);
	}

	@Test
	public void testVariancepAggregator() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(400, 999));
		IQuantity aggregate = mockDoubleCollection.getAggregate(MockAggregators.VARIANCEP);
		// Reference value from excel
		Assert.assertEquals(74775.9847640378, aggregate.doubleValue(), EPSILON);
	}

	@Test
	public void testStddevAggregator() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(400, 999));
		IQuantity aggregate = mockDoubleCollection.getAggregate(MockAggregators.STDDEV);
		// Reference value from excel
		Assert.assertEquals(273.794436114, aggregate.doubleValue(), EPSILON);
	}

	@Test
	public void testStddevpAggregator() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(400, 999));
		IQuantity aggregate = mockDoubleCollection.getAggregate(MockAggregators.STDDEVP);
		// Reference value from excel
		Assert.assertEquals(273.451978899473, aggregate.doubleValue(), EPSILON);
	}

	@Test
	public void testVarianceAndStddevAcrossUnits() {
		IItemCollection seconds = durationCollection(UnitLookup.SECOND, 1, 3);
		IItemCollection milliseconds = durationCollection(UnitLookup.MILLISECOND, 5000, 7000, 9000);
		// The combined values are [1, 3, 5, 7, 9] seconds, with population variance 8 and sample variance 10.
		assertVarianceAndStddev(ItemCollectionToolkit.merge(() -> Stream.of(seconds, milliseconds)), UnitLookup.SECOND,
				8, 10);
		assertVarianceAndStddev(ItemCollectionToolkit.merge(() -> Stream.of(milliseconds, seconds)),
				UnitLookup.MILLISECOND, 8_000_000, 10_000_000);
	}

	private static void assertVarianceAndStddev(
		IItemCollection items, IUnit varianceUnit, double populationVariance, double sampleVariance) {
		IQuantity population = items.getAggregate(Aggregators.variancep(DURATION));
		IQuantity sample = items.getAggregate(Aggregators.variance(DURATION));
		// Variance retains the input unit; its numeric value represents squared deviations in that unit.
		Assert.assertEquals(varianceUnit, population.getUnit());
		Assert.assertEquals(varianceUnit, sample.getUnit());
		Assert.assertEquals(populationVariance, population.doubleValue(), EPSILON);
		Assert.assertEquals(sampleVariance, sample.doubleValue(), EPSILON);
		Assert.assertEquals(Math.sqrt(8),
				items.getAggregate(Aggregators.stddevp(DURATION)).doubleValueIn(UnitLookup.SECOND), EPSILON);
		Assert.assertEquals(Math.sqrt(10),
				items.getAggregate(Aggregators.stddev(DURATION)).doubleValueIn(UnitLookup.SECOND), EPSILON);
	}

	private static IItemCollection durationCollection(IUnit unit, Number ... values) {
		MockNumberType type = new MockNumberType() {
			@Override
			@SuppressWarnings("unchecked")
			public <M> IMemberAccessor<M, MockItem<Number, MockNumberType>> getAccessor(IAccessorKey<M> key) {
				if (DURATION.getKey().equals(key)) {
					return item -> (M) unit.quantity(item.getValue());
				}
				return super.getAccessor(key);
			}
		};
		return new MockItemCollection<Number, MockNumberType>(values, type);
	}

	@Test
	public void testVarianceAggregetorZeroElement() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(0, 1));
		IQuantity aggregate = mockDoubleCollection.getAggregate(MockAggregators.VARIANCE);
		// Reference value from excel
		Assert.assertNull(aggregate);
	}

	@Test
	public void testVariancepAggregetorZeroElement() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(0, 1));
		IQuantity aggregate = mockDoubleCollection.getAggregate(MockAggregators.VARIANCEP);
		// Reference value from excel
		Assert.assertNull(aggregate);
	}

	@Test
	public void testStddevAggregetorZeroElement() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(0, 1));
		IQuantity aggregate = mockDoubleCollection.getAggregate(MockAggregators.STDDEV);
		// Reference value from excel
		Assert.assertNull(aggregate);
	}

	@Test
	public void testStddevpAggregetorZeroElement() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(0, 1));
		IQuantity aggregate = mockDoubleCollection.getAggregate(MockAggregators.STDDEVP);
		// Reference value from excel
		Assert.assertNull(aggregate);
	}

	@Test
	public void testVarianceAggregetorOneElement() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(1, 999));
		IQuantity aggregate = mockDoubleCollection.getAggregate(MockAggregators.VARIANCE);
		// Reference value from excel
		Assert.assertNull(aggregate);
	}

	@Test
	public void testVariancepAggregetorOneElement() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(1, 999));
		IQuantity aggregate = mockDoubleCollection.getAggregate(MockAggregators.VARIANCEP);
		// Reference value from excel
		Assert.assertEquals(aggregate.doubleValue(), 0, EPSILON);
	}

	@Test
	public void testStddevAggregatorOneElement() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(1, 999));
		IQuantity aggregate = mockDoubleCollection.getAggregate(MockAggregators.STDDEV);
		// Reference value from excel
		Assert.assertNull(aggregate);
	}

	@Test
	public void testStddevpAggregetorOneElement() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(1, 999));
		IQuantity aggregate = mockDoubleCollection.getAggregate(MockAggregators.STDDEVP);
		// Reference value from excel
		Assert.assertEquals(aggregate.doubleValue(), 0, EPSILON);
	}

	@Test
	public void testItemWithMinAndMaxAggregator() {
		IItemCollection items = MockCollections.getNumberCollection(new Number[] {49.806, 1.001, 10.0});
		assertExtremeItems(items);
	}

	@Test
	public void testItemWithMinAndMaxAcrossChunks() {
		IItemCollection first = MockCollections.getNumberCollection(new Number[] {1.001, 20.0});
		IItemCollection second = MockCollections.getNumberCollection(new Number[] {49.806, 10.0});
		IItem expectedMin = first.iterator().next().iterator().next();
		IItem expectedMax = second.iterator().next().iterator().next();
		IItemCollection forward = ItemCollectionToolkit.merge(() -> Stream.of(first, second));
		IItemCollection reverse = ItemCollectionToolkit.merge(() -> Stream.of(second, first));
		for (IItemCollection items : new IItemCollection[] {forward, reverse}) {
			Assert.assertSame(expectedMin, items.getAggregate(Aggregators.itemWithMin(MockAttributes.DOUBLE_VALUE)));
			Assert.assertSame(expectedMax, items.getAggregate(Aggregators.itemWithMax(MockAttributes.DOUBLE_VALUE)));
			assertExtremeItems(items);
		}
	}

	private static void assertExtremeItems(IItemCollection items) {
		IItem minimum = items.getAggregate(Aggregators.itemWithMin(MockAttributes.DOUBLE_VALUE));
		IItem maximum = items.getAggregate(Aggregators.itemWithMax(MockAttributes.DOUBLE_VALUE));
		IQuantity minimumValue = MockAttributes.DOUBLE_VALUE.getAccessor(ItemToolkit.getItemType(minimum))
				.getMember(minimum);
		IQuantity maximumValue = MockAttributes.DOUBLE_VALUE.getAccessor(ItemToolkit.getItemType(maximum))
				.getMember(maximum);
		Assert.assertEquals(1.001, minimumValue.doubleValue(), EPSILON);
		Assert.assertEquals(49.806, maximumValue.doubleValue(), EPSILON);
		Assert.assertEquals(items.getAggregate(Aggregators.min(MockAttributes.DOUBLE_VALUE)), minimumValue);
		Assert.assertEquals(items.getAggregate(Aggregators.max(MockAttributes.DOUBLE_VALUE)), maximumValue);
	}

	@Test
	public void testAdvancedMinAggregator() {
		IItemCollection mockCollection = MockCollections.getNumberCollection(new Number[] {101, 10, 135, 10});
		AdvancedMinAggregator<IQuantity, IQuantity> advancedMinAggregator = new Aggregators.AdvancedMinAggregator<>(
				"MinMock", "Should return the first number", MockAttributes.DOUBLE_VALUE, MockAttributes.INDEX_VALUE);
		IQuantity aggregate = mockCollection.getAggregate(advancedMinAggregator);
		Assert.assertEquals(101.0d, aggregate.doubleValue(), EPSILON);
	}

	@Test
	public void testAdvancedMaxAggregator() {
		IItemCollection mockCollection = MockCollections.getNumberCollection(new Number[] {101, 10, 135, 10});
		AdvancedMaxAggregator<IQuantity, IQuantity> advancedMinAggregator = new Aggregators.AdvancedMaxAggregator<>(
				"MaxMock", "Should return the last number", MockAttributes.DOUBLE_VALUE, MockAttributes.INDEX_VALUE);
		IQuantity aggregate = mockCollection.getAggregate(advancedMinAggregator);
		Assert.assertEquals(10.0d, aggregate.doubleValue(), EPSILON);
	}

	// This should never be the case, but spanking the system a bit.
	@Test
	public void testAdvancedMaxAggregatorNullOnTheWay() {
		IItemCollection mockCollection = MockCollections.getNumberCollection(new Number[] {101, null, 135, 10});
		AdvancedMaxAggregator<IQuantity, IQuantity> advancedMinAggregator = new Aggregators.AdvancedMaxAggregator<>(
				"MaxMock", "Should return the last number", MockAttributes.DOUBLE_VALUE, MockAttributes.INDEX_VALUE);
		IQuantity aggregate = mockCollection.getAggregate(advancedMinAggregator);
		Assert.assertEquals(10.0d, aggregate.doubleValue(), EPSILON);
	}

	@Test
	public void testAdvancedMaxAggregatorLastElementNull() {
		IItemCollection mockCollection = MockCollections.getNumberCollection(new Number[] {101, null, 135, null});
		AdvancedMaxAggregator<IQuantity, IQuantity> advancedMinAggregator = new Aggregators.AdvancedMaxAggregator<>(
				"MaxMock", "Should return the last number", MockAttributes.DOUBLE_VALUE, MockAttributes.INDEX_VALUE);
		try {
			mockCollection.getAggregate(advancedMinAggregator);
			Assert.fail("You really should never get here!");
		} catch (NullPointerException e) {
			// Should puke on creating the quantity. If we would not have used quantities in the mock item attributes this would have been fine.
		}
	}

	public static void main(String[] args) {
		System.out.println(MockCollections.generateFullPrecisionString(MockCollections.generateNumberArray(400, 999)));
	}
}
