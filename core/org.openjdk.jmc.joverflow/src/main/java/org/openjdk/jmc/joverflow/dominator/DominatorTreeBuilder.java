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

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.Arrays;

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaField;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.Root;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.util.ChunkedIntArray;

/**
 * Builds a {@link Snapshot}'s {@link DominatorTree} using Lengauer-Tarjan. Progress getters and
 * {@link #cancel()} may be called from any thread.
 * <p>
 * Follows instance fields, array elements, static fields, and each class's superclass, class
 * loader, signers and protection domain. The {@code java.lang.ref.Reference.referent} field is
 * excluded by default; see {@link #setFollowWeakReferences(boolean)}. Objects reachable only
 * through weak or soft references are therefore reported as unreachable.
 * <p>
 * Unreachable objects are grouped under {@link DominatorTree#getUnreachableObjectsNode()}. Their
 * references to live objects are ignored when computing live retained sizes.
 * <p>
 * A supplied factory isolates reads in a separate {@link ReadBuffer}, allowing concurrent snapshot
 * reads. Heap-object visited tags are unchanged.
 * <p>
 * Memory usage is roughly 60 bytes per object plus 8 bytes per reference at peak, and about 36
 * bytes per object for the finished tree.
 */
public class DominatorTreeBuilder {

	public enum Phase {
		SCANNING_HEAP("Scanning heap", 55),
		BUILDING_GRAPH("Building reference graph", 10),
		COMPUTING_DOMINATORS("Computing dominators", 30),
		COMPUTING_RETAINED_SIZES("Computing retained sizes", 5),
		DONE("Done", 0);

		private final String description;
		private final int weight;

		Phase(String description, int weight) {
			this.description = description;
			this.weight = weight;
		}

		/**
		 * English phase label.
		 */
		public String getDescription() {
			return description;
		}
	}

	private static final int NONE = Snapshot.NO_GLOBAL_OBJECT_INDEX;
	private static final int CANCEL_CHECK_INTERVAL_MASK = (1 << 12) - 1;
	private static final int PROGRESS_INTERVAL_MASK = (1 << 16) - 1;
	private static final int ARRAY_READ_CHUNK_ELEMENTS = 1 << 16;
	private static final int INITIAL_STACK_SIZE = 1024;

	private final Snapshot snapshot;
	private final ReadBuffer.Factory readBufferFactory;
	private boolean followWeakReferences;
	private boolean built;

	private volatile boolean cancelled;
	private volatile Phase phase = Phase.SCANNING_HEAP;
	private volatile int phasePercentage;

	private int idSize;
	private JavaClass referenceClass;
	private int referentFieldIdx = -1;

	private ReadBuffer readBuf;
	private int keyOffset;
	private int[] globalToNode;
	private int[] nodeToGlobal;
	private int[] parent;
	private int[] shallow;
	private int[] succStart;
	private ChunkedIntArray succ;
	private int numNodes;
	private int unreachableNode = DominatorTree.NO_NODE;
	private int[] stackNode = new int[INITIAL_STACK_SIZE];
	private int[] stackCursor = new int[INITIAL_STACK_SIZE];
	private int stackSize;
	private byte[] bytes = new byte[256];
	private int currentGlobalIdx;
	private int numProcessed;
	private int numTotal;

	/**
	 * @param snapshot
	 *            the snapshot to build the dominator tree for
	 * @param readBufferFactory
	 *            factory for the same heap dump, supporting
	 *            {@link ReadBuffer.Factory#create(Object)} with null. If null, uses the snapshot's
	 *            buffer and requires exclusive snapshot access until the build finishes.
	 */
	public DominatorTreeBuilder(Snapshot snapshot, ReadBuffer.Factory readBufferFactory) {
		this.snapshot = snapshot;
		this.readBufferFactory = readBufferFactory;
	}

	/**
	 * Whether to follow {@code java.lang.ref.Reference.referent}. Defaults to false.
	 */
	public void setFollowWeakReferences(boolean followWeakReferences) {
		this.followWeakReferences = followWeakReferences;
	}

	public void cancel() {
		cancelled = true;
	}

	public Phase getPhase() {
		return phase;
	}

	public int getPhaseProgressPercentage() {
		return phasePercentage;
	}

	/**
	 * Estimated overall progress, in percent.
	 */
	public int getProgressPercentage() {
		Phase currentPhase = phase;
		int result = 0;
		for (Phase p : Phase.values()) {
			if (p == currentPhase) {
				result += p.weight * phasePercentage / 100;
				break;
			}
			result += p.weight;
		}
		return Math.min(100, result);
	}

