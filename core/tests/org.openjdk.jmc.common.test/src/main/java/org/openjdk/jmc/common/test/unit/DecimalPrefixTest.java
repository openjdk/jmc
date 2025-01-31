/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.common.test.unit;

import org.junit.Test;
import org.openjdk.jmc.common.unit.DecimalPrefix;
import org.openjdk.jmc.test.MCTestCase;

import static org.junit.Assert.assertEquals;
import static org.openjdk.jmc.common.unit.DecimalPrefix.NONE;
import static org.openjdk.jmc.common.unit.DecimalPrefix.QUECTO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.QUETTA;
import static org.openjdk.jmc.common.unit.DecimalPrefix.RONTO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.RONNA;
import static org.openjdk.jmc.common.unit.DecimalPrefix.YOCTO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.YOTTA;
import static org.openjdk.jmc.common.unit.DecimalPrefix.ZEPTO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.ZETTA;

public class DecimalPrefixTest extends MCTestCase {
	@Test
	public void testZero() {
		assertEquals(0, DecimalPrefix.getFloorLog1000(0));
		assertEquals(NONE, DecimalPrefix.getEngFloorPrefix(0));
	}

	@Test
	public void testQuecto() {
		double quectos = 2 * Math.pow(10, -30);
		assertEquals(-10, DecimalPrefix.getFloorLog1000(quectos));
		assertEquals(QUECTO, DecimalPrefix.getEngFloorPrefix(quectos));
		assertEquals(-10, DecimalPrefix.getFloorLog1000(-quectos));
		assertEquals(QUECTO, DecimalPrefix.getEngFloorPrefix(-quectos));
	}

	@Test
	public void testRonto() {
		double rontos = 2 * Math.pow(10, -27);
		assertEquals(-9, DecimalPrefix.getFloorLog1000(rontos));
		assertEquals(RONTO, DecimalPrefix.getEngFloorPrefix(rontos));
		assertEquals(-9, DecimalPrefix.getFloorLog1000(-rontos));
		assertEquals(RONTO, DecimalPrefix.getEngFloorPrefix(-rontos));
	}

	@Test
	public void testYocto() {
		double yoctos = 2 * Math.pow(10, -24);
		assertEquals(-8, DecimalPrefix.getFloorLog1000(yoctos));
		assertEquals(YOCTO, DecimalPrefix.getEngFloorPrefix(yoctos));
		assertEquals(-8, DecimalPrefix.getFloorLog1000(-yoctos));
		assertEquals(YOCTO, DecimalPrefix.getEngFloorPrefix(-yoctos));
	}

	@Test
	public void testZepto() {
		double zeptos = 2 * Math.pow(10, -21);
		assertEquals(-7, DecimalPrefix.getFloorLog1000(zeptos));
		assertEquals(ZEPTO, DecimalPrefix.getEngFloorPrefix(zeptos));
		assertEquals(-7, DecimalPrefix.getFloorLog1000(-zeptos));
		assertEquals(ZEPTO, DecimalPrefix.getEngFloorPrefix(-zeptos));
	}

	@Test
	public void testQuetta() {
		double quettas = 2 * Math.pow(10, 30);
		assertEquals(10, DecimalPrefix.getFloorLog1000(quettas));
		assertEquals(QUETTA, DecimalPrefix.getEngFloorPrefix(quettas));
		assertEquals(10, DecimalPrefix.getFloorLog1000(-quettas));
		assertEquals(QUETTA, DecimalPrefix.getEngFloorPrefix(-quettas));
	}

	@Test
	public void testRonna() {
		double ronnas = 2 * Math.pow(10, 27);
		assertEquals(9, DecimalPrefix.getFloorLog1000(ronnas));
		assertEquals(RONNA, DecimalPrefix.getEngFloorPrefix(ronnas));
		assertEquals(9, DecimalPrefix.getFloorLog1000(-ronnas));
		assertEquals(RONNA, DecimalPrefix.getEngFloorPrefix(-ronnas));
	}

	@Test
	public void testYotta() {
		double yottas = 2 * Math.pow(10, 24);
		assertEquals(8, DecimalPrefix.getFloorLog1000(yottas));
		assertEquals(YOTTA, DecimalPrefix.getEngFloorPrefix(yottas));
		assertEquals(8, DecimalPrefix.getFloorLog1000(-yottas));
		assertEquals(YOTTA, DecimalPrefix.getEngFloorPrefix(-yottas));
	}

	@Test
	public void testZetta() {
		double zettas = 2 * Math.pow(10, 21);
		assertEquals(7, DecimalPrefix.getFloorLog1000(zettas));
		assertEquals(ZETTA, DecimalPrefix.getEngFloorPrefix(zettas));
		assertEquals(7, DecimalPrefix.getFloorLog1000(-zettas));
		assertEquals(ZETTA, DecimalPrefix.getEngFloorPrefix(-zettas));
	}
}
