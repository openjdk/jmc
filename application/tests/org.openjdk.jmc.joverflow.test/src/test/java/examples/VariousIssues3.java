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
package examples;

/**
 * A simple piece of code that generates a bunch of problematic data structures and then goes into
 * sleep for long enough for the user to take a heap dump. The resulting dump is stored alongside
 * the test source files and is analyzed in org.openjdk.jmc.joverflow.stats.VariousIssuesTest3.
 * <p>
 * It doesn't matter whether this example runs in 32-bit or 64-bit mode.
 */
@SuppressWarnings("unused")
public class VariousIssues3 {

	public static final int N_UINTS_INSTANCES = 50000;
	public static final int N_ULONG_INSTANCES = 10000;
	public static final int N_BAD_ULONG_INSTANCES = N_ULONG_INSTANCES * 95 / 100;

	public UnderusedInts[] underusedInts3Bytes;

	public UnderusedIntsAndChars[] underusedIntsAndChars;

	public UnderusedLongs[] underusedLongs;

	public static class UnderusedInts {
		private int intUnused3Bytes;
		private boolean boolField;

		private UnderusedInts(int i) {
			this.intUnused3Bytes = i;
		}
	}

	public static class UnderusedIntsAndChars {
		private int intUnused2Bytes;
		private char charUnused1Byte;
		private short shortUnusedAtAll; // Always 0 - shouldn't be reported as unused high bytes

		private UnderusedIntsAndChars(int i, char c) {
			this.intUnused2Bytes = i;
			this.charUnused1Byte = c;
		}
	}

	public static class UnderusedLongs {
		private long longUnused6Bytes; // 6 or more bytes unused in 95% of instances, others use all
		private int intUnusedAtAll; // Always 0 - shouldn't be reported as unused high bytes

		private UnderusedLongs(long l) {
			this.longUnused6Bytes = l;
		}
	}

	/** Main method that initializes all data and goes into sleep */
	public static void main(String args[]) {
		VariousIssues3 v = new VariousIssues3();

		ExampleUtils.printPidAndSleep("various-issues3.hprof");
	}

	/** Made public to be used in the same test that analyzes the heap dump */
	public VariousIssues3() {
		constructDataWithUnusedHiBytes();
	}

	private void constructDataWithUnusedHiBytes() {
		underusedInts3Bytes = new UnderusedInts[N_UINTS_INSTANCES];
		for (int i = 0; i < N_UINTS_INSTANCES; i++) {
			underusedInts3Bytes[i] = new UnderusedInts(i & Byte.MAX_VALUE);
		}

		underusedIntsAndChars = new UnderusedIntsAndChars[N_UINTS_INSTANCES];
		for (int i = 0; i < N_UINTS_INSTANCES; i++) {
			underusedIntsAndChars[i] = new UnderusedIntsAndChars(i & Short.MAX_VALUE, (char) (i & Byte.MAX_VALUE));
		}

		underusedLongs = new UnderusedLongs[N_ULONG_INSTANCES];
		for (int i = 0; i < N_BAD_ULONG_INSTANCES; i++) {
			underusedLongs[i++] = new UnderusedLongs(i & Short.MAX_VALUE);
			underusedLongs[i] = new UnderusedLongs(0);
		}
		for (int i = N_BAD_ULONG_INSTANCES; i < N_ULONG_INSTANCES; i++) {
			underusedLongs[i] = new UnderusedLongs(Long.MAX_VALUE - i);
		}
	}
}
