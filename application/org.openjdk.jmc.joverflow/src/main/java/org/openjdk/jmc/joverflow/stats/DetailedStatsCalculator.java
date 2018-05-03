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

import java.util.ArrayList;
import java.util.HashMap;

import org.openjdk.jmc.joverflow.descriptors.CollectionClassDescriptor;
import org.openjdk.jmc.joverflow.descriptors.CollectionDescriptors;
import org.openjdk.jmc.joverflow.descriptors.CollectionInstanceDescriptor;
import org.openjdk.jmc.joverflow.heap.model.HeapStringReader;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.CachedReadBuffer;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.support.DupStringStats;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.support.ProblemRecorder;

/**
 * This class handles heap dump objects that are given to it by the instance of HeapScaner (that is
 * created in its constructor). The HeapScaner scans the heap from GC roots, keeping the reference
 * chain from a GC root to the current object at all times. This class analyzes each scanned object
 * for various kinds of problems, and records problem type/overhead if anything found via the
 * supplied instance of {@link org.openjdk.jmc.joverflow.support.ProblemRecorder}. See the latter for
 * more information on problems and object kinds that they can occur on.
 */
class DetailedStatsCalculator implements ProblemChecker, Constants {
	private final Snapshot snapshot;
	private final HeapScaner scaner;
	private final ProblemRecorder problemRecorder;
	private final InterimRefChain refChain;
	private final CollectionDescriptors colDescriptors;

	private final HeapStats heapStats;

	private final int ptrSize, objHeaderSize, arrayHeaderSize;

	private int numCols;
	@SuppressWarnings("unused")
	private long totalColImplSize; // May use in future
	private int numEmptyUnusedCols, numEmptyUsedCols, numEmptyCols, numSmallCols;
	private int numSparseSmallCols, numSparseLargeCols, numBoxedNumberCols, numBarCols;
	private long emptyUsedColsOvhd, emptyUnusedColsOvhd, emptyColsOvhd, smallColsOvhd;
	private long sparseSmallColsOvhd, sparseLargeColsOvhd, boxedNumberColsOvhd, barColsOvhd;

	private int numObjArrays;
	@SuppressWarnings("unused")
	private long totalObjArraysShallowSize; // May use in future
	private int numLengthZeroObjArrays, numLengthOneObjArrays, numEmptyObjArrays;
	private int numSparseArrays, numBoxedNumberArrays, numBarObjArrays;
	private long lengthZeroObjArraysOvhd, lengthOneObjArraysOvhd, emptyObjArraysOvhd;
	private long sparseObjArraysOvhd, boxNumObjArraysOvhd, barObjArraysOvhd;

	private int numValueArrays;
	private int numLengthZeroValueArrays, numLengthOneValueArrays, numEmptyValueArrays;
	private int numLZTValueArrays, numUnusedHiBytesValueArrays;
	private long lengthZeroValueArraysOvhd, lengthOneValueArraysOvhd, emptyValueArraysOvhd;
	private long lztValueArraysOvhd, unusedHiBytesArraysOvhd;

	// Handling duplicate Strings
	private final HeapStringReader stringReader;
	private final int stringInstShallowSize;
	private final DupStringHandler dupStringHandler;
	private final DupArrayHandler dupArrayHandler;

	public DetailedStatsCalculator(Snapshot snapshot, HeapStats heapStats, ProblemRecorder problemRecorder,
			boolean useBreadthFirstScan) {
		this.snapshot = snapshot;
		this.problemRecorder = problemRecorder;
		colDescriptors = new CollectionDescriptors(snapshot);
		scaner = useBreadthFirstScan ? new BreadthFirstHeapScanner(snapshot, this, problemRecorder)
				: new DepthFirstHeapScaner(snapshot, this, problemRecorder, colDescriptors);
		refChain = scaner.getRefChain();

		this.heapStats = heapStats;
		ptrSize = snapshot.getPointerSize();
		objHeaderSize = snapshot.getObjectHeaderSize();
		arrayHeaderSize = objHeaderSize + 4;

		DupStringStats dupStringStats = heapStats.dupStringStats;
		stringReader = snapshot.getStringReader();
		stringInstShallowSize = dupStringStats.stringInstShallowSize;
		dupStringHandler = new DupStringHandler(stringReader, dupStringStats.dupStrings, refChain,
				stringInstShallowSize);

		dupArrayHandler = new DupArrayHandler(heapStats.dupArrayStats.dupArrays, refChain);

		for (JavaClass clazz : snapshot.getClasses()) {
			if (clazz.isArray()) {
				continue;
			}
			clazz.setAttachment(DataFieldStats.newInstance(clazz));
		}
	}

