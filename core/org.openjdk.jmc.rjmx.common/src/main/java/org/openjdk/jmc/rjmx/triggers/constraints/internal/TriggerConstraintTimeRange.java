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
package org.openjdk.jmc.rjmx.triggers.constraints.internal;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.openjdk.jmc.rjmx.triggers.ITriggerConstraint;
import org.openjdk.jmc.rjmx.triggers.TriggerConstraint;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;

/**
 * Constraint that validates to true if the validation method is executed within the time boundary.
 * Only the hour and minute components of the date range are considered. If the hour component of
 * the "from" component is larger than the "to" component, it is assumed that the range over the day
 * boundary is meant.
 */
public class TriggerConstraintTimeRange extends TriggerConstraint {
	private static final String XML_ELEMENT_TIME_FROM = "from"; //$NON-NLS-1$
	private static final String XML_ELEMENT_TIME_TO = "to"; //$NON-NLS-1$

	/**
	 * Constructor.
	 */
	public TriggerConstraintTimeRange() {
	}

	/**
	 * @see ITriggerConstraint#isValid(TriggerEvent)
	 */
	@Override
	public boolean isValid(TriggerEvent e) {
		return validateTime(e.getCreationTime());
	}

	private boolean validateTime(Date validationDate) {
		int now = minuteOfDay(validationDate);
		int from = minuteOfDay(getSetting(XML_ELEMENT_TIME_FROM).getDateTime());
		int to = minuteOfDay(getSetting(XML_ELEMENT_TIME_TO).getDateTime());
		if (from <= to) {
			return now >= from && now <= to;
		} else {
			return now >= from || now <= to;
		}
	}

	private static int minuteOfDay(Date time) {
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(time);
		return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
	}

}
