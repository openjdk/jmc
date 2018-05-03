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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.openjdk.jmc.joverflow.batch.BatchProblemRecorder;
import org.openjdk.jmc.joverflow.batch.DetailedStats;
import org.openjdk.jmc.joverflow.batch.ReferencedObjCluster;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.support.DupStringStats;
import org.openjdk.jmc.joverflow.support.ReferenceChain;

/**
 * Looks at several (currently only 3 is really supported) heap dumps, and determines which
 * duplicated string clusters are most likely long-lived. So far this is based just on the fact that
 * the same cluster shows up in two out of three dumps.
 */
public class LongLivedStringClustersCalculator {
	private final int numDumps;
	private final List<ReferencedObjCluster.DupStrings> dupStringsToFields[];

	private volatile DetailedDupStringStatsCalculator ssc;
	private int curDumpNum;

	@SuppressWarnings("unchecked")
	public LongLivedStringClustersCalculator(int numDumps) {
		this.numDumps = numDumps;
		dupStringsToFields = new ArrayList[numDumps];
	}

	public DupStringStats update(Snapshot snapshot) throws HprofParsingCancelledException {
		long totalObjSize = snapshot.getRoughTotalObjectSize();
		int minOverhead = (int) (totalObjSize / 1000);
		BatchProblemRecorder recorder = new BatchProblemRecorder();

		ssc = new DetailedDupStringStatsCalculator(snapshot, recorder);
		DupStringStats dss = ssc.calculate();

		DetailedStats ds = recorder.getDetailedStats(minOverhead);
		dupStringsToFields[curDumpNum] = ds.dupStringClusters.get(1);

		curDumpNum++;
		ssc = null; // Help the GC
		return dss;
	}

	public void calculate() {
		int minNumRepeats = numDumps / 2 + 1;

		LinkedHashSet<String> longLivedFields = new LinkedHashSet<>();

		for (int i = 0; i < numDumps - minNumRepeats + 1; i++) {
			// Get each cluster from the current dump, and check if it's present in the
			// next (at least) minNumRepeats - 1 dumps
			List<ReferencedObjCluster.DupStrings> clusters1 = dupStringsToFields[i];

			for (ReferencedObjCluster cluster1 : clusters1) {
				String classAndField1 = ReferenceChain.toStringInStraightOrder(cluster1.getReferer());

				for (int j = i + 1; j < numDumps; j++) {
					List<ReferencedObjCluster.DupStrings> clusters2 = dupStringsToFields[j];

					for (ReferencedObjCluster cluster2 : clusters2) {
						// TODO: the fact that there is just a single check below implies that there are just 3 dumps in total
						// FIXME: The code below is most likely wrong; it has been changed just to compile after we changed many things in ReferenceChain etc.
						if (cluster2.getReferer().toString().equals(classAndField1)) {

							longLivedFields.add(classAndField1);
						}
					}
				}
			}
		}

		System.out.println("\nLONG-LIVED FIELDS:");
		for (String longLivedField : longLivedFields) {
			System.out.println(longLivedField);
		}
	}

	public int getProgressPercentage() {
		DetailedDupStringStatsCalculator sscCopy = ssc;
		if (sscCopy != null) {
			return sscCopy.getProgressPercentage();
		} else {
			return 100;
		}
	}
}
