/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.greychart.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_MS;
import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_NS;
import static org.openjdk.jmc.common.unit.UnitLookup.NANOSECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.greychart.ui.messages.internal.Messages;
import org.openjdk.jmc.greychart.ui.views.ChartModel.AxisRange;
import org.openjdk.jmc.greychart.ui.views.ChartModel.RangedAxis;
import org.openjdk.jmc.ui.misc.DateTimeChooser;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.QuantityKindProposal;

import org.openjdk.jmc.greychart.TickDensity;
import org.openjdk.jmc.greychart.data.RenderingMode;
import org.openjdk.jmc.greychart.impl.DefaultYAxis;

public class ChartMenuBuilder {

	public static IContributionItem createShowMenu(ChartComposite chart) {
		MenuManager m = new MenuManager(Messages.ChartComposite_SHOW_MENU_TEXT);
		m.add(createShow(Messages.ChartComposite_SHOW_LAST_15_SECONDS, 15 * ChartComposite.ONE_SECOND, chart));
		m.add(createShow(Messages.ChartComposite_SHOW_LAST_MINUTE, ChartComposite.ONE_MINUTE, chart));
		m.add(createShow(Messages.ChartComposite_SHOW_LAST_10_MINUTES, 10 * ChartComposite.ONE_MINUTE, chart));
		m.add(createShow(Messages.ChartComposite_SHOW_LAST_HOUR, ChartComposite.ONE_HOUR, chart));
		m.add(createShow(Messages.ChartComposite_SHOW_LAST_DAY, ChartComposite.ONE_DAY, chart));
		m.add(createShow(Messages.ChartComposite_SHOW_LAST_WEEK, ChartComposite.ONE_WEEK, chart));
		m.add(createShowAll(chart));
		m.add(createShowCustomLast(chart));
		m.add(createShowCustomRange(chart));
		return m;
	}

	private static Action createShow(String label, final long nanoSeconds, final ChartComposite chart) {
		return new Action(label) {
			@Override
			public void run() {
				chart.showLast(nanoSeconds);
			}
		};
	}

	private static Action createShowAll(final ChartComposite chart) {
		return new Action(Messages.ChartComposite_SHOW_ALL) {
			@Override
			public void run() {
				chart.showAll();
			}
		};
	}

	private static Action createShowCustomLast(final ChartComposite chart) {
		return new Action(Messages.ChartComposite_SHOW_CUSTOM_LAST) {
			@Override
			public void run() {
				CustomTimeInputDialog dialog = new CustomTimeInputDialog(Display.getCurrent().getActiveShell());
				if (dialog.open() == Window.OK) {
					chart.showLast(dialog.getTimeInNanoSeconds());
				}
			}
		};
	}

	private static Action createShowCustomRange(final ChartComposite chart) {
		return new Action(Messages.ChartComposite_SHOW_CUSTOM_RANGE) {
			@Override
			public void run() {
				CustomTimeRangeDialog dialog = new CustomTimeRangeDialog(Display.getCurrent().getActiveShell(), chart);
				if (dialog.open() == Window.OK) {
					chart.showTimeRange(dialog.getFromTimeNanos(), dialog.getToTimeNanos());
				}
			}
		};
	}

	public static IContributionItem createRangeMenu(final ChartModel chartModel) {
		MenuManager rangeMenu = new MenuManager(Messages.ChartComposite_MENU_Y_AXIS_RANGE_TEXT);
		rangeMenu.add(new Action(Messages.ChartComposite_MENU_AUTO_RANGE_TEXT) {
			@Override
			public void run() {
				chartModel.getYAxis().setRangeType(AxisRange.AUTO);
				chartModel.getYAxis().notifyObservers();
			};
		});
		rangeMenu.add(new Action(Messages.ChartComposite_MENU_AUTO_RANGE_ZERO_TEXT) {
			@Override
			public void run() {
				chartModel.getYAxis().setRangeType(AxisRange.AUTO_ZERO);
				chartModel.getYAxis().notifyObservers();
			};
		});
		rangeMenu.add(new Action(Messages.ChartComposite_MENU_CUSTOM_RANGE_TEXT) {
			@Override
			public void run() {
				RangedAxis yAxis = chartModel.getYAxis();
				RangeInputDialog rangeInputDialog = new RangeInputDialog(Display.getCurrent().getActiveShell(),
						yAxis.getKindOfQuantity(), yAxis.getMinValue(), yAxis.getMaxValue());
				if (rangeInputDialog.open() == Window.OK) {
					yAxis.setRangeType(AxisRange.CUSTOM);
					yAxis.setMinValue(rangeInputDialog.getFromValue());
					yAxis.setMaxValue(rangeInputDialog.getToValue());
					yAxis.notifyObservers();
				}
			};
		});
		return rangeMenu;
	}

