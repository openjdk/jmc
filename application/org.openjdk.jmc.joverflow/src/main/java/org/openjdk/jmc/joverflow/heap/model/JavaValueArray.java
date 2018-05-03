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
package org.openjdk.jmc.joverflow.heap.model;

import java.io.IOException;
import java.nio.BufferUnderflowException;

import org.openjdk.jmc.joverflow.heap.parser.DumpCorruptedException;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.util.MiscUtils;
import org.openjdk.jmc.joverflow.util.StringInterner;

/**
 * A primitive array, that is, an array of ints, boolean, floats etc.
 */
public class JavaValueArray extends JavaLazyReadObject implements ArrayTypeCodes {
	private final int length;

	public JavaValueArray(JavaClass clazz, long objOfsInFile, int length, int[] dataChunk, int startPosInChunk,
			int globalObjectIndex) {
		super(clazz, objOfsInFile, dataChunk, startPosInChunk, globalObjectIndex);
		this.length = length;
	}

	/**
	 * Returns the total size of this object in the heap. That is a sum of object's data (workload)
	 * size plus the size of the object header, plus the size of array length field, adjusted with
	 * MiscUtils.getAlignedObjectSize().
	 */
	@Override
	public final int getSize() {
		JavaClass clz = getClazz();
		return MiscUtils.getAlignedObjectSize(elementSize(getElementType()) * length + clz.getArrayHeaderSize(),
				clz.getObjectAlignment());
	}

	@Override
	public final int getImplInclusiveSize() {
		return getSize();
	}

	public final char getElementType() {
		String clazzName = getClazz().getName();
		return clazzName.charAt(clazzName.length() - 1);
	}

	public final int getElementSize() {
		return elementSize(getElementType());
	}

	@Override
	protected final byte[] readValue() throws IOException {
		if (length == 0) {
			return Snapshot.EMPTY_BYTE_ARRAY;
		}
		ReadBuffer buf = clazz.getReadBuffer();
		int idSize = clazz.getHprofPointerSize();
		// Skip this object's ID, stack trace serial number (int), array length (int)
		// and element type (byte)
		long offset = getObjOfsInFile() + idSize + 9;
		int sizeInBytes = length * elementSize(getElementType());
		byte[] res = new byte[sizeInBytes];
		try {
			buf.get(offset, res);
		} catch (BufferUnderflowException ex) {
			throw new DumpCorruptedException.Runtime("primitive array size for " + getClazz().getHumanFriendlyName()
					+ " array ID " + MiscUtils.toHex(readId()) + " at offset " + getObjOfsInFile() + " is " + length
					+ " bytes, which exceeds heap dump file size");
		}
		return res;
	}

	/**
	 * Optimized version of readValue() that reads bytes into the provided array and returns the
	 * number of bytes read. If the provided array is shorter than needed, its contents are not
	 * changed. Thus the caller should always compare the returned result with array length, and if
	 * the result is larger, allocate an array of sufficient size and repeat the attempt.
	 */
	public final int readValue(byte[] value) {
		if (length == 0) {
			return 0;
		}
		try {
			ReadBuffer buf = clazz.getReadBuffer();
			int idSize = clazz.getHprofPointerSize();
			// Skip this object's ID, stack trace serial number (int), array length (int)
			// and element type (byte)
			long offset = getObjOfsInFile() + idSize + 9;
			int sizeInBytes = length * elementSize(getElementType());
			if (sizeInBytes > value.length) {
				return sizeInBytes;
			}

			buf.get(offset, value, sizeInBytes);
			return sizeInBytes;
		} catch (IOException ex) {
			throw new DumpCorruptedException.Runtime("exception caught: " + ex);
		} catch (BufferUnderflowException ex) {
			throw new DumpCorruptedException.Runtime("primitive array size for " + getClazz().getHumanFriendlyName()
					+ " array ID " + MiscUtils.toHex(readId()) + " at offset " + getObjOfsInFile() + " is " + length
					+ " bytes, which exceeds heap dump file size");
		}
	}

