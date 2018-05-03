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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openjdk.jmc.flightrecorder.ui.RecordingLoader;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrNavigator;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrUi;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCButton.Labels;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCDialog;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;

/**
 * Class for testing Flight Recording editor related stuff
 */
public class JfrRecordingTest extends MCJemmyTestBase {
	private static int initialZipFileMemoryFactor;
	private static final int EXTREME_ZIPFILE_MEMORY_FACTOR = 1000000;
	private static final int LOW_ZIPFILE_MEMORY_FACTOR = 4;
	private static final String RECORDING = "8u60.jfr";
	private static final String JROCKIT_RECORDING = "transitions_R28.2.jfr";
	private static final String JROCKIT_JFR_HEADER = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.FILE_OPENER_JROCKIT_TITLE;
	private static final String JROCKIT_JFR_TEXT = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.FILE_OPENER_JROCKIT_TEXT;

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void after() {
			MCMenu.closeActiveEditor();
		}
	};

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			initialZipFileMemoryFactor = RecordingLoader.getZippedFileMemoryFactor();
		}

		@Override
		public void after() {
			RecordingLoader.setZippedFileMemoryFactor(initialZipFileMemoryFactor);
		}
	};

	/**
	 * Verifies that Mission Control correctly displays an error message when trying to open a
	 * legacy recording (JRockit)
	 */
	@Test
	public void verifyNotSupportedMessage() {
		// Open recording, verify dialog
		openCompressedRecording(JROCKIT_RECORDING, initialZipFileMemoryFactor, false);
		MCDialog jrockitDialog = MCDialog.getByDialogTitleAndText(JROCKIT_JFR_HEADER, JROCKIT_JFR_TEXT);
		Assert.assertNotNull("Could not find a dialog with the title '" + JROCKIT_JFR_HEADER + "' and the text '"
				+ JROCKIT_JFR_TEXT + "'", jrockitDialog);
		jrockitDialog.clickButton(Labels.OK);
	}

	/**
	 * Testing opening a compressed recording that isn't supposed to generate any dialogs regarding
	 * unpacking of the file (unless JMC/Jemmy leaks a lot of memory and causes memory shortage).
	 */
	@Test
	public void verifySmallCompressedRecording() {
		openCompressedRecording(RECORDING, LOW_ZIPFILE_MEMORY_FACTOR, false);
		Assert.assertTrue("Unable to find an opened recording in JMC",
				MCJemmyBase.waitForEditor(30000, RECORDING));
	}

	/**
	 * Testing opening a compressed recording that is supposed to generate dialogs regarding
	 * unpacking of the file (by means of setting the RecordingLoader.zippedFileMemoryFactor to an
	 * extreme value)
	 */
	@Test
	public void verifyLargeCompressedRecording() {
		openCompressedRecording(RECORDING, EXTREME_ZIPFILE_MEMORY_FACTOR, false);
		// removing all of the target file stuff from the dialog text
		String dialogText = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.FILE_OPENER_ZIPPED_FILE_TEXT;
		String truncatedDialogText = dialogText.replace("{0}", RECORDING).substring(0,
				dialogText.indexOf("{1}"));
		MCDialog decompressDialog = MCDialog.getByDialogTitleAndText(
				org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.FILE_OPENER_ZIPPED_FILE_TITLE,
				truncatedDialogText);
		Assert.assertNotNull("Could not find a dialog with the title '"
				+ org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.FILE_OPENER_ZIPPED_FILE_TITLE
				+ "' and the text '" + truncatedDialogText + "'", decompressDialog);
		decompressDialog.clickButton(Labels.YES);
		Assert.assertTrue("Unable to find an opened recording in JMC",
				MCJemmyBase.waitForEditor(30000, RECORDING));
	}

	/**
	 * Open a recording and make sure that all tabs are accessible
	 */
	@Test
	public void verifyNormalRecordingTabTraversal() {
		openCompressedRecording(RECORDING, initialZipFileMemoryFactor, true);
		for (JfrUi.Tabs tabName : Arrays.asList(JfrUi.Tabs.values())) {
			JfrNavigator.selectTab(tabName);
		}
	}

	private void openCompressedRecording(String fileName, int zippedFileMemoryFactor, boolean waitForEditor) {
		RecordingLoader.setZippedFileMemoryFactor(zippedFileMemoryFactor);
		JfrUi.openJfr(materialize("jfr", fileName, JfrRecordingTest.class), false, waitForEditor);
	}

}
