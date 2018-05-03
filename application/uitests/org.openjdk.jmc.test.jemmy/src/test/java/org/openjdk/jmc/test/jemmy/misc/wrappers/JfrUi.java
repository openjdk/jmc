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
package org.openjdk.jmc.test.jemmy.misc.wrappers;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.openjdk.jmc.test.jemmy.TestHelper;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.helpers.EventSettingsData;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCButton.Labels;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable.TableRow;

/**
 * The Jemmy wrapper for the Flight Recorder UI
 */
public class JfrUi extends MCJemmyBase {
	public static final String END_TIME_COLUMN_HEADER = "End Time";
	public static final String SETTING_VALUE_COULMN_HEADER = "Setting Value";
	public static final String SETTING_NAME_COLUMN_HEADER = "Setting Name";
	public static final String SETTING_FOR_COLUMN_HEADER = "Setting For";

	/*
	 * Members ======= List of tabs
	 */
	public static enum Tabs {
		JAVA_APPLICATION,
		MEMORY,
		LOCK_INSTANCES,
		FILE_IO,
		SOCKET_IO,
		METHOD_PROFILING,
		EXCEPTIONS,
		THREAD_DUMPS,
		JVM_INTERNALS,
		GARBAGE_COLLECTIONS,
		GC_CONFIG,
		COMPILATIONS,
		CODE_CACHE,
		CLASS_LOADING,
		VM_OPERATIONS,
		ALLOCATIONS,
		ENVIRONMENT,
		PROCESSES,
		ENVIRONMENT_VARIABLES,
		SYSTEM_PROPS,
		RECORDING;

		public static String[] text(Tabs tab) {
			String[] tabText = {""};
			switch (tab) {
			case JVM_INTERNALS:
				tabText = new String[] {"JVM Internals"};
				break;
			case SYSTEM_PROPS:
				tabText = new String[] {"Environment", "System Properties"};
				break;
			case RECORDING:
				tabText = new String[] {"Environment", "Recording"};
				break;
			case GARBAGE_COLLECTIONS:
				tabText = new String[] {"JVM Internals", "Garbage Collections"};
				break;
			case GC_CONFIG:
				tabText = new String[] {"JVM Internals", "GC Configuration"};
				break;
			case ALLOCATIONS:
				tabText = new String[] {"JVM Internals", "TLAB Allocations"};
				break;
			case MEMORY:
				tabText = new String[] {"Java Application", "Memory"};
				break;
			case METHOD_PROFILING:
				tabText = new String[] {"Java Application", "Method Profiling"};
				break;
			case JAVA_APPLICATION:
				tabText = new String[] {"Java Application"};
				break;
			case EXCEPTIONS:
				tabText = new String[] {"Java Application", "Exceptions"};
				break;
			case COMPILATIONS:
				tabText = new String[] {"JVM Internals", "Compilations"};
				break;
			case CODE_CACHE:
				tabText = new String[] {"JVM Internals", "Compilations", "Code Cache"};
				break;
			case CLASS_LOADING:
				tabText = new String[] {"JVM Internals", "Class Loading"};
				break;
			case VM_OPERATIONS:
				tabText = new String[] {"JVM Internals", "VM Operations"};
				break;
			case THREAD_DUMPS:
				tabText = new String[] {"Java Application", "Thread Dumps"};
				break;
			case LOCK_INSTANCES:
				tabText = new String[] {"Java Application", "Lock Instances"};
				break;
			case FILE_IO:
				tabText = new String[] {"Java Application", "File I/O"};
				break;
			case SOCKET_IO:
				tabText = new String[] {"Java Application", "Socket I/O"};
				break;
			case ENVIRONMENT:
				tabText = new String[] {"Environment"};
				break;
			case PROCESSES:
				tabText = new String[] {"Environment", "Processes"};
				break;
			case ENVIRONMENT_VARIABLES:
				tabText = new String[] {"Environment", "Environment Variables"};
				break;
			default:
				break;

			}
			return tabText;
		}
	}

	/**
	 * Opens the file in Mission Control, waits for the editor to show up and the system to become idle (all
	 * rendering and rule calculation done)
	 *
	 * @param file
	 *            a file representing a flight recording
	 */
	public static void openJfr(File file) {
		openJfr(file, true);
	}

