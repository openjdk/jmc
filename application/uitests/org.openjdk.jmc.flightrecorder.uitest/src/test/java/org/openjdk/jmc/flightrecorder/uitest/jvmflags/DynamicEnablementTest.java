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
package org.openjdk.jmc.flightrecorder.uitest.jvmflags;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCDialog;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCLink;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;

/**
 * Class for testing of JFR dynamic enablement from Mission Control
 */
@SuppressWarnings("restriction")
public class DynamicEnablementTest extends MCJemmyTestBase {
	private static final String COMMERCIAL_FEATURES_QUESTION_TITLE = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.COMMERCIAL_FEATURES_QUESTION_TITLE;
	private static final String COMMERCIAL_FEATURES_LINK_FAIL = org.openjdk.jmc.rjmx.messages.internal.Messages.JVMSupport_FLIGHT_RECORDER_DISABLED;
	private static final String COMMERCIAL_FEATURES_LINK_FAIL2 = org.openjdk.jmc.rjmx.messages.internal.Messages.JVMSupport_FLIGHT_RECORDER_NOT_ENABLED;

	private static boolean hasDynamicEnablement;
	private static String connection;
	private static String jvmflags;

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			MCMenu.ensureProgressViewVisible();
		}
	};

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			assumePropertySet("mc.test.connection");
			assumePropertySet("mc.test.dynamicEnablement");
			connection = System.getProperty("mc.test.connection");
			jvmflags = System.getProperty("mc.test.jvmflags");
			hasDynamicEnablement = Boolean.parseBoolean(System.getProperty("mc.test.dynamicEnablement"));
		}

		@Override
		public void after() {
			MCMenu.closeActiveEditor();
		}
	};

	/**
	 * Tests that, depending on the VM startup flags, the JFR dynamic enablement is handled correctly by Mission Control
	 */
	@Test
	public void dynamicEnablement() {
		MC.jvmBrowser.selectContextOption("Start Flight Recording...", connection);
		if (hasDynamicEnablement) {
			if (jvmflags.equals("x")) { // no jvm flags -> we expect a dialog
				MCDialog dialog = new MCDialog(COMMERCIAL_FEATURES_QUESTION_TITLE);
				sleep(1000);
				dialog.clickButton("&Yes");
				sleep(1000);
				MCDialog flr = new MCDialog("Start Flight Recording");
				flr.clickButton("Cancel");
			} else if (jvmflags.equals("-XX:+UnlockCommercialFeatures")) { // no dialog
				MCDialog flr = new MCDialog("Start Flight Recording");
				flr.clickButton("Cancel");
			} else if (jvmflags.equals("-XX:-FlightRecorder")) { // dialog with error, should not start flr
				Assert.assertTrue("Could not find the problem message with text: " + COMMERCIAL_FEATURES_LINK_FAIL,
						MCLink.exists(COMMERCIAL_FEATURES_LINK_FAIL));
			}
		} else {
			if (jvmflags.equals("x") || jvmflags.equals("-XX:+UnlockCommercialFeatures")) {
				Assert.assertTrue("Could not find the problem message with text: " + COMMERCIAL_FEATURES_LINK_FAIL2,
						MCLink.exists(COMMERCIAL_FEATURES_LINK_FAIL2));
			}
		}
	}
}
