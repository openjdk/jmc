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

import java.util.Observable;
import java.util.logging.Level;

import org.eclipse.ui.IMemento;

import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.ui.UIPlugin;

import org.openjdk.jmc.greychart.data.RenderingMode;

/**
 * Class that holds information about a chart. It for backward compatibility it uses
 * "chartComposite" XML-tag, even though all the gui has been separated
 */
public class ChartModel extends Observable {
	public static final String XML_TAG_CHART_MODEL = "chartModel"; //$NON-NLS-1$
	public static final String XML_Y_RANGE_SETTING = "yRangeSetting"; //$NON-NLS-1$
	public static final String XML_RENDERING_MODE_SETTING = "renderingModeSetting"; //$NON-NLS-1$
	public static final String XML_KIND_OF_QUANTITY = "kindOfQuantity"; //$NON-NLS-1$
	public static final String XML_FROM_RANGE_TYPE = "rangeFromType"; //$NON-NLS-1$
	public static final String XML_FROM_RANGE_VALUE = "rangeFromValue"; //$NON-NLS-1$
	public static final String XML_TO_RANGE_TYPE = "rangeToType"; //$NON-NLS-1$
	public static final String XML_TO_RANGE_VALUE = "rangeToValue"; //$NON-NLS-1$
	public static final String XML_Y_AXIS_TITLE = "yAxisTitle"; //$NON-NLS-1$
	public static final String XML_X_AXIS_TITLE = "xAxisTitle"; //$NON-NLS-1$
	public static final String XML_CHART_TITLE = "chartTitle"; //$NON-NLS-1$

	public enum AxisRange {
		AUTO, CUSTOM, AUTO_ZERO
	}

	public static final String NO_VALUE = ""; //$NON-NLS-1$

	/**
	 * Class that holds information about an axis.
	 */
	public static class Axis extends Observable {
		String m_title;

		public String getTitle() {
			return m_title == null ? NO_VALUE : m_title;

		}

		public void setTitle(String title) {
			m_title = title;
			setChanged();
		}
	}

	/**
	 */
	public static class RangedAxis extends Axis {
		private AxisRange m_rangeType = AxisRange.AUTO_ZERO;
		private KindOfQuantity<?> m_kindOfQuantity = UnitLookup.NUMBER;
		private IQuantity m_minValue = m_kindOfQuantity.getDefaultUnit().quantity(0);
		private IQuantity m_maxValue = m_kindOfQuantity.getDefaultUnit().quantity(100);

		public IQuantity getMinValue() {
			return m_minValue;
		}

		public IQuantity getMaxValue() {
			return m_maxValue;
		}

		public void setMinValue(IQuantity minValue) {
			if (minValue.getUnit().getContentType() == m_kindOfQuantity && !minValue.equals(m_minValue)) {
				m_minValue = minValue;
				setChanged();
			}
		}

		public void setMaxValue(IQuantity maxValue) {
			if (maxValue.getUnit().getContentType() == m_kindOfQuantity && !maxValue.equals(m_maxValue)) {
				m_maxValue = maxValue;
				setChanged();
			}
		}

		public void setRangeType(AxisRange rangeType) {
			if (rangeType != m_rangeType) {
				m_rangeType = rangeType;
				setChanged();
			}
		}

		public AxisRange getRangeType() {
			if (m_rangeType == AxisRange.CUSTOM) {
				if (!m_kindOfQuantity.equals(m_minValue.getUnit().getContentType())
						|| !m_kindOfQuantity.equals(m_maxValue.getUnit().getContentType())) {
					return (m_minValue.doubleValue() <= 0 && m_maxValue.doubleValue() >= 0) ? AxisRange.AUTO_ZERO
							: AxisRange.AUTO;
				}
			}
			return m_rangeType;
		}

		public void setKindOfQuantity(KindOfQuantity<?> type) {
			if (!m_kindOfQuantity.equals(type)) {
				m_kindOfQuantity = type;
				setChanged();
			}
		}

		public KindOfQuantity<?> getKindOfQuantity() {
			return m_kindOfQuantity;
		}
	}

	final private Axis m_xAxis = new Axis();
	final private RangedAxis m_yAxis = new RangedAxis();

	private String m_title;
	private RenderingMode m_renderingMode = RenderingMode.SUBSAMPLING;

	public Axis getXAxis() {
		return m_xAxis;
	}

	public RangedAxis getYAxis() {
		return m_yAxis;
	}

	public String getComponentTag() {
		return XML_TAG_CHART_MODEL;
	}

	public void setRenderingMode(RenderingMode mode) {
		m_renderingMode = mode;
		setChanged();
	}

	public RenderingMode getRenderingMode() {
		return m_renderingMode;
	}

	/**
	 * @param title
	 */
	public void setChartTitle(String title) {
		m_title = title;
		setChanged();
	}

