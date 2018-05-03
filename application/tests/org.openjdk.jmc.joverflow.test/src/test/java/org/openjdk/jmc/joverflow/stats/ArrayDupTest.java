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

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.openjdk.jmc.joverflow.batch.BatchProblemRecorder;
import org.openjdk.jmc.joverflow.batch.DetailedStats;
import org.openjdk.jmc.joverflow.batch.ReferencedObjCluster;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.support.DupArrayStats;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.support.PrimitiveArrayWrapper;
import org.openjdk.jmc.joverflow.support.ReferenceChain;
import org.openjdk.jmc.joverflow.util.MiscUtils;
import org.openjdk.jmc.joverflow.util.ObjectToIntMap;

import examples.DuplicateArrays;

/**
 * Testing our duplicated array calculation functionality. Uses a heap dump called
 * duplicate-arrays.hprof, that is generated from examples.DuplicateArrays running on the HotSpot
 * JVM in 32-bit mode.
 */
@Ignore("Requires a generated hprof file. This test can be reenabled if the file is available.")
public class ArrayDupTest {

	private static final int ARRAY_HDR_SIZE = Constants.STANDARD_32BIT_OBJ_HEADER_SIZE + 4;
	private static final int ALIGNMENT = Constants.DEFAULT_OBJECT_ALIGNMENT_IN_MEMORY;

	// Generate the same data as in the heap dump, to make some calculations easier
	private DuplicateArrays daExample = new DuplicateArrays();

	@Test
	public void testArrayDupStats() throws Exception {
		runTest(false);
		runTest(true);
	}

	private void runTest(boolean useBreadthFirst) throws Exception {
		Snapshot snapshot = SnapshotReader.readAndResolveHeapDump("duplicate-arrays.hprof");
		BatchProblemRecorder recorder = new BatchProblemRecorder();
		StandardStatsCalculator ssc = new StandardStatsCalculator(snapshot, recorder, useBreadthFirst);
		HeapStats hs = ssc.calculate();
		DetailedStats ds = recorder.getDetailedStats((int) hs.totalObjSize / 1000);

		// Statistics for individual arrays
		checkIndividualDupArrays(hs.dupArrayStats);

		// Statistics for dup array clusters
		checkDupArrayClusters(ds);
	}

