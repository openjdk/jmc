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
package org.openjdk.jmc.joverflow.batch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.joverflow.descriptors.CollectionClassDescriptor;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.stats.ClassloaderStats;
import org.openjdk.jmc.joverflow.stats.LengthHistogram;
import org.openjdk.jmc.joverflow.stats.ObjectHistogram;
import org.openjdk.jmc.joverflow.support.CompressibleStringStats;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.support.DupArrayStats;
import org.openjdk.jmc.joverflow.support.DupStringStats;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.support.NumberEncodingStringStats;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.support.RefChainElementImpl;
import org.openjdk.jmc.joverflow.support.ReferenceChain;
import org.openjdk.jmc.joverflow.support.ShortArrayStats;
import org.openjdk.jmc.joverflow.util.MiscUtils;
import org.openjdk.jmc.joverflow.util.ObjectToIntMap;

/**
 * Functionality for generating text report from results of batch analysis (HeapStats and
 * DetailedStats objects). This is currently used by command-line JOverflow tool.
 */
public class ReportFormatter {

	private static final String DASH_SEPARATOR = "\n---------------------------------------------------";

	private final HeapStats hs;
	private final DetailedStats ds;
	private final FormattedOutputBuffer b;

	public ReportFormatter(HeapStats hs, DetailedStats ds) {
		this.hs = hs;
		this.ds = ds;
		b = new FormattedOutputBuffer(hs.totalObjSize);
	}

	public HeapStats getHeapStats() {
		return hs;
	}

	public DetailedStats getDetailedStats() {
		return ds;
	}

	public String getReport(
		boolean printFullObjectHistogram, int printedRefChainDepth, String[] refChainStopperClassPrefixes) {
		formatOverallStats();
		formatDetailedStats(printFullObjectHistogram, printedRefChainDepth, refChainStopperClassPrefixes);
		return b.getOutput();
	}