	@Override
	public void visitReferencedObjects(JavaHeapObjectVisitor v) {
		super.visitReferencedObjects(v);
	}

	public int getLength() {
		return length;
	}

	public Object getElements() {
		final int len = getLength();
		final char et = getElementType();
		byte[] data = getValue();
		int index = 0;
		switch (et) {
		case 'Z': {
			boolean[] res = new boolean[len];
			for (int i = 0; i < len; i++) {
				res[i] = booleanAt(index, data);
				index++;
			}
			return res;
		}
		case 'B': {
			byte[] res = new byte[len];
			for (int i = 0; i < len; i++) {
				res[i] = byteAt(index, data);
				index++;
			}
			return res;
		}
		case 'C': {
			char[] res = new char[len];
			for (int i = 0; i < len; i++) {
				res[i] = charAt(index, data);
				index += 2;
			}
			return res;
		}
		case 'S': {
			short[] res = new short[len];
			for (int i = 0; i < len; i++) {
				res[i] = shortAt(index, data);
				index += 2;
			}
			return res;
		}
		case 'I': {
			int[] res = new int[len];
			for (int i = 0; i < len; i++) {
				res[i] = intAt(index, data);
				index += 4;
			}
			return res;
		}
		case 'J': {
			long[] res = new long[len];
			for (int i = 0; i < len; i++) {
				res[i] = longAt(index, data);
				index += 8;
			}
			return res;
		}
		case 'F': {
			float[] res = new float[len];
			for (int i = 0; i < len; i++) {
				res[i] = floatAt(index, data);
				index += 4;
			}
			return res;
		}
		case 'D': {
			double[] res = new double[len];
			for (int i = 0; i < len; i++) {
				res[i] = doubleAt(index, data);
				index += 8;
			}
			return res;
		}
		default: {
			throw new RuntimeException("unknown primitive type?");
		}
		}
	}

	private void checkIndex(int index) {
		if (index < 0 || index >= getLength()) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
	}

	private void requireType(char type) {
		if (getElementType() != type) {
			throw new RuntimeException("not of type : " + type);
		}
	}

	public boolean getBooleanAt(int index) {
		checkIndex(index);
		requireType('Z');
		return booleanAt(index, getValue());
	}

	public byte getByteAt(int index) {
		checkIndex(index);
		requireType('B');
		return byteAt(index, getValue());
	}

	public char getCharAt(int index) {
		checkIndex(index);
		requireType('C');
		return charAt(index << 1, getValue());
	}

	public short getShortAt(int index) {
		checkIndex(index);
		requireType('S');
		return shortAt(index << 1, getValue());
	}

	public int getIntAt(int index) {
		checkIndex(index);
		requireType('I');
		return intAt(index << 2, getValue());
	}

	public long getLongAt(int index) {
		checkIndex(index);
		requireType('J');
		return longAt(index << 3, getValue());
	}

	public float getFloatAt(int index) {
		checkIndex(index);
		requireType('F');
		return floatAt(index << 2, getValue());
	}

	public double getDoubleAt(int index) {
		checkIndex(index);
		requireType('D');
		return doubleAt(index << 3, getValue());
	}

	@Override
	public String valueAsString() {
		return valueAsString(false);
	}

