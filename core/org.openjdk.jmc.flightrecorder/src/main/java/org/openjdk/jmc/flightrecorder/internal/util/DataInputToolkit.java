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
package org.openjdk.jmc.flightrecorder.internal.util;

public class DataInputToolkit {

	public static final byte BYTE_SIZE = 1;
	public static final byte BOOLEAN_SIZE = 1;
	public static final byte SHORT_SIZE = 2;
	public static final byte CHAR_SIZE = 2;
	public static final byte INTEGER_SIZE = 4;
	public static final byte LONG_SIZE = 8;
	public static final byte FLOAT_SIZE = 4;
	public static final byte DOUBLE_SIZE = 8;

	public static int readUnsignedByte(byte[] bytes, int offset) {
		return bytes[offset] & 0xFF;
	}

	public static byte readByte(byte[] bytes, int offset) {
		return bytes[offset];
	}

	public static int readUnsignedShort(byte[] bytes, int offset) {
		int ch1 = (bytes[offset] & 0xff);
		int ch2 = (bytes[offset + 1] & 0xff);
		return (ch1 << 8) + (ch2 << 0);
	}

	public static short readShort(byte[] bytes, int offset) {
		return (short) readUnsignedShort(bytes, offset);
	}

	public static char readChar(byte[] bytes, int offset) {
		return (char) readUnsignedShort(bytes, offset);
	}

	public static long readUnsignedInt(byte[] bytes, int index) {
		return readInt(bytes, index) & 0xffffffffL;
	}

	public static int readInt(byte[] bytes, int index) {
		int ch1 = (bytes[index] & 0xff);
		int ch2 = (bytes[index + 1] & 0xff);
		int ch3 = (bytes[index + 2] & 0xff);
		int ch4 = (bytes[index + 3] & 0xff);
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}

	public static long readLong(byte[] bytes, int index) {
		return (((long) bytes[index + 0] << 56) + ((long) (bytes[index + 1] & 255) << 48)
				+ ((long) (bytes[index + 2] & 255) << 40) + ((long) (bytes[index + 3] & 255) << 32)
				+ ((long) (bytes[index + 4] & 255) << 24) + ((bytes[index + 5] & 255) << 16)
				+ ((bytes[index + 6] & 255) << 8) + ((bytes[index + 7] & 255) << 0));
	}

	public static float readFloat(byte[] bytes, int offset) {
		return Float.intBitsToFloat(readInt(bytes, offset));
	}

	public static double readDouble(byte[] bytes, int offset) {
		return Double.longBitsToDouble(readLong(bytes, offset));
	}

	public static boolean readBoolean(byte[] bytes, int offset) {
		return bytes[offset] != 0;
	}
}
