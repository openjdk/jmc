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

import java.lang.reflect.Field;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jmc.joverflow.batch.BatchProblemRecorder;
import org.openjdk.jmc.joverflow.batch.DetailedStats;
import org.openjdk.jmc.joverflow.batch.ReferencedObjCluster;
import org.openjdk.jmc.joverflow.descriptors.CollectionClassDescriptor;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.support.ClassAndOvhdCombo;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.support.ReferenceChain;
import org.openjdk.jmc.joverflow.util.MiscUtils;

import examples.VariousIssues;

/**
 * Testing our general problematic data structure calculation functionality. Uses a heap dump called
 * various-issues.hprof, that is generated from examples.VariousIssues running on the HotSpot JVM in
 * 32-bit mode.
 * <p>
 * IMPORTANT: you may need to re-generate the heap dump on the same JVM that you are going to run
 * this test on. Currently the test re-create an instance of VariousIssues class in memory to
 * compare results from the heap dump with those obtained from the live instance. On different JVMs,
 * with different implementations of HashMap etc., this data may not be the same. The code also
 * makes assumptions about the internal structure of ConcurrentHashMap etc, that are correct for JDK
 * 6, but may not be true for JDK 7 and 8.
 */
// TODO: this is work in progress, many more checks can and should be added.
@Ignore("Requires a generated hprof file. This test can be reenabled if the file is available.")
public class VariousIssueTest {

	private static final int OBJ_HDR_SIZE = Constants.STANDARD_32BIT_OBJ_HEADER_SIZE;
	private static final int PTR_SIZE = 4; // In 32-bit mode
	private static final int STRING_OBJ_SIZE = 24; // In 32-bit mode

	// Generate the same data as in the heap dump, to make some calculations easier.
	private VariousIssues variousIssues = new VariousIssues();

	private Snapshot snapshot;

	private int ptrSize;

	@Test
	public void testBadDataStats() throws Exception {
		runTest(false);
		runTest(true);
	}

	private void runTest(boolean useBreadthFirst) throws Exception {
		snapshot = SnapshotReader.readAndResolveHeapDump("various-issues.hprof");
		BatchProblemRecorder recorder = new BatchProblemRecorder();
		StandardStatsCalculator ssc = new StandardStatsCalculator(snapshot, recorder, useBreadthFirst);
		HeapStats hs = ssc.calculate();
		DetailedStats ds = recorder.getDetailedStats((int) hs.totalObjSize / 1000);

		Assert.assertEquals(4, hs.ptrSize);
		Assert.assertEquals(8, hs.objHeaderSize);

		ptrSize = hs.ptrSize;

		checkObjectHisto(hs, STRING_OBJ_SIZE);

		checkProblematicCollections(hs);

		checkProblematicObjectArrays(hs);

		checkForLZTPrimitiveArrays(hs, ds);

		checkForUHBPrimitiveArrays(hs, ds);

		checkProblematicCollectionClusters(ds);

		checkAlwaysNullFields(hs);
	}

	private void checkObjectHisto(HeapStats hs, int strInstSize) {
		List<ObjectHistogram.Entry> objHistogram = hs.objHisto.getListSortedByInclusiveSize(0);

		boolean foundDataClass1 = false;
		boolean foundEmptyInstanceClass = false;
		for (ObjectHistogram.Entry entry : objHistogram) {
			if (entry.getClazz().getName().equals(VariousIssues.DataClass1.class.getName())) {
				foundDataClass1 = true;
				Assert.assertEquals(entry.getTotalInclusiveSize(), entry.getTotalShallowSize());
				Assert.assertEquals(VariousIssues.DATA_CLASS_1_INSTANCES, entry.getNumInstances());

				int objSize = OBJ_HDR_SIZE + PTR_SIZE + 4 + PTR_SIZE + PTR_SIZE;
				objSize = MiscUtils.getAlignedObjectSize(objSize, 8);
				long expectedTotalSize = objSize * entry.getNumInstances();

				Assert.assertEquals(expectedTotalSize, entry.getTotalShallowSize());

			} else if (entry.getClazz().getName().equals(VariousIssues.EmptyInstanceClass.class.getName())) {
				foundEmptyInstanceClass = true;
				Assert.assertEquals(entry.getTotalInclusiveSize(), entry.getTotalShallowSize());
				Assert.assertEquals(VariousIssues.SPARSE_MAP1_ELEMENTS, entry.getNumInstances());
				long expectedTotalSize = OBJ_HDR_SIZE * entry.getNumInstances();
				Assert.assertEquals(expectedTotalSize, entry.getTotalShallowSize());

			} else if (entry.getClazz().getName().equals(Constants.JAVA_LANG_STRING)) {
				Assert.assertTrue(entry.getTotalInclusiveSize() != entry.getTotalShallowSize());
				long expectedTotalShallowSize = strInstSize * entry.getNumInstances();
				Assert.assertEquals(expectedTotalShallowSize, entry.getTotalShallowSize());
			}
		}

		Assert.assertTrue(foundDataClass1);
		Assert.assertTrue(foundEmptyInstanceClass);
	}

