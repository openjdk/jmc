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
package org.openjdk.jmc.joverflow;

import java.io.IOException;

import org.openjdk.jmc.joverflow.batch.BatchProblemRecorder;
import org.openjdk.jmc.joverflow.batch.DetailedStats;
import org.openjdk.jmc.joverflow.batch.ReportFormatter;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.DumpCorruptedException;
import org.openjdk.jmc.joverflow.heap.parser.HeapDumpReader;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.stats.StandardStatsCalculator;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.util.FileUtils;
import org.openjdk.jmc.joverflow.util.ProgressMeter;
import org.openjdk.jmc.joverflow.util.VerboseOutputCollector;

/**
 * Programmatic access to JOverflow text report generation functionality. User of this class should
 * call parseDump() once to read and analyze a heap dump, and then can call getReport() methods one
 * or more times on the returned instance.
 */
public class ReportGenerator {

	private static final boolean DEFAULT_PRINT_FULL_OBJ_HISTO = false;
	private static final int DEFAULT_PRINTED_REF_CHAIN_DEPTH = 8;
	private static final String[] DEFAULT_REF_CHAIN_STOPPER_CLASS_PREFIXES = new String[] {"oracle.apps."};

	private ReportFormatter reportFormatter;

	// We have the fields below instead of local variables only to support
	// cancellation.
	private volatile HeapDumpReader reader;
	private volatile StandardStatsCalculator ssc;

	/**
	 * An instance of a subclass of this class can be passed to
	 * {@link ReportGenerator#parseDump(String, ReportGenerator.Progress, boolean)}. It is used for
	 * getting info on heap dump reading/analysis progress, and for asynchronous cancellation of
	 * this process.
	 */
	public abstract static class Progress {

		private ReportGenerator rg;

		/**
		 * Heap dump reading/analysis code calls this method periodically with the value, in percent
		 * points (1..100), of its progress.
		 */
		public abstract void reportProgress(int percent);

		/**
		 * Cancels execution of {@link #parseDump(String, Progress, boolean)} asynchronously. That
		 * method will throw a {@link HprofParsingCancelledException} immediately or soon after the
		 * call to cancel().
		 */
		public void cancel() {
			ReportGenerator localRg = rg;
			if (localRg == null) {
				return;
			}

			HeapDumpReader localReader = localRg.reader;
			if (localReader != null) {
				localReader.cancelReading();
			}
			StandardStatsCalculator localSsc = localRg.ssc;
			if (localSsc != null) {
				localSsc.cancelCalculation();
			}
		}

		private void setReportGenerator(ReportGenerator rg) {
			this.rg = rg;
		}
	}

	/**
	 * Parses and analyses a heap dump, and if everything is ok, returns an instance of this class.
	 *
	 * @param hprofFile
	 *            name of the heap dump (.hprof) file
	 * @param progress
	 *            if non-null, it will be called periodically to report progress, and can also be
	 *            used by the user to cancel execution.
	 * @param useBreadthFirstScan
	 *            if true, the dump will be scanned in breadth-first order during detailed analysis.
	 *            Otherwise, depth-first order is used. Depth-first order is usually faster, but it
	 *            often generates more diverse reference chains, which makes analysis results less
	 *            useful.
	 * @return an instance of this class containing heap dump analysis results
	 * @throws IOException
	 *             if an I/O error occurred when reading .hprof file
	 * @throws DumpCorruptedException
	 *             if data corruption is detected in the .hprof file
	 * @throws HprofParsingCancelledException
	 *             if {@link Progress#cancel()} was invoked
	 */
	public static ReportGenerator parseDump(String hprofFile, Progress progress, boolean useBreadthFirstScan)
			throws IOException, DumpCorruptedException, HprofParsingCancelledException {
		FileUtils.checkFileExistsAndReadable(hprofFile, false);

		return new ReportGenerator(hprofFile, progress, useBreadthFirstScan);
	}

	/**
	 * Returns the results of JOverflow analysis as a single string. Internally, calls
	 * {@link #getReport(boolean, int, String[])} with default parameter values (see public
	 * DEFAULT_xxx constants in this class)
	 */
	public String getReport() {
		return getReport(DEFAULT_PRINT_FULL_OBJ_HISTO, DEFAULT_PRINTED_REF_CHAIN_DEPTH,
				DEFAULT_REF_CHAIN_STOPPER_CLASS_PREFIXES);
	}

	/**
	 * Returns the results of JOverflow analysis as a single string.
	 *
	 * @param printFullClassHistogram
	 *            if true, class histogram containing all classes is printed. Otherwise, only
	 *            classes whose instances consume more than 0.1% of the heap are printed.
	 * @param printedRefChainDepth
	 *            depth of reference chains printed in the report
	 * @param refChainStopperClassPrefixes
	 *            if any class in a reference chain starts with any of these prefixes, the reference
	 *            chain is printed all the way down to this class instead of the fixed depth.
	 */
	public String getReport(
		boolean printFullClassHistogram, int printedRefChainDepth, String[] refChainStopperClassPrefixes) {
		return reportFormatter.getReport(printFullClassHistogram, printedRefChainDepth, refChainStopperClassPrefixes);
	}

	private ReportGenerator(String hprofFile, Progress progress, boolean useBreadthFirstScan)
			throws IOException, DumpCorruptedException, HprofParsingCancelledException {
		CallbackProgressMeter pm = null;
		if (progress != null) {
			progress.setReportGenerator(this);
			pm = new CallbackProgressMeter(progress);
			pm.start();
		}

		Snapshot snapshot = readSnapshot(hprofFile);

		BatchProblemRecorder recorder = new BatchProblemRecorder();
		ssc = new StandardStatsCalculator(snapshot, recorder, useBreadthFirstScan);
		HeapStats hs = ssc.calculate();
		ssc = null;
		int minOvhdToReport = (int) hs.totalObjSize / 1000;
		DetailedStats ds = recorder.getDetailedStats(minOvhdToReport);

		if (pm != null) {
			pm.stopReporting();
		}

		reportFormatter = new ReportFormatter(hs, ds);
	}

	private Snapshot readSnapshot(String fileName)
			throws IOException, DumpCorruptedException, HprofParsingCancelledException {
		VerboseOutputCollector vc = new VerboseOutputCollector();
		reader = HeapDumpReader.createReader(new ReadBuffer.CachedReadBufferFactory(fileName, 0), 0, vc);
		Snapshot snapshot = reader.read();
		reader = null;
		return snapshot;
	}

	private class CallbackProgressMeter extends ProgressMeter {

		private final Progress pc;
		private int prevReadPercent, prevAnalyzePercent;

		CallbackProgressMeter(Progress pc) {
			this.pc = pc;
		}

		@Override
		public int queryPercentage() {
			int readPercent = prevReadPercent, analyzePercent = prevAnalyzePercent;
			HeapDumpReader localReader = reader;
			if (localReader != null) {
				readPercent = localReader.getProgressPercentage();
				prevReadPercent = readPercent;
			} else if (prevReadPercent > 0) {
				readPercent = 100;
			}
			StandardStatsCalculator localSsc = ssc;
			if (localSsc != null) {
				analyzePercent = localSsc.getProgressPercentage();
				prevAnalyzePercent = analyzePercent;
			}

			// Assume that reading takes 1/3 of time and analysis takes 2/3.
			return readPercent / 3 + analyzePercent * 2 / 3;
		}

		@Override
		public void reportProgress(int progressPerecentage) {
			pc.reportProgress(progressPerecentage);
		}
	}
}
