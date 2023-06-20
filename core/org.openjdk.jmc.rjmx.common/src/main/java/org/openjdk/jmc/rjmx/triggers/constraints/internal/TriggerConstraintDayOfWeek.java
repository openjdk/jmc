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
import java.util.GregorianCalendar;

import org.openjdk.jmc.rjmx.triggers.ITriggerConstraint;
import org.openjdk.jmc.rjmx.triggers.TriggerConstraint;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;

/**
 * Constraint which validates on the day of week.
 */
public class TriggerConstraintDayOfWeek extends TriggerConstraint {
	private static final String XML_ELEMENT_MONDAY = "monday"; //$NON-NLS-1$
	private static final String XML_ELEMENT_TUESDAY = "tuesday"; //$NON-NLS-1$
	private static final String XML_ELEMENT_WEDNESDAY = "wednesday"; //$NON-NLS-1$
	private static final String XML_ELEMENT_THURSDAY = "thursday"; //$NON-NLS-1$
	private static final String XML_ELEMENT_FRIDAY = "friday"; //$NON-NLS-1$
	private static final String XML_ELEMENT_SATURDAY = "saturday"; //$NON-NLS-1$
	private static final String XML_ELEMENT_SUNDAY = "sunday"; //$NON-NLS-1$
	private final GregorianCalendar m_calendar = new GregorianCalendar();

	/**
	 * Constructor
	 */
	public TriggerConstraintDayOfWeek() {

	}

	/**
	 * Returns true if the day the event was created is a valid day.
	 *
	 * @see ITriggerConstraint#isValid(TriggerEvent)
	 */
	@Override
	public boolean isValid(TriggerEvent e) {
		m_calendar.setTime(e.getCreationTime());
		switch (m_calendar.get(Calendar.DAY_OF_WEEK)) {
		case Calendar.MONDAY:
			return getSetting(XML_ELEMENT_MONDAY).getBoolean().booleanValue();
		case Calendar.TUESDAY:
			return getSetting(XML_ELEMENT_TUESDAY).getBoolean().booleanValue();

		case Calendar.WEDNESDAY:
			return getSetting(XML_ELEMENT_WEDNESDAY).getBoolean().booleanValue();

		case Calendar.THURSDAY:
			return getSetting(XML_ELEMENT_THURSDAY).getBoolean().booleanValue();

		case Calendar.FRIDAY:
			return getSetting(XML_ELEMENT_FRIDAY).getBoolean().booleanValue();

		case Calendar.SATURDAY:
			return getSetting(XML_ELEMENT_SATURDAY).getBoolean().booleanValue();

		case Calendar.SUNDAY:
			return getSetting(XML_ELEMENT_SUNDAY).getBoolean().booleanValue();
		default:
			throw new IllegalArgumentException("Error in validation routine!"); //$NON-NLS-1$
		}
	}
}
