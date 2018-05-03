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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.Image;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrNavigator;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrStackFrame;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrStackTrace;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrUi;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;

/**
 * Class for testing the Stack Trace Component in the JFR UI.
 */
public class StackTraceComponentTest extends MCJemmyTestBase {

	private static JfrStackTrace stackTrace = null;

	// What is a reasonable maximum value? How many siblings can there be reasonably?
	private int MAX_KEYPRESSES_TO_FIND_EDGE = 1000;

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			MCMenu.restoreStackTraceView();
			JfrUi.openJfr(materialize("jfr", "plain_recording.jfr", StackTraceComponentTest.class));
			JfrNavigator.selectTab(JfrUi.Tabs.METHOD_PROFILING);
			stackTrace = new JfrStackTrace();
		}

		@Override
		public void after() {
			MCMenu.closeActiveEditor();
		}
	};

	/**
	 * Asserts that the three buttons in the toolbar for controlling branch interaction (Choose
	 * Frame Group, Next Frame Group and Previous Frame Group) all have the expected state.
	 *
	 * @param enabledState
	 *            {@code true} if the buttons are expected to be enabled.
	 */
	private void verifyControlIconStates(Boolean enabledState) {
		String message;
		if (!enabledState) {
			message = "enabled.";
		} else {
			message = "not enabled.";
		}
		Assert.assertEquals("The 'Choose Frame Group' toolitem in the toolbar is " + message,
				stackTrace.frameButtonEnabled(), enabledState);

		Assert.assertEquals("The 'Next Frame Group' toolitem in the toolbar is " + message,
				stackTrace.nextButtonEnabled(), enabledState);

		Assert.assertEquals("The 'Previous Frame Group' toolitem in the toolbar is " + message,
				stackTrace.previousButtonEnabled(), enabledState);
	}

	// When the formatting of methods is investigated, a method with parameters must be found
	// This method finds a stack frame with such a method.
	private int getIndexOfAFrameWithParameters() {
		int index = 0;
		int size = stackTrace.getFrameCount();
		String text = stackTrace.getFrame(index).text;
		Pattern regexp_pattern = Pattern.compile(".*\\(\\.\\.\\.\\)");
		Matcher match = regexp_pattern.matcher(text);
		while (!match.matches()) {
			if (index == size - 1) {
				return -1; // We have reached the end without finding any method with parameters.
			}
			index++;
			text = stackTrace.getFrame(index).text;
			match = regexp_pattern.matcher(text);
		}
		return index;
	}

	/*
	 * Splits a string on the first occurrence of a single character string (e.g. " ") and returns
	 * an array with the first and second parts. The separator is not included in the returned
	 * parts.
	 */
	private String[] split(String input, String separator) {
		int index = input.indexOf(separator);
		String[] result = new String[2];
		result[0] = input.substring(0, index);
		result[1] = input.substring(index + 1);
		return result;
	}

	private void verifyPattern(String pattern, String actualFormat) {
		Pattern regexp_pattern = Pattern.compile(pattern);
		Matcher match = regexp_pattern.matcher(actualFormat);

		/*
		 * TODO: Add a comprehensive summary of the settings in the context menu when the mismatch
		 * occurred. The pattern *is* enough to identify the setting when looking in the source
		 * code, but a summary would be helpful.
		 */
		Assert.assertTrue(
				"The formatting of methods printed in the stack trace does not match the expectations. Actual format was '"
						+ actualFormat + "' and the pattern it should match was '" + regexp_pattern + "'.",
				match.matches());
	}

	@Test
	public void verifySiblingCount() {
		stackTrace.selectFirstFrame();
		stackTrace.setViewAsTree(false);
		stackTrace.setChooseFrameGroup(false);
		stackTrace.clickGroupTracesFromThreadRootButton();

		// Get the count from the top item
		stackTrace.selectFirstFrame();
		int total = stackTrace.getFirstFrame().count;

		// Find and select the next branch
		int nextBranchIndex = stackTrace.selectFrameWithBranch(1);
		Assert.assertNotEquals("Did not find any frame with branch", nextBranchIndex, -1);
		stackTrace.selectFrame(nextBranchIndex);

		// Switch to Choose Frame Group
		stackTrace.setChooseFrameGroup(true);

		// Calculate the sum of the items below (and including) nextBranchIndex
		int count = stackTrace.getAllFrames().stream().skip(nextBranchIndex).mapToInt(i -> i.count).sum();

		Assert.assertEquals("The number of frames do not match! The top frame has count " + total
				+ ", but the sum of the children below it gives " + count + ".", count, total);
	}

	@Test
	public void verifyIconsDisabledWhenTreeMode() {
		stackTrace.setViewAsTree(true);
		verifyControlIconStates(false);
	}

	@Test
	public void verifyBranchInteractions() {

		// We can only do this for table mode
		stackTrace.setViewAsTree(false);

		// We must not be in "Choose Frame Group" mode when we test this.
		stackTrace.setChooseFrameGroup(false);

		// Verify that the buttons "Choose Frame Group", "Previous Frame Group"
		// and "Next Frame Group" are enabled when, and only when, on a branch.
		stackTrace.selectFirstFrame();
		int itemIndex = stackTrace.selectFrameWithBranch();
		Assert.assertTrue("Failed to select an item with a branch.", itemIndex >= 0);
		verifyControlIconStates(true);

		// Verify that left and right keyboard works (=makes a difference) on branches and only on branches.
		stackTrace.selectFirstFrame();
		JfrStackFrame topItemBefore = stackTrace.getFirstFrame();
		stackTrace.keyboardRight(); // We are often at the left edge, so try with right
		stackTrace.selectFirstFrame();
		JfrStackFrame topItemAfter = stackTrace.getFirstFrame();
		Assert.assertNotEquals("The right key doesn't work properly when on a branch item.", topItemBefore,
				topItemAfter);

		// Verify that you reach an edge eventually on a branch by going left and right until the edge is found
		int keyPressCounter = 0;
		topItemBefore = topItemAfter; // This is the new reference.
		stackTrace.keyboardLeft();
		// We are often at the left edge, so try with right
		keyPressCounter++;
		topItemAfter = stackTrace.getFirstFrame();
		while (!topItemAfter.equals(topItemBefore)) {
			topItemBefore = topItemAfter;
			stackTrace.keyboardLeft();
			keyPressCounter++;
			topItemAfter = stackTrace.getFirstFrame();
			if (keyPressCounter > MAX_KEYPRESSES_TO_FIND_EDGE) {
				Assert.fail("Failed to reach an edge when moving left in Stack Trace. Number of attempts: "
						+ keyPressCounter);
			}
		}

		keyPressCounter = 0;
		topItemBefore = topItemAfter; // This is the new reference.
		// Now go through them all in the other direction (right)
		stackTrace.keyboardRight();
		keyPressCounter++;
		topItemAfter = stackTrace.getFirstFrame();
		while (!topItemAfter.equals(topItemBefore)) {
			topItemBefore = topItemAfter;
			stackTrace.keyboardRight();
			keyPressCounter++;
			topItemAfter = stackTrace.getFirstFrame();
			if (keyPressCounter > MAX_KEYPRESSES_TO_FIND_EDGE) {
				Assert.fail("Failed to reach an edge when moving right in Stack Trace. Number of attempts: "
						+ keyPressCounter);
			}
		}

		// Verify that the number of keystrokes matches the actual numbers.
		stackTrace.setChooseFrameGroup(true);
		int itemCount = stackTrace.getFrameCount();
		Assert.assertEquals(
				"The number of keystrokes to reach edge (" + keyPressCounter
						+ ") does not match the number of lines in 'Choose Frame Group' mode (" + itemCount + ").",
				itemCount, keyPressCounter);

		stackTrace.setChooseFrameGroup(false);
		// Verify the case with non-branch items.
		itemIndex = stackTrace.selectFrameWithoutBranch();
		// Can be impossible to find if in mode "Group Traces from Thread Root", so switch and try again.
		if (itemIndex < 0) {
			stackTrace.clickGroupTracesFromLastMethodButton();
			itemIndex = stackTrace.selectFrameWithoutBranch();
		}

		Assert.assertTrue("Failed to select an item without a branch.", itemIndex >= 0);
		verifyControlIconStates(false);
		stackTrace.selectFrame(itemIndex);
		JfrStackFrame itemBefore = stackTrace.getFrame(itemIndex);
		stackTrace.keyboardRight(); // We are often at the left edge, so try with right
		stackTrace.selectFrame(itemIndex);
		JfrStackFrame itemAfter = stackTrace.getFrame(itemIndex);
		Assert.assertEquals("The right key doesn't work properly when on a non-branch item.", itemBefore, itemAfter);
	}

	@Test
	public void verifyMethodInformationDisplayed() {

		String text;
		String pattern;
		stackTrace.setViewAsTree(false);
		stackTrace.selectFirstFrame();
		stackTrace.setChooseFrameGroup(false);

		// Make sure other settings that affect the text on the lines are as expected.
		stackTrace.setDistinguishFramesByOptimizationType(false);
		stackTrace.setDistinguishFramesByLevel(JfrStackTrace.LevelOption.METHOD);

		// Trying to find a frame with a return type that is not void or of a primitive type. Will fail the test if not found
		int frame = 0;
		boolean found = false;
		while (!found && frame < stackTrace.getFrameCount()) {
			if (stackTrace.getFrame(frame).getText().substring(0, 1).matches("\\w?[A-Z]")) {
				found = true;
			} else {
				frame++;
			}
		}
		
		Assert.assertTrue("Could not find suitable frame to analyze. Recording not suitable for this test", found);

		// All hidden
		stackTrace.setMethodFormatting(JfrStackTrace.FormatOption.HIDDEN, JfrStackTrace.FormatOption.HIDDEN,
				JfrStackTrace.FormatOption.HIDDEN);
		text = stackTrace.getFrame(frame).getText();

		/*
		 * Pattern below: A method name followed by () or (...) The pattern for a class name could
		 * be written as \w if it wasn't for the fact that class names can contain $ and digits.
		 * Instead [\w\$\d] is used to match the characters of a class. The same pattern is used for
		 * method names too. I recommend e.g. http://www.regexplanet.com/advanced/java/index.html to
		 * try out regular expressions with
		 */
		pattern = "[\\w\\$\\d]+\\((\\.\\.\\.)?\\)";
		verifyPattern(pattern, text);

		// Adding class as return value
		stackTrace.setMethodFormatting(JfrStackTrace.FormatOption.CLASS_NAME, null, null);
		// Pattern below: Previous text, but with a Class name and space before it
		pattern = "[\\w\\$\\d]+ " + Pattern.quote(text);
		text = stackTrace.getFrame(frame).getText();
		verifyPattern(pattern, text);

		// Adding class and package name as return value
		stackTrace.setMethodFormatting(JfrStackTrace.FormatOption.CLASS_AND_PACKAGE_NAME, null, null);
		// Pattern below: Previous text, but with one or more Class name(s) separated by dots before it
		pattern = "([\\w\\$\\d]+\\.)+[\\w\\$\\d]+\\." + Pattern.quote(text);
		text = stackTrace.getFrame(frame).getText();
		verifyPattern(pattern, text);

		// Adding class name of class
		String[] parts = split(text, " ");
		stackTrace.setMethodFormatting(null, JfrStackTrace.FormatOption.CLASS_NAME, null);
		// Pattern below: Added one class name and dot after the space
		pattern = Pattern.quote(parts[0]) + " [\\w\\$\\d]+\\." + Pattern.quote(parts[1]);
		text = stackTrace.getFrame(frame).getText();
		verifyPattern(pattern, text);

		// Adding class and package name of class
		parts = split(text, " ");
		stackTrace.setMethodFormatting(null, JfrStackTrace.FormatOption.CLASS_AND_PACKAGE_NAME, null);
		// Pattern below: Added one or more class names, separated with dots, immediately after the space
		pattern = Pattern.quote(parts[0]) + " ([\\w\\$\\d]+\\.)+[\\w\\$\\d]+\\." + Pattern.quote(parts[1]);
		text = stackTrace.getFrame(frame).getText();
		verifyPattern(pattern, text);

		// Adding class name of parameters
		int goodIndex = getIndexOfAFrameWithParameters();
		Assert.assertTrue("No method on the stack trace contains any parameters. Cannot test formatting of parameters.",
				goodIndex > -1);

		text = stackTrace.getFrame(goodIndex).getText();
		parts = split(text, "(");
		stackTrace.setMethodFormatting(null, null, JfrStackTrace.FormatOption.CLASS_NAME);
		// Pattern below: Added one or more class names separated with , within the parenthesis
		pattern = Pattern.quote(parts[0]) + "\\([\\w\\$\\d]+(, [\\w\\$\\d]+)*\\)";
		text = stackTrace.getFrame(goodIndex).getText();
		verifyPattern(pattern, text);

		/*
		 * Adding class and package name of parameters We take a simpler approach here and do not
		 * compare with the former text. We just require that there should be one or more
		 * parameters, and they should have all have one or more class names, separated with dots if
		 * more than one
		 */
		stackTrace.setMethodFormatting(null, null, JfrStackTrace.FormatOption.CLASS_AND_PACKAGE_NAME);
		pattern = ".*\\(([\\w\\$\\d]+\\.)*[\\w\\$\\d]+(, ([\\w\\$\\d]+\\.)*[\\w\\$\\d]+)*\\)";
		text = stackTrace.getFrame(goodIndex).getText();
		verifyPattern(pattern, text);

		// Adding that frames should be distinguished by line
		text = stackTrace.getFirstFrame().getText();
		stackTrace.setDistinguishFramesByLevel(JfrStackTrace.LevelOption.LINE_NUMBER);
		stackTrace.selectFirstFrame();
		pattern = Pattern.quote(text) + ":[\\d]+";
		text = stackTrace.getFirstFrame().getText();
		verifyPattern(pattern, text);

		// Adding that frames should be distinguished by byte code index
		text = stackTrace.getFirstFrame().getText();
		stackTrace.setDistinguishFramesByLevel(JfrStackTrace.LevelOption.BYTE_CODE_INDEX);
		stackTrace.selectFirstFrame();
		pattern = Pattern.quote(text) + " \\[BCI: \\d+\\]";
		text = stackTrace.getFirstFrame().getText();
		verifyPattern(pattern, text);
	}

	@Test
	public void verifyIcons() {
		Image[] icons = stackTrace.getStackFrameImages();

		Assert.assertEquals(
				"It should be exactly six different icons for the frames in the Stack Trace View, but it was "
						+ icons.length,
				6, icons.length);

		for (int index = 0; index < icons.length; index++) {
			Assert.assertNotNull(
					"No icon in the Stack Trace View should be null, but icon number " + index + " was null.",
					null == icons[index]);
			for (int i = index + 1; i < icons.length; i++) {
				Assert.assertNotEquals("Two icons should not be equal in the Stack Trace View, but the icon number "
						+ index + " was equal to icon number " + i + ".", icons[index], icons[i]);
			}
		}
	}
}
