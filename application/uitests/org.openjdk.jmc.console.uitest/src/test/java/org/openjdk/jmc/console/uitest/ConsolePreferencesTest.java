/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.console.uitest;

import org.eclipse.jface.resource.JFaceResources;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.console.ui.preferences.CommunicationPage;
import org.openjdk.jmc.rjmx.preferences.PreferencesKeys;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCButton.Labels;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCDialog;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;

/**
 * Class to test basic functionality of the JMX Console preferences UI.
 */
public class ConsolePreferencesTest extends MCJemmyTestBase {

	private static final String SECURE_MAIL_SERVER_LABEL = Messages.CommunicationPage_CAPTION_SECURE_MAIL_SERVER;

	private MCDialog preferences;

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			preferences = MCMenu.openPreferencesDialog();
		}

		@Override
		public void after() {
			preferences.closeWithButton(Labels.CANCEL);
		}
	};

	@Test
	public void checkRestoreDefaults() throws Exception {
		preferences.selectTreeItem("JDK Mission Control", "JMX Console", "Communication");

		// Enter some non-default values for each field
		String msSuffix = UnitLookup.MILLISECOND.getAppendableSuffix(true);
		preferences.enterText(CommunicationPage.UPDATE_INTERVAL_FIELD_NAME, "1234" + msSuffix);
		preferences.enterText(CommunicationPage.RETAINED_EVENT_FIELD_NAME, "123456");
		preferences.enterText(CommunicationPage.SERVER_HOST_FIELD_NAME, "othermail.example.com");
		preferences.enterText(CommunicationPage.SERVER_PORT_FIELD_NAME, "1234");
		preferences.clickButton(SECURE_MAIL_SERVER_LABEL);
		preferences.enterText(CommunicationPage.USERNAME_FIELD_NAME, "testuser");
		preferences.enterText(CommunicationPage.PASSWORD_FIELD_NAME, "testpassword");

		// Click "Restore Defaults" button
		preferences.clickButton(JFaceResources.getString("defaults"));

		// Check that all fields restored to defaults
		ITypedQuantity<LinearUnit> intervalQty = UnitLookup.TIMESPAN.parseInteractive(preferences.getText(CommunicationPage.UPDATE_INTERVAL_FIELD_NAME));
		Assert.assertEquals(PreferencesKeys.DEFAULT_UPDATE_INTERVAL, intervalQty.longValueIn(UnitLookup.MILLISECOND));
		Assert.assertEquals(String.valueOf(PreferencesKeys.DEFAULT_RETAINED_EVENT_VALUES),
				preferences.getText(CommunicationPage.RETAINED_EVENT_FIELD_NAME));
		Assert.assertEquals(PreferencesKeys.DEFAULT_MAIL_SERVER,
				preferences.getText(CommunicationPage.SERVER_HOST_FIELD_NAME));
		Assert.assertEquals(String.valueOf(PreferencesKeys.DEFAULT_MAIL_SERVER_PORT),
				preferences.getText(CommunicationPage.SERVER_PORT_FIELD_NAME));
		Assert.assertEquals(PreferencesKeys.DEFAULT_MAIL_SERVER_SECURE,
				preferences.getButtonState(SECURE_MAIL_SERVER_LABEL));
		Assert.assertEquals(PreferencesKeys.DEFAULT_MAIL_SERVER_USER,
				preferences.getText(CommunicationPage.USERNAME_FIELD_NAME));
		Assert.assertEquals(PreferencesKeys.DEFAULT_MAIL_SERVER_PASSWORD,
				preferences.getText(CommunicationPage.PASSWORD_FIELD_NAME));
	}

}
