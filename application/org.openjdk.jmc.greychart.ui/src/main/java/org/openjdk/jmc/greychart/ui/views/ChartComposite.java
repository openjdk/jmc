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
package org.openjdk.jmc.greychart.ui.views;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.openjdk.jmc.common.unit.IFormatter;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.greychart.AxisContentType;
import org.openjdk.jmc.greychart.TickDensity;
import org.openjdk.jmc.greychart.TickFormatter;
import org.openjdk.jmc.greychart.YAxis;
import org.openjdk.jmc.greychart.YAxis.Position;
import org.openjdk.jmc.greychart.impl.DefaultXYGreyChart;
import org.openjdk.jmc.greychart.impl.DefaultXYLineRenderer;
import org.openjdk.jmc.greychart.impl.DefaultYAxis;
import org.openjdk.jmc.greychart.impl.EmptyTitleRenderer;
import org.openjdk.jmc.greychart.impl.NanosXAxis;
import org.openjdk.jmc.greychart.impl.OptimizingProvider;
import org.openjdk.jmc.greychart.impl.SamplePoint;
import org.openjdk.jmc.greychart.impl.TimestampFormatter;
import org.openjdk.jmc.greychart.impl.WorldToDeviceConverter;
import org.openjdk.jmc.greychart.ui.messages.internal.Messages;
import org.openjdk.jmc.greychart.ui.views.ChartModel.RangedAxis;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.accessibility.AccessibilityConstants;
import org.openjdk.jmc.ui.accessibility.FocusTracker;
import org.openjdk.jmc.ui.accessibility.MCAccessibleListener;
import org.openjdk.jmc.ui.common.util.Environment;
import org.openjdk.jmc.ui.common.util.Environment.OSType;
import org.openjdk.jmc.ui.common.xydata.DefaultXYData;
import org.openjdk.jmc.ui.common.xydata.ITimestampedData;

/**
 * The GUI for an attribute chart
 */
public class ChartComposite extends SelectionCanvas {

	public static final long ONE_SECOND = 1000 * 1000 * 1000;
	public static final long ONE_MINUTE = 60 * ONE_SECOND;
	public static final long ONE_HOUR = 60 * ONE_MINUTE;
	public static final long ONE_DAY = 24 * ONE_HOUR;
	public static final long ONE_WEEK = 7 * ONE_DAY;
	private static final long MINIMUM_WORLD_WIDTH = ONE_SECOND;
	private static final double WORLD_PADDING = 0.05;
	private static final double ZOOM_FACTOR_MENU_OUT = 2.0;
	private static final double ZOOM_FACTOR_MENU_IN = 1.0 / ZOOM_FACTOR_MENU_OUT;
	private static final double ZOOM_FACTOR_WHEEL_OUT = 1.25;
	private static final double ZOOM_FACTOR_WHEEL_IN = 1.0 / ZOOM_FACTOR_WHEEL_OUT;
	private static final double ZOOM_MIDDLE = 0.5;

	private static final String DRAW_PROPERTY = "org.openjdk.jmc.rjmx.ui.chart.immediatedraw"; //$NON-NLS-1$
	private static final boolean IMMEDIATE_DRAWING;

	static {
		// Workaround for slow SWT redraw on Cocoa.
		boolean drawImmediately;
		if (System.getProperty(DRAW_PROPERTY) != null) {
			drawImmediately = Boolean.getBoolean(DRAW_PROPERTY);
		} else {
			// Enable on OS X due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=410293
			drawImmediately = (Environment.getOSType() == OSType.MAC);
		}
		IMMEDIATE_DRAWING = drawImmediately;
	}

	private static class QuantityFormatter implements TickFormatter {
		private final IUnit outUnit;
		private final IFormatter<IQuantity> formatter;

		private QuantityFormatter(KindOfQuantity<?> kindOfQuantity) {
			outUnit = kindOfQuantity.getDefaultUnit();
			formatter = kindOfQuantity.getDefaultFormatter();
		}

