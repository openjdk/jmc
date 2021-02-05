/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2021, Red Hat Inc. All rights reserved.
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCChartCanvas;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrNavigator;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrUi;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCButton;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCSashForm;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCScrolledComposite;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCText;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTextCanvas;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCToolBar;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.misc.TimeFilter;

public class JfrThreadsPageTest extends MCJemmyTestBase {

	private static final int TIME_OFFSET = Calendar.SECOND * 7;
	private static final String PLAIN_JFR = "plain_recording.jfr";
	private static final String TABLE_COLUMN_HEADER = "Thread";
	private static final String OK_BUTTON = "OK";
	private static final String RESET_BUTTON = "Reset";
	private static final String FOLD_CHART = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.ThreadsPage_FOLD_CHART_TOOLTIP;
	private static final String FOLD_TABLE = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.ThreadsPage_FOLD_TABLE_TOOLTIP;
	private static final String HIDE_THREAD = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.ThreadsPage_HIDE_THREAD_ACTION;
	private static final String RESET_CHART = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.ThreadsPage_RESET_CHART_TO_SELECTION_ACTION;
	private static final String SCROLLED_COMPOSITE_NAME = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.ThreadsPage_SCROLLED_COMPOSITE_NAME;
	private static final String SHOW_CHART = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.ThreadsPage_SHOW_CHART_TOOLTIP;
	private static final String SHOW_TABLE = org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.ThreadsPage_SHOW_TABLE_TOOLTIP;
	private static final String TIME_FILTER_ERROR = org.openjdk.jmc.ui.misc.Messages.TimeFilter_ERROR;

