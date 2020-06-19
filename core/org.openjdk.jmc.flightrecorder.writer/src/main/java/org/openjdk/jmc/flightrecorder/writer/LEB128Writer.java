package org.openjdk.jmc.flightrecorder.writer;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface LEB128Writer {
	int EXT_BIT = 0x80;
	long COMPRESSED_INT_MASK = -EXT_BIT;

	/**
	 * Get a default {@linkplain LEB128Writer} instance
	 *
	 * @return a new instance of {@linkplain LEB128Writer}
	 */
	static LEB128Writer getInstance() {
		return new LEB128ByteArrayWriter(32767);
	}

	/** Reset the writer. Discard any collected data and set position to 0. */
	void reset();

	/**
	 * Write {@linkplain Character} data in LEB128 encoding
	 *
	 * @param data
	 *            the data
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeChar(char data);

	/**
	 * Write {@linkplain Character} data in LEB128 encoding at the given offset
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param data
	 *            the data
	 * @return the writer position after the data has been written
	 */
	long writeChar(long offset, char data);

	/**
	 * Write {@linkplain Short} data in LEB128 encoding
	 *
	 * @param data
	 *            the data
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeShort(short data);

	/**
	 * Write {@linkplain Short} data in LEB128 encoding at the given offset
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param data
	 *            the data
	 * @return the writer position after the data has been written
	 */
	long writeShort(long offset, short data);

	/**
	 * Write {@linkplain Integer} data in LEB128 encoding
	 *
	 * @param data
	 *            the data
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeInt(int data);

	/**
	 * Write {@linkplain Integer} data in LEB128 encoding at the given offset
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param data
	 *            the data
	 * @return the writer position after the data has been written
	 */
	long writeInt(long offset, int data);

	/**
	 * Write {@linkplain Long} data in LEB128 encoding
	 *
	 * @param data
	 *            the data
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeLong(long data);

	/**
	 * Write {@linkplain Long} data in LEB128 encoding at the given offset
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param data
	 *            the data
	 * @return the writer position after the data has been written
	 */
	long writeLong(long offset, long data);

	/**
	 * Write {@linkplain Float} data in default Java encoding
	 *
	 * @param data
	 *            the data
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeFloat(float data);

	/**
	 * Write {@linkplain Float} data in default Java encoding at the given offset
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param data
	 *            the data
	 * @return the writer position after the data has been written
	 */
	long writeFloat(long offset, float data);

	/**
	 * Write {@linkplain Double} data in default Java encoding
	 *
	 * @param data
	 *            the data
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeDouble(double data);

	/**
	 * Write {@linkplain Double} data in default Java encoding at the given offset
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param data
	 *            the data
	 * @return the writer position after the data has been written
	 */
	long writeDouble(long offset, double data);

	/**
	 * Write {@linkplain Boolean} data in default Java encoding
	 *
	 * @param data
	 *            the data
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeBoolean(boolean data);

	/**
	 * Write {@linkplain Boolean} data in default Java encoding at the given offset
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param data
	 *            the data
	 * @return the writer position after the data has been written
	 */
	long writeBoolean(long offset, boolean data);

	/**
	 * Write {@linkplain Byte} data
	 *
	 * @param data
	 *            the data
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeByte(byte data);

	/**
	 * Write {@linkplain Byte} data at the given offset
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param data
	 *            the data
	 * @return the writer position after the data has been written
	 */
	long writeByte(long offset, byte data);

	/**
	 * Write an array of {@linkplain Byte} elements
	 *
	 * @param data
	 *            the data
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeBytes(byte ... data);

	/**
	 * Write an array of {@linkplain Byte} elements at the given offset
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param data
	 *            the data
	 * @return the writer position after the data has been written
	 */
	long writeBytes(long offset, byte ... data);

	/**
	 * Write {@linkplain String} as a sequence of bytes representing UTF8 encoded string. The
	 * sequence starts with LEB128 encoded int for the length of the sequence followed by the
	 * sequence bytes.
	 *
	 * @param data
	 *            the data
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeUTF(String data);

	/**
	 * Write {@linkplain String} byte array data as a sequence of bytes representing UTF8 encoded
	 * string. The sequence starts with LEB128 encoded int for the length of the sequence followed
	 * by the sequence bytes.
	 *
	 * @param utf8Data
	 *            the byte array representation of an UTF8 string
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeUTF(byte[] utf8Data);

	/**
	 * Write {@linkplain String} as a sequence of bytes representing UTF8 encoded string at the
	 * given offset. The sequence starts with LEB128 encoded int for the length of the sequence
	 * followed by the sequence bytes.
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param data
	 *            the data
	 * @return the writer position after the data has been written
	 */
	long writeUTF(long offset, String data);

	/**
	 * Write {@linkplain String} byte array data at the given offset. The sequence starts with
	 * LEB128 encoded int for the length of the sequence followed by the sequence bytes.
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param utf8Data
	 *            the byte array representation of an UTF8 string
	 * @return the writer position after the data has been written
	 */
	long writeUTF(long offset, byte[] utf8Data);

	/**
	 * Write {@linkplain String} byte array data in special encoding. The string will translate to
	 * (byte)0 for {@literal null} value, (byte)1 for empty string and (byte)3 for the sequence of
	 * bytes representing UTF8 encoded string. The sequence starts with LEB128 encoded int for the
	 * length of the sequence followed by the sequence bytes.
	 *
	 * @param utf8Data
	 *            the byte array representation of an UTF8 string
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeCompactUTF(byte[] utf8Data);

	/**
	 * Write {@linkplain String} as a sequence of bytes representing UTF8 encoded string at the
	 * given offset. The sequence starts with LEB128 encoded int for the length of the sequence
	 * followed by the sequence bytes.
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param utf8Data
	 *            the byte array representation of an UTF8 string
	 * @return the writer position after the data has been written
	 */
	long writeCompactUTF(long offset, byte[] utf8Data);

	/**
	 * Write {@linkplain String} in special encoding. The string will translate to (byte)0 for
	 * {@literal null} value, (byte)1 for empty string and (byte)3 for the sequence of bytes
	 * representing UTF8 encoded string. The sequence starts with LEB128 encoded int for the length
	 * of the sequence followed by the sequence bytes.
	 *
	 * @param data
	 *            the data
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeCompactUTF(String data);

	/**
	 * Write {@linkplain String} byte array data in special encoding at the given offset. The string
	 * will translate to (byte)0 for {@literal null} value, (byte)1 for empty string and (byte)3 for
	 * the sequence of bytes representing UTF8 encoded string. The sequence starts with LEB128
	 * encoded int for the length of the sequence followed by the sequence bytes.
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param data
	 *            the data
	 * @return the writer position after the data has been written
	 */
	long writeCompactUTF(long offset, String data);

	/**
	 * Write {@linkplain Short} data in default Java encoding
	 *
	 * @param data
	 *            the data
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeShortRaw(short data);

	/**
	 * Write {@linkplain Short} data in default Java encoding at the given offset
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param data
	 *            the data
	 * @return the writer position after the data has been written
	 */
	long writeShortRaw(long offset, short data);

	/**
	 * Write {@linkplain Integer} data in default Java encoding
	 *
	 * @param data
	 *            the data
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeIntRaw(int data);

	/**
	 * Write {@linkplain Integer} data in default Java encoding at the given offset
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param data
	 *            the data
	 * @return the writer position after the data has been written
	 */
	long writeIntRaw(long offset, int data);

	/**
	 * Write {@linkplain Long} data in default Java encoding
	 *
	 * @param data
	 *            the data
	 * @return the writer instance for chaining
	 */
	LEB128Writer writeLongRaw(long data);

	/**
	 * Write {@linkplain Long} data in default Java encoding at the given offset
	 *
	 * @param offset
	 *            the offset from which to start writing the data
	 * @param data
	 *            the data
	 * @return the writer position after the data has been written
	 */
	long writeLongRaw(long offset, long data);

	/**
	 * Transfer the written data to a byte array
	 *
	 * @return byte array containing the written data
	 */
	default byte[] export() {
		final byte[][] dataRef = new byte[1][];
		export(buffer -> {
			int limit = buffer.limit();
			buffer.flip();
			int len = buffer.remaining();
			if (buffer.hasArray()) {
				dataRef[0] = new byte[len];
				System.arraycopy(buffer.array(), buffer.arrayOffset() + buffer.position(), dataRef[0], 0, len);
				buffer.position(buffer.limit());
			} else {
				dataRef[0] = new byte[len];
				buffer.get(dataRef[0]);
			}
			buffer.limit(limit);
		});
		return dataRef[0];
	}

	/**
	 * Transfer the written data as a {@linkplain ByteBuffer}
	 *
	 * @param consumer
	 *            a {@linkplain ByteBuffer} callback
	 */
	void export(Consumer<ByteBuffer> consumer);

	/** @return current writer position */
	int position();

	/**
	 * @return number of bytes written adjusted by the number of bytes necessary to encode the
	 *         length itself
	 */
	int length();

	/** @return the maximum number of bytes the writer can process */
	int capacity();
}
