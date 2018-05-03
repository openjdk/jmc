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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JmxConsole;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCButton;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCHyperlink;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCLabel;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;

/**
 * Class for testing the JMX Console Threads page
 */
public class ThreadsTabTest extends MCJemmyTestBase {
	private static final int TIMESTAMP_LABEL_MAX_SPAN_SECONDS = 3;
	private static final int MIN_THREADS = 10;
	private static final int MAX_THREADS = 150;
	private static String DEAD_LOCK = "Deadlock Detection";
	private static String REFRESH_LIVE_THREADS = "Refresh Live Threads";
	private static String LIVE_THREADS = "Live Threads ";
	private static String REFRESH_STACK_TRACE = "Refresh Stack Trace";
	private static String STACK_TRACE = "Stack traces for selected threads ";
	private static MCTable threadsTable;

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			MC.jvmBrowser.connect();
			JmxConsole.selectTab(JmxConsole.Tabs.THREADS);
			threadsTable = MCTable.getByColumnHeader("Thread Name");
		}
	};

	@Test
	public void testAllocationOptionUnchecked() {
		// ensure that the Allocation checkbox is unchecked
		MCButton.getByLabel("Allocation").setState(false);
		// read all cells in the column "Allocated Bytes"
		List<String> columnData = threadsTable.getColumnItemTexts("Allocated Memory");
		// verify that the number of cells is sane and that they only contain "Not Enabled"
		assertSaneNumberOfRows(columnData);
		for (String data : columnData) {
			Assert.assertEquals("Not Enabled", data);
		}
	}

	@Test
	public void testAllocationOptionChecked() {
		// ensure that the Allocation checkbox is checked
		MCButton.getByLabel("Allocation").setState(true);
		// read all cells in the column "Allocated Bytes"
		List<String> columnData = threadsTable.getColumnItemTexts("Allocated Memory");
		// verify that the cells contain data that can be parsed as Double
		assertSaneNumberOfRows(columnData);
		verifyDoubles(columnData);
	}

	@Test
	public void testTimestampLabelsUpdating() {
		MCButton.getByLabel(DEAD_LOCK).click();

		MCHyperlink.getByTooltip(REFRESH_LIVE_THREADS).click();

		verifyRefreshing(LIVE_THREADS, true);
		MCHyperlink.getByTooltip(REFRESH_LIVE_THREADS).click();
		verifyRefreshing(LIVE_THREADS, false);

		// We need to select the table here so that the stack trace view is rendered.
		// threadsTable.mouse().click(1, new Point(50, 50));
		threadsTable.click();
		MCJemmyBase.waitForIdle();

		MCHyperlink.getByTooltip(REFRESH_STACK_TRACE).click();

		verifyRefreshing(STACK_TRACE, true);
		MCHyperlink.getByTooltip(REFRESH_STACK_TRACE).click();

		verifyRefreshing(STACK_TRACE, false);

		// make sure that the labels are equal (or at least close)
		Assert.assertTrue("Time labels for " + LIVE_THREADS + "and " + STACK_TRACE + "should be equal",
				fuzzyTimeLabelCompare(getRefreshTime(LIVE_THREADS), getRefreshTime(STACK_TRACE),
						TIMESTAMP_LABEL_MAX_SPAN_SECONDS));
	}

	private void assertSaneNumberOfRows(List<String> columnData) {
		int numOfThreads = columnData.size();
		System.out.println("Threads. Current number of threads: " + numOfThreads);
		Assert.assertFalse("There are less than " + MIN_THREADS + " rows in the threads list",
				numOfThreads < MIN_THREADS);
		Assert.assertFalse("There are more than " + MAX_THREADS + " rows in the threads list",
				numOfThreads > MAX_THREADS);
	}

	/**
	 * This method takes an array of Strings and verifies that the strings can be transformed to
	 * Double. The string versions of the doubles comes from the UI and have spaces in them.
	 */
	private void verifyDoubles(List<String> values) {
		StringBuilder sb = new StringBuilder();
		String[] splitnumbers = null;
		for (String s : values) {
			splitnumbers = s.split("\\D");
			for (String number : splitnumbers) {
				sb.append(number);
			}
			Assert.assertNotNull("fail to make a digit of the string: " + sb.toString(), Double.valueOf(sb.toString()));
			sb.delete(0, (sb.length() - 1));
		}
	}

	private void verifyRefreshing(String label, boolean condition) {
		MCJemmyBase.waitForIdle();
		String a = getRefreshTime(label);
		sleep(6000);
		String b = getRefreshTime(label);
		if (condition) {
			Assert.assertTrue(buildAssertionMessageString(label, true, a, b), a.equals(b));
		} else {
			Assert.assertFalse(buildAssertionMessageString(label, false, a, b), a.equals(b));
		}
	}

	private String buildAssertionMessageString(String label, boolean eqaulity, String first, String second) {
		return "Time stamp for label \"" + label.trim() + "\" should" + ((eqaulity == true) ? "" : " not")
				+ " be equal between samples (but was " + first + " and " + second + ")";
	}

	private boolean fuzzyTimeLabelCompare(String first, String second, int maxSpanSeconds) {
		DateFormat df = DateFormat.getTimeInstance(DateFormat.MEDIUM);
		boolean fuzzyMatch = true;
		try {
			if (Math.abs(df.parse(first).getTime() - df.parse(second).getTime()) > maxSpanSeconds * 1000) {
				fuzzyMatch = false;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return fuzzyMatch;
	}

	private String getRefreshTime(String label) {
		MCJemmyBase.waitForIdle();
		return MCLabel.getByLabelSubstring(label).getText().split(label)[1];
	}
}
