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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@SuppressWarnings("restriction")
class ConstantPoolTest {
	private ConstantPool instance;

	@BeforeEach
	void setUp() {
		TypeImpl type = Mockito.mock(TypeImpl.class);
		Mockito.when(type.canAccept(ArgumentMatchers.any())).thenReturn(true);

		TypedValueImpl nullValue = new TypedValueImpl(type, null, 0);

		Mockito.when(type.nullValue()).thenReturn(nullValue);

		instance = new ConstantPool(type);
	}

	@Test
	void addOrGetNull() {
		TypedValueImpl value = instance.addOrGet(null);
		assertNotNull(value);
		assertTrue(value.isNull());
	}

	@Test
	void addOrGetNonNull() {
		Object objectValue = "hello";
		TypedValueImpl value = instance.addOrGet(objectValue);
		assertNotNull(value);
		assertFalse(value.isNull());
		assertEquals(objectValue, value.getValue());
	}

	@Test
	void getNegativeIndex() {
		assertNull(instance.get(-1));
	}

	@Test
	void getNonExistent() {
		assertNull(instance.get(100));
	}

	@Test
	void get() {
		Object objectValue = "hello";
		TypedValueImpl value = instance.addOrGet(objectValue);

		assertEquals(value, instance.get(value.getConstantPoolIndex()));
	}
}
