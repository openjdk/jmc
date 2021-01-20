/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Datadog, Inc. All rights reserved.
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

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.test.mock.item.MockAttributes;
import org.openjdk.jmc.common.test.mock.item.MockCollections;
import org.openjdk.jmc.common.unit.IQuantity;

public class StreamTest {

	@Test
	public void testIItemIterableStream() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(99, 100));
		List<IItemIterable> iterables = mockDoubleCollection.stream().collect(Collectors.toList());
		List<String> ids = mockDoubleCollection.stream().map(iterable -> iterable.getType().getIdentifier())
				.collect(Collectors.toList());
		Assert.assertEquals(1, iterables.size());
		assertEquals("mock/MockNumberItem", ids.get(0));
	}

	@Test
	public void testIItemStream() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(999, 100));
		List<IItem> items = mockDoubleCollection.stream().flatMap((iterable) -> iterable.stream())
				.collect(Collectors.toList());
		Assert.assertEquals(999, items.size());
	}

	@Test
	public void testValues() {
		IItemCollection mockDoubleCollection = MockCollections
				.getNumberCollection(MockCollections.generateNumberArray(99, 100));
		List<IQuantity> values = mockDoubleCollection.values(MockAttributes.DOUBLE_VALUE).get()
				.collect(Collectors.toList());
		Assert.assertEquals(99, values.size());
		values.forEach((value) -> Assert.assertTrue(value.doubleValue() >= 0 && value.doubleValue() <= 100));
	}

}
