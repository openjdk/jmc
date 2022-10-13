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
package org.openjdk.jmc.flightrecorder.test.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.test.TestToolkit;
import org.openjdk.jmc.common.test.io.IOResource;
import org.openjdk.jmc.common.test.io.IOResourceSet;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Branch;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Fork;

/**
 * Utility for presenting aggregates stack traces in textual form.
 */
// TODO: Add support for formatting different frame categorizations, sorting traces with root on top or bottom, and "distinguish on optimization"
@SuppressWarnings("nls")
public class StacktraceTestToolkit {
	private static final String STACKTRACE_DIRECTORY = "stacktraces";
	private static final String STACKTRACE_INDEXFILE = "index.txt";
	private static final FrameSeparator methodFrameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);

	/**
	 * Return the files that can be used for comparing old printouts.
	 *
	 * @return the test file need for comparing files.
	 * @throws IOException
	 *             if the files could not be located.
	 */
	public static IOResourceSet[] getTestResources() throws IOException {
		IOResourceSet recordings = RecordingToolkit.getRecordings();
		IOResourceSet stacktraces = getStackTraceBaselines();
		if (recordings.getResources().size() != stacktraces.getResources().size()) {
			throw new RuntimeException("The number of stacktraces baselines ( " + stacktraces.getResources().size()
					+ ") does not match the number of recording files (" + recordings.getResources().size());
		}
		List<IOResourceSet> list = new ArrayList<>();
		for (IOResource recordingfile : recordings) {
			IOResource stacktraceFile = stacktraces.findWithPrefix(recordingfile.getName());
			if (stacktraceFile == null) {
				throw new RuntimeException("Could not find stacktrace baseline file for " + recordingfile);
			}
			list.add(new IOResourceSet(recordingfile, stacktraceFile));
		}

		return list.toArray(new IOResourceSet[list.size()]);
	}

	/**
	 * Return the directory where the stacktrace baseline files reside.
	 *
	 * @return the printout file directory
	 * @throws IOException
	 *             if the directory could not be found
	 */
	public static File getStacktracesDirectory() throws IOException {
		return TestToolkit.getProjectDirectory(StacktraceTestToolkit.class, STACKTRACE_DIRECTORY);
	}

	private static IOResourceSet getStackTraceBaselines() throws IOException {
		return TestToolkit.getResourcesInDirectory(StacktraceTestToolkit.class, STACKTRACE_DIRECTORY,
				STACKTRACE_INDEXFILE);
	}

	/**
	 * Prints the aggregated stacktraces from of a recording to another file in text format.
	 *
	 * @param sourceFile
	 *            the source recording file
	 * @param destinationFile
	 *            the destination file for the printing.
	 */
	public static void printStacktraces(File sourceFile, File destinationFile)
			throws IOException, CouldNotLoadRecordingException {
		try (FileOutputStream output = new FileOutputStream(destinationFile);
				Writer writer = new OutputStreamWriter(output, RecordingToolkit.RECORDING_TEXT_FILE_CHARSET)) {
			IItemCollection events = JfrLoaderToolkit.loadEvents(sourceFile);
			for (String e : getAggregatedStacktraceLines(events, methodFrameSeparator)) {
				writer.append(e).append('\n');
			}
		}
	}

	public static List<String> getStacktracesBaseline(IOResourceSet resourceSet) throws IOException, Exception {
		return getLines(PrintoutsToolkit.stripHeader(StringToolkit.readString(resourceSet.getResource(1).open())));
	}

	/**
	 * Get lines, including empty lines represented by empty string.
	 */
	public static List<String> getLines(String content) {
		List<String> result = new ArrayList<>();
		Scanner scanner = new Scanner(content);
		while (scanner.hasNextLine()) {
			result.add(scanner.nextLine());
		}
		scanner.close();
		return result;
	}

	/**
	 * Gets the aggregated stack traces from an item collection as a list of strings.
	 *
	 * @param items
	 *            the item collection
	 * @return a list of strings representing the aggregated stack traces
	 */
	public static List<String> getAggregatedStacktraceLines(IItemCollection items) {
		return getAggregatedStacktraceLines(items, methodFrameSeparator);
	}

	/**
	 * Gets the aggregated stack traces grouped on frameCategorization from an item collection as a
	 * list of strings.
	 *
	 * @param items
	 *            the item collection
	 * @param frameCategorization
	 *            the frame categorization
	 * @return a list of strings representing the aggregated stack traces
	 */
	private static List<String> getAggregatedStacktraceLines(IItemCollection items, FrameSeparator frameSeparator) {
		return walkStacktraceTree(items, frameSeparator);
	}

	private static List<String> walkStacktraceTree(IItemCollection items, FrameSeparator frameSeparator) {
		StacktraceModel sModel = new StacktraceModel(false, frameSeparator, items);
		Fork root = sModel.getRootFork();
		List<String> traces = new ArrayList<>();
		return walkStacktraceTree(root, frameSeparator, "", traces);
	}

	private static List<String> walkStacktraceTree(
		Fork fork, FrameSeparator frameSeparator, String indent, List<String> traces) {
		for (Branch branch : fork.getBranches()) {
			handleBranch(branch, frameSeparator, indent, traces);
			Fork endFork = branch.getEndFork();
			walkStacktraceTree(endFork, frameSeparator, indent + "    ", traces);
			traces.add("");
		}
		return traces;
	}

	private static void handleBranch(Branch branch, FrameSeparator frameSeparator, String indent, List<String> traces) {
		StacktraceFrame sFrame = branch.getFirstFrame();
		traces.add(createFrameLine(frameSeparator, indent, sFrame));
		for (StacktraceFrame frame : branch.getTailFrames()) {
			traces.add(createFrameLine(frameSeparator, indent, frame));
		}
	}

	private static String createFrameLine(FrameSeparator frameSeparator, String indent, StacktraceFrame sFrame) {
		return indent + StacktraceFormatToolkit.formatFrame(sFrame.getFrame(), frameSeparator) + " ("
				+ sFrame.getItemCount() + ")";
	}

}