	private void formatOverallStats() {
		b.println(DASH_SEPARATOR);
		b.println("1. OVERALL STATS:\n");

		b.println("1.0 Fundamentals\n");

		b.format("Pointer size: %d bytes %s\n", hs.ptrSize, hs.usingNarrowPointers ? "(narrow in 64-bit mode)" : "");
		b.format("Object header size: %d bytes\n", hs.objHeaderSize);
		b.format("Object alignment: %d bytes\n", hs.objAlignment);

		b.format("\nTotal num of objects: %,d\n", hs.nObjects);
		b.format("Instances: %,d, object arrays: %,d, primitive arrays: %,d\n", hs.nInstances, hs.nObjectArrays,
				hs.nValueArrays);
		b.format("Total size of all objects: %s\n", b.k(hs.totalObjSize));
		b.format("Instances: %s, object arrays: %s, primitive arrays: %s\n", b.k(hs.totalInstSize),
				b.k(hs.totalObjArraySize), b.k(hs.totalValueArraySize));

		b.print("\nMinimum reported overhead: ");
		if (ds.minOvhdToReport < 1024) {
			b.println(ds.minOvhdToReport + " bytes");
		} else {
			b.println((ds.minOvhdToReport / 1024) + "K");
		}

		b.println("\n1.1 Assorted raw stats; no distinction between collections and standalone arrays\n");

		b.format("Total size of object headers: %s\n", b.k(hs.ovhdObjHeaders));
		b.format("Num and size of all *$Entry instances: %,d, %s\n", hs.nEntryInstances, b.k(hs.entryClassSize));

		ShortArrayStats soa = hs.shortObjArrayStats;
		b.format("\nNum and overhead of length 0 obj arrays: %,d  %s\n", soa.n0LenObjs, b.k(soa.ovhd0LenObjs));
		b.format("Num and overhead of length 1 obj arrays: %,d  %s\n", soa.n1LenObjs, b.k(soa.ovhd1LenObjs));
		b.format("Num and overhead of length 2..4 obj arrays: %,d  %s\n", soa.n4LenObjs, b.k(soa.ovhd4LenObjs));
		b.format("Num and overhead of length 5..8 obj arrays: %,d  %s\n", soa.n8LenObjs, b.k(soa.ovhd8LenObjs));

		ShortArrayStats spa = hs.shortPrimitiveArrayStats;
		b.format("\nNum and overhead of length 0 primitive arrays: %,d  %s\n", spa.n0LenObjs, b.k(spa.ovhd0LenObjs));
		b.format("Num and overhead of length 1 primitive arrays: %,d  %s\n", spa.n1LenObjs, b.k(spa.ovhd1LenObjs));
		b.format("Num and overhead of length 2..4 primitive arrays: %,d  %s\n", spa.n4LenObjs, b.k(spa.ovhd4LenObjs));
		b.format("Num and overhead of length 5..8 primitive arrays: %,d  %s\n", spa.n8LenObjs, b.k(spa.ovhd8LenObjs));

		ShortArrayStats sstrs = hs.shortStringStats;
		b.format("\nNum and overhead of length 0 Strings: %,d  %s\n", sstrs.n0LenObjs, b.k(sstrs.ovhd0LenObjs));
		b.format("Num and overhead of length 1 Strings: %,d  %s\n", sstrs.n1LenObjs, b.k(sstrs.ovhd1LenObjs));
		b.format("Num and overhead of length 2..4 Strings: %,d  %s\n", sstrs.n4LenObjs, b.k(sstrs.ovhd4LenObjs));
		b.format("Num and overhead of length 5..8 Strings: %,d  %s\n", sstrs.n8LenObjs, b.k(sstrs.ovhd8LenObjs));

		NumberEncodingStringStats nesStats = hs.numberEncodingStringStats;
		b.format("\nNum and overhead of Strings that encode int numbers: %,d  %s\n", nesStats.nStringsEncodingInts,
				b.k(nesStats.stringsEncodingIntsOvhd));

		b.format("\nNum of boxed Numbers: %,d\n", hs.nBoxedNumbers);
		b.format("Overhead of boxed Numbers: %s\n", b.k(hs.ovhdBoxedNumbers));

		b.format("\nNum of wrapped Unmodifiable* collection classes:\n");
		for (ObjectToIntMap.Entry<String> entry : hs.unmodifiableClasses) {
			b.format("%20s : %,d\n", entry.key, entry.value);
		}
		// FIXME: We probably can't get anything useful out of these collections. Remove calculation of this?
//		b.format("\nNum of wrapped Synchronized* collection classes:\n");
//		for (ObjectToIntMap.Entry<String> entry : hs.synchronizedClasses) {
//			b.format("%20s : %4d\n", entry.key, entry.value);
//		}

		b.println("\n1.2 Stats on the JVM that produced the dump (from System.getProperties()):\n");

		HashMap<String, String> systemProps = hs.systemProperties;
		if (systemProps != null) {
			for (Map.Entry<String, String> entry : systemProps.entrySet()) {
				String key = entry.getKey();
				if (key.startsWith("\"java.runtime.") || key.startsWith("\"java.vm.")
						|| key.startsWith("\"java.specification.") || key.startsWith("\"os.")) {
					b.println(key + " = " + entry.getValue());
				}
			}
		} else {
			b.println("  *** Could not be found ***");
		}

		b.println("\n1.3 Stats on classloaders\n");

		ClassloaderStats clStats = hs.classloaderStats;
		ObjectToIntMap<JavaObject> clInstToNumLoadedClasses = clStats.getCLInstToNumLoadedClasses();
		ObjectToIntMap<JavaClass> clClazzToNumLoadedClasses = clStats.getClClazzToNumLoadedClasses();
		int numClTypesWithLoadedClasses = 0;
		for (ObjectToIntMap.Entry<JavaClass> entry : clClazzToNumLoadedClasses.getEntries()) {
			if (entry.value > 0) {
				numClTypesWithLoadedClasses++;
			}
		}
		int numClInstancesWithOneLoaded = 0;
		HashSet<JavaClass> clClassesWithOneLoaded = new HashSet<>();
		for (ObjectToIntMap.Entry<JavaObject> entry : clInstToNumLoadedClasses.getEntries()) {
			if (entry.value == 1) {
				numClInstancesWithOneLoaded++;
				clClassesWithOneLoaded.add(entry.key.getClazz());
			}
		}

		b.format("Num of classes extending java.lang.ClassLoader: %,d\n", clClazzToNumLoadedClasses.size());
		b.format("Classloader instances with loaded classes: num: %,d types: %,d\n", clInstToNumLoadedClasses.size(),
				numClTypesWithLoadedClasses);
		b.format("Classloader instances with only one loaded class: num: %,d types: %,d\n", numClInstancesWithOneLoaded,
				clClassesWithOneLoaded.size());

		b.println("\n1.4 Stats on compressible strings\n");

		CompressibleStringStats cs = hs.compressibleStringStats;
		b.format("Total num of String objects: %,d\n", cs.nTotalStrings);
		b.format("Total used bytes in backing arrays: %s\n", b.k(cs.totalUsedBackingArrayBytes));
		int percent = (int) (((double) cs.nCompressedStrings) * 100 / cs.nTotalStrings);
		b.format("Num of Strings with backing byte[] arrays: %,d (%d%% of all Strings)\n", cs.nCompressedStrings,
				percent);
		b.format("Total used bytes in backing byte[] arrays: %s\n", b.k(cs.compressedBackingArrayBytes));
		percent = (int) (((double) cs.nAsciiCharBackedStrings) * 100 / cs.nTotalStrings);
		b.format("Num of Strings backed by ASCII char[] arrays: %,d (%d%% of all Strings)\n",
				cs.nAsciiCharBackedStrings, percent);
		b.format("Total used bytes in backing ASCII char[] arrays: %s\n", b.k(cs.asciiCharBackingArrayBytes));
	}

