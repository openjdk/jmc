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
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.test.TestToolkit;
import org.openjdk.jmc.common.test.io.IOResource;
import org.openjdk.jmc.common.test.io.IOResourceSet;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.RecordingPrinter;
import org.openjdk.jmc.flightrecorder.RecordingPrinter.Verbosity;

@SuppressWarnings("nls")
public class PrintoutsToolkit {
	private static final Pattern EVENT_PATTERN = Pattern.compile("<event [\\s\\S]*?</event>");
	private static final String PRINTOUTS_DIRECTORY = "printouts";
	private static final String PRINTOUTS_INDEXFILE = "index.txt";
	private static final String UNIX_LINE_SEPARATOR = "\n";
	static final String LICENSE_HEADER;

	static {
		String header = null;
		try {
			header = StringToolkit.readString(
					PrintoutsToolkit.class.getClassLoader().getResourceAsStream("license/license.txt"),
					RecordingToolkit.RECORDING_TEXT_FILE_CHARSET);
			// Make sure that newlines are always parsed as \n regardless of operating system
			header = header.replaceAll("\\r\\n", UNIX_LINE_SEPARATOR);
		} catch (IOException e) {
			throw new RuntimeException("License header not found!");
		}
		LICENSE_HEADER = header;
	}

	/**
	 * Return the files that can be used for comparing old printouts.
	 *
	 * @return the test file need for comparing files.
	 * @throws IOException
	 *             if the files could not be located.
	 */
	public static IOResourceSet[] getTestResources() throws IOException {
		IOResourceSet recordings = RecordingToolkit.getRecordings();
		IOResourceSet printouts = getPrintouts();
		if (recordings.getResources().size() != printouts.getResources().size()) {
			throw new RuntimeException("The number of printouts does not match the number of recording files.");
		}
		List<IOResourceSet> list = new ArrayList<>();
		for (IOResource recordinfile : recordings) {
			IOResource printoutFile = printouts.findWithPrefix(recordinfile.getName());
			if (printoutFile == null) {
				throw new RuntimeException("Could not find printout file for " + recordinfile);
			}
			list.add(new IOResourceSet(recordinfile, printoutFile));
		}

		return list.toArray(new IOResourceSet[list.size()]);
	}

	/**
	 * Return the directory where the printout files reside.
	 *
	 * @return the printout file directory
	 * @throws IOException
	 *             if the directory could not be found
	 */
	public static File getPrintoutDirectory() throws IOException {
		return TestToolkit.getProjectDirectory(PrintoutsToolkit.class, PRINTOUTS_DIRECTORY);
	}

	private static IOResourceSet getPrintouts() throws IOException {
		return TestToolkit.getResourcesInDirectory(PrintoutsToolkit.class, PRINTOUTS_DIRECTORY, PRINTOUTS_INDEXFILE);
	}

	/**
	 * Prints the contents of a recording to another file in text format.
	 *
	 * @param sourceFile
	 *            the source recording file
	 * @param destinationFile
	 *            the destination file for the printing.
	 */
	public static void printRecording(File sourceFile, File destinationFile)
			throws IOException, CouldNotLoadRecordingException {
		try (FileOutputStream output = new FileOutputStream(destinationFile);
				Writer writer = new OutputStreamWriter(output, RecordingToolkit.RECORDING_TEXT_FILE_CHARSET)) {
			writer.append(LICENSE_HEADER);
			IItemCollection events = JfrLoaderToolkit.loadEvents(sourceFile);
			for (String e : getEventsAsStrings(events)) {
				writer.append(e).append('\n');
			}
		}
	}

	public static List<String> getEventsAsStrings(IItemCollection items) throws IOException {
		List<String> events = new ArrayList<>();
		Iterator<IItemIterable> itemIterable = items.iterator();
		while (itemIterable.hasNext()) {
			Iterator<IItem> itemIterator = itemIterable.next().iterator();
			while (itemIterator.hasNext()) {
				IItem item = itemIterator.next();
				StringWriter writer = new StringWriter();
				// Make sure that newlines are always printed with \n regardless of operating system
				PrintWriter unixNewlineWriter = new PrintWriter(writer) {
					@Override
					public void println() {
						print(UNIX_LINE_SEPARATOR);
					}
				};
				RecordingPrinter printer = new RecordingPrinter(unixNewlineWriter, Verbosity.HIGH, false);
				printer.printEvent(item);
				events.add(writer.toString());
			}
		}
		Collections.sort(events);
		return events;
	}

	public static List<String> getEventsFromPrintout(IOResourceSet resourceSet) throws IOException, Exception {
		List<String> events = new ArrayList<>();
		try (InputStream is = resourceSet.getResource(1).open()) {
			String baseline = StringToolkit.readString(is, RecordingToolkit.RECORDING_TEXT_FILE_CHARSET);
			baseline = stripHeader(baseline);
			try (Scanner scanner = new Scanner(baseline)) {
				String eventAsText = scanner.findWithinHorizon(EVENT_PATTERN, 0);
				while (eventAsText != null) {
					events.add(eventAsText);
					eventAsText = scanner.findWithinHorizon(EVENT_PATTERN, 0);
				}
			}
		}
		return events;
	}

	public static void main(String[] args) throws IOException, CouldNotLoadRecordingException {
		printRecording(new File(args[0]), new File(args[1]));
	}

	static String stripHeader(String baseline) throws Exception {
		// Make sure that newlines are always parsed as \n regardless of operating system
		baseline = baseline.replaceAll("\\r\\n", UNIX_LINE_SEPARATOR);
		if (!baseline.startsWith(LICENSE_HEADER)) {
			throw new Exception("No license header in baseline!");
		}
		return baseline.substring(LICENSE_HEADER.length());
	}
}
