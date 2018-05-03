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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.ObjectName;

import org.eclipse.osgi.util.NLS;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.openjdk.jmc.console.ui.diagnostic.preferences.DiagnosticPage;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.internal.RJMXConnection;
import org.openjdk.jmc.rjmx.services.IOperation;
import org.openjdk.jmc.rjmx.services.IOperation.OperationImpact;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.ui.operations.OperationsLabelProvider;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.helpers.ConnectionHelper;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCButton;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCDialog;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTabFolder;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTree;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JmxConsole;

/**
 * Class testing various Diagnostic Commands from the JMX Console Diagnostic Commands page
 */
public class DiagnosticCommandsTabTest extends MCJemmyTestBase {
	private static final String OPERATION_WARNING_DIALOG_TITLE = org.openjdk.jmc.console.ui.diagnostic.form.Messages.DiagnosticTab_WARNING_FOR_ADVANCED_USER_TITLE;
	private static MCTable commandTable;
	private static MCTabFolder resultTabFolder;
	private static MCButton executeButton;
	private static MCTree params;
	private static final String COMMAND = "VM.uptime";
	private static final String PARAM = "Add a prefix with current date";
	private static String recording_parameter_name;
	private static String[] PARAMETER_NAME_COLUMN_VISIBILITY = new String[] {"Visible Columns", "Name"};
	private static String[] PARAMETER_DESCRIPTION_COLUMN_VISIBILITY = new String[] {"Visible Columns", "Description"};

