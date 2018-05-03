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
package org.openjdk.jmc.joverflow.stats;

import org.openjdk.jmc.joverflow.descriptors.ArrayBasedCollectionDescriptor;
import org.openjdk.jmc.joverflow.descriptors.CollectionDescriptors;
import org.openjdk.jmc.joverflow.descriptors.CollectionInstanceDescriptor;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.util.MiscUtils;

/**
 * Detects "vertical bar"-shaped multi-dimensional arrays and ArrayLists/Vectors: those where the
 * "vertical" (outer) dimension is considerably bigger than the "horizonal" one, i.e. which consist
 * of a large number of short sub-arrays. Since each sub-array has its own header plus a pointer to
 * it from the outer array, "vertical bar" arrays can create significant overhead
 */
class BarArrayHandler {
	private final JavaHeapObject[] elements;
	private final int outerDim;
	private final CollectionDescriptors colDescriptors;

	/**
	 * Returns an instance of BarArrayHandler if the given collection is an ArrayList, Vector or a
	 * subclass of one of these, and its size is greater than one. Otherwise, returns null.
	 */
	static BarArrayHandler createInstance(CollectionInstanceDescriptor colDesc, CollectionDescriptors colDescriptors) {
		JavaClass clazz = colDesc.getClassDescriptor().getClazz();
		if (!isArrayListOrVector(clazz)) {
			return null;
		}
		int numEls = colDesc.getNumElements();
		if (numEls <= 1) {
			return null;
		}
		ArrayBasedCollectionDescriptor arrayColDesc = (ArrayBasedCollectionDescriptor) colDesc;
		JavaObjectArray elsArray = arrayColDesc.getElementsArray();
		if (elsArray == null) {
			return null; // Unresolved
		}
		JavaHeapObject[] elements = elsArray.getElements();

		return new BarArrayHandler(elements, numEls, colDescriptors);
	}

	/**
	 * Returns an instance of BarArrayHandler for the given standalone array, if its size is greater
	 * than one.
	 */
	static BarArrayHandler createInstance(JavaHeapObject[] elements, CollectionDescriptors colDescriptors) {
		if (elements.length <= 1) {
			return null;
		}
		return new BarArrayHandler(elements, elements.length, colDescriptors);
	}

	private BarArrayHandler(JavaHeapObject[] elements, int numEls, CollectionDescriptors colDescriptors) {
		this.elements = elements;
		this.outerDim = numEls;
		this.colDescriptors = colDescriptors;
	}

	/**
	 * Checks if the array or collection associated with this object has a "vertical bar" shape. If
	 * so, returns the overhead; otherwise returns 0.
	 */
	int calculateOverhead() {
		Snapshot snapshot = colDescriptors.getSnapshot();
		int ptrSize = snapshot.getPointerSize();
		int arrHeaderSize = snapshot.getArrayHeaderSize();

		int maxInnerDim = 0, minInnerDim = Integer.MAX_VALUE;
		@SuppressWarnings("unused")
		int totalSubArraysLen = 0, numNonNullEls = 0;
		JavaClass subArrayClazz = null;

		// Current two-dimensional array's overhead due to array header and object
		// alignment for each sub-array
		int oldInnerArraysOvhd = 0;
		int elementSize = 0;
		int subCollectionObjSize = 0;

		for (int i = 0; i < outerDim; i++) {
			JavaHeapObject line = elements[i];
			if (line == null) {
				continue;
			}

			JavaClass clazz = line.getClazz();
			if (subArrayClazz != null) {
				// Make sure all elements of the top array have the same type
				if (clazz != subArrayClazz) {
					return 0;
				}
			} else {
				subArrayClazz = clazz;
				if (!clazz.isArray()) {
					subCollectionObjSize = clazz.getInstanceSize();
				}
			}

			int subArrayLen;

			if (line instanceof JavaObjectArray) {
				JavaObjectArray objSubArray = (JavaObjectArray) line;
				subArrayLen = objSubArray.getLength();
				elementSize = ptrSize;
			} else if (line instanceof JavaValueArray) {
				JavaValueArray valueSubArray = (JavaValueArray) line;
				subArrayLen = valueSubArray.getLength();
				if (elementSize == 0) {
					elementSize = valueSubArray.getElementSize();
				}
			} else {
				if (!isArrayListOrVector(clazz)) {
					return 0;
				}
				CollectionInstanceDescriptor subColDesc = colDescriptors.getDescriptor((JavaObject) line);
				subArrayLen = subColDesc.getNumElements();
				elementSize = ptrSize;
			}

			numNonNullEls++;
			totalSubArraysLen += subArrayLen;
			if (subArrayLen > maxInnerDim) {
				maxInnerDim = subArrayLen;
			}
			if (subArrayLen < minInnerDim) {
				minInnerDim = subArrayLen;
			}

			int unalignedLineSize = subArrayLen * elementSize + arrHeaderSize;
			int alignedLineSize = MiscUtils.getAlignedObjectSize(unalignedLineSize, snapshot.getObjectAlignment());
			oldInnerArraysOvhd += arrHeaderSize + (alignedLineSize - unalignedLineSize);
		}

		// If all subarrays are null, this kind of overhead is undefined
		if (numNonNullEls == 0) {
			return 0;
		}
		// Check if this is actually not a "vertical-bar" shaped array
		if (maxInnerDim >= outerDim) {
			return 0;
		}

		int outerPtrOvhd = (outerDim - maxInnerDim) * ptrSize;
		int innerColOvhd = (outerDim - maxInnerDim) * subCollectionObjSize;

		// Calculate inner arrays overhead in the "new" array, where we swap
		// dimensions, so that there are maxInnerDim lines each with outerDim elements
		int unalignedLineSize = outerDim * elementSize + arrHeaderSize;
		int alignedLineSize = MiscUtils.getAlignedObjectSize(unalignedLineSize, snapshot.getObjectAlignment());
		int newInnerArraysOvhd = (arrHeaderSize + alignedLineSize - unalignedLineSize) * maxInnerDim;

		int innerArraysOvhd = oldInnerArraysOvhd - newInnerArraysOvhd;

		int ovhd = outerPtrOvhd + innerColOvhd + innerArraysOvhd;
		return ovhd;
	}

	private static boolean isArrayListOrVector(JavaClass clazz) {
		return clazz.isOrSubclassOf(Constants.ARRAY_LIST) || clazz.isOrSubclassOf(Constants.VECTOR);
	}
}
