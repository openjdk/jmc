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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;

import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrNavigator;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrUi;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable.TableRow;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCText;

/**
 * Class for flight recorder component related testing.
 */
public class JfrComponentTests extends MCJemmyTestBase {
	private static final String SEMI_COLON = ";";
	private static final String TAB = "\t";
	private static final String LINEBREAK = "\n";
	private static String CLIPBOARD_SETTINGS = "Clipboard Settings";
	private static String[] COPY_RAW = new String[] {CLIPBOARD_SETTINGS, "Copy Raw"};
	private static String[] COPY_CSV = new String[] {CLIPBOARD_SETTINGS, "Copy as CSV"};
	private static String[] COPY_VISIBLE = new String[] {CLIPBOARD_SETTINGS, "Copy Visible"};
	private static String[] COPY_HEADERS = new String[] {CLIPBOARD_SETTINGS, "Copy Column Headers"};
	private static String[] END_TIME_COLUMN_VISIBILITY = new String[] {"Visible Columns", "End Time"};
	private static String[] events = new String[] {"Class Load", "File Write", "Java Thread Statistics", "Thread Dump"};

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			MCMenu.minimizeStackTraceView();
			// open the java2d_demo.jfr file
			JfrUi.openJfr(materialize("jfr", "plain_recording.jfr", JfrComponentTests.class));
		}

		@Override
		public void after() {
			MCMenu.closeActiveEditor();
			MCMenu.restoreStackTraceView();
		}
	};

	/**
	 * Copies a row in the raw format and verifies that this matches what is shown in the table
	 * (with conversion)
	 */
	@Test
	public void copyRawFromRecordingTable() {
		// get the recording table
		MCTable table = getRecordingSettingsTable();
		// turn on the End Time column
		table.contextChoose(true, END_TIME_COLUMN_VISIBILITY);
		// ensure that we still have a selection in the table (may disappear when adding/removing columns)
		table.select(0);
		// ensure that options are set
		table.contextChoose(true, COPY_RAW);
		table.contextChoose(false, COPY_CSV);
		table.contextChoose(false, COPY_VISIBLE);
		table.contextChoose(true, COPY_HEADERS);
		// do the actual verification of the copy operations
		String result = doCopyVerification(table);
		Assert.assertNull(result, result);
	}

	/**
	 * Copies a row in CSV format and verifies that this matches what is shown in the table
	 */
	@Test
	public void copyCsv() {
		MCTable table = getRecordingSettingsTable();
		// ensure that options are set
		table.contextChoose(true, COPY_RAW);
		table.contextChoose(true, COPY_CSV);
		table.contextChoose(false, COPY_VISIBLE);
		table.contextChoose(true, COPY_HEADERS);
		// do the actual verification of the copy operations
		String result = doCopyVerification(table);
		Assert.assertNull(result, result);
	}

	/**
	 * Copies the visible columns of a selected row and verifies that this matches what is shown in
	 * the table
	 */
	@Test
	public void copyVisible() {
		MCTable table = getRecordingSettingsTable();
		// ensure that options are set
		table.contextChoose(true, COPY_RAW);
		table.contextChoose(false, COPY_CSV);
		table.contextChoose(true, COPY_VISIBLE);
		table.contextChoose(true, COPY_HEADERS);
		// do the actual verification of the copy operations
		String result = doCopyVerification(table);
		Assert.assertNull(result, result);
	}

	/**
	 * Copies a text field and verifies that the clipboard contents are the same as the text value
	 * in the control
	 */
	@Test
	public void copyTextField() {
		Assume.assumeFalse("Skipping on Linux", MCJemmyBase.OS_NAME.contains("linux"));
		for (JfrUi.Tabs tab : new JfrUi.Tabs[] {JfrUi.Tabs.JVM_INTERNALS, JfrUi.Tabs.GC_CONFIG,
				JfrUi.Tabs.ENVIRONMENT}) {
			JfrNavigator.selectTab(tab);
			for (MCText textControl : MCText.getVisible()) {
				String textValue = normalizeString(textControl.getText());
				if (!"".equals(textValue)) {
					String clipboardValue = "";
					int maxRetries = 5;
					boolean textsAreEqual = false;
					while (!textsAreEqual && maxRetries > 0) {
						maxRetries--;
						textControl.copyToClipboard();
						clipboardValue = normalizeString(MCJemmyBase.getStringFromClipboard());
						textsAreEqual = textValue.equals(clipboardValue);
					}
					Assert.assertTrue("Not equal text values: \"" + textValue + "\" and \"" + clipboardValue + "\"",
							textsAreEqual);
				}
			}
		}
	}

	private String normalizeString(String input) {
		return input.replaceAll("\\r\\n", "\n");
	}

	private String doCopyVerification(MCTable table) {
		boolean isCsv = table.getContextOptionState(COPY_CSV);
		boolean hasHeaders = table.getContextOptionState(COPY_HEADERS);
		String errorMsg = null;
		// for each event, copy and verify the data
		for (String event : events) {
			table.select(event);
			// copy the row from the table-UI
			TableRow uITableRow = extractRow(table, JfrUi.SETTING_FOR_COLUMN_HEADER, event);
			// copy the selected row to the clipboard
			table.contextChoose("Copy");
			// get the contents of the clipboard
			String clipboardBuffer = MCJemmyBase.getStringFromClipboard();
			// get the column headers
			List<String> columnHeaders = getColumnNameList(uITableRow);
			// parse the clipboard
			List<String> clipboardData = parseLineData(isCsv, hasHeaders, clipboardBuffer);

			// compare the results, set an iteration limit depending on what can be seen in the UI (and thus verified)
			int columns = (uITableRow.getColumns().size() < clipboardData.size()) ? uITableRow.getColumns().size()
					: clipboardData.size();
			for (int index = 0; index < columns && errorMsg == null; index++) {
				String header = columnHeaders.get(index);
				String tableValue = uITableRow.getText(index);
				String clipboardValue = clipboardData.get(index);
				if (compare(header, tableValue, clipboardValue) != 0) {
					errorMsg = "Value equality check failed for event: " + event + ", column " + header
							+ ". Table value: \"" + tableValue + "\", Clipboard value: \"" + clipboardValue + "\"";
				}
			}
			// finish the outer loop as well if comparison didn't pass
			if (errorMsg != null) {
				break;
			}
		}
		return errorMsg;
	}

	private int compare(String header, String tableValue, String clipboardValue) {
		// initialize to a "bad" value, 0 is good
		int compareValue = 99;
		if (!tableValue.equals(clipboardValue)) {
			if (JfrUi.END_TIME_COLUMN_HEADER.equals(header)) {
				// time stamp kind of data to be compared
				try {
					compareValue = UnitLookup.TIMESTAMP.parsePersisted(clipboardValue).in(UnitLookup.EPOCH_S)
							.compareTo(UnitLookup.TIMESTAMP.parseInteractive(tableValue).in(UnitLookup.EPOCH_S));
				} catch (QuantityConversionException e) {
					System.out.println("Problem parsing the timestamp of one or both of the strings: \""
							+ clipboardValue + "\" and \"" + tableValue + "\"");
					e.printStackTrace();
				}
			} else {
				// ordinary strings
				compareValue = (tableValue.equals(clipboardValue)) ? 0 : 1;
			}
		} else {
			compareValue = 0;
		}
		return compareValue;
	}

	private TableRow extractRow(MCTable table, String columnName, String value) {
		List<TableRow> allRows = table.getRows();
		TableRow row = null;
		for (TableRow thisRow : allRows) {
			if (thisRow.getText(columnName).equals(value)) {
				row = thisRow;
				break;
			}
		}
		return row;
	}

	private MCTable getRecordingSettingsTable() {
		JfrNavigator.selectTab(JfrUi.Tabs.RECORDING);
		MCJemmyBase.focusSectionByTitle("Event Settings", false);
		MCTable table = MCJemmyBase.getTables(false).get(0);
		// select the first line to ensure that something is selected if we want to perform a context click operation
		table.select(0);
		return table;
	}

	private List<String> parseLineData(boolean isCsv, boolean hasHeader, String clipboardBuffer) {
		Assert.assertTrue("Header info expected while clipboard buffer only contains one line",
				clipboardBuffer.split(LINEBREAK).length > 1);
		if (hasHeader) {
			return parseLine(isCsv, getSecondClipboardLine(clipboardBuffer));
		} else {
			return parseLine(isCsv, getFirstClipboardLine(clipboardBuffer));
		}
	}

	private List<String> getColumnNameList(TableRow tr) {
		Map<Integer, String> reversedMap = new HashMap<>();
		tr.getColumnNameMap().forEach((s, i) -> reversedMap.put(i, s));
		List<String> result = new ArrayList<>();
		for (int i = 0; i < reversedMap.size(); i++) {
			result.add(reversedMap.get(i));
		}
		return result;
	}

	private String getFirstClipboardLine(String clipboardBuffer) {
		return getLine(clipboardBuffer, 0);
	}

	private String getSecondClipboardLine(String clipboardBuffer) {
		return getLine(clipboardBuffer, 1);
	}

	private String getLine(String clipboardBuffer, int lineIndex) {
		List<String> result = tokenize(clipboardBuffer, LINEBREAK);
		if (result.size() > lineIndex) {
			return result.get(lineIndex);
		} else {
			return null;
		}
	}

	private List<String> parseLine(boolean isCsv, String line) {
		List<String> result;
		if (isCsv) {
			result = new ArrayList<>();
			// removing the first and last characters of each string (")
			tokenize(line, SEMI_COLON).stream().forEach(string -> result.add(string.substring(1, string.length() - 1)));
		} else {
			result = tokenize(line, TAB);
		}
		return result;
	}

	private List<String> tokenize(String line, String delimiter) {
		List<String> result = new ArrayList<>();
		String[] tokens = line.split(delimiter);
		for (String token : tokens) {
			result.add(token.trim());
		}
		return result;
	}
}
