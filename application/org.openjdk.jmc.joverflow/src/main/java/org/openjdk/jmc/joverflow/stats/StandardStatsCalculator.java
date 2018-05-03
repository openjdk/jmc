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

import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.DumpCorruptedException;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.support.ProblemRecorder;

/**
 * Generates the standard heap dump statistics, by calling OverallStatsCalculator and then
 * DetailedStatsCalculator. Provides an additional method to query progress percentage.
 */
public class StandardStatsCalculator {

	private final Snapshot snapshot;
	private final ProblemRecorder problemRecorder;
	private final boolean useBreadthFirst;

	private OverallStatsCalculator osc;
	private DetailedStatsCalculator dsc;
	private volatile int stage;

	public StandardStatsCalculator(Snapshot snapshot, ProblemRecorder problemRecorder, boolean useBreadthFirst) {
		this.snapshot = snapshot;
		this.problemRecorder = problemRecorder;
		this.useBreadthFirst = useBreadthFirst;
	}

	public HeapStats calculate() throws DumpCorruptedException, HprofParsingCancelledException {
		snapshot.setCalculatingStats(true);
		try {
			osc = new OverallStatsCalculator(snapshot);

			stage = 1;
			HeapStats hs = osc.calculate();
			osc = null; // Help the GC

			problemRecorder.initialize(snapshot, hs);

			dsc = new DetailedStatsCalculator(snapshot, hs, problemRecorder, useBreadthFirst);
			stage = 2;
			dsc.calculate();

			return hs;
		} catch (DumpCorruptedException.Runtime ex) {
			throw ex.getCause();
		} finally {
			snapshot.setCalculatingStats(false);
		}
	}

	public synchronized int getProgressPercentage() {
		// Below we assume that calculating detailed stats takes approximately
		// twice as much time as calculating overall stats - hence the number 33.
		switch (stage) {
		case 0:
			return 0;
		case 1: {
			OverallStatsCalculator localOsc = osc;
			if (localOsc == null) {
				// Finished overall stats, but haven't started detailed yet
				return 33;
			} else {
				return localOsc.getProgressPercentage() / 3;
			}
		}
		case 2:
			return 33 + dsc.getProgressPercentage() * 2 / 3;
		}

		return 0;
	}

	public synchronized void cancelCalculation() {
		OverallStatsCalculator localOsc = osc;
		if (localOsc != null) {
			osc.cancelCalculation();
		}

		DetailedStatsCalculator localDsc = dsc;
		if (localDsc != null) {
			localDsc.cancelCalculation();
		}
	}
}
