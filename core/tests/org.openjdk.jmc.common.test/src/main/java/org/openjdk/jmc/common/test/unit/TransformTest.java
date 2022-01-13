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
package org.openjdk.jmc.common.test.unit;

import static org.openjdk.jmc.common.unit.BinaryPrefix.EXBI;
import static org.openjdk.jmc.common.unit.BinaryPrefix.KIBI;
import static org.openjdk.jmc.common.unit.BinaryPrefix.NOBI;
import static org.openjdk.jmc.common.unit.BinaryPrefix.TEBI;
import static org.openjdk.jmc.common.unit.BinaryPrefix.YOBI;
import static org.openjdk.jmc.common.unit.DecimalPrefix.EXA;
import static org.openjdk.jmc.common.unit.DecimalPrefix.KILO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.NONE;
import static org.openjdk.jmc.common.unit.DecimalPrefix.YOTTA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openjdk.jmc.common.test.MCTestCase;
import org.openjdk.jmc.common.unit.BinaryScaleFactor;
import org.openjdk.jmc.common.unit.DecimalScaleFactor;
import org.openjdk.jmc.common.unit.IScalarAffineTransform;
import org.openjdk.jmc.common.unit.ImpreciseScaleFactor;
import org.openjdk.jmc.common.unit.ScaleFactor;

@SuppressWarnings("nls")
public class TransformTest extends MCTestCase {

	@Test
	public void testBinaryUnityEqualsDecimal() {
		BinaryScaleFactor bin1 = BinaryScaleFactor.get(0);
		DecimalScaleFactor dec1 = DecimalScaleFactor.get(0);
		assertTrue(bin1.isUnity());
		assertTrue(dec1.isUnity());
		assertEquals(bin1, dec1);
		assertEquals(dec1, bin1);
		assertEquals(bin1.hashCode(), dec1.hashCode());
	}

	@Test
	public void testLinearScaling() {
		ScaleFactor mmToM = new ImpreciseScaleFactor(1.0 / 1000);
		assertEquals(1, mmToM.targetValue(1000));
	}

	public static void assertInRange(long value, IScalarAffineTransform factor) {
		assertFalse("Out of range :" + factor.targetValue(value), factor.targetOutOfRange(value, Long.MAX_VALUE));
	}

	public static void assertInRange(double value, IScalarAffineTransform factor) {
		assertFalse("Out of range :" + factor.targetValue(value), factor.targetOutOfRange(value, Long.MAX_VALUE));
	}

	public static void assertOutOfRange(long value, IScalarAffineTransform factor) {
		assertTrue("Not out of range :" + factor.targetValue(value), factor.targetOutOfRange(value, Long.MAX_VALUE));
	}

	public static void assertOutOfRange(double value, IScalarAffineTransform factor) {
		assertTrue("Not out of range :" + factor.targetValue(value), factor.targetOutOfRange(value, Long.MAX_VALUE));
	}

	public static void assertInIntRange(long value, IScalarAffineTransform factor) {
		assertFalse("Out of range :" + factor.targetValue(value), factor.targetOutOfRange(value, Integer.MAX_VALUE));
	}

	public static void assertInIntRange(double value, IScalarAffineTransform factor) {
		assertFalse("Out of range :" + factor.targetValue(value), factor.targetOutOfRange(value, Integer.MAX_VALUE));
	}

	public static void assertOutOfIntRange(long value, IScalarAffineTransform factor) {
		assertTrue("Not out of range :" + factor.targetValue(value), factor.targetOutOfRange(value, Integer.MAX_VALUE));
	}

	public static void assertOutOfIntRange(double value, IScalarAffineTransform factor) {
		assertTrue("Not out of range :" + factor.targetValue(value), factor.targetOutOfRange(value, Integer.MAX_VALUE));
	}