	/**
	 * Opens the file in Mission Control, optionally waits for the editor to show up and the system to become
	 * idle (all rendering and rule calculation done)
	 *
	 * @param file
	 *            a file representing a flight recording
	 * @param waitForEditor
	 *            {@code true} if supposed to wait for the editor before returning. Otherwise
	 *            will return immediately
	 */
	public static void openJfr(File file, boolean waitForEditor) {
		openJfr(file, true, waitForEditor);
	}

	/**
	 * Opens the file in Mission Control, waits for the editor to show up and the system to become idle (all
	 * rendering and rule calculation done)
	 *
	 * @param file
	 *            a file representing a flight recording
	 * @param handlePotentialDecompressionDialog
	 *            {@code true} if a (potential) decompression dialog should be handled by
	 *            clicking the Yes button. Otherwise won't check for the dialog
	 * @param waitForEditor
	 *            {@code true} if supposed to wait for the editor before returning. Otherwise
	 *            will return immediately
	 */
	public static void openJfr(File file, boolean handlePotentialDecompressionDialog, boolean waitForEditor) {
		TestHelper.openJfr(file.getAbsolutePath());

		if (handlePotentialDecompressionDialog) {
			String dialogText = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.FILE_OPENER_ZIPPED_FILE_TEXT;
			MCDialog decompressDialog = MCDialog.getByDialogTitleAndText(
					org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.FILE_OPENER_ZIPPED_FILE_TITLE,
					dialogText.replace("{0}", file.getName()).substring(0, dialogText.indexOf("{1}")));
			if (decompressDialog != null) {
				decompressDialog.clickButton(Labels.YES);
			}
		}

		if (waitForEditor) {
			Assert.assertTrue("Could not find JFR editor for file \"" + file.getName() + "\"",
					waitForSubstringMatchedEditor(30000, file.getName()));
		}
	}

	/**
	 * Switches to the Recording tab, parses the event settings table and returns an
	 * EventSettingsData object
	 *
	 * @return an EventSettingsData object with the settings of the currently opened recording
	 */
	public static EventSettingsData parseEventSettingsTable() {
		JfrNavigator.selectTab(Tabs.RECORDING);
		focusSectionByTitle("Event Settings", false);
		MCTable settingsTable = getTables(false).get(0);
		// Turning on the "End Time" column (if not already visible)
		Integer index = settingsTable.getColumnIndex(END_TIME_COLUMN_HEADER);
		if (index == null || index == -1) {
			// ensuring that one (any) table item is focused before trying to context choose
			settingsTable.click();
			settingsTable.contextChoose("Visible Columns", END_TIME_COLUMN_HEADER);
		}

		EventSettingsData settings = new EventSettingsData();
		List<TableRow> tableData = settingsTable.getRows();

		for (TableRow row : tableData) {
			String eventName = row.getText(SETTING_FOR_COLUMN_HEADER);
			String name = row.getText(SETTING_NAME_COLUMN_HEADER).replaceAll("[\\p{Z}]", " ");
			String value = row.getText(SETTING_VALUE_COULMN_HEADER).replaceAll("[\\p{Z}]", " ");
			String eventEndtime = row.getText(END_TIME_COLUMN_HEADER).replaceAll("[\\p{Z}]", " ");
			settings.add(eventName, eventEndtime, name, value);
		}
		return settings;
	}

	public static String getRangeNavigatorStartTime() {
		return MCLabel.getByName("navigator.start.time").getText();
	}

	public static String getRangeNavigatorEndTime() {
		return MCLabel.getByName("navigator.end.time").getText();
	}

	public static void clickRangeNavigatorButton(RangeNavigatorButtons button) {
		clickRangeNavigatorButton(button, 1);
	}

	public static void clickRangeNavigatorButton(RangeNavigatorButtons button, int times) {
		MCButton.getByName(RangeNavigatorButtons.name(button)).click(times);
	}

	public static boolean isRangeNavigatorButtonEnabled(RangeNavigatorButtons button) {
		return MCButton.getByName(RangeNavigatorButtons.name(button)).isEnabled();
	}

	public static enum RangeNavigatorButtons {
		ZOOMIN, ZOOMOUT, BACKWARD, FORWARD, ALL;

		public static String name(RangeNavigatorButtons button) {
			String result = "";
			switch (button) {
			case ZOOMIN:
				result = "navigator.zoom.in";
				break;
			case ZOOMOUT:
				result = "navigator.zoom.out";
				break;
			case BACKWARD:
				result = "navigator.move.backward";
				break;
			case FORWARD:
				result = "navigator.move.forward";
				break;
			case ALL:
				result = "navigator.select.all";
				break;
			default:
				break;
			}
			return result;
		}
	}

}