	/**
	 * Invokes methods of HeapScaner, which results in callbacks into this class, that perform
	 * detailed stats calculations. In the end, updates the instance of HeapStats passed to the
	 * constructor.
	 */
	public void calculate() throws HprofParsingCancelledException {
		scaner.analyzeViaRoots();
		scaner.analyzeViaAllObjectsEnum();
		scaner.done();

		// Collect the contents of java.lang.System.props table (this is the one returned by System.getProperties())
		HashMap<String, String> systemProps = SystemPropertiesReader.readProperties(snapshot, colDescriptors);

		// IMPORTANT: should do this so that optimizations in CachedReadBuffer do not
		// cause problems if objects are read from the dump again and repeatedly, for
		// example by the GUI JOverflow tool.
		ReadBuffer readBuf = snapshot.getReadBuffer();
		if (readBuf instanceof CachedReadBuffer) {
			((CachedReadBuffer) readBuf).incrementPass();
		}

		ArrayList<CollectionClassDescriptor> overheadsByClass = colDescriptors.getOverheadsByClass();
		ObjectHistogram objHisto = new ObjectHistogram(snapshot);

		heapStats.setObjectHistogram(objHisto)
				.setCollectionNumberStats(numCols, numEmptyUnusedCols, numEmptyUsedCols, numEmptyCols, numSmallCols,
						numSparseSmallCols, numSparseLargeCols, numBoxedNumberCols, numBarCols)
				.setCollectionOverhead(emptyUnusedColsOvhd, emptyUsedColsOvhd, emptyColsOvhd, smallColsOvhd,
						sparseSmallColsOvhd, sparseLargeColsOvhd, boxedNumberColsOvhd, barColsOvhd)
				.setObjArrayNumberStats(numObjArrays, numLengthZeroObjArrays, numLengthOneObjArrays, numEmptyObjArrays,
						numSparseArrays, numBoxedNumberArrays, numBarObjArrays)
				.setObjArrayOverhead(lengthZeroObjArraysOvhd, lengthOneObjArraysOvhd, emptyObjArraysOvhd,
						sparseObjArraysOvhd, boxNumObjArraysOvhd, barObjArraysOvhd)
				.setCollectionOverheadByClass(overheadsByClass)
				.setValueArrayNumberStats(numValueArrays, numLengthZeroValueArrays, numLengthOneValueArrays,
						numEmptyValueArrays, numLZTValueArrays, numUnusedHiBytesValueArrays)
				.setValueArrayOverhead(lengthZeroValueArraysOvhd, lengthOneValueArraysOvhd, emptyValueArraysOvhd,
						lztValueArraysOvhd, unusedHiBytesArraysOvhd)
				.setSystemProperties(systemProps);
	}

	@Override
	public CollectionInstanceDescriptor handleInstance(JavaObject obj, JavaThing[] fields) {
		JavaClass clazz = obj.getClazz();
		DataFieldStats fieldStats = (DataFieldStats) clazz.getAttachment();
		fieldStats.handleFields(fields);

		if (obj.isVisitedAsCollectionImpl()) {
			return null;
		}

		if (clazz.isCollection()) {
			return handleCollection(obj);
		} else {
			clazz.updateInclusiveInstanceSize(clazz.getInstanceSize());
			if (problemRecorder.shouldRecordGoodInstance(obj)) {
				refChain.recordCurrentRefChainForGoodInstance(obj);
			}
			return null;
		}
	}

