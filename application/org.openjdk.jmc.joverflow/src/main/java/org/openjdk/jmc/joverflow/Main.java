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

import java.io.File;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmc.joverflow.batch.BatchProblemRecorder;
import org.openjdk.jmc.joverflow.batch.DetailedStats;
import org.openjdk.jmc.joverflow.batch.ReportFormatter;
import org.openjdk.jmc.joverflow.codeanalysis.DupStringFieldFinder;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.DumpCorruptedException;
import org.openjdk.jmc.joverflow.heap.parser.HeapDumpReader;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.stats.LongLivedStringClustersCalculator;
import org.openjdk.jmc.joverflow.stats.StandardStatsCalculator;
import org.openjdk.jmc.joverflow.support.DupStringStats;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.util.FileUtils;
import org.openjdk.jmc.joverflow.util.MemNumFormatter;
import org.openjdk.jmc.joverflow.util.ProgressMeter;
import org.openjdk.jmc.joverflow.util.Utils;
import org.openjdk.jmc.joverflow.util.VerboseOutputCollector;

/**
 * Main class of the JOverflow tool. Reads the specified dump file, calculates and prints overall
 * statistics for it, then runs anti-pattern analysis.
 */
public class Main {

	private static final String VERSION = "0.8";

	private static final String USAGE = "Usage: joverflow <options> heap_dump_file\n\n" + "where options include\n"
			+ "  -verbose          Print verbose diagnostic messages if problems\n"
			+ "           occur when parsing heap dump file, etc.\n"
			+ "  -depth_first, -dfs    Use depth-first heap scan (default)\n"
			+ "  -breadth_first, -bfs  Use breadth-first heap scan (slower but produces\n"
			+ "           generally shorter and more useful reference chains)\n"
			+ "  -full_obj_histo   Print full object histogram\n"
			+ "           (normally only top memory consumers are printed)\n"
			+ "  -ref_chain_depth=<n> Print up to n elements in reverse reference\n"
			+ "           chains leading to problematic objects (default is 8)\n"
			+ "  -ref_chain_stoppers=<prefix1,prefix2,...> Stop printing a reference\n"
			+ "           chain when class starting with any of prefixes is reached\n"
			+ "           (default is oracle.apps.)"
			+ "  -pointer_size=<size in bytes>   Explicitly specify JVM pointer size\n"
			+ "           to be used in calculations. Makes sense for 64-bit heap dumps.\n"
			+ "  -use_mmap         Use mmap to access data on disk during heap analysis\n"
			+ "           (default is JOverflow's own custom disk cache)";

	private static final int MIN_OVHD_TO_REPORT_AS_HEAP_FRACTION = 1000; // 0.1%

	private static String[] refChainStopperClassPrefixes = new String[] {"oracle.apps."};

	private static boolean printFullObjectHistogram;
	private static int printedRefChainDepth = 8;
	private static int explicitPointerSize;
	private static boolean useMmap;
	private static boolean useBreadthFirst;
	private static boolean findLongLivedStrings;
	private static File stringsToInternTextFile;
	private static boolean verbose;

	private static long startTime0, startTime1, snapshotReadTimeMs;

	public static void main(String args[]) throws Exception {
		System.err.println("JOverflow version " + VERSION);

		ArrayList<String> dumpFileNames = new ArrayList<>();
		ArrayList<String> classpath = new ArrayList<>();

		for (String arg : args) {
			if (arg.charAt(0) == '-') {
				if (arg.equals("-help")) {
					System.out.println(USAGE);
				} else if (arg.equals("-verbose")) {
					verbose = true;
				} else if (arg.equals("-full_obj_histo")) {
					printFullObjectHistogram = true;
				} else if (arg.startsWith("-ref_chain_depth=")) {
					printedRefChainDepth = parseNumericFlag(arg);
				} else if (arg.startsWith("-ref_chain_stoppers=")) {
					refChainStopperClassPrefixes = parseCommaSeparatedStringsFlag(arg);
				} else if (arg.startsWith("-pointer_size=")) {
					explicitPointerSize = parseNumericFlag(arg);
				} else if (arg.equals("-use_mmap")) {
					useMmap = true;
				} else if (arg.equals("-depth_first") || arg.equals("-dfs")) {
					useBreadthFirst = false;
				} else if (arg.equals("-breadth_first") || arg.equals("-bfs")) {
					useBreadthFirst = true;
				} else if (arg.equals("-long_lived_strings")) {
					findLongLivedStrings = true;
				} else if (arg.startsWith("-print_string_fields_to_intern=")) {
					stringsToInternTextFile = parseFileNameFlag(arg);
				} else {
					System.err.println("Unknown flag: " + arg);
					System.exit(-1);
				}
			} else {
				if (arg.endsWith(".txt")) {
					File jarListFile = FileUtils.fileExistsAndReadableOrExit(arg);
					classpath.addAll(FileUtils.readTextFile(jarListFile));
				} else if (arg.endsWith(".jar")) {
					classpath.add(arg);
				} else {
					FileUtils.fileExistsAndReadableOrExit(arg);
					dumpFileNames.add(arg);
				}
			}
		}

		if (dumpFileNames.isEmpty()) {
			System.err.println("No dump file specified");
			System.exit(-1);
		}

		VerboseOutputCollector vc = new VerboseOutputCollector();

		if (findLongLivedStrings) {
			// This is a highly experimental thing, may be removed any time
			if (dumpFileNames.size() <= 1) {
				System.err.println("-string_fields_to_intern requires more than one dump file");
				System.exit(-1);
			}
			generateLongLivedStringsReport(dumpFileNames, vc);
		} else {
			if (dumpFileNames.size() > 1) {
				System.err.println("More than one dump file specified: " + dumpFileNames);
				System.exit(-1);
			}
			String fileName = dumpFileNames.get(0);
			checkFileAndGetSize(fileName);

			Snapshot snapshot = readSnapshot(fileName, vc);
			generateStandardReport(snapshot, classpath);
		}
	}