	public static IContributionItem createLabelDensityMenu(final DefaultYAxis yAxis) {
		MenuManager labelDensityMenu = new MenuManager(Messages.ChartComposite_MENU_LABEL_DENSITY_TEXT);
		for (final TickDensity tickDensity : TickDensity.values()) {
			labelDensityMenu.add(new Action(TickDensityName.getReadableName(tickDensity)) {
				@Override
				public void run() {
					yAxis.setTickDensity(tickDensity);
				}
			});
		}
		return labelDensityMenu;
	}

	public static IContributionItem createRenderingModeMenu(ChartModel chartModel) {
		MenuManager renderingModeMenu = new MenuManager(Messages.ChartComposite_MENU_RENDERING_MODE_TEXT);
		renderingModeMenu.add(createRenderingMode(Messages.ChartComposite_MENU_RENDERING_MODE_SUBSAMPLING_TEXT,
				chartModel, RenderingMode.SUBSAMPLING));
		renderingModeMenu.add(createRenderingMode(Messages.ChartComposite_MENU_RENDERING_MODE_AVERAGING_TEXT,
				chartModel, RenderingMode.AVERAGING));
		return renderingModeMenu;
	}

	private static Action createRenderingMode(String menuName, final ChartModel chartModel, final RenderingMode mode) {
		return new Action(menuName) {
			@Override
			public void run() {
				chartModel.setRenderingMode(mode);
				chartModel.notifyObservers();
			}
		};
	}

	public static IContributionItem createTitleMenu(final ChartModel chartModel) {
		MenuManager titleMenu = new MenuManager(Messages.ChartComposite_MENU_EDIT_TITLES_TEXT);
		titleMenu.add(new Action(Messages.ChartComposite_MENU_GRAPH_TITLE_TEXT) {
			@Override
			public void run() {
				String defaultValue = chartModel.getChartTitle();
				InputDialog inputDialog = new InputDialog(Display.getCurrent().getActiveShell(),
						Messages.ChartComposite_INPUT_GRAPH_TITLE_TITLE,
						Messages.ChartComposite_INPUT_GRAPH_TITLE_MESSAGE, defaultValue, null);
				if (inputDialog.open() == Window.OK) {
					chartModel.setChartTitle(inputDialog.getValue());
					chartModel.notifyObservers();
				}
			}
		});

		titleMenu.add(new Action(Messages.ChartComposite_MENU_Y_AXIS_TITLE_TEXT) {
			@Override
			public void run() {
				InputDialog inputDialog = new InputDialog(Display.getCurrent().getActiveShell(),
						Messages.ChartComposite_INPUT_Y_AXIS_TITLE_TITLE,
						Messages.ChartComposite_INPUT_Y_AXIS_TITLE_MESSAGE, chartModel.getYAxis().getTitle(), null);
				if (inputDialog.open() == Window.OK) {
					chartModel.getYAxis().setTitle(inputDialog.getValue());
					chartModel.getYAxis().notifyObservers();
				}
			}
		});

		titleMenu.add(new Action(Messages.ChartComposite_MENU_X_AXIS_TITLE_TEXT) {
			@Override
			public void run() {
				InputDialog inputDialog = new InputDialog(Display.getCurrent().getActiveShell(),
						Messages.ChartComposite_INPUT_X_AXIS_TITLE_TITLE,
						Messages.ChartComposite_INPUT_X_AXIS_TITLE_MESSAGE, chartModel.getXAxis().getTitle(), null);
				if (inputDialog.open() == Window.OK) {
					chartModel.getXAxis().setTitle(inputDialog.getValue());
					chartModel.getXAxis().notifyObservers();
				}
			}
		});
		return titleMenu;
	}

	public static class RangeInputDialog extends TitleAreaDialog {
		private Text fromText;
		private Text toText;
		private final KindOfQuantity<?> kindOfQuantity;
		private IQuantity fromValue;
		private IQuantity toValue;

		public RangeInputDialog(Shell parentShell, KindOfQuantity<?> kindOfQuantity, IQuantity fromValue,
				IQuantity toValue) {
			super(parentShell);
			this.kindOfQuantity = kindOfQuantity;
			this.fromValue = fromValue;
			this.toValue = toValue;
		}

