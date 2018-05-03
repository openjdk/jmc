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
import org.openjdk.jmc.joverflow.descriptors.CollectionClassDescriptor;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.support.ClassAndOvhdCombo;
import org.openjdk.jmc.joverflow.support.Constants.ProblemKind;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.support.ReferenceChain;
import org.openjdk.jmc.joverflow.util.MemNumFormatter;

import examples.VariousIssues2;

/**
 * Testing our general problematic data structure calculation functionality. Uses a heap dump called
 * various-issues2.hprof, that is generated from examples.VariousIssues2 running on the HotSpot JVM
 * in 32-bit mode.
 */
@Ignore("Requires a generated hprof file. This test can be reenabled if the file is available.")
public class VariousIssueTest2 {

	private int ptrSize;

	@Test
	public void testVariousIssues() throws Exception {
		runTest(false);
		runTest(true);
	}

	private void runTest(boolean useBreadthFirst) throws Exception {
		Snapshot snapshot = SnapshotReader.readAndResolveHeapDump("various-issues2.hprof");
		BatchProblemRecorder recorder = new BatchProblemRecorder();
		StandardStatsCalculator ssc = new StandardStatsCalculator(snapshot, recorder, useBreadthFirst);
		HeapStats hs = ssc.calculate();
		DetailedStats ds = recorder.getDetailedStats((int) hs.totalObjSize / 1000);

		Assert.assertEquals(4, hs.ptrSize);
		Assert.assertEquals(8, hs.objHeaderSize);

		ptrSize = hs.ptrSize;

		checkReferenceChains(hs, ds);

		checkStatsForBoxedNumbers(hs, ds);

		checkStatsForSparseCollections(hs, ds);
	}

	private void checkReferenceChains(HeapStats hs, DetailedStats ds) {
		MemNumFormatter nf = new MemNumFormatter(hs.totalObjSize);

		boolean found1 = false;
		boolean found2 = false;
		boolean found3 = false;
		boolean found4 = false;
		boolean found5 = false;
		boolean found6 = false;

		List<ReferencedObjCluster.Collections> colReverseChains = ds.collectionClusters.get(0);
		for (ReferencedObjCluster.Collections c : colReverseChains) {
			if (!c.clusterAsString(nf).contains("Vector: 1 of EMPTY")) {
				continue;
			}

			int ovhd = c.getTotalOverhead();
			String refChainStr = ReferenceChain.toStringInReverseOrder(c.getReferer(), 8);
			// Remove the number in the final "JavaLocal@12345", so that these strings
			// remain stable in different heap dumps
			String truncatedRefChain = refChainStr.substring(0, refChainStr.indexOf('@')).trim();

			// Here we rely not on exact numbers, but on the fact that the overhead of our
			// data is big enough, so that no JVM-created Vector can exceed it.
			if (ovhd > VariousIssues2.EMPTY_VECTOR1_SIZE * ptrSize) {
				found1 = true;
				Assert.assertEquals(
						"<--examples.VariousIssues2$CustomRef1.ref<--examples.VariousIssues2.chain1Head<<-Java Local",
						truncatedRefChain);
			} else if (ovhd > VariousIssues2.EMPTY_VECTOR2_SIZE * ptrSize) {
				found2 = true;
				Assert.assertEquals("<--examples.VariousIssues2$CustomRef2.ref<--examples.VariousIssues2$CustomRef1.ref"
						+ "<--examples.VariousIssues2.chain2Head<<-Java Local", truncatedRefChain);
			} else if (ovhd > VariousIssues2.EMPTY_VECTOR3_SIZE * ptrSize) {
				found3 = true;
				Assert.assertEquals("<--{ArrayList}<--examples.VariousIssues2$CustomRef1.ref"
						+ "<--examples.VariousIssues2.chain3Head<<-Java Local", truncatedRefChain);
			} else if (ovhd > VariousIssues2.EMPTY_VECTOR4_SIZE * ptrSize) {
				found4 = true;
				Assert.assertEquals(
						"<--examples.VariousIssues2$LLElementC.ref<--{examples.VariousIssues2$LLElementC.next}"
								+ "<--examples.VariousIssues2$CustomRef1.ref<--examples.VariousIssues2$CustomRef2.ref"
								+ "<--examples.VariousIssues2.chain4Head<<-Java Local",
						truncatedRefChain);
			} else if (ovhd > VariousIssues2.EMPTY_VECTOR5_SIZE * ptrSize) {
				found5 = true;
				// There is no single best way to determine the actual class for a linked list when its elements
				// are of interleaved hierarchically related classes. So we consider both variants ok for now.
				Assert.assertTrue(truncatedRefChain
						.equals("<--examples.VariousIssues2$LLElementC.ref<--{examples.VariousIssues2$LLElementP.next}"
								+ "<--examples.VariousIssues2$CustomRef2.ref<--examples.VariousIssues2.chain5Head"
								+ "<<-Java Local")
						|| truncatedRefChain.equals(
								"<--examples.VariousIssues2$LLElementC.ref<--{examples.VariousIssues2$LLElementC.next}"
										+ "<--examples.VariousIssues2$CustomRef2.ref<--examples.VariousIssues2.chain5Head"
										+ "<<-Java Local"));
			} else if (ovhd > VariousIssues2.EMPTY_VECTOR6_SIZE * ptrSize) {
				found6 = true;
				Assert.assertEquals(
						"<--examples.VariousIssues2$LLElementP.ref<--{examples.VariousIssues2$LLElementP.next}"
								+ "<--examples.VariousIssues2$CustomRef2.ref<--examples.VariousIssues2.chain6Head"
								+ "<<-Java Local",
						truncatedRefChain);
			}
		}

		Assert.assertTrue(found1);
		Assert.assertTrue(found2);
		Assert.assertTrue(found3);
		Assert.assertTrue(found4);
		Assert.assertTrue(found5);
		Assert.assertTrue(found6);
	}