	private static void generateStandardReport(Snapshot snapshot, ArrayList<String> classpath) {
		ReportFormatter rf = calculateAndFormatStandardStats(snapshot);

		long endTime = System.currentTimeMillis();
		long heapAnalysisTimeMs = endTime - startTime1;

		if (stringsToInternTextFile != null) {
			System.err.println("Storing info on String fields to intern into " + stringsToInternTextFile.getName());
			List<String> fieldsToIntern = DupStringFieldFinder.getFieldsToInternAsText(rf.getHeapStats(),
					rf.getDetailedStats());
			try {
				FileUtils.writeTextToFile(stringsToInternTextFile, fieldsToIntern);
			} catch (IOException ex) {
				System.err.println(
						"Could not write to file " + stringsToInternTextFile.getName() + ": " + ex.getMessage());
			}
		}

		long totalTimeMs = endTime - startTime0;
		int heapAnalysisTimeSec = (int) (heapAnalysisTimeMs / 1000);
		int totalTimeSec = (int) (totalTimeMs / 1000);

		// A bit of cleanup after all progress/debugging output
		System.err.println(
				"\rDone. Heap analysis time: " + heapAnalysisTimeSec + " sec, total time: " + totalTimeSec + " sec");

		VerboseOutputCollector vc = snapshot.getVerboseOutputCollector();
		if (verbose) {
			List<String> warnings = vc.getWarnings();
			if (!warnings.isEmpty()) { // In verbose mode, print all warnings first
				for (String warning : warnings) {
					System.err.println(warning);
				}
			}
		}

		// Print the output from heap analysis
		System.out.println(rf.getReport(printFullObjectHistogram, printedRefChainDepth, refChainStopperClassPrefixes));

		System.out.println("\nSnapshot read time: " + (snapshotReadTimeMs / 1000) + " sec");
		System.out.println("Heap analysis time: " + heapAnalysisTimeSec + " sec");
		System.out.println("Total time: " + totalTimeSec + " sec");
		System.out.print("GC time: ");
		List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean gcBean : gcBeans) {
			System.out.print(gcBean.getName() + " " + (gcBean.getCollectionTime()) + " ms  ");
		}
		System.out.println();

		System.gc();
		long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println("Used memory: " + (usedMemory / 1024 / 1024) + "M");

