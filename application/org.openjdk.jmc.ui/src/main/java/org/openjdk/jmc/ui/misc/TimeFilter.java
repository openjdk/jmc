/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2021 Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.ui.misc;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.misc.PatternFly.Palette;

public class TimeFilter extends Composite {

	private enum FilterType {
		START, END
	};

	public static final String START_TIME_NAME = "timefilter.startTime.text.name"; //$NON-NLS-1$
	public static final String END_TIME_NAME = "timefilter.endTime.text.name"; //$NON-NLS-1$
	public static final String dateFormat = "yyyy-MM-dd ";
	public static final String timeFormat = "HH:mm:ss:SSS";
	private boolean isMultiDayRecording = false;
	public Calendar calendar;
	private ChartCanvas chartCanvas;
	private XYChart chart;
	private SimpleDateFormat sdf;
	private SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
	private TimeDisplay startDisplay;
	private TimeDisplay endDisplay;

	public TimeFilter(Composite parent, IRange<IQuantity> recordingRange, Listener resetListener) {
		super(parent, SWT.NONE);
		this.setLayout(new GridLayout(7, false));

		inspectRecordingRange(recordingRange);

		Label eventsLabel = new Label(this, SWT.LEFT);
		eventsLabel.setText(Messages.TimeFilter_FILTER_EVENTS);
		eventsLabel.setFont(JFaceResources.getFontRegistry().get(JFaceResources.BANNER_FONT));

		Label fromLabel = new Label(this, SWT.CENTER);
		fromLabel.setText(Messages.TimeFilter_FROM);

		startDisplay = new TimeDisplay(this, FilterType.START, recordingRange.getStart());

		Label toLabel = new Label(this, SWT.CENTER);
		toLabel.setText(Messages.TimeFilter_TO);

		endDisplay = new TimeDisplay(this, FilterType.END, recordingRange.getEnd());

		Button resetBtn = new Button(this, SWT.PUSH);
		resetBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		resetBtn.setText(Messages.TimeFilter_RESET);
		resetBtn.addListener(SWT.Selection, resetListener);
	}

	/**
	 * Determines whether or not the time range of the recording spans multiple days, and if not,
	 * sets up a Calendar object to hold the date of the recording.
	 * 
	 * @param recordingRange
	 */
	private void inspectRecordingRange(IRange<IQuantity> recordingRange) {
		long firstDateEpoch = recordingRange.getStart().in(UnitLookup.EPOCH_MS).longValue();
		long secondDateEpoch = recordingRange.getEnd().in(UnitLookup.EPOCH_MS).longValue();
		isMultiDayRecording = !dateFormatter.format(firstDateEpoch).equals(dateFormatter.format(secondDateEpoch));
		if (!isMultiDayRecording) {
			calendar = Calendar.getInstance();
			calendar.setTimeInMillis(firstDateEpoch);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			calendar.add(Calendar.MILLISECOND, calendar.getTimeZone().getRawOffset());
		}
	}

	protected void updateRange() {
		chart.setVisibleRange(startDisplay.getCurrentTime(), endDisplay.getCurrentTime());
		chartCanvas.redrawChart();
	}

	public void setChart(XYChart chart) {
		this.chart = chart;
	}

	public void setChartCanvas(ChartCanvas canvas) {
		this.chartCanvas = canvas;
	}

	public void setStartTime(IQuantity time) {
		startDisplay.setTime(time);
	}

	public void setEndTime(IQuantity time) {
		endDisplay.setTime(time);
	}

	private class TimeDisplay extends Composite {

		private boolean bypassModifyListener;
		private FilterType type;
		private int lastEventTime;
		private IQuantity defaultTime;
		private IQuantity currentTime;
		private Text timeText;

