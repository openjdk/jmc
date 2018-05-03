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

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openjdk.jmc.greychart.TickFormatter;

/**
 * Class for formatting a timestamp where the time is in nanoseconds since 1970. This class is not
 * thread safe.
 */
public final class TimestampFormatter implements TickFormatter {
	private static final long NANOS_PER_MICROS = 1000;
	private static final long NANOS_PER_MILLIS = 1000 * NANOS_PER_MICROS;
	private static final long NANOS_PER_SECOND = 1000 * NANOS_PER_MILLIS;
	private static final long NANOS_PER_MINUTE = 60 * NANOS_PER_SECOND;
	private static final long NANOS_PER_HOUR = 60 * NANOS_PER_MINUTE;
	private static final long NANOS_PER_DAY = 24 * NANOS_PER_HOUR;

	public static enum Precision {

		NANOS(1, 6),
		MICROS(NANOS_PER_MICROS, 3),
		MILLIS(NANOS_PER_MILLIS, 0),
		SECONDS(NANOS_PER_SECOND, 0),
		MINUTES(NANOS_PER_MINUTE, 0),
		HOURS(NANOS_PER_HOUR, 0),
		DAYS(NANOS_PER_DAY, 0),
		MONTHS(30 * NANOS_PER_DAY, 0),
		YEARS(365 * NANOS_PER_DAY, 0),
		AUTOMATIC(0, 0);

		private static final long CUT_OFF_RATIO = 10000;

		private long m_nanos;
		private int m_fractionDigits;

		private Precision(long nanos, int digits) {
			m_nanos = nanos;
			m_fractionDigits = digits;
		}

		public boolean isLargerThan(Precision precision) {
			return precision.ordinal() < ordinal();
		}

		public boolean isLessThan(Precision precision) {
			return ordinal() < precision.ordinal();
		}

		public long inNanos() {
			return m_nanos;
		}

		public int getFractionDigits() {
			return m_fractionDigits;
		}

		public static Precision getPrecisionForRange(long duration) {
			for (Precision precision : Precision.values()) {
				if (duration < CUT_OFF_RATIO * precision.inNanos() && precision != AUTOMATIC) {
					return precision;
				}
			}
			return YEARS;
		}
	}

	private final NumberFormat m_format = NumberFormat.getInstance();
	private final long m_factor;
	private Precision m_precision = Precision.AUTOMATIC;
	private boolean m_alwaysShowDate = false;
	private String m_timeDateSeparator = " "; //$NON-NLS-1$

	TimestampFormatter(int factor) {
		m_factor = factor;
	}

	public static TimestampFormatter createNanoTimestampFormatter() {
		return new TimestampFormatter(1);
	}

	public static TimestampFormatter createMillisTimestampFormatter() {
		return new TimestampFormatter(1000 * 1000);
	}

	public String format(long value, long startTimestamp, long endTimestamp) {
		return format(value, startTimestamp, endTimestamp, null);
	}

	@Override
	public String format(Number value, Number min, Number max, Number labelDistance) {
		long nanos1970timestamp = value.longValue() * m_factor;
		long nanosDuration = max.longValue() * m_factor - min.longValue() * m_factor;
		Date millisDate = new Date(nanos1970timestamp / NANOS_PER_MILLIS);

		Precision precision = m_precision == Precision.AUTOMATIC ? Precision.getPrecisionForRange(nanosDuration)
				: m_precision;

		String text = ""; //$NON-NLS-1$
		if (precision.isLargerThan(Precision.SECONDS) || getAlwaysShowDate()) {
			text += formatDate(precision, millisDate);
			text += getTimeDateSeapator();
		}

		if (precision.isLessThan(Precision.DAYS)) {
			text += formatTime(precision, millisDate);
			if (precision.isLessThan(Precision.SECONDS)) {
				text += formatSubSecond(precision, nanos1970timestamp % NANOS_PER_SECOND);
			}
		}

		return text;
	}

	@Override
	public String getUnitString(Number min, Number max) {
		return ""; //$NON-NLS-1$
	}

	protected String formatSubSecond(Precision precision, long nanos) {
		return "." + formatMillis(precision, nanos / ((double) NANOS_PER_MILLIS)); //$NON-NLS-1$
	}

	protected String formatTime(Precision precision, Date date) {
		if (precision.isLargerThan(Precision.SECONDS)) {
			return DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
		} else {
			return DateFormat.getTimeInstance(DateFormat.MEDIUM).format(date);
		}
	}

	protected String formatDate(Precision precision, Date date) {
		if (precision == Precision.YEARS) {
			return new SimpleDateFormat("yyyy").format(date); //$NON-NLS-1$
		} else {
			return DateFormat.getDateInstance(DateFormat.SHORT).format(date);
		}
	}

	protected String formatMillis(Precision precision, double millis) {
		m_format.setMinimumIntegerDigits(3);
		m_format.setMaximumIntegerDigits(3);
		m_format.setMinimumFractionDigits(precision.getFractionDigits());
		m_format.setMaximumFractionDigits(precision.getFractionDigits());
		return m_format.format(millis);
	}

	public final void setPrecision(Precision precision) {
		m_precision = precision;
	}

	public final Precision getPrecision() {
		return m_precision;
	}

	public final boolean getAlwaysShowDate() {
		return m_alwaysShowDate;
	}

	public final void setAlwaysShowDate(boolean alwaysShow) {
		m_alwaysShowDate = alwaysShow;
	}

	public final void setTimeDateSeparator(String separator) {
		m_timeDateSeparator = separator;
	}

	public final String getTimeDateSeapator() {
		return m_timeDateSeparator;
	}
}
