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
package org.openjdk.jmc.flightrecorder.uitest;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCChartCanvas;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrNavigator;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrUi;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;

public class JfrThreadsPageTest extends MCJemmyTestBase {

	private static final String PLAIN_JFR = "plain_recording.jfr";
	private static final String TABLE_COLUMN_HEADER = "Thread";
	private static final String HIDE_THREAD = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.ThreadsPage_HIDE_THREAD_ACTION;
	private static final String RESET_CHART = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.ThreadsPage_RESET_CHART_TO_SELECTION_ACTION;

	private static MCChartCanvas chartCanvas;
	private static MCTable threadsTable;

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			JfrUi.openJfr(materialize("jfr", PLAIN_JFR, JfrThreadsPageTest.class));
			JfrNavigator.selectTab(JfrUi.Tabs.THREADS);
			threadsTable = MCTable.getByColumnHeader(TABLE_COLUMN_HEADER);
			chartCanvas = MCChartCanvas.getChartCanvas();
		}

		@Override
		public void after() {
			MCMenu.closeActiveEditor();
		}
	};

	@Test
	public void testMenuItemEnablement() {
		final int numThreads = threadsTable.getItemCount();
		Assert.assertTrue(numThreads > 0);

		Assert.assertFalse(chartCanvas.isContextMenuItemEnabled(RESET_CHART));
		Assert.assertTrue(chartCanvas.isContextMenuItemEnabled(HIDE_THREAD));

		chartCanvas.clickContextMenuItem(HIDE_THREAD);

		Assert.assertTrue(chartCanvas.isContextMenuItemEnabled(RESET_CHART));
		Assert.assertTrue(chartCanvas.isContextMenuItemEnabled(HIDE_THREAD));

		chartCanvas.clickContextMenuItem(RESET_CHART);

		Assert.assertFalse(chartCanvas.isContextMenuItemEnabled(RESET_CHART));
		Assert.assertTrue(chartCanvas.isContextMenuItemEnabled(HIDE_THREAD));
	}

	@Test
	public void testHideAllThreads() {
		final int numSelection = 7;
		final int numThreads = threadsTable.getItemCount();
		Assert.assertTrue(numThreads > 0 && numThreads >= numSelection);
		Assert.assertTrue(chartCanvas.isContextMenuItemEnabled(HIDE_THREAD));
		Assert.assertFalse(chartCanvas.isContextMenuItemEnabled(RESET_CHART));

		// Select a limited number of threads in the chart using the table
		threadsTable.selectItems(0, numSelection - 1);

		// Hide all the threads from the chart
		for (int i = 0; i < numSelection; i++) {
			chartCanvas.clickContextMenuItem(HIDE_THREAD);
		}

		// Once all threads are hidden from the chart, the hide thread menu item will be disabled
		Assert.assertFalse(chartCanvas.isContextMenuItemEnabled(HIDE_THREAD));
		Assert.assertTrue(chartCanvas.isContextMenuItemEnabled(RESET_CHART));

		chartCanvas.clickContextMenuItem(RESET_CHART);

		// Verify the menu item isEnabled values are back to their default values
		Assert.assertTrue(chartCanvas.isContextMenuItemEnabled(HIDE_THREAD));
		Assert.assertFalse(chartCanvas.isContextMenuItemEnabled(RESET_CHART));
	}
}

