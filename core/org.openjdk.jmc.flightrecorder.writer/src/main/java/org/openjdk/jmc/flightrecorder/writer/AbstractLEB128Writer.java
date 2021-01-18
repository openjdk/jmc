/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.writer;

import java.nio.charset.StandardCharsets;

abstract class AbstractLEB128Writer implements LEB128Writer {
	@Override
	public final LEB128Writer writeChar(char data) {
		writeChar(position(), data);
		return this;
	}

	@Override
	public final long writeChar(long offset, char data) {
		return writeLong(offset, data & 0x000000000000ffffL);
	}

	@Override
	public final LEB128Writer writeShort(short data) {
		writeShort(position(), data);
		return this;
	}

	@Override
	public final long writeShort(long offset, short data) {
		return writeLong(offset, data & 0x000000000000ffffL);
	}

	@Override
	public final LEB128Writer writeInt(int data) {
		writeInt(position(), data);
		return this;
	}

	@Override
	public final long writeInt(long offset, int data) {
		return writeLong(offset, data & 0x00000000ffffffffL);
	}

	@Override
	public final LEB128Writer writeLong(long data) {
		writeLong(position(), data);
		return this;
	}

	@Override
	public final long writeLong(long offset, long data) {
		if ((data & LEB128Writer.COMPRESSED_INT_MASK) == 0) {
			return writeByte(offset, (byte) (data & 0xff));
		}
		offset = writeByte(offset, (byte) (data | LEB128Writer.EXT_BIT));
		data >>= 7;
		if ((data & LEB128Writer.COMPRESSED_INT_MASK) == 0) {
			return writeByte(offset, (byte) data);
		}
		offset = writeByte(offset, (byte) (data | LEB128Writer.EXT_BIT));
		data >>= 7;
		if ((data & LEB128Writer.COMPRESSED_INT_MASK) == 0) {
			return writeByte(offset, (byte) data);
		}
		offset = writeByte(offset, (byte) (data | LEB128Writer.EXT_BIT));
		data >>= 7;
		if ((data & LEB128Writer.COMPRESSED_INT_MASK) == 0) {
			return writeByte(offset, (byte) data);
		}
		offset = writeByte(offset, (byte) (data | LEB128Writer.EXT_BIT));
		data >>= 7;
		if ((data & LEB128Writer.COMPRESSED_INT_MASK) == 0) {
			return writeByte(offset, (byte) data);
		}
		offset = writeByte(offset, (byte) (data | LEB128Writer.EXT_BIT));
		data >>= 7;
		if ((data & LEB128Writer.COMPRESSED_INT_MASK) == 0) {
			return writeByte(offset, (byte) data);
		}
		offset = writeByte(offset, (byte) (data | LEB128Writer.EXT_BIT));
		data >>= 7;
		if ((data & LEB128Writer.COMPRESSED_INT_MASK) == 0) {
			return writeByte(offset, (byte) data);
		}
		offset = writeByte(offset, (byte) (data | LEB128Writer.EXT_BIT));
		data >>= 7;
		if ((data & LEB128Writer.COMPRESSED_INT_MASK) == 0) {
			return writeByte(offset, (byte) data);
		}
		offset = writeByte(offset, (byte) (data | LEB128Writer.EXT_BIT));
		return writeByte(offset, (byte) (data >> 7));
	}

	@Override
	public final LEB128Writer writeFloat(float data) {
		writeFloat(position(), data);
		return this;
	}

	@Override
	public final LEB128Writer writeDouble(double data) {
		writeDouble(position(), data);
		return this;
	}

	@Override
	public final LEB128Writer writeBoolean(boolean data) {
		writeBoolean(position(), data);
		return this;
	}

	@Override
	public final long writeBoolean(long offset, boolean data) {
		return writeByte(offset, data ? (byte) 1 : (byte) 0);
	}

	@Override
	public final LEB128Writer writeByte(byte data) {
		writeByte(position(), data);
		return this;
	}

	@Override
	public final LEB128Writer writeBytes(byte ... data) {
		writeBytes(position(), data);
		return this;
	}

	@Override
	public final LEB128Writer writeUTF(String data) {
		writeUTF(position(), data);
		return this;
	}

	@Override
	public final LEB128Writer writeUTF(byte[] data) {
		writeUTF(position(), data);
		return this;
	}

	@Override
	public final long writeUTF(long offset, String data) {
		return writeUTF(offset, data == null ? null : data.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public final long writeUTF(long offset, byte[] data) {
		int len = data == null ? 0 : data.length;
		long pos = writeInt(offset, len);
		if (len > 0) {
			pos = writeBytes(pos, data);
		}
		return pos;
	}

	@Override
	public final LEB128Writer writeCompactUTF(byte[] data) {
		writeCompactUTF(position(), data);
		return this;
	}

	@Override
	public final LEB128Writer writeCompactUTF(String data) {
		writeCompactUTF(position(), data);
		return this;
	}

	@Override
	public final long writeCompactUTF(long offset, byte[] data) {
		if (data == null) {
			return writeByte(offset, (byte) 0); // special NULL encoding
		}
		if (data.length == 0) {
			return writeByte(offset, (byte) 1); // special empty string encoding
		}
		long pos = writeByte(offset, (byte) 3); // UTF-8 string
		pos = writeInt(pos, data.length);
		pos = writeBytes(pos, data);
		return pos;
	}

	@Override
	public final long writeCompactUTF(long offset, String data) {
		return writeCompactUTF(offset, data != null ? data.getBytes(StandardCharsets.UTF_8) : null);
	}

	@Override
	public final LEB128Writer writeShortRaw(short data) {
		writeShortRaw(position(), data);
		return this;
	}

	@Override
	public final LEB128Writer writeIntRaw(int data) {
		writeIntRaw(position(), data);
		return this;
	}

	@Override
	public final LEB128Writer writeLongRaw(long data) {
		writeLongRaw(position(), data);
		return this;
	}

	@Override
	public final int length() {
		return adjustLength(position());
	}

	static int adjustLength(int length) {
		int extraLen = 0;
		do {
			extraLen = getPackedIntLen(length + extraLen);
		} while (getPackedIntLen(length + extraLen) != extraLen);
		return length + extraLen;
	}

	static int getPackedIntLen(long data) {
		if ((data & COMPRESSED_INT_MASK) == 0) {
			return 1;
		}
		data >>= 7;
		if ((data & COMPRESSED_INT_MASK) == 0) {
			return 2;
		}
		data >>= 7;
		if ((data & COMPRESSED_INT_MASK) == 0) {
			return 3;
		}
		data >>= 7;
		if ((data & COMPRESSED_INT_MASK) == 0) {
			return 4;
		}
		data >>= 7;
		if ((data & COMPRESSED_INT_MASK) == 0) {
			return 5;
		}
		data >>= 7;
		if ((data & COMPRESSED_INT_MASK) == 0) {
			return 6;
		}
		data >>= 7;
		if ((data & COMPRESSED_INT_MASK) == 0) {
			return 7;
		}
		data >>= 7;
		if ((data & COMPRESSED_INT_MASK) == 0) {
			return 8;
		}
		return 9;
	}
}