		private void validateInput() {
			String errorMessage = null;
			try {
				IQuantity fromQuantity = kindOfQuantity.parseInteractive(fromText.getText());
				IQuantity toQuantity = kindOfQuantity.parseInteractive(toText.getText());
				if (fromQuantity.compareTo(toQuantity) > 0) {
					errorMessage = Messages.ChartComposite_DIALOG_RANGE_INPUT_MESSAGE_FROM_SMALLER_THAN_TO;
				}
			} catch (QuantityConversionException e) {
				errorMessage = e.getLocalizedMessage();
			}
			getButton(IDialogConstants.OK_ID).setEnabled(errorMessage == null);
			setErrorMessage(errorMessage);
		}

		@Override
		protected Control createContents(Composite parent) {
			getShell().setText(Messages.ChartComposite_DIALOG_RANGE_INPUT_TITLE);
			Control contents = super.createContents(parent);
			contents.getShell().setSize(400, 250);
			DisplayToolkit.placeDialogInCenter(getParentShell(), getShell());
			setMessage(Messages.ChartComposite_DIALOG_RANGE_INPUT_TEXT);
			setTitle(Messages.ChartComposite_DIALOG_RANGE_INPUT_TITLE);
			validateInput();
			return contents;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite composite = (Composite) super.createDialogArea(parent);
			createCustomArea(composite);
			applyDialogFont(composite);
			return composite;
		}

