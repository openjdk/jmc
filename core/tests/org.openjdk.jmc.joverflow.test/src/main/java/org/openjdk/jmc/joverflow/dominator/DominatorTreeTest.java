/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.joverflow.dominator;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Root;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.HeapDumpReader;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.util.VerboseOutputCollector;

import com.sun.management.HotSpotDiagnosticMXBean;

/**
 * Checks dominators and retained sizes against known objects in a live heap dump.
 */
public class DominatorTreeTest {
	// Distinctive lengths identify test arrays in the dump.
	private static final int EXCLUSIVE_ARRAY_LENGTH = (1 << 20) + 7;
	private static final int SHARED_ARRAY_LENGTH = 12347;
	private static final int SOFT_ARRAY_LENGTH = 65543;
	private static final int READ_BUFFER_SIZE = 64 * 1024 * 1024;

	static final class Holder {
		final byte[] exclusive = new byte[EXCLUSIVE_ARRAY_LENGTH];
		final int[] shared;

		Holder(int[] shared) {
			this.shared = shared;
		}
	}

	// Only static references; the test class uses the app loader.
	private static Holder sHolder;
	private static int[] sShared;
	private static SoftReference<byte[]> sSoftlyReachable;

	private static File dumpFile;
	private static Snapshot snapshot;

	private static final class Nodes {
		int testClass = DominatorTree.NO_NODE;
		int holderClass = DominatorTree.NO_NODE;
		int holder = DominatorTree.NO_NODE;
		int exclusiveArray = DominatorTree.NO_NODE;
		int sharedArray = DominatorTree.NO_NODE;
		int softArray = DominatorTree.NO_NODE;
		int numHolders;
	}

	@BeforeClass
	public static void dumpHeap() throws Exception {
		setUpStatics();
		dumpFile = File.createTempFile("joverflow-dominator-test", ".hprof");
		Assert.assertTrue(dumpFile.delete()); // dumpHeap() refuses to overwrite an existing file
		HotSpotDiagnosticMXBean bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
		bean.dumpHeap(dumpFile.getAbsolutePath(), true);

		ReadBuffer.Factory factory = new ReadBuffer.CachedReadBufferFactory(dumpFile.getAbsolutePath(),
				READ_BUFFER_SIZE);
		HeapDumpReader reader = HeapDumpReader.createReader(factory, 0, new VerboseOutputCollector());
		snapshot = reader.read();
	}

	/**
	 * A separate frame avoids retaining test objects as stack roots in the dump.
	 */
	private static void setUpStatics() {
		sShared = new int[SHARED_ARRAY_LENGTH];
		sHolder = new Holder(sShared);
		sSoftlyReachable = new SoftReference<>(new byte[SOFT_ARRAY_LENGTH]);
	}

	@AfterClass
	public static void cleanUp() {
		if (snapshot != null) {
			snapshot.discard();
		}
		if (dumpFile != null) {
			dumpFile.delete();
		}
		sHolder = null;
		sShared = null;
		sSoftlyReachable = null;
	}

