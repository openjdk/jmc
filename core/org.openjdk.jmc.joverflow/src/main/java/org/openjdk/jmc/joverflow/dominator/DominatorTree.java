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

import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;

/**
 * Immutable heap-dump dominator tree produced by {@link DominatorTreeBuilder}.
 * <p>
 * A dominates B if every GC-root path to B passes through A. B's immediate dominator is its closest
 * strict dominator. A's retained size is the total shallow size of all objects it dominates,
 * including A.
 * <p>
 * Node ids are dense in {@code 0 .. getNodeCount()-1}, in DFS preorder; each immediate dominator
 * has a smaller id. {@link #ROOT_NODE} represents all GC roots. Unreachable objects share a second
 * synthetic parent; see {@link #getUnreachableObjectsNode()}. Children are ordered by decreasing
 * retained size.
 * <p>
 * Tree queries are thread safe. Objects returned by {@link #getObject(int)} read lazily through the
 * underlying {@link Snapshot}, which is not safe for concurrent reads.
 */
public final class DominatorTree {
	/**
	 * Synthetic root representing all GC roots.
	 */
	public static final int ROOT_NODE = 0;

	/**
	 * Sentinel for an absent node.
	 */
	public static final int NO_NODE = -1;

	private final Snapshot snapshot;
	private final int[] nodeToGlobalIndex;
	private final int[] idom;
	private final int[] shallowSize;
	private final long[] retainedSize;
	private final int[] childStart;
	private final int[] children;
	private final int unreachableNode;

	DominatorTree(Snapshot snapshot, int[] nodeToGlobalIndex, int[] idom, int[] shallowSize, long[] retainedSize,
			int[] childStart, int[] children, int unreachableNode) {
		this.snapshot = snapshot;
		this.nodeToGlobalIndex = nodeToGlobalIndex;
		this.idom = idom;
		this.shallowSize = shallowSize;
		this.retainedSize = retainedSize;
		this.childStart = childStart;
		this.children = children;
		this.unreachableNode = unreachableNode;
	}

	public Snapshot getSnapshot() {
		return snapshot;
	}

	/**
	 * Node count, including synthetic nodes.
	 */
	public int getNodeCount() {
		return idom.length;
	}

	/**
	 * Object count (instances, arrays and classes), excluding synthetic nodes.
	 */
	public int getObjectCount() {
		return getNodeCount() - (unreachableNode == NO_NODE ? 1 : 2);
	}

	/**
	 * Synthetic parent of unreachable objects, or {@link #NO_NODE} if all objects are reachable.
	 */
	public int getUnreachableObjectsNode() {
		return unreachableNode;
	}

	public boolean isSyntheticNode(int node) {
		return node == ROOT_NODE || node == unreachableNode;
	}

	/**
	 * Whether the object is reachable from GC roots. False for synthetic nodes.
	 */
	public boolean isReachableFromGcRoots(int node) {
		return !isSyntheticNode(node) && (unreachableNode == NO_NODE || node < unreachableNode);
	}

	/**
	 * Index for {@link Snapshot#getObjectAtGlobalIndex(int)}, or
	 * {@link Snapshot#NO_GLOBAL_OBJECT_INDEX} for synthetic nodes.
	 */
	public int getGlobalObjectIndex(int node) {
		return nodeToGlobalIndex[node];
	}

	/**
	 * Heap object, or null for synthetic nodes.
	 */
	public JavaHeapObject getObject(int node) {
		int globalIndex = nodeToGlobalIndex[node];
		return globalIndex == Snapshot.NO_GLOBAL_OBJECT_INDEX ? null : snapshot.getObjectAtGlobalIndex(globalIndex);
	}

	/**
	 * Immediate dominator, or {@link #NO_NODE} for the root.
	 */
	public int getImmediateDominator(int node) {
		return idom[node];
	}

	/**
	 * Whether {@code dominator} properly dominates {@code node} (a node does not dominate itself).
	 */
	public boolean dominates(int dominator, int node) {
		for (int d = idom[node]; d != NO_NODE; d = idom[d]) {
			if (d == dominator) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Shallow size, or 0 for synthetic nodes.
	 */
	public int getShallowSize(int node) {
		return shallowSize[node];
	}

	public long getRetainedSize(int node) {
		return retainedSize[node];
	}

	/**
	 * Total shallow size, including unreachable objects.
	 */
	public long getTotalSize() {
		return retainedSize[ROOT_NODE];
	}

	/**
	 * Total shallow size of unreachable objects.
	 */
	public long getUnreachableSize() {
		return unreachableNode == NO_NODE ? 0 : retainedSize[unreachableNode];
	}

	public int getChildCount(int node) {
		return childStart[node + 1] - childStart[node];
	}

	/**
	 * Returns child {@code childIdx}; children are ordered by decreasing retained size.
	 */
	public int getChild(int node, int childIdx) {
		if (childIdx < 0 || childIdx >= getChildCount(node)) {
			throw new IndexOutOfBoundsException(
					"Child index " + childIdx + " for node " + node + " with " + getChildCount(node) + " children");
		}
		return children[childStart[node] + childIdx];
	}
}