	/**
	 * For an object that is a known collection, checks if it has any problems. If a problem is
	 * found, it's recorded along with the current reference chain from GC root. Also records the
	 * implementation-inclusive size of this collection in its JavaClass, unless this object happens
	 * to be a part of implementation of another collection (like HashMap in HashSet). When
	 * impl-inclusive size is determined in {@link CollectionInstanceDescriptor#getImplSize()}, all
	 * the collection impl-n objects, e.g. HashMap$Entry, are marked with
	 * {@link JavaLazyReadObject#setVisitedAsCollectionImpl()}. This is important, since such
	 * objects are half-ignored later by handleInstance() method above.
	 */
	private CollectionInstanceDescriptor handleCollection(JavaObject col) {
		CollectionInstanceDescriptor colDesc = colDescriptors.getDescriptor(col);
		CollectionClassDescriptor classDesc = colDesc.getClassDescriptor();

		// Check if this collection is in the implementation (via encapsulation) of
		// another one. For example, an instance of java.util.HashMap is encapsulated
		// by an instance of java.util.HashSet. In this case, the current collection
		// should not be inspected for overhead on its own.
		JavaObject potentialParentCol = refChain.getPointingJavaObject();
		if (potentialParentCol != null) {
			if (classDesc.isInImplementationOf(potentialParentCol.getClazz().getName())) {
				return null;
			}
		}

		numCols++;
		// Get impl-inclusive size and mark collection implementation objects
		int implSize = colDesc.getImplSize();

		col.getClazz().updateInclusiveInstanceSize(implSize);
		totalColImplSize += implSize;

		// Check if this collection is empty. A collection with this problem cannot
		// have other problems.
		int nEls = colDesc.getNumElements();
		if (nEls == 0) {
			ProblemKind problemKind;
			if (colDesc.getClassDescriptor().canDetermineModCount()) {
				if (colDesc.getModCount() != 0) {
					problemKind = ProblemKind.EMPTY_USED;
					numEmptyUsedCols++;
					emptyUsedColsOvhd += implSize;
				} else {
					problemKind = ProblemKind.EMPTY_UNUSED;
					numEmptyUnusedCols++;
					emptyUnusedColsOvhd += implSize;
				}
			} else {
				problemKind = ProblemKind.EMPTY;
				numEmptyCols++;
				emptyColsOvhd += implSize;
			}
			classDesc.addProblematicCollection(problemKind, implSize);
			refChain.recordCurrentRefChainForColCluster(col, colDesc, problemKind, implSize);
//			System.out.println("Empty collection, impl size = " + implSize);
			return colDesc;
		}

		int ovhd;
		boolean goodCollection = true;

		// Check if this collection is sparse
		if (colDesc instanceof CollectionInstanceDescriptor.CapacityDifferentFromSize) {
			CollectionInstanceDescriptor.CapacityDifferentFromSize arColDesc = (CollectionInstanceDescriptor.CapacityDifferentFromSize) colDesc;
			ovhd = arColDesc.getSparsenessOverhead(ptrSize);
			if (ovhd > 0) {
				goodCollection = false;
				ProblemKind problemKind;
				if (arColDesc.getCapacity() <= arColDesc.getDefaultCapacity()) {
					problemKind = ProblemKind.SPARSE_SMALL;
					numSparseSmallCols++;
					sparseSmallColsOvhd += ovhd;
				} else {
					problemKind = ProblemKind.SPARSE_LARGE;
					numSparseLargeCols++;
					sparseLargeColsOvhd += ovhd;
				}
				classDesc.addProblematicCollection(problemKind, ovhd);
				refChain.recordCurrentRefChainForColCluster(col, colDesc, problemKind, ovhd);
//				System.out.println(
//						problemKind + " collection, nEls = " + nEls + ", capacity = " + capacity + ", ovhd = " + ovhd);
			}
		}

		if (nEls <= SMALL_COL_MAX_SIZE) {
			goodCollection = false;
			// Calculate overhead as a number of bytes we save if we replace this data
			// structure with an array of objects (or two, for maps). The array's own
			// overhead is its header. The formula below is still not ideal, because the
			// user likely won't be able to keep the exact-size array for each small
			// collection - instead, they would probably have to use arrays of the same
			// (highest) fixed size for all collections created at the same place in the code.
			int multiplier = colDesc.getClassDescriptor().isMap() ? 2 : 1;
			ovhd = colDesc.getImplSize() - multiplier * (nEls * ptrSize + arrayHeaderSize);

			numSmallCols++;
			smallColsOvhd += ovhd;
			classDesc.addProblematicCollection(ProblemKind.SMALL, ovhd);
			refChain.recordCurrentRefChainForColCluster(col, colDesc, ProblemKind.SMALL, ovhd);
		}

		// Check if this collection contains boxed numbers.
		/*
		 * TODO: Our calculations for boxed arrays are much more precise, since they take into
		 * account possible multiple pointers to the same boxed object. To implement the same for a
		 * collection, we need to iterate all its elements, which may be time-consuming...
		 */
		ovhd = 0;
		if (classDesc.isMap()) {
			JavaHeapObject[] entryObjs = colDesc.getSampleKeyAndValue();
			int totalObjSize = 0, totalBoxedSize = 0, numPtrs = 0;
			for (JavaHeapObject keyOrValue : entryObjs) {
				if (keyOrValue == null) {
					continue;
				}
				int boxedNumSize = keyOrValue.getClazz().getBoxedNumberSize();
				if (boxedNumSize > 0) {
					totalBoxedSize += boxedNumSize;
					totalObjSize += keyOrValue.getSize();
					numPtrs++;
				}
			}
			if (totalBoxedSize > 0) {
				// Take into account what happens if we replace this with an array of numbers,
				// with a normal array header size.
				ovhd = colDesc.getImplSize() + (totalObjSize + ptrSize * numPtrs - totalBoxedSize) * nEls
						- arrayHeaderSize * numPtrs;
			}
		} else {
			JavaHeapObject obj = colDesc.getSampleElement();
			if (obj != null) {
				int boxedNumSize = obj.getClazz().getBoxedNumberSize();
				if (boxedNumSize > 0) {
					ovhd = colDesc.getImplSize() + (obj.getSize() + ptrSize - boxedNumSize) * nEls - arrayHeaderSize;
				}
			}
		}

		if (ovhd > 0) {
			goodCollection = false;
			numBoxedNumberCols++;
			boxedNumberColsOvhd += ovhd;
			classDesc.addProblematicCollection(ProblemKind.BOXED, ovhd);
			refChain.recordCurrentRefChainForColCluster(col, colDesc, ProblemKind.BOXED, ovhd);
		}

		// Check if this is a WeakHashMap or its subclass, in which elements have hard
		// references back to keys.
		WeakMapHandler wmHandler = WeakMapHandler.createInstance(colDesc);
		if (wmHandler != null) {
			WeakMapHandler.Result result = wmHandler.calculateOverhead();
			if (result != null) {
//				numBadWeakCols++;
//				badWeakColsOverhead += ovhd;
				goodCollection = false;
				classDesc.addProblematicCollection(ProblemKind.WEAK_MAP_WITH_BACK_REFS, result.overhead);
				refChain.recordCurrentRefChainForWeakHashMapWithBackRefs(col, colDesc, result.overhead,
						result.valueTypeAndFieldSample);
			}
		}

		BarArrayHandler barHandler = BarArrayHandler.createInstance(colDesc, colDescriptors);
		if (barHandler != null) {
			ovhd = barHandler.calculateOverhead();
			if (ovhd > 0) {
				goodCollection = false;
				numBarCols++;
				barColsOvhd += ovhd;
				classDesc.addProblematicCollection(ProblemKind.BAR, ovhd);
				refChain.recordCurrentRefChainForColCluster(col, colDesc, ProblemKind.BAR, ovhd);
			}
		}

		if (goodCollection) { // No defects found for this collection
			refChain.recordCurrentRefChainForGoodCollection(col, colDesc);
		}

		return colDesc;
	}

