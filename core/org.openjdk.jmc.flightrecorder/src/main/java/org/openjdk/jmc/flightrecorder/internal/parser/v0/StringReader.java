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

import java.io.UnsupportedEncodingException;

import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;

/**
 * Reads a string
 */
final class StringReader implements IValueReader {
	public static final StringReader INSTANCE = new StringReader();
	private static final int STRING_ELEMENT_SIZE = 2; // Each element in a JFR String is stored as an unsigned short (2 bytes)
	private static final String CHARSET = "UTF-16"; //$NON-NLS-1$

	@Override
	public Object readValue(byte[] data, Offset offset, long timestamp) throws InvalidJfrFileException {
		return readString(data, offset);
	}

	public static String readString(byte[] data, Offset offset) throws InvalidJfrFileException {
		int byteCount = readStringByteCount(data, offset);
		int index = offset.get();
		offset.increase(byteCount);
		try {
			return new String(data, index, byteCount, CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private static final int UNREASONABLE_STRING_LENGTH = 100000000; // 200 Mibibyte String limit, only intended to avoid OOM

	private static int readStringByteCount(byte[] data, Offset offset) throws InvalidJfrFileException {
		int numberOfStringElements = NumberReaders.readInt(data, offset);
		if (numberOfStringElements < 0 || numberOfStringElements > UNREASONABLE_STRING_LENGTH) {
			throw new InvalidJfrFileException();
		}
		return numberOfStringElements * STRING_ELEMENT_SIZE;
	}

	@Override
	public ContentType<?> getValueType() {
		return UnitLookup.PLAIN_TEXT;
	}

}