	@Test
	public void testTreeConsistency() throws Exception {
		DominatorTree tree = build(false);
		int n = tree.getNodeCount();
		int numObjects = snapshot.getObjects().size() + snapshot.getNumClasses();
		Assert.assertEquals(numObjects, tree.getObjectCount());
		Assert.assertEquals(numObjects + (tree.getUnreachableObjectsNode() == DominatorTree.NO_NODE ? 1 : 2), n);
		Assert.assertEquals(DominatorTree.NO_NODE, tree.getImmediateDominator(DominatorTree.ROOT_NODE));
		Assert.assertTrue(tree.isSyntheticNode(DominatorTree.ROOT_NODE));
		Assert.assertNull(tree.getObject(DominatorTree.ROOT_NODE));

		// Every object appears exactly once
		int keyOffset = snapshot.getNumClasses() - 1;
		BitSet seen = new BitSet();
		BitSet gcRoots = new BitSet();
		for (Root root : snapshot.getRoots()) {
			int globalIdx = snapshot.getGlobalObjectIndexForId(root.getId());
			if (globalIdx != Snapshot.NO_GLOBAL_OBJECT_INDEX) {
				gcRoots.set(globalIdx + keyOffset);
			}
		}
		long totalShallowSize = 0;
		for (int node = 1; node < n; node++) {
			int idom = tree.getImmediateDominator(node);
			Assert.assertTrue("Dominator must precede node " + node, idom >= 0 && idom < node);
			Assert.assertTrue(tree.dominates(DominatorTree.ROOT_NODE, node));
			Assert.assertTrue(tree.getRetainedSize(node) >= tree.getShallowSize(node));
			if (tree.isSyntheticNode(node)) {
				Assert.assertEquals(tree.getUnreachableObjectsNode(), node);
				Assert.assertEquals(0, tree.getShallowSize(node));
				continue;
			}
			int globalIdx = tree.getGlobalObjectIndex(node);
			Assert.assertFalse("Object listed twice: " + globalIdx, seen.get(globalIdx + keyOffset));
			seen.set(globalIdx + keyOffset);
			if (gcRoots.get(globalIdx + keyOffset)) {
				Assert.assertEquals("GC root must be dominated only by the synthetic root: " + globalIdx,
						DominatorTree.ROOT_NODE, idom);
			}
			JavaHeapObject obj = tree.getObject(node);
			Assert.assertEquals(globalIdx, obj.getGlobalObjectIndex());
			Assert.assertEquals(obj.getSize(), tree.getShallowSize(node));
			totalShallowSize += obj.getSize();
		}
		Assert.assertEquals(numObjects, seen.cardinality());
		Assert.assertEquals(totalShallowSize, tree.getTotalSize());

		// Children are consistent with immediate dominators, and sorted by retained size
		long numChildren = 0;
		for (int node = 0; node < n; node++) {
			long childrenRetained = 0;
			for (int i = 0; i < tree.getChildCount(node); i++) {
				int child = tree.getChild(node, i);
				Assert.assertEquals(node, tree.getImmediateDominator(child));
				childrenRetained += tree.getRetainedSize(child);
				if (i > 0) {
					Assert.assertTrue(tree.getRetainedSize(tree.getChild(node, i - 1)) >= tree.getRetainedSize(child));
				}
				numChildren++;
			}
			Assert.assertEquals(tree.getRetainedSize(node) - tree.getShallowSize(node), childrenRetained);
		}
		Assert.assertEquals(n - 1, numChildren);

		int unreachable = tree.getUnreachableObjectsNode();
		if (unreachable != DominatorTree.NO_NODE) {
			Assert.assertEquals(DominatorTree.ROOT_NODE, tree.getImmediateDominator(unreachable));
			Assert.assertEquals(tree.getRetainedSize(unreachable), tree.getUnreachableSize());
			for (int node = 1; node < n; node++) {
				boolean underUnreachable = node == unreachable || tree.dominates(unreachable, node);
				Assert.assertEquals(!underUnreachable, tree.isReachableFromGcRoots(node));
			}
		} else {
			Assert.assertEquals(0, tree.getUnreachableSize());
		}
	}

	@Test
	public void testGcRootDiscoveredThroughAnotherRoot() throws Exception {
		JavaClass testClass = snapshot.getClassForName(DominatorTreeTest.class.getName());
		JavaObject holder = (JavaObject) testClass.getStaticField("sHolder");
		JavaHeapObject exclusive = (JavaHeapObject) holder.getField("exclusive");
		List<Root> roots = snapshot.getRoots();
		List<Root> originalRoots = new ArrayList<>(roots);
		try {
			// Visit the holder first, so its array is discovered before the array's own root.
			roots.clear();
			roots.add(new Root(holder.readId(), 0, Root.JNI_GLOBAL, ""));
			roots.add(new Root(exclusive.readId(), 0, Root.JNI_GLOBAL, ""));
			roots.addAll(originalRoots);
			DominatorTree tree = build(false);
			Nodes nodes = findNodes(tree);
			Assert.assertNotEquals(DominatorTree.NO_NODE, nodes.holder);
			Assert.assertNotEquals(DominatorTree.NO_NODE, nodes.exclusiveArray);
			Assert.assertEquals(DominatorTree.ROOT_NODE, tree.getImmediateDominator(nodes.holder));
			Assert.assertEquals(DominatorTree.ROOT_NODE, tree.getImmediateDominator(nodes.exclusiveArray));
			Assert.assertFalse(tree.dominates(nodes.holder, nodes.exclusiveArray));
			Assert.assertEquals(tree.getShallowSize(nodes.holder), tree.getRetainedSize(nodes.holder));
		} finally {
			roots.clear();
			roots.addAll(originalRoots);
		}
	}

