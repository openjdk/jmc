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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;

/**
 * Checks Lengauer-Tarjan against known graphs and brute-force results on random graphs.
 */
public class DominatorCalculatorTest {

	@Test
	public void testChain() throws Exception {
		int[][] g = {{1}, {2}, {3}, {}};
		Assert.assertArrayEquals(new int[] {-1, 0, 1, 2}, computeIdoms(g));
	}

	@Test
	public void testDiamond() throws Exception {
		int[][] g = {{1, 2}, {3}, {3}, {}};
		Assert.assertArrayEquals(new int[] {-1, 0, 0, 0}, computeIdoms(g));
	}

	@Test
	public void testCycleAndSelfLoop() throws Exception {
		int[][] g = {{1}, {2, 3}, {1, 0}, {3}};
		Assert.assertArrayEquals(new int[] {-1, 0, 1, 1}, computeIdoms(g));
	}

	@Test
	public void testDuplicateEdges() throws Exception {
		int[][] g = {{1, 1, 2, 2}, {3, 3}, {3}, {1}};
		Assert.assertArrayEquals(new int[] {-1, 0, 0, 0}, computeIdoms(g));
	}

	/**
	 * Figure 1 of Lengauer-Tarjan, with R=0 and A..L=1..12.
	 */
	@Test
	public void testLengauerTarjanPaperExample() throws Exception {
		final int R = 0, A = 1, B = 2, C = 3, D = 4, E = 5, F = 6, G = 7, H = 8, I = 9, J = 10, K = 11, L = 12;
		int[][] g = new int[13][];
		g[R] = new int[] {A, B, C};
		g[A] = new int[] {D};
		g[B] = new int[] {A, D, E};
		g[C] = new int[] {F, G};
		g[D] = new int[] {L};
		g[E] = new int[] {H};
		g[F] = new int[] {I};
		g[G] = new int[] {I, J};
		g[H] = new int[] {E, K};
		g[I] = new int[] {K};
		g[J] = new int[] {I};
		g[K] = new int[] {I, R};
		g[L] = new int[] {H};

		int[] expected = new int[13];
		expected[R] = -1;
		expected[A] = R;
		expected[B] = R;
		expected[C] = R;
		expected[D] = R;
		expected[E] = R;
		expected[F] = C;
		expected[G] = C;
		expected[H] = R;
		expected[I] = R;
		expected[J] = G;
		expected[K] = R;
		expected[L] = D;
		Assert.assertArrayEquals(expected, computeIdoms(g));
		Assert.assertArrayEquals(expected, bruteForceIdoms(g));
	}

	@Test
	public void testRandomGraphsAgainstBruteForce() throws Exception {
		Random rnd = new Random(20260425);
		for (int iteration = 0; iteration < 300; iteration++) {
			int n = 2 + rnd.nextInt(60);
			int[][] g = randomReachableGraph(n, rnd);
			Assert.assertArrayEquals("Graph " + Arrays.deepToString(g), bruteForceIdoms(g), computeIdoms(g));
		}
	}

	@Test
	public void testDeepChainDoesNotOverflowStack() throws Exception {
		int n = 200000;
		int[][] g = new int[n][];
		for (int v = 0; v < n - 1; v++) {
			g[v] = new int[] {v + 1};
		}
		g[n - 1] = new int[] {0};
		int[] idoms = computeIdoms(g);
		for (int v = 1; v < n; v++) {
			Assert.assertEquals(v - 1, idoms[v]);
		}
	}

	@Test
	public void testSortDescending() throws Exception {
		Random rnd = new Random(42);
		for (int iteration = 0; iteration < 100; iteration++) {
			int n = rnd.nextInt(200);
			long[] key = new long[n];
			int[] a = new int[n + 10];
			for (int i = 0; i < n; i++) {
				key[i] = rnd.nextInt(20);
				a[i + 5] = i;
			}
			DominatorTreeBuilder.sortDescending(a, 5, 5 + n, key);
			boolean[] seen = new boolean[n];
			for (int i = 5; i < 5 + n; i++) {
				Assert.assertFalse(seen[a[i]]);
				seen[a[i]] = true;
				if (i > 5) {
					Assert.assertTrue(key[a[i - 1]] >= key[a[i]]);
				}
			}
			// Elements outside the sorted range are untouched
			for (int i = 0; i < 5; i++) {
				Assert.assertEquals(0, a[i]);
				Assert.assertEquals(0, a[5 + n + i]);
			}
		}
	}

