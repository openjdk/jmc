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

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jmc.joverflow.descriptors.CollectionInstanceDescriptor;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.support.ProblemRecorder;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.util.ObjectToIntMap;

/**
 * This test verifies some basic invariants in the data obtained by JOverflow. It should, in
 * principle, pass for any heap dump. However, there is probably a chance that if the dump is
 * non-lethally corrupted, as it sometimes happens, e.g. it has HashMap$Entry objects forming a
 * cycle, some of the checks in the test may fail. Also heap dumps containing multiple versions for
 * some classes may have a problem, because the final checks in testConsistency() have to operate on
 * string class names rather than JavaClass objects.
 * <p>
 * The most common problem that can be discovered in consistency checks is that the total number of
 * instances and/or total size reported via JavaClass and via ProblemRecorder are different. For
 * most classes this shouldn't happen, but for collection impl-n classes this is always the case.
 * Actually, objects that are exclusively collection impl, such as HashMap$Entry, should not get
 * into ProblemRecorder at all, i.e. for them total size from recorder should always be 0. Others,
 * like Object[] or char[] arrays, may be either standalone or a part of collection (or String for
 * char[]) implementation.
 * <p>
 * With collection impl-n objects, delicate problems may occur that are related to the accuracy of
 * marking objects with JavaLazyReadObject.setVisitedAsCollectionImpl(). This is done when a
 * collection is first inspected in DetailedStatsCalculator.handleCollection(). For objects marked
 * as collection impl-n, size is not recorded in DetailedStatsCalculator.handleInstance() - hence
 * the above difference in total size. However, if for some reason an impl-n object is not marked as
 * such, or marked only *after* it got handled by handleInstance(), stats will go wrong for its
 * class.
 * <p>
 * One recent case like that was observed with WeakHashMap$Entry objects. It eventually turned out
 * that WHM$Entry has two 'next' fields - one its own and another one inherited from its superclass,
 * java.lang.ref.Reference. It turned out that depth-first scanner followed the superclass 'next'
 * field eagerly, and via it could get to some other WHM$Entry objects (belonging to other
 * WeakHashMaps!) before these objects were properly inspected and marked as collection impl. The
 * solution was to mark the superclass WHM$Entry.next field as "banned" in
 * CollectionDescriptors.setBannedFields(), so that it's never followed.
 */
@Ignore("Requires a generated hprof file. This test can be reenabled if the file is available.")
public class VerifyConsistency {

	private static final String SNAPSHOT_FILE_NAME = "various-issues3.hprof";

	private Snapshot snapshot;

	@Test
	public void testConsistency() throws Exception {
		ObjectToIntMap<String> classesWithDifferentNumInstDFS = runTest(false);
		ObjectToIntMap<String> classesWithDifferentNumInstBFS = runTest(true);

		// Verify that different scan methods produce same results
		Assert.assertEquals(classesWithDifferentNumInstDFS.size(), classesWithDifferentNumInstBFS.size());
		for (ObjectToIntMap.Entry<String> entry : classesWithDifferentNumInstDFS.getEntries()) {
			int numInstancesDFS = entry.value;
			Assert.assertEquals("Problem for class " + entry.key, numInstancesDFS,
					classesWithDifferentNumInstBFS.get(entry.key));
		}
	}