	@Override
	public void handleObjectArray(JavaObjectArray objArray, JavaHeapObject[] elements) {
		if (objArray.isVisitedAsCollectionImpl()) {
			return;
		}

		numObjArrays++;
		int arraySize = objArray.getSize();
		totalObjArraysShallowSize += arraySize;
		objArray.getClazz().updateInclusiveInstanceSize(arraySize);

		boolean goodArray = true;

		if (elements.length == 0) {
//			goodArray = false;
			CollectionClassDescriptor classDesc = colDescriptors.getStandaloneArrayDescriptor(objArray);
			numLengthZeroObjArrays++;
			int ovhd = arraySize;
			lengthZeroObjArraysOvhd += ovhd;
			classDesc.addProblematicCollection(ProblemKind.LENGTH_ZERO, ovhd);
			refChain.recordCurrentRefChainForColCluster(objArray, new ArrayObjDescriptor(classDesc, 0, arraySize),
					ProblemKind.LENGTH_ZERO, ovhd);
			return;
		}

		if (elements.length == 1) {
			goodArray = false;
			CollectionClassDescriptor classDesc = colDescriptors.getStandaloneArrayDescriptor(objArray);
			numLengthOneObjArrays++;
			int ovhd = arraySize;
			lengthOneObjArraysOvhd += ovhd;
			classDesc.addProblematicCollection(ProblemKind.LENGTH_ONE, ovhd);
			refChain.recordCurrentRefChainForColCluster(objArray, new ArrayObjDescriptor(classDesc, 0, arraySize),
					ProblemKind.LENGTH_ONE, ovhd);
		}

		int nNullEntries = 0;
		boolean boxedNumsPresent = false;
		int totalBoxedNumOvhd = 0;
		for (JavaHeapObject element : elements) {
			if (element != null) {
				int primitiveNumSize = element.getClazz().getBoxedNumberSize();
				if (primitiveNumSize > 0) {
					boxedNumsPresent = true;
					// Below is how much memory we would save (or maybe lose) if we replace a
					// pointer with the primitive type array slot
					totalBoxedNumOvhd += (ptrSize - primitiveNumSize);
					JavaLazyReadObject elementObj = (JavaLazyReadObject) element;
					// If the same Number object is referenced from two places, don't count it twice
					if (!elementObj.isVisitedAsOther()) {
						elementObj.setVisitedAsOther();
						totalBoxedNumOvhd += element.getSize(); // Savings from getting rid of boxed Number
					}
				}
			} else {
				nNullEntries++;
			}
		}

		CollectionClassDescriptor classDesc = colDescriptors.getStandaloneArrayDescriptor(objArray);
		ArrayObjDescriptor arrayDesc = new ArrayObjDescriptor(classDesc, elements.length, arraySize);

		if (nNullEntries > elements.length / 2) {
			// Empty or sparse object array dangling from something other than a known collection
			goodArray = false;
			if (nNullEntries == elements.length) {
				numEmptyObjArrays++;
				int ovhd = objArray.getSize();
				emptyObjArraysOvhd += ovhd;
				classDesc.addProblematicCollection(ProblemKind.EMPTY, ovhd);
				refChain.recordCurrentRefChainForColCluster(objArray, arrayDesc, ProblemKind.EMPTY, ovhd);
			} else {
				numSparseArrays++;
				int ovhd = nNullEntries * ptrSize;
				sparseObjArraysOvhd += ovhd;
				classDesc.addProblematicCollection(ProblemKind.SPARSE_ARRAY, ovhd);
				refChain.recordCurrentRefChainForColCluster(objArray, arrayDesc, ProblemKind.SPARSE_ARRAY, ovhd);
			}
		}

		if (boxedNumsPresent) {
			numBoxedNumberArrays++;
			// In extreme cases, the overhead of boxed numbers can actually be negative. For example,
			// with 4-byte pointers, if we have 20 elements of Double[] array pointing at a single
			// Double object, we will use 4*20 + 16 = 96 bytes. However, a double[] array of the same
			// size would use 8*20 = 160 bytes. Thus we count all boxed arrays for consistency above,
			// but we add up the overhead and store the details only for those where overhead is real.
			if (totalBoxedNumOvhd > 0) {
				goodArray = false;
				boxNumObjArraysOvhd += totalBoxedNumOvhd;
				classDesc.addProblematicCollection(ProblemKind.BOXED, totalBoxedNumOvhd);
				refChain.recordCurrentRefChainForColCluster(objArray, arrayDesc, ProblemKind.BOXED, totalBoxedNumOvhd);
			}
		}

		BarArrayHandler barHandler = BarArrayHandler.createInstance(elements, colDescriptors);
		if (barHandler != null) {
			int ovhd = barHandler.calculateOverhead();
			if (ovhd > 0) {
				goodArray = false;
				numBarObjArrays++;
				barObjArraysOvhd += ovhd;
				classDesc.addProblematicCollection(ProblemKind.BAR, ovhd);
				refChain.recordCurrentRefChainForColCluster(objArray, arrayDesc, ProblemKind.BAR, ovhd);
			}
		}

		if (goodArray) { // No defects found for this array
			refChain.recordCurrentRefChainForGoodCollection(objArray, arrayDesc);
		}
	}

