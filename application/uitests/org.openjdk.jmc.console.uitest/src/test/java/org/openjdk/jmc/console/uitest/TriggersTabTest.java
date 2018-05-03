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
package org.openjdk.jmc.console.uitest;

import static org.openjdk.jmc.rjmx.triggers.condition.internal.Messages.TriggerCondition_MAX_TRIGGER_TOOLTIP;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.helpers.ConnectionHelper;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTriggersPage;

/**
 * Class for testing the JMX Console Triggers page
 */
public class TriggersTabTest extends MCJemmyTestBase {
	private static final int MAX_WAIT = 30;
	private static final String DIAGNOSTIC_COMMAND_LOG_FILE_TOOLTIP = "The file where the command result will be written. This action will fail if the file specified is not writable from the process.";
	private static final String DIAGNOSTIC_COMMAND_TOOLTIP = "The diagnostic command to execute.";
	private static final String DIAGNOSTIC_COMMAND = "Thread.print";
	private static final String APPLE_PIE = "Appelpaj";
	private static final String CPU_USAGE_MACHINE_TOO_HIGH = "CPU Usage - Machine (Too High)";
	private static final String[] ALERT_ATTRIBUTE_PATH = {"java.lang", "OperatingSystem", "SystemCpuLoad"};
	private static final String ALERT_ACTION_NAME = "Application alert";
	private static final String DIAGNOSTIC_COMMAND_ACTION_NAME = "Invoke Diagnostic Command";
	private static final String ALERT_RULE_GROUP_NAME = "Krank Test Rules";
	private static final String ALERT_RULE_NAME = "CPU Load - ";

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void after() {
			MCTriggersPage.resetTriggerRules();
			MCJemmyBase.focusMc();
		}
	};

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			MC.jvmBrowser.connect();
		}

		@Override
		public void after() {
			MC.jvmBrowser.disconnect();
		}
	};

	@Test
	public void testApplicationAlert() {
		Assume.assumeTrue("This feature is only valid on JDK7u4 or later.",
				ConnectionHelper.is7u4orLater(TEST_CONNECTION));

		MCTriggersPage.createTriggerRule(null, null, "1 s", ALERT_ACTION_NAME, null, ALERT_RULE_GROUP_NAME,
				ALERT_RULE_NAME + "Application Alert", ALERT_ATTRIBUTE_PATH);

		// Activate the new rule
		MCTriggersPage.toggleTriggerRule(true, ALERT_RULE_GROUP_NAME, ALERT_RULE_NAME + "Application Alert");

		// Verify that the alert dialog eventually appears, set it to not show on subsequent alerts and close it
		MCTriggersPage.closeTriggerAlertDialog(true);
	}

	@Test
	public void testDiagnosticCommand() {
		Assume.assumeTrue("This feature is only valid on JDK8u0 or later.",
				ConnectionHelper.is8u0orLater(TEST_CONNECTION));

		// Create a log file and record its initial update time
		String filename = createFile("Diagnostics", "log");
		long creationTime = getFileModificationTime(filename);

		// Create a map of parameters for the diagnostic command action
		Map<String, Comparable<?>> actionParams = new HashMap<>();
		actionParams.put(DIAGNOSTIC_COMMAND_LOG_FILE_TOOLTIP, filename);
		actionParams.put(DIAGNOSTIC_COMMAND_TOOLTIP, DIAGNOSTIC_COMMAND);

		MCTriggersPage.createTriggerRule(null, null, "1 s", DIAGNOSTIC_COMMAND_ACTION_NAME, actionParams,
				ALERT_RULE_GROUP_NAME, ALERT_RULE_NAME + "Diagnostic Command", ALERT_ATTRIBUTE_PATH);

		// Activate the new rule
		MCTriggersPage.toggleTriggerRule(true, ALERT_RULE_GROUP_NAME, ALERT_RULE_NAME + "Diagnostic Command");

		// Verify that the file gets updated.
		Assert.assertTrue("File \"" + filename + "\" hasn't been updated.",
				verifyFileUpdated(filename, creationTime, MAX_WAIT));
	}

	@SuppressWarnings("restriction")
	@Test
	public void testExportImport() {
		Assume.assumeTrue("This feature is only valid on JDK7u0 or later.",
				ConnectionHelper.is7u0orLater(TEST_CONNECTION));

		// create a temporary xml file to use for exporting and importing rules
		String filename = createFile("myTriggers", "xml");

		// select and edit a rule
		MCTriggersPage.setTriggerCondition(TriggerCondition_MAX_TRIGGER_TOOLTIP, "88 %", "Java SE",
				"CPU Usage - JVM Process (Too High)");

		// rename the selected rule
		MCTriggersPage.renameTriggerRule(APPLE_PIE, "Java SE", "CPU Usage - JVM Process (Too High)");

		// select and edit another rule
		MCTriggersPage.setTriggerCondition(TriggerCondition_MAX_TRIGGER_TOOLTIP, "88 %", "Java SE",
				CPU_USAGE_MACHINE_TOO_HIGH);

		// open up the export dialog and export the two rules
		List<String[]> rulePaths = new ArrayList<>();
		rulePaths.add(new String[] {"Java SE", APPLE_PIE});
		rulePaths.add(new String[] {"Java SE", CPU_USAGE_MACHINE_TOO_HIGH});
		MCTriggersPage.exportTriggerRules(filename, rulePaths);

		// delete the rules, one by one
		MCTriggersPage.removeTriggerRule("Java SE", CPU_USAGE_MACHINE_TOO_HIGH);
		MCTriggersPage.removeTriggerRule("Java SE", APPLE_PIE);

		// ensure that the rules don't exist anymore
		sleep(1000);
		Assert.assertFalse("Rule " + CPU_USAGE_MACHINE_TOO_HIGH + " is still there!",
				MCTriggersPage.hasTriggerRule("Java SE", CPU_USAGE_MACHINE_TOO_HIGH));
		Assert.assertFalse("Rule " + APPLE_PIE + " is still there!",
				MCTriggersPage.hasTriggerRule("Java SE", APPLE_PIE));

		// import the exported rules
		MCTriggersPage.importTriggerRules(filename);

		// ensure that the rules exist (again)
		sleep(1000);
		Assert.assertTrue("Rule " + CPU_USAGE_MACHINE_TOO_HIGH + " is still there!",
				MCTriggersPage.hasTriggerRule("Java SE", CPU_USAGE_MACHINE_TOO_HIGH));
		Assert.assertTrue("Rule " + APPLE_PIE + " is still there!",
				MCTriggersPage.hasTriggerRule("Java SE", APPLE_PIE));
	}

	private String createFile(String prefix, String suffix) {
		String filename = null;
		try {
			File connectionFile = File.createTempFile(prefix, "." + suffix);
			connectionFile.deleteOnExit();
			filename = connectionFile.getAbsolutePath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return filename;
	}

	private long getFileModificationTime(String path) {
		long result = -1L;
		File file = new File(path);
		if (file.exists()) {
			result = file.lastModified();
		}
		return result;
	}

	private boolean verifyFileUpdated(String path, long baseline, int maxWait) {
		boolean updated = false;
		File file = new File(path);
		for (int i = 0; i < maxWait && !updated; i++) {
			updated = baseline < file.lastModified();
			sleep(1000);
		}
		return updated;
	}
}
