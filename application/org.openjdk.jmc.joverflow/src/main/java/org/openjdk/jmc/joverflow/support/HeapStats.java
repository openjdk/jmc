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
package org.openjdk.jmc.joverflow.support;

import java.util.ArrayList;
import java.util.HashMap;

import org.openjdk.jmc.joverflow.descriptors.CollectionClassDescriptor;
import org.openjdk.jmc.joverflow.stats.ClassloaderStats;
import org.openjdk.jmc.joverflow.stats.LengthHistogram;
import org.openjdk.jmc.joverflow.stats.ObjectHistogram;
import org.openjdk.jmc.joverflow.util.ObjectToIntMap;

/**
 * Container for heap statistics. Most of the contained information in it falls in two categories:
 * "overall stats" (higher level information) and 'detailed stats" (lower level, more detailed
 * information). They are filled out by OverallStatsCalculator and DetailedStatsCalculator
 * respectively. This class also references two separate objects, DupStringStats and DupArrayStats,
 * that themselves are filled out in two stages in the same "calculator" classes.
 */
public class HeapStats {
	// ---------------------------- Overall stats -------------------------------
	/** Pointer size */
	public int ptrSize;
	/** Object header size */
	public int objHeaderSize;
	/** Object alignment in memory */
	public int objAlignment;
	/** True if compressed pointers are used in 64-bit mode */
	public boolean usingNarrowPointers;
	/** Total number of classes */
	public int nClasses;

	/** Total number of objects */
	public int nObjects;
	/** Total number of instances */
	public int nInstances;
	/** Total number of object arrays */
	public int nObjectArrays;
	/** Total number of value arrays */
	public int nValueArrays;

	/** Total size of objects */
	public long totalObjSize;
	/** Total instance size */
	public long totalInstSize;
	/** Total object array size */
	public long totalObjArraySize;
	/** Total value array size */
	public long totalValueArraySize;

	/** Overhead of VM object headers */
	public long ovhdObjHeaders;
	/** Number of instances of *$Entry classes */
	public int nEntryInstances;
	/** Overhead of *$Entry classes */
	public long entryClassSize;

	public ClassloaderStats classloaderStats;

	public ShortArrayStats shortStringStats;

	public ShortArrayStats shortPrimitiveArrayStats;

	public ShortArrayStats shortObjArrayStats;

	/** Number of boxed Numbers (e.g. Integer or Long) */
	public int nBoxedNumbers;
	/** Overhead of boxed Numbers */
	public long ovhdBoxedNumbers;

	// Statistics for wrapped Unmodifiable* and Synchronized* collection classes
	public ObjectToIntMap.Entry<String>[] unmodifiableClasses;
	// Not used when printing reports in ReportFormatter. Can perhaps be removed entirely.
//	public ObjectToIntMap.Entry<String>[] synchronizedClasses;

	// --------------------------- Detailed stats -------------------------------

	/** Total number of Collections */
	public int numCols; //
	/** Number of empty, small, sparse, boxed-number and bar Collections */
	public int numEmptyUnusedCols, numEmptyUsedCols, numEmptyCols, numSmallCols, numSparseSmallCols, numSparseLargeCols,
			numBoxedNumberCols, numBarCols;
	/** Overhead caused by the above abnormal Collections */
	public long emptyUnusedColsOverhead, emptyUsedColsOverhead, emptyColsOverhead, smallColsOverhead,
			sparseSmallColsOverhead, sparseLargeColsOverhead, boxedNumberColsOverhead, barColsOverhead;

	/** Total number of standalone object arrays */
	public int numObjArrays;
	/** Number of length 0, length 1, empty, sparse, boxed-number and bar object arrays */
	public int numLengthZeroObjArrays, numLengthOneObjArrays, numEmptyObjArrays, numSparseObjArrays;
	public int numBoxedNumberArrays, numBarObjArrays;
	/** Overhead caused by the above abnormal arrays */
	public long lengthZeroObjArraysOverhead, lengthOneObjArraysOverhead, emptyObjArraysOverhead;
	public long sparseObjArraysOverhead, boxedNumberArraysOverhead, barObjArraysOverhead;

	/** Total number of standalone primitive arrays */
	public int numValueArrays;
	/**
	 * Number of length 0, length 1, empty, long-zero-tail and unused-high-bytes primitive arrays
	 */
	public int numLengthZeroValueArrays, numLengthOneValueArrays;
	public int numEmptyValueArrays, numLZTValueArrays, numUnusedHiBytesValueArrays;
	/** Overhead caused by the above abnormal arrays */
	public long lengthZeroValueArraysOverhead, lengthOneValueArraysOverhead;
	public long emptyValueArraysOverhead, lztValueArraysOverhead, unusedHiBytesValueArraysOverhead;