	/**
	 * Scans the heap dump, performs most of the consistency checks, and returns the table mapping a
	 * JavaClass to the number of instances of this class *as recorded by ProblemRecorder*, only for
	 * the classes where the number of instances recorded in this way is different from
	 * JavaClass.getNumInstances(). Classes where recorded num of instances is different from the
	 * real one are collection implementation classes, ranging from obvious (such as HashMap$Entry)
	 * to less obvious (e.g. Object[]), to a few quite non-obvious (HashMap and LinkedHashMap, which
	 * can be both a first-class collection class and an implementation class in HashSet and
	 * LinkedHashSet, respectively).
	 */
	private ObjectToIntMap<String> runTest(boolean useBreadthFirst) throws Exception {
		snapshot = SnapshotReader.readAndResolveHeapDump(SNAPSHOT_FILE_NAME);
		VerifyRecorder vr = new VerifyRecorder();
		StandardStatsCalculator ssc = new StandardStatsCalculator(snapshot, vr, useBreadthFirst);
		HeapStats hs = ssc.calculate();

		long totalObjSizeInclusive = 0, totalObjSizeShallow = 0;
		for (JavaClass c : snapshot.getClasses()) {
			int sizeFromRecorder = vr.classToObjSize.get(c);
			long sizeFromJavaClass = c.getTotalInclusiveInstanceSize();
			if (sizeFromRecorder == -1) {
				// -1 from ObjectToIntMap.get() means "not present", but for our purposes
				// 0 is what's appropriate
				sizeFromRecorder = 0;
			}
			if ((sizeFromRecorder == 0 && sizeFromJavaClass != 0)
					|| (sizeFromRecorder != 0 && sizeFromRecorder != sizeFromJavaClass)) {
				Assert.fail("Different inclusive size for " + c.getHumanFriendlyName() + ": " + " from recorder: "
						+ sizeFromRecorder + " | from JavaClass: " + sizeFromJavaClass + ", diff = "
						+ (sizeFromJavaClass - sizeFromRecorder));
			}

			totalObjSizeInclusive += sizeFromJavaClass;
			totalObjSizeShallow += c.getTotalShallowInstanceSize();
		}

		long totalObjSizeFromRecorder = 0;
		for (ObjectToIntMap.Entry<JavaClass> v : vr.classToObjSize.getEntries()) {
			totalObjSizeFromRecorder += v.value;
		}

		// Check consistency of total obj numbers obtained in different ways
		ObjectToIntMap<String> classesWithDifferentNumInst = new ObjectToIntMap<>(100);
		ObjectToIntMap.Entry<JavaClass> entries[] = vr.classToObjNum.getEntries();
		for (ObjectToIntMap.Entry<JavaClass> entry : entries) {
			JavaClass clazz = entry.key;
			String className = clazz.getName();
			int numInstancesFromJavaClass = clazz.getNumInstances();
			int numInstancesFromRecorder = entry.value;
			if (numInstancesFromJavaClass != numInstancesFromRecorder) {
				if (clazz.isArray() || className.contains("$") || className.equals("java.util.HashMap")
						|| className.equals("java.util.LinkedHashMap")) {
					if (className.endsWith("$Entry")) {
						// These should not be passed to the ProblemRecorder at all
						// (assuming all *$Entry classes are in implementation of
						// some known collection)
						Assert.assertEquals(className, 0, numInstancesFromRecorder);
					}
					classesWithDifferentNumInst.put(className, numInstancesFromRecorder);
				} else {
					Assert.fail("Different reported num of instances for " + clazz.getName() + ": from JavaClass = "
							+ numInstancesFromJavaClass + ", from recorder =" + numInstancesFromRecorder);
				}
			}
		}

		// Check consistency of total obj size obtained in different ways
		Assert.assertEquals(hs.totalObjSize, vr.totalObjSize);
		Assert.assertEquals(hs.totalObjSize, totalObjSizeShallow);
		Assert.assertEquals(hs.totalObjSize, totalObjSizeInclusive);
		Assert.assertEquals(hs.totalObjSize, totalObjSizeFromRecorder);

		// Check consistency for Strings
		JavaClass stringClass = snapshot.getClassForName("java.lang.String");
		JavaClass charArrayClass = snapshot.getClassForName("[C");
		JavaClass byteArrayClass = snapshot.getClassForName("[B");
		long strSh = stringClass.getTotalShallowInstanceSize();
		long strIncl = stringClass.getTotalInclusiveInstanceSize();
		long charSh = charArrayClass.getTotalShallowInstanceSize();
		long charIncl = charArrayClass.getTotalInclusiveInstanceSize();
		long byteSh = byteArrayClass.getTotalShallowInstanceSize();
		long byteIncl = byteArrayClass.getTotalInclusiveInstanceSize();

		long shallowSizeSum = strSh + charSh + byteSh;
		long inclusiveSizeSum = strIncl + charIncl + byteIncl;
		Assert.assertEquals(shallowSizeSum, inclusiveSizeSum);

		return classesWithDifferentNumInst;
	}

	private static class VerifyRecorder implements ProblemRecorder {

		private JavaHeapObject prevObj;

		private HashMap<JavaHeapObject, RefChainElement> objToReferer = new HashMap<>();
		private ObjectToIntMap<JavaClass> classToObjSize = new ObjectToIntMap<>(1000);
		private ObjectToIntMap<JavaClass> classToObjNum = new ObjectToIntMap<>(1000);
		private long totalObjSize;

		private void check(JavaHeapObject obj, RefChainElement referer) {
			check(obj, obj.getSize(), referer);
		}

		private void check(JavaHeapObject obj, int size, RefChainElement referer) {
			RefChainElement oldReferer = objToReferer.get(obj);
			if (oldReferer != null) {
				if (referer != oldReferer) {
					Assert.fail(obj.valueAsString() + " reached twice via different referrers: " + oldReferer + " and "
							+ referer);
				} else {
					if (obj == prevObj) {
						return; // Presumably recording a different problem for the same obj
					}
					Assert.fail(obj.valueAsString() + " reached twice via same referrer: " + referer);
				}
			} else {
				objToReferer.put(obj, referer);
				totalObjSize += size;
				classToObjSize.putOrIncrementBy(obj.getClazz(), size);
				classToObjNum.putOneOrIncrement(obj.getClazz());
				prevObj = obj;
			}
		}

		// Implementation of ProblemRecorder methods

		@Override
		public void initialize(Snapshot snapshot, HeapStats hs) {
		}

		@Override
		public void recordProblematicCollection(
			JavaLazyReadObject obj, CollectionInstanceDescriptor colDesc, Constants.ProblemKind ovhdKind, int ovhd,
			RefChainElement referer) {
			check(obj, colDesc.getImplSize(), referer);
		}

		@Override
		public void recordDuplicateString(
			JavaObject obj, String val, int implInclusiveSize, int ovhd, boolean hasDupCharArray,
			RefChainElement referer) {
			check(obj, implInclusiveSize, referer);
		}

		@Override
		public void recordDuplicateArray(JavaValueArray obj, int ovhd, RefChainElement referer) {
			check(obj, referer);
		}

		@Override
		public void recordWeakHashMapWithBackRefs(
			JavaObject obj, CollectionInstanceDescriptor colDesc, int ovhd, String valueTypeAndFieldSample,
			RefChainElement referer) {
			check(obj, referer);
		}

		@Override
		public void recordGoodCollection(
			JavaLazyReadObject obj, CollectionInstanceDescriptor colDesc, RefChainElement referer) {
			check(obj, colDesc.getImplSize(), referer);
		}

		@Override
		public void recordNonDuplicateString(JavaObject obj, int implInclusiveSize, RefChainElement referer) {
			check(obj, implInclusiveSize, referer);
		}

		@Override
		public void recordNonDuplicateArray(JavaValueArray obj, RefChainElement referer) {
			check(obj, referer);
		}

		@Override
		public boolean shouldRecordGoodInstance(JavaObject obj) {
			return true;
		}

		@Override
		public void recordGoodInstance(JavaObject obj, RefChainElement referer) {
			check(obj, referer);
		}
	}
}