	private void formatDetailedStats(
		boolean printFullObjectHistogram, int printedRefChainDepth, String[] refChainStopperClassPrefixes) {
		b.println(DASH_SEPARATOR);
		b.startSection("2. CLASS AND OBJECT INFORMATION:", "High memory consumption by instances of", 10.0);

		List<ObjectHistogram.Entry> objHistogram = hs.objHisto
				.getListSortedByInclusiveSize(printFullObjectHistogram ? 0 : ds.minOvhdToReport);

		b.format("\nTotal classes: %,d  Total objects: %,d\n", hs.nClasses, hs.nObjects);
		int[] smallInstClasses = hs.objHisto.calculateNumSmallInstClasses();
		b.format("Classes with no instances: %,d  Classes with 1 instance: %,d\n", smallInstClasses[0],
				smallInstClasses[1]);
		b.println("\nObject histogram for top memory consumers");
		b.println(" #instances    Shallow size   Impl-inclusive size   Class name");
		b.println("---------------------------------------------------------------");
		for (ObjectHistogram.Entry entry : objHistogram) {
			b.format("%,10d  %16s   %16s    %s\n", entry.getNumInstances(), b.k(entry.getTotalShallowSize()),
					b.k(entry.getTotalInclusiveSize()), entry.getClazz().getHumanFriendlyNameWithLoaderIfNeeded());
			b.criticalCheck(entry.getTotalInclusiveSize(), entry.getClazz().getHumanFriendlyNameWithLoaderIfNeeded());
		}

		b.println(DASH_SEPARATOR);
		b.startSection("3. NUMBER, SIZE AND NEAREST FIELDS FOR HIGH MEMORY CONSUMERS:");

		b.println();
		List<ReferencedObjCluster.HighSizeObjects> hsFields = ds.highSizeObjClusters.get(1);
		for (ReferencedObjCluster.HighSizeObjects c : hsFields) {
			RefChainElement classAndField = c.getReferer();
			String classAndFieldStr = ReferenceChain.toStringInStraightOrder(classAndField);

			String fieldDefiningClass = getFieldDefiningClassFromFieldRefChain(
					ReferenceChain.getRootElement(classAndField));
			if (fieldDefiningClass != null) {
				classAndFieldStr += " (defined in " + fieldDefiningClass + ")";
			}

			b.print("  ");
			b.print(classAndFieldStr);
			b.println(" -->");
			b.println(c.clusterAsString(b.getMemNumFormatter()));
		}

		b.println(DASH_SEPARATOR);
		b.startSection("4. NUMBER, SIZE AND REF CHAINS FOR TOP MEMORY CONSUMERS:");

		b.println();
		List<ReferencedObjCluster.HighSizeObjects> hsReverseChains = ds.highSizeObjClusters.get(0);
		for (ReferencedObjCluster c : hsReverseChains) {
			b.println(c.clusterAsString(b.getMemNumFormatter()));
			b.print("    ");
			b.println(ReferenceChain.toStringInReverseOrder(c.getReferer(), printedRefChainDepth,
					refChainStopperClassPrefixes));
		}

		b.println(DASH_SEPARATOR);
		b.startSection("5. PROBLEMATIC COLLECTIONS:", "High overhead due to problematic collections of kind", 2.0);
		b.format("\nTotal collections: %,d\n", hs.numCols);

		b.format("\nEmpty unused collections total number:  %,10d, ovhd: %14s\n", hs.numEmptyUnusedCols,
				b.k(hs.emptyUnusedColsOverhead));
		b.criticalCheck(hs.emptyUnusedColsOverhead, "empty unused");
		printClassesWithProblemKind(Constants.ProblemKind.EMPTY_UNUSED, false, false);
		b.format("\nEmpty used collections total number:    %,10d, ovhd: %14s\n", hs.numEmptyUsedCols,
				b.k(hs.emptyUsedColsOverhead));
		b.criticalCheck(hs.emptyUsedColsOverhead, "empty used");
		printClassesWithProblemKind(Constants.ProblemKind.EMPTY_USED, false, false);
		b.format("\nEmpty collections total number:         %,10d, ovhd: %14s\n", hs.numEmptyCols,
				b.k(hs.emptyColsOverhead));
		b.criticalCheck(hs.emptyColsOverhead, "empty");
		printClassesWithProblemKind(Constants.ProblemKind.EMPTY, false, false);
		b.format("\nSmall sparse collections total number:  %,10d, ovhd: %14s\n", hs.numSparseSmallCols,
				b.k(hs.sparseSmallColsOverhead));
		b.criticalCheck(hs.sparseSmallColsOverhead, "small sparse");
		printClassesWithProblemKind(Constants.ProblemKind.SPARSE_SMALL, false, false);
		b.format("\nLarge sparse collections total number:  %,10d, ovhd: %14s\n", hs.numSparseLargeCols,
				b.k(hs.sparseLargeColsOverhead));
		b.criticalCheck(hs.sparseLargeColsOverhead, "large sparse");
		printClassesWithProblemKind(Constants.ProblemKind.SPARSE_LARGE, false, false);
		b.format("\nBoxed Number collections total number:  %,10d, ovhd: %14s\n", hs.numBoxedNumberCols,
				b.k(hs.boxedNumberColsOverhead));
		b.criticalCheck(hs.boxedNumberColsOverhead, "boxed number");
		printClassesWithProblemKind(Constants.ProblemKind.BOXED, false, false);
		b.format("\nVertical bar collections total number:  %,10d, ovhd: %14s\n", hs.numBarCols,
				b.k(hs.barColsOverhead));
		b.criticalCheck(hs.barColsOverhead, "vertical bar");
		printClassesWithProblemKind(Constants.ProblemKind.BAR, false, false);
		b.format("\nSmall collections total number:         %,10d, ovhd: %14s\n", hs.numSmallCols,
				b.k(hs.smallColsOverhead));
		b.criticalCheck(hs.smallColsOverhead, "small");
		printClassesWithProblemKind(Constants.ProblemKind.SMALL, false, false);

		b.println(DASH_SEPARATOR);
		b.startSection("6. PROBLEMATIC STANDALONE OBJECT ARRAYS:",
				"High overhead due to problematic object arrays of kind", 2.0);
		b.format("\nTotal standalone obj arrays: %,d\n", hs.numObjArrays);

		b.format("\nLength 0 object arrays number:          %,10d, ovhd: %14s\n", hs.numLengthZeroObjArrays,
				b.k(hs.lengthZeroObjArraysOverhead));
		b.criticalCheck(hs.lengthZeroObjArraysOverhead, "length 0");
		printClassesWithProblemKind(Constants.ProblemKind.LENGTH_ZERO, true, false);
		b.format("\nLength 1 object arrays number:          %,10d, ovhd: %14s\n", hs.numLengthOneObjArrays,
				b.k(hs.lengthOneObjArraysOverhead));
		b.criticalCheck(hs.lengthOneObjArraysOverhead, "length 1");
		printClassesWithProblemKind(Constants.ProblemKind.LENGTH_ONE, true, false);
		b.format("\nEmpty object arrays number:             %,10d, ovhd: %14s\n", hs.numEmptyObjArrays,
				b.k(hs.emptyObjArraysOverhead));
		b.criticalCheck(hs.emptyObjArraysOverhead, "empty");
		printClassesWithProblemKind(Constants.ProblemKind.EMPTY, true, false);
		b.format("\nSparse object arrays number:            %,10d, ovhd: %14s\n", hs.numSparseObjArrays,
				b.k(hs.sparseObjArraysOverhead));
		b.criticalCheck(hs.sparseObjArraysOverhead, "sparse");
		printClassesWithProblemKind(Constants.ProblemKind.SPARSE_ARRAY, true, false);
		b.format("\nBoxed Number object arrays number:      %,10d, ovhd: %14s\n", hs.numBoxedNumberArrays,
				b.k(hs.boxedNumberArraysOverhead));
		b.criticalCheck(hs.boxedNumberArraysOverhead, "boxed");
		printClassesWithProblemKind(Constants.ProblemKind.BOXED, true, false);
		b.format("\nVertical bar object arrays number:      %,10d, ovhd: %14s\n", hs.numBarObjArrays,
				b.k(hs.barObjArraysOverhead));
		b.criticalCheck(hs.barObjArraysOverhead, "vertical bar");
		printClassesWithProblemKind(Constants.ProblemKind.BAR, true, false);

		b.println(DASH_SEPARATOR);
		b.startSection("7. PROBLEMATIC STANDALONE PRIMITIVE ARRAYS:",
				"High overhead due to problematic primitive arrays of kind", 2.0);
		b.format("\nTotal standalone primitive arrays: %,d\n", hs.numValueArrays);

		b.format("\nLength 0 primitive arrays number:         %,8d, ovhd: %14s\n", hs.numLengthZeroValueArrays,
				b.k(hs.lengthZeroValueArraysOverhead));
		b.criticalCheck(hs.lengthZeroValueArraysOverhead, "length 0");
		printClassesWithProblemKind(Constants.ProblemKind.LENGTH_ZERO, true, true);
		b.format("\nLength 1 primitive arrays number:         %,8d, ovhd: %14s\n", hs.numLengthOneValueArrays,
				b.k(hs.lengthOneValueArraysOverhead));
		b.criticalCheck(hs.lengthOneValueArraysOverhead, "length 1");
		printClassesWithProblemKind(Constants.ProblemKind.LENGTH_ONE, true, true);
		b.format("\nEmpty primitive arrays number:            %,8d, ovhd: %14s\n", hs.numEmptyValueArrays,
				b.k(hs.emptyValueArraysOverhead));
		b.criticalCheck(hs.emptyValueArraysOverhead, "empty");
		printClassesWithProblemKind(Constants.ProblemKind.EMPTY, true, true);
		b.format("\nLong zero-tail primitive arrays number:   %,8d, ovhd: %14s\n", hs.numLZTValueArrays,
				b.k(hs.lztValueArraysOverhead));
		b.criticalCheck(hs.lztValueArraysOverhead, "long zero-tail (LZT)");
		printClassesWithProblemKind(Constants.ProblemKind.LZT, true, true);
		b.format("\nUnused high bytes primitive arrays number:%,8d, ovhd: %14s\n", hs.numUnusedHiBytesValueArrays,
				b.k(hs.unusedHiBytesValueArraysOverhead));
		b.criticalCheck(hs.unusedHiBytesValueArraysOverhead, "unused high bytes");
		printClassesWithProblemKind(Constants.ProblemKind.UNUSED_HI_BYTES, true, true);

		b.println(DASH_SEPARATOR);
		b.startSection("8. NUMBER, OVERHEAD AND NEAREST FIELDS FOR PROBLEMATIC COLLECTIONS AND ARRAYS:");

		b.println();
		List<ReferencedObjCluster.Collections> colFields = ds.collectionClusters.get(1);
		for (ReferencedObjCluster.Collections c : colFields) {
			if (c.getTotalOverhead() < ds.minOvhdToReport / 4) {
				break;
			}
			RefChainElement classAndField = c.getReferer();
			String classAndFieldStr = ReferenceChain.toStringInStraightOrder(classAndField);

			String fieldDefiningClass = getFieldDefiningClassFromFieldRefChain(
					ReferenceChain.getRootElement(classAndField));
			if (fieldDefiningClass != null) {
				classAndFieldStr += " (defined in " + fieldDefiningClass + ")";
			}

			b.print("  ");
			b.print(classAndFieldStr);
			b.println(" -->");
			b.println(c.clusterAsString(b.getMemNumFormatter()));
		}

		b.println(DASH_SEPARATOR);
		b.startSection("9. NUMBER, OVERHEAD AND REF CHAINS FOR PROBLEMATIC COLLECTIONS AND ARRAYS:");

		b.println();
		List<ReferencedObjCluster.Collections> colReverseChains = ds.collectionClusters.get(0);
		for (ReferencedObjCluster c : colReverseChains) {
			// For individual clusters, we set a smaller overhead threshold
			if (c.getTotalOverhead() < ds.minOvhdToReport / 4) {
				break;
			}
			b.println(c.clusterAsString(b.getMemNumFormatter()));
			b.print("    ");
			b.println(ReferenceChain.toStringInReverseOrder(c.getReferer(), printedRefChainDepth,
					refChainStopperClassPrefixes));
		}

		b.println(DASH_SEPARATOR);
		b.startSection("10. DUPLICATE STRING STATS:", "High overhead due to duplicate strings", 5.0);

		DupStringStats dss = hs.dupStringStats;
		b.format("\nTotal strings: %,d Unique strings: %,d Duplicate values: %,d Overhead: %s\n", dss.nStrings,
				dss.nUniqueStringValues, dss.nUniqueDupStringValues, b.k(dss.dupStringsOverhead));
		b.criticalCheck(dss.dupStringsOverhead, "");

		b.format("Top duplicate Strings:\n");
		b.format("    Ovhd        Num char[]s  Num objs  Max arr len  Value\n");
		for (DupStringStats.Entry entry : dss.dupStrings) {
			if (entry.overhead < ds.minOvhdToReport) {
				break;
			}

			b.format("%14s   %6d     %6d     %6d       ", b.k(entry.overhead), entry.nBackingArrays,
					entry.nStringInstances, entry.maxArrayLen);

			String string = MiscUtils.removeEndLinesAndAddQuotes(entry.string, 100);
			b.println(string);
		}

		b.println(DASH_SEPARATOR);
		b.startSection("11. NUMBER, OVERHEAD AND NEAREST FIELDS FOR DUPLICATE STRINGS:");

		b.println();
		List<ReferencedObjCluster.DupStrings> rsFields = ds.dupStringClusters.get(1);
		for (ReferencedObjCluster c : rsFields) {
			if (c.getTotalOverhead() < ds.minOvhdToReport / 4) {
				break;
			}
			RefChainElement classAndField = c.getReferer();
			String classAndFieldStr = ReferenceChain.toStringInStraightOrder(classAndField);

			String fieldDefiningClass = getFieldDefiningClassFromFieldRefChain(
					ReferenceChain.getRootElement(classAndField));
			if (fieldDefiningClass != null) {
				classAndFieldStr += " (defined in " + fieldDefiningClass + ")";
			}

			b.print("  ");
			b.print(classAndFieldStr);
			b.println(" -->");
			b.println(c.clusterAsString(b.getMemNumFormatter()));
		}

		b.println(DASH_SEPARATOR);
		b.startSection("12. NUMBER, OVERHEAD AND REF CHAINS FOR DUPLICATE STRINGS:");

		b.println();
		List<ReferencedObjCluster.DupStrings> rsReverseChains = ds.dupStringClusters.get(0);
		for (ReferencedObjCluster c : rsReverseChains) {
			// For individual clusters, we set a smaller overhead threshold
			if (c.getTotalOverhead() < ds.minOvhdToReport / 4) {
				break;
			}
			b.println(c.clusterAsString(b.getMemNumFormatter()));
			b.print("    ");
			b.println(ReferenceChain.toStringInReverseOrder(c.getReferer(), printedRefChainDepth,
					refChainStopperClassPrefixes));
		}

		b.println(DASH_SEPARATOR);
		b.startSection("13. DUPLICATE PRIMITIVE ARRAY STATS:", "High overhead due to duplicate primitive arrays", 5.0);

		DupArrayStats das = hs.dupArrayStats;
		b.format("\nTotal primitive arrays: %,d Unique arrays: %,d Duplicate values: %,d Overhead: %s\n", das.nArrays,
				das.nUniqueArrays, das.nDifferentDupArrayValues, b.k(das.dupArraysOverhead));
		b.criticalCheck(das.dupArraysOverhead, "");

		b.format("Top duplicate primitive arrays:\n");
		b.format("    Ovhd         Num objs  Array len    Value\n");
		for (DupArrayStats.Entry entry : das.dupArrays) {
			if (entry.overhead < ds.minOvhdToReport) {
				break;
			}

			b.format("%14s   %6d     %6d     ", b.k(entry.overhead), entry.nArrayInstances,
					entry.firstArray.getLength());

			String arrayAsString = entry.firstArray.valueAsString();
			b.println(arrayAsString);
		}

		b.println(DASH_SEPARATOR);
		b.startSection("14. NUMBER, OVERHEAD AND NEAREST FIELDS FOR DUPLICATE PRIMITIVE ARRAYS:");

		b.println();
		List<ReferencedObjCluster.DupArrays> daFields = ds.dupArrayClusters.get(1);
		for (ReferencedObjCluster c : daFields) {
			if (c.getTotalOverhead() < ds.minOvhdToReport / 4) {
				break;
			}
			RefChainElement classAndField = c.getReferer();
			String classAndFieldStr = ReferenceChain.toStringInStraightOrder(classAndField);

			String fieldDefiningClass = getFieldDefiningClassFromFieldRefChain(
					ReferenceChain.getRootElement(classAndField));
			if (fieldDefiningClass != null) {
				classAndFieldStr += " (defined in " + fieldDefiningClass + ")";
			}

			b.print("  ");
			b.print(classAndFieldStr);
			b.println(" -->");
			b.println(c.clusterAsString(b.getMemNumFormatter()));
		}

		b.println(DASH_SEPARATOR);
		b.startSection("15. NUMBER, OVERHEAD AND REF CHAINS FOR DUPLICATE PRIMITIVE ARRAYS:");

		b.println();
		List<ReferencedObjCluster.DupArrays> daReverseChains = ds.dupArrayClusters.get(0);
		for (ReferencedObjCluster c : daReverseChains) {
			// For individual clusters, we set a smaller overhead threshold
			if (c.getTotalOverhead() < ds.minOvhdToReport / 4) {
				break;
			}
			b.println(c.clusterAsString(b.getMemNumFormatter()));
			b.print("    ");
			b.println(ReferenceChain.toStringInReverseOrder(c.getReferer(), printedRefChainDepth,
					refChainStopperClassPrefixes));
		}

		b.println(DASH_SEPARATOR);
		b.startSection(
				"16. WEAK HASHMAPS WITH REFS FROM VALUES TO KEYS\n"
						+ "    (conservative estimate; deep object size not calculated):",
				"Found WeakHashMaps with references from values to keys, minimum overhead", 0.01);

		b.format("\n      Ovhd       Num collections   Type\n");
		Constants.ProblemKind weakKind = Constants.ProblemKind.WEAK_MAP_WITH_BACK_REFS;
		for (CollectionClassDescriptor cd : hs.overheadsByClass) {
			int numProblematicCollections = cd.getNumProblematicCollections(weakKind);
			if (numProblematicCollections == 0) {
				continue;
			}

			String className = cd.getClazz().getHumanFriendlyName();
			int ovhd = cd.getProblematicCollectionsOverhead(weakKind);
			b.criticalCheck(ovhd, "");
			b.format("%14s   %,8d        %s\n", b.k(ovhd), numProblematicCollections, className);
		}

		b.println();
		List<ReferencedObjCluster.WeakHashMaps> wmReverseChains = ds.weakHashMapClusters.get(0);
		for (ReferencedObjCluster c : wmReverseChains) {
			// Here we print data for any overhead, just in case
			b.println(c.clusterAsString(b.getMemNumFormatter()));
			b.print("    ");
			b.println(ReferenceChain.toStringInReverseOrder(c.getReferer(), printedRefChainDepth,
					refChainStopperClassPrefixes));
		}

		b.println();
		List<ReferencedObjCluster.WeakHashMaps> wmFields = ds.weakHashMapClusters.get(1);
		for (ReferencedObjCluster c : wmFields) {
			// Here we print data for any overhead, just in case
			b.print("  ");
			b.print(ReferenceChain.toStringInStraightOrder(c.getReferer()));
			b.println(" -->");
			b.println(c.clusterAsString(b.getMemNumFormatter()));
		}
		b.println();

		b.println(DASH_SEPARATOR);
		b.startSection("17. DATA FIELDS ALWAYS OR ALMOST ALWAYS NULL/ZERO, OR NO FIELDS:",
				"High overhead due to fields that are null/zero/non-existent in", 1.0);

		printProblemFieldsHistogram(hs.objHisto, true, 1.0f, ds.minOvhdToReport);
		printProblemFieldsHistogram(hs.objHisto, true, 0.9f, ds.minOvhdToReport);

		b.println(DASH_SEPARATOR);
		b.startSection("18. PRIMITIVE DATA FIELDS WITH UNUSED HIGH BYTES:",
				"High overhead due to primitive fields with unused high bytes", 1.0);

		printProblemFieldsHistogram(hs.objHisto, false, 1.0f, ds.minOvhdToReport);
		printProblemFieldsHistogram(hs.objHisto, false, 0.9f, ds.minOvhdToReport);

		b.println(DASH_SEPARATOR);
		b.startSection("19. STRING LENGTH STATISTICS:", "High memory consumption by Strings of length", 10.0);

		List<LengthHistogram.Entry> strLenHisto = hs.stringLengthHistogram
				.getPrunedAndSortedEntries(ds.minOvhdToReport);

		b.println("\n    Length         Count              Size");
		b.println("----------------------------------------------");
		for (LengthHistogram.Entry entry : strLenHisto) {
			int strLen = entry.getLength();
			String formattedLen = strLen >= 0 ? String.format("%,d", strLen) : "other";
			b.format(" %9s    %,10d     %16s\n", formattedLen, entry.getCount(), b.k(entry.getSize()));
			b.criticalCheck(entry.getSize(), formattedLen);
		}
	}