	/**
	 * Checks the information reported for problematic collections. Since a real heap dump always
	 * contains some data structures generated by the JDK itself, we cannot simply check for our own
	 * data, and have to use workarounds...
	 */
	private void checkProblematicCollections(HeapStats hs) throws Exception {
		boolean foundSparseHashMap = false;
		boolean foundEmptyUnusedCHM = false;
		boolean foundEmptyUsedCHM = false;
		boolean foundSparseWeakHashMap = false;
		boolean foundBarArrayList = false;

		for (CollectionClassDescriptor cd : hs.overheadsByClass) {
			String className = cd.getClassName();
			if (className.equals(Constants.HASH_MAP)) {
				foundSparseHashMap = true;
				int expectedOverhead = getExpectedSparseMapOverhead(variousIssues.sparseMap1);
				Assert.assertTrue(
						cd.getProblematicCollectionsOverhead(Constants.ProblemKind.SPARSE_LARGE) >= expectedOverhead);
			} else if (className.equals(Constants.CONCURRENT_HASH_MAP)) {
				// Overhead calculation below relies on CHM implementation in JDK6. From JDK7 update ?,
				// some CHM sub-structures are allocated lazily
				int unusedColsOvhd = cd.getProblematicCollectionsOverhead(Constants.ProblemKind.EMPTY_UNUSED);
				if (unusedColsOvhd > 0) {
					foundEmptyUnusedCHM = true;
					int expectedOverhead = getExpectedEmptyCHMOverhead(variousIssues.emptyUnusedConcHashMaps.length,
							VariousIssues.EMPTY_CHM_CAPACITY);
					Assert.assertEquals(expectedOverhead, unusedColsOvhd);
				}
				int usedColsOvhd = cd.getProblematicCollectionsOverhead(Constants.ProblemKind.EMPTY_USED);
				if (usedColsOvhd > 0) {
					foundEmptyUsedCHM = true;
					int expectedOverhead = getExpectedEmptyCHMOverhead(variousIssues.emptyUsedConcHashMaps.length,
							VariousIssues.EMPTY_CHM_CAPACITY);
					Assert.assertEquals(expectedOverhead, usedColsOvhd);
				}
			} else if (className.equals(Constants.WEAK_HASH_MAP)) {
				foundSparseWeakHashMap = true;
				int expectedOverhead = getExpectedSparseMapOverhead(variousIssues.weakHashMapWithBackRefs);
				Assert.assertTrue(
						cd.getProblematicCollectionsOverhead(Constants.ProblemKind.SPARSE_LARGE) >= expectedOverhead);
			} else if (className.equals(Constants.ARRAY_LIST)) {
				foundBarArrayList = true;
				int expectedOverhead = getExpectedBarArrayListOverhead();
				Assert.assertEquals(expectedOverhead, cd.getProblematicCollectionsOverhead(Constants.ProblemKind.BAR));
			}
		}

		Assert.assertTrue(foundSparseHashMap);
		Assert.assertTrue(foundEmptyUnusedCHM);
		Assert.assertTrue(foundEmptyUsedCHM);
		Assert.assertTrue(foundSparseWeakHashMap);
		Assert.assertTrue(foundBarArrayList);
	}

