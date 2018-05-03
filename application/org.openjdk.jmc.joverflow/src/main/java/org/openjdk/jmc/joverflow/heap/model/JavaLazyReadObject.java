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

import org.openjdk.jmc.joverflow.heap.parser.DumpCorruptedException;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;

/*
 * Base class for lazily read Java heap objects (JavaObject, JavaObjectArray and JavaValueArray).
 * Contains a long offset into the heap dump, at which the contents of this object are located. Also
 * contains a pointer back into an array in JavaObjectTable where the data from which this object
 * gets created is contained.
 */
public abstract class JavaLazyReadObject extends JavaHeapObject {

	/** See {@link JavaHeapObject#isVisited()} */
	private static final int VISITED_MASK = 1 << 31;

	/** See {@link #isVisitedAsCollectionImpl()} */
	private static final int VISITED_COLLECTION_IMPL_MASK = 1 << 30;

	/** See {@link #isVisitedAsOther()} */
	private static final int VISITED_OTHER = 1 << 29;

	/**
	 * Mask that denotes the number of bits available for assigning an "internal id" to the object.
	 * These are 32 bits minus the above bits (various "visited" etc.)
	 */
	protected static final int INTERNAL_ID_MASK = 0x1FFFFFFF;

	/** JavaClass for this object */
	protected final JavaClass clazz;

	/** File offset from which this object data starts */
	private final long objOfsInFile;

	/** Data chunk in JavaObjectTable where data for this object is stored */
	private final int[] dataChunk;

	/** Offset in dataChunk at which the data for this object is stored */
	private final int startPosInChunk;

	private final int globalObjectIndex;

	protected JavaLazyReadObject(JavaClass clazz, long objOfsInFile, int[] dataChunk, int startPosInChunk,
			int globalObjectIndex) {
		this.clazz = clazz;
		this.objOfsInFile = objOfsInFile;
		this.dataChunk = dataChunk;
		this.startPosInChunk = startPosInChunk;
		this.globalObjectIndex = globalObjectIndex;
	}

	@Override
	public final JavaClass getClazz() {
		return clazz;
	}

	/**
	 * Returns the starting offset of this object in the heap dump file. This is internal
	 * information needed to read object contents, but it can also be used as an object's unique
	 * identifier.
	 */
	public final long getObjOfsInFile() {
		return objOfsInFile;
	}

	@Override
	public final int getGlobalObjectIndex() {
		return globalObjectIndex;
	}

	/**
	 * Reads this object's content from mmapped file and returns it as byte array
	 */
	public final byte[] getValue() {
		try {
			return readValue();
		} catch (IOException ex) {
			throw new DumpCorruptedException.Runtime(
					"lazy read failed at offset " + objOfsInFile + " with exception " + ex);
		}
	}

	@Override
	public final long readId() {
		return readId(clazz.getReadBuffer(), clazz.getHprofPointerSize());
	}

	final long readId(ReadBuffer readBuf, int hprofPointerSize) {
		try {
			if (hprofPointerSize == 4) {
				return (readBuf.getInt(objOfsInFile)) & Snapshot.SMALL_ID_MASK;
			} else {
				return readBuf.getLong(objOfsInFile);
			}
		} catch (IOException ex) {
			throw new DumpCorruptedException.Runtime(
					"lazy read failed at offset " + objOfsInFile + " with exception " + ex);
		}
	}

	/**
	 * Returns true if this object has been visited during detailed analysis. Uses the tags field in
	 * the object. After the object is marked visited, it cannot be scanned again.
	 */
	@Override
	public boolean isVisited() {
		int tagsPos = startPosInChunk + 2;
		return (dataChunk[tagsPos] & VISITED_MASK) != 0;
	}

	/** @see #isVisited() */
	@Override
	public void setVisited() {
		int tagsPos = startPosInChunk + 2;
		dataChunk[tagsPos] |= VISITED_MASK;
	}

	/**
	 * Sets this object's "visited" tag. Returns true if it has not been set before, and false if
	 * this object has already been visited.
	 */
	@Override
	public boolean setVisitedIfNot() {
		if (isVisited()) {
			return false;
		}
		setVisited();
		return true;
	}

