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
package org.openjdk.jmc.joverflow.util;

import org.junit.Assert;
import org.junit.Test;

public class TestChunkedIntArray {

	@Test
	public void testGrowAcrossChunks() {
		ChunkedIntArray array = new ChunkedIntArray();
		Assert.assertTrue(array.isEmpty());
		int n = 3 * 1024 * 1024 + 17; // More than three chunks
		for (int i = 0; i < n; i++) {
			array.add(i * 3);
		}
		Assert.assertEquals(n, array.size());
		Assert.assertFalse(array.isEmpty());
		for (int i = 0; i < n; i++) {
			Assert.assertEquals(i * 3, array.get(i));
		}
		array.set(n - 1, -5);
		array.set(0, -7);
		Assert.assertEquals(-5, array.get(n - 1));
		Assert.assertEquals(-7, array.get(0));

		int[] copy = array.toArray();
		Assert.assertEquals(n, copy.length);
		Assert.assertEquals(-7, copy[0]);
		Assert.assertEquals(3, copy[1]);
		Assert.assertEquals(-5, copy[n - 1]);
	}

	@Test
	public void testClearAndRelease() {
		ChunkedIntArray array = new ChunkedIntArray();
		array.add(1);
		array.add(2);
		array.clear();
		Assert.assertEquals(0, array.size());
		array.add(3);
		Assert.assertEquals(3, array.get(0));
		array.release();
		Assert.assertTrue(array.isEmpty());
		Assert.assertEquals(0, array.toArray().length);
		array.add(4);
		Assert.assertEquals(4, array.get(0));
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testGetOutOfBounds() {
		ChunkedIntArray array = new ChunkedIntArray();
		array.add(1);
		array.get(1);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testSetOutOfBounds() {
		ChunkedIntArray array = new ChunkedIntArray();
		array.set(0, 1);
	}
}