	private void checkStatsForBoxedNumbers(HeapStats hs, DetailedStats ds) {
		boolean found1 = false;
		boolean found2 = false;
		boolean found3 = false;

		// Below -2 is sizeof(short), 16 is sizeof(Short)
		int expectedShortArrayOvhd = VariousIssues2.SHORT_ARRAY_SIZE * (ptrSize - 2)
				+ VariousIssues2.SHORT_ARRAY_UNIQUE_NUMS * 16;
		// Currently the overhead of boxed collections includes the collection's
		// implementation size, which may or may not be a good metrics.
		// It's hard to calculate here, so we skip this check for now
		// int expectedCharacterMapOvhd = map_implementation_size +
		// VariousIssues2.CHARACTER_MAP_SIZE *
		// (16 - 2 + ptrSize); // 16 is sizeof(Character), -2 is sizeof(char)

		// Check the Class.field -> cluster view
		List<ReferencedObjCluster.Collections> colFields = ds.collectionClusters.get(1);
		for (ReferencedObjCluster.Collections c : colFields) {
			List<ClassAndOvhdCombo> list = c.getList();
			for (ClassAndOvhdCombo clzAndOvhd : list) {
				if (clzAndOvhd.getProblemKind() == ProblemKind.BOXED) {
					String classAndField = ReferenceChain.toStringInStraightOrder(c.getReferer());
					if (classAndField.contains("arrayOfUniqueIntegers")) {
						found1 = true;
						// Below 16 is sizeof(Integer), -4 is sizeof(int)
						int expectedOvhd = VariousIssues2.INTEGER_ARRAY_NONNULL_ELEMENTS * (16 - 4 + ptrSize);
						Assert.assertEquals(expectedOvhd, c.getTotalOverhead());
					} else if (classAndField.contains("arrayOfShorts")) {
						found2 = true;
						Assert.assertEquals(expectedShortArrayOvhd, c.getTotalOverhead());
					} else if (classAndField.contains("characterMap")) {
						found3 = true;
						// Assert.assertEquals(expectedCharacterMapOvhd, c.getTotalOverhead());
					}
				}
			}
		}

		Assert.assertTrue(found1);
		Assert.assertTrue(found2);
		Assert.assertTrue(found3);

		found1 = false;

		// Check the "problematic collections by type" view
		for (CollectionClassDescriptor cd : hs.overheadsByClass) {
			String className = cd.getClassName();
			if (className.equals("[Ljava.lang.Short;")) {
				found1 = true;
				Assert.assertEquals(1, cd.getNumProblematicCollections(ProblemKind.BOXED));
				Assert.assertEquals(expectedShortArrayOvhd, cd.getProblematicCollectionsOverhead(ProblemKind.BOXED));
			}
		}

		Assert.assertTrue(found1);
	}

