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
package org.openjdk.jmc.ui.dial;

import java.awt.Color;

import org.eclipse.ui.IMemento;

import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.ColorToolkit;

public class DialConfiguration {

	private final static String KEY_USE_WATERMARK = "useWatermark"; //$NON-NLS-1$
	private final static String KEY_GRADIENT_VALUE_TYPE = "gradientValueType"; //$NON-NLS-1$
	private final static String KEY_GRADIENT_BEGIN_VALUE = "gradientBeginValue"; //$NON-NLS-1$
	private final static String KEY_GRADIENT_END_VALUE = "gradientEndValue"; //$NON-NLS-1$
	private final static String KEY_GRADIENT_BEGIN_COLOR = "gradientBeginColor"; //$NON-NLS-1$
	private final static String KEY_GRADIENT_END_COLOR = "gradientEndColor"; //$NON-NLS-1$
	private final static String KEY_WATERMARK_COLOR = "watermarkColor"; //$NON-NLS-1$

	private Color m_gradientBeginColor = new Color(107, 143, 183);
	private Color m_gradientEndColor = new Color(107, 143, 183);
	private Color m_waterMarkColor = new Color(150, 150, 150);
	private IQuantity m_gradientBeginValue;
	private IQuantity m_gradientEndValue;
	private boolean m_useWatermark = true;

	public Color getWatermarkColor() {
		return m_waterMarkColor;
	}

	public Color getGradientBeginColor() {
		return m_gradientBeginColor;
	}

	public void setGradientBeginColor(Color color) {
		m_gradientBeginColor = color;
	}

	public void setGradientEndColor(Color color) {
		m_gradientEndColor = color;
	}

	public IQuantity getGradientBeginValue() {
		return m_gradientBeginValue;
	}

	public IQuantity getGradientEndValue() {
		return m_gradientEndValue;
	}

	public Color getGradientEndColor() {
		return m_gradientEndColor;
	}

	public boolean getUseWatermark() {
		return m_useWatermark;
	}

	public void setUseWatermark(boolean useWatermark) {
		m_useWatermark = useWatermark;
	}

	public void setGradientBeginValue(IQuantity beginValue) {
		m_gradientBeginValue = beginValue;
	}

	public void setGradientEndValue(IQuantity endValue) {
		m_gradientEndValue = endValue;
	}

	public void setWatermarkColor(Color color) {
		m_waterMarkColor = color;
	}

	public void saveState(IMemento state) {
		state.putBoolean(KEY_USE_WATERMARK, m_useWatermark);
		if (m_gradientBeginValue != null && m_gradientEndValue != null && m_gradientEndValue.getUnit().getContentType()
				.equals(m_gradientBeginValue.getUnit().getContentType())) {
			state.putString(KEY_GRADIENT_VALUE_TYPE, m_gradientBeginValue.getUnit().getContentType().getIdentifier());
			state.putString(KEY_GRADIENT_BEGIN_VALUE, m_gradientBeginValue.persistableString());
			state.putString(KEY_GRADIENT_END_VALUE, m_gradientEndValue.persistableString());
		}
		state.putString(KEY_GRADIENT_BEGIN_COLOR, ColorToolkit.encode(m_gradientBeginColor));
		state.putString(KEY_GRADIENT_END_COLOR, ColorToolkit.encode(m_gradientEndColor));
		state.putString(KEY_WATERMARK_COLOR, ColorToolkit.encode(m_waterMarkColor));
	}

	public void restoreState(IMemento state) {
		Boolean useWatermark = state.getBoolean(KEY_USE_WATERMARK);
		if (useWatermark != null) {
			setUseWatermark(useWatermark);
		}

		try {
			String type = state.getString(KEY_GRADIENT_VALUE_TYPE);
			String begin = state.getString(KEY_GRADIENT_BEGIN_VALUE);
			String end = state.getString(KEY_GRADIENT_END_VALUE);
			if (type != null && begin != null && end != null) {
				ContentType<?> contentType = UnitLookup.getContentType(type);
				if (contentType instanceof KindOfQuantity<?>) {
					setGradientBeginValue(((KindOfQuantity<?>) contentType).parsePersisted(begin));
					setGradientEndValue(((KindOfQuantity<?>) contentType).parsePersisted(end));
				}
			}
		} catch (QuantityConversionException e) {
			// Ignore, default to null.
		}

		String beginColor = state.getString(KEY_GRADIENT_BEGIN_COLOR);
		if (beginColor != null) {
			setGradientBeginColor(ColorToolkit.decode(beginColor));
		}
		String endColor = state.getString(KEY_GRADIENT_END_COLOR);
		if (endColor != null) {
			setGradientEndColor(ColorToolkit.decode(endColor));
		}
		String watermarkColor = state.getString(KEY_WATERMARK_COLOR);
		if (watermarkColor != null) {
			setWatermarkColor(ColorToolkit.decode(watermarkColor));
		}
	}

}