	/** Overhead stats (empty, sparse, etc) by Collection class */
	public ArrayList<CollectionClassDescriptor> overheadsByClass;

	/** Object histogram containing various stats on memory consumption by objects */
	public ObjectHistogram objHisto;

	/** Stats for duplicate strings, both overall and detailed */
	public DupStringStats dupStringStats;

	/** Stats for compressible strings (those that use only ASCII characters) */
	public CompressibleStringStats compressibleStringStats;

	/** Stats for strings that encode numbers, e.g. ints */
	public NumberEncodingStringStats numberEncodingStringStats;

	/** Stats on length of Strings */
	public LengthHistogram stringLengthHistogram;

	/** Stats for duplicated arrays, both overall and detailed */
	public DupArrayStats dupArrayStats;

	/** Contents of java.lang.System.props table, or null if unresolved etc. */
	public HashMap<String, String> systemProperties;

	// ----------------------------Setting overall stats ------------------------

	public HeapStats setGeneralStats(
		int ptrSize, int objHeaderSize, int objAlignment, boolean usingNarrowPointers, int nClasses, int nObjects,
		int nInstances, int nObjectArrays, long totalObjSize, long totalInstSize, long totalObjArraySize) {
		this.ptrSize = ptrSize;
		this.objHeaderSize = objHeaderSize;
		this.objAlignment = objAlignment;
		this.usingNarrowPointers = usingNarrowPointers;
		this.nClasses = nClasses;
		this.nObjects = nObjects;
		this.nInstances = nInstances;
		this.nObjectArrays = nObjectArrays;
		this.nValueArrays = nObjects - nInstances - nObjectArrays;
		this.totalObjSize = totalObjSize;
		this.totalInstSize = totalInstSize;
		this.totalObjArraySize = totalObjArraySize;
		this.totalValueArraySize = totalObjSize - totalInstSize - totalObjArraySize;
		return this;
	}

	public HeapStats setObjOverheadStats(long ovhdObjHeaders, int nEntryInstances, long entryClassSize) {
		this.ovhdObjHeaders = ovhdObjHeaders;
		this.nEntryInstances = nEntryInstances;
		this.entryClassSize = entryClassSize;
		return this;
	}

	public HeapStats setClassloaderStats(ClassloaderStats clStats) {
		this.classloaderStats = clStats;
		return this;
	}

	public HeapStats setShortObjArrayStats(ShortArrayStats shortArrayStats) {
		this.shortObjArrayStats = shortArrayStats;
		return this;
	}

	public HeapStats setShortPrimitiveArrayStats(ShortArrayStats shortArrayStats) {
		this.shortPrimitiveArrayStats = shortArrayStats;
		return this;
	}

	public HeapStats setShortStringStats(ShortArrayStats shortStringStats) {
		this.shortStringStats = shortStringStats;
		return this;
	}

	public HeapStats setBoxedNumberStats(int nBoxedNumbers, long ovhdBoxedNumbers) {
		this.nBoxedNumbers = nBoxedNumbers;
		this.ovhdBoxedNumbers = ovhdBoxedNumbers;
		return this;
	}

	public HeapStats setWrappedCollectionStats(
		ObjectToIntMap.Entry<String>[] unmodifiableClasses, ObjectToIntMap.Entry<String>[] synchronizedClasses) {
		this.unmodifiableClasses = unmodifiableClasses;
		// Not used when printing reports in ReportFormatter. Can perhaps be removed entirely including method argument.
//		this.synchronizedClasses = synchronizedClasses;
		return this;
	}

	public HeapStats setDupStringStats(DupStringStats dupStringStats) {
		this.dupStringStats = dupStringStats;
		return this;
	}

	public HeapStats setCompressibleStringStats(CompressibleStringStats compressibleStringStats) {
		this.compressibleStringStats = compressibleStringStats;
		return this;
	}

	public HeapStats setNumberEncodingStringStats(NumberEncodingStringStats nesStats) {
		this.numberEncodingStringStats = nesStats;
		return this;
	}

	public HeapStats setStringLengthHistogram(LengthHistogram lenHisto) {
		this.stringLengthHistogram = lenHisto;
		return this;
	}

	public HeapStats setDupArrayStats(DupArrayStats dupArrayStats) {
		this.dupArrayStats = dupArrayStats;
		return this;
	}

	// --------------------------- Setting detailed stats -----------------------

	public HeapStats setObjectHistogram(ObjectHistogram objHisto) {
		this.objHisto = objHisto;
		return this;
	}

