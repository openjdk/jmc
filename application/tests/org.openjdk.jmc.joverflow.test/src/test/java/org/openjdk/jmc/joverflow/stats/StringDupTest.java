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

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jmc.joverflow.batch.BatchProblemRecorder;
import org.openjdk.jmc.joverflow.batch.DetailedStats;
import org.openjdk.jmc.joverflow.batch.ReferencedObjCluster;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.support.DupStringStats;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.support.ReferenceChain;
import org.openjdk.jmc.joverflow.util.ObjectToIntMap;

import examples.DuplicateStrings;

/**
 * Testing our duplicated string calculation functionality. Uses a heap dump called
 * duplicate-strings.hprof, that is generated from examples.DuplicateStrings running on the HotSpot
 * JVM in 32-bit mode.
 */
@Ignore("Requires a generated hprof file. This test can be reenabled if the file is available.")
public class StringDupTest {

	private static final int STRING_OBJ_SIZE = 24; // In 32-bit mode
	private static final int ARRAY_HDR_SIZE = Constants.STANDARD_32BIT_OBJ_HEADER_SIZE + 4;

	// Generate the same data as in the heap dump, to make some calculations easier
	private DuplicateStrings dsExample = new DuplicateStrings();

	@Test
	public void testStringDupStats() throws Exception {
		runTest(false);
		runTest(true);
	}

	private void runTest(boolean useBreadthFirst) throws Exception {
		Snapshot snapshot = SnapshotReader.readAndResolveHeapDump("duplicate-strings.hprof");
		BatchProblemRecorder recorder = new BatchProblemRecorder();
		StandardStatsCalculator ssc = new StandardStatsCalculator(snapshot, recorder, useBreadthFirst);
		HeapStats hs = ssc.calculate();
		DetailedStats ds = recorder.getDetailedStats((int) hs.totalObjSize / 1000);

		DupStringStats dss = hs.dupStringStats;

		int strInstSize = STRING_OBJ_SIZE;
		Assert.assertEquals(strInstSize, dss.stringInstShallowSize);

		// Statistics for individual strings
		checkIndividualDupStrings(dss, strInstSize);

		// Statistics for dup string clusters
		checkDupStringClusters(ds, strInstSize);
	}

