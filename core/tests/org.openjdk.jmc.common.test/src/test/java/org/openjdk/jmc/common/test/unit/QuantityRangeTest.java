/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.common.test.unit;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;

public class QuantityRangeTest {

	@Test
	public void intersection_overlap() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(5),
				UnitLookup.SECOND.quantity(15));
		IRange<IQuantity> intersection = QuantityRange.intersection(r1, r2);
		IRange<IQuantity> expected = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(5),
				UnitLookup.SECOND.quantity(10));
		Assert.assertEquals(expected, intersection);
	}

	@Test
	public void intersection_full_overlap() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> intersection = QuantityRange.intersection(r1, r2);
		IRange<IQuantity> expected = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		Assert.assertEquals(expected, intersection);
	}

	@Test
	public void intersection_limit() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(10),
				UnitLookup.SECOND.quantity(20));
		IRange<IQuantity> intersection = QuantityRange.intersection(r1, r2);
		IRange<IQuantity> expected = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(10),
				UnitLookup.SECOND.quantity(10));
		Assert.assertEquals(expected, intersection);
	}

	@Test
	public void intersection_disjoint() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(5));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(10),
				UnitLookup.SECOND.quantity(15));
		IRange<IQuantity> intersection = QuantityRange.intersection(r1, r2);
		Assert.assertNull(intersection);
	}

	@Test
	public void union_overlap() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(5),
				UnitLookup.SECOND.quantity(15));
		IRange<IQuantity> union = QuantityRange.union(r1, r2);
		IRange<IQuantity> expected = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(15));
		Assert.assertEquals(expected, union);
	}

	@Test
	public void union_full_overlap() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> union = QuantityRange.union(r1, r2);
		IRange<IQuantity> expected = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		Assert.assertEquals(expected, union);
	}

	@Test
	public void union_limit() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(10));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(10),
				UnitLookup.SECOND.quantity(20));
		IRange<IQuantity> union = QuantityRange.union(r1, r2);
		IRange<IQuantity> expected = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(20));
		Assert.assertEquals(expected, union);
	}

	@Test
	public void union_disjoint() {
		IRange<IQuantity> r1 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(0),
				UnitLookup.SECOND.quantity(5));
		IRange<IQuantity> r2 = QuantityRange.createWithEnd(UnitLookup.SECOND.quantity(10),
				UnitLookup.SECOND.quantity(15));
		IRange<IQuantity> union = QuantityRange.union(r1, r2);
		Assert.assertNull(union);
	}
}