	/**
	 * Builds the dominator tree. This method may only be called once.
	 *
	 * @throws IOException
	 *             if the heap dump could not be read
	 * @throws HprofParsingCancelledException
	 *             if {@link #cancel()} was called before the computation finished
	 */
	public DominatorTree build() throws IOException, HprofParsingCancelledException {
		if (built) {
			throw new IllegalStateException("build() can only be called once");
		}
		built = true;
		try {
			scanHeap();
			int n = numNodes;
			int[] predStart = new int[n + 1];
			int[] preds = buildPredecessors(predStart);
			int[] parent = this.parent;
			this.parent = null;

			setPhase(Phase.COMPUTING_DOMINATORS);
			int[] idom = DominatorCalculator.computeImmediateDominators(n, parent, predStart, preds, (done, total) -> {
				checkCancelled();
				phasePercentage = (int) (100L * done / total);
			});

			setPhase(Phase.COMPUTING_RETAINED_SIZES);
			long[] retained = computeRetainedSizes(idom);
			int[] childStart = new int[n + 1];
			int[] children = buildChildren(idom, retained, childStart);
			int[] nodeToGlobal = Arrays.copyOf(this.nodeToGlobal, n);
			int[] shallow = Arrays.copyOf(this.shallow, n);
			setPhase(Phase.DONE);
			return new DominatorTree(snapshot, nodeToGlobal, idom, shallow, retained, childStart, children,
					unreachableNode);
		} finally {
			releaseScanState();
		}
	}

	private void scanHeap() throws IOException, HprofParsingCancelledException {
		setPhase(Phase.SCANNING_HEAP);
		idSize = snapshot.getHprofPointerSize();
		initReferentField();

		int numClasses = snapshot.getNumClasses();
		int numObjects = snapshot.getObjects().size();
		numTotal = numObjects + numClasses;
		// Global indices are <= 0 for classes and > 0 for other objects; map them to a dense key
		keyOffset = numClasses - 1;
		globalToNode = new int[snapshot.getGlobalObjectIndexBound() + numClasses];
		int capacity = numTotal + 2; // All objects plus the two synthetic nodes
		nodeToGlobal = new int[capacity];
		parent = new int[capacity];
		shallow = new int[capacity];
		succStart = new int[capacity + 1];
		succ = new ChunkedIntArray();

		numNodes = 1;
		nodeToGlobal[DominatorTree.ROOT_NODE] = NONE;
		parent[DominatorTree.ROOT_NODE] = DominatorTree.NO_NODE;

		readBuf = readBufferFactory != null ? readBufferFactory.create(null) : snapshot.getReadBuffer();
		try {
			// Record every GC-root edge, including roots first reached through another root.
			int numRootsChecked = 0;
			for (Root root : snapshot.getRoots()) {
				if ((numRootsChecked++ & CANCEL_CHECK_INTERVAL_MASK) == 0) {
					checkCancelled();
				}
				int globalIdx = snapshot.getGlobalObjectIndexForId(root.getId());
				if (globalIdx != NONE) {
					succ.add(globalIdx);
				}
			}
			int numRootEdges = succ.size();
			succStart[DominatorTree.ROOT_NODE + 1] = numRootEdges;
			for (int i = 0; i < numRootEdges; i++) {
				if ((i & CANCEL_CHECK_INTERVAL_MASK) == 0) {
					checkCancelled();
				}
				int globalIdx = succ.get(i);
				if (!isVisited(globalIdx)) {
					dfs(globalIdx, DominatorTree.ROOT_NODE);
				}
			}
			scanUnreachableObjects();
		} finally {
			if (readBufferFactory != null) {
				readBuf.close();
			}
			readBuf = null;
		}
		phasePercentage = 100;
	}

	private void initReferentField() {
		referenceClass = snapshot.getWeakReferenceClass();
		if (referenceClass == null) {
			return;
		}
		JavaField[] fields = referenceClass.getDefinedFields();
		for (int i = 0; i < fields.length; i++) {
			if ("referent".equals(fields[i].getName())) {
				referentFieldIdx = i;
				return;
			}
		}
	}