	public String valueAsString(boolean bigLimit) {
		StringBuilder result;
		byte[] value = getValue();
		char elementSignature = getElementType();
		int elSize = elementSize(elementSignature);
		int limit = bigLimit ? 1000 : (elementSignature == 'C') ? 32 : 10;
		result = new StringBuilder(limit * 8);
		result.append(getElementTypeName(elementSignature)).append('[');
		result.append(value.length / elSize).append(']');
		for (int i = 1; i < getClazz().getNumArrayDimensions(); i++) {
			result.append("[]");
		}
		result.append('{');
		int num = 0;

		for (int i = 0; i < value.length;) {
			if (num > 0 && elementSignature != 'C') {
				result.append(", ");
			}
			if (num >= limit || result.length() > 74) {
				result.append(" ...");
				break;
			}
			num++;

			switch (elementSignature) {
			case 'C':
				result.append(charAsString(charAt(i, value)));
				break;
			case 'Z':
				result.append(booleanAsString(booleanAt(i, value)));
				break;
			case 'B':
				result.append(byteAsString(byteAt(i, value)));
				break;
			case 'S':
				result.append(shortAt(i, value));
				break;
			case 'I':
				result.append(intAt(i, value));
				break;
			case 'J': // long
				result.append(longAt(i, value));
				break;
			case 'F':
				result.append(floatAt(i, value));
				break;
			case 'D': // double
				result.append(doubleAt(i, value));
				break;
			default: {
				throw new RuntimeException("unknown primitive type?");
			}
			}

			i += elSize;
		}

		result.append('}');
		return StringInterner.internString(result.toString());
	}

	/** For an array element signature such as 'C', returns human-readable name ("char") */
	public static String getElementTypeName(char sig) {
		switch (sig) {
		case 'B':
			return "byte";
		case 'Z':
			return "boolean";
		case 'C':
			return "char";
		case 'S':
			return "short";
		case 'I':
			return "int";
		case 'F':
			return "float";
		case 'J':
			return "long";
		case 'D':
			return "double";
		default:
			return null;
		}
	}

	public String[] getValuesAsStrings() {
		final int len = getLength();
		final char et = getElementType();
		byte[] data = getValue();
		String[] res = new String[len];
		int index = 0;
		switch (et) {
		case 'Z': {
			for (int i = 0; i < len; i++) {
				res[i] = StringInterner.internString(booleanAsString(booleanAt(index, data)));
				index++;
			}
			return res;
		}
		case 'B': {
			for (int i = 0; i < len; i++) {
				res[i] = StringInterner.internString(byteAsString(byteAt(index, data)));
				index++;
			}
			return res;
		}
		case 'C': {
			for (int i = 0; i < len; i++) {
				res[i] = StringInterner.internString(charAsString(charAt(index, data)));
				index += 2;
			}
			return res;
		}
		case 'S': {
			for (int i = 0; i < len; i++) {
				res[i] = StringInterner.internString(String.valueOf(shortAt(index, data)));
				index += 2;
			}
			return res;
		}
		case 'I': {
			for (int i = 0; i < len; i++) {
				res[i] = StringInterner.internString(String.valueOf(intAt(index, data)));
				index += 4;
			}
			return res;
		}
		case 'J': {
			for (int i = 0; i < len; i++) {
				res[i] = StringInterner.internString(String.valueOf(longAt(index, data)));
				index += 8;
			}
			return res;
		}
		case 'F': {
			for (int i = 0; i < len; i++) {
				res[i] = StringInterner.internString(String.valueOf(floatAt(index, data)));
				index += 4;
			}
			return res;
		}
		case 'D': {
			for (int i = 0; i < len; i++) {
				res[i] = StringInterner.internString(String.valueOf(doubleAt(index, data)));
				index += 8;
			}
			return res;
		}
		default: {
			throw new RuntimeException("unknown primitive type?");
		}
		}
	}

	private static int elementSize(char sig) {
		switch (sig) {
		case 'B':
		case 'Z':
			return 1;
		case 'C':
		case 'S':
			return 2;
		case 'I':
		case 'F':
			return 4;
		case 'J':
		case 'D':
			return 8;
		default:
			throw new RuntimeException("invalid array element type: " + sig);
		}
	}

	private static String charAsString(char val) {
		if (val > 32) {
			return String.valueOf(val);
		} else {
			return "\\0x" + Integer.toString(val, 16);
		}
	}

	private static String booleanAsString(boolean val) {
		return val ? "true" : "false";
	}

	private static String byteAsString(byte val) {
		int intVal = 0xFF & val;
		return "0x" + Integer.toString(intVal, 16);
	}
}
