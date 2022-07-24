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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmc.common.test.MCTestCase;
import org.openjdk.jmc.common.unit.DecimalScaleFactor;
import org.openjdk.jmc.common.unit.IScalarAffineTransform;
import org.openjdk.jmc.common.unit.SimpleAffineTransform;

public class AffineTransformTest extends MCTestCase {

	private IScalarAffineTransform milliCelsiusToCelsius;
	private IScalarAffineTransform celsiusToKelvin;
	private IScalarAffineTransform kelvinToCelsius;
	private IScalarAffineTransform celsiusToFahrenheit;
	private IScalarAffineTransform fahrenheitToCelsius;

	@Before
	public void setUp() throws Exception {
		milliCelsiusToCelsius = DecimalScaleFactor.get(-3);
		celsiusToKelvin = new SimpleAffineTransform(1, 273.15);
		kelvinToCelsius = celsiusToKelvin.invert();
		celsiusToFahrenheit = new SimpleAffineTransform(1.8, 32);
		fahrenheitToCelsius = celsiusToFahrenheit.invert();
	}

	private static void assertTransform(double expectedValue, IScalarAffineTransform transform, double sourceValue) {
		double ulp = Math.ulp(expectedValue);
		Assert.assertEquals(expectedValue, transform.targetValue(sourceValue), ulp);
	}

	@Test
	public void testOffsetTransform() {
		assertTransform(273.15 + 25, celsiusToKelvin, 25.0);
		assertTransform(273.15 + 100, celsiusToKelvin, 100.0);
	}

	@Test
	public void testInverseOffsetTransform() {
		assertTransform(25.0, kelvinToCelsius, 273.15 + 25.0);
		assertTransform(100.0, kelvinToCelsius, 273.15 + 100.0);
	}

	@Test
	public void testAffineTransform() {
		assertTransform(32, celsiusToFahrenheit, 0);
		assertTransform(98.6, celsiusToFahrenheit, 37.0);
		assertTransform(100.0, celsiusToFahrenheit, 37.7777777777777777777);
	}

	@Test
	public void testInverseAffineTransform() {
		assertTransform(0, fahrenheitToCelsius, 32);
		assertTransform(37.0, fahrenheitToCelsius, 98.6);
		assertTransform(37.7777777777777777777, fahrenheitToCelsius, 100.0);
	}

	@Test
	public void testConcatTransform() {
		IScalarAffineTransform fahrenheitToKelvin = celsiusToKelvin.concat(fahrenheitToCelsius);
		assertTransform(273.15, fahrenheitToKelvin, 32);
		assertTransform(273.15 + 37, fahrenheitToKelvin, 98.6);
		IScalarAffineTransform milliCelsiusToKelvin = celsiusToKelvin.concat(milliCelsiusToCelsius);
		assertTransform(273.15 + 0.007, milliCelsiusToKelvin, 7);
	}

	@Test
	public void testInvertAndConcatTransform() {
		IScalarAffineTransform fahrenheitToKelvin = kelvinToCelsius.invertAndConcat(fahrenheitToCelsius);
		assertTransform(273.15, fahrenheitToKelvin, 32);
		assertTransform(273.15 + 37, fahrenheitToKelvin, 98.6);
		IScalarAffineTransform milliCelsiusToKelvin = kelvinToCelsius.invertAndConcat(milliCelsiusToCelsius);
		assertTransform(273.15 + 0.007, milliCelsiusToKelvin, 7);

		IScalarAffineTransform kelvinToMilliCelsius = milliCelsiusToCelsius.invertAndConcat(kelvinToCelsius);
		assertTransform(7, kelvinToMilliCelsius, 273.15 + 0.007);
	}
}