	private void checkProblematicObjectArrays(HeapStats hs) {
		boolean foundBarObjectArray = false;

		for (CollectionClassDescriptor cd : hs.overheadsByClass) {
			String className = cd.getClassName();
			if (className.equals("[[Lexamples.VariousIssues$ObjectSubClass;")) {
				foundBarObjectArray = true;
				int expectedOverhead = getExpectedBarArrayOverhead(variousIssues.barArray1);
				Assert.assertEquals(expectedOverhead, cd.getProblematicCollectionsOverhead(Constants.ProblemKind.BAR));
			}
		}

		Assert.assertTrue(foundBarObjectArray);
	}

	private int getExpectedSparseMapOverhead(Object map) throws NoSuchFieldException, IllegalAccessException {
		Field tableField = map.getClass().getDeclaredField("table");
		tableField.setAccessible(true);
		Object[] table = (Object[]) tableField.get(map);
		int numNullSlots = 0;
		for (int i = 0; i < table.length; i++) {
			if (table[i] == null) {
				numNullSlots++;
			}
		}

		return numNullSlots * PTR_SIZE;
	}

	/**
	 * Relies on ConcurrentHashMap (CHM) implementaiton in JDK6. From JDK7 update ?, some CHM
	 * substructures are allocated lazily, so the code below will need to be changed.
	 */
	private int getExpectedEmptyCHMOverhead(int numCHMs, int chmCapacity)
			throws NoSuchFieldException, IllegalAccessException {
		int objHeaderSize = snapshot.getObjectHeaderSize();
		int arrHeaderSize = snapshot.getArrayHeaderSize();

		int chmSize = objHeaderSize + 2 * 4 // Primitive fields defined in CHM
				+ 4 * ptrSize // Pointer fields defined in CHM
				+ 2 * ptrSize; // Pointer fields defined in superclass AbstractMap
		chmSize = MiscUtils.getAlignedObjectSize(chmSize, 8);

		int numSegments = 16;

		int segArraySize = arrHeaderSize + numSegments * ptrSize;
		segArraySize = MiscUtils.getAlignedObjectSize(segArraySize, 8);

		int segSize = objHeaderSize + 4 * 4 + ptrSize + ptrSize; // One ptr in superclass, ReentrantLock
		segSize = MiscUtils.getAlignedObjectSize(segSize, 8);

		int oneSegCapacity = chmCapacity / numSegments;
		int numEntriesInTable = findNextPowerOfTwoNumber(oneSegCapacity); // entries.length;
		int tableArraySize = arrHeaderSize + numEntriesInTable * ptrSize;
		tableArraySize = MiscUtils.getAlignedObjectSize(tableArraySize, 8);

		// There is a field called 'sync' in segment's superclass
		int syncSize = objHeaderSize + 3 * ptrSize + 4; // Inherits 3 ptr and one int field from superclasses
		syncSize = MiscUtils.getAlignedObjectSize(syncSize, 8);

		int totalSize = chmSize + segArraySize + numSegments * (segSize + tableArraySize + syncSize);
		return totalSize * numCHMs;
	}

	private int getExpectedBarArrayOverhead(Object[][] arr) {
		int arrHeaderSize = snapshot.getArrayHeaderSize();
		int objAlignment = snapshot.getObjectAlignment();

		int outerDim = arr.length;
		int innerDim = arr[0].length;

		int innerArrayWorkloadSize = ptrSize * innerDim;
		int innerArraySize = MiscUtils.getAlignedObjectSize(arrHeaderSize + innerArrayWorkloadSize, objAlignment);
		int origArrayOvhd = (innerArraySize - innerArrayWorkloadSize + ptrSize) * outerDim;

		// Now calculate the same metrics for the "flipped" array
		int newInnerArrayWorkloadSize = ptrSize * outerDim;
		int newInnerArraySize = MiscUtils.getAlignedObjectSize(arrHeaderSize + newInnerArrayWorkloadSize, objAlignment);
		int newArrayOvhd = (newInnerArraySize - newInnerArrayWorkloadSize + ptrSize) * innerDim;

		return origArrayOvhd - newArrayOvhd;
	}

