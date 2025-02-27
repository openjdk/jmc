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
package examples;

/**
 * A simple piece of code that generates a bunch of duplicated strings and then goes into sleep for
 * long enough for the user to take a heap dump. The resulting dump is stored alongside the test
 * source files and is analyzed in org.openjdk.jmc.joverflow.stats.StringDupTest.
 * <p>
 * IMPORTANT: a heap dump for this class should be generated by the JVM running in 32-bit mode.
 * Numbers in the test are based on pointer size etc. in this mode, where we know their exact values
 * and don't have to care about different object header size on different VMs, compressed
 * references, etc.
 */
@SuppressWarnings("unused")
public class DuplicateArrays {

	public static final int N_UNIQ_ARRAYS_OF_EACH_TYPE = 1000;
	public static final int N_SAME_ARRAYS_0 = 2000;
	public static final int N_SAME_ARRAYS_1 = 2700;
	public static final int N_SAME_ARRAYS_2 = 1600;
	public static final int N_SAME_ARRAYS_3 = 100;
	public static final int N_NONDUP_ARRAYS_1 = 150;
	public static final int N_NONDUP_ARRAYS_2 = 200;

	private byte[][] uniqueByteArrays;
	private int[][] uniqueIntArrays;
	private char[][] uniqueCharArrays;
	private double[][] uniqueDoubleArrays;

	// This array contains N_SAME_ARRAYS_0 different array objects.
	// They all have the same value, a copy of uniqueByteArrays[0]
	public final ArrayContainer[] dupByteArrays0;

	// This array contains N_SAME_ARRAYS_1 different array objects.
	// 1/3 of them a copy of uniqueIntArrays[0], 1/3 - uniqueIntArrays[1]
	// and 1/3 - uniqueIntArrays[2].
	public final ArrayContainer[] dupIntArrays1;

	// This array contains N_SAME_ARRAYS_2 different array objects,
	// half of them a copy of uniqueCharArrays[998], and half of
	// uniqueCharArrays[999]. In addition, it contains N_NONDUP_ARRAYS_2
	// non-dup arrays, each == to a separate array from the beginning
	// of uniqueCharArrays.
	public final ArrayContainer[] dupCharArrays2;

	// This array contains N_SAME_ARRAYS_3 different array objects.
	// They all have the same value, a copy of uniqueDoubleArrays[0]
	public final ArrayContainer[] dupDoubleArrays3;

	// We generate elements of each array out of individual characters of
	// strings, which in turn are generated from the prefix below and some
	// additional unique chars.
	// A prefix for each string, contains 50 characters
	public static final String PREFIX = "12345678901234567890123456789012345678901234567890";
	public final String[] uniqueStrings;
	public static final int ARRAY_LENGTH = PREFIX.length() + 8;

	public static void main(String args[]) {
		DuplicateArrays d = new DuplicateArrays();

		ExampleUtils.printPidAndSleep("duplicate-arrays.hprof");
	}