	private static MCChartCanvas chartCanvas;
	private static MCSashForm sashForm;
	private static MCScrolledComposite scrolledComposite;
	private static MCTextCanvas textCanvas;
	private static MCTable threadsTable;
	private static MCText startTimeText;
	private static MCText endTimeText;
	private static MCToolBar toolbar;
	private boolean scHorizontalBarVisible;
	private boolean selected;
	private final int numLanesSelection = 5;
	private final int minLaneHeight = 25;
	private SimpleDateFormat sdf = new SimpleDateFormat(TimeFilter.timeFormat);

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			JfrUi.openJfr(materialize("jfr", PLAIN_JFR, JfrThreadsPageTest.class));
			JfrNavigator.selectTab(JfrUi.Tabs.THREADS);
			scrolledComposite = MCScrolledComposite.getByName(SCROLLED_COMPOSITE_NAME);
			scHorizontalBarVisible = scrolledComposite.isHorizontalBarVisible();
			toolbar = MCToolBar.getByToolTip(SHOW_TABLE);
			toolbar.clickToolItem(SHOW_TABLE);
			chartCanvas = MCChartCanvas.getChartCanvas();
			textCanvas = MCTextCanvas.getTextCanvas();
			selected = false;
			sashForm = MCSashForm.getMCSashForm();
			threadsTable = MCTable.getByColumnHeader(TABLE_COLUMN_HEADER);
			startTimeText = MCText.getByName(TimeFilter.START_TIME_NAME);
			endTimeText = MCText.getByName(TimeFilter.END_TIME_NAME);
		}

		@Override
		public void after() {
			selected = false;
			scHorizontalBarVisible = false;
			toolbar.clickToolItem(FOLD_TABLE);
			MCMenu.closeActiveEditor();
		}
	};

	/**
	 * Some tests require interaction with the chart canvas, and may not work if widgets are
	 * obstructed. If the scrolled composite has a horizontal scrollbar, some widgets may not be
	 * visible. If the chart canvas is too short, then the tests might not be able to interact with
	 * the required lanes.
	 *
	 * @return true if all the controls required are visible
	 */
	private boolean isControlsVisible() {
		return !scHorizontalBarVisible && (chartCanvas.getHeight() >= numLanesSelection * minLaneHeight);
	}

	@Test
	public void testTextCanvasSelection() throws InterruptedException, ExecutionException, TimeoutException {
		Assume.assumeTrue(isControlsVisible());

		threadsTable.selectItems(0, 0);
		Assert.assertEquals(1, threadsTable.getSelectionCount());

		CompletableFuture<Void> future = new CompletableFuture<>();
		CompletableFuture.supplyAsync(new Supplier<Void>() {

			@Override
			public Void get() {
				textCanvas.setSelectionListener(() -> {
					selected = !selected;
					future.complete(null);
				});
				textCanvas.clickTextCanvas();
				return future.join();
			}

		}).get(10, TimeUnit.SECONDS);

		Assert.assertTrue(selected);
	}

	@Test
	public void testZoom() {
		Assume.assumeTrue(isControlsVisible());

		final String startTime = startTimeText.getText();
		MCButton zoomInBtn = MCButton.getByImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_FA_ZOOM_IN));
		MCButton zoomOutBtn = MCButton.getByImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_FA_ZOOM_OUT));

		// zoom with display bar
		zoomInBtn.click();
		chartCanvas.clickChart();
		Assert.assertNotEquals(startTime, startTimeText.getText());

		zoomOutBtn.click();
		chartCanvas.clickChart();
		Assert.assertEquals(startTime, startTimeText.getText());

		// zoom with controls
		chartCanvas.clickChart();
		chartCanvas.keyboardZoomIn();
		Assert.assertNotEquals(startTime, startTimeText.getText());

		chartCanvas.keyboardZoomOut();
		Assert.assertEquals(startTime, startTimeText.getText());
	}

	@Test
	public void testResetButtons() throws ParseException {
		Assume.assumeTrue(isControlsVisible());

		final String startTime = startTimeText.getText();
		Date startDate = sdf.parse(startTime);
		startDate.setTime(startDate.getTime() + TIME_OFFSET);
		final String newStartTime = sdf.format(new Date(startDate.getTime()));

		MCButton resetBtn = MCButton.getByLabel(RESET_BUTTON);
		MCButton scaleToFitBtn = MCButton.getByImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_FA_SCALE_TO_FIT));

		startTimeText.setText(newStartTime);
		Assert.assertNotEquals(startTime, startTimeText.getText());

		resetBtn.click();
		Assert.assertEquals(startTime, startTimeText.getText());

		startTimeText.setText(newStartTime);
		Assert.assertNotEquals(startTime, startTimeText.getText());

		scaleToFitBtn.click();
		Assert.assertEquals(startTime, startTimeText.getText());
	}

	@Test
	public void testTimeFilterInvalid() throws ParseException {
		Assume.assumeTrue(isControlsVisible());

		final String startTime = startTimeText.getText();
		Date startDate = sdf.parse(startTime);
		startDate.setTime(startDate.getTime() - TIME_OFFSET);
		final String invalidStartTime = sdf.format(new Date(startDate.getTime()));

		final String endTime = endTimeText.getText();
		Date endDate = sdf.parse(endTime);
		endDate.setTime(endDate.getTime() + TIME_OFFSET);
		final String invalidEndTime = sdf.format(new Date(endDate.getTime()));

		MCButton resetBtn = MCButton.getByLabel(RESET_BUTTON);

		startTimeText.setText(invalidStartTime);
		MCButton okButton = MCButton.getByLabel(TIME_FILTER_ERROR, OK_BUTTON);
		Assert.assertNotNull(okButton);
		okButton.click();

		MCButton.focusMc();
		resetBtn.click();
		Assert.assertEquals(startTime, startTimeText.getText());

		endTimeText.setText(invalidEndTime);
		okButton = MCButton.getByLabel(TIME_FILTER_ERROR, OK_BUTTON);
		Assert.assertNotNull(okButton);
		okButton.click();
	}

	@Test
	public void testMenuItemEnablement() {
		Assume.assumeTrue(isControlsVisible());

		final int numThreads = threadsTable.getItemCount();

		Assert.assertTrue(numThreads > 0);

		Assert.assertFalse(chartCanvas.isContextMenuItemEnabled(RESET_CHART));
		Assert.assertTrue(chartCanvas.isContextMenuItemEnabled(HIDE_THREAD));

		chartCanvas.clickContextMenuItem(HIDE_THREAD);

		Assert.assertTrue(chartCanvas.isContextMenuItemEnabled(RESET_CHART));
		Assert.assertTrue(chartCanvas.isContextMenuItemEnabled(HIDE_THREAD));

		chartCanvas.clickContextMenuItem(RESET_CHART);

		chartCanvas = MCChartCanvas.getChartCanvas();
		Assert.assertFalse(chartCanvas.isContextMenuItemEnabled(RESET_CHART));
		Assert.assertTrue(chartCanvas.isContextMenuItemEnabled(HIDE_THREAD));
	}

	@Test
	public void testHideAllThreads() {
		Assume.assumeTrue(isControlsVisible());

		final int numThreads = threadsTable.getItemCount();

		Assert.assertTrue(numThreads > 0 && numThreads >= numLanesSelection);
		Assert.assertTrue(chartCanvas.isContextMenuItemEnabled(HIDE_THREAD));
		Assert.assertFalse(chartCanvas.isContextMenuItemEnabled(RESET_CHART));

		// Select a limited number of threads in the chart using the table
		threadsTable.selectItems(0, numLanesSelection - 1);

		// Hide all the threads from the chart
		for (int i = 0; i < numLanesSelection; i++) {
			chartCanvas.clickContextMenuItem(HIDE_THREAD);
		}

		// Once all threads are hidden from the chart, the hide thread menu item will be disabled
		Assert.assertFalse(chartCanvas.isContextMenuItemEnabled(HIDE_THREAD));
		Assert.assertTrue(chartCanvas.isContextMenuItemEnabled(RESET_CHART));

		chartCanvas.clickContextMenuItem(RESET_CHART);

		// Verify the menu item isEnabled values are back to their default values
		chartCanvas = MCChartCanvas.getChartCanvas();
		Assert.assertTrue(chartCanvas.isContextMenuItemEnabled(HIDE_THREAD));
		Assert.assertFalse(chartCanvas.isContextMenuItemEnabled(RESET_CHART));
	}

	@Test
	public void testFoldingChart() {
		// Sash weights should both be non-zero to display the chart and table
		Assert.assertTrue(sashForm.getWeights()[0] != 0 && sashForm.getWeights()[1] != 0);

		// Sash weight corresponding to the chart should be zero when folded
		toolbar.clickToolItem(FOLD_CHART);
		Assert.assertTrue(sashForm.getWeights()[0] != 0 && sashForm.getWeights()[1] == 0);

		// When unfolded, the sash weights should be non-zero
		toolbar.clickToolItem(SHOW_CHART);
		Assert.assertTrue(sashForm.getWeights()[0] != 0 && sashForm.getWeights()[1] != 0);
	}

	@Test
	public void testFoldingTable() {
		// Sash weights should both be non-zero to display the chart and table
		Assert.assertTrue(sashForm.getWeights()[0] != 0 && sashForm.getWeights()[1] != 0);

		// Sash weight corresponding to the table should be zero when folded
		toolbar.clickToolItem(FOLD_TABLE);
		Assert.assertTrue(sashForm.getWeights()[0] == 0 && sashForm.getWeights()[1] != 0);

		// When unfolded, the sash weights should be non-zero
		toolbar.clickToolItem(SHOW_TABLE);
		Assert.assertTrue(sashForm.getWeights()[0] != 0 && sashForm.getWeights()[1] != 0);
	}

	@Test
	public void testInvalidFoldingActions() {
		toolbar.clickToolItem(FOLD_TABLE);
		int[] weights = sashForm.getWeights();
		toolbar.clickToolItem(FOLD_CHART);
		// If the table is already folded, the fold chart action shouldn't work
		Assert.assertTrue(Arrays.equals(weights, sashForm.getWeights()));
		toolbar.clickToolItem(SHOW_TABLE);

		toolbar.clickToolItem(FOLD_CHART);
		weights = sashForm.getWeights();
		toolbar.clickToolItem(FOLD_TABLE);
		// If the chart is already folded, the fold table action shouldn't work
		Assert.assertTrue(Arrays.equals(weights, sashForm.getWeights()));

		// Bring back the chart before retiring
		toolbar.clickToolItem(SHOW_CHART);
	}

	@Test
	public void testPersistingSashWeights() {
		// Fold the table away
		toolbar.clickToolItem(FOLD_TABLE);
		int[] weights = sashForm.getWeights();
		Assert.assertTrue(sashForm.getWeights()[0] == 0 && sashForm.getWeights()[1] != 0);
		MCMenu.closeActiveEditor();

		// Re-open the JFR file & verify the table is still folded
		JfrUi.openJfr(materialize("jfr", PLAIN_JFR, JfrThreadsPageTest.class));
		JfrNavigator.selectTab(JfrUi.Tabs.THREADS);
		sashForm = MCSashForm.getMCSashForm();
		Assert.assertTrue(Arrays.equals(weights, sashForm.getWeights()));

		// Bring back the table before retiring
		toolbar = MCToolBar.getByToolTip(SHOW_TABLE);
		toolbar.clickToolItem(SHOW_TABLE);
	}
}
