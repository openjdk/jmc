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
package org.openjdk.jmc.common.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.junit.Test;

@SuppressWarnings("nls")
public class SortedHeadTest {

	private final Comparator<Integer> COMPARATOR = new Comparator<Integer>() {

		@Override
		public int compare(Integer o1, Integer o2) {
			return (o1).compareTo(o2);
		}
	};

	@Test
	public void testAddIncreasing() {
		Integer[] init = new Integer[] {9, 2, 6, 2, 8};
		SortedHead<Integer> sh = new SortedHead<>(init, COMPARATOR);
		for (int i = 0; i < 10; i++) {
			sh.addObject(i);
		}
		Integer[] tail = sh.getTail();
		assertEquals(0, (int) init[0]);
		assertEquals(2, (int) init[init.length - 1]);
		assertEquals(10, tail.length);
	}

	@Test
	public void testAddDecreasing() {
		Integer[] init = new Integer[] {8, 2, 6, 2, 9};
		SortedHead<Integer> sh = new SortedHead<>(init, COMPARATOR);
		for (int i = 9; i >= 0; i--) {
			sh.addObject(i);
		}
		Integer[] tail = sh.getTail();
		assertEquals(0, (int) init[0]);
		assertEquals(2, (int) init[init.length - 1]);
		assertEquals(10, tail.length);
	}

	@Test
	public void testRandom() {
		int valueCount = 1000000;
		Integer[] testArr = new Integer[valueCount];
		Random rand = new Random();
		for (int i = valueCount - 1; i >= 0; i--) {
			testArr[i] = rand.nextInt();
		}

		Integer[] fastSortArray = new Integer[1000];
		System.arraycopy(testArr, 0, fastSortArray, 0, fastSortArray.length);

		long t = System.currentTimeMillis();
		SortedHead<Integer> sh = new SortedHead<>(fastSortArray, COMPARATOR);
		for (int i = fastSortArray.length; i < testArr.length; i++) {
			sh.addObject(testArr[i]);
		}
		Integer[] tail = sh.getTail();
		System.out.println("SortedHead used " + (System.currentTimeMillis() - t));

		t = System.currentTimeMillis();
		Arrays.sort(testArr, COMPARATOR);
		System.out.println("Arrays.sort used " + (System.currentTimeMillis() - t));

		for (int i = 0; i < fastSortArray.length; i++) {
			assertEquals(testArr[i], fastSortArray[i]);
		}
		assertEquals(valueCount - fastSortArray.length, tail.length);
	}
}
