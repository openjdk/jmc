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
package org.openjdk.jmc.greychart.impl;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import org.openjdk.jmc.greychart.TickFormatter;
import org.openjdk.jmc.greychart.util.ChartRenderingToolkit;

/**
 * Not thread safe! Use a new instance per chart!
 */
public class DoubleFormatter implements TickFormatter {
	private double lastLabelDistance = Double.MIN_VALUE;
	private double lastMin = Double.MAX_VALUE;
	private double lastMax = Double.MIN_VALUE;
	private NumberFormat formatter = DecimalFormat.getInstance();

	@Override
	public String format(Number value, Number min, Number max, Number labelDistance) {
		if (lastMin != min.doubleValue() || lastMax != max.doubleValue()
				|| lastLabelDistance != labelDistance.doubleValue()) {
			formatter = createFormat(min.doubleValue(), max.doubleValue(), labelDistance.doubleValue());
			lastMin = min.doubleValue();
			lastMax = max.doubleValue();
			lastLabelDistance = labelDistance.doubleValue();
		}
		return formatter.format(value.doubleValue());
	}

	/**
	 * Creates a new {@link NumberFormat} to format the labels of the axis.
	 *
	 * @param min
	 *            the value of the label closest to negative infinity
	 * @param max
	 *            the value of the label closest to positive infinity
	 * @param tickInterval
	 *            the distance between two label
	 * @return a {@link NumberFormat} to format the labels in an uniform way
	 */
	private static NumberFormat createFormat(double min, double max, double tickInterval) {
		NumberFormat format;
		double maxValue = Math.max(Math.abs(max), Math.abs(min));
		if (maxValue >= 1e9 || maxValue < 1e-9) {
			format = new EngineeringFormat(min, max, tickInterval);
		} else if (tickInterval == ChartRenderingToolkit.fastFloor(tickInterval)) {
			format = NumberFormat.getIntegerInstance();
		} else {
			int exp = log10Exponent(tickInterval);
			format = NumberFormat.getInstance();
			format.setMaximumFractionDigits(-exp);
			format.setMinimumFractionDigits(-exp);
		}
		return format;
	}

	@Override
	public String getUnitString(Number min, Number max) {
		return ""; //$NON-NLS-1$
	}

	/**
	 * A {@link NumberFormat} that formats numbers on the format 12.34E56.
	 *
	 * @note It does NOT parse strings! Will return null instead of a number.
	 */
	private static class EngineeringFormat extends NumberFormat {
		final static NumberFormat FORMAT = new DecimalFormat("##0.00"); //$NON-NLS-1$
		final static long serialVersionUID = 0;

		private final double baseNumber;
		private final double lsnNumber;
		private final String exponent;

		/**
		 * Creates a new {@link EngineeringFormat} that will format values in given range from min
		 * to max and modulo lsn.
		 *
		 * @param min
		 *            the value closest to negative infinity
		 * @param max
		 *            the value closest to positive infinity
		 * @param lsn
		 *            least significant value, the smallest step taken between min and max
		 */
		public EngineeringFormat(double min, double max, double lsn) {
			this(min, max, lsn, new DecimalFormatSymbols());
		}

		/**
		 * Creates a new {@link EngineeringFormat} that will format values in given range from min
		 * to max and modulo lsn.
		 *
		 * @param min
		 *            the value closest to negative infinity
		 * @param max
		 *            the value closest to positive infinity
		 * @param lsn
		 *            least significant value, the smallest step taken between min and max
		 * @param symbols
		 *            the set of symbols to be used
		 */
		public EngineeringFormat(double min, double max, double lsn, DecimalFormatSymbols symbols) {
			int minExp = log10Exponent(min);
			int maxExp = log10Exponent(max);
			int minBaseExp = getEvenThree(minExp);
			int maxBaseExp = getEvenThree(maxExp);
			int baseExp = Math.max(maxBaseExp, minBaseExp);
			StringBuffer sb = new StringBuffer("E"); //$NON-NLS-1$
			if (baseExp < 0) {
				sb.append(symbols.getMinusSign());
			}
			sb.append(Math.abs(baseExp));
			exponent = sb.toString();
			baseNumber = Math.pow(10, baseExp);
			int lsnExp = log10Exponent(lsn);
			lsnNumber = Math.pow(10, lsnExp);
			FORMAT.setGroupingUsed(false);
		}

		private int getEvenThree(int minExp) {
			return (minExp / 3) * 3;
		}

		@Override
		public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
			if (Math.round(number / lsnNumber) == 0.0) {
				toAppendTo.append('0');
				return toAppendTo;
			}
			FORMAT.format(number / baseNumber, toAppendTo, pos);
			toAppendTo.append(exponent);
			return toAppendTo;
		}

		@Override
		public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
			return format((double) number, toAppendTo, pos);
		}

		@Override
		public Number parse(String source, ParsePosition parsePosition) {
			return null;
		}
	}

	/**
	 * Returns the 10 logarithm of the value rounded down.
	 *
	 * @param d
	 *            the value to calculate exponent for
	 * @return the rounded down 10 exponent
	 */
	private static int log10Exponent(double d) {
		return (int) Math.round(ChartRenderingToolkit.fastFloor(ChartRenderingToolkit.log10(Math.abs(d))));
	}
}