	@Override
	public void handleValueArray(JavaValueArray valueArray) {
		if (valueArray.isVisitedAsCollectionImpl()) {
			return;
		}

		numValueArrays++;
		valueArray.getClazz().updateInclusiveInstanceSize(valueArray.getSize());
		boolean goodArray = true;

		byte[] data = valueArray.getValue();
		int elSize = valueArray.getElementSize();
		int numElements = data.length / elSize;
		CollectionClassDescriptor classDesc = colDescriptors.getStandaloneArrayDescriptor(valueArray);
		ArrayObjDescriptor arrayDesc = new ArrayObjDescriptor(classDesc, numElements, valueArray.getSize());
		char elementType = valueArray.getElementType();

		PrimitiveArrayHandler pah = PrimitiveArrayHandler.createInstance(data, elSize,
				elementType == 'F' || elementType == 'D');

		if (pah.isLength0()) {
//			goodArray = false;
			numLengthZeroValueArrays++;
			int ovhd = valueArray.getSize();
			lengthZeroValueArraysOvhd += ovhd;
			classDesc.addProblematicCollection(ProblemKind.LENGTH_ZERO, ovhd);
			refChain.recordCurrentRefChainForColCluster(valueArray, arrayDesc, ProblemKind.LENGTH_ZERO, ovhd);
			return;
		}

		if (pah.isLength1()) {
			goodArray = false;
			numLengthOneValueArrays++;
			int ovhd = valueArray.getSize() + ptrSize - elSize;
			lengthOneValueArraysOvhd += ovhd;
			classDesc.addProblematicCollection(ProblemKind.LENGTH_ONE, ovhd);
			refChain.recordCurrentRefChainForColCluster(valueArray, arrayDesc, ProblemKind.LENGTH_ONE, ovhd);
		}
		if (pah.isEmpty()) {
			goodArray = false;
			numEmptyValueArrays++;
			int ovhd = valueArray.getSize();
			emptyValueArraysOvhd += ovhd;
			classDesc.addProblematicCollection(ProblemKind.EMPTY, ovhd);
			refChain.recordCurrentRefChainForColCluster(valueArray, arrayDesc, ProblemKind.EMPTY, ovhd);
		}
		int ovhd = pah.getLztOverhead();
		if (ovhd > 0) {
			goodArray = false;
			numLZTValueArrays++;
			lztValueArraysOvhd += ovhd;
			classDesc.addProblematicCollection(ProblemKind.LZT, ovhd);
			refChain.recordCurrentRefChainForColCluster(valueArray, arrayDesc, ProblemKind.LZT, ovhd);
		}
		ovhd = pah.getUnusedHighBytesOvhd();
		if (ovhd > 0) {
			goodArray = false;
			numUnusedHiBytesValueArrays++;
			unusedHiBytesArraysOvhd += ovhd;
			classDesc.addProblematicCollection(ProblemKind.UNUSED_HI_BYTES, ovhd);
			refChain.recordCurrentRefChainForColCluster(valueArray, arrayDesc, ProblemKind.UNUSED_HI_BYTES, ovhd);
		}

		if (goodArray) {
			refChain.recordCurrentRefChainForGoodCollection(valueArray, arrayDesc);
		}

		dupArrayHandler.handleArray(valueArray);
	}

