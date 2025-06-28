/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.test.util.PrintoutsToolkit;
import org.openjdk.jmc.flightrecorder.test.util.StacktraceTestToolkit;
import org.openjdk.jmc.test.TestToolkit;

@SuppressWarnings("nls")
public class StacktraceBaselineGenerator {
	public static void main(String[] args) throws URISyntaxException, IOException, CouldNotLoadRecordingException {
		generatePrintouts(true); // Generate unfiltered printouts (show hidden frames)
		generatePrintouts(false); // Generate filtered printouts (hide hidden frames)
		generateStacktraceBaselines(true); // Generate unfiltered stacktrace baselines
		generateStacktraceBaselines(false); // Generate filtered stacktrace baselines
	}

	private static void generatePrintouts(boolean showHiddenFrames)
			throws URISyntaxException, IOException, CouldNotLoadRecordingException {
		String directoryName = showHiddenFrames ? "printouts" : "filtered-printouts";
		String description = showHiddenFrames ? "unfiltered printouts" : "filtered printouts (hidden frames excluded)";

		// Get the project directory and construct the full path to resources
		File projectDirectory = TestToolkit.getProjectDirectory(StacktraceBaselineGenerator.class, "src");
		File printoutsDirectory = new File(projectDirectory, "main/resources/" + directoryName);
		File recordingDirectory = new File(projectDirectory, "main/resources/recordings");

		// Read excluded recordings
		Set<String> excludedRecordings = getExcludedRecordings();

		// Create the directory if it doesn't exist
		if (!printoutsDirectory.exists()) {
			printoutsDirectory.mkdirs();
		}

		System.out.println("Deleting all files in directory " + printoutsDirectory);
		if (printoutsDirectory.exists() && printoutsDirectory.isDirectory()) {
			File[] files = printoutsDirectory.listFiles();
			if (files != null) {
				for (File file : files) {
					if (!file.delete()) {
						System.out.println("Could not remove old files!\nExiting!");
						System.exit(1);
					}
				}
			}
		}

		System.out.println("Recording directory: " + recordingDirectory);
		File[] recordingFiles = recordingDirectory.listFiles();
		if (recordingFiles == null) {
			System.out.println("No recording files found");
			return;
		}

		for (File recordingFile : recordingFiles) {
			if (recordingFile.isFile() && recordingFile.getName().endsWith(".jfr")) {
				if (excludedRecordings.contains(recordingFile.getName())) {
					System.out.println("Skipping " + recordingFile.getName() + " (excluded recording)");
					continue;
				}
				File printoutFile = new File(printoutsDirectory, recordingFile.getName() + ".txt");
				System.out.println("Generating " + description + " for " + recordingFile.getName() + " ...");
				PrintoutsToolkit.printRecordingWithFrameFiltering(recordingFile, printoutFile, showHiddenFrames);
				System.out.println(" finished!");
			}
		}

		// Create index.txt file for resource loading - only include recordings we actually processed
		File indexFile = new File(printoutsDirectory, "index.txt");
		try (java.io.PrintWriter writer = new java.io.PrintWriter(indexFile)) {
			// Write header comments
			if (showHiddenFrames) {
				writer.println("# Unfiltered printout baselines (hidden frames shown)");
			} else {
				writer.println("# Filtered printout baselines (hidden frames excluded)");
			}
			writer.println("# Lines starting with # are comments and will be ignored");
			writer.println();

			// Write core JDK version recordings
			writer.println("# Core JDK version printouts");
			for (File recordingFile : recordingFiles) {
				if (recordingFile.isFile() && recordingFile.getName().endsWith(".jfr")
						&& isCoreJdkRecording(recordingFile.getName())
						&& !excludedRecordings.contains(recordingFile.getName())) {
					writer.println(recordingFile.getName() + ".txt");
				}
			}
			writer.println();

			// Write special parser test recordings
			writer.println("# Special parser test printouts");
			for (File recordingFile : recordingFiles) {
				if (recordingFile.isFile() && recordingFile.getName().endsWith(".jfr")
						&& !isCoreJdkRecording(recordingFile.getName())
						&& !excludedRecordings.contains(recordingFile.getName())) {
					writer.println(recordingFile.getName() + ".txt");
				}
			}
			writer.println();

			// Write edge case recordings (including excluded ones)
			writer.println("# Edge case test printouts");
			for (File recordingFile : recordingFiles) {
				if (recordingFile.isFile() && recordingFile.getName().endsWith(".jfr")
						&& excludedRecordings.contains(recordingFile.getName())) {
					writer.println(recordingFile.getName() + ".txt");
				}
			}
		}
		System.out.println("Created " + directoryName + " index file: " + indexFile);

		// Create exclude.txt file
		File excludeFile = new File(printoutsDirectory, "exclude.txt");
		try (java.io.PrintWriter writer = new java.io.PrintWriter(excludeFile)) {
			if (showHiddenFrames) {
				writer.println("# Printout baselines excluded from testing");
			} else {
				writer.println("# Filtered printout baselines excluded from testing");
			}
			writer.println("# These correspond to excluded recordings");
			writer.println();
			writer.println("# No events in source recording - causes test failures");
			for (String excludedRecording : excludedRecordings) {
				writer.println(excludedRecording + ".txt");
			}
		}
		System.out.println("Created " + directoryName + " exclude file: " + excludeFile);
	}