	@Test
	public void testBinaryTooHighLong() {
		BinaryScaleFactor factor = YOBI.valueFactorTo(KIBI);
		assertInRange(0, factor);
		assertOutOfRange(17, factor);
		factor = TEBI.valueFactorTo(KIBI);
		assertInIntRange(1, factor);
		assertInRange(2, factor);
		// Test sign asymmetry, -2 is ok, but not 2.
		assertOutOfIntRange(2, factor);
		assertOutOfIntRange(3, factor);
		assertInRange(1L << 32, factor);
		// Test sign asymmetry, -(2^33) is ok, but not 2^33.
		assertInRange((1L << 33) - 1, factor);
		assertOutOfRange(1L << 33, factor);
		assertOutOfRange(1L << 34, factor);
		assertEquals(1L << 62, factor.targetValue(1L << 32));
		assertEquals(Long.MAX_VALUE, factor.targetValue(1L << 33));
		assertEquals(Long.MAX_VALUE, factor.targetValue(1L << 34));
	}

	@Test
	public void testBinaryTooHighDouble() {
		BinaryScaleFactor factor = YOBI.valueFactorTo(KIBI);
		assertInRange(0.0, factor);
		assertOutOfRange(17.0, factor);
		factor = TEBI.valueFactorTo(KIBI);
		assertInIntRange(1.0, factor);
		assertInRange(2.0, factor);
		// Test sign asymmetry, -2.0 is ok, but not 2.0.
		assertOutOfIntRange(2.0, factor);
		assertOutOfIntRange(3.0, factor);
		assertInRange((double) (1L << 32), factor);
		// Test sign asymmetry, -(2^33) is ok, but not 2^33.
		assertInRange((double) ((1L << 33) - 1), factor);
		assertOutOfRange((double) (1L << 33), factor);
		assertOutOfRange((double) (1L << 34), factor);
		assertEquals(1L << 62, (long) factor.targetValue((double) (1L << 32)));
		assertEquals(Long.MAX_VALUE, (long) factor.targetValue((double) (1L << 33)));
		assertEquals(Long.MAX_VALUE, (long) factor.targetValue((double) (1L << 34)));
	}

	@Test
	public void testBinaryTooLowLong() {
		BinaryScaleFactor factor = YOBI.valueFactorTo(KIBI);
		assertInRange(-0, factor);
		assertOutOfRange(-17, factor);
		factor = TEBI.valueFactorTo(KIBI);
		assertInIntRange(-1, factor);
		assertInRange(-2, factor);
		// Test sign asymmetry, -2 is ok, but not 2.
		assertInIntRange(-2, factor);
		assertOutOfIntRange(-3, factor);
		assertInRange(-(1L << 32), factor);
		// Test sign asymmetry, -(2^33) is ok, but not 2^33.
		assertInRange(-(1L << 33), factor);
		assertOutOfRange(-(1L << 33) - 1, factor);
		assertOutOfRange(-(1L << 34), factor);
		assertEquals(-(1L << 62), factor.targetValue(-(1L << 32)));
		assertEquals(Long.MIN_VALUE, factor.targetValue(-(1L << 33)));
		assertEquals(Long.MIN_VALUE, factor.targetValue(-(1L << 34)));
	}

	@Test
	public void testBinaryTooLowDouble() {
		BinaryScaleFactor factor = YOBI.valueFactorTo(KIBI);
		assertInRange(-0.0, factor);
		assertOutOfRange(-17.0, factor);
		factor = TEBI.valueFactorTo(KIBI);
		assertInIntRange(-1.0, factor);
		assertInRange(-2.0, factor);
		// Test sign asymmetry, -2.0 is ok, but not 2.0.
		assertInIntRange(-2.0, factor);
		assertOutOfIntRange(-3.0, factor);
		assertInRange((double) -(1L << 32), factor);
		// Test sign asymmetry, -(2^33) is ok, but not 2^33.
		assertInRange((double) -(1L << 33), factor);
		assertOutOfRange((double) (-(1L << 33) - 1), factor);
		assertOutOfRange((double) -(1L << 34), factor);
		assertEquals(-(1L << 62), (long) factor.targetValue((double) -(1L << 32)));
		assertEquals(Long.MIN_VALUE, (long) factor.targetValue((double) -(1L << 33)));
		assertEquals(Long.MIN_VALUE, (long) factor.targetValue((double) -(1L << 34)));
	}