		public TimeDisplay(TimeFilter parent, FilterType type, IQuantity defaultTime) {
			super(parent, SWT.NONE);
			this.type = type;
			this.defaultTime = defaultTime;
			this.setLayout(new GridLayout());
			timeText = new Text(this, SWT.SEARCH | SWT.SINGLE);
			timeText.setData("name", type == FilterType.START ? START_TIME_NAME : END_TIME_NAME); //$NON-NLS-1$

			// if the recording spans multiple days, include the date in the time display
			if (!isMultiDayRecording) {
				timeText.setTextLimit(12);
				sdf = new SimpleDateFormat(timeFormat);
			} else {
				timeText.setTextLimit(23);
				sdf = new SimpleDateFormat(dateFormat + timeFormat);
			}
			timeText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					if (getBypassModifyListener()) {
						setBypassModifyListener(false);
						return;
					}

					/**
					 * If the user edits a Text by highlighting a character and overwrites it with a
					 * new one, the ModifyListener will fire twice. To prevent validation (and
					 * potential error dialogs) from occurring twice, compare the time of the
					 * current ModifyEvent to the last seen ModifyEvent.
					 */
					if (e.time == lastEventTime) {
						return;
					} else {
						lastEventTime = e.time;
					}

					String newTimestring = timeText.getText();
					if (!isValidSyntax(newTimestring)) {
						return;
					}
					IQuantity newTime = convertStringToIQuantity(newTimestring);
					if (currentTime == null || newTime == null) {
						return;
					}
					if (isWithinRange(newTime)) {
						timeText.setForeground(Palette.PF_BLACK.getSWTColor());
						currentTime = newTime;
						parent.updateRange();
					} else {
						timeText.setForeground(Palette.PF_RED_100.getSWTColor());
					}
				}
			});
		}

		/**
		 * Converts the IQuantity time to a string and displays it in the Text
		 * 
		 * @param time
		 *            IQuantity
		 */
		public void setTime(IQuantity time) {
			setBypassModifyListener(true);
			String timestring = sdf.format(new Date(time.in(UnitLookup.EPOCH_MS).longValue()));
			this.currentTime = time;
			timeText.setText(timestring);
			timeText.setForeground(Palette.PF_BLACK.getSWTColor());
			setBypassModifyListener(false);
		}

		/**
		 * Converts a formatted time string into an IQuantity. If the recording range is within a
		 * single day, the SimpleDateFormat format will be HH:mm:ss:SSS and need to be added to the
		 * base date (calendar) in order to calculate the epoch milliseconds.
		 *
		 * @param timestring
		 *            String
		 * @return IQuantity
		 */
		private IQuantity convertStringToIQuantity(String timestring) {
			try {
				long parsedTime = sdf.parse(timestring).getTime();
				if (!isMultiDayRecording) {
					parsedTime += calendar.getTimeInMillis();
				}
				return UnitLookup.EPOCH_MS.quantity(parsedTime);
			} catch (ParseException e) {
			}
			return null;
		}

		/**
		 * Verify that the passed time is within the recording range
		 * 
		 * @param time
		 *            IQuantity
		 * @return true if the specified time is within the time range of the recording
		 */
		private boolean isWithinRange(IQuantity time) {
			if (time == null) {
				return false;
			}
			long timeMillis = time.in(UnitLookup.EPOCH_MS).longValue();
			if (type == FilterType.START) {
				if (timeMillis < defaultTime.in(UnitLookup.EPOCH_MS).longValue()) {
					DialogToolkit.showWarning(getDisplay().getActiveShell(), Messages.TimeFilter_ERROR,
							Messages.TimeFilter_START_TIME_PRECEEDS_ERROR);
					return false;
				} else if (timeMillis > endDisplay.getDefaultTime().in(UnitLookup.EPOCH_MS).longValue()
						|| timeMillis > endDisplay.getCurrentTime().in(UnitLookup.EPOCH_MS).longValue()) {
					DialogToolkit.showWarning(getDisplay().getActiveShell(), Messages.TimeFilter_ERROR,
							Messages.TimeFilter_START_TIME_LONGER_THAN_END_ERROR);
					endDisplay.getDefaultTime().in(UnitLookup.EPOCH_MS).longValue();
					return false;
				}
			} else {
				if (timeMillis > defaultTime.in(UnitLookup.EPOCH_MS).longValue()) {
					DialogToolkit.showWarning(getDisplay().getActiveShell(), Messages.TimeFilter_ERROR,
							Messages.TimeFilter_END_TIME_EXCEEDS_ERROR);
					return false;
				} else if (timeMillis < startDisplay.getDefaultTime().in(UnitLookup.EPOCH_MS).longValue()
						|| timeMillis < startDisplay.getCurrentTime().in(UnitLookup.EPOCH_MS).longValue()) {
					DialogToolkit.showWarning(getDisplay().getActiveShell(), Messages.TimeFilter_ERROR,
							Messages.TimeFilter_START_TIME_LONGER_THAN_END_ERROR);
					return false;
				}
			}
			return true;
		}

		/**
		 * Verify that the passed time string matches the expected time format
		 * 
		 * @param formattedTimestring
		 *            String
		 * @return true if the text corresponds to the current SimpleDateFormat format
		 */
		private boolean isValidSyntax(String formattedTimestring) {
			if (formattedTimestring.length() != timeText.getTextLimit()) {
				return false;
			}
			try {
				sdf.parse(formattedTimestring);
			} catch (ParseException e) {
				return false;
			}
			return true;
		}

		private IQuantity getDefaultTime() {
			return defaultTime;
		}

		private IQuantity getCurrentTime() {
			return currentTime;
		}

		// When programmatically changing the Text (e.g., this.setTime()), use
		// a boolean to prevent the ModifyListener from firing
		private boolean getBypassModifyListener() {
			return this.bypassModifyListener;
		}

		private void setBypassModifyListener(boolean bypassModifyListener) {
			this.bypassModifyListener = bypassModifyListener;
		}
	}
}
