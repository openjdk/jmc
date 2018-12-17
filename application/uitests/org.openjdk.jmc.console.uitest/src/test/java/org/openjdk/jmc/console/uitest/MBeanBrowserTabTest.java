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
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.openjdk.jmc.console.ui.mbeanbrowser.notifications.MBeanNotificationLogInspector;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.helpers.ConnectionHelper;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JmxConsole;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCButton;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTabFolder;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCToolBar;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTree;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

/**
 * Class for for testing MBean Browser related actions in the JMX Console.
 */
@SuppressWarnings("restriction")
public class MBeanBrowserTabTest extends MCJemmyTestBase {
	private static int[] BYTES_TO_ALLOC = new int[2];
	private static final long[] threadIds = new long[2];
	private static final Runnable[] jobs = new Runnable[2];
	private static final String EXECUTE_COMMAND = "Execute";
	private static final String THREAD_INFO_COMMAND = "getThreadInfo : CompositeData";
	private static final String THREAD_ALLOC_BYTES_COMMAND = "getThreadAllocatedBytes : long";
	private static final String OPERATIONS_TAB = org.openjdk.jmc.console.ui.mbeanbrowser.tab.Messages.FeatureSectionPart_OPERATIONS_TAB_TITLE_TEXT;
	private static final String NOTIFICATIONS_TAB = org.openjdk.jmc.console.ui.mbeanbrowser.tab.Messages.FeatureSectionPart_NOTIFICATIONS_TAB_TITLE_TEXT;
	private static final String ATTRIBUTES_TAB = org.openjdk.jmc.console.ui.mbeanbrowser.tab.Messages.FeatureSectionPart_ATTRIBUTES_TAB_TITLE_TEXT;
	private static final String MBEANBROWSER_TREE_NAME = org.openjdk.jmc.console.ui.mbeanbrowser.tree.MBeanTreeSectionPart.MBEANBROWSER_MBEAN_TREE_NAME;
	private static final String RESULT_TREE_NAME = org.openjdk.jmc.rjmx.ui.operations.ExecuteOperationForm.RESULT_TREE_NAME;;
	private static final String RESULT_TAB_NAME = org.openjdk.jmc.rjmx.ui.operations.ExecuteOperationForm.RESULT_TAB_NAME;
	private static final String MBEANBROWSER_NOTIFICATIONSTAB_LOGTREE_NAME = org.openjdk.jmc.console.ui.mbeanbrowser.notifications.MBeanNotificationLogInspector.MBEANBROWSER_NOTIFICATIONSTAB_LOGTREE_NAME;
	private static final String VALUE_COLUMN_NAME = org.openjdk.jmc.rjmx.ui.attributes.Messages.AttributeInspector_VALUE_COLUMN_HEADER;
	private static final int DEFAULT_FONT_HEIGHT = 11;
	private static final int TEXT_FONT_HEIGHT = 16;

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			JmxConsole.selectTab(JmxConsole.Tabs.MBEAN_BROWSER);
		}

		@Override
		public void after() {
			// Close the results (if found)
			MCTabFolder tabFolder = MCTabFolder.getByName("operation.result");
			if (tabFolder != null) {
				tabFolder.closeAll();
			}
		}
	};

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {

		private FontData[] saveDefaultFont;
		private FontData[] saveTextFont;

		@Override
		public void before() {
			// Set default and text font to some predefined, but different, sizes
			DisplayToolkit.safeSyncExec(() -> {
				FontData[] defaultFontData = JFaceResources.getDefaultFont().getFontData();
				saveDefaultFont = FontDescriptor.copy(defaultFontData);
				defaultFontData[0].setHeight(DEFAULT_FONT_HEIGHT);
				JFaceResources.getFontRegistry().put(JFaceResources.DEFAULT_FONT, defaultFontData);

				FontData[] textFontData = JFaceResources.getTextFont().getFontData();
				saveTextFont = FontDescriptor.copy(textFontData);
				textFontData[0].setHeight(TEXT_FONT_HEIGHT);
				JFaceResources.getFontRegistry().put(JFaceResources.TEXT_FONT, textFontData);
			});
			// not using the default test connection since we're starting threads of interest in this particular JVM
			MC.jvmBrowser.connect("The JVM Running Mission Control");
			initialiseTestThreads();
		}

		@Override
		public void after() {
			terminateTestThreads();
			// Restore original font sizes
			DisplayToolkit.safeSyncExec(() -> {
				if (saveDefaultFont != null) {
					JFaceResources.getFontRegistry().put(JFaceResources.DEFAULT_FONT, saveDefaultFont);
				}
				if (saveTextFont != null) {
					JFaceResources.getFontRegistry().put(JFaceResources.TEXT_FONT, saveTextFont);
				}
			});
		}
	};

	/**
	 * Verify that the Mbean Browser page Operations works as expected
	 */
	@Test
	public void testThreadInfo() {
		// Select the Threading MBean
		MCTree.getByName(MBEANBROWSER_TREE_NAME).select("java.lang", "Threading");

		// Select the attributes tab
		MCTabFolder.getByTabName(ATTRIBUTES_TAB).select(ATTRIBUTES_TAB);

		// Finding the table that contains an item that matches the command we want to perform
		MCTable operationsTable = getOperationsTable(THREAD_INFO_COMMAND);

		MCTree paramsTree = null;

		// Get the indexes of the matching commands
		List<Integer> indexes = operationsTable.getItemIndexes(THREAD_INFO_COMMAND);

		// For each of the indexes select each item until we find the one we need
		for (int i : indexes) {
			operationsTable.select(i);
			MCTree thisTree = MCTree.getByItem("p0");
			if (!thisTree.hasItem("p1")) {
				paramsTree = thisTree;
				break;
			}
		}
		// Make sure that we actually found a matching command
		Assert.assertNotNull("Could not find the parameter tree", paramsTree);

		// Invoke the operation to get info about the first thread (main)
		paramsTree.select("p0");
		paramsTree.enterText("1");
		MCButton.getByLabel(EXECUTE_COMMAND).click();

		// Try to select the item "threadName" from the result tree
		MCTree resultTree = MCTree.getByName(RESULT_TREE_NAME);
		resultTree.select("threadName");
	}
	
	/**
	 * Verify that the Mbean Browser page Notifications works as expected
	 */
	@Test
	public void testGcSubscription() {
		Assume.assumeTrue("This feature is only valid on JDK7u0 or later.",
				ConnectionHelper.is7u0orLater(TEST_CONNECTION));

		MCTree mBeanTree = MCTree.getByName(MBEANBROWSER_TREE_NAME);

		// find a suitable collector to operate on
		mBeanTree.select("java.lang", "GarbageCollector");
		mBeanTree.expand();
		String collector = null;
		for (String thisCollector : Arrays.asList("PS MarkSweep", "MarkSweepCompact", "G1 Old Generation")) {
			if (mBeanTree.hasItem("java.lang", "GarbageCollector", thisCollector)) {
				collector = thisCollector;
				break;
			}
		}
		Assert.assertNotNull("Could not find a suitable collector", collector);

		// select the collector found
		mBeanTree.select("java.lang", "GarbageCollector", collector);

		// Select the Notifications tab
		MCTabFolder.getByTabName(NOTIFICATIONS_TAB).select(NOTIFICATIONS_TAB);

		// for the garbage collector found; read the current (latest) log entry in the tree named
		// "mbeanbrowser.notificationsTab.LogInspectorTree"
		List<String> logEntryBeforeGC = getLatestNotificationLogEntry(
				MCTree.getByName(MBEANBROWSER_NOTIFICATIONSTAB_LOGTREE_NAME));

		// turn on subscription for the garbage collector
		MCButton.getByLabel("Subscribe").setState(true);

		// switch to the memory tab and click the ToolItem with the tooltip "Run a full garbage collection"
		JmxConsole.selectTab(JmxConsole.Tabs.MEMORY);

		MCToolBar.getByToolTip("Run a full garbage collection").clickToolItem("Run a full garbage collection");

		// switch back to MBean Browser
		JmxConsole.selectTab(JmxConsole.Tabs.MBEAN_BROWSER);

		// Select the Notifications tab
		MCTabFolder.getByTabName(NOTIFICATIONS_TAB).select(NOTIFICATIONS_TAB);

		// select the collector found (previously)
		mBeanTree.select("java.lang", "GarbageCollector", collector);

		// for the garbage collector found; read the current (latest) log entry in the tree named
		// "mbeanbrowser.notificationsTab.LogInspectorTree"
		List<String> logEntryAfterGC = getLatestNotificationLogEntry(
				MCTree.getByName(MBEANBROWSER_NOTIFICATIONSTAB_LOGTREE_NAME));

		// Verify that we actually get a new event in the log
		verifyNewNotification(logEntryBeforeGC, logEntryAfterGC);
	}

	/**
	 * Gets the allocated bytes for a single thread from the UI and checks that the value is as
	 * expected
	 */
	@Test
	public void testOneThreadAtaTime() {
		Assume.assumeTrue("This feature is only valid on JDK7u0 or later.",
				ConnectionHelper.is7u0orLater(TEST_CONNECTION));

		for (int i = 0; i < threadIds.length; i++) {
			MCTree mBeanTree = MCTree.getByName(MBEANBROWSER_TREE_NAME);
			mBeanTree.select("java.lang", "Threading");

			// Finding the table that contains an item that matches the command we want to perform
			MCTable operationsTable = getOperationsTable(THREAD_INFO_COMMAND);
			operationsTable.select(THREAD_ALLOC_BYTES_COMMAND, true);
			MCTree paramsTree = MCTree.getByItem("p0");
			paramsTree.select("p0");
			paramsTree.enterText(Long.toString(threadIds[i]));

			MCButton.getByLabel(EXECUTE_COMMAND).click();

			// Get the result
			MCTabFolder resultFolder = MCTabFolder.getByName(RESULT_TAB_NAME);

			// Verify
			assertAllocatedBytes(BYTES_TO_ALLOC[i], Long.parseLong(resultFolder.getText()));
		}
	}

	/**
	 * Gets the allocated bytes for two threads from the UI and checks that the values are as
	 * expected
	 */
	@Test
	public void testTwoThreadsAtaTime() {
		Assume.assumeTrue("This feature is only valid on JDK7u0 or later.",
				ConnectionHelper.is7u0orLater(TEST_CONNECTION));

		MCTree mBeanTree = MCTree.getByName(MBEANBROWSER_TREE_NAME);
		mBeanTree.select("java.lang", "Threading");

		// Finding the table that contains an item that matches the command we want to perform
		MCTable operationsTable = getOperationsTable(THREAD_INFO_COMMAND);
		operationsTable.select(THREAD_ALLOC_BYTES_COMMAND + "[]");

		MCTree paramsTree = MCTree.getByItem("p0");
		paramsTree.select("p0");
		paramsTree.enterText("2");
		for (int i = 0; i < 2; i++) {
			paramsTree.select("p0", "[" + i + "]");
			paramsTree.enterText(Long.toString(threadIds[i]));
		}

		MCButton.getByLabel(EXECUTE_COMMAND).click();

		// Get the result tree
		MCTree resultTree = MCTree.getByName(RESULT_TREE_NAME);

		// for each thread select the result and verify
		for (int i = 0; i < 2; i++) {
			resultTree.select("[" + i + "]");
			assertAllocatedBytes(BYTES_TO_ALLOC[i], Long.parseLong(resultTree.getSelectedItemText("Value")));
		}
	}

	/**
	 * Attempts to get allocated bytes for invalid thread id, -1
	 */
	@Test
	public void testInvalidThreadId() {
		Assume.assumeTrue("This feature is only valid on JDK7u0 or later.",
				ConnectionHelper.is7u0orLater(TEST_CONNECTION));

		MCTree mBeanTree = MCTree.getByName(MBEANBROWSER_TREE_NAME);
		mBeanTree.select("java.lang", "Threading");

		// Finding the table that contains an item that matches the command we want to perform
		MCTable operationsTable = getOperationsTable(THREAD_INFO_COMMAND);
		operationsTable.select(THREAD_ALLOC_BYTES_COMMAND, true);
		MCTree paramsTree = MCTree.getByItem("p0");
		paramsTree.select("p0");
		paramsTree.enterText("-1");

		MCButton.getByLabel(EXECUTE_COMMAND).click();

		// Get the result
		MCTabFolder resultFolder = MCTabFolder.getByName(RESULT_TAB_NAME);

		// Verify
		Assert.assertTrue("Invalid thread ID parameter: -1",
				patternMatcher(resultFolder.getText(), "Invalid thread ID parameter: -1"));
	}

	/**
	 * Attempts to get allocated bytes for non-existent thread id, 1000 Note: we're just assuming no
	 * thread id 1000 exists which should be fine in the test environment
	 */
	@Test
	public void testNonExistingThreadId() {
		Assume.assumeTrue("This feature is only valid on JDK7u0 or later.",
				ConnectionHelper.is7u0orLater(TEST_CONNECTION));

		MCTree mBeanTree = MCTree.getByName(MBEANBROWSER_TREE_NAME);
		mBeanTree.select("java.lang", "Threading");

		// Finding the table that contains an item that matches the command we want to perform
		MCTable operationsTable = getOperationsTable(THREAD_INFO_COMMAND);
		operationsTable.select(THREAD_ALLOC_BYTES_COMMAND, true);
		MCTree paramsTree = MCTree.getByItem("p0");
		paramsTree.select("p0");
		paramsTree.enterText("1000");

		MCButton.getByLabel(EXECUTE_COMMAND).click();

		// Get the result
		MCTabFolder resultFolder = MCTabFolder.getByName(RESULT_TAB_NAME);

		// Verify
		Assert.assertTrue("-1", patternMatcher(resultFolder.getText(), "-1"));
	}

	/**
	 * Verify that the Mbean Browser page Operations works as expected
	 */
	@Test
	public void testValueFontSize() {
		// Select the Threading MBean
		MCTree.getByName(MBEANBROWSER_TREE_NAME).select("java.lang", "Threading");

		// Select the attributes tab
		MCTabFolder.getByTabName(ATTRIBUTES_TAB).select(ATTRIBUTES_TAB);

		// Finding the table that contains an item that matches the command we want to perform
		MCTable operationsTable = getOperationsTable(THREAD_INFO_COMMAND);

		MCTree paramsTree = null;

		// Get the indexes of the matching commands
		List<Integer> indexes = operationsTable.getItemIndexes(THREAD_INFO_COMMAND);

		// For each of the indexes select each item until we find the one we need
		for (int i : indexes) {
			operationsTable.select(i);
			MCTree thisTree = MCTree.getByItem("p0");
			if (!thisTree.hasItem("p1")) {
				paramsTree = thisTree;
				break;
			}
		}
		// Make sure that we actually found a matching command
		Assert.assertNotNull("Could not find the parameter tree", paramsTree);

		// Get the font used by the Value column in the tree
		int valueColIdx = paramsTree.getColumnIndex(VALUE_COLUMN_NAME);
		paramsTree.select("p0");
		List<Font> fonts = paramsTree.getSelectedItemFonts();
		Font valueFont = fonts.get(valueColIdx);

		// Ensure that the font is the text font, but sized to match the default font
		final Font[] textFontHolder = new Font[1];
		DisplayToolkit.safeSyncExec(() -> {
			textFontHolder[0] = JFaceResources.getFontRegistry().getItalic(JFaceResources.TEXT_FONT);
		});
		FontData[] expectedFontData = FontDescriptor.createFrom(textFontHolder[0]).setHeight(DEFAULT_FONT_HEIGHT).getFontData();
		Assert.assertArrayEquals(expectedFontData, valueFont.getFontData());
	}

	private List<String> getLatestNotificationLogEntry(MCTree logTree) {
		List<List<String>> log = logTree.getAllItemTexts();
		if (log.size() == 0) {
			return null;
		} else {
			return log.get(log.size() - 1); // Assume that the last entry is the latest one.
		}
	}

	private MCTable getOperationsTable(String command) {
		// Switching to the operations tab
		MCTabFolder.getByTabName(OPERATIONS_TAB).select(OPERATIONS_TAB);
		MCTable operationsTable = null;
		for (MCTable table : MCJemmyBase.getTables()) {
			if (table.hasItem(command)) {
				operationsTable = table;
				break;
			}
		}
		Assert.assertNotNull("Could not find the operations table", operationsTable);
		return operationsTable;
	}

	private void verifyNewNotification(List<String> before, List<String> after) {
		DateFormat df = MBeanNotificationLogInspector.getDateFormat();
		try {
			if (before == null) {
				Assert.assertNotNull("There isn't any newer log entry!", after);
			} else {
				Calendar beforeCal = Calendar.getInstance();
				Calendar afterCal = Calendar.getInstance();
				beforeCal.setTime(df.parse(before.get(0)));
				afterCal.setTime(df.parse(after.get(0)));
				Assert.assertTrue("There isn't any newer log entry!", beforeCal.before(afterCal));
			}
		} catch (ParseException e) {
			Assert.fail(e.getMessage());
		}

	}

	private static boolean patternMatcher(String matchText, String textStringToMatch) {
		if (textStringToMatch.equals("")) {
			return (matchText.equals(""));
		}

		Pattern pattern = Pattern.compile(textStringToMatch);
		Matcher matcher = pattern.matcher(matchText);
		return matcher.find();
	}

	/*
	 * Allocates the specified number of bytes and then waits until signaled before finishing
	 */
	static class AllocJob implements Runnable {

		private final long bytesToAlloc;
		private boolean waiting = false;

		AllocJob(long bytesToAlloc) {
			this.bytesToAlloc = bytesToAlloc;
		}

		boolean isWaiting() {
			synchronized (this) {
				return waiting;
			}
		}

		@Override
		public void run() {
			byte[] buffer = new byte[(int) bytesToAlloc];
			synchronized (this) {
				waiting = true;
				try {
					wait();
				} catch (InterruptedException e) { /* Ignore */
				}
			}
			// Adding the following to avoid optimizations that could possibly
			// detect that the result is not used and remove the alloc.
			if (buffer.length > 1000 * 1024 * 1024) {
				System.err.println("Should not get here!");
			}
		}
	}

	private static void initialiseTestThreads() {
		// Allocate a different number of bytes in each thread
		BYTES_TO_ALLOC[0] = 5 * 1024 * 1024;
		BYTES_TO_ALLOC[1] = 10 * 1024 * 1024;

		// Start the test threads
		threadIds[0] = startAllocThread(0);
		threadIds[1] = startAllocThread(1);
	}

	private static void terminateTestThreads() {
		terminateAllocThread(0);
		terminateAllocThread(1);
	}

	private static long startAllocThread(int threadIndex) {
		// Create the job for the thread and start the thread
		AllocJob job = new AllocJob(BYTES_TO_ALLOC[threadIndex]);
		jobs[threadIndex] = job;
		Thread allocThread = new Thread(job);
		allocThread.start();

		// Wait for the job to reach the waiting state
		while (!job.isWaiting()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) { /* Do nothing */
			}
		}

		return allocThread.getId();
	}

	private static void terminateAllocThread(int threadIndex) {
		synchronized (jobs[threadIndex]) {
			jobs[threadIndex].notifyAll();
		}
	}

	private void assertAllocatedBytes(long expectedAllocatedBytes, long allocatedBytesFromUi) {
		// Expect tolerance to be within 0.1% i.e. factor of 0.001
		double tolerance = expectedAllocatedBytes * 0.001;
		Assert.assertEquals(expectedAllocatedBytes, allocatedBytesFromUi, tolerance);
	}

}
