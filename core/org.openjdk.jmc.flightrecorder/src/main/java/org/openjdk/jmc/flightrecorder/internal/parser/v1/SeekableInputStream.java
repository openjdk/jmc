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
package org.openjdk.jmc.flightrecorder.internal.parser.v1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.openjdk.jmc.flightrecorder.internal.util.DataInputToolkit;

/**
 * Byte array input stream that is not synchronized, not checked and which
 */
class SeekableInputStream implements IDataInput {
	private static final byte STRING_ENCODING_NULL = 0;
	private static final byte STRING_ENCODING_EMPTY_STRING = 1;
	static final byte STRING_ENCODING_CONSTANT_POOL = 2;
	private static final byte STRING_ENCODING_UTF8_BYTE_ARRAY = 3;
	private static final byte STRING_ENCODING_CHAR_ARRAY = 4;
	private static final byte STRING_ENCODING_LATIN1_BYTE_ARRAY = 5;

	private final byte[] buffer;
	private int pos;

	public SeekableInputStream(byte[] buffer) {
		this.buffer = buffer;
	}

	public void seek(long pos) throws IOException {
		if (pos >= 0 && pos < buffer.length) {
			this.pos = (int) pos;
		} else {
			throw new IOException("Seeking for " + pos + " in buffer of length " + buffer.length); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void readFully(byte[] b) {
		readFully(b, 0, b.length);
	}

	public void readFully(byte[] dst, int off, int len) {
		int start = pos;
		pos += len;
		System.arraycopy(buffer, start, dst, off, len);
	}

	@Override
	public boolean readBoolean() throws IOException {
		boolean value = DataInputToolkit.readBoolean(buffer, pos);
		pos += DataInputToolkit.BOOLEAN_SIZE;
		return value;
	}

	@Override
	public byte readByte() throws IOException {
		byte value = DataInputToolkit.readByte(buffer, pos);
		pos += DataInputToolkit.BYTE_SIZE;
		return value;
	}

	@Override
	public int readUnsignedByte() throws IOException {
		int value = DataInputToolkit.readUnsignedByte(buffer, pos);
		pos += DataInputToolkit.BYTE_SIZE;
		return value;
	}

	@Override
	public short readShort() throws IOException {
		short value = DataInputToolkit.readShort(buffer, pos);
		pos += DataInputToolkit.SHORT_SIZE;
		return value;
	}

	@Override
	public int readUnsignedShort() throws IOException {
		int value = DataInputToolkit.readUnsignedShort(buffer, pos);
		pos += DataInputToolkit.SHORT_SIZE;
		return value;
	}

	@Override
	public char readChar() throws IOException {
		char value = DataInputToolkit.readChar(buffer, pos);
		pos += DataInputToolkit.CHAR_SIZE;
		return value;
	}

	@Override
	public int readInt() throws IOException {
		int value = DataInputToolkit.readInt(buffer, pos);
		pos += DataInputToolkit.INTEGER_SIZE;
		return value;
	}

	@Override
	public long readUnsignedInt() throws IOException {
		long value = DataInputToolkit.readUnsignedInt(buffer, pos);
		pos += DataInputToolkit.INTEGER_SIZE;
		return value;
	}

	@Override
	public long readLong() throws IOException {
		long value = DataInputToolkit.readLong(buffer, pos);
		pos += DataInputToolkit.LONG_SIZE;
		return value;
	}

	@Override
	public float readFloat() throws IOException {
		float value = DataInputToolkit.readFloat(buffer, pos);
		pos += DataInputToolkit.FLOAT_SIZE;
		return value;
	}

	@Override
	public double readDouble() throws IOException {
		double value = DataInputToolkit.readDouble(buffer, pos);
		pos += DataInputToolkit.DOUBLE_SIZE;
		return value;
	}

	@Override
	public String readRawString(byte encoding) throws IOException {
		switch (encoding) {
		case STRING_ENCODING_NULL:
			return null;
		case STRING_ENCODING_EMPTY_STRING:
			return ""; //$NON-NLS-1$
		case STRING_ENCODING_UTF8_BYTE_ARRAY:
		case STRING_ENCODING_LATIN1_BYTE_ARRAY:
			int size = readInt();
			int start = pos;
			pos += size;
			return new String(buffer, start, size,
					encoding == STRING_ENCODING_UTF8_BYTE_ARRAY ? StandardCharsets.UTF_8 : StandardCharsets.ISO_8859_1);
		case STRING_ENCODING_CHAR_ARRAY:
			int charCount = readInt();
			char[] c = new char[charCount];
			for (int i = 0; i < c.length; i++) {
				c[i] = readChar();
			}
			return new String(c);
		default:
			throw new IOException("Disallowed raw string encoding: " + encoding); //$NON-NLS-1$
		}
	}

	@Override
	public void skipString() throws IOException {
		byte encoding = readByte();
		switch (encoding) {
		case STRING_ENCODING_NULL:
		case STRING_ENCODING_EMPTY_STRING:
			return;
		case STRING_ENCODING_UTF8_BYTE_ARRAY:
		case STRING_ENCODING_LATIN1_BYTE_ARRAY:
			int size = readInt();
			pos += size;
			return;
		case STRING_ENCODING_CHAR_ARRAY:
			int charCount = readInt();
			/*
			 * NOTE: Could skip ahead unless in "compressed" subclass.
			 *
			 * With the way compact strings work in JDK 9, one would think that there are very few
			 * real life cases where this non-standard "compressed longs" encoding would be
			 * beneficial over either (modified) UTF-8 or just dumping the char[].
			 */
			for (int i = 0; i < charCount; i++) {
				readChar();
			}
			return;
		case STRING_ENCODING_CONSTANT_POOL:
			readLong();
			return;
		default: {
			throw new IOException();
		}
		}
	}

	private static class CompressedIntsDataInput extends SeekableInputStream {

		public CompressedIntsDataInput(byte[] buffer) {
			super(buffer);
		}

		@Override
		public short readShort() throws IOException {
			return (short) readCompressedLong();
		}

		@Override
		public int readUnsignedShort() throws IOException {
			return (int) readCompressedLong();
		}

		@Override
		public char readChar() throws IOException {
			return (char) readCompressedLong();
		}

		@Override
		public int readInt() throws IOException {
			return (int) readCompressedLong();
		}

		@Override
		public long readUnsignedInt() throws IOException {
			return readCompressedLong();
		}

		@Override
		public long readLong() throws IOException {
			return readCompressedLong();
		}

		private long readCompressedLong() throws IOException {
			long ret = 0;
			for (int i = 0; i < 8; i++) {
				byte b = readByte();
				ret += (b & 0x7FL) << (7 * i);
				if (b >= 0) {
					return ret;
				}
			}
			return ret + ((readByte() & 0xFFL) << 56);
		}

	}

	static SeekableInputStream build(byte[] data, boolean compressedInts) {
		return compressedInts ? new CompressedIntsDataInput(data) : new SeekableInputStream(data);
	}

}
