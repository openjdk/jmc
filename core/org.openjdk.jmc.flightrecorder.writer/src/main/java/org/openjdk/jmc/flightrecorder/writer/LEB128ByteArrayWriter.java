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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Consumer;

/** Byte-array writer with default support for LEB128 encoded integer types */
final class LEB128ByteArrayWriter extends AbstractLEB128Writer {
	private byte[] array;
	private int pointer = 0;

	LEB128ByteArrayWriter(int intialCapacity) {
		array = new byte[intialCapacity];
	}

	@Override
	public void reset() {
		Arrays.fill(array, (byte) 0);
		pointer = 0;
	}

	@Override
	public long writeFloat(long offset, float data) {
		return writeIntRaw(offset, Float.floatToIntBits(data));
	}

	@Override
	public long writeDouble(long offset, double data) {
		return writeLongRaw(offset, Double.doubleToLongBits(data));
	}

	@Override
	public long writeByte(long offset, byte data) {
		int newOffset = (int) (offset + 1);
		if (newOffset >= array.length) {
			array = Arrays.copyOf(array, newOffset * 2);
		}
		array[(int) offset] = data;
		pointer = Math.max(newOffset, pointer);
		return newOffset;
	}

	@Override
	public long writeBytes(long offset, byte ... data) {
		if (data == null) {
			return offset;
		}
		int newOffset = (int) (offset + data.length);
		if (newOffset >= array.length) {
			array = Arrays.copyOf(array, newOffset * 2);
		}
		System.arraycopy(data, 0, array, (int) offset, data.length);
		pointer = Math.max(newOffset, pointer);
		return newOffset;
	}

	@Override
	public long writeShortRaw(long offset, short data) {
		return writeBytes(offset, (byte) ((data >> 8) & 0xff), (byte) (data & 0xff));
	}

	@Override
	public long writeIntRaw(long offset, int data) {
		return writeBytes(offset, (byte) ((data >> 24) & 0xff), (byte) ((data >> 16) & 0xff),
				(byte) ((data >> 8) & 0xff), (byte) (data & 0xff));
	}

	@Override
	public long writeLongRaw(long offset, long data) {
		return writeBytes(offset, (byte) ((data >> 56) & 0xff), (byte) ((data >> 48) & 0xff),
				(byte) ((data >> 40) & 0xff), (byte) ((data >> 32) & 0xff), (byte) ((data >> 24) & 0xff),
				(byte) ((data >> 16) & 0xff), (byte) ((data >> 8) & 0xff), (byte) (data & 0xff));
	}

	@Override
	public void export(Consumer<ByteBuffer> consumer) {
		ByteBuffer bb = ByteBuffer.wrap(array, 0, pointer);
		bb.position(pointer);
		consumer.accept(bb);
	}

	@Override
	public int position() {
		return pointer;
	}

	@Override
	public int capacity() {
		return array.length;
	}
}
