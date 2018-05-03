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
package org.openjdk.jmc.flightrecorder.uitest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

import org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards.RecordingWizardPage;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.helpers.ConnectionHelper;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrWizard;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;

/**
 * Base Class for testing JFR metadata related stuff
 */
public abstract class MetadataTestBase extends MCJemmyTestBase {
	static String ATTR_SEPARATOR = "#";
	static String ATTR_DELIMITER = ",";
	private static final String BASELINE_JFR_FILE = "MetadataBaseline_";
	protected static final String BASELINE_JFR_FILE_SUFFIX = ".jfr";
	protected static final String BASELINE_TXT_FILE_SUFFIX = ".txt";
	protected static final String JFR_FOLDER = "jfr";
	protected final String BUTTON_FINISH = "Finish";
	protected JfrWizard wizardShell = null;
	protected static final String RECORDINGWIZARD_DURATION = RecordingWizardPage.COMPONENT_ID + ".duration"; // org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards.RecordingWizardPage.createDurationText()
	protected File currentJfrFile;
	protected ArrayList<String> errors;

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			errors = new ArrayList<>();
		}
	};

	@Test
	public void testMetadata() {
		Assume.assumeTrue("This feature is only valid on JDK8u0 or later.",
				ConnectionHelper.is8u0orLater(TEST_CONNECTION));
		doRecording();

		if (ConnectionHelper.is9u0EAorLater(TEST_CONNECTION)) {
			handleRecording(BASELINE_JFR_FILE + "9");
		} else {
			handleRecording(BASELINE_JFR_FILE + "8");
		}
	}

	protected abstract void handleRecording(String filename);

	protected String prepareFailure() {
		File destFile = new File(getResultDir().getAbsolutePath() + File.separator + JFR_FOLDER + File.separator
				+ "Metadata_failing.jfr");
		copyFile(currentJfrFile, destFile);
		StringBuilder output = new StringBuilder();
		for (String s : errors) {
			output.append(s + "\n");
		}
		return output.toString();
	}

	protected void doComparison(
		SortedMap<String, SortedMap<String, String>> first, SortedMap<String, SortedMap<String, String>> second,
		boolean storedFirst) {
		for (String eventName : first.keySet()) {
			Map<String, String> firstFieldsMap = first.get(eventName);
			Map<String, String> secondFieldsMap = second.get(eventName);

			if (secondFieldsMap != null) {
				for (String eventAttrName : firstFieldsMap.keySet()) {
					String firstEventField = firstFieldsMap.get(eventAttrName);
					String secondEventField = secondFieldsMap.get(eventAttrName);

					if (secondEventField != null) {
						if (!firstEventField.equals(secondEventField)) {
							addEventAttrError(eventName, eventAttrName, eventAttrName, firstEventField,
									secondEventField, storedFirst);
						}
					} else {
						if (storedFirst) {
							errors.add("Attribute \"" + eventAttrName + "\" in event metadata for \"" + eventName
									+ "\" was not found in current recording");
						} else {
							errors.add("Attribute \"" + eventAttrName + "\" in event metadata for \"" + eventName
									+ "\" was not found in baseline recording");
						}
					}
				}
			} else {
				if (storedFirst) {
					errors.add("Event metadata \"" + eventName + "\" was not found in current recording.");
				} else {
					errors.add("Event metadata \"" + eventName + "\" was not found in baseline recording.");
				}
			}
		}
	}

	private void addEventAttrError(
		String eventName, String eventAttrName, String field, String first, String second, boolean storedFirst) {
		StringBuilder sb = new StringBuilder("Event (" + eventName + ") metadata for attribute \"" + eventAttrName
				+ "\" content type mismatch.\nIn ");
		if (storedFirst) {
			sb.append("baseline recording: \"" + first + "\". In current recording: \"" + second + "\"");
		} else {
			sb.append("current recording: \"" + first + "\". In baseline recording: \"" + second + "\"");
		}
		errors.add(sb.toString());
	}

	private void doRecording() {
		wizardShell = MC.jvmBrowser.startFlightRecordingWizard();
		wizardShell.setDurationText("1 s");
		wizardShell.disableEvent("Java Virtual Machine", "Initial System Property");
		wizardShell.disableEvent("Operating System", "Initial Environment Variable");
		wizardShell.disableEvent("Operating System", "System Process");
		currentJfrFile = new File(wizardShell.getCompleteFilePath());
		wizardShell.startAndWaitForRecordingEditor();
	}

	protected void copyFile(File sourceFile, File destFile) {
		prepareFile(destFile);
		try (FileChannel source = new FileInputStream(sourceFile).getChannel();
				FileChannel destination = new FileOutputStream(destFile).getChannel()) {
			destination.transferFrom(source, 0, source.size());
		} catch (IOException e) {
			e.printStackTrace();
			fail("Error copying file \"" + sourceFile.getAbsolutePath() + "\" to \"" + destFile.getAbsolutePath()
					+ "\". Error:\n" + e.getMessage());
		}
	}

	protected static void fail(String message) {
		System.out.println(message);
		Assert.fail(message);
	}

	protected static void writeMapToFile(SortedMap<String, SortedMap<String, String>> map, File file) {
		prepareFile(file);
		try (PrintStream ps = new PrintStream(new FileOutputStream(file))) {
			JfrMetadataToolkit.writeMap(map, ps);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Error writing to file \"" + file.getAbsolutePath() + "\". Error:\n" + e.getMessage());
		}
	}

	private static void prepareFile(File file) {
		if (file.exists()) {
			file.delete();
		}

		File parent = file.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			fail("Error creating file \"" + file.getAbsolutePath() + "\". Error:\n" + e.getMessage());
		}
	}

	protected SortedMap<String, SortedMap<String, String>> parseTextFile(File file) {
		SortedMap<String, SortedMap<String, String>> eventTypeMap = new TreeMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line = br.readLine();
			while (line != null) {
				String[] tokens = line.split(ATTR_DELIMITER);
				if (tokens.length > 0) {
					SortedMap<String, String> attributes = new TreeMap<>();
					for (int i = 1; i < tokens.length; i++) {
						String[] attribute = tokens[i].split(ATTR_SEPARATOR);
						attributes.put(attribute[0], attribute[1]);
					}
					eventTypeMap.put(tokens[0], attributes);
				}
				line = br.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			fail("Error parsing the text file \"" + file.getAbsolutePath() + "\" Error: " + e.getMessage());
		}
		return eventTypeMap;
	}
}