	/**
	 * Checks the given String for duplication. Additionally, calculates the inclusive size of this
	 * String, that is, the size of the object itself plus the size of its char[] array, unless it
	 * has already been seen before (i.e. this char[] is utilized by more than one String).
	 */
	@Override
	public void handleString(JavaObject strObj) {
		JavaClass stringClazz = strObj.getClazz();
		stringClazz.updateInclusiveInstanceSize(stringInstShallowSize);

		boolean duplicated = dupStringHandler.handleString(strObj);

		int implInclusiveSize = stringInstShallowSize;
		JavaValueArray backingCharArray = duplicated ? dupStringHandler.getLastReadBackingArray()
				: stringReader.getCharArrayForString(strObj);
		if (backingCharArray != null) { // Not sure why we can get null here - truncated heap dumps?
			if (!backingCharArray.isVisited()) {
				int backingCharArraySize = backingCharArray.getSize();
				stringClazz.updateInclusiveInstanceSize(backingCharArraySize);
				backingCharArray.setVisited();
				implInclusiveSize += backingCharArraySize;
				scaner.incrementCurrentProcessedObjNo();
			}
		}

		if (!duplicated) {
			// Normal, non-duplicated string. Record it, so that eventually for fields
			// pointing at duplicated strings we also know how many "good" strings they
			// also point to.
			refChain.recordCurrentRefChainForNonDupString(strObj, implInclusiveSize);
		}
	}

