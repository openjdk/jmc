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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Contains the base information about all instances and arrays (but not classes) of the heap dump
 * in the compact table form. Provides a method for creating an instance of JavaLazyReadObject that
 * in effect contains the same information as the table, but in a more manageable form. Also
 * provides a method to iterate over all objects in the table.
 * <p>
 * The internal table is organized, conceptually, as an array of ints. However, in reality it
 * consists of multiple 1MB "chunks", to facilitate building this table incrementally and to avoid
 * issues with GC that a very big array could create. Each heap object corresponds to 3 (for
 * instances) or 4 (for arrays) ints in the array. The first two ints contain the object's offset in
 * the dump, its class index in a separate class table, and a bit specifying whether this object is
 * an array, all squeezed collectively into 64 bits. The third int is the "tag word", where various
 * setVisited() etc. methods of {@link JavaLazyReadObject} can set bits as they need. Finally, for
 * arrays the fourth int contains the array's length.
 * <p>
 * Since we currently use ints to index objects, and objects on average take 3.5 slots in the table,
 * the maximum number of objects that this table can accomodate is 2^31 / 3.5 ~= 613 million.
 */
class JavaObjectTable {

	private static final int CHUNK_MAGNITUDE = 20; // Corresponds to 1024*1024
	private static final int CHUNK_SIZE = 1 << CHUNK_MAGNITUDE;

	private static final int POS_IN_CHUNK_MASK = CHUNK_SIZE - 1;

	private static final long LONG_LOW_WORD_MASK = 0x0FFFFFFFFL;

	// The following variables control how the high int of the two ints (that
	// collectively contain obj offset, class index and "is array" bit) is used.
	// The maximum number of classes is 2^(32 - classIdxShift).
	// The maximum object offset is 2^(32 + classIdxShift - 1).
	// For example, if we have a 32GB (2^35) file, classIdxShift = 4. Then the
	// maximum number of classes this table can accomodate is  2^28 = 268435456.
	private final int classIdxShift, arrayMask, objOfsHighWordMask;

	private final int[][] objects;
	private final JavaClass[] classes;

	private final int numObjs, lastObjEndPos;

	private JavaObjectTable(int[][] objects, JavaClass[] classes, int numObjs, int lastObjEndPos, int classIdxShift,
			int arrayMask) {
		this.objects = objects;
		this.classes = classes;
		this.numObjs = numObjs;
		this.lastObjEndPos = lastObjEndPos;
		this.classIdxShift = classIdxShift;
		this.arrayMask = arrayMask;
		this.objOfsHighWordMask = arrayMask - 1;
	}

	JavaLazyReadObject getObject(int objPosInTable) {
		int chunkIdx = objPosInTable >> CHUNK_MAGNITUDE;
		int[] chunk = objects[chunkIdx];
		int posInCurChunk = objPosInTable & POS_IN_CHUNK_MASK;
		int startPosInCurChunk = posInCurChunk;
		int classAndOfsWord1 = chunk[posInCurChunk++];
		int classAndOfsWord2 = chunk[posInCurChunk++];
		long objOfsInFile = ((classAndOfsWord2) & LONG_LOW_WORD_MASK)
				| (((long) (classAndOfsWord1 & objOfsHighWordMask)) << 32);
		int classIdx = classAndOfsWord1 >>> classIdxShift;
		JavaClass clazz = classes[classIdx];
		boolean isArray = (classAndOfsWord1 & arrayMask) != 0;
		if (isArray) {
			int length = chunk[posInCurChunk + 1];
			if (clazz.isSingleDimPrimitiveArray()) {
				return new JavaValueArray(clazz, objOfsInFile, length, chunk, startPosInCurChunk, objPosInTable);
			} else {
				return new JavaObjectArray(clazz, objOfsInFile, length, chunk, startPosInCurChunk, objPosInTable);
			}
		} else {
			return new JavaObject(clazz, objOfsInFile, chunk, startPosInCurChunk, objPosInTable);
		}
	}

	int size() {
		return numObjs;
	}