		@Override
		public String getUnitString(Number min, Number max) {
			return ChartModel.NO_VALUE;
		}

		@Override
		public String format(Number value, Number min, Number max, Number labelDistance) {
			return formatter.format(outUnit.quantity(value));
		}
	}

	private IAction zoomInAction;
	private IAction zoomOutAction;
	private DefaultYAxis yAxis;

	final private ChartModel m_chartModel = new ChartModel();
	private final DefaultXYGreyChart<ITimestampedData> m_chart;
	private long m_viewWidth;
	private long m_viewEnd = System.currentTimeMillis() * 1000 * 1000;
	private long m_dataStart = Long.MAX_VALUE;
	private long m_dataEnd = Long.MIN_VALUE;
	private boolean m_enableUpdates;
	private final Consumer<Boolean> m_enableUpdatesCallback;
	private ChartSampleTooltipProvider m_cstp = null;
	private Rectangle m_plotBounds;

	/**
	 * @param parent
	 *            Parent widget
	 * @param style
	 *            Widget style
	 * @param enableUpdatesCallback
	 *            A callback that is called when the chart changes in a manner that affects whether
	 *            it is updated on new values or not. Typically this is used to enable or disable UI
	 *            elements that control automatic updates. The boolean parameter for the callback
	 *            tells whether automatic updates have been enabled (true) or not (false). If null,
	 *            then no callbacks are made.
	 */
	public ChartComposite(Composite parent, int style, Consumer<Boolean> enableUpdatesCallback) {
		super(parent);
		m_chart = createChart();
		m_enableUpdatesCallback = enableUpdatesCallback;
		setupAntiAliasingListener();
		setupObservers();

		MenuManager popupMenu = createContextMenu();
		setMenu(popupMenu.createContextMenu(this));

		setupAccessibility();
		setupMouseTracker();
		updateXAxis();
		updateYAxis();
		setupMouseWheelListener();
		yAxis.setTickDensity(TickDensity.NORMAL);
	}

	public void refresh() {
		if (!isDisposed()) {
			zoomInAction.setEnabled(m_viewWidth > MINIMUM_WORLD_WIDTH && !m_enableUpdates);
			((NanosXAxis) getChart().getXAxis()).setRange(m_viewEnd - m_viewWidth, m_viewEnd);
			m_chart.setXAxis(m_chart.getXAxis());
			if (IMMEDIATE_DRAWING && isVisible() && !getClientArea().isEmpty()) {
				GC gc = new GC(this);
				paint(gc);
				if (isFocusControl()) {
					FocusTracker.drawFocusOn(this, gc);
				}
				gc.dispose();
			} else {
				redraw();
				/*
				 * Explicit calls to update() should be avoided unless absolutely necessary. They
				 * may have a negative performance impact and may cause issues on Mac OS X Cocoa
				 * (SWT 3.6). If it is required here, there must be a justifying comment.
				 */
				// update();
			}
		}
	}

	private NanosXAxis getXAxis() {
		return (NanosXAxis) getChart().getXAxis();
	}