	private void checkStatsForSparseCollections(HeapStats hs, DetailedStats ds) {
		boolean found1 = false;
		boolean found2 = false;
		boolean found3 = false;
		boolean found4 = false;

		// Check the Class.field -> cluster view
		List<ReferencedObjCluster.Collections> colFields = ds.collectionClusters.get(1);
		for (ReferencedObjCluster.Collections c : colFields) {
			List<ClassAndOvhdCombo> list = c.getList();
			ClassAndOvhdCombo clzAndOvhd = list.get(0);
			String classAndField = ReferenceChain.toStringInStraightOrder(c.getReferer());
			if (classAndField.contains("smallSparseHashMaps")) {
				// These collections are both SMALL and SPARSE_SMALL. The overhead of small is higher
				found1 = true;
				Assert.assertEquals(ProblemKind.SMALL, clzAndOvhd.getProblemKind());
				Assert.assertEquals(VariousIssues2.NUM_OF_SMALL_SPARSE_HASHMAPS, clzAndOvhd.getNumInstances());
				Assert.assertEquals("HashMap", clzAndOvhd.getClazz().getHumanFriendlyName());
				clzAndOvhd = list.get(1);
				Assert.assertEquals(ProblemKind.SPARSE_SMALL, clzAndOvhd.getProblemKind());
				Assert.assertEquals(VariousIssues2.NUM_OF_SMALL_SPARSE_HASHMAPS, clzAndOvhd.getNumInstances());
				Assert.assertEquals("HashMap", clzAndOvhd.getClazz().getHumanFriendlyName());
				// 15 below is 16 (default HashMap capacity) - 1
				Assert.assertEquals((long) VariousIssues2.NUM_OF_SMALL_SPARSE_HASHMAPS * 15 * ptrSize,
						clzAndOvhd.getOverhead());
			} else if (classAndField.contains("largeSparseHashMaps")) {
				found2 = true;
				Assert.assertEquals(ProblemKind.SPARSE_LARGE, clzAndOvhd.getProblemKind());
				Assert.assertEquals(VariousIssues2.NUM_OF_LARGE_SPARSE_HASHMAPS, clzAndOvhd.getNumInstances());
				Assert.assertEquals("HashMap", clzAndOvhd.getClazz().getHumanFriendlyName());
				// In this case the exact overhead may be different, because some slots may be
				// empty, because some collision chains may be longer than 1 element.
				int emptySlots = VariousIssues2.LARGE_MAP_EXPLICIT_CAPACITY - VariousIssues2.NUM_ELS_IN_MAP;
				int expectedOvhd = VariousIssues2.NUM_OF_LARGE_SPARSE_HASHMAPS * emptySlots * ptrSize;
				Assert.assertTrue(((double) Math.abs(expectedOvhd - clzAndOvhd.getOverhead())) / expectedOvhd < 0.1);
			} else if (classAndField.contains("smallSparseArrayLists") && classAndField.contains("Object[]")) {
				found3 = true;
				// These collections are both SMALL and SPARSE_SMALL. The overhead of SMALL is higher
				Assert.assertEquals(ProblemKind.SMALL, clzAndOvhd.getProblemKind());
				Assert.assertEquals(VariousIssues2.NUM_OF_SMALL_SPARSE_ARRAYLISTS, clzAndOvhd.getNumInstances());
				Assert.assertEquals("ArrayList", clzAndOvhd.getClazz().getHumanFriendlyName());
				clzAndOvhd = list.get(1);
				Assert.assertEquals(ProblemKind.SPARSE_SMALL, clzAndOvhd.getProblemKind());
				Assert.assertEquals(VariousIssues2.NUM_OF_SMALL_SPARSE_ARRAYLISTS, clzAndOvhd.getNumInstances());
				Assert.assertEquals("ArrayList", clzAndOvhd.getClazz().getHumanFriendlyName());
				// 9 below is 10 (default ArrayListCapacity) - 1
				Assert.assertEquals((long) VariousIssues2.NUM_OF_SMALL_SPARSE_ARRAYLISTS * 9 * ptrSize,
						clzAndOvhd.getOverhead());
			} else if (classAndField.contains("largeSparseArrayLists") && classAndField.contains("Object[]")) {
				found4 = true;
				Assert.assertEquals(ProblemKind.SPARSE_LARGE, clzAndOvhd.getProblemKind());
				Assert.assertEquals(VariousIssues2.NUM_OF_LARGE_SPARSE_ARRAYLISTS, clzAndOvhd.getNumInstances());
				Assert.assertEquals("ArrayList", clzAndOvhd.getClazz().getHumanFriendlyName());
				int nEmptySlots = 11; // First 16 elements in each list (10 * 3 / 2 + 1), then we remove al but first 5
				Assert.assertEquals((long) VariousIssues2.NUM_OF_LARGE_SPARSE_ARRAYLISTS * nEmptySlots * ptrSize,
						clzAndOvhd.getOverhead());
			}
		}

		Assert.assertTrue(found1);
		Assert.assertTrue(found2);
		Assert.assertTrue(found3);
		Assert.assertTrue(found4);
	}
}