	/**
	 * @return
	 */
	public String getChartTitle() {
		return m_title == null ? NO_VALUE : m_title;
	}

	public void saveState(IMemento memento) {
		memento.putString(XML_Y_RANGE_SETTING, getYAxis().getRangeType().toString());
		memento.putString(XML_FROM_RANGE_TYPE, getYAxis().getMinValue().getUnit().getContentType().getIdentifier());
		memento.putString(XML_FROM_RANGE_VALUE, getYAxis().getMinValue().persistableString());
		memento.putString(XML_TO_RANGE_TYPE, getYAxis().getMaxValue().getUnit().getContentType().getIdentifier());
		memento.putString(XML_TO_RANGE_VALUE, getYAxis().getMaxValue().persistableString());
		memento.putString(XML_Y_AXIS_TITLE, m_yAxis.m_title);
		memento.putString(XML_X_AXIS_TITLE, m_xAxis.m_title);
		memento.putString(XML_CHART_TITLE, m_title);
		memento.putString(XML_KIND_OF_QUANTITY, getYAxis().getKindOfQuantity().getIdentifier());
		memento.putString(XML_RENDERING_MODE_SETTING, getRenderingMode().toString());
	}

	public void restoreState(IMemento memento) {
		KindOfQuantity<?> kindOfQuantity = UnitLookup.NUMBER;
		try {
			String quantityId = memento.getString(XML_KIND_OF_QUANTITY);
			// On upgrading from earlier workspaces, quantityId will be null.
			if (quantityId != null) {
				ContentType<?> contentType = UnitLookup.getContentType(quantityId);
				kindOfQuantity = (contentType instanceof KindOfQuantity<?>) ? (KindOfQuantity<?>) contentType
						: UnitLookup.NUMBER;
			}
		} catch (IllegalArgumentException ex) {
			UIPlugin.getDefault().getLogger().log(Level.WARNING, "Problem reading range values.", ex); //$NON-NLS-1$
		}
		getYAxis().setKindOfQuantity(kindOfQuantity);
		AxisRange yRangeValue = AxisRange.AUTO_ZERO;
		IQuantity minValue = UnitLookup.NUMBER.getDefaultUnit().quantity(0);
		IQuantity maxValue = UnitLookup.NUMBER.getDefaultUnit().quantity(100);
		try {
			String rangeSetting = memento.getString(XML_Y_RANGE_SETTING);
			if (rangeSetting != null) {
				yRangeValue = AxisRange.valueOf(rangeSetting);
			}
			minValue = restoreTypeValue(memento, XML_FROM_RANGE_TYPE, XML_FROM_RANGE_VALUE, minValue);
			maxValue = restoreTypeValue(memento, XML_TO_RANGE_TYPE, XML_TO_RANGE_VALUE, maxValue);
		} catch (IllegalArgumentException ex) {
			UIPlugin.getDefault().getLogger().log(Level.WARNING, "Problem reading range values.", ex); //$NON-NLS-1$
		} catch (QuantityConversionException ex) {
			UIPlugin.getDefault().getLogger().log(Level.WARNING, "Problem reading range values.", ex); //$NON-NLS-1$
		}
		getYAxis().setRangeType(yRangeValue);
		getYAxis().setMinValue(minValue);
		getYAxis().setMaxValue(maxValue);

		try {
			String renderingMode = memento.getString(XML_RENDERING_MODE_SETTING);
			if (renderingMode != null) {
				setRenderingMode(RenderingMode.valueOf(renderingMode));
			}
		} catch (IllegalArgumentException ex) {
			UIPlugin.getDefault().getLogger().log(Level.WARNING, "Problem reading rendering mode.", ex); //$NON-NLS-1$
		}

		getYAxis().setTitle(memento.getString(XML_Y_AXIS_TITLE));
		getXAxis().setTitle(memento.getString(XML_X_AXIS_TITLE));
		setChartTitle(memento.getString(XML_CHART_TITLE));
		getXAxis().notifyObservers();
		getYAxis().notifyObservers();
		notifyObservers();
	}

	private static IQuantity restoreTypeValue(
		IMemento memento, final String typeKey, final String valueKey, final IQuantity defaultValue)
			throws QuantityConversionException {
		String fromRangeTypeId = memento.getString(typeKey);
		if (fromRangeTypeId != null) {
			ContentType<?> contentType = UnitLookup.getContentType(fromRangeTypeId);
			KindOfQuantity<?> fromRangeType = (contentType instanceof KindOfQuantity<?>)
					? (KindOfQuantity<?>) contentType : UnitLookup.NUMBER;
			String valueString = memento.getString(valueKey);
			if (valueString != null) {
				return fromRangeType.parsePersisted(valueString);
			}
		}
		return defaultValue;
	}
}
