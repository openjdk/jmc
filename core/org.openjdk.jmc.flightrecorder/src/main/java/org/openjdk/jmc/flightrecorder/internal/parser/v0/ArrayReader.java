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
package org.openjdk.jmc.flightrecorder.internal.parser.v0;

import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.DataType;
import org.openjdk.jmc.flightrecorder.internal.util.DataInputToolkit;

/**
 * Reads an array with element of a certain type from a byte array.
 */
final class ArrayReader implements IValueReader {

	private final IValueReader reader;

	ArrayReader(IValueReader reader) {
		this.reader = reader;
	}

	@Override
	public Object readValue(byte[] bytes, Offset offset, long timestamp) throws InvalidJfrFileException {
		int arraySize = readArraySize(bytes, offset.get());
		offset.increase(DataType.INTEGER.getSize());
		Object[] array = new Object[arraySize];
		for (int n = 0; n < arraySize; n++) {
			array[n] = reader.readValue(bytes, offset, timestamp);
		}
		return array;
	}

	private static final int UNREASONABLE_ARRAY_LENGTH = 10000000; // Very high limit, only intended to avoid OOM

	private static int readArraySize(byte[] data, int offset) throws InvalidJfrFileException {
		int length = DataInputToolkit.readInt(data, offset);
		if (length < 0 || length > UNREASONABLE_ARRAY_LENGTH) {
			throw new InvalidJfrFileException();
		}
		return length;
	}

	@Override
	public ContentType<?> getValueType() {
		return UnitLookup.UNKNOWN;
	}

	// FIXME: JMC-5907, array of primitives are currently parsed to array of IQuantity. Should we produce primitive arrays instead?
//	private static Object readPrimitiveArray(byte[] bytes, Offset offset, DataType dataType)
//			throws InvalidFlrFileException {
//		int arraySize = ArrayParser.readArraySize(bytes, offset.get());
//		offset.increase(DataType.INTEGER.getSize());
//		int index = offset.get();
//		int dataSize = dataType.getSize();
//		offset.increase(arraySize * dataSize);
//		switch (dataType) {
//		case BYTE:
//		case U1:
//			return Arrays.copyOfRange(bytes, index, index + arraySize);
//		case BOOLEAN:
//			boolean[] booleans = new boolean[arraySize];
//			for (int n = 0; n < arraySize; n++) {
//				booleans[n] = NumberParser.readBoolean(bytes, index + n * dataSize);
//			}
//			return booleans;
//		case SHORT:
//		case U2:
//			short[] shorts = new short[arraySize];
//			for (int n = 0; n < arraySize; n++) {
//				shorts[n] = NumberParser.readShort(bytes, index + n * dataSize);
//			}
//			return shorts;
//		case INTEGER:
//		case U4:
//			int[] ints = new int[arraySize];
//			for (int n = 0; n < arraySize; n++) {
//				ints[n] = NumberParser.readInt(bytes, index + n * dataSize);
//			}
//			return ints;
//		case LONG:
//		case U8:
//			long[] longs = new long[arraySize];
//			for (int n = 0; n < arraySize; n++) {
//				longs[n] = NumberParser.readLong(bytes, index + n * dataSize);
//			}
//			return longs;
//		case FLOAT:
//			float[] floats = new float[arraySize];
//			for (int n = 0; n < arraySize; n++) {
//				floats[n] = NumberParser.readFloat(bytes, index + n * dataSize);
//			}
//			return floats;
//		case DOUBLE:
//			double[] doubles = new double[arraySize];
//			for (int n = 0; n < arraySize; n++) {
//				doubles[n] = NumberParser.readDouble(bytes, index + n * dataSize);
//			}
//			return doubles;
//		case STRING:
//		case UTF8:
//			String[] strings = new String[arraySize];
//			if (dataType == DataType.STRING) {
//				for (int n = 0; n < arraySize; n++) {
//					strings[n] = StringParser.readString(bytes, offset);
//				}
//			} else {
//				for (int n = 0; n < arraySize; n++) {
//					strings[n] = UTFStringParser.readString(bytes, offset);
//				}
//			}
//			return strings;
//		}
//		throw new InvalidFlrFileException();
//	}
}
