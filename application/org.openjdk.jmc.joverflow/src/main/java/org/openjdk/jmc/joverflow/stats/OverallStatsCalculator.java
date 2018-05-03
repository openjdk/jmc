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

import java.util.Collection;

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.CachedReadBuffer;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.support.DupArrayStats;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.support.ShortArrayStats;
import org.openjdk.jmc.joverflow.util.ObjectToIntMap;

/**
 * Functionality for calculating overall, generally high-level stats about objects in the heap.
 */
class OverallStatsCalculator implements Constants {
	private final Snapshot snapshot;
	private final int ptrSize;

	private int nObjs, nObjs2ndPass;
	private volatile boolean cancelled;

	public OverallStatsCalculator(Snapshot snapshot) {
		this.snapshot = snapshot;
		ptrSize = snapshot.getPointerSize();
	}

	/**
	 * Calculates and returns the overall object stats.
	 *
	 * @throws org.openjdk.jmc.joverflow.heap.parser.DumpCorruptedException.Runtime
	 *             if reading some object from heap dump uncovers that the file is corrupted
	 */
	public HeapStats calculate() throws HprofParsingCancelledException {
		int objHeaderSize = snapshot.getObjectHeaderSize();
		long arrHeaderSize = snapshot.getArrayHeaderSize(); // Made long to get long result when multiplying by int

		StringStatsCollector stringStatsCollector = new StringStatsCollector(snapshot);
		PrimitiveArrayDuplicationMap arrayDupMap = new PrimitiveArrayDuplicationMap(snapshot);

		Collection<JavaLazyReadObject> allObjects = snapshot.getObjects();

		nObjs = 0;
		int nInstances = 0, nObjectArrays = 0;
		long totalObjectSize = 0, totalInstSize = 0, totalObjArraySize = 0;
		int nEntryInstances = 0;
		long entryClassSize = 0;
		int n0LenObjArrays = 0, n1ObjArrays = 0, n4ObjArrays = 0, n8ObjArrays = 0;
		int n0LenValArrays = 0, n1LenValArrays = 0, n4LenValArrays = 0, n8LenValArrays = 0;
		int lenZeroObjArraySize = 0, lenOneObjArraySize = 0;
		int nBoxedNumbers = 0;
		long ovhdBoxedNumbers = 0;

		int curChunk = 0;

		ObjectToIntMap<String> unmodifiableClassInstanceCount = new ObjectToIntMap<>(15);
		ObjectToIntMap<String> synchronizedClassInstanceCount = new ObjectToIntMap<>(15);

		for (JavaLazyReadObject obj : allObjects) {
			nObjs++;

			JavaClass clazz = obj.getClazz();
			clazz.incNumInstances();
			String clazzName = clazz.getName();
			int objSize = obj.getSize();
			totalObjectSize += objSize;

			if (obj instanceof JavaObject) {
				nInstances++;
				totalInstSize += objSize;

				int primitiveNumSize;

				if (clazzName.endsWith("$Entry")) {
					nEntryInstances++;
					entryClassSize += objSize;
				} else if (clazzName.startsWith("java.util.Collections$Unmodifiable")) {
					unmodifiableClassInstanceCount.putOneOrIncrement(clazzName);
				} else if (clazzName.startsWith("java.util.Collections$Synchronized")) {
					synchronizedClassInstanceCount.putOneOrIncrement(clazzName);
				} else if (clazz.isString()) {
					stringStatsCollector.add((JavaObject) obj);
				} else if ((primitiveNumSize = clazz.getBoxedNumberSize()) != 0) {
					nBoxedNumbers++;
					ovhdBoxedNumbers += objSize - primitiveNumSize + ptrSize;
				}

			} else if (obj instanceof JavaObjectArray) {
				nObjectArrays++;
				totalObjArraySize += objSize;
				clazz.updateShallowInstanceSize(objSize);
				JavaObjectArray objArray = (JavaObjectArray) obj;
				int length = objArray.getLength();

				if (length == 0) {
					n0LenObjArrays++;
					if (lenZeroObjArraySize == 0) {
						lenZeroObjArraySize = objArray.getSize();
					}
				} else if (length == 1) {
					n1ObjArrays++;
					if (lenOneObjArraySize == 0) {
						lenOneObjArraySize = objArray.getSize();
					}
				} else if (length <= 4) {
					n4ObjArrays++;
				} else if (length <= 8) {
					n8ObjArrays++;
				}
			} else if (obj instanceof JavaValueArray) {
				clazz.updateShallowInstanceSize(objSize);
				JavaValueArray valArray = (JavaValueArray) obj;
				int length = valArray.getLength();
				if (length == 0) {
//					System.out.println("Zero-length val array: " + obj + "   , size = " + objSize);
					n0LenValArrays++;
				} else if (length == 1) {
					n1LenValArrays++;
				} else if (length <= 4) {
					n4LenValArrays++;
				} else if (length <= 8) {
					n8LenValArrays++;
				}

				// Performance optimization: scan as many primitive arrays as possible on
				// the first pass, although there is a separate 2nd pass for them. However,
				// reading more objects from disk now improves cache locality.
				if (!(clazz.isCharArray() || clazz.isByteArray())) {
					// This array, because of its type, is guaranteed to not belong to a String
					arrayDupMap.add(valArray);
				}
			}

//			System.out.println(obj + "   , size = " + objSize);

			int newCurChunk = nObjs >> 17; // Check every 128K objects
			if (newCurChunk > curChunk) {
				curChunk = newCurChunk;
				if (cancelled) {
					throw new HprofParsingCancelledException();
				}
			}
		}

		long ovhdObjectHeaders = nObjs * objHeaderSize;

		ObjectToIntMap.Entry<String>[] unmodifiableClasses = unmodifiableClassInstanceCount
				.getEntriesSortedByValueThenKey();
		ObjectToIntMap.Entry<String>[] synchronizedClasses = synchronizedClassInstanceCount
				.getEntriesSortedByValueThenKey();

		// Do one more pass, this time to uncover duplicated primitive arrays.
		// We could not do it on the previous pass, because there we generally
		// unable to distinguish standalone char[] arrays from those that are
		// backing Strings.
		curChunk = 0;

		for (JavaLazyReadObject obj : allObjects) {
			nObjs2ndPass++; // This is pure progress tracking
			if (!(obj instanceof JavaValueArray)) {
				continue;
			}
			JavaClass clazz = obj.getClazz();
			// Ignore if not a char[] or byte[] array - should have been scanned on previous pass
			if (!(clazz.isCharArray() || clazz.isByteArray())) {
				continue;
			}
			// Ignore if it's a char[] array for some String
			if (obj.isVisitedAsCollectionImpl()) {
				continue;
			}

			JavaValueArray valArray = (JavaValueArray) obj;
			arrayDupMap.add(valArray);

			int newCurChunk = nObjs2ndPass >> 17; // Check every 128K objects
			if (newCurChunk > curChunk) {
				curChunk = newCurChunk;
				if (cancelled) {
					throw new HprofParsingCancelledException();
				}
			}
		}

		arrayDupMap.calculateFinalStats();
		DupArrayStats dupArrayStats = new DupArrayStats(arrayDupMap.getNumArrays(), arrayDupMap.getNumUniqueArrays(),
				arrayDupMap.getNumDifferentDupArrayValues(), arrayDupMap.getDupArrays(),
				arrayDupMap.getDupArraysOverhead());

		// IMPORTANT: should do this for optimizations in CachedReadBuffer to work!
		ReadBuffer readBuf = snapshot.getReadBuffer();
		if (readBuf instanceof CachedReadBuffer) {
			((CachedReadBuffer) readBuf).incrementPass();
		}

		ClassloaderStats clStats = new ClassloaderStats(snapshot);

		return new HeapStats()
				.setGeneralStats(ptrSize, objHeaderSize, snapshot.getObjectAlignment(), snapshot.usingNarrowPointers(),
						snapshot.getNumClasses(), nObjs, nInstances, nObjectArrays, totalObjectSize, totalInstSize,
						totalObjArraySize)
				.setObjOverheadStats(ovhdObjectHeaders, nEntryInstances, entryClassSize).setClassloaderStats(clStats)
				.setShortObjArrayStats(new ShortArrayStats(n0LenObjArrays, lenZeroObjArraySize * n0LenObjArrays,
						n1ObjArrays, lenOneObjArraySize * n1ObjArrays, n4ObjArrays, arrHeaderSize * n4ObjArrays,
						n8ObjArrays, arrHeaderSize * n8ObjArrays))
				// TODO: need a better way to calculate overhead for short primitive arrays, at least of size 0 and 1
				// Currently it's likely inconsistent with what is reported by detailed analysis
				.setShortPrimitiveArrayStats(new ShortArrayStats(n0LenValArrays, arrHeaderSize * n0LenValArrays,
						n1LenValArrays, arrHeaderSize * n1LenValArrays, n4LenValArrays, arrHeaderSize * n4LenValArrays,
						n8LenValArrays, arrHeaderSize * n8LenValArrays))
				.setShortStringStats(stringStatsCollector.getShortStringStats())
				.setBoxedNumberStats(nBoxedNumbers, ovhdBoxedNumbers)
				.setWrappedCollectionStats(unmodifiableClasses, synchronizedClasses)
				.setDupStringStats(stringStatsCollector.getDuplicationStats())
				.setCompressibleStringStats(stringStatsCollector.getCompressibleStringStats())
				.setNumberEncodingStringStats(stringStatsCollector.getNumberEncodingStringStats())
				.setStringLengthHistogram(stringStatsCollector.getLengthHistogram()).setDupArrayStats(dupArrayStats);
	}

	/** Used for progress reporting */
	public synchronized int getProgressPercentage() {
		return (int) (((long) ((nObjs * 3 + nObjs2ndPass) / 4) * 100 / snapshot.getNumObjects()));
	}

	public void cancelCalculation() {
		cancelled = true;
	}
}