	@Test
	public void testBinaryNotTooHighLong() {
		BinaryScaleFactor factor = EXBI.valueFactorTo(KIBI);
		assertInRange(0, factor);
		assertInRange(17, factor);
	}

	@Test
	public void testBinaryNotTooHighDouble() {
		BinaryScaleFactor factor = EXBI.valueFactorTo(KIBI);
		assertInRange(0.0, factor);
		assertInRange(17.0, factor);
	}

	@Test
	public void testBinaryNotTooLowLong() {
		BinaryScaleFactor factor = EXBI.valueFactorTo(KIBI);
		assertInRange(-0, factor);
		assertInRange(-17, factor);
	}

	@Test
	public void testBinaryNotTooLowDouble() {
		BinaryScaleFactor factor = EXBI.valueFactorTo(KIBI);
		assertInRange(-0.0, factor);
		assertInRange(-17.0, factor);
	}

	@Test
	public void testBinaryPrecisionLong() {
		BinaryScaleFactor factor = NOBI.valueFactorTo(KIBI);
		assertInRange(0, factor);
		assertInRange(1023, factor);
		assertInRange(1024, factor);
		assertInRange(1025, factor);
		assertInIntRange(1023, factor);
		assertInIntRange(1024, factor);
		assertInIntRange(1025, factor);
	}

	@Test
	public void testBinaryPrecisionNegativeLong() {
		BinaryScaleFactor factor = NOBI.valueFactorTo(KIBI);
		assertInRange(-0, factor);
		assertInRange(-1023, factor);
		assertInRange(-1024, factor);
		assertInRange(-1025, factor);
		assertInIntRange(-1023, factor);
		assertInIntRange(-1024, factor);
		assertInIntRange(-1025, factor);
	}

	@Test
	public void testBinaryPrecisionDouble() {
		BinaryScaleFactor factor = NOBI.valueFactorTo(KIBI);
		assertInRange(0.0, factor);
		assertInRange(1023.0, factor);
		assertInRange(1024.0, factor);
		assertInRange(1025.0, factor);
		assertInIntRange(1023.0, factor);
		assertInIntRange(1024.0, factor);
		assertInIntRange(1025.0, factor);
	}

	@Test
	public void testBinaryPrecisionNegativeDouble() {
		BinaryScaleFactor factor = NOBI.valueFactorTo(KIBI);
		assertInRange(-0.0, factor);
		assertInRange(-1023.0, factor);
		assertInRange(-1024.0, factor);
		assertInRange(-1025.0, factor);
		assertInIntRange(-1023.0, factor);
		assertInIntRange(-1024.0, factor);
		assertInIntRange(-1025.0, factor);
	}

	@Test
	public void testDecimalTooHighLong() {
		DecimalScaleFactor factor = YOTTA.valueFactorTo(KILO);
		assertInRange(0, factor);
		assertOutOfRange(17, factor);
	}

	@Test
	public void testDecimalTooHighDouble() {
		DecimalScaleFactor factor = YOTTA.valueFactorTo(KILO);
		assertInRange(0.0, factor);
		assertOutOfRange(17.0, factor);
	}

	@Test
	public void testDecimalTooLowLong() {
		DecimalScaleFactor factor = YOTTA.valueFactorTo(KILO);
		assertInRange(-0, factor);
		assertOutOfRange(-17, factor);
	}

	@Test
	public void testDecimalTooLowDouble() {
		DecimalScaleFactor factor = YOTTA.valueFactorTo(KILO);
		assertInRange(-0.0, factor);
		assertOutOfRange(-17.0, factor);
	}

	@Test
	public void testDecimalNotTooHighLong() {
		DecimalScaleFactor factor = EXA.valueFactorTo(KILO);
		assertInRange(0, factor);
		assertInRange(17, factor);
	}

