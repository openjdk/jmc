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
package org.openjdk.jmc.common.test.mock.item;

import java.text.NumberFormat;
import java.util.Random;

import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.IItemCollection;

public class MockCollections {
	public static IItemCollection getNumberCollection(Number[] values) {
		return new MockItemCollection<Number, MockNumberType>(values, new MockNumberType());
	}

	/**
	 * Will contain the exact same array of n number of double values between 0 and X between
	 * invocations.
	 */
	public static Number[] generateNumberArray(int n, double x) {
		Number[] array = new Number[n];
		Random RND = new Random(4711);
		for (int i = 0; i < array.length; i++) {
			array[i] = RND.nextDouble() * x;
		}
		return array;
	}

	/**
	 * Used to verify statistical calculations in external application.
	 */
	public static String generateFullPrecisionString(Number[] values) {
		NumberFormat formatter = NumberFormat.getInstance();
		formatter.setMaximumFractionDigits(340);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			builder.append(formatter.format(values[i]));
			builder.append("\n");
		}
		return builder.toString();
	}

	public static IItemCollection getStackTraceCollection(IMCStackTrace[] traces) {
		return new MockItemCollection<IMCStackTrace, MockStacktraceType>(traces, new MockStacktraceType());
	}

	public static void main(String[] args) {
		System.out.println(generateFullPrecisionString(generateNumberArray(5, 5)));
	}
}