	private int getExpectedBarArrayListOverhead() {
		// We know that our BAR ArrayList and array have same dimensions
		Object[][] arr = variousIssues.barArray1;
		int ovhd = getExpectedBarArrayOverhead(arr);
		int arrayListObjSize = snapshot.getClassForName("java.util.ArrayList").getInstanceSize();

		int outerDim = arr.length;
		int innerDim = arr[0].length;

		ovhd += (outerDim - innerDim) * arrayListObjSize;
		return ovhd;
	}

	private void checkProblematicCollectionClusters(DetailedStats ds) throws Exception {
		List<ReferencedObjCluster.Collections> colFields = ds.collectionClusters.get(1);

		boolean foundSparseMap1 = false;

		for (ReferencedObjCluster.Collections c : colFields) {
			String classAndField = ReferenceChain.toStringInStraightOrder(c.getReferer());
			if (classAndField.startsWith(VariousIssues.class.getName() + ".sparseMap1")) {
				foundSparseMap1 = true;
				int expectedOverhead = getExpectedSparseMapOverhead(variousIssues.sparseMap1);
				Assert.assertEquals(expectedOverhead, c.getTotalOverhead());
			}
		}

		Assert.assertTrue(foundSparseMap1);
	}

	private void checkForLZTPrimitiveArrays(HeapStats hs, DetailedStats ds) {
		boolean foundLZTCharArray = false;
		int expectedLZTCharOverhead = (VariousIssues.LZT_CHAR_ARRAY_SIZE - VariousIssues.LZT_CHAR_ARRAY_WORK_SIZE) * 2;
		int expectedUnusedHiByteCharOvhd = VariousIssues.LZT_CHAR_ARRAY_SIZE; // Only half of each char is used
		boolean foundLZTIntArray = false;
		int expectedLZTIntOvhd = (VariousIssues.LZT_INT_ARRAY_SIZE - VariousIssues.LZT_INT_ARRAY_WORK_SIZE) * 4;
		boolean foundLZTLongArray = false;
		int expectedLZTLongOvhd = (VariousIssues.LZT_LONG_ARRAY_SIZE - VariousIssues.LZT_LONG_ARRAY_WORK_SIZE) * 8;

		// In checks below we have greater or equals instead of equals, since the
		// Java runtime generates its own data structures that may have the same problem.
		for (CollectionClassDescriptor cd : hs.overheadsByClass) {
			String className = cd.getClassName();
			if (className.equals("[C")) {
				foundLZTCharArray = true;
				int actualOvhd = cd.getProblematicCollectionsOverhead(Constants.ProblemKind.LZT);
				Assert.assertTrue("Expected: " + expectedLZTCharOverhead + ", actual: " + actualOvhd,
						actualOvhd >= expectedLZTCharOverhead);
			} else if (className.equals("[I")) {
				foundLZTIntArray = true;
				int actualOvhd = cd.getProblematicCollectionsOverhead(Constants.ProblemKind.LZT);
				Assert.assertTrue("Expected: " + expectedLZTIntOvhd + ", actual: " + actualOvhd,
						actualOvhd >= expectedLZTIntOvhd);
			} else if (className.equals("[J")) {
				foundLZTLongArray = true;
				int actualOvhd = cd.getProblematicCollectionsOverhead(Constants.ProblemKind.LZT);
				Assert.assertTrue("Expected: " + expectedLZTIntOvhd + ", actual: " + actualOvhd,
						actualOvhd >= expectedLZTLongOvhd);
			}
		}

		Assert.assertTrue(foundLZTCharArray);
		Assert.assertTrue(foundLZTIntArray);
		Assert.assertTrue(foundLZTLongArray);

		foundLZTCharArray = false;
		foundLZTIntArray = false;
		foundLZTLongArray = false;

		List<ReferencedObjCluster.Collections> colFields = ds.collectionClusters.get(1);

		for (ReferencedObjCluster.Collections c : colFields) {
			String classAndField = ReferenceChain.toStringInStraightOrder(c.getReferer());
			if (classAndField.startsWith(VariousIssues.class.getName() + ".lztCharArray")) {
				foundLZTCharArray = true;
				Assert.assertEquals(expectedLZTCharOverhead + expectedUnusedHiByteCharOvhd, c.getTotalOverhead());
				List<ClassAndOvhdCombo> problems = c.getList();
				ClassAndOvhdCombo prob1 = problems.get(0);
				Assert.assertEquals(Constants.ProblemKind.LZT, prob1.getProblemKind());
				Assert.assertEquals(expectedLZTCharOverhead, prob1.getOverhead());
				ClassAndOvhdCombo prob2 = problems.get(1);
				Assert.assertEquals(Constants.ProblemKind.UNUSED_HI_BYTES, prob2.getProblemKind());
				Assert.assertEquals(expectedUnusedHiByteCharOvhd, prob2.getOverhead());
			} else if (classAndField.startsWith(VariousIssues.class.getName() + ".lztIntArray")) {
				foundLZTIntArray = true;
				Assert.assertEquals(expectedLZTIntOvhd, c.getTotalOverhead());
			} else if (classAndField.startsWith(VariousIssues.class.getName() + ".lztLongArray")) {
				foundLZTLongArray = true;
				Assert.assertEquals(expectedLZTLongOvhd, c.getTotalOverhead());
			}
		}

		Assert.assertTrue(foundLZTCharArray);
		Assert.assertTrue(foundLZTIntArray);
		Assert.assertTrue(foundLZTLongArray);
	}

