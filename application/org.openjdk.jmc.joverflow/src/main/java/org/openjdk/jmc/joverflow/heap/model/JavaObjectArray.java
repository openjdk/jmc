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
 * Represents a Java object array.
 */
public class JavaObjectArray extends JavaLazyReadObject {
	private final int length;

	public JavaObjectArray(JavaClass clazz, long objOfsInFile, int length, int[] dataChunk, int startPosInChunk,
			int globalObjectIndex) {
		super(clazz, objOfsInFile, dataChunk, startPosInChunk, globalObjectIndex);
		this.length = length;
	}

	/**
	 * Returns the total size of this object in the heap. That is the sum of object's data
	 * (workload) size plus all the VM overhead: size of the object header plus the size of array
	 * length field.
	 */
	@Override
	public final int getSize() {
		JavaClass clz = getClazz();
		return MiscUtils.getAlignedObjectSize(clz.getPointerSize() * length + clz.getArrayHeaderSize(),
				clz.getObjectAlignment());
	}

	@Override
	public final int getImplInclusiveSize() {
		return getSize();
	}

	@Override
	public String valueAsString() {
		return valueAsString(false);
	}

	public String valueAsString(boolean bigLimit) {
		StringBuilder result;
		JavaHeapObject[] elements = getElements();
		int limit = bigLimit ? 1000 : 32;
		result = new StringBuilder(limit * 16);
		String humanFriendlyName = getClazz().getHumanFriendlyName();
		result.append(humanFriendlyName);
		int arrayStartPos = humanFriendlyName.indexOf('[');
		if (arrayStartPos != -1) {
			result.insert(arrayStartPos + 1, elements.length);
		}
		result.append('{');
		int num = 0;

		for (JavaHeapObject element : elements) {
			if (num > 0) {
				result.append(", ");
			}
			if (num >= limit || (!bigLimit && result.length() > 74)) {
				result.append(" ...");
				break;
			}
			num++;

			result.append(element != null ? element.valueAsString() : "null");
		}
		result.append('}');
		return StringInterner.internString(result.toString());
	}

	public JavaHeapObject[] getElements() {
		Snapshot snapshot = getClazz().getSnapshot();
		byte[] data = getValue();
		final int idSize = snapshot.getHprofPointerSize();
		JavaHeapObject[] elements = new JavaHeapObject[length];
		int index = 0;
		for (int i = 0; i < elements.length; i++) {
			long id = objectIdAt(index, data);
			index += idSize;
			elements[i] = snapshot.getObjectForId(id);
		}
		return elements;
	}

	// Use Comparator instead of implementing Comparable if sorting is needed 
//	@Override
//	public int compareTo(JavaThing other) {
//		if (other instanceof JavaObjectArray) {
//			return 0;
//		}
//		return super.compareTo(other);
//	}

	public int getLength() {
		return length;
	}

	@Override
	public void visitReferencedObjects(JavaHeapObjectVisitor v) {
		super.visitReferencedObjects(v);
		JavaHeapObject[] elements = getElements();
		for (JavaHeapObject element : elements) {
			if (element != null) {
				v.visit(element);
			}
		}
	}

	@Override
	protected final byte[] readValue() throws IOException {
		if (length == 0) {
			return Snapshot.EMPTY_BYTE_ARRAY;
		}
		ReadBuffer buf = clazz.getReadBuffer();
		int idSize = clazz.getHprofPointerSize();
		// Skip object ID, stack trace serial number (int), array length (int),
		// and array class ID
		long offset = getObjOfsInFile() + 2 * idSize + 8;
		byte[] res = new byte[length * idSize];
		try {
			buf.get(offset, res);
		} catch (BufferUnderflowException ex) {
			throw new DumpCorruptedException.Runtime(
					"object array size for array ID " + MiscUtils.toHex(readId()) + " at offset " + getObjOfsInFile()
							+ " is " + res.length + " bytes, which exceeds heap dump file size");
		}
		return res;
	}
}