	private static final String[] definiteCommandNames = {"Thread.print", "VM.command_line", "VM.version", "help",
			"JFR.check", "JFR.dump", "JFR.start", "JFR.stop",};

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void after() {
			resultTabFolder.closeAll();
		}
	};

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			skipIfEarlierThan8u0(TEST_CONNECTION);
			setAllOperationImpactStatuses(false);
			MC.jvmBrowser.connect();
			JmxConsole.selectTab(JmxConsole.Tabs.DIAGNOSTIC_COMMANDS);
			for (MCTable table : MCJemmyBase.getTables()) {
				if (table.hasItem(COMMAND)) {
					commandTable = table;
					break;
				}
			}
			commandTable.select(COMMAND);
			params = MCTree.getByItem(PARAM);
			params.select(PARAM);
			params.contextChoose(true, PARAMETER_NAME_COLUMN_VISIBILITY);
			params.contextChoose(false, PARAMETER_DESCRIPTION_COLUMN_VISIBILITY);

			// Starting in JDK9, the "name" parameter is used for both recording ids (numbers)
			// and for recording names (strings that are not numbers)
			if (ConnectionHelper.is9u0EAorLater(TEST_CONNECTION)) {
				recording_parameter_name = "name";
			} else {
				recording_parameter_name = "recording";
			}

			executeButton = MCButton.getByLabel("Execute");
			executeButton.click();
			resultTabFolder = MCTabFolder.getByName("operation.result");
			resultTabFolder.closeAll();
		}

		@Override
		public void after() {
			if (testsRun()) {
				MC.jvmBrowser.disconnect();
				setAllOperationImpactStatuses(true);
			}
		}
	};

	@Test
	public void testCommandLine() {
		commandTable.select("VM.command_line");
		executeButton.click();
		resultTabFolder.select("VM.command_line");
		Assert.assertTrue("VM.command_line result doesn't match expected result",
				resultTabFolder.getText().contains("VM Arguments"));
	}

	@Test
	public void testPrintThreads() {
		commandTable.select("Thread.print");
		executeButton.click();
		String result = resultTabFolder.getText();
		Assert.assertTrue("Full thread dump: " + result, patternMatcher(result, "Full thread dump"));
		Assert.assertTrue("HotSpot: " + result, patternMatcher(result, "HotSpot"));
		Assert.assertTrue("Finalizer" + result, patternMatcher(result, "Finalizer"));
		Assert.assertTrue("at java.lang" + result, patternMatcher(result, "at java.lang"));
	}

	@Test
	public void testVersion() {
		commandTable.select("VM.version");
		executeButton.click();
		String result = resultTabFolder.getText();
		Assert.assertTrue("HotSpot", patternMatcher(result, "HotSpot"));
		Assert.assertTrue("VM version", patternMatcher(result, "VM version"));
	}

	@Test
	public void testOutputUpdated() {
		commandTable.select("help");
		executeButton.click();
		String helpResult = resultTabFolder.getText();
		resultTabFolder.closeAll();
		commandTable.select("VM.version");
		executeButton.click();
		String versionResult = resultTabFolder.getText();
		Assert.assertFalse("Expected output from second command execution (" + versionResult
				+ ") to be different from the first (" + helpResult + ')', versionResult.equals(helpResult));
	}

	@Test
	public void testStartFlightRecording() {
		commandTable.select("JFR.start");
		params.select(true, "name");
		params.enterText("");
		executeButton.click();
		sleep(500);
		String result = resultTabFolder.getText();
		resultTabFolder.closeAll();

		String recordingIdentifierPattern = "Started recording (.*?)\\. .*";
		Pattern pattern = Pattern.compile(recordingIdentifierPattern, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(result);
		Assert.assertTrue(
				"The output from JFR.start didn't match the pattern for finding the recorder identifier. The pattern is '"
						+ recordingIdentifierPattern + "' and the output was '" + result + "'.",
				matcher.matches());
		String recordingIdentifier = matcher.group(1);

		commandTable.select("JFR.check");
		params.select(true, recording_parameter_name);
		params.enterText(recordingIdentifier);
		executeButton.click();
		sleep(500);
		result = resultTabFolder.getText();
		resultTabFolder.closeAll();

		String expectedOutput;
		if (ConnectionHelper.is9u0EAorLater(TEST_CONNECTION)) {
			expectedOutput = "Recording: recording=\\d+ name=\"Recording-" + recordingIdentifier
					+ "\".* \\(running\\).*";
		} else {
			expectedOutput = "Recording: recording=" + recordingIdentifier + " name=.* \\(running\\).*";
		}

		Assert.assertTrue(
				"Output from JFR.check diagnostic command" + " is not matching expected pattern. Actual output was: '"
						+ result + "'. Expected output was: '" + expectedOutput + '\'',
				patternMatcher(result, expectedOutput));

		commandTable.select("JFR.stop");
		params.select(true, recording_parameter_name);
		params.enterText(recordingIdentifier);
		executeButton.click();
		sleep(500);
		result = resultTabFolder.getText();
		expectedOutput = "Stopped [recording ]*[\"Recording-]*" + recordingIdentifier + ".*";
		Assert.assertTrue(
				"Output from JFR.stop diagnostic command" + " is not matching expected pattern. Actual output was: '"
						+ result + "'. Expected output was: '" + expectedOutput + '\'',
				patternMatcher(result, expectedOutput));
	}

	@Test
	public void testStartNamedFlightRecording() {
		String recordingName = getRandomRecordingName();
		commandTable.select("JFR.start");
		params.select(true, "name");
		params.enterText(recordingName);
		executeButton.click();
		sleep(500);
		String result = resultTabFolder.getText();
		resultTabFolder.closeAll();

		commandTable.select("JFR.check");
		params.select(true, "name");
		params.enterText(recordingName);
		executeButton.click();
		sleep(500);
		result = resultTabFolder.getText();
		resultTabFolder.closeAll();
		String expectedOutput = "Recording: recording=.* name=\"" + recordingName + "\"";
		Assert.assertTrue("Output from JFR.check (with name) diagnostic command"
				+ " is not matching expected pattern. Actual output was: '" + result + "'. Expected output was: '"
				+ expectedOutput + '\'', patternMatcher(result, expectedOutput));

		String recordingIdPattern = "Recording: recording=(\\d+) name=\"" + recordingName + "\".*";
		Pattern pattern = Pattern.compile(recordingIdPattern, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(result);
		Assert.assertTrue(
				"The output from JFR.check didn't match the pattern for finding the recorder id. The pattern is '"
						+ recordingIdPattern + "' and the output was '" + result + "'.",
				matcher.matches());
		String recordingId = matcher.group(1);

		/*
		 * need to move focus among the commands to clear the previous values in the params tree
		 * (both name and id isn't allowed when issuing the command)
		 */
		commandTable.select("JFR.dump");

		commandTable.select("JFR.check");
		params.select(true, recording_parameter_name);
		params.enterText(recordingId);
		executeButton.click();
		sleep(500);
		result = resultTabFolder.getText();
		resultTabFolder.closeAll();
		Assert.assertTrue("Output from JFR.check (with recording id) diagnostic command"
				+ " is not matching expected pattern. Actual output was: '" + result + "'. Expected output was: '"
				+ expectedOutput + '\'', patternMatcher(result, expectedOutput));

		// stop recording
		commandTable.select("JFR.stop");
		params.select(true, recording_parameter_name);
		params.enterText(recordingId);
		executeButton.click();
		sleep(500);
		result = resultTabFolder.getText();
		expectedOutput = "Stopped [recording ]*\"?"
				+ ((ConnectionHelper.is9u0EAorLater(TEST_CONNECTION)) ? recordingName : recordingId) + ".*";
		Assert.assertTrue(
				"Output from JFR.stop diagnostic command" + " is not matching expected pattern. Actual output was: '"
						+ result + "'. Expected output was: '" + expectedOutput + '\'',
				patternMatcher(result, expectedOutput));
	}

	@Test
	public void testHelp() {
		List<String> golden = getAllCommands(TEST_CONNECTION);
		List<String> commands = new ArrayList<>();
		commandTable.select("help");
		executeButton.click();
		resultTabFolder.select("help");
		String[] lines = resultTabFolder.getText().split("The following commands are available:")[1]
				.split("For more information")[0].split(System.getProperty("line.separator"));
		for (String line2 : lines) {
			String line = line2.trim();
			if (line.length() > 0) {
				commands.add(line);
			}
		}
		assertSaneNumberOfHelpCommands(commands, 8, 40);
		assertDefiniteCommandNames(commands);
		assertSameCommands(golden, commands);
	}

	@Test
	public void runCommandAndUnsetHighWarning() {
		setOperationImpactStatus(true, false, false);
		try {
			commandTable.select("GC.class_histogram");
			executeButton.click();
			handleWarningDialog(OperationImpact.IMPACT_HIGH, true);
			Assert.assertTrue("The preferences setting wasn't updated",
					getOperationImpactStatus(OperationImpact.IMPACT_HIGH) == false);
		} finally {
			setAllOperationImpactStatuses(false);
		}
	}

	@Test
	public void runCommandAndUnsetMediumWarning() {
		setOperationImpactStatus(false, true, false);
		try {
			commandTable.select("GC.run");
			executeButton.click();
			handleWarningDialog(OperationImpact.IMPACT_MEDIUM, true);
			Assert.assertTrue("The preferences setting wasn't updated",
					getOperationImpactStatus(OperationImpact.IMPACT_MEDIUM) == false);
		} finally {
			setAllOperationImpactStatuses(false);
		}
	}

	@Test
	public void runCommandWithHighImpactWarning() {
		setOperationImpactStatus(true, false, false);
		try {

			commandTable.select("GC.class_histogram");
			executeButton.click();
			handleWarningDialog(OperationImpact.IMPACT_HIGH, false);
			Assert.assertTrue("The preferences setting was updated",
					getOperationImpactStatus(OperationImpact.IMPACT_HIGH) == true);
		} finally {
			setAllOperationImpactStatuses(false);
		}
	}

	@Test
	public void runCommandWithMediumImpactWarning() {
		setOperationImpactStatus(false, true, false);
		try {
			commandTable.select("GC.run");
			executeButton.click();
			handleWarningDialog(OperationImpact.IMPACT_MEDIUM, false);
			Assert.assertTrue("The preferences setting was updated",
					getOperationImpactStatus(OperationImpact.IMPACT_MEDIUM) == true);
		} finally {
			setAllOperationImpactStatuses(false);
		}
	}

	private static String getRandomRecordingName() {
		String name = "Named Flight Recording ";
		Random r = new Random();
		r.setSeed(System.currentTimeMillis());
		return name + r.nextInt();
	}

	private static List<String> getAllCommands(String connectionName) {
		IConnectionHandle handle = createConnectionHandle(connectionName);
		List<String> operationNames = new ArrayList<>();
		try {
			IMBeanHelperService mBeanHelperService = handle.getServiceOrThrow(IMBeanHelperService.class);
			ObjectName mBeanName = ConnectionToolkit.createObjectName("com.sun.management:type=DiagnosticCommand");
			Collection<IOperation> operations = ((RJMXConnection) mBeanHelperService).getOperations(mBeanName);
			for (IOperation operation : operations) {
				operationNames
						.add(extractSubstring(operation.getDescription(), "dcmd.name=", "dcmd.permissionAction="));
			}
		} catch (Exception e) {
			System.err.println(
					"Failed to get operations from MBean: com.sun.management/DiagnosticCommand\n" + e.getMessage());
			e.printStackTrace();
		} finally {
			disposeConnectionHandle(handle);
		}
		return operationNames;
	}

	private static String extractSubstring(String input, String matchStart, String matchEnd) {
		return input.substring(input.indexOf(matchStart) + matchStart.length(), input.indexOf(matchEnd)).trim();
	}

	/**
	 * Checks that the number of help commands is sane
	 */
	private void assertSaneNumberOfHelpCommands(List<String> help, int atLeast, int atMost) {
		Assert.assertTrue("Diagnostic Commands has too few commands (" + help.size() + "), something is broken",
				help.size() > atLeast);
		Assert.assertTrue("Diagnostic Commands has too many commands (" + help.size() + "), something is broken",
				help.size() < atMost);
	}

	/**
	 * Check that a few command names that we expect to _always_ be in the list are present
	 */
	private void assertDefiniteCommandNames(List<String> help) {
		List<String> definiteCommandNamesList = Arrays.asList(definiteCommandNames);
		listContains(definiteCommandNamesList, help);
	}

	/**
	 * Check that the two list contains _exactly_ the same commands
	 */
	private void assertSameCommands(List<String> expectedHelp, List<String> actaulHelp) {
		Assert.assertEquals(expectedHelp.size(), actaulHelp.size());
		listContains(expectedHelp, actaulHelp);
	}

	private void listContains(List<String> subsetElements, List<String> supersetElements) {
		for (String searchTarget : subsetElements) {
			Assert.assertTrue("The subsetElements (" + supersetElements + ") is not a subset of the supersetElements ("
					+ subsetElements + ")", supersetElements.contains(searchTarget));
		}
	}

	private static boolean patternMatcher(String matchText, String textStringToMatch) {
		if (textStringToMatch.equals("")) {
			return (matchText.equals(""));
		}

		Pattern pattern = Pattern.compile(textStringToMatch, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(matchText);
		return matcher.find();
	}

	private static void handleWarningDialog(OperationImpact impact, boolean disableImpactWarning) {
		MCDialog warning = new MCDialog(getOperationImpactWarningTitle(impact));
		if (disableImpactWarning) {
			warning.setButtonState(getOperationImpactButtonLabel(impact), false);
		}
		warning.clickButton(MCButton.Labels.OK);
	}

	private static MCDialog openDiagnosticCommandPreferences() {
		MCDialog preferences = MCMenu.openPreferencesDialog();
		preferences.selectTreeItem("JDK Mission Control", "JMX Console", "Diagnostic Commands");
		return preferences;
	}

	private static void setOperationImpactStatus(boolean high, boolean medium, boolean unknown) {
		MCDialog preferences = openDiagnosticCommandPreferences();
		Map<OperationImpact, Boolean> impactMap = getImpactMap(high, medium, unknown);
		for (OperationImpact impact : impactMap.keySet()) {
			preferences.setButtonState(getOperationImpactButtonLabel(impact), impactMap.get(impact));
		}
		preferences.clickButton(MCButton.Labels.APPLY_AND_CLOSE);
	}

	private static void setAllOperationImpactStatuses(boolean state) {
		setOperationImpactStatus(state, state, state);
	}

	private static boolean getOperationImpactStatus(OperationImpact impact) {
		MCDialog preferences = openDiagnosticCommandPreferences();
		boolean state = preferences.getButtonState(getOperationImpactButtonLabel(impact));
		preferences.clickButton(MCButton.Labels.APPLY_AND_CLOSE);
		return state;
	}

	private static String getOperationImpactButtonLabel(OperationImpact impact) {
		return DiagnosticPage.getPromptQuestion(impact);
	}

	private static String getOperationImpactWarningTitle(OperationImpact impact) {
		return NLS.bind(OPERATION_WARNING_DIALOG_TITLE, OperationsLabelProvider.impactAsString(impact));
	}

	private static Map<OperationImpact, Boolean> getImpactMap(boolean high, boolean medium, boolean unknown) {
		Map<OperationImpact, Boolean> impactMap = new HashMap<>();
		impactMap.put(OperationImpact.IMPACT_HIGH, high);
		impactMap.put(OperationImpact.IMPACT_MEDIUM, medium);
		impactMap.put(OperationImpact.IMPACT_UNKNOWN, unknown);
		return impactMap;
	}
}