	/**
	 * Scans unreachable objects under a synthetic node, classes and collections first so their
	 * implementation objects remain beneath them.
	 */
	private void scanUnreachableObjects() throws IOException, HprofParsingCancelledException {
		ChunkedIntArray[] pseudoRoots = new ChunkedIntArray[4];
		for (int i = 0; i < pseudoRoots.length; i++) {
			pseudoRoots[i] = new ChunkedIntArray();
		}
		for (JavaClass clazz : snapshot.getClasses()) {
			int globalIdx = clazz.getGlobalObjectIndex();
			if (!isVisited(globalIdx)) {
				pseudoRoots[0].add(globalIdx);
			}
		}
		int numChecked = 0;
		for (JavaLazyReadObject obj : snapshot.getObjects()) {
			if ((++numChecked & PROGRESS_INTERVAL_MASK) == 0) {
				checkCancelled();
			}
			int globalIdx = obj.getGlobalObjectIndex();
			if (isVisited(globalIdx)) {
				continue;
			}
			JavaClass clazz = obj.getClazz();
			if (clazz.isCollectionWithOtherCollectionInImpl()) {
				pseudoRoots[1].add(globalIdx);
			} else if (clazz.isCollection() || clazz.isString()) {
				pseudoRoots[2].add(globalIdx);
			} else {
				pseudoRoots[3].add(globalIdx);
			}
		}

		boolean anyUnreachable = false;
		for (ChunkedIntArray list : pseudoRoots) {
			anyUnreachable |= !list.isEmpty();
		}
		if (!anyUnreachable) {
			return;
		}

		unreachableNode = numNodes++;
		nodeToGlobal[unreachableNode] = NONE;
		parent[unreachableNode] = DominatorTree.ROOT_NODE;
		shallow[unreachableNode] = 0;
		succStart[unreachableNode + 1] = succ.size();

		for (ChunkedIntArray list : pseudoRoots) {
			for (int i = 0, size = list.size(); i < size; i++) {
				int globalIdx = list.get(i);
				if (!isVisited(globalIdx)) {
					dfs(globalIdx, unreachableNode);
				}
			}
			list.release();
		}
	}

	private boolean isVisited(int globalIdx) {
		return globalToNode[globalIdx + keyOffset] != 0;
	}

	/**
	 * Iterative DFS avoids stack overflow on long reference chains.
	 */
	private void dfs(int startGlobalIdx, int parentNode) throws HprofParsingCancelledException {
		visit(startGlobalIdx, parentNode);
		while (stackSize > 0) {
			int top = stackSize - 1;
			int node = stackNode[top];
			int cursor = stackCursor[top];
			if (cursor < succStart[node + 1]) {
				stackCursor[top] = cursor + 1;
				int globalIdx = succ.get(cursor);
				if (!isVisited(globalIdx)) {
					visit(globalIdx, node);
				}
			} else {
				stackSize--;
			}
		}
	}

	private void visit(int globalIdx, int parentNode) throws HprofParsingCancelledException {
		JavaHeapObject obj = snapshot.getObjectAtGlobalIndex(globalIdx);
		int node = numNodes++;
		globalToNode[globalIdx + keyOffset] = node;
		nodeToGlobal[node] = globalIdx;
		parent[node] = parentNode;
		shallow[node] = obj.getSize();

		currentGlobalIdx = globalIdx;
		if (obj instanceof JavaObject) {
			readInstanceReferences((JavaObject) obj);
		} else if (obj instanceof JavaObjectArray) {
			readObjectArrayReferences((JavaObjectArray) obj);
		} else if (obj instanceof JavaClass) {
			readClassReferences((JavaClass) obj);
		}
		succStart[node + 1] = succ.size();

		if (stackSize == stackNode.length) {
			stackNode = Arrays.copyOf(stackNode, stackSize * 2);
			stackCursor = Arrays.copyOf(stackCursor, stackSize * 2);
		}
		stackNode[stackSize] = node;
		stackCursor[stackSize] = succStart[node];
		stackSize++;

		if ((++numProcessed & CANCEL_CHECK_INTERVAL_MASK) == 0) {
			checkCancelled();
			phasePercentage = (int) (100L * numProcessed / numTotal);
		}
	}

	/**
	 * HPROF stores fields from the concrete class through its superclasses; see
	 * JavaObject.parseFields().
	 */
	private void readInstanceReferences(JavaObject obj) {
		JavaClass clazz = obj.getClazz();
		if (!clazz.hasReferenceFields()) {
			return;
		}
		int len = clazz.getFieldsSizeInFile();
		if (len <= 0 || !read(obj.getObjOfsInFile() + 2L * idSize + 8, len)) {
			return;
		}
		int index = 0;
		for (JavaClass c = clazz; c != null; c = c.getSuperclass()) {
			JavaField[] fields = c.getDefinedFields();
			for (int i = 0; i < fields.length; i++) {
				char typeId = fields[i].getTypeId();
				if (typeId == 'L' || typeId == '[') {
					if (index + idSize > len) {
						return; // Inconsistent class description, e.g. a synthesized class
					}
					if (followWeakReferences || c != referenceClass || i != referentFieldIdx) {
						addReference(idAt(index));
					}
					index += idSize;
				} else {
					int size = primitiveSize(typeId);
					if (size < 0) {
						return;
					}
					index += size;
				}
			}
		}
	}