	public HeapStats setCollectionNumberStats(
		int numCols, int numEmptyUnusedCols, int numEmptyUsedCols, int numEmptyCols, int numSmallCols,
		int numSparseSmallCols, int numSparseLargeCols, int numBoxedNumberCols, int numBarCols) {
		this.numCols = numCols;
		this.numEmptyUnusedCols = numEmptyUnusedCols;
		this.numEmptyUsedCols = numEmptyUsedCols;
		this.numEmptyCols = numEmptyCols;
		this.numSmallCols = numSmallCols;
		this.numSparseSmallCols = numSparseSmallCols;
		this.numSparseLargeCols = numSparseLargeCols;
		this.numBoxedNumberCols = numBoxedNumberCols;
		this.numBarCols = numBarCols;
		return this;
	}

	public HeapStats setCollectionOverhead(
		long emptyUnusedColsOvhd, long emptyUsedColsOvhd, long emptyColsOvhd, long smallColsOvhd,
		long sparseSmallColsOvhd, long sparseLargeColsOvhd, long boxedNumberColsOvhd, long barColsOvhd) {
		this.emptyUnusedColsOverhead = emptyUnusedColsOvhd;
		this.emptyUsedColsOverhead = emptyUsedColsOvhd;
		this.emptyColsOverhead = emptyColsOvhd;
		this.smallColsOverhead = smallColsOvhd;
		this.sparseSmallColsOverhead = sparseSmallColsOvhd;
		this.sparseLargeColsOverhead = sparseLargeColsOvhd;
		this.boxedNumberColsOverhead = boxedNumberColsOvhd;
		this.barColsOverhead = barColsOvhd;
		return this;
	}

	public HeapStats setObjArrayNumberStats(
		int numObjArrays, int numLengthZeroObjArrays, int numLengthOneObjArrays, int numEmptyObjArrays,
		int numSparseObjArrays, int numBoxedNumberArrays, int numBarObjArrays) {
		this.numObjArrays = numObjArrays;
		this.numLengthZeroObjArrays = numLengthZeroObjArrays;
		this.numLengthOneObjArrays = numLengthOneObjArrays;
		this.numEmptyObjArrays = numEmptyObjArrays;
		this.numSparseObjArrays = numSparseObjArrays;
		this.numBoxedNumberArrays = numBoxedNumberArrays;
		this.numBarObjArrays = numBarObjArrays;
		return this;
	}

	public HeapStats setObjArrayOverhead(
		long lengthZeroObjArraysOvhd, long lengthOneObjArraysOvhd, long emptyArraysOvhd, long sparseArraysOvhd,
		long boxedNumberArraysOvhd, long barArraysOvhd) {
		this.lengthZeroObjArraysOverhead = lengthZeroObjArraysOvhd;
		this.lengthOneObjArraysOverhead = lengthOneObjArraysOvhd;
		this.emptyObjArraysOverhead = emptyArraysOvhd;
		this.sparseObjArraysOverhead = sparseArraysOvhd;
		this.boxedNumberArraysOverhead = boxedNumberArraysOvhd;
		this.barObjArraysOverhead = barArraysOvhd;
		return this;
	}

	public HeapStats setCollectionOverheadByClass(ArrayList<CollectionClassDescriptor> overheadsByClass) {
		this.overheadsByClass = overheadsByClass;
		return this;
	}

	public HeapStats setValueArrayNumberStats(
		int numValueArrays, int numLengthZeroValueArrays, int numLengthOneValueArrays, int numEmptyValueArrays,
		int numLZTValueArrays, int numUnusedHiBytesValueArrays) {
		this.numValueArrays = numValueArrays;
		this.numLengthZeroValueArrays = numLengthZeroValueArrays;
		this.numLengthOneValueArrays = numLengthOneValueArrays;
		this.numEmptyValueArrays = numEmptyValueArrays;
		this.numLZTValueArrays = numLZTValueArrays;
		this.numUnusedHiBytesValueArrays = numUnusedHiBytesValueArrays;
		return this;
	}

	public HeapStats setValueArrayOverhead(
		long lengthZeroValueArraysOvhd, long lengthOneValueArraysOvhd, long emptyValueArraysOvhd,
		long lztValueArraysOvhd, long unusedHiBytesValueArraysOvhd) {
		this.lengthZeroValueArraysOverhead = lengthZeroValueArraysOvhd;
		this.lengthOneValueArraysOverhead = lengthOneValueArraysOvhd;
		this.emptyValueArraysOverhead = emptyValueArraysOvhd;
		this.lztValueArraysOverhead = lztValueArraysOvhd;
		this.unusedHiBytesValueArraysOverhead = unusedHiBytesValueArraysOvhd;
		return this;
	}

	public HeapStats setSystemProperties(HashMap<String, String> systemProps) {
		this.systemProperties = systemProps;
		return this;
	}
}