	private void checkForUHBPrimitiveArrays(HeapStats hs, DetailedStats ds) {
		boolean foundUHBCharArray = false;
		int expectedUHBCharOverhead = VariousIssues.UNUSED_HI_BYTES_CHAR_ARRAY_SIZE;
		boolean foundUHBIntArray = false;
		int expectedUHBIntOvhd = VariousIssues.UNUSED_HI_BYTES_INT_ARRAY_SIZE * 2;
		boolean foundUHBLongArray = false;
		int expectedUHBLongOvhd = VariousIssues.UNUSED_HI_BYTES_LONG_ARRAY_SIZE * 6;

		// In checks below we have greater or equals instead of equals, since the
		// Java runtime generates its own data structures that may have the same problem.
		for (CollectionClassDescriptor cd : hs.overheadsByClass) {
			String className = cd.getClassName();
			if (className.equals("[C")) {
				foundUHBCharArray = true;
				int actualOvhd = cd.getProblematicCollectionsOverhead(Constants.ProblemKind.UNUSED_HI_BYTES);
				Assert.assertTrue("Expected: " + expectedUHBCharOverhead + ", actual: " + actualOvhd,
						actualOvhd >= expectedUHBCharOverhead);
			} else if (className.equals("[I")) {
				foundUHBIntArray = true;
				int actualOvhd = cd.getProblematicCollectionsOverhead(Constants.ProblemKind.UNUSED_HI_BYTES);
				Assert.assertTrue("Expected: " + expectedUHBIntOvhd + ", actual: " + actualOvhd,
						actualOvhd >= expectedUHBIntOvhd);
			} else if (className.equals("[J")) {
				foundUHBLongArray = true;
				int actualOvhd = cd.getProblematicCollectionsOverhead(Constants.ProblemKind.UNUSED_HI_BYTES);
				Assert.assertTrue("Expected: " + expectedUHBIntOvhd + ", actual: " + actualOvhd,
						actualOvhd >= expectedUHBLongOvhd);
			}
		}

		Assert.assertTrue(foundUHBCharArray);
		Assert.assertTrue(foundUHBIntArray);
		Assert.assertTrue(foundUHBLongArray);

		foundUHBCharArray = false;
		foundUHBIntArray = false;
		foundUHBLongArray = false;

		List<ReferencedObjCluster.Collections> colFields = ds.collectionClusters.get(1);

		for (ReferencedObjCluster.Collections c : colFields) {
			String classAndField = ReferenceChain.toStringInStraightOrder(c.getReferer());
			if (classAndField.startsWith(VariousIssues.class.getName() + ".uhbCharArray")) {
				foundUHBCharArray = true;
				Assert.assertEquals(expectedUHBCharOverhead, c.getTotalOverhead());
			} else if (classAndField.startsWith(VariousIssues.class.getName() + ".uhbIntArray")) {
				foundUHBIntArray = true;
				Assert.assertEquals(expectedUHBIntOvhd, c.getTotalOverhead());
			} else if (classAndField.startsWith(VariousIssues.class.getName() + ".uhbLongArray")) {
				foundUHBLongArray = true;
				Assert.assertEquals(expectedUHBLongOvhd, c.getTotalOverhead());
			}
		}

		Assert.assertTrue(foundUHBCharArray);
		Assert.assertTrue(foundUHBIntArray);
		Assert.assertTrue(foundUHBLongArray);
	}