	@Test
	public void testDecimalNotTooHighDouble() {
		DecimalScaleFactor factor = EXA.valueFactorTo(KILO);
		assertInRange(0.0, factor);
		assertInRange(17.0, factor);
	}

	@Test
	public void testDecimalNotTooLowLong() {
		DecimalScaleFactor factor = EXA.valueFactorTo(KILO);
		assertInRange(-0, factor);
		assertInRange(-17, factor);
	}

	@Test
	public void testDecimalNotTooLowDouble() {
		DecimalScaleFactor factor = EXA.valueFactorTo(KILO);
		assertInRange(-0.0, factor);
		assertInRange(-17.0, factor);
	}

	@Test
	public void testBinaryRounding() {
		BinaryScaleFactor factor = NOBI.valueFactorTo(KIBI);
		assertEquals(0, factor.targetValue(511));
		assertEquals(1, factor.targetValue(512));
		assertEquals(1, factor.targetValue(513));
		assertEquals(1, factor.targetValue(1023));
		assertEquals(1, factor.targetValue(1024));
		assertEquals(1, factor.targetValue(1025));
		assertEquals(0, factor.targetValue(-511));
		assertEquals(0, factor.targetValue(-512));
		assertEquals(-1, factor.targetValue(-513));
		assertEquals(-1, factor.targetValue(-1023));
		assertEquals(-1, factor.targetValue(-1024));
		assertEquals(-1, factor.targetValue(-1025));
	}

	@Test
	public void testBinaryFlooring() {
		BinaryScaleFactor factor = NOBI.valueFactorTo(KIBI);
		assertEquals(0, factor.targetFloor(511));
		assertEquals(0, factor.targetFloor(512));
		assertEquals(0, factor.targetFloor(513));
		assertEquals(0, factor.targetFloor(1023));
		assertEquals(1, factor.targetFloor(1024));
		assertEquals(1, factor.targetFloor(1025));
		assertEquals(-1, factor.targetFloor(-511));
		assertEquals(-1, factor.targetFloor(-512));
		assertEquals(-1, factor.targetFloor(-513));
		assertEquals(-1, factor.targetFloor(-1023));
		assertEquals(-1, factor.targetFloor(-1024));
		assertEquals(-2, factor.targetFloor(-1025));
	}

	@Test
	public void testDecimalRounding() {
		DecimalScaleFactor factor = NONE.valueFactorTo(KILO);
		assertEquals(0, factor.targetValue(499));
		assertEquals(1, factor.targetValue(500));
		assertEquals(1, factor.targetValue(501));
		assertEquals(1, factor.targetValue(999));
		assertEquals(1, factor.targetValue(1000));
		assertEquals(1, factor.targetValue(1001));
		assertEquals(0, factor.targetValue(-499));
		assertEquals(0, factor.targetValue(-500));
		assertEquals(-1, factor.targetValue(-501));
		assertEquals(-1, factor.targetValue(-999));
		assertEquals(-1, factor.targetValue(-1000));
		assertEquals(-1, factor.targetValue(-1001));
	}

	@Test
	public void testDecimalFlooring() {
		DecimalScaleFactor factor = NONE.valueFactorTo(KILO);
		assertEquals(0, factor.targetFloor(499));
		assertEquals(0, factor.targetFloor(500));
		assertEquals(0, factor.targetFloor(501));
		assertEquals(0, factor.targetFloor(999));
		assertEquals(1, factor.targetFloor(1000));
		assertEquals(1, factor.targetFloor(1001));
		assertEquals(-1, factor.targetFloor(-499));
		assertEquals(-1, factor.targetFloor(-500));
		assertEquals(-1, factor.targetFloor(-501));
		assertEquals(-1, factor.targetFloor(-999));
		assertEquals(-1, factor.targetFloor(-1000));
		assertEquals(-2, factor.targetFloor(-1001));
	}
}
