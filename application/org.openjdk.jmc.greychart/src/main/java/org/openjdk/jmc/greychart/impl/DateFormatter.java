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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openjdk.jmc.greychart.TickFormatter;

/**
 * Handles numbers in ms from 1970. Will automatically choose a representation that suits the world
 * width.
 */
public final class DateFormatter implements TickFormatter {
	public final static long MILLISECOND = 1;
	public final static long SECOND = 1000;
	public final static long MINUTE = 60 * SECOND;
	public final static long FIVE_MINUTES = 5 * MINUTE;
	public final static long QUARTER_HOUR = 3 * FIVE_MINUTES;
	public final static long THIRD_HOUR = 20 * MINUTE;
	public final static long HALF_HOUR = 2 * QUARTER_HOUR;
	public final static long HOURS = 2 * HALF_HOUR;
	public final static long QUARTER_DAY = 6 * HOURS;
	public final static long HALF_DAY = 12 * HOURS;
	public final static long DAY = 2 * HALF_DAY;
	public final static long WEEK = 7 * DAY;
	public final static long MONTH = 4 * WEEK;
	public final static long YEAR = 365 * DAY;

	// The range array is used to find the correct formatting string to use.
	private final static long[] RANGE_ARRAY = {MILLISECOND, SECOND, 10 * SECOND, 10 * MINUTE, 2 * DAY, 2 * WEEK,
			10 * WEEK, MONTH, YEAR, Long.MAX_VALUE};

	private final static String[] FORMATTING_STRINGS = {"S", "s:SSS", "mm:ss", "HH:mm", "EEE HH:mm", "EEE", "ww", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
			"MMMMM", "yyyy"}; //$NON-NLS-1$ //$NON-NLS-2$

	private final static String[] UNIT_STRINGS = {" (ms)", " (s:ms)", " (m:s)", " (h:m)", "", "", "", "", "", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
			" (week)", "", "", "", ""}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	private static SimpleDateFormat[] m_formatterArray = new SimpleDateFormat[FORMATTING_STRINGS.length];

	// Reusable date object.
	private final Date m_dateCache = new Date();

	@Override
	public String format(Number value, Number min, Number max, Number labelInterval) {
		return getFormattedString(value.longValue(), getRangeIndex(max.longValue() - min.longValue()));
	}

	@Override
	public String getUnitString(Number min, Number max) {
		return UNIT_STRINGS[getRangeIndex(max.longValue() - min.longValue())];
	}

	private synchronized SimpleDateFormat getDateFormat(int rangeIndex) {
		if (m_formatterArray[rangeIndex] == null) {
			m_formatterArray[rangeIndex] = new SimpleDateFormat(FORMATTING_STRINGS[rangeIndex]);
		}
		return m_formatterArray[rangeIndex];
	}

	private String getFormattedString(long time, int rangeIndex) {
		m_dateCache.setTime(time);
		return getDateFormat(rangeIndex).format(m_dateCache);
	}

	private int getRangeIndex(long range) {
		for (int i = 0; i < RANGE_ARRAY.length; i++) {
			if (range < RANGE_ARRAY[i]) {
				return (i >= 1) ? i - 1 : 0;
			}
		}
		return 0;
	}
}
