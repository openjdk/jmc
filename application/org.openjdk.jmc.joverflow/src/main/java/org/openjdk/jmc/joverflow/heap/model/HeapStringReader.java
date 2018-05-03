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

/**
 * Encapsulates functionality for reading Strings from the heap dump. For better performance, keeps
 * and reuses a single internal byte and char array.
 */
public class HeapStringReader implements ImplInclusiveSizeCalculator {
	// Indices of fields within a String instance
	private final int stringValueIdx, stringOffsetIdx, stringCountIdx;

	private byte[] byteBuf = new byte[200];
	private char[] charBuf = new char[100];

	private JavaThing[] fields;
	private JavaValueArray valueArray;

	HeapStringReader(Snapshot snapshot) {
		JavaClass stringClass = snapshot.getClassForName("java.lang.String");
		stringValueIdx = stringClass.getInstanceFieldIndex("value");
		stringOffsetIdx = stringClass.getInstanceFieldIndexOrMinusOne("offset");
		stringCountIdx = stringClass.getInstanceFieldIndexOrMinusOne("count");
		stringClass.setImplInclusiveSizeCalculator(this);
	}

	public String readString(JavaObject strObj) {
		fields = strObj.getFields(fields);
		JavaThing stringValueField = fields[stringValueIdx];
		// Looks like the problem below may occur in heap dumps containing minor
		// corruption/incompleteness (some pointers cannot be resolved). HotSpot
		// occasionally produces such dumps for unknown reason.
		if (stringValueField == null || !(stringValueField instanceof JavaValueArray)) {
			return null;
		}

		valueArray = (JavaValueArray) stringValueField;
		int offset = 0;
		int count = valueArray.getLength();
		if (stringOffsetIdx != -1) { // Old-fashioned String implementation with offset/count
			offset = ((JavaInt) fields[stringOffsetIdx]).getValue();
			count = ((JavaInt) fields[stringCountIdx]).getValue();
		}
		readCharElements(offset, count);

		return new String(charBuf, 0, count);
	}

	/**
	 * Returns JavaValueArray that was obtained internally in the last call to readString().
	 */
	public JavaValueArray getLastReadBackingArray() {
		return valueArray;
	}

	public JavaValueArray getCharArrayForString(JavaObject strObj) {
		fields = strObj.getFields(fields);
		JavaThing stringValueField = fields[stringValueIdx];
		if (stringValueField == null || !(stringValueField instanceof JavaValueArray)) {
			return null;
		}

		return (JavaValueArray) stringValueField;
	}

	private void readCharElements(int offset, int count) {
		if (charBuf.length < count) {
			charBuf = new char[count];
		}

		boolean isCompressedString = valueArray.getClazz().isByteArray();

		int len = valueArray.getLength();
		if (isCompressedString) {
			if (byteBuf.length < len) {
				byteBuf = new byte[len];
			}
		} else {
			if (byteBuf.length < len * 2) {
				byteBuf = new byte[len * 2];
			}
		}

		int byteBufLen = valueArray.readValue(byteBuf);
		if (byteBufLen > byteBuf.length) {
			throw new RuntimeException("Unexpected length: " + byteBufLen);
		}

		if (isCompressedString) {
			int byteIdx = offset;
			for (int charIdx = 0; charIdx < count; charIdx++) {
				charBuf[charIdx] = (char) (byteBuf[byteIdx++] & 0xff);
			}
		} else {
			int byteIdx = offset * 2;
			for (int charIdx = 0; charIdx < count; charIdx++) {
				int b1 = (byteBuf[byteIdx++] & 0xff);
				int b2 = (byteBuf[byteIdx++] & 0xff);
				charBuf[charIdx] = (char) ((b1 << 8) + b2);
			}
		}
	}

	@Override
	public int calculateImplInclusiveSize(JavaObject javaObj) {
		int result = javaObj.getSize();
		JavaValueArray charArray = getCharArrayForString(javaObj);
		if (charArray != null) {
			result += charArray.getSize();
		}
		return result;
	}
}