	private void checkAlwaysNullFields(HeapStats hs) throws Exception {
		ObjectHistogram objHisto = hs.objHisto;
		List<ObjectHistogram.ProblemFieldsEntry> nfClasses = objHisto.getListSortedByNullFieldsOvhd(1.0f);
		boolean dataClass1Found = false;
		boolean emptyInstanceClassFound = false;

		for (ObjectHistogram.ProblemFieldsEntry nfClass : nfClasses) {
			if (nfClass.getClazz().getName().equals(VariousIssues.DataClass1.class.getName())) {
				dataClass1Found = true;
				Assert.assertEquals(VariousIssues.DATA_CLASS_1_INSTANCES, nfClass.getNumInstances());
				String[] fieldNames = nfClass.getProblemFieldNames();
				Assert.assertEquals(1, fieldNames.length);
				Assert.assertEquals("alwaysNull", fieldNames[0]);
				Assert.assertEquals(nfClass.getNumInstances() * PTR_SIZE, nfClass.getAllProblemFieldsOvhd());
				Assert.assertEquals(ObjectHistogram.ProblemFieldsEntry.Status.SOME_FIELDS_EMPTY, nfClass.getStatus());
			} else if (nfClass.getClazz().getName().equals(VariousIssues.EmptyInstanceClass.class.getName())) {
				emptyInstanceClassFound = true;
				int numInstances = VariousIssues.SPARSE_MAP1_ELEMENTS;
				Assert.assertEquals(numInstances, nfClass.getNumInstances());
				int objSize = OBJ_HDR_SIZE;
				Assert.assertEquals(objSize * numInstances, nfClass.getAllProblemFieldsOvhd());
			}
		}

		Assert.assertTrue(dataClass1Found);
		Assert.assertTrue(emptyInstanceClassFound);

		nfClasses = objHisto.getListSortedByNullFieldsOvhd(0.9f);
		boolean almostAlwaysAllNullClassFound = false;

		for (ObjectHistogram.ProblemFieldsEntry nfClass : nfClasses) {
			if (nfClass.getClazz().getName().equals(VariousIssues.AlmostAlwaysAllNullClass.class.getName())) {
				almostAlwaysAllNullClassFound = true;
				int numAllNullInstances = VariousIssues.NULL_CLASS_INSTANCES;
				int numClassInstances = numAllNullInstances + VariousIssues.NOT_NULL_CLASS_INSTANCES;
				Assert.assertEquals(numClassInstances, nfClass.getNumInstances());
				int nullFieldsSize = PTR_SIZE + 4;
				int expectedOverhead = nullFieldsSize * numAllNullInstances;
				Assert.assertEquals(expectedOverhead, nfClass.getAllProblemFieldsOvhd());
			}
		}

		Assert.assertTrue(almostAlwaysAllNullClassFound);
	}

	private static int findNextPowerOfTwoNumber(int value) {
		int result = 1;
		while (result < value) {
			result <<= 1;
		}
		return result;
	}
}