	private void printClassesWithProblemKind(
		Constants.ProblemKind problemKind, boolean selectArrays, boolean selectPrimitiveArrays) {
		for (CollectionClassDescriptor cd : hs.overheadsByClass) {
			JavaClass clazz = cd.getClazz();
			if (clazz.isArray()) {
				if (!selectArrays) {
					continue;
				}
				if (clazz.isAnyDimPrimitiveArray()) {
					if (!selectPrimitiveArrays) {
						continue;
					}
				} else { // An object array
					if (selectPrimitiveArrays) {
						continue;
					}
				}
			} else { // Not an array
				if (selectArrays) {
					continue;
				}
			}

			int ovhd = cd.getProblematicCollectionsOverhead(problemKind);
			if (ovhd < ds.minOvhdToReport) {
				continue;
			}
			b.format("%39s   %,8d        %14s\n", clazz.getHumanFriendlyName(),
					cd.getNumProblematicCollections(problemKind), b.k(ovhd));
		}
	}

	/**
	 * If nullFields parameter is true, prints the info on null/zero fields. Otherwise, prints the
	 * info on primitive fields underutilizing high bytes.
	 */
	private void printProblemFieldsHistogram(
		ObjectHistogram objHisto, boolean nullFields, float percentile, int minOverheadToReport) {
		List<ObjectHistogram.ProblemFieldsEntry> problemClasses = nullFields
				? objHisto.getListSortedByNullFieldsOvhd(percentile)
				: objHisto.getListSortedByUnusedHiByteFieldsOvhd(percentile);
		int nClasses = problemClasses.size();
		int nObjects = 0;
		long totalOverhead = 0;

		for (ObjectHistogram.ProblemFieldsEntry probClazz : problemClasses) {
			nObjects += probClazz.getNumInstances();
			totalOverhead += probClazz.getAllProblemFieldsOvhd();
		}
		int percentileAsInt = (int) (percentile * 100);

		String subIssue = String.format("%d%% of instances", percentileAsInt);
		if (nullFields) {
			b.format("\nClasses with some fields null/zero in %s, or no fields: %,d\n", subIssue, nClasses);
			b.format("Objects: %,d  Overhead: %s\n", nObjects, b.k(totalOverhead));
			b.criticalCheck(totalOverhead, subIssue);
			b.println("Null fields ovhd  #instances        Class name   /   Null fields");
		} else {
			b.format("\nClasses with primitive fields that don't use high byte(s) in %s: %,d\n", subIssue, nClasses);
			b.format("Objects: %,d  Overhead: %s\n", nObjects, b.k(totalOverhead));
			b.criticalCheck(totalOverhead, subIssue);
			b.println("Bad fields ovhd   #instances        Class name   /   Null fields");
		}

		b.println("----------------------------------------------------------------");
		for (ObjectHistogram.ProblemFieldsEntry nfClass : problemClasses) {
			if (nfClass.getAllProblemFieldsOvhd() < minOverheadToReport) {
				break;
			}
			b.format("%15s %,10d   %38s\n", b.k(nfClass.getAllProblemFieldsOvhd()), nfClass.getNumInstances(),
					nfClass.getClazz().getHumanFriendlyNameWithLoaderIfNeeded());
			b.print("       ");
			b.println(nfClass.getFieldsAsString());
		}
	}

	private static String getFieldDefiningClassFromFieldRefChain(RefChainElement desc) {
		if (!(desc instanceof RefChainElementImpl.AbstractField)) {
			return null;
		}

		RefChainElementImpl.AbstractField fieldDesc = (RefChainElementImpl.AbstractField) desc;
		JavaClass clazz = fieldDesc.getJavaClass();
		int fieldIdx = fieldDesc.getFieldIdx();

		JavaClass defClazz = clazz.getDeclaringClassForField(fieldIdx);
		if (defClazz == clazz || defClazz == null) {
			return null;
		} else {
			return defClazz.getName();
		}
	}

}
