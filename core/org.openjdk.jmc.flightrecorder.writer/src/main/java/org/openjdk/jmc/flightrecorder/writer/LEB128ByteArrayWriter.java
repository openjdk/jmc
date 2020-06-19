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