	@Test
	public void testKnownObjects() throws Exception {
		DominatorTree tree = build(false);
		Nodes nodes = findNodes(tree);
		Assert.assertNotEquals(DominatorTree.NO_NODE, nodes.testClass);
		Assert.assertNotEquals(DominatorTree.NO_NODE, nodes.holderClass);
		Assert.assertEquals(1, nodes.numHolders);
		Assert.assertNotEquals(DominatorTree.NO_NODE, nodes.exclusiveArray);
		Assert.assertNotEquals(DominatorTree.NO_NODE, nodes.sharedArray);

		Assert.assertTrue(tree.isReachableFromGcRoots(nodes.testClass));
		Assert.assertTrue(tree.isReachableFromGcRoots(nodes.holder));

		// The holder is referenced only from a static field of the test class
		Assert.assertEquals(nodes.testClass, tree.getImmediateDominator(nodes.holder));
		// Only the holder references the exclusive array.
		Assert.assertEquals(nodes.holder, tree.getImmediateDominator(nodes.exclusiveArray));
		Assert.assertEquals(1, tree.getChildCount(nodes.holder));
		Assert.assertEquals(nodes.exclusiveArray, tree.getChild(nodes.holder, 0));
		Assert.assertEquals(tree.getShallowSize(nodes.holder) + tree.getShallowSize(nodes.exclusiveArray),
				tree.getRetainedSize(nodes.holder));
		Assert.assertTrue(tree.getShallowSize(nodes.exclusiveArray) >= EXCLUSIVE_ARRAY_LENGTH);
		// The shared array is referenced both from the holder and from a static field
		Assert.assertEquals(nodes.testClass, tree.getImmediateDominator(nodes.sharedArray));
		Assert.assertTrue(tree.dominates(nodes.testClass, nodes.exclusiveArray));
		Assert.assertFalse(tree.dominates(nodes.holderClass, nodes.holder));

		// The GC may have cleared the softly reachable array before the dump.
		if (nodes.softArray != DominatorTree.NO_NODE) {
			Assert.assertFalse(tree.isReachableFromGcRoots(nodes.softArray));
		}
	}

	@Test
	public void testFollowingWeakReferences() throws Exception {
		DominatorTree tree = build(true);
		Nodes nodes = findNodes(tree);
		if (nodes.softArray != DominatorTree.NO_NODE) {
			Assert.assertTrue(tree.isReachableFromGcRoots(nodes.softArray));
			JavaHeapObject softReference = tree.getObject(tree.getImmediateDominator(nodes.softArray));
			Assert.assertEquals(SoftReference.class.getName(), softReference.getClazz().getName());
		}
		Assert.assertEquals(nodes.testClass, tree.getImmediateDominator(nodes.holder));
		Assert.assertEquals(nodes.holder, tree.getImmediateDominator(nodes.exclusiveArray));
	}

	@Test
	public void testCancellation() throws Exception {
		DominatorTreeBuilder builder = createBuilder();
		builder.cancel();
		try {
			builder.build();
			Assert.fail("Expected the build to be cancelled");
		} catch (HprofParsingCancelledException e) {
			// Expected
		}
	}

	@Test
	public void testProgressReporting() throws Exception {
		DominatorTreeBuilder builder = createBuilder();
		Assert.assertEquals(DominatorTreeBuilder.Phase.SCANNING_HEAP, builder.getPhase());
		Assert.assertEquals(0, builder.getProgressPercentage());
		builder.build();
		Assert.assertEquals(DominatorTreeBuilder.Phase.DONE, builder.getPhase());
		Assert.assertEquals(100, builder.getProgressPercentage());
		try {
			builder.build();
			Assert.fail("Expected build() to be usable only once");
		} catch (IllegalStateException e) {
			// Expected
		}
	}

	private static DominatorTreeBuilder createBuilder() {
		ReadBuffer.Factory factory = new ReadBuffer.CachedReadBufferFactory(dumpFile.getAbsolutePath(),
				READ_BUFFER_SIZE);
		return new DominatorTreeBuilder(snapshot, factory);
	}

	private static DominatorTree build(boolean followWeakReferences) throws Exception {
		DominatorTreeBuilder builder = createBuilder();
		builder.setFollowWeakReferences(followWeakReferences);
		return builder.build();
	}

	private static Nodes findNodes(DominatorTree tree) {
		JavaClass testClass = snapshot.getClassForName(DominatorTreeTest.class.getName());
		JavaClass holderClass = snapshot.getClassForName(Holder.class.getName());
		Assert.assertNotNull(testClass);
		Assert.assertNotNull(holderClass);
		Nodes nodes = new Nodes();
		for (int node = 1; node < tree.getNodeCount(); node++) {
			if (tree.isSyntheticNode(node)) {
				continue;
			}
			JavaHeapObject obj = tree.getObject(node);
			if (obj == testClass) {
				nodes.testClass = node;
			} else if (obj == holderClass) {
				nodes.holderClass = node;
			} else if (obj instanceof JavaObject && obj.getClazz() == holderClass) {
				nodes.holder = node;
				nodes.numHolders++;
			} else if (obj instanceof JavaValueArray) {
				JavaValueArray array = (JavaValueArray) obj;
				if (array.getClazz().isByteArray() && array.getLength() == EXCLUSIVE_ARRAY_LENGTH) {
					nodes.exclusiveArray = node;
				} else if (array.getClazz().isByteArray() && array.getLength() == SOFT_ARRAY_LENGTH) {
					nodes.softArray = node;
				} else if (array.getElementType() == 'I' && array.getLength() == SHARED_ARRAY_LENGTH) {
					nodes.sharedArray = node;
				}
			}
		}
		return nodes;
	}
}