	public int getProgressPercentage() {
		return scaner.getProgressPercentage();
	}

	public void cancelCalculation() {
		scaner.cancelCalculation();
	}

	/**
	 * A collection instance descriptor that's instantiated and used for any object array.
	 */
	private static class ArrayObjDescriptor implements CollectionInstanceDescriptor {

		private static final String NOT_SUPPORTED = "is not supported for arrays";

		private final CollectionClassDescriptor classDesc;
		private final int numElements, arraySize;

		ArrayObjDescriptor(CollectionClassDescriptor classDesc, int numElements, int arraySize) {
			this.classDesc = classDesc;
			this.numElements = numElements;
			this.arraySize = arraySize;
		}

		@Override
		public CollectionClassDescriptor getClassDescriptor() {
			return classDesc;
		}

		@Override
		public int getNumElements() {
			return numElements;
		}

		@Override
		public int getImplSize() {
			return arraySize;
		}

		@Override
		public void iterateList(ListIteratorCallback cb) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void iterateMap(MapIteratorCallback cb) {
			throw new UnsupportedOperationException();
		}

		@Override
		public JavaHeapObject getSampleElement() {
			throw new UnsupportedOperationException("Getting sample element " + NOT_SUPPORTED);
		}

		@Override
		public JavaHeapObject[] getSampleKeyAndValue() {
			throw new UnsupportedOperationException("Getting sample key/value " + NOT_SUPPORTED);
		}

		@Override
		public long getModCount() {
			throw new UnsupportedOperationException("Getting modCount " + NOT_SUPPORTED);
		}

		@Override
		public boolean hasExtraObjFields() {
			return false;
		}

		@Override
		public void filterExtraObjFields(JavaThing[] fields) {
			throw new UnsupportedOperationException("Filtering extra obj fields " + NOT_SUPPORTED);
		}
	}
}