	private void checkDupArrayClusters(DetailedStats ds) {
		int overhead;
		List<ReferencedObjCluster.DupArrays> dupArraysToFields = ds.dupArrayClusters.get(1);

		ReferencedObjCluster.DupArrays c = dupArraysToFields.get(0);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_1, c.getNumBadObjects());
		Assert.assertEquals(DuplicateArrays.N_NONDUP_ARRAYS_1, c.getNumNonDupArrays());
		ObjectToIntMap.Entry<PrimitiveArrayWrapper> entries[] = c.getEntries();
		Assert.assertEquals(3, entries.length);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_1 / 3, entries[0].value);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_1 / 3, entries[1].value);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_1 / 3, entries[2].value);

		int arraySize = DuplicateArrays.ARRAY_LENGTH * 4 + ARRAY_HDR_SIZE;
		arraySize = MiscUtils.getAlignedObjectSize(arraySize, ALIGNMENT);
		// -3 below is because this array contains copied sub-arrays with 3 different values
		overhead = (DuplicateArrays.N_SAME_ARRAYS_1 - 3) * arraySize;

		// Just in case: since the actual overhead per cluster is calculated by
		// summing up approximate overhead per each array copy as it's encountered
		// (see DupArrayStats.getOvhdForNextArrayCopy()), the actual calculated
		// number may be slightly different than the above ideal number.
		// Furthermore, this actual value may change depending on the order in
		// which arrays are added to several clusters (say first half strings
		// are added to cluster1, then another half to cluster2, or vice versa).
		// Some arrays may not even get into any cluster (actually, such an
		// array would form a cluster of 1 element), but they are still
		// handled by the above method, and thus the overhead calculated for
		// one array may affect the overhead calculated for the next one.
		Assert.assertEquals(overhead, c.getTotalOverhead());

		Assert.assertEquals("examples.DuplicateArrays$ArrayContainer.ints",
				ReferenceChain.toStringInStraightOrder(c.getReferer()));

		int[] ints = (int[]) entries[0].key.getArray().getElements();
		Assert.assertTrue(Arrays.equals(daExample.dupIntArrays1[0].ints, ints)
				^ Arrays.equals(daExample.dupIntArrays1[1].ints, ints)
				^ Arrays.equals(daExample.dupIntArrays1[2].ints, ints));

		c = dupArraysToFields.get(1);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_2, c.getNumBadObjects());
		Assert.assertEquals(DuplicateArrays.N_NONDUP_ARRAYS_2, c.getNumNonDupArrays());
		entries = c.getEntries();
		Assert.assertEquals(2, entries.length);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_2 / 2, entries[0].value);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_2 / 2, entries[1].value);

		arraySize = DuplicateArrays.ARRAY_LENGTH * 2 + ARRAY_HDR_SIZE;
		arraySize = MiscUtils.getAlignedObjectSize(arraySize, ALIGNMENT);
		// -2 below is because this array contains copied sub-arrays with 2 different values
		overhead = (DuplicateArrays.N_SAME_ARRAYS_2 - 2) * arraySize;
		Assert.assertEquals(overhead, c.getTotalOverhead());

		Assert.assertEquals("examples.DuplicateArrays$ArrayContainer.chars",
				ReferenceChain.toStringInStraightOrder(c.getReferer()));

		char[] chars = (char[]) entries[0].key.getArray().getElements();
		Assert.assertTrue(Arrays.equals(daExample.dupCharArrays2[0].chars, chars)
				^ Arrays.equals(daExample.dupCharArrays2[1].chars, chars));

		c = dupArraysToFields.get(2);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_0, c.getNumBadObjects());
		Assert.assertEquals(0, c.getNumNonDupArrays());
		entries = c.getEntries();
		Assert.assertEquals(1, entries.length);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_0, entries[0].value);

		arraySize = DuplicateArrays.ARRAY_LENGTH + ARRAY_HDR_SIZE;
		arraySize = MiscUtils.getAlignedObjectSize(arraySize, ALIGNMENT);
		overhead = (DuplicateArrays.N_SAME_ARRAYS_0 - 1) * arraySize;
		Assert.assertEquals(overhead, c.getTotalOverhead());

		Assert.assertEquals("examples.DuplicateArrays$ArrayContainer.bytes",
				ReferenceChain.toStringInStraightOrder(c.getReferer()));

		byte[] bytes = (byte[]) entries[0].key.getArray().getElements();
		Assert.assertTrue(Arrays.equals(daExample.dupByteArrays0[0].bytes, bytes));
	}

	private void checkIndividualDupArrays(DupArrayStats das) {
		// The first 3 entries are for int[] arrays
		DupArrayStats.Entry dupArray = das.dupArrays.get(0);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_1 / 3, dupArray.nArrayInstances);
		Assert.assertEquals("[I", dupArray.firstArray.getClazz().getName());
		Assert.assertEquals(DuplicateArrays.ARRAY_LENGTH, dupArray.firstArray.getLength());

		int arraySize = DuplicateArrays.ARRAY_LENGTH * 4 + ARRAY_HDR_SIZE;
		arraySize = MiscUtils.getAlignedObjectSize(arraySize, ALIGNMENT);
		int overhead = (DuplicateArrays.N_SAME_ARRAYS_1 / 3 - 1) * arraySize;
		Assert.assertEquals(overhead, dupArray.overhead);

		dupArray = das.dupArrays.get(1);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_1 / 3, dupArray.nArrayInstances);
		Assert.assertEquals("[I", dupArray.firstArray.getClazz().getName());
		Assert.assertEquals(DuplicateArrays.ARRAY_LENGTH, dupArray.firstArray.getLength());
		Assert.assertEquals(overhead, dupArray.overhead);

		dupArray = das.dupArrays.get(2);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_1 / 3, dupArray.nArrayInstances);
		Assert.assertEquals("[I", dupArray.firstArray.getClazz().getName());
		Assert.assertEquals(DuplicateArrays.ARRAY_LENGTH, dupArray.firstArray.getLength());
		Assert.assertEquals(overhead, dupArray.overhead);

		int[] ints = (int[]) dupArray.firstArray.getElements();
		Assert.assertTrue(Arrays.equals(daExample.dupIntArrays1[0].ints, ints)
				^ Arrays.equals(daExample.dupIntArrays1[1].ints, ints)
				^ Arrays.equals(daExample.dupIntArrays1[2].ints, ints));

		// The next entry is for byte[] arrays
		dupArray = das.dupArrays.get(3);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_0, dupArray.nArrayInstances);
		Assert.assertEquals("[B", dupArray.firstArray.getClazz().getName());
		Assert.assertEquals(DuplicateArrays.ARRAY_LENGTH, dupArray.firstArray.getLength());

		arraySize = DuplicateArrays.ARRAY_LENGTH + ARRAY_HDR_SIZE;
		arraySize = MiscUtils.getAlignedObjectSize(arraySize, ALIGNMENT);
		overhead = (DuplicateArrays.N_SAME_ARRAYS_0 - 1) * arraySize;
		Assert.assertEquals(overhead, dupArray.overhead);

		byte[] bytes = dupArray.firstArray.getValue();
		Assert.assertTrue(Arrays.equals(daExample.dupByteArrays0[0].bytes, bytes));

		// The next 2 entries are for char[] arrays
		dupArray = das.dupArrays.get(4);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_2 / 2, dupArray.nArrayInstances);
		Assert.assertEquals("[C", dupArray.firstArray.getClazz().getName());
		Assert.assertEquals(DuplicateArrays.ARRAY_LENGTH, dupArray.firstArray.getLength());

		arraySize = DuplicateArrays.ARRAY_LENGTH * 2 + ARRAY_HDR_SIZE;
		arraySize = MiscUtils.getAlignedObjectSize(arraySize, ALIGNMENT);
		overhead = (DuplicateArrays.N_SAME_ARRAYS_2 / 2 - 1) * arraySize;
		Assert.assertEquals(overhead, dupArray.overhead);

		char[] chars = (char[]) dupArray.firstArray.getElements();
		Assert.assertTrue(Arrays.equals(daExample.dupCharArrays2[0].chars, chars)
				^ Arrays.equals(daExample.dupCharArrays2[1].chars, chars));

		dupArray = das.dupArrays.get(5);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_2 / 2, dupArray.nArrayInstances);
		Assert.assertEquals("[C", dupArray.firstArray.getClazz().getName());
		Assert.assertEquals(DuplicateArrays.ARRAY_LENGTH, dupArray.firstArray.getLength());
		Assert.assertEquals(overhead, dupArray.overhead);

		chars = (char[]) dupArray.firstArray.getElements();
		Assert.assertTrue(Arrays.equals(daExample.dupCharArrays2[0].chars, chars)
				^ Arrays.equals(daExample.dupCharArrays2[1].chars, chars));

		// The next entry is for double[] arrays
		dupArray = das.dupArrays.get(6);
		Assert.assertEquals(DuplicateArrays.N_SAME_ARRAYS_3, dupArray.nArrayInstances);
		Assert.assertEquals("[D", dupArray.firstArray.getClazz().getName());
		Assert.assertEquals(DuplicateArrays.ARRAY_LENGTH, dupArray.firstArray.getLength());

		arraySize = DuplicateArrays.ARRAY_LENGTH * 8 + ARRAY_HDR_SIZE;
		arraySize = MiscUtils.getAlignedObjectSize(arraySize, ALIGNMENT);
		overhead = (DuplicateArrays.N_SAME_ARRAYS_3 - 1) * arraySize;
		Assert.assertEquals(overhead, dupArray.overhead);

		double[] doubles = (double[]) dupArray.firstArray.getElements();
		Assert.assertTrue(Arrays.equals(daExample.dupDoubleArrays3[0].doubles, doubles));
	}
}