	private void setupMouseWheelListener() {
		addListener(SWT.MouseVerticalWheel, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (!m_enableUpdates) {
					Rectangle bounds = getXAxis().getRenderedBounds();
					int x = Math.max(translateDisplayToImageXCoordinate(event.x) - bounds.x, 0);
					x = Math.min(x, bounds.width);
					double location = (double) x / bounds.width;
					if (event.count < 0) {
						zoom(ZOOM_FACTOR_WHEEL_OUT, location);
					} else if (event.count > 0 && zoomInAction.isEnabled()) {
						zoom(ZOOM_FACTOR_WHEEL_IN, location);
					}
				}
			}
		});
	}

	private void setupAntiAliasingListener() {
		final AntiAliasingListener antialiasingListener = new AntiAliasingListener(m_chart);
		addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				UIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(antialiasingListener);
			}
		});
		UIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(antialiasingListener);
	}

	private void setupAccessibility() {
		FocusTracker.enableFocusTracking(this);
		final MCAccessibleListener accessabilityListener = new MCAccessibleListener();
		accessabilityListener.setComponentType(AccessibilityConstants.COMPONENT_TYPE_GRAPH);
		getAccessible().addAccessibleListener(accessabilityListener);
		m_chartModel.addObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				accessabilityListener.setName(m_chartModel.getChartTitle());
			}
		});
	}

	private void setupMouseTracker() {
		addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseHover(MouseEvent e) {
				Rectangle plotArea = m_plotBounds;
				if (!m_enableUpdates && plotArea != null) {
					org.eclipse.swt.graphics.Rectangle clientArea = getClientArea();

					int topMargin = plotArea.x + clientArea.x;
					int leftMargin = plotArea.y + clientArea.y;
					Point translated = translateDisplayToImageCoordinates(e.x, e.y);
					Point mousePoint = new Point(translated.x - topMargin, translated.y - leftMargin); // Drawing coordinate point
					SamplePoint closestSample = null;
					OptimizingProvider closestProvider = null;
					long distance = Long.MAX_VALUE;

					if (getChart().getOptimizingProvider() != null) {
						for (OptimizingProvider provider : getChart().getOptimizingProvider().getChildren()) {
							WorldToDeviceConverter yConverter = provider.getYSampleToDeviceConverterFor(yAxis);
							Iterator<SamplePoint> samples = provider.getSamples(plotArea.width);
							SamplePoint data = getClosestPoint(yConverter, mousePoint, samples);
							if (data == null) {
								continue;
							}
							long newDistance = getSquaredDistance(yConverter, data, mousePoint);
							if (closestSample == null || newDistance < distance) {
								closestSample = data;
								closestProvider = provider;
								distance = newDistance;
								if (distance == 0) {
									break;
								}
							}
						}
					}

					if (closestSample != null && closestProvider != null && m_cstp != null) {
						WorldToDeviceConverter yConverter = closestProvider.getYSampleToDeviceConverterFor(yAxis);
						DefaultXYData<Number, Number> xyData = new DefaultXYData<>(closestSample.x,
								yConverter.getDeviceCoordinate(closestSample.y));

						getChart().getPlotRenderer().circleValue(xyData);
						setToolTipText(m_cstp.getTooltip(closestProvider.getDataSeries(), closestSample.y));
						redraw();
					}
				} else {
					clearCircledValue();
				}
			}

			@Override
			public void mouseExit(MouseEvent e) {
				clearCircledValue();
			}
		});
	}

	public void setChartSampleTooltipProvider(ChartSampleTooltipProvider cstp) {
		m_cstp = cstp;
	}

	private DefaultXYGreyChart<ITimestampedData> createChart() {
		DefaultXYGreyChart<ITimestampedData> chart = new DefaultXYGreyChart<>();

		chart.setTitleRenderer(new EmptyTitleRenderer(chart, 10));
		chart.setIndexRenderer(null);
		chart.setAntialiasingEnabled(AntiAliasingListener.isUsingAntialiasing());
		// Seems to be handling its own updates.
		chart.setAutoUpdateOnAxisChange(false);
		DefaultXYLineRenderer plotRenderer = chart.getPlotRenderer();
		plotRenderer.setUseClip(false);
		plotRenderer.setDrawOnXAxis(true);
		plotRenderer.setExtrapolateMissingData(false);

		NanosXAxis d = new NanosXAxis(chart);
		// DateXAxis d = new DateXAxis(m_chart);
		d.setFormatter(TimestampFormatter.createNanoTimestampFormatter());
		chart.setXAxis(d);
		chart.getXAxis().setTitle(Messages.ChartComposite_X_AXIS_TITLE);

		yAxis = new DefaultYAxis(chart);
		yAxis.setPosition(Position.LEFT);
		chart.addYAxis(yAxis);
		return chart;
	}

	private void updateXAxis() {
		NanosXAxis xAxis = (NanosXAxis) getChart().getXAxis();
		xAxis.setTitle(getChartModel().getXAxis().getTitle());
		redraw();
	}

	private void updateYAxis() {
		RangedAxis axisModel = getChartModel().getYAxis();
		DefaultYAxis yAxisLeft = getFirstLeftAxis();
		yAxisLeft.setTitle(axisModel.getTitle());
		final KindOfQuantity<?> kindOfQuantity = axisModel.getKindOfQuantity();

		if (kindOfQuantity == UnitLookup.MEMORY) {
			yAxisLeft.setContentType(AxisContentType.BYTES);
		} else {
			yAxisLeft.setContentType(AxisContentType.UNKNOWN);
		}

		yAxisLeft.setFormatter(new QuantityFormatter(kindOfQuantity));

		switch (axisModel.getRangeType()) {
		case AUTO:
			yAxisLeft.setAutoRangeEnabled(true);
			yAxisLeft.setAlwaysShowZero(false);
			yAxisLeft.setAutoPadding(0.2);
			break;
		case AUTO_ZERO:
			yAxisLeft.setAutoRangeEnabled(true);
			yAxisLeft.setAlwaysShowZero(true);
			yAxisLeft.setAutoPadding(0.2);
			break;
		case CUSTOM:
			yAxisLeft.setAutoRangeEnabled(false);
			yAxisLeft.setAlwaysShowZero(false);
			IUnit outUnit = axisModel.getKindOfQuantity().getDefaultUnit();
			yAxisLeft.setRange(axisModel.getMinValue().doubleValueIn(outUnit),
					axisModel.getMaxValue().doubleValueIn(outUnit));
			yAxisLeft.setAutoPadding(0.0);
			break;
		}
		redraw();
	}

	private DefaultYAxis getFirstLeftAxis() {
		YAxis[] yAxis = getChart().getYAxis();
		for (YAxis y : yAxis) {
			if (((DefaultYAxis) y).getPosition() == Position.LEFT) {
				return (DefaultYAxis) y;
			}
		}
		return (DefaultYAxis) yAxis[0];
	}

	private void setupObservers() {
		getChartModel().getXAxis().addObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				updateXAxis();
			}
		});

		getChartModel().getYAxis().addObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				updateYAxis();
			}
		});
	}

	public ChartModel getChartModel() {
		return m_chartModel;
	}

	private void clearCircledValue() {
		getChart().getPlotRenderer().circleValue(null);
		setToolTipText(null);
		redraw();
	}

	public DefaultXYGreyChart<ITimestampedData> getChart() {
		return m_chart;
	}

	private static SamplePoint getClosestPoint(
		WorldToDeviceConverter yConverter, Point mousePoint, Iterator<SamplePoint> s) {
		SamplePoint result = null;
		// Yes, this is a slow operation. Trick to speed up would be to use an intermediary model like the ones in
		// previous versions of Mission Control, and together with the min,max,in,out points save the XY data that
		// contributed those values. Then this would be done linearly with resolution.
		long distance = Long.MAX_VALUE;
		for (Iterator<SamplePoint> iter = s; iter.hasNext();) {
			SamplePoint data = iter.next();
			long tmpDist = getSquaredDistance(yConverter, data, mousePoint);
			if (tmpDist < distance) {
				distance = tmpDist;
				result = data;
			}
		}
		return result;
	}

	private static long getSquaredDistance(WorldToDeviceConverter yConverter, SamplePoint data, Point mousePoint) {
		long xdiff = data.x - mousePoint.x;
		long ydiff = yConverter.getDeviceCoordinate(data.y) - mousePoint.y;
		return xdiff * xdiff + ydiff * ydiff;
	}

	private MenuManager createContextMenu() {
		MenuManager popupMenu = new MenuManager();
		popupMenu.add(ChartMenuBuilder.createShowMenu(this));
		popupMenu.add(ChartMenuBuilder.createRangeMenu(m_chartModel));
		popupMenu.add(ChartMenuBuilder.createLabelDensityMenu(yAxis));
		popupMenu.add(ChartMenuBuilder.createRenderingModeMenu(m_chartModel));
		popupMenu.add(ChartMenuBuilder.createTitleMenu(m_chartModel));

		zoomInAction = new Action(Messages.ChartComposite_MENU_ZOOM_IN_TEXT) {
			@Override
			public void run() {
				zoom(ZOOM_FACTOR_MENU_IN, ZOOM_MIDDLE);
			}
		};
		zoomOutAction = new Action(Messages.ChartComposite_MENU_ZOOM_OUT_TEXT) {
			@Override
			public void run() {
				zoom(ZOOM_FACTOR_MENU_OUT, ZOOM_MIDDLE);
			}
		};
		popupMenu.add(zoomInAction);
		popupMenu.add(zoomOutAction);
		popupMenu.add(new ExportChartAsImage(m_chart));
		return popupMenu;
	}

	/**
	 * Show the last values in the data series. Depends on that the data range is set correctly.
	 *
	 * @param viewWidth
	 *            X axis view width
	 */
	public void showLast(long viewWidth) {
		m_viewWidth = viewWidth;
		if (m_enableUpdatesCallback != null) {
			m_enableUpdatesCallback.accept(true);
		}
		if (m_dataEnd > 0) {
			m_viewEnd = m_dataEnd;
		}
		refresh();
	}

	/**
	 * Show all values in the data series. Depends on that the data range is set correctly.
	 */
	public void showAll() {
		m_viewWidth = m_dataEnd - m_dataStart;
		m_viewEnd = m_dataEnd;
		refresh();
	}

	public void extendsDataRangeToInclude(long timestamp) {
		setDataRange(Math.min(m_dataStart, timestamp), Math.max(m_dataEnd, timestamp));
	}

	/**
	 * Tell the chart about the range of available values in the data series.
	 *
	 * @param dataStart
	 *            Lowest X value available in the data series
	 * @param dataEnd
	 *            Highest X value available in the data series
	 */
	public void setDataRange(long dataStart, long dataEnd) {
		if (dataEnd > 0 && (m_enableUpdates || m_dataEnd < 0)) {
			m_viewEnd = dataEnd;
		}
		m_dataStart = dataStart;
		m_dataEnd = dataEnd;
		refresh();
	}

	public void setUpdatesEnabled(boolean enabled) {
		m_enableUpdates = enabled;
		zoomInAction.setEnabled(!enabled);
		zoomOutAction.setEnabled(!enabled);
		if (m_enableUpdates && m_dataEnd > 0) {
			m_viewEnd = m_dataEnd;
			refresh();
		}
	}

	private void zoom(double factor, double location) {
		if (m_dataEnd > m_dataStart) {
			double padding = m_viewWidth * WORLD_PADDING;
			long worldStart = m_viewEnd - m_viewWidth;
			double zoomPoint = worldStart + m_viewWidth * location;

			double newWorldEnd = zoomPoint + ((m_viewEnd - zoomPoint) * factor);
			double newWorldStart = zoomPoint - ((zoomPoint - worldStart) * factor);

			m_viewEnd = (long) Math.min(Math.max(m_viewEnd, m_dataEnd + padding), newWorldEnd);
			newWorldStart = Math.max(Math.min(worldStart, m_dataStart - padding), newWorldStart);
			m_viewWidth = (long) (m_viewEnd - newWorldStart);

			refresh();
		}
	}

	@Override
	protected Rectangle render(Graphics2D ctx, Rectangle where) {
		m_chart.render(ctx, where);
		m_plotBounds = m_chart.getPlotRenderer().getRenderedBounds();
		return m_plotBounds;
	}

	@Override
	protected void selectionStart() {
		if (m_enableUpdatesCallback != null) {
			m_enableUpdatesCallback.accept(false);
		}
	}

	@Override
	protected void selectionComplete(double start, double end) {
		m_viewEnd = m_viewEnd - (long) ((1 - end) * m_viewWidth);
		m_viewWidth = (long) (m_viewWidth * (end - start));
		refresh();
	}

}