	@Test(expected = HprofParsingCancelledException.class)
	public void testSortCancellation() throws Exception {
		int n = 100000;
		int[] a = new int[n];
		long[] key = new long[n];
		for (int i = 0; i < n; i++) {
			a[i] = i;
			key[i] = i;
		}
		DominatorTreeBuilder.sortDescending(a, 0, n, key, (done, total) -> {
			if (done > 0) {
				Assert.assertTrue("Cancellation must be checked before sorting finishes", done < total);
				throw new HprofParsingCancelledException();
			}
		});
	}

	private static int[][] randomReachableGraph(int n, Random rnd) {
		List<List<Integer>> succ = new ArrayList<>();
		for (int v = 0; v < n; v++) {
			succ.add(new ArrayList<>());
		}
		// A random spanning tree guarantees reachability
		for (int v = 1; v < n; v++) {
			succ.get(rnd.nextInt(v)).add(v);
		}
		int extraEdges = rnd.nextInt(3 * n);
		for (int i = 0; i < extraEdges; i++) {
			succ.get(rnd.nextInt(n)).add(rnd.nextInt(n));
		}
		int[][] g = new int[n][];
		for (int v = 0; v < n; v++) {
			List<Integer> list = succ.get(v);
			// Shuffle so that the DFS order differs from the numbering
			Collections.shuffle(list, rnd);
			g[v] = list.stream().mapToInt(Integer::intValue).toArray();
		}
		return g;
	}

	/**
	 * Renumbers the graph in DFS preorder and maps computed dominators back to original ids.
	 */
	private static int[] computeIdoms(int[][] successors) throws Exception {
		int n = successors.length;
		int[] number = new int[n];
		Arrays.fill(number, -1);
		int[] vertex = new int[n];
		int[] parent = new int[n];
		int count = 0;

		number[0] = count;
		vertex[count] = 0;
		parent[count] = -1;
		count++;
		Deque<int[]> stack = new ArrayDeque<>();
		stack.push(new int[] {0, 0});
		while (!stack.isEmpty()) {
			int[] top = stack.peek();
			int v = top[0];
			if (top[1] < successors[v].length) {
				int w = successors[v][top[1]++];
				if (number[w] == -1) {
					number[w] = count;
					vertex[count] = w;
					parent[count] = number[v];
					count++;
					stack.push(new int[] {w, 0});
				}
			} else {
				stack.pop();
			}
		}
		Assert.assertEquals("All vertices must be reachable from the root", n, count);

		int[] predStart = new int[n + 1];
		int numEdges = 0;
		for (int v = 0; v < n; v++) {
			for (int w : successors[v]) {
				predStart[number[w] + 1]++;
				numEdges++;
			}
		}
		for (int w = 0; w < n; w++) {
			predStart[w + 1] += predStart[w];
		}
		int[] preds = new int[numEdges];
		int[] fill = predStart.clone();
		for (int v = 0; v < n; v++) {
			for (int w : successors[v]) {
				preds[fill[number[w]]++] = number[v];
			}
		}

		int[] idomByNumber = DominatorCalculator.computeImmediateDominators(n, parent, predStart, preds, null);
		int[] idom = new int[n];
		for (int i = 0; i < n; i++) {
			idom[vertex[i]] = idomByNumber[i] == -1 ? -1 : vertex[idomByNumber[i]];
		}
		return idom;
	}

	/**
	 * Removing d identifies the nodes it dominates; their immediate dominator is the deepest strict
	 * dominator.
	 */
	private static int[] bruteForceIdoms(int[][] successors) {
		int n = successors.length;
		boolean[][] dom = new boolean[n][n]; // dom[d][v]: d dominates v
		for (int d = 0; d < n; d++) {
			boolean[] reachable = new boolean[n];
			if (d != 0) {
				Deque<Integer> queue = new ArrayDeque<>();
				queue.add(0);
				reachable[0] = true;
				while (!queue.isEmpty()) {
					int v = queue.poll();
					for (int w : successors[v]) {
						if (w != d && !reachable[w]) {
							reachable[w] = true;
							queue.add(w);
						}
					}
				}
			}
			for (int v = 0; v < n; v++) {
				dom[d][v] = !reachable[v];
			}
		}
		int[] idom = new int[n];
		idom[0] = -1;
		for (int v = 1; v < n; v++) {
			int best = -1;
			int bestDepth = -1;
			for (int d = 0; d < n; d++) {
				if (d == v || !dom[d][v]) {
					continue;
				}
				int depth = 0;
				for (int e = 0; e < n; e++) {
					if (dom[e][d]) {
						depth++;
					}
				}
				if (depth > bestDepth) {
					bestDepth = depth;
					best = d;
				}
			}
			idom[v] = best;
		}
		return idom;
	}
}
