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

import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.util.IntArrayList;

/**
 * Computes the immediate dominator of every vertex in a flow graph, using the Lengauer-Tarjan
 * algorithm (the "simple" O(E log V) variant that uses path compression without balancing).
 * <p>
 * Graph representation:
 * <ul>
 * <li>Vertices are numbered {@code 0 .. n-1} in DFS preorder from root 0. Every vertex is
 * reachable, and its DFS parent has a smaller number.</li>
 * <li>{@code parent[v]} is the DFS tree parent of vertex {@code v}. The edge from the parent is
 * implicit: it is always treated as an incoming edge of {@code v}, whether or not it is also
 * present in the explicit predecessor lists.</li>
 * <li>The remaining incoming edges of {@code v} are {@code preds[predStart[v] .. predStart[v+1])}.
 * Duplicate edges and self-loops are allowed.</li>
 * </ul>
 */
final class DominatorCalculator {

	/**
	 * Receives periodic progress notifications, and may abort the computation by throwing.
	 */
	interface ProgressCallback {
		void progress(int numDone, int numTotal) throws HprofParsingCancelledException;
	}

	private static final int PROGRESS_INTERVAL_MASK = (1 << 16) - 1;

	private final int[] semi;
	private final int[] label;
	private final int[] ancestor;
	private final IntArrayList compressPath = new IntArrayList(1024);

	private DominatorCalculator(int n) {
		semi = new int[n];
		label = new int[n];
		ancestor = new int[n];
		for (int v = 0; v < n; v++) {
			semi[v] = v;
			label[v] = v;
			ancestor[v] = -1;
		}
	}

	/**
	 * @param n
	 *            the number of vertices
	 * @param parent
	 *            the DFS tree parent of each vertex; {@code parent[0]} is ignored
	 * @param predStart
	 *            offsets into {@code preds}, of length {@code n + 1}
	 * @param preds
	 *            the explicit predecessors of all vertices, grouped by vertex
	 * @param progress
	 *            progress callback, or null
	 * @return an array where element {@code v} is the immediate dominator of vertex {@code v}, and
	 *         element 0 (the root) is -1
	 */
	static int[] computeImmediateDominators(
		int n, int[] parent, int[] predStart, int[] preds, ProgressCallback progress)
			throws HprofParsingCancelledException {
		if (n == 0) {
			return new int[0];
		}
		DominatorCalculator calc = new DominatorCalculator(n);
		int[] semi = calc.semi;
		int[] bucketHead = new int[n];
		int[] bucketNext = new int[n];
		int[] idom = new int[n];
		for (int v = 0; v < n; v++) {
			bucketHead[v] = -1;
			idom[v] = -1;
		}

		for (int w = n - 1; w >= 1; w--) {
			int p = parent[w];

			// Step 2: compute the semidominator of w, as the minimum over all predecessors v of
			// semi(eval(v)). For predecessors not yet linked into the forest, eval(v) == v.
			int semiW = w;
			int u = calc.eval(p);
			if (semi[u] < semiW) {
				semiW = semi[u];
			}
			for (int i = predStart[w], end = predStart[w + 1]; i < end; i++) {
				u = calc.eval(preds[i]);
				if (semi[u] < semiW) {
					semiW = semi[u];
				}
			}
			semi[w] = semiW;
			bucketNext[w] = bucketHead[semiW];
			bucketHead[semiW] = w;
			calc.ancestor[w] = p; // link(p, w)

			// Step 3: implicitly define the immediate dominator of each vertex in bucket(p)
			for (int v = bucketHead[p]; v != -1;) {
				int next = bucketNext[v];
				u = calc.eval(v);
				idom[v] = semi[u] < semi[v] ? u : p;
				v = next;
			}
			bucketHead[p] = -1;

			if (progress != null && ((n - w) & PROGRESS_INTERVAL_MASK) == 0) {
				progress.progress(n - w, n);
			}
		}

		// Step 4: explicitly define the immediate dominators, in preorder
		for (int w = 1; w < n; w++) {
			if (idom[w] != semi[w]) {
				idom[w] = idom[idom[w]];
			}
		}
		idom[0] = -1;
		return idom;
	}

	private int eval(int v) {
		if (ancestor[v] == -1) {
			return v;
		}
		compress(v);
		return label[v];
	}

	/**
	 * Iterative path compression avoids stack overflow on long reference chains.
	 */
	private void compress(int v) {
		compressPath.clear();
		int x = v;
		while (ancestor[ancestor[x]] != -1) {
			compressPath.add(x);
			x = ancestor[x];
		}
		for (int i = compressPath.size() - 1; i >= 0; i--) {
			x = compressPath.get(i);
			int a = ancestor[x];
			if (semi[label[a]] < semi[label[x]]) {
				label[x] = label[a];
			}
			ancestor[x] = ancestor[a];
		}
	}
}
