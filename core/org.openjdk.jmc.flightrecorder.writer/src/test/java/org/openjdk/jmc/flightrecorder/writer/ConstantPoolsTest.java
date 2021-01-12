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
package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConstantPoolsTest {
	private ConstantPools instance;
	private TypeImpl type1;
	private TypeImpl type2;

	@BeforeEach
	void setUp() {
		instance = new ConstantPools();
		type1 = Mockito.mock(TypeImpl.class);
		type2 = Mockito.mock(TypeImpl.class);
	}

	@Test
	void forTypeWithCP() {
		Mockito.when(type1.hasConstantPool()).thenReturn(true);
		assertNotNull(instance.forType(type1));
	}

	@Test
	void forTypeWithoutCP() {
		Mockito.when(type1.hasConstantPool()).thenReturn(false);
		assertThrows(IllegalArgumentException.class, () -> instance.forType(type1));
	}

	@Test
	void size() {
		assertEquals(0, instance.size());
		addConstantPool();
		assertEquals(2, instance.size());
	}

	@Test
	void iteratorEmpty() {
		Iterator<ConstantPool> iterator = instance.iterator();
		assertNotNull(iterator);
		assertFalse(iterator.hasNext());
	}

	@Test
	void iterator() {
		addConstantPool();
		Iterator<ConstantPool> iterator = instance.iterator();
		assertNotNull(iterator);
		assertTrue(iterator.hasNext());
		assertNotNull(iterator.next());
	}

	@Test
	void spliteratorEmpty() {
		Spliterator<ConstantPool> iterator = instance.spliterator();
		assertNotNull(iterator);
		AtomicReference<ConstantPool> pool = new AtomicReference<>();
		assertFalse(iterator.tryAdvance(pool::set));
		assertNull(pool.get());
	}

	@Test
	void spliterator() {
		addConstantPool();
		Spliterator<ConstantPool> iterator = instance.spliterator();
		assertNotNull(iterator);
		AtomicReference<ConstantPool> pool = new AtomicReference<>();
		assertTrue(iterator.tryAdvance(pool::set));
		assertNotNull(pool.get());
	}

	@Test
	void forEachEmpty() {
		AtomicInteger cntr = new AtomicInteger();
		instance.forEach(cp -> cntr.incrementAndGet());
		assertEquals(0, cntr.get());
	}

	@Test
	void forEach() {
		addConstantPool();
		AtomicInteger cntr = new AtomicInteger();
		instance.forEach(cp -> cntr.incrementAndGet());
		assertNotEquals(0, cntr.get());
	}

	private void addConstantPool() {
		Mockito.when(type1.hasConstantPool()).thenReturn(true);
		Mockito.when(type2.hasConstantPool()).thenReturn(true);
		instance.forType(type1);
		instance.forType(type2);
	}
}
