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
import java.util.Collection;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.helpers.EventSettingsData;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrNavigator;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrUi;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;

/**
 * Class for testing opening of old Flight Recordings
 */
@RunWith(Parameterized.class)
public class OldRecordingsVerificationTest extends MCJemmyTestBase {
	@Parameter
	public String fileName;
	@Parameter(value = 1)
	public int numOfSettings;

	@Parameters(name = "Recording file {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {{"7u40.jfr", 77}, {"7u60.jfr", 77}, {"7u76.jfr", 77}, {"8u0.jfr", 72},
				{"8u20.jfr", 85}, {"8u40.jfr", 85}, {"8u60.jfr", 86}});
	}

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			JfrUi.openJfr(materialize("jfr", fileName, OldRecordingsVerificationTest.class));
		}

		@Override
		public void after() {
			MCMenu.closeActiveEditor();
		}
	};

	/**
	 * Opens the recording and verifies the number of unique events (verifying the parsing of jfr metadata)
	 */
	@Test
	public void verifyRecording() {
		// First do a total tab traversal (standard tabs)
		for (JfrUi.Tabs tabName : Arrays.asList(JfrUi.Tabs.values())) {
			JfrNavigator.selectTab(tabName);
		}
		// Also, check that the Recording settings table contains data
		EventSettingsData recordingEventSettings = JfrUi.parseEventSettingsTable();
		int currentNumOfEventSettings = recordingEventSettings.getAllEventNames().size();
		Assert.assertTrue(
				"File " + fileName + ": Incorrect number of event settings in the recording settings table. Should be "
						+ numOfSettings + " but was " + currentNumOfEventSettings,
				currentNumOfEventSettings == numOfSettings);
	}

}
