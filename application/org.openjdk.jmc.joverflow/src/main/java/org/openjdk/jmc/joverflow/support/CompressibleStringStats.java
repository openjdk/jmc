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
package org.openjdk.jmc.joverflow.support;

/**
 * Container for statistics on Strings that are, or can be, "compressed" - that is, their backing
 * char[] array can be replaced with byte[], because all the characters in it are ASCII.
 * <p>
 * In reality, there is a subtle issue when a heap dump from JDK version earlier than JDK7u6 is
 * used, where several String objects can point to the same backing array and furthermore can use
 * different or overlapping parts of it. In this situation there may be no good way to calculate the
 * number of chars (bytes) backing certain String objects. Currently when making calculations we
 * look only at the chars (bytes) that logically belong to the string (i.e. we may look only at a
 * part of the backing array). Furthermore, when calculating all totals, we add up the numbers of
 * chars (bytes) equal to String.length(), and we add zero if the given backing array has already
 * been seen before, from another String object. As a result, the number of "used backing array
 * bytes" may end up considerably smaller than the total number of bytes in all arrays backing
 * Strings.
 */
public class CompressibleStringStats {
	/** Total number of String objects */
	public final int nTotalStrings;
	/** Total number of backing char[] arrays */
	public final int nBackingCharArrays;

	/** Used bytes in backing arrays, calculated as explained in class-level comments */
	public final long totalUsedBackingArrayBytes;
	/** Number of String objects backed by byte[] arrays */
	public final int nCompressedStrings;
	/** Number of used bytes in backing byte[] arrays */
	public final long compressedBackingArrayBytes;
	/** Number of String objects backed by char[] arrays with ASCII chars only */
	public final int nAsciiCharBackedStrings;
	/** Number of used in backing char[] arrays with ASCII chars only */
	public final long asciiCharBackingArrayBytes;

	public CompressibleStringStats(int nTotalStrings, int nBackingCharArrays, long totalUsedBackingArrayBytes,
			int nCompressedStrings, long compressedBackingArrayBytes, int nAsciiCharBackedStrings,
			long asciiCharBackingArrayBytes) {
		this.nTotalStrings = nTotalStrings;
		this.nBackingCharArrays = nBackingCharArrays;
		this.totalUsedBackingArrayBytes = totalUsedBackingArrayBytes;
		this.nCompressedStrings = nCompressedStrings;
		this.compressedBackingArrayBytes = compressedBackingArrayBytes;
		this.nAsciiCharBackedStrings = nAsciiCharBackedStrings;
		this.asciiCharBackingArrayBytes = asciiCharBackingArrayBytes;
	}
}
