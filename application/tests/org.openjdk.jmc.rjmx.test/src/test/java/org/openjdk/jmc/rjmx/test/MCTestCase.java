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
package org.openjdk.jmc.rjmx.test;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Assume;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.common.version.JavaVMVersionToolkit;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.common.version.JavaVersionSupport;

/**
 * TestCase extended with convenience assert methods.
 */
@SuppressWarnings("nls")
public class MCTestCase {
	/**
	 * Shadowing {@link Assert#assertNull(String, Object)} to provide a more useful error message.
	 */
	static public void assertNull(String message, Object object) {
		if (object != null) {
			failNotEquals(message, null, object);
		}
	}

	/**
	 * Shadowing {@link Assert#assertNull(Object)} to provide a more useful error message.
	 */
	static public void assertNull(Object object) {
		if (object != null) {
			failNotEquals(null, null, object);
		}
	}

	static public void failNotEquals(String message, Object expected, Object actual) {
		if (!expected.equals(actual)) {
			Assert.fail(
					((message != null) ? message + ' ' : "") + "expected:<" + expected + "> but was:<" + actual + ">");
		}
	}

	/**
	 * Asserts that with respect to all bits in the mask, a long is equal to the given value. If it
	 * is not an AssertionFailedError is thrown with the given message.
	 */
	static public void assertMaskedEquals(String message, long expected, long actual, long mask) {
		if (((expected ^ actual) & mask) != 0) {
			Assert.fail(((message != null) ? message + ' ' : "") //$NON-NLS-1$
					+ "masked with " + hex(mask) + " expected:<" + hex(expected) + "> was not:<" + hex(actual) + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

	/**
	 * Asserts that with respect to all bits in the mask, a long is equal to the given value.
	 */
	static public void assertMaskedEquals(long expected, long actual, long mask) {
		assertMaskedEquals(null, expected, actual, mask);
	}

	protected static String hex(long val) {
		return "0x" + Long.toHexString(val); //$NON-NLS-1$
	}

	/**
	 * Asserts that a {@link Comparable} is within the given (inclusive) range. If it is not an
	 * AssertionFailedError is thrown with the given message.
	 */
	static public <T extends Comparable<T>> void assertBetween(String message, T min, T max, T actual) {
		if ((min.compareTo(actual) > 0) || (max.compareTo(actual) < 0)) {
			Assert.fail(((message != null) ? message + ' ' : "") //$NON-NLS-1$
					+ "expected in:[" + min + ", " + max + "] was not:<" + actual + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

	/**
	 * Asserts that a {@link Comparable} is within the given (inclusive) range.
	 */
	static public <T extends Comparable<T>> void assertBetween(T min, T max, T actual) {
		assertBetween(null, min, max, actual);
	}

	/**
	 * Asserts that a {@link Comparable} is less or equal than the given value. If it is not an
	 * AssertionFailedError is thrown with the given message.
	 */
	static public <T extends Comparable<T>> void assertMax(String message, T max, T actual) {
		if (max.compareTo(actual) < 0) {
			Assert.fail(((message != null) ? message + ' ' : "") //$NON-NLS-1$
					+ "expected max:<" + max + "> was not:<" + actual + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	/**
	 * Asserts that a {@link Comparable} is less or equal than the given value.
	 */
	static public <T extends Comparable<T>> void assertMax(T max, T actual) {
		assertMax(null, max, actual);
	}

	/**
	 * Asserts that a {@link Comparable} is greater or equal than the given value. If it is not an
	 * AssertionFailedError is thrown with the given message.
	 */
	static public <T extends Comparable<T>> void assertMin(String message, T min, T actual) {
		if (min.compareTo(actual) > 0) {
			Assert.fail(((message != null) ? message + ' ' : "") //$NON-NLS-1$
					+ "expected min:<" + min + "> was not:<" + actual + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	/**
	 * Asserts that a {@link Comparable} is greater or equal than the given value.
	 */
	static public <T extends Comparable<T>> void assertMin(T min, T actual) {
		assertMin(null, min, actual);
	}

	/**
	 * Asserts that a {@link Comparable} is less than the given value. If it is not an
	 * AssertionFailedError is thrown with the given message.
	 */
	static public <T extends Comparable<T>> void assertLessThan(String message, T greaterVal, T actual) {
		if (greaterVal.compareTo(actual) <= 0) {
			Assert.fail(((message != null) ? message + ' ' : "") //$NON-NLS-1$
					+ "expected less than:<" + greaterVal + "> was not:<" + actual + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	/**
	 * Asserts that a {@link Comparable} is less than the given value.
	 */
	static public <T extends Comparable<T>> void assertLessThan(T greaterVal, T actual) {
		assertLessThan(null, greaterVal, actual);
	}

	/**
	 * Asserts that a {@link Comparable} is greater than the given value. If it is not an
	 * AssertionFailedError is thrown with the given message.
	 */
	static public <T extends Comparable<? super T>> void assertGreaterThan(String message, T lesserVal, T actual) {
		if (lesserVal.compareTo(actual) >= 0) {
			Assert.fail(((message != null) ? message + ' ' : "") //$NON-NLS-1$
					+ "expected greater than:<" + lesserVal + "> was not:<" + actual + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	/**
	 * Asserts that a {@link Comparable} is greater than the given value.
	 */
	static public <T extends Comparable<? super T>> void assertGreaterThan(T lesserVal, T actual) {
		assertGreaterThan(null, lesserVal, actual);
	}

	public static void assertArrayEqualsWithMoreInfo(String message, Object[] expecteds, Object[] actuals) {
		try {
			assertArrayEquals(message, expecteds, actuals);
		} catch (AssertionError e) {
			StringBuilder sb = new StringBuilder();
			sb.append(e.getMessage());
			sb.append(" (Expected {");
			sb.append(StringToolkit.join(Arrays.asList(expecteds), ", ")).append("}");
			sb.append(", got {");
			sb.append(StringToolkit.join(Arrays.asList(actuals), ", ")).append("}");
			throw new AssertionError(sb.toString(), e);
		}
	}

	protected void skipIfEarlierThan8u0() {
		Assume.assumeTrue("This feature is only valid on JDK8u0 or later.", //$NON-NLS-1$
				(getClientVersion().compareTo(JVMVersion.JDK8)) >= 0);
	}

	protected void skipIfEarlierThan7u40() {
		Assume.assumeTrue("This feature is only valid on JDK7u40 or later.", //$NON-NLS-1$
				(getClientVersion().compareTo(JVMVersion.JDK7u40)) >= 0);
	}

	protected void skipIfEarlierThan7u4() {
		Assume.assumeTrue("This feature is only valid on JDK7u4 or later.", //$NON-NLS-1$
				(getClientVersion().compareTo(JVMVersion.JDK7u4)) >= 0);
	}

	protected void skipIfEarlierThan7u0() {
		Assume.assumeTrue("This feature is only valid on JDK7u0 or later.", //$NON-NLS-1$
				(getClientVersion().compareTo(JVMVersion.JDK7)) >= 0);
	}

	// This enum needs to be in the proper order.
	private enum JVMVersion {
		ANY, UNKNOWN, JRockit, JDK6, JDK7, JDK7u4, JDK7u40, JDK8
	}

	private JVMVersion getClientVersion() {
		if (JavaVMVersionToolkit.isJRockitJVMName(System.getProperty("java.vm.name"))) { //$NON-NLS-1$
			return JVMVersion.JRockit;
		} else if (JavaVMVersionToolkit.isHotspotJVMName(System.getProperty("java.vm.name"))) { //$NON-NLS-1$
			JavaVersion javaVersion = new JavaVersion(System.getProperty("java.version")); //$NON-NLS-1$
			if (javaVersion.isGreaterOrEqualThan(JavaVersionSupport.JDK_8)) {
				return JVMVersion.JDK8;
			} else if (javaVersion.isGreaterOrEqualThan(JavaVersionSupport.JDK_7_U_40)) {
				return JVMVersion.JDK7u40;
			} else if (javaVersion.isGreaterOrEqualThan(JavaVersionSupport.JDK_7_U_4)) {
				return JVMVersion.JDK7u4;
			} else if (javaVersion.isGreaterOrEqualThan(JavaVersionSupport.JDK_7)) {
				return JVMVersion.JDK7;
			} else if (javaVersion.isGreaterOrEqualThan(JavaVersionSupport.JDK_6)) {
				return JVMVersion.JDK6;
			}
		}
		return JVMVersion.UNKNOWN;
	}
}