	public DuplicateArrays() {
		uniqueStrings = new String[N_UNIQ_ARRAYS_OF_EACH_TYPE];
		for (int i = 0; i < N_UNIQ_ARRAYS_OF_EACH_TYPE; i++) {
			uniqueStrings[i] = PREFIX + String.format("%8d", i);
		}

		initUniqueArrays();

		dupByteArrays0 = new ArrayContainer[N_SAME_ARRAYS_0];
		for (int i = 0; i < N_SAME_ARRAYS_0; i++) {
			byte[] ar = new byte[uniqueByteArrays[0].length];
			System.arraycopy(uniqueByteArrays[0], 0, ar, 0, uniqueByteArrays[0].length);
			dupByteArrays0[i] = new ArrayContainer(ar);
		}

		dupIntArrays1 = new ArrayContainer[N_SAME_ARRAYS_1 + N_NONDUP_ARRAYS_1];
		for (int i = 0; i < N_SAME_ARRAYS_1; i++) {
			int[] ar = new int[uniqueIntArrays[997].length];
			System.arraycopy(uniqueIntArrays[997], 0, ar, 0, uniqueIntArrays[997].length);
			dupIntArrays1[i++] = new ArrayContainer(ar);
			ar = new int[uniqueIntArrays[998].length];
			System.arraycopy(uniqueIntArrays[998], 0, ar, 0, uniqueIntArrays[998].length);
			dupIntArrays1[i++] = new ArrayContainer(ar);
			ar = new int[uniqueIntArrays[999].length];
			System.arraycopy(uniqueIntArrays[999], 0, ar, 0, uniqueIntArrays[999].length);
			dupIntArrays1[i] = new ArrayContainer(ar);
		}
		for (int i = N_SAME_ARRAYS_1, j = 0; i < N_SAME_ARRAYS_1 + N_NONDUP_ARRAYS_1; i++, j++) {
			dupIntArrays1[i] = new ArrayContainer(uniqueIntArrays[j]);
		}

		dupCharArrays2 = new ArrayContainer[N_SAME_ARRAYS_2 + N_NONDUP_ARRAYS_2];
		for (int i = 0; i < N_SAME_ARRAYS_2; i++) {
			char[] ar = new char[uniqueCharArrays[998].length];
			System.arraycopy(uniqueCharArrays[998], 0, ar, 0, uniqueCharArrays[998].length);
			dupCharArrays2[i++] = new ArrayContainer(ar);
			ar = new char[uniqueCharArrays[999].length];
			System.arraycopy(uniqueCharArrays[999], 0, ar, 0, uniqueCharArrays[999].length);
			dupCharArrays2[i] = new ArrayContainer(ar);
		}
		for (int i = N_SAME_ARRAYS_2, j = 0; i < N_SAME_ARRAYS_2 + N_NONDUP_ARRAYS_2; i++, j++) {
			dupCharArrays2[i] = new ArrayContainer(uniqueCharArrays[j]);
		}

		dupDoubleArrays3 = new ArrayContainer[N_SAME_ARRAYS_3];
		for (int i = 0; i < N_SAME_ARRAYS_3; i++) {
			double[] ar = new double[uniqueDoubleArrays[0].length];
			System.arraycopy(uniqueDoubleArrays[0], 0, ar, 0, uniqueDoubleArrays[0].length);
			dupDoubleArrays3[i] = new ArrayContainer(ar);
		}

		// Get rid of extra references to arrays, so that they don't distort our calculations
		uniqueByteArrays = null;
		uniqueCharArrays = null;
		uniqueIntArrays = null;
		uniqueDoubleArrays = null;
	}

	public static class ArrayContainer {
		public byte[] bytes;
		public int[] ints;
		public char[] chars;
		public double[] doubles;

		ArrayContainer(byte[] bytes) {
			this.bytes = bytes;
		}

		ArrayContainer(int[] ints) {
			this.ints = ints;
		}

		ArrayContainer(char[] chars) {
			this.chars = chars;
		}

		ArrayContainer(double[] doubles) {
			this.doubles = doubles;
		}
	}

	private void initUniqueArrays() {
		uniqueByteArrays = new byte[N_UNIQ_ARRAYS_OF_EACH_TYPE][];
		for (int i = 0; i < N_UNIQ_ARRAYS_OF_EACH_TYPE; i++) {
			String s = uniqueStrings[i];
			byte ar[] = new byte[s.length()];
			for (int j = 0; j < s.length(); j++) {
				ar[j] = (byte) s.charAt(j);
			}
			uniqueByteArrays[i] = ar;
		}

		uniqueIntArrays = new int[N_UNIQ_ARRAYS_OF_EACH_TYPE][];
		for (int i = 0; i < N_UNIQ_ARRAYS_OF_EACH_TYPE; i++) {
			String s = uniqueStrings[i];
			int ar[] = new int[s.length()];
			for (int j = 0; j < s.length(); j++) {
				ar[j] = s.charAt(j);
			}
			uniqueIntArrays[i] = ar;
		}

		uniqueCharArrays = new char[N_UNIQ_ARRAYS_OF_EACH_TYPE][];
		for (int i = 0; i < N_UNIQ_ARRAYS_OF_EACH_TYPE; i++) {
			String s = uniqueStrings[i];
			char ar[] = new char[s.length()];
			for (int j = 0; j < s.length(); j++) {
				ar[j] = s.charAt(j);
			}
			uniqueCharArrays[i] = ar;
		}

		uniqueDoubleArrays = new double[N_UNIQ_ARRAYS_OF_EACH_TYPE][];
		for (int i = 0; i < N_UNIQ_ARRAYS_OF_EACH_TYPE; i++) {
			String s = uniqueStrings[i];
			double ar[] = new double[s.length()];
			for (int j = 0; j < s.length(); j++) {
				ar[j] = s.charAt(j);
			}
			uniqueDoubleArrays[i] = ar;
		}

	}
}