	static boolean isVisited(int tagWord) {
		return (tagWord & VISITED_MASK) != 0;
	}

	/**
	 * Returns true if this object has been visited as a part of collection implementation, when
	 * calculating its size. It signals that the object is not a standalone one. In some situations
	 * this is crucially important, for example when we sort char[] arrays into those that back
	 * Strings and those that are standalone (independent).
	 */
	public boolean isVisitedAsCollectionImpl() {
		int tagsPos = startPosInChunk + 2;
		return (dataChunk[tagsPos] & VISITED_COLLECTION_IMPL_MASK) != 0;
	}

	/** @see #isVisitedAsCollectionImpl() */
	public void setVisitedAsCollectionImpl() {
		int tagsPos = startPosInChunk + 2;
		dataChunk[tagsPos] |= VISITED_COLLECTION_IMPL_MASK;
	}

	/**
	 * Returns true if this object has been visited as part of some secondary operation - not the
	 * general visit (isVisited()), and not a visit as a part of known collection
	 * (isVisitedAsCollectionImpl()). Parts of the tool with non-overlapping functionality are free
	 * to use this for their own purposes, for example to avoid counting the same boxed Number
	 * referenced from an Object[] array multiple times.
	 */
	public boolean isVisitedAsOther() {
		int tagsPos = startPosInChunk + 2;
		return (dataChunk[tagsPos] & VISITED_OTHER) != 0;
	}

	/** @see #isVisitedAsOther() */
	public void setVisitedAsOther() {
		int tagsPos = startPosInChunk + 2;
		dataChunk[tagsPos] |= VISITED_OTHER;
	}

	/**
	 * Returns the internal id for this object, that should been previously set by
	 * {@link #setInternalId(int)}. So far these ids are used to handle duplicate Strings and
	 * arrays. Separate String or array objects with the same logical value are assigned the same
	 * id. Each String or array with a different logical value has a different id.
	 */
	public int getInternalId() {
		int tagsPos = startPosInChunk + 2;
		return dataChunk[tagsPos] & INTERNAL_ID_MASK;
	}

	/** See {@link #getInternalId()} */
	public void setInternalId(int id) {
		int tagsPos = startPosInChunk + 2;
		dataChunk[tagsPos] |= id;
	}

	protected abstract byte[] readValue() throws IOException;

	/** Reads object ID from given index from given byte array */
	protected final long objectIdAt(int index, byte[] data) {
		int idSize = getClazz().getHprofPointerSize();
		if (idSize == 4) {
			return (intAt(index, data)) & Snapshot.SMALL_ID_MASK;
		} else {
			return longAt(index, data);
		}
	}

	// utility methods to read primitive types from byte array

	protected static byte byteAt(int index, byte[] value) {
		return value[index];
	}

	protected static boolean booleanAt(int index, byte[] value) {
		return (value[index] & 0xff) != 0;
	}

	protected static char charAt(int index, byte[] value) {
		int b1 = (value[index++] & 0xff);
		int b2 = (value[index] & 0xff);
		return (char) ((b1 << 8) + b2);
	}

	protected static short shortAt(int index, byte[] value) {
		int b1 = (value[index++] & 0xff);
		int b2 = (value[index] & 0xff);
		return (short) ((b1 << 8) + b2);
	}

	protected static int intAt(int index, byte[] value) {
		int b1 = (value[index++] & 0xff);
		int b2 = (value[index++] & 0xff);
		int b3 = (value[index++] & 0xff);
		int b4 = (value[index] & 0xff);
		return ((b1 << 24) + (b2 << 16) + (b3 << 8) + b4);
	}

	protected static long longAt(int index, byte[] value) {
		long val = 0;
		for (int j = 0; j < 8; j++) {
			val = val << 8;
			int b = (value[index++]) & 0xff;
			val |= b;
		}
		return val;
	}

	protected static float floatAt(int index, byte[] value) {
		int val = intAt(index, value);
		return Float.intBitsToFloat(val);
	}

	protected static double doubleAt(int index, byte[] value) {
		long val = longAt(index, value);
		return Double.longBitsToDouble(val);
	}
}