	private void readObjectArrayReferences(JavaObjectArray array) throws HprofParsingCancelledException {
		int length = array.getLength();
		if (length <= 0) {
			return;
		}
		long ofs = array.getObjOfsInFile() + 2L * idSize + 8;
		int chunkElements = Math.min(length, ARRAY_READ_CHUNK_ELEMENTS);
		for (int done = 0; done < length;) {
			checkCancelled();
			int count = Math.min(chunkElements, length - done);
			if (!read(ofs + (long) done * idSize, count * idSize)) {
				return;
			}
			for (int i = 0, index = 0; i < count; i++, index += idSize) {
				addReference(idAt(index));
			}
			done += count;
		}
	}

	private void readClassReferences(JavaClass clazz) {
		JavaClass superclass = clazz.getSuperclass();
		if (superclass != null) {
			addReferenceToObject(superclass);
		}
		addReferenceToThing(clazz.getLoader());
		addReferenceToThing(clazz.getSigners());
		addReferenceToThing(clazz.getProtectionDomain());
		for (JavaThing value : clazz.getStaticValues()) {
			addReferenceToThing(value);
		}
	}

	private void addReferenceToThing(JavaThing thing) {
		if (thing instanceof JavaHeapObject) {
			addReferenceToObject((JavaHeapObject) thing);
		}
	}

	private void addReferenceToObject(JavaHeapObject obj) {
		addReferenceToGlobalIndex(obj.getGlobalObjectIndex());
	}

	private void addReference(long objId) {
		int globalIdx = snapshot.getGlobalObjectIndexForId(objId);
		if (globalIdx != NONE) {
			addReferenceToGlobalIndex(globalIdx);
		}
	}

	private void addReferenceToGlobalIndex(int globalIdx) {
		if (globalIdx == currentGlobalIdx) {
			return; // Self-references never affect dominators
		}
		if (unreachableNode != DominatorTree.NO_NODE) {
			// Garbage-to-live edges must not affect live dominators.
			int targetNode = globalToNode[globalIdx + keyOffset];
			if (targetNode != 0 && targetNode < unreachableNode) {
				return;
			}
		}
		succ.add(globalIdx);
	}

	/**
	 * Returns false for unreadable or corrupt ranges.
	 */
	private boolean read(long ofs, int len) {
		if (bytes.length < len) {
			bytes = new byte[Math.max(len, bytes.length * 2)];
		}
		try {
			readBuf.get(ofs, bytes, len);
			return true;
		} catch (IOException | BufferUnderflowException | IndexOutOfBoundsException e) {
			return false;
		}
	}

	private long idAt(int index) {
		byte[] b = bytes;
		if (idSize == 4) {
			int value = ((b[index] & 0xFF) << 24) | ((b[index + 1] & 0xFF) << 16) | ((b[index + 2] & 0xFF) << 8)
					| (b[index + 3] & 0xFF);
			return value & Snapshot.SMALL_ID_MASK;
		}
		long value = 0;
		for (int i = 0; i < 8; i++) {
			value = (value << 8) | (b[index + i] & 0xFF);
		}
		return value;
	}

	private static int primitiveSize(char typeId) {
		switch (typeId) {
		case 'Z':
		case 'B':
			return 1;
		case 'S':
		case 'C':
			return 2;
		case 'I':
		case 'F':
			return 4;
		case 'J':
		case 'D':
			return 8;
		default:
			return -1;
		}
	}

