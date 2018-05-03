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
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.support.HeapStats;

import examples.VariousIssues3;

/**
 * Testing our general problematic data structure calculation functionality. Uses a heap dump called
 * various-issues3.hprof, that is generated from examples.VariousIssues3.
 */
@SuppressWarnings("unused")
@Ignore("Requires a generated hprof file. This test can be reenabled if the file is available.")
public class VariousIssueTest3 {

	@Test
	public void testVariousIssues() throws Exception {
		Snapshot snapshot = SnapshotReader.readAndResolveHeapDump("various-issues3.hprof");
		BatchProblemRecorder recorder = new BatchProblemRecorder();
		StandardStatsCalculator ssc = new StandardStatsCalculator(snapshot, recorder, false);
		HeapStats hs = ssc.calculate();
		DetailedStats ds = recorder.getDetailedStats((int) hs.totalObjSize / 1000);

		checkFieldsWithUnusedHiBytes(hs);
	}

	private void checkFieldsWithUnusedHiBytes(HeapStats hs) throws Exception {
		ObjectHistogram objHisto = hs.objHisto;
		List<ObjectHistogram.ProblemFieldsEntry> problemClasses = objHisto.getListSortedByUnusedHiByteFieldsOvhd(1.0f);
		boolean underusedIntsAndCharsFound = false;
		boolean underusedIntsFound = false;

		for (ObjectHistogram.ProblemFieldsEntry problemClass : problemClasses) {
			if (problemClass.getClazz().getName().equals(VariousIssues3.UnderusedIntsAndChars.class.getName())) {
				underusedIntsAndCharsFound = true;
				Assert.assertEquals(VariousIssues3.N_UINTS_INSTANCES, problemClass.getNumInstances());
				String[] fieldNames = problemClass.getProblemFieldNames();
				Assert.assertEquals(2, fieldNames.length);
				String[] expectedFieldNames = new String[] {"charUnused1Byte", "intUnused2Bytes"};
				assertContains(expectedFieldNames, fieldNames[0]);
				assertContains(expectedFieldNames, fieldNames[1]);
				Assert.assertEquals(3 * VariousIssues3.N_UINTS_INSTANCES, problemClass.getAllProblemFieldsOvhd());
				Assert.assertEquals(ObjectHistogram.ProblemFieldsEntry.Status.SOME_FIELDS_UNUSED_HI_BYTES,
						problemClass.getStatus());
			} else if (problemClass.getClazz().getName().equals(VariousIssues3.UnderusedInts.class.getName())) {
				underusedIntsFound = true;
				int numInstances = VariousIssues3.N_UINTS_INSTANCES;
				Assert.assertEquals(numInstances, problemClass.getNumInstances());
				String[] fieldNames = problemClass.getProblemFieldNames();
				Assert.assertEquals(1, fieldNames.length);
				Assert.assertEquals("intUnused3Bytes", fieldNames[0]);
				Assert.assertEquals(3 * VariousIssues3.N_UINTS_INSTANCES, problemClass.getAllProblemFieldsOvhd());
			}
		}

		Assert.assertTrue(underusedIntsAndCharsFound);
		Assert.assertTrue(underusedIntsFound);

		problemClasses = objHisto.getListSortedByUnusedHiByteFieldsOvhd(0.9f);
		boolean underusedLongsFound = false;

		for (ObjectHistogram.ProblemFieldsEntry problemClass : problemClasses) {
			if (problemClass.getClazz().getName().equals(VariousIssues3.UnderusedLongs.class.getName())) {
				underusedLongsFound = true;
				// Note that the number of instances reported below is the number of *all*
				// instances of VariousIssues3.UnderusedLongs. We cannot report the number of
				// actual "bad" instances (which seems more appropriate on the surface), because
				// in the general case we may or may not have a clear separation between "bad" and
				// "good" instances. That is, if a class has two problematic fields x, y and four
				// instances a, b, c, d - then x may be bad in a, b, c and y may be bad in b, c, d.
				// Then it's not clear whether all instances are "equally bad". Even if one instance,
				// say a, was completely good (all bytes are used in all fields in it), it would
				// still be very hard technically to segregate such instances.
				// However, the *overhead* we report is always accurate, i.e. says how many bytes
				// could be saved if we change field type in "bad" instances. However, it ignores
				// "good" instances (the ones where bytes are fully utilized) :-)
				Assert.assertEquals(VariousIssues3.N_ULONG_INSTANCES, problemClass.getNumInstances());
				int expectedOvhd = 6 * VariousIssues3.N_BAD_ULONG_INSTANCES;
				Assert.assertEquals(expectedOvhd, problemClass.getAllProblemFieldsOvhd());
			}
		}

		Assert.assertTrue(underusedLongsFound);
	}

	private void assertContains(String[] strs, String s) throws Exception {
		for (String str : strs) {
			if (s.equals(str)) {
				return;
			}
		}
		Assert.fail(s + " is not contained in " + arrayToString(strs));
	}

	private String arrayToString(String[] strs) {
		StringBuilder sb = new StringBuilder(strs[0].length() * strs.length + 10);
		sb.append('{');

		for (int i = 0; i < strs.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append('"').append(strs[i]).append('"');
		}
		sb.append('}');
		return sb.toString();
	}

}
