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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.accessibility.MCAccessibleListener;
import org.openjdk.jmc.ui.accessibility.SimpleTraverseListener;
import org.openjdk.jmc.ui.charts.AWTChartToolkit;
import org.openjdk.jmc.ui.charts.SubdividedQuantityRange;
import org.openjdk.jmc.ui.charts.XYQuantities;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.preferences.PreferenceConstants;

public class Navigator extends Composite {

	private static final IAttribute<IQuantity> X_START_ATTRIBUTE = JfrAttributes.START_TIME;
	private static final IAttribute<IQuantity> X_END_ATTRIBUTE = JfrAttributes.END_TIME;
	private static final Color SELECTED_TOPCOLOR = new Color(128, 128, 255);
	private static final Color SELECTED_BOTTOMCOLOR = new Color(0, 250, 250);

	private static final Color ALL_TOPCOLOR = Color.RED;
	private static final Color ALL_BOTTOMCOLOR = new Color(252, 128, 3);

	private static final IQuantity MINIMUM_DURATION = UnitLookup.NANOSECOND.quantity(1000);
	// FIXME: Used in code that should be re-implemented (see commented out code in 'create' methods below)
//	private static final double ZOOM_FACTOR = 2;
//	private static final double TRANSLATION_FACTOR = 0.3;
	private static final int RESOLUTION = 800;

	private final JComponentNavigator navigator;
	// FIXME: more explicit threading support
	private volatile IRange<IQuantity> fullRange;
	private double[] allItems;
	private double[] selectedItems;
	private final Button translateBackButton;
	private final Button translateForwardButton;
	private final Button zoomOutButton;
	private final Button zoomInButton;
	private final Button selectAllButton;
	private final Label leftLabel;
	private final Label centerLabel;
	private final Label rightLabel;
	private Runnable navigatorObserver;

	public Navigator(Composite parent, FormToolkit toolkit) {
		super(parent, SWT.NONE);

		setLayout(MCLayoutFactory.createPackedLayout(1, true));

		Composite topRow = toolkit.createComposite(this);
		topRow.setLayout(new GridLayout(1, true));
		topRow.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		centerLabel = createTimeLabel(topRow, toolkit, SWT.CENTER);

		navigator = new NavigatorCanvas(this, toolkit);
		navigator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite bottomRow = toolkit.createComposite(this);

		GridLayout gridLayout = new GridLayout(7, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		bottomRow.setLayout(gridLayout);
		bottomRow.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));

		leftLabel = createTimeLabel(bottomRow, toolkit, SWT.LEFT);
		GridData leftLabelLayoutData = new GridData(SWT.LEFT, SWT.FILL, true, true);
		// Setting widthHint causes the label to always have the same width as rightLabelContainer,
		// so the range navigator buttons (which are centered between these) will always
		// stay at the same place, regardless of the number of characters in these labels.
		leftLabelLayoutData.widthHint = 1000;
		leftLabel.setLayoutData(leftLabelLayoutData);
		leftLabel.addTraverseListener(new SimpleTraverseListener());
		setData(leftLabel, "start.time"); //$NON-NLS-1$

		translateBackButton = createTranslateBackward(bottomRow);
		translateBackButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		zoomOutButton = createZoomOut(bottomRow);
		zoomOutButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		selectAllButton = createSelectAll(bottomRow);
		selectAllButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		zoomInButton = createZoomIn(bottomRow);
		zoomInButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		translateForwardButton = createTranslateForward(bottomRow);
		translateForwardButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		// Ubuntu interprets SWT.RIGHT in the label creation differently from other OSes.
		// This is a workaround. We build a container where we first put an empty label, then the
		// real label. If you change this, please test that it still works on all platforms.
		Composite rightLabelContainer = toolkit.createComposite(bottomRow, SWT.NONE);
		GridLayout rightLabelContainerLayout = new GridLayout(2, false);
		rightLabelContainerLayout.marginWidth = 0;
		rightLabelContainerLayout.marginHeight = 0;
		rightLabelContainer.setLayout(rightLabelContainerLayout);
		GridData rightLabelLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		// Setting widthHint causes the container to always have the same width as leftLabel,
		// so the range navigator buttons (which are centered between these) will always
		// stay at the same place, regardless of the number of characters in these labels.
		rightLabelLayoutData.widthHint = 1000;
		rightLabelContainer.setLayoutData(rightLabelLayoutData);

