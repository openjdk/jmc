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
package org.openjdk.jmc.greychart.impl;

/**
 * An axis that renders dates that have been contributed as measured in nanoseconds, between the
 * current time and midnight, January 1, 1970 UTC.
 */
public class NanosXAxis extends AbstractAliasingLongAxis {
	// The aliasing array is used to find the right tick starting value and distance.
	private final static long[] ALIASING_ARRAY = {NanoFormatter.NANOSECOND, NanoFormatter.MICROSECOND,
			NanoFormatter.MILLISECOND, toNano(DateFormatter.SECOND), 30 * toNano(DateFormatter.SECOND),
			toNano(DateFormatter.MINUTE), toNano(DateFormatter.FIVE_MINUTES), toNano(DateFormatter.QUARTER_HOUR),
			toNano(DateFormatter.THIRD_HOUR), toNano(DateFormatter.HALF_HOUR), toNano(DateFormatter.HOURS),
			toNano(DateFormatter.QUARTER_DAY), toNano(DateFormatter.HALF_DAY), toNano(DateFormatter.DAY),
			toNano(DateFormatter.WEEK), toNano(DateFormatter.MONTH), toNano(DateFormatter.YEAR), Long.MAX_VALUE};

	private static final long DEFAULT_RANGE = 2 * NanoFormatter.MILLISECOND;

	private boolean m_autoRangeEnabled = false;

	private static long toNano(long ms) {
		return ms * NanoFormatter.MILLISECOND;
	}

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            the chart that owns the axis.
	 */
	public NanosXAxis(DefaultXYGreyChart<?> owner) {
		super(owner, ALIASING_ARRAY, DEFAULT_RANGE, 3 * NanoFormatter.MILLISECOND);
		setTickMarksEnabled(true);
		setNumberOfTicks(10);
		setFormatter(TimestampFormatter.createNanoTimestampFormatter());
	}

	/**
	 * Sets whether auto-range is enabled for this axis. When enabled, the axis will automatically
	 * determine its range based on the actual data.
	 *
	 * @param enable
	 *            true to enable auto-range, false to use fixed range
	 */
	public void setAutoRangeEnabled(boolean enable) {
		m_autoRangeEnabled = enable;
		if (enable) {
			// Update fields immediately to avoid null pointer issues
			updateAutoRangeFields();
		}
	}

	/**
	 * @return true if auto-range is enabled
	 */
	public boolean isAutoRangeEnabled() {
		return m_autoRangeEnabled;
	}

	@Override
	protected DefaultXYGreyChart<?> getOwner() {
		return (DefaultXYGreyChart<?>) super.getOwner();
	}

	@Override
	public Number getMin() {
		if (isAutoRangeEnabled()) {
			updateAutoRangeFields();
		}
		return super.getMin();
	}

	@Override
	public Number getMax() {
		if (isAutoRangeEnabled()) {
			updateAutoRangeFields();
		}
		return super.getMax();
	}

	private void updateAutoRangeFields() {
		DefaultXYGreyChart<?> chart = getOwner();
		if (chart != null) {
			OptimizingProvider provider = chart.getXAxisProvider();
			if (provider != null) {
				long minValue = provider.getDataMinX();
				long maxValue = provider.getDataMaxX();

				if (minValue != Long.MAX_VALUE && maxValue != Long.MIN_VALUE && minValue != maxValue) {
					setRange(minValue, maxValue);
				} else if (m_min == null || m_max == null) {
					setRange(0L, DEFAULT_RANGE);
				}
			} else if (m_min == null || m_max == null) {
				setRange(0L, DEFAULT_RANGE);
			}
		}
	}
}