		List<String> warningKinds = vc.getWarningKinds();
		if (!warningKinds.isEmpty()) { // Mention warning kinds in the end anyway
			System.err.print("Note: there were some warning(s) of kind ");
			for (String warning : warningKinds) {
				System.err.print(warning);
				System.err.print(", ");
			}
			System.err.println("\nRun the tool with -verbose to get more details");
		}
	}

	/**
	 * Highly experimental functionality, which attempts to determine, from three heap dumps, which
	 * duplicate string clusters are likely long-lived and thus worth optimizing.
	 */
	private static void generateLongLivedStringsReport(ArrayList<String> fileNames, VerboseOutputCollector vc)
			throws IOException, HprofParsingCancelledException {
		System.err.println("Comparing heap dumps to find long-living string clusters...");

		final LongLivedStringClustersCalculator llc = new LongLivedStringClustersCalculator(fileNames.size());

		for (String fileName : fileNames) {
			System.err.println("Processing heap dump " + fileName);
			Snapshot snapshot = readSnapshot(fileName, vc);
			long totalObjSize = snapshot.getRoughTotalObjectSize();
			MemNumFormatter nf = new MemNumFormatter(totalObjSize);

			ProgressMeter pm = new PrintingProgressMeter() {
				@Override
				public int queryPercentage() {
					return llc.getProgressPercentage();
				}
			};
			pm.start();

			DupStringStats dss = llc.update(snapshot);

			pm.stopReporting();

			System.out.println("Heap dump " + fileName + ":");
			System.out.format("Total strings: %,d Unique string values: %,d Duplicate values: %,d Overhead: %s%n",
					dss.nStrings, dss.nUniqueStringValues, dss.nUniqueDupStringValues,
					nf.getNumInKAndPercent(dss.dupStringsOverhead));
		}

		llc.calculate();
	}

	private static Snapshot readSnapshot(String fileName, VerboseOutputCollector vc) {
		startTime0 = System.currentTimeMillis();
		System.err.println("Reading heap dump...");

		Snapshot snapshot = null;
		ReadBuffer.Factory bufFactory = useMmap ? new ReadBuffer.MmappedBufferFactory(fileName)
				: new ReadBuffer.CachedReadBufferFactory(fileName, 0);
		try {
			final HeapDumpReader reader = HeapDumpReader.createReader(bufFactory, explicitPointerSize, vc);
			ProgressMeter pm = new PrintingProgressMeter() {
				@Override
				public int queryPercentage() {
					return reader.getProgressPercentage();
				}
			};
			pm.start();

			snapshot = reader.read();

			pm.stopReporting();
		} catch (DumpCorruptedException ex) {
			System.err.println("Heap dump corrupted: " + ex.getMessage());
			List<String> warnings = vc.getWarnings();
			if (!warnings.isEmpty()) { // In verbose mode, print all warnings first
				System.err.println("The following warnings were recorded before the fatal error was detected:");
				for (String warning : warnings) {
					System.err.println(warning);
				}
			}
			List<String> debugInfo = vc.getDebugInfo();
			if (!debugInfo.isEmpty()) { // In verbose mode, print all warnings first
				System.err.println("Additional debug info:");
				for (String debug : debugInfo) {
					System.err.println(debug);
				}
			}

			System.exit(-1);
		} catch (HprofParsingCancelledException ex) {
			System.err.println("Parsing heap dump cancelled by user");
			System.exit(-1);
		}

		System.err.println("\rRead " + snapshot.getNumObjects() + " objects");

		startTime1 = System.currentTimeMillis();
		snapshotReadTimeMs = startTime1 - startTime0;
		System.err.println("Snapshot read time: " + (snapshotReadTimeMs / 1000) + " sec");

		return snapshot;
	}

	private static ReportFormatter calculateAndFormatStandardStats(Snapshot snapshot) {
		System.err.println("Calculating stats...");

		BatchProblemRecorder recorder = new BatchProblemRecorder();
		final StandardStatsCalculator ssc = new StandardStatsCalculator(snapshot, recorder, useBreadthFirst);

		ProgressMeter pm = new PrintingProgressMeter() {
			@Override
			public int queryPercentage() {
				return ssc.getProgressPercentage();
			}
		};
		pm.start();

		HeapStats hs = null;
		try {
			hs = ssc.calculate();
		} catch (DumpCorruptedException ex) {
			System.err.println("Heap dump corrupted: " + ex.getMessage());
			System.exit(-1);
		} catch (HprofParsingCancelledException ex) {
			System.err.println("Heap dump parsing cancelled by user");
			System.exit(-1);
		}

		pm.stopReporting();

		int minOverheadToReport = (int) (hs.totalObjSize / MIN_OVHD_TO_REPORT_AS_HEAP_FRACTION);
		DetailedStats ds = recorder.getDetailedStats(minOverheadToReport);

		return new ReportFormatter(hs, ds);
	}

	/**
	 * Parses a flag that's already known to start with "something=", like "foo=2". If what follows
	 * '=' is not a number, the method prints an error and invokes System.exit(-1).
	 */
	private static int parseNumericFlag(String flag) {
		String numStr = getFlagValue(flag);
		try {
			return Integer.parseInt(numStr);
		} catch (NumberFormatException ex) {
			System.err.println("No proper number in flag " + flag);
			System.exit(-1);
		}
		return 0; // Just to make the compiler happy
	}

	private static String[] parseCommaSeparatedStringsFlag(String flag) {
		String valStr = getFlagValue(flag);
		if (valStr.indexOf(',') == -1) {
			return new String[] {valStr};
		}

		return Utils.split(valStr, ',');
	}

	private static File parseFileNameFlag(String flag) {
		String fileName = getFlagValue(flag);
		return FileUtils.fileWritableOrExit(fileName);
	}

	private static String getFlagValue(String flag) {
		int equalsPos = flag.indexOf('=');
		if (equalsPos == -1 || equalsPos == flag.length() - 1) {
			System.err.println(flag + " is not a flag with value");
			System.exit(-1);
		}

		return flag.substring(equalsPos + 1);
	}

	private static long checkFileAndGetSize(String fileName) {
		File f = FileUtils.fileExistsAndReadableOrExit(fileName);
		return f.length();
	}

	private static abstract class PrintingProgressMeter extends ProgressMeter {

		@Override
		public void stopReporting() {
			super.stopReporting();
			System.err.print('\r');
		}

		@Override
		public void reportProgress(int progressPerecentage) {
			System.err.print('\r');
			System.err.print(progressPerecentage);
			System.err.print('%');
		}

	}
}