	Collection<JavaLazyReadObject> getObjects() {
		return new AbstractCollection<JavaLazyReadObject>() {
			@Override
			public Iterator<JavaLazyReadObject> iterator() {
				return new Iterator<JavaLazyReadObject>() {
					private int curObjPos = 1; // 1 is important; see Builder.posInCurChunk
					private int curChunk = 0;
					private int curChunkEndPos = CHUNK_SIZE - 1;

					@Override
					public boolean hasNext() {
						return curObjPos < lastObjEndPos;
					}

					@Override
					public JavaLazyReadObject next() {
						if (!hasNext()) {
							throw new NoSuchElementException();
						}
						JavaLazyReadObject result = getObject(curObjPos);
						if (result instanceof JavaObject) {
							curObjPos += 3;
						} else {
							curObjPos += 4;
						}
						if (curObjPos > curChunkEndPos - 3) {
							curChunk++;
							curObjPos = curChunk * CHUNK_SIZE;
							curChunkEndPos = curObjPos + CHUNK_SIZE - 1;
						}
						return result;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public int size() {
				return numObjs;
			}
		};
	}

	Collection<JavaLazyReadObject> getUnvisitedObjects() {

		class UnvisitedObjIterator implements Iterator<JavaLazyReadObject> {
			private int curChunk = 0;
			private int curChunkEndPos = CHUNK_SIZE - 1;
			private int curObjPos = 1; // 1 is important; see Builder.posInCurChunk

			UnvisitedObjIterator() {
				moveToNextUnvisitedObjectIfNeeded();
			}

			@Override
			public boolean hasNext() {
				return curObjPos < lastObjEndPos;
			}

			@Override
			public JavaLazyReadObject next() {
				JavaLazyReadObject result = getObject(curObjPos);
				if (result instanceof JavaObject) {
					curObjPos += 3;
				} else {
					curObjPos += 4;
				}
				moveToNextUnvisitedObjectIfNeeded();
				return result;
			}

			private void moveToNextUnvisitedObjectIfNeeded() {
				while (curObjPos < lastObjEndPos) {
					if (curObjPos > curChunkEndPos - 3) {
						curChunk++;
						curObjPos = curChunk * CHUNK_SIZE;
						curChunkEndPos = curObjPos + CHUNK_SIZE - 1;
					}
					if (curObjIsVisited()) {
						if (curObjIsArray()) {
							curObjPos += 4;
						} else {
							curObjPos += 3;
						}
					} else {
						break;
					}
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			private boolean curObjIsVisited() {
				return JavaLazyReadObject.isVisited(objects[curChunk][(curObjPos & POS_IN_CHUNK_MASK) + 2]);
			}

			private boolean curObjIsArray() {
				int firstWord = objects[curChunk][curObjPos & POS_IN_CHUNK_MASK];
				return (firstWord & arrayMask) != 0;
			}
		}

		return new AbstractCollection<JavaLazyReadObject>() {
			@Override
			public Iterator<JavaLazyReadObject> iterator() {
				return new UnvisitedObjIterator();
			}

			@Override
			public int size() {
				throw new UnsupportedOperationException();
			}
		};
	}

	static class Builder {

		private final int classIdxShift;
		private final int arrayMask;

		private final ArrayList<int[]> chunksAsList;

		private int curChunkIdx = -1;
		private int[] curChunk;
		private int posInCurChunk; // Set to 1 for the very first object; see constructor

		private int numObjs;

		Builder(long hprofFileSize) {
			int nObjOfsBits = 0;
			while (hprofFileSize > 0) {
				nObjOfsBits++;
				hprofFileSize >>= 1;
			}
			if (nObjOfsBits > 32) {
				classIdxShift = nObjOfsBits - 31;
			} else {
				classIdxShift = 1;
			}
			arrayMask = 1 << (classIdxShift - 1);

			chunksAsList = new ArrayList<>();
			addChunk();
			// It is important that the very first object is located at position 1:
			// this guarantees that the implementation of getObjectGlobalIndex() in
			// JavaLazyReadObject returns only non-zero positive values, as required.
			posInCurChunk = 1;
		}

		JavaObjectTable buildJavaObjectTable(JavaClass[] classes) {
			int[][] objects = chunksAsList.toArray(new int[chunksAsList.size()][]);
			int lastObjEndPos = curChunkIdx * CHUNK_SIZE + posInCurChunk;
			return new JavaObjectTable(objects, classes, numObjs, lastObjEndPos, classIdxShift, arrayMask);
		}

		int addJavaObject(int classIdx, long objOfsInFile) {
			if (posInCurChunk > CHUNK_SIZE - 4) {
				addChunk();
			}
			int curAbsPos = curChunkIdx * CHUNK_SIZE + posInCurChunk;

			addClassAndOfs(classIdx, objOfsInFile, false);
			posInCurChunk++; // Tags word
			return curAbsPos;
		}

		int addJavaArray(int classIdx, long objOfsInFile, int length) {
			if (posInCurChunk > CHUNK_SIZE - 4) {
				addChunk();
			}
			int curAbsPos = curChunkIdx * CHUNK_SIZE + posInCurChunk;

			addClassAndOfs(classIdx, objOfsInFile, true);
			posInCurChunk++; // Tags word
			addInt(length);
			return curAbsPos;
		}

		int getNumObjects() {
			return numObjs;
		}

		private void addClassAndOfs(int classIdx, long objOfsInFile, boolean isArray) {
			numObjs++;
			curChunk[posInCurChunk++] = ((int) (objOfsInFile >> 32)) | (classIdx << classIdxShift)
					| (isArray ? arrayMask : 0);
			curChunk[posInCurChunk++] = (int) objOfsInFile;
		}

		private void addInt(int intNum) {
			curChunk[posInCurChunk++] = intNum;
		}

		private void addChunk() {
			curChunkIdx++;
			curChunk = new int[CHUNK_SIZE];
			chunksAsList.add(curChunk);
			posInCurChunk = 0;
		}
	}
}
