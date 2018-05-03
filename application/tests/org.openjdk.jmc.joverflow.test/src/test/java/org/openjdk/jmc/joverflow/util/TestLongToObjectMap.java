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
package org.openjdk.jmc.joverflow.util;

import java.util.HashSet;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 */
public class TestLongToObjectMap {

	private static final int DATA_SIZE = 300000;
	private static String[] data;

	/*
	 * TODO: It looks like the map works much slower when numbers that we use as keys are big, e.g.
	 * prefix = Integer.MAX_VALUE - DATA_SIZE / 2.
	 * 
	 * Investigate!
	 */
	private static final long prefix = Integer.MAX_VALUE / 2 + 1;

	@BeforeClass
	public static void setUp() {
		data = new String[DATA_SIZE];
		for (int i = 0; i < DATA_SIZE; i++) {
			data[i] = Integer.toString(i);
		}
	}

	@Test
	public void testAddAndCheckElementsNotLinked() {
		addAndCheckElements(false);
	}

	@Test
	public void testAddAndCheckElementsLinked() {
		addAndCheckElements(true);
	}

	@Test
	public void testAddAndIterateElementsLinked() {
		LongToObjectMap<String> map = new LongToObjectMap<>(DATA_SIZE / 10, true);
		for (int i = 0; i < DATA_SIZE; i++) {
			map.put(prefix + i * 3, data[i]);
		}
//		System.out.println(map.getNumRehashes());

		int i = 0;
		for (String s : map.values()) {
			Assert.assertEquals(data[i], s);
			i++;
		}
		Assert.assertEquals(DATA_SIZE, i);
	}

	@Test
	public void testAddAndIterateElementsNotLinked() {
		LongToObjectMap<String> map = new LongToObjectMap<>(DATA_SIZE / 100, false);
		for (int i = 0; i < DATA_SIZE; i++) {
			map.put(prefix + i * 3, data[i]);
		}
//		System.out.println(map.getNumRehashes());

		for (int i = 0; i < DATA_SIZE; i++) {
			Assert.assertEquals(data[i], map.get(prefix + i * 3));
		}

		HashSet<String> stringsInMap = new HashSet<>(DATA_SIZE);
		int i = 0;
		for (String s : map.values()) {
			Assert.assertFalse(stringsInMap.contains(s));
			stringsInMap.add(s);
			i++;
		}
		Assert.assertEquals(DATA_SIZE, i);
		Assert.assertEquals(DATA_SIZE, stringsInMap.size());

		for (String s : data) {
			Assert.assertTrue(stringsInMap.contains(s));
			stringsInMap.remove(s);
		}
		Assert.assertEquals(0, stringsInMap.size());
	}

	private void addAndCheckElements(boolean linked) {
		LongToObjectMap<String> map = new LongToObjectMap<>(DATA_SIZE / 100, linked);
		for (int i = 0; i < DATA_SIZE; i++) {
			map.put(prefix + i, data[i]);
		}

		for (int i = 0; i < DATA_SIZE; i++) {
			Assert.assertEquals(data[i], map.get(prefix + i));
		}
	}

}
