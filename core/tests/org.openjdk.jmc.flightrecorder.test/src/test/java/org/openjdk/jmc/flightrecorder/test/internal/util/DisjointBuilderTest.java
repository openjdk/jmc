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
package org.openjdk.jmc.flightrecorder.test.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.internal.util.DisjointBuilder;
import org.openjdk.jmc.flightrecorder.internal.util.DisjointBuilder.ArrayFactory;

@SuppressWarnings("nls")
public class DisjointBuilderTest {

	/**
	 * Dummy class used to test DisjointBuilder without external dependencies
	 */
	private static class RangeObject {
		private final IQuantity start;
		private final IQuantity end;

		RangeObject(IQuantity start, IQuantity end) {
			this.start = start;
			this.end = end;
		}

	}

	private final static IMemberAccessor<IQuantity, RangeObject> START = new IMemberAccessor<IQuantity, RangeObject>() {

		@Override
		public IQuantity getMember(RangeObject inObject) {
			return inObject.start;
		}
	};

	private final static IMemberAccessor<IQuantity, RangeObject> END = new IMemberAccessor<IQuantity, RangeObject>() {

		@Override
		public IQuantity getMember(RangeObject inObject) {
			return inObject.end;
		}
	};

	private static final ArrayFactory<RangeObject> ARRAY_FACTORY = new ArrayFactory<RangeObject>() {

		@Override
		public RangeObject[] createArray(int size) {
			return new RangeObject[size];
		}
	};

	@Test
	public void testInOrderAdd() throws IOException {
		ArrayList<DisjointBuilder<RangeObject>> builders = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			DisjointBuilder<RangeObject> builder = new DisjointBuilder<>(START, END);
			for (int j = 0; j < 10000; j++) {
				long start = i * 10000 + j;
				builder.add(new RangeObject(UnitLookup.NUMBER_UNITY.quantity(start),
						UnitLookup.NUMBER_UNITY.quantity(start + 1)));
			}
			builders.add(builder);
		}

		Iterable<RangeObject[]> arrays = DisjointBuilder.toArrays(builders, ARRAY_FACTORY);

		Iterator<RangeObject[]> iterator = arrays.iterator();
		RangeObject[] array = iterator.next();
		assertFalse(iterator.hasNext());
		assertEquals(10 * 10000, array.length);
		checkNoOverlap(array);

	}

	@Test
	public void testRandomAdd() throws IOException {
		for (int i = 0; i < 100; i++) {
			testRandomAdd(i);
		}
	}

	@Test
	public void testEmpty() throws IOException {
		ArrayList<DisjointBuilder<RangeObject>> empty = new ArrayList<>();
		assertFalse(DisjointBuilder.toArrays(empty, ARRAY_FACTORY).iterator().hasNext());
		empty.add(new DisjointBuilder<>(START, END));
		assertFalse(DisjointBuilder.toArrays(empty, ARRAY_FACTORY).iterator().hasNext());
	}

	private static void testRandomAdd(long randomSeed) throws IOException {
		Random rand = new Random(randomSeed);
		ArrayList<DisjointBuilder<RangeObject>> builders = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			DisjointBuilder<RangeObject> builder = new DisjointBuilder<>(START, END);
			for (int j = 0; j < 100; j++) {
				int start = rand.nextInt(10000000);
				builder.add(new RangeObject(UnitLookup.NUMBER_UNITY.quantity(start),
						UnitLookup.NUMBER_UNITY.quantity(start + rand.nextInt(10))));
			}
			builders.add(builder);
		}

		Iterable<RangeObject[]> arrays = DisjointBuilder.toArrays(builders, ARRAY_FACTORY);
		for (RangeObject[] array : arrays) {
			checkNoOverlap(array);
		}
	}

	private static void checkNoOverlap(RangeObject[] array) throws IOException {
		IQuantity lastEnd = UnitLookup.NUMBER_UNITY.quantity(0);
		for (RangeObject element : array) {
			if (element.start.compareTo(lastEnd) < 0) {
				Assert.fail("DisjointBuilder must build arrays of non-overlapping ranges: "
						+ element.start.clampedLongValueIn(UnitLookup.NUMBER_UNITY) + " < "
						+ lastEnd.clampedLongValueIn(UnitLookup.NUMBER_UNITY));
			}
			lastEnd = element.end;
		}
	}
}