	private void checkDupStringClusters(DetailedStats ds, int strInstSize) {
		int overhead;
		List<ReferencedObjCluster.DupStrings> dupStringsToFields = ds.dupStringClusters.get(1);

		ReferencedObjCluster.DupStrings c = dupStringsToFields.get(0);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_12, c.getNumBadObjects());
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_12, c.getNumDupBackingCharArrays());
		Assert.assertEquals(0, c.getNumNonDupStrings());
		ObjectToIntMap.Entry<String> entries[] = c.getEntries();
		Assert.assertEquals(2, entries.length);
		int nTopDupStrings1 = DuplicateStrings.N_SAME_STRINGS_12 * 2 / 3;
		int nTopDupStrings2 = DuplicateStrings.N_SAME_STRINGS_12 / 3;
		Assert.assertEquals(dsExample.TOP_DUP_STRING_1, entries[0].key);
		Assert.assertEquals(nTopDupStrings1, entries[0].value);
		Assert.assertEquals(dsExample.TOP_DUP_STRING_2, entries[1].key);
		Assert.assertEquals(nTopDupStrings2, entries[1].value);

		int singleStringInclusiveSize = strInstSize + entries[0].key.length() * 2 + ARRAY_HDR_SIZE;

		/*
		 * The global number of TOP_DUP_STRING_1 copies is nTopDupStrings1 + 1 (number of copies in
		 * dupStrings12 array plus the single instance in uniqueStrings). So the global overhead for
		 * this string is (nTopDupStrings1 + 1 - 1) * string_size = nTopDupStrings1 * string_size
		 * The overhead per string copy is nTopDupStrings1 * string_size / (nTopDupStrings1 + 1) The
		 * overhead of this cluster is proportional to the number of strings in it, i.e.
		 * nTopDupStrings1 * nTopDupStrings1 * string_size / (nTopDupStrings1 + 1). It is easy to
		 * show that when x is a large number, x^2 / (x + 1) ~= (x^2 - 1) / (x + 1) = x - 1 So the
		 * value of the above expression is very close to (nTopDupStrings1 - 1) * string_size Same
		 * with other overhead calculations in this code. Note, however, that since the actual
		 * overhead per cluster is calculated in a different way (by summing up approximate overhead
		 * per each string copy as it's added to the cluster, see
		 * DupStringStats.getOvhdForNextStringCopy()), the actual calculated number may be slightly
		 * different than the above approximation. Furthermore, this actual value may change
		 * depending on the order in which strings are added to several clusters (say first half
		 * strings are added to cluster1, then another half to cluster2, or vice versa).
		 *
		 * NOTE: we wouldn't have this complexity below if we simply set uniqueStrings to null in
		 * the end of the example that generates this code. However, it's somewhat interesting to
		 * test how JOverflow handles some non-trivial data :-)
		 */
		int topDupString1Ovhd = (nTopDupStrings1 - 1) * singleStringInclusiveSize;
		int topDupString2Ovhd = (nTopDupStrings2 - 1) * singleStringInclusiveSize;
		overhead = topDupString1Ovhd + topDupString2Ovhd;
		// See above why our overhead calculation is not absolutely exact
		Assert.assertTrue("Expected: " + overhead + ", actual: " + c.getTotalOverhead(),
				Math.abs(overhead - c.getTotalOverhead()) <= 2);
		Assert.assertTrue(ReferenceChain.toStringInStraightOrder(c.getReferer())
				.startsWith("examples.DuplicateStrings.dupStrings12"));

		c = dupStringsToFields.get(1);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_0, c.getNumBadObjects());
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_0, c.getNumDupBackingCharArrays());
		Assert.assertEquals(0, c.getNumNonDupStrings());
		entries = c.getEntries();
		Assert.assertEquals(1, entries.length);
		Assert.assertEquals(dsExample.TOP_DUP_STRING_0, entries[0].key);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_0, entries[0].value);
		overhead = (DuplicateStrings.N_SAME_STRINGS_0 - 1) * singleStringInclusiveSize;
		Assert.assertTrue("Expected: " + overhead + ", actual: " + c.getTotalOverhead(),
				Math.abs(overhead - c.getTotalOverhead()) <= 2);
		Assert.assertTrue(ReferenceChain.toStringInStraightOrder(c.getReferer())
				.startsWith("examples.DuplicateStrings.dupStrings0"));

		c = dupStringsToFields.get(2);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_3, c.getNumBadObjects());
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_3, c.getNumDupBackingCharArrays());
		Assert.assertEquals(DuplicateStrings.N_NONDUP_STRINGS_3, c.getNumNonDupStrings());
		entries = c.getEntries();
		Assert.assertEquals(1, entries.length);
		Assert.assertEquals(dsExample.TOP_DUP_STRING_3, entries[0].key);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_3, entries[0].value);
		overhead = (DuplicateStrings.N_SAME_STRINGS_3 - 1) * singleStringInclusiveSize;
		Assert.assertTrue("Expected: " + overhead + ", actual: " + c.getTotalOverhead(),
				Math.abs(overhead - c.getTotalOverhead()) <= 2);
		Assert.assertTrue(ReferenceChain.toStringInStraightOrder(c.getReferer())
				.startsWith("examples.DuplicateStrings.dupStrings3"));

		c = dupStringsToFields.get(3);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_4, c.getNumBadObjects());
		// 400 strings below point to a separate backing array each, and the remaining 400 point to
		// the same backing array, so 400 + 1 = 401 backing array
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_4 / 2 + 1, c.getNumDupBackingCharArrays());
		Assert.assertEquals(0, c.getNumNonDupStrings());
		entries = c.getEntries();
		int charArraySize = entries[0].key.length() * 2 + ARRAY_HDR_SIZE;
		Assert.assertEquals(1, entries.length);
		Assert.assertEquals(dsExample.TOP_DUP_STRING_4, entries[0].key);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_4, entries[0].value);
		overhead = (DuplicateStrings.N_SAME_STRINGS_4 - 1) * strInstSize
				+ (DuplicateStrings.N_SAME_STRINGS_4 / 2) * charArraySize;
		Assert.assertTrue("Expected: " + overhead + ", actual: " + c.getTotalOverhead(),
				Math.abs(overhead - c.getTotalOverhead()) <= 2);
		Assert.assertTrue(ReferenceChain.toStringInStraightOrder(c.getReferer())
				.startsWith("examples.DuplicateStrings.dupStrings4"));
	}

	private void checkIndividualDupStrings(DupStringStats dss, int strInstSize) {
		DupStringStats.Entry dupStr = dss.dupStrings.get(0);
		Assert.assertEquals(dsExample.TOP_DUP_STRING_0, dupStr.string);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_0 + 1, dupStr.nStringInstances);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_0 + 1, dupStr.nBackingArrays);
		// Overhead in this case is calculated as String object size (24 bytes in 32-bit mode)
		// plus char[] array size (size of chars + 12 bytes array header). 8-byte alignment
		// happens automatically for these numbers, fortunately.
		int overhead = DuplicateStrings.N_SAME_STRINGS_0 * (strInstSize + dupStr.string.length() * 2 + ARRAY_HDR_SIZE);
		Assert.assertEquals(overhead, dupStr.overhead);

		dupStr = dss.dupStrings.get(1);
		Assert.assertEquals(dsExample.TOP_DUP_STRING_1, dupStr.string);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_12 * 2 / 3 + 1, dupStr.nStringInstances);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_12 * 2 / 3 + 1, dupStr.nBackingArrays);
		overhead = DuplicateStrings.N_SAME_STRINGS_12 * 2 / 3
				* (strInstSize + dupStr.string.length() * 2 + ARRAY_HDR_SIZE);
		Assert.assertEquals(overhead, dupStr.overhead);

		dupStr = dss.dupStrings.get(2);
		Assert.assertEquals(dsExample.TOP_DUP_STRING_2, dupStr.string);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_12 / 3 + 1, dupStr.nStringInstances);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_12 / 3 + 1, dupStr.nBackingArrays);
		overhead = DuplicateStrings.N_SAME_STRINGS_12 / 3 * (strInstSize + dupStr.string.length() * 2 + ARRAY_HDR_SIZE);
		Assert.assertEquals(overhead, dupStr.overhead);

		dupStr = dss.dupStrings.get(3);
		Assert.assertEquals(dsExample.TOP_DUP_STRING_3, dupStr.string);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_3 + 1, dupStr.nStringInstances);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_3 + 1, dupStr.nBackingArrays);
		overhead = DuplicateStrings.N_SAME_STRINGS_3 * (strInstSize + dupStr.string.length() * 2 + ARRAY_HDR_SIZE);
		Assert.assertEquals(overhead, dupStr.overhead);

		dupStr = dss.dupStrings.get(4);
		Assert.assertEquals(dsExample.TOP_DUP_STRING_4, dupStr.string);
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_4 + 2, dupStr.nStringInstances);
		// Num of backing arrays below is for: original string (uniqueStrings[4])
		// plus one copy (TOP_DUP_STRING_4) plus 400 more copies of TOP_DUP_STRING_4
		// created in the loop
		Assert.assertEquals(DuplicateStrings.N_SAME_STRINGS_4 / 2 + 2, dupStr.nBackingArrays);
		overhead = (DuplicateStrings.N_SAME_STRINGS_4 + 1) * strInstSize
				+ (DuplicateStrings.N_SAME_STRINGS_4 / 2 + 1) * (dupStr.string.length() * 2 + ARRAY_HDR_SIZE);
		Assert.assertEquals(overhead, dupStr.overhead);
	}
}