		Label rightDummyLabel = toolkit.createLabel(rightLabelContainer, null);
		rightDummyLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		rightLabel = createTimeLabel(rightLabelContainer, toolkit, SWT.RIGHT);
		rightLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, true));
		rightLabel.addTraverseListener(new SimpleTraverseListener());
		setData(rightLabel, "end.time"); //$NON-NLS-1$
		setButtonDecorations();
	}

	private void setData(Widget widget, String id) {
		widget.setData("name", "navigator." + id); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void enableButton(Button button, boolean enabled) {
		if (button != null && !button.isDisposed()) {
			button.setEnabled(enabled);
		}
	}

	private Button createTranslateForward(Composite container) {
		throw new UnsupportedOperationException("Reimplement"); //$NON-NLS-1$
//		Button button = new Button(container, SWT.NONE);
//		button.setToolTipText(Messages.NAVIGATOR_MOVE_FORWARD_TEXT);
//		button.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				navigator.translate(TRANSLATION_FACTOR);
//			}
//
//		});
//		setData(button, "move.forward"); //$NON-NLS-1$
//		hookTimeAccessibility(button);
//		return button;
	}

	private Button createTranslateBackward(Composite container) {
		throw new UnsupportedOperationException("Reimplement"); //$NON-NLS-1$
//		Button button = new Button(container, SWT.NONE);
//		button.setToolTipText(Messages.NAVIGATOR_MOVE_BACKWARD_TEXT);
//		button.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				navigator.translate(-TRANSLATION_FACTOR);
//			}
//		});
//		setData(button, "move.backward"); //$NON-NLS-1$
//		hookTimeAccessibility(button);
//		return button;
	}

	private Button createZoomIn(Composite container) {
		throw new UnsupportedOperationException("Reimplement"); //$NON-NLS-1$
//		Button button = new Button(container, SWT.NONE);
//		button.setToolTipText(Messages.NAVIGATOR_ZOOM_IN_TEXT);
//		setData(button, "zoom.in"); //$NON-NLS-1$
//		button.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				navigator.zoom(ZOOM_FACTOR, MINIMUM_DURATION);
//			}
//		});
//		hookTimeAccessibility(button);
//		return button;
	}

	private Button createZoomOut(Composite container) {
		throw new UnsupportedOperationException("Reimplement"); //$NON-NLS-1$
//		Button button = new Button(container, SWT.NONE);
//		button.setToolTipText(Messages.NAVIGATOR_ZOOM_OUT_TEXT);
//		setData(button, "zoom.out"); //$NON-NLS-1$
//		button.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				navigator.zoom(1 / ZOOM_FACTOR, MINIMUM_DURATION);
//			}
//		});
//
//		hookTimeAccessibility(button);
//		return button;
	}

	private Button createSelectAll(Composite container) {
		Button button = new Button(container, SWT.NONE);
		button.setToolTipText(Messages.NAVIGATOR_SELECT_ALL_TEXT);
		setData(button, "select.all"); //$NON-NLS-1$
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				navigator.setCurrentRange(fullRange);
			}
		});

		hookTimeAccessibility(button);
		return button;
	}

	private void setButtonDecorations() {
		boolean buttonAsText = UIPlugin.getDefault().getPreferenceStore()
				.getBoolean(PreferenceConstants.P_ACCESSIBILITY_BUTTONS_AS_TEXT);
		if (buttonAsText) {
			setButtonTexts();
		} else {
			setButtonPictures();
		}
	}

	private void setButtonTexts() {
		translateBackButton.setImage(null);
		translateForwardButton.setImage(null);
		zoomOutButton.setImage(null);
		zoomInButton.setImage(null);
		selectAllButton.setImage(null);
		translateBackButton.setText(Messages.NAVIGATOR_MOVE_BACKWARD_TEXT);
		translateForwardButton.setText(Messages.NAVIGATOR_MOVE_FORWARD_TEXT);
		zoomOutButton.setText(Messages.NAVIGATOR_ZOOM_OUT_TEXT);
		zoomInButton.setText(Messages.NAVIGATOR_ZOOM_IN_TEXT);
		selectAllButton.setText(Messages.NAVIGATOR_SELECT_ALL_TEXT);
	}

	private void setButtonPictures() {
		translateBackButton.setText(""); //$NON-NLS-1$
		translateForwardButton.setText(""); //$NON-NLS-1$
		zoomOutButton.setText(""); //$NON-NLS-1$
		zoomInButton.setText(""); //$NON-NLS-1$
		selectAllButton.setText(""); //$NON-NLS-1$
		translateBackButton.setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_NAV_BACKWARD));
		translateForwardButton.setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_NAV_FORWARD));
		zoomOutButton.setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_ZOOM_OUT));
		zoomInButton.setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_ZOOM_IN));
		selectAllButton.setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_SELECT_ALL));
	}

	private void hookTimeAccessibility(Button button) {
		MCAccessibleListener listener = new MCAccessibleListener();
		listener.setName(button.getToolTipText());
		button.getAccessible().addAccessibleListener(listener);
	}

	public void setNavigatorRange(IRange<IQuantity> range) {
		fullRange = range;
		navigator.setNavigatorRange(range);
	}

	public IRange<IQuantity> getCurrentRange() {
		return navigator.getCurrentRange();
	}

	public void setCurrentRange(IRange<IQuantity> range) {
		navigator.setCurrentRange(range);
	}

	public void setNavigatorObserver(Runnable navigatorObserver) {
		this.navigatorObserver = navigatorObserver;
	}

	private static Label createTimeLabel(Composite parent, FormToolkit toolkit, int alignMent) {
		Label l = toolkit.createLabel(parent, "-- : -- : --", SWT.WRAP | SWT.READ_ONLY | alignMent); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		return l;
	}

	private class NavigatorCanvas extends JComponentNavigator {
		public NavigatorCanvas(Composite parent, FormToolkit toolkit) {
			super(parent, toolkit);
		}

		@Override
		protected void onRangeChange() {
			IRange<IQuantity> currentRange = navigator.getCurrentRange();
			boolean hasMinBound = navigator.isLimitingMin();
			boolean hasMaxBound = navigator.isLimitingMax();
			IQuantity duration = currentRange.getExtent();

			enableButton(translateBackButton, hasMinBound);
			enableButton(translateForwardButton, hasMaxBound);
			enableButton(zoomOutButton, hasMinBound || hasMaxBound);
			enableButton(zoomInButton, duration.compareTo(MINIMUM_DURATION) > 0);
			enableButton(selectAllButton, hasMinBound || hasMaxBound);

			leftLabel.setText(currentRange.getStart().displayUsing(IDisplayable.AUTO));
			centerLabel.setText(duration.displayUsing(IDisplayable.AUTO));
			rightLabel.setText(currentRange.getEnd().displayUsing(IDisplayable.AUTO));
			if (navigatorObserver != null) {
				navigatorObserver.run();
			}
		}

		@Override
		protected void renderBackdrop(Graphics2D g2, int width, int height) {
			g2.fillRect(0, 0, width, height);
			if (allItems != null && fullRange != null) {
				XYQuantities<?> allQuantities = XYQuantities.create(null, allItems, UnitLookup.NUMBER_UNITY,
						getFullRangeAxis(width, RESOLUTION));
				IQuantity maxY = allQuantities.getMaxY();
				if (maxY != null && maxY.doubleValue() > 0) {
					// Make anything with a value (not NaN) show at least one pixel by shifting origo one pixel.
					IQuantity minY = maxY.getUnit().quantity(-maxY.doubleValue() / (height - 1));
					SubdividedQuantityRange yRange = new SubdividedQuantityRange(minY, maxY, height, 20);
					allQuantities.setYRange(yRange);

					g2.setPaint(new GradientPaint(0, 0, ALL_BOTTOMCOLOR, 0, height, ALL_TOPCOLOR));
					AWTChartToolkit.drawRightAngleChart(g2, allQuantities, width, height);

					if (selectedItems != null) {
						g2.setPaint(new GradientPaint(0, 0, SELECTED_BOTTOMCOLOR, 0, height, SELECTED_TOPCOLOR));
						XYQuantities<?> selectedQuantities = XYQuantities.create(null, selectedItems,
								UnitLookup.NUMBER_UNITY, getFullRangeAxis(width, RESOLUTION));
						selectedQuantities.setYRange(yRange);
						AWTChartToolkit.drawRightAngleChart(g2, selectedQuantities, width, height);
					}
				}
			}
		}
	}

	public void setAllItems(IItemCollection allItems) {
		this.allItems = allItems == null ? null : countItemsInMaxResolution(allItems);
		selectedItems = null;
		navigator.invalidateNavigator(true);
		navigator.redraw();
	}

	public void setSelectedItems(IItemCollection selectedItems) {
		this.selectedItems = countItemsInMaxResolution(selectedItems);
		navigator.invalidateNavigator(true);
		navigator.redraw();
	}

	private SubdividedQuantityRange getFullRangeAxis(int pixels, int subdividers) {
		// FIXME: Use with pixel subdivisions instead.
		return new SubdividedQuantityRange(subdividers, fullRange.getStart(), fullRange.getEnd(), pixels);
	}

	private double[] countItemsInMaxResolution(IItemCollection items) {
		// FIXME: Extract the needed functionality from SubdividedQuantityRange
		SubdividedQuantityRange fullRangeAxis = getFullRangeAxis(RESOLUTION, 1);
		double[] values = new double[RESOLUTION];
		Iterator<? extends IItemIterable> iterator = items.iterator();
		while (iterator.hasNext()) {
			IItemIterable next = iterator.next();
			IType<IItem> type = next.getType();
			IMemberAccessor<IQuantity, IItem> endAccessor = X_END_ATTRIBUTE.getAccessor(type);
			IMemberAccessor<IQuantity, IItem> startAccessor = X_START_ATTRIBUTE.getAccessor(type);
			Iterator<? extends IItem> ii = next.iterator();
			while (ii.hasNext()) {
				IItem item = ii.next();
				int xPos = (int) fullRangeAxis.getPixel(endAccessor.getMember(item));
				if (xPos >= 0) {
					if (startAccessor != endAccessor) { // Only optimization check
						int xPosStart = (int) fullRangeAxis.getPixel(startAccessor.getMember(item));
						if (xPosStart < values.length) {
							for (int i = xPosStart; i <= Math.min(xPos, values.length - 1); i++) {
								values[i]++;
							}
						}
					} else if (xPos < values.length) {
						values[xPos]++;
					}
				}
			}
		}
		// For use with adjusted y axis.
		for (int i = 0; i < values.length; i++) {
			if (values[i] == 0) {
				values[i] = Double.NaN;
			}
		}
		return values;
	}
}
