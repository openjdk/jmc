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
package org.openjdk.jmc.joverflow.util;

/**
 * Miscellaneous utility methods.
 */
public class MiscUtils {

	/**
	 * Returns object size that's padded to the specified alignment granularity (which is usually 8
	 * bytes).
	 */
	public static int getAlignedObjectSize(int size, int objAlignment) {
		if ((size & (objAlignment - 1)) == 0) {
			return size;
		} else {
			return (size & (~(objAlignment - 1))) + objAlignment;
		}
	}

	public static String toHex(long addr) {
		return "0x" + Long.toHexString(addr);
	}

	/**
	 * For a given string, replaces all occurrences of chars 10 and 13 with "\\n" and "\\r" string
	 * literals, and adds quotes before and after the string. Additionally, if maxLen is greater
	 * than zero, truncates the string to that length if it's longer, adding " ...[length xyz]" in
	 * the end
	 */
	public static String removeEndLinesAndAddQuotes(String s, int maxLen) {
		if (maxLen > 0 && s.length() > maxLen) { // Don't print very long strings fully
			s = s.substring(0, maxLen - 16) + " ...[length " + s.length() + ']';
		}

		if (s.indexOf('\n') == -1 && s.indexOf('\r') == -1) {
			return "\"" + s + "\"";
		}

		char[] dst = new char[s.length() * 2 + 2];
		dst[0] = '"';
		int dstIdx = 1;
		int len = s.length();

		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if (c == '\n') {
				dst[dstIdx++] = '\\';
				dst[dstIdx++] = 'n';
			} else if (c == '\r') {
				dst[dstIdx++] = '\\';
				dst[dstIdx++] = 'r';
			} else {
				dst[dstIdx++] = c;
			}
		}

		dst[dstIdx++] = '"';

		return new String(dst, 0, dstIdx);
	}

	public static String asCommaSeparatedList(String[] strings) {
		if (strings.length == 1) {
			return strings[0];
		}

		StringBuilder sb = new StringBuilder(strings.length * 10);
		sb.append(strings[0]);
		for (int i = 1; i < strings.length; i++) {
			sb.append(", ");
			sb.append(strings[i]);
		}
		return sb.toString();
	}
}