	/**
	 * Converts global indices to node ids and transposes successors into CSR predecessors.
	 */
	private int[] buildPredecessors(int[] predStart) throws HprofParsingCancelledException {
		setPhase(Phase.BUILDING_GRAPH);
		int n = numNodes;
		int numEdges = succ.size();
		for (int i = 0; i < numEdges; i++) {
			int targetNode = globalToNode[succ.get(i) + keyOffset];
			succ.set(i, targetNode);
			predStart[targetNode + 1]++;
			if ((i & PROGRESS_INTERVAL_MASK) == 0) {
				checkCancelled();
				phasePercentage = (int) (50L * i / numEdges);
			}
		}
		globalToNode = null;
		for (int w = 0; w < n; w++) {
			predStart[w + 1] += predStart[w];
		}
		// Fill in each node's segment from its end, using predStart[w + 1] as a decreasing cursor
		int[] preds = new int[numEdges];
		for (int v = n - 1; v >= 0; v--) {
			for (int i = succStart[v], end = succStart[v + 1]; i < end; i++) {
				int w = succ.get(i);
				preds[--predStart[w + 1]] = v;
			}
			if ((v & PROGRESS_INTERVAL_MASK) == 0) {
				checkCancelled();
				phasePercentage = 50 + (int) (50L * (n - v) / n);
			}
		}
		// Now predStart[w + 1] is the start of w's segment; shift back into place
		for (int w = 0; w < n; w++) {
			predStart[w] = predStart[w + 1];
		}
		predStart[n] = numEdges;
		succ.release();
		succ = null;
		succStart = null;
		return preds;
	}

	private long[] computeRetainedSizes(int[] idom) {
		int n = numNodes;
		long[] retained = new long[n];
		for (int v = 0; v < n; v++) {
			retained[v] = shallow[v];
		}
		// Children always have larger ids than their dominator
		for (int v = n - 1; v >= 1; v--) {
			retained[idom[v]] += retained[v];
		}
		return retained;
	}

	private int[] buildChildren(int[] idom, long[] retained, int[] childStart) throws HprofParsingCancelledException {
		int n = numNodes;
		for (int v = 1; v < n; v++) {
			childStart[idom[v] + 1]++;
		}
		for (int p = 0; p < n; p++) {
			childStart[p + 1] += childStart[p];
		}
		int[] children = new int[n - 1];
		for (int v = n - 1; v >= 1; v--) {
			children[--childStart[idom[v] + 1]] = v;
		}
		for (int p = 0; p < n; p++) {
			childStart[p] = childStart[p + 1];
		}
		childStart[n] = n - 1;

		for (int p = 0; p < n; p++) {
			int from = childStart[p];
			int to = childStart[p + 1];
			if (to - from > 1) {
				sortDescending(children, from, to, retained, (done, total) -> checkCancelled());
			}
			if ((p & PROGRESS_INTERVAL_MASK) == 0) {
				checkCancelled();
				phasePercentage = (int) (100L * p / n);
			}
		}
		return children;
	}

	/**
	 * Sorts {@code a[from..to)} by descending key using in-place, nonrecursive heapsort.
	 */
	static void sortDescending(int[] a, int from, int to, long[] key) throws HprofParsingCancelledException {
		sortDescending(a, from, to, key, null);
	}

	static void sortDescending(int[] a, int from, int to, long[] key, DominatorCalculator.ProgressCallback progress)
			throws HprofParsingCancelledException {
		int n = to - from;
		if (progress != null) {
			progress.progress(0, n);
		}
		for (int i = n / 2 - 1; i >= 0; i--) {
			if (progress != null && (i & CANCEL_CHECK_INTERVAL_MASK) == 0) {
				progress.progress(0, n);
			}
			siftDown(a, from, i, n, key);
		}
		for (int end = n - 1; end > 0; end--) {
			if (progress != null && (end & CANCEL_CHECK_INTERVAL_MASK) == 0) {
				progress.progress(n - end, n);
			}
			int tmp = a[from];
			a[from] = a[from + end];
			a[from + end] = tmp;
			siftDown(a, from, 0, end, key);
		}
	}

	/**
	 * Min-heap extraction yields descending order.
	 */
	private static void siftDown(int[] a, int from, int i, int n, long[] key) {
		while (true) {
			int left = 2 * i + 1;
			if (left >= n) {
				return;
			}
			int right = left + 1;
			int smallest = (right < n && key[a[from + right]] < key[a[from + left]]) ? right : left;
			if (key[a[from + smallest]] < key[a[from + i]]) {
				int tmp = a[from + i];
				a[from + i] = a[from + smallest];
				a[from + smallest] = tmp;
				i = smallest;
			} else {
				return;
			}
		}
	}

	private void setPhase(Phase newPhase) {
		phase = newPhase;
		phasePercentage = 0;
	}

	private void checkCancelled() throws HprofParsingCancelledException {
		if (cancelled) {
			throw new HprofParsingCancelledException();
		}
	}

	private void releaseScanState() {
		globalToNode = null;
		nodeToGlobal = null;
		parent = null;
		shallow = null;
		succStart = null;
		succ = null;
		stackNode = null;
		stackCursor = null;
		bytes = null;
	}
}
