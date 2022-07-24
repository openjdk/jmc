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
package org.openjdk.jmc.common.test.item;

import org.junit.Assert;
import org.junit.Test;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.Aggregators.AdvancedMaxAggregator;
import org.openjdk.jmc.common.item.Aggregators.AdvancedMinAggregator;
import org.openjdk.jmc.common.test.MCTestCase;
import org.openjdk.jmc.common.test.mock.item.MockAggregators;
import org.openjdk.jmc.common.test.mock.item.MockAttributes;
import org.openjdk.jmc.common.test.mock.item.MockCollections;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;

@SuppressWarnings("nls")
public class AggregatorTest extends MCTestCase {
	private final static double EPSILON = 1e-9;

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