	private static void generateStacktraceBaselines(boolean showHiddenFrames)
			throws URISyntaxException, IOException, CouldNotLoadRecordingException {
		String directoryName = showHiddenFrames ? "stacktraces" : "filtered-stacktraces";
		String description = showHiddenFrames ? "stacktrace baselines"
				: "filtered stacktrace baselines (hidden frames excluded)";

		// Get the project directory and construct the full path to resources
		File projectDirectory = TestToolkit.getProjectDirectory(StacktraceBaselineGenerator.class, "src");
		File stacktracesDirectory = new File(projectDirectory, "main/resources/" + directoryName);
		File recordingDirectory = new File(projectDirectory, "main/resources/recordings");

		// Read excluded recordings
		Set<String> excludedRecordings = getExcludedRecordings();

		// Create the directory if it doesn't exist
		if (!stacktracesDirectory.exists()) {
			stacktracesDirectory.mkdirs();
		}

		System.out.println("Deleting all files in directory " + stacktracesDirectory);
		if (stacktracesDirectory.exists() && stacktracesDirectory.isDirectory()) {
			File[] files = stacktracesDirectory.listFiles();
			if (files != null) {
				for (File file : files) {
					if (!file.delete()) {
						System.out.println("Could not remove old files!\nExiting!");
						System.exit(1);
					}
				}
			}
		}

		System.out.println("Recording directory: " + recordingDirectory);
		File[] recordingFiles = recordingDirectory.listFiles();
		if (recordingFiles == null) {
			System.out.println("No recording files found");
			return;
		}

		for (File recordingFile : recordingFiles) {
			if (recordingFile.isFile() && recordingFile.getName().endsWith(".jfr")) {
				if (excludedRecordings.contains(recordingFile.getName())) {
					System.out.println("Skipping " + recordingFile.getName() + " (excluded recording)");
					continue;
				}
				File stacktraceFile = new File(stacktracesDirectory, recordingFile.getName() + ".txt");
				System.out.println("Generating " + description + " for " + recordingFile.getName() + " ...");
				StacktraceTestToolkit.printStacktracesWithFrameFiltering(recordingFile, stacktraceFile,
						showHiddenFrames);
				System.out.println(" finished!");
			}
		}

		// Create index.txt file for resource loading
		File stacktraceIndexFile = new File(stacktracesDirectory, "index.txt");
		try (java.io.PrintWriter writer = new java.io.PrintWriter(stacktraceIndexFile)) {
			// Write header comments
			if (showHiddenFrames) {
				writer.println("# Stacktrace baselines for structure validation");
			} else {
				writer.println("# Filtered stacktrace baselines (hidden frames excluded)");
			}
			writer.println("# Lines starting with # are comments and will be ignored");
			writer.println();

			// Write core JDK version recordings
			writer.println("# Core JDK version stacktraces");
			for (File recordingFile : recordingFiles) {
				if (recordingFile.isFile() && recordingFile.getName().endsWith(".jfr")
						&& isCoreJdkRecording(recordingFile.getName())
						&& !excludedRecordings.contains(recordingFile.getName())) {
					writer.println(recordingFile.getName() + ".txt");
				}
			}
			writer.println();

			// Write special parser test recordings, that can also still be used for general parser validation and testing
			writer.println("# Special parser test stacktraces");
			for (File recordingFile : recordingFiles) {
				if (recordingFile.isFile() && recordingFile.getName().endsWith(".jfr")
						&& !isCoreJdkRecording(recordingFile.getName())
						&& !excludedRecordings.contains(recordingFile.getName())) {
					writer.println(recordingFile.getName() + ".txt");
				}
			}
			writer.println();

			// Write edge case recordings (including excluded ones). These are the recordings that should not be used for general parser validation and testing
			writer.println("# Edge case test stacktraces");
			for (File recordingFile : recordingFiles) {
				if (recordingFile.isFile() && recordingFile.getName().endsWith(".jfr")
						&& excludedRecordings.contains(recordingFile.getName())) {
					writer.println(recordingFile.getName() + ".txt");
				}
			}
		}
		System.out.println("Created " + directoryName + " index file: " + stacktraceIndexFile);

		// Create exclude.txt file
		File excludeFile = new File(stacktracesDirectory, "exclude.txt");
		try (java.io.PrintWriter writer = new java.io.PrintWriter(excludeFile)) {
			if (showHiddenFrames) {
				writer.println("# Stacktrace baselines excluded from testing");
			} else {
				writer.println("# Filtered stacktrace baselines excluded from testing");
			}
			writer.println("# These correspond to excluded recordings");
			writer.println();
			writer.println("# No events in source recording - causes test failures");
			for (String excludedRecording : excludedRecordings) {
				writer.println(excludedRecording + ".txt");
			}
		}
		System.out.println("Created " + directoryName + " exclude file: " + excludeFile);
	}

	private static Set<String> getExcludedRecordings() throws IOException {
		try {
			File projectDirectory = TestToolkit.getProjectDirectory(StacktraceBaselineGenerator.class, "src");
			File excludeFile = new File(projectDirectory, "main/resources/recordings/exclude.txt");
			Set<String> excluded = new HashSet<>();

			if (excludeFile.exists()) {
				try (Scanner scanner = new Scanner(excludeFile)) {
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine().trim();
						if (!line.isEmpty() && !line.startsWith("#")) {
							excluded.add(line);
						}
					}
				}
			}
			return excluded;
		} catch (Exception e) {
			System.out
					.println("Warning: Could not read exclude file, proceeding without exclusions: " + e.getMessage());
			return new HashSet<>();
		}
	}

	private static boolean isCoreJdkRecording(String fileName) {
		return fileName.matches("^\\d+u\\d+\\.jfr$");
	}
}