		private Control createCustomArea(Composite parent) {
			Composite numberFields = new Composite(parent, SWT.NONE);
			numberFields.setLayout(new GridLayout(2, false));
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.widthHint = 80;
			Label minimumLabel = new Label(numberFields, SWT.NONE);
			minimumLabel.setText(Messages.ChartComposite_DIALOG_RANGE_INPUT_MINIMUM_TEXT);
			minimumLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			fromText = new Text(numberFields, SWT.SINGLE | SWT.BORDER);
			fromText.setLayoutData(gd);
			fromText.setFocus();
			fromText.setText(fromValue.interactiveFormat());
			fromText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					validateInput();
				}
			});
			QuantityKindProposal.install(fromText, kindOfQuantity);
			Label maximumLabel = new Label(numberFields, SWT.NONE);
			maximumLabel.setText(Messages.ChartComposite_DIALOG_RANGE_INPUT_MAXIMUM_TEXT);
			maximumLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			toText = new Text(numberFields, SWT.SINGLE | SWT.BORDER);
			toText.setLayoutData(gd);
			toText.setText(toValue.interactiveFormat());
			toText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					validateInput();
				}
			});
			QuantityKindProposal.install(toText, kindOfQuantity);
			return numberFields;
		}

		public IQuantity getFromValue() {
			return fromValue;
		}

		public IQuantity getToValue() {
			return toValue;
		}

		@Override
		protected void okPressed() {
			try {
				fromValue = kindOfQuantity.parseInteractive(fromText.getText());
				toValue = kindOfQuantity.parseInteractive(toText.getText());
				super.okPressed();
			} catch (QuantityConversionException e) {
				// This should not happen due to earlier validation.
			}
		}
	}

	public static class CustomTimeInputDialog extends TitleAreaDialog {
		private Text timeText;
		private long timeInNanoSeconds;

		public CustomTimeInputDialog(Shell parentShell) {
			super(parentShell);
		}

		private void validateInput() {
			String errorMessage = validateTimespan(timeText.getText());
			getButton(IDialogConstants.OK_ID).setEnabled(errorMessage == null);
			setErrorMessage(errorMessage);
		}

		private String validateTimespan(String text) {
			try {
				IQuantity timespan = TIMESPAN.parseInteractive(text);
				if (timespan.doubleValue() <= 0.0) {
					return Messages.CustomTimeInputDialog_ERROR_INVALID_VALUE;
				}
				timeInNanoSeconds = timespan.longValueIn(NANOSECOND);
			} catch (QuantityConversionException e) {
				return e.getLocalizedMessage();
			}
			return null;
		}

		@Override
		protected Control createContents(Composite parent) {
			getShell().setText(Messages.CustomTimeInputDialog_TITLE);
			Control contents = super.createContents(parent);
			contents.getShell().setSize(400, 200);
			DisplayToolkit.placeDialogInCenter(getParentShell(), getShell());
			setMessage(Messages.CustomTimeInputDialog_MESSAGE);
			setTitle(Messages.CustomTimeInputDialog_TITLE);
			validateInput();
			return contents;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite composite = (Composite) super.createDialogArea(parent);
			createCustomArea(composite);
			applyDialogFont(composite);
			return composite;
		}

		private Control createCustomArea(Composite parent) {
			Composite timeFields = new Composite(parent, SWT.NONE);
			timeFields.setLayout(new GridLayout(2, false));
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.widthHint = 120;
			Label timeLabel = new Label(timeFields, SWT.NONE);
			timeLabel.setText(Messages.CustomTimeInputDialog_TIME_FIELD_LABEL);
			timeLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			timeText = new Text(timeFields, SWT.SINGLE | SWT.BORDER);
			timeText.setLayoutData(gd);
			timeText.setFocus();
			timeText.setText("1 h");
			timeText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					validateInput();
				}
			});
			QuantityKindProposal.install(timeText, TIMESPAN);
			return timeFields;
		}

		public long getTimeInNanoSeconds() {
			return timeInNanoSeconds;
		}

		@Override
		protected void okPressed() {
			String errorMessage = validateTimespan(timeText.getText());
			if (errorMessage == null) {
				super.okPressed();
			}
		}
	}

	public static class CustomTimeRangeDialog extends TitleAreaDialog {
		private DateTimeChooser startTimeChooser;
		private DateTimeChooser endTimeChooser;
		private final ChartComposite chart;
		private long fromTimeNanos;
		private long toTimeNanos;

		public CustomTimeRangeDialog(Shell parentShell, ChartComposite chart) {
			super(parentShell);
			this.chart = chart;
		}

		private void validateInput() {
			String errorMessage = validateTimeRange();
			getButton(IDialogConstants.OK_ID).setEnabled(errorMessage == null);
			setErrorMessage(errorMessage);
		}

		private String validateTimeRange() {
			long startTimeMs = startTimeChooser.getTimestamp();
			long endTimeMs = endTimeChooser.getTimestamp();

			try {
				// Convert milliseconds to nanoseconds
				fromTimeNanos = EPOCH_MS.quantity(startTimeMs).longValueIn(EPOCH_NS);
				toTimeNanos = EPOCH_MS.quantity(endTimeMs).longValueIn(EPOCH_NS);
			} catch (QuantityConversionException e) {
				// This should rarely happen since we're converting between well-defined epoch units
				return e.getLocalizedMessage();
			}

			if (fromTimeNanos >= toTimeNanos) {
				return Messages.CustomTimeRangeDialog_ERROR_FROM_AFTER_TO;
			}

			return null;
		}

		@Override
		protected Control createContents(Composite parent) {
			getShell().setText(Messages.CustomTimeRangeDialog_TITLE);
			Control contents = super.createContents(parent);
			contents.getShell().setSize(450, 300);
			DisplayToolkit.placeDialogInCenter(getParentShell(), getShell());
			setMessage(Messages.CustomTimeRangeDialog_MESSAGE);
			setTitle(Messages.CustomTimeRangeDialog_TITLE);
			validateInput();
			return contents;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite composite = (Composite) super.createDialogArea(parent);
			createCustomArea(composite);
			applyDialogFont(composite);
			return composite;
		}

		private Control createCustomArea(Composite parent) {
			Composite timeFields = new Composite(parent, SWT.NONE);
			timeFields.setLayout(new GridLayout(2, false));

			// Start time
			Label startLabel = new Label(timeFields, SWT.NONE);
			startLabel.setText(Messages.CustomTimeRangeDialog_FROM_FIELD_LABEL);
			startLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

			startTimeChooser = new DateTimeChooser(timeFields, SWT.NONE);
			startTimeChooser.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			// End time
			Label endLabel = new Label(timeFields, SWT.NONE);
			endLabel.setText(Messages.CustomTimeRangeDialog_TO_FIELD_LABEL);
			endLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

			endTimeChooser = new DateTimeChooser(timeFields, SWT.NONE);
			endTimeChooser.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			// Prefill with actual provider data range
			long dataStart = chart.getProviderMinTime();
			long dataEnd = chart.getProviderMaxTime();
			if (dataStart != Long.MAX_VALUE && dataEnd != Long.MIN_VALUE) {
				// Convert nanoseconds to milliseconds for DateTimeChooser
				long startMs = EPOCH_NS.quantity(dataStart).clampedLongValueIn(EPOCH_MS);
				long endMs = EPOCH_NS.quantity(dataEnd).clampedLongValueIn(EPOCH_MS);
				startTimeChooser.setTimestamp(startMs);
				endTimeChooser.setTimestamp(endMs);
			}

			return timeFields;
		}

		public long getFromTimeNanos() {
			return fromTimeNanos;
		}

		public long getToTimeNanos() {
			return toTimeNanos;
		}

		@Override
		protected void okPressed() {
			String errorMessage = validateTimeRange();
			if (errorMessage == null) {
				super.okPressed();
			}
		}
	}

}
