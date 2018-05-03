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
package org.openjdk.jmc.rjmx.subscription.internal;

import java.util.Properties;

import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;

/**
 * A difference transformation. Will keep track of the last value and on each update return the
 * difference between the current and last value.
 */
public class DifferenceTransformation extends AbstractSingleMRITransformation {

	private MRIValueEvent m_lastAttributeEvent;
	private long m_rateMS;

	@Override
	public void setProperties(Properties properties) {
		super.setProperties(properties);
		m_rateMS = Long.parseLong(properties.getProperty("rate", "0")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public Object createSubscriptionValue(MRIValueEvent event) {
		Object lastValue = null;
		if (m_lastAttributeEvent != null) {
			lastValue = m_lastAttributeEvent.getValue();
		}
		Object value = event.getValue();
		Number eventValue = null;
		if (value instanceof Number && lastValue instanceof Number) {
			eventValue = subtract((Number) value, (Number) lastValue);
			if (m_rateMS > 0) {
				eventValue = eventValue.doubleValue() * m_rateMS
						/ (event.getTimestamp() - m_lastAttributeEvent.getTimestamp());
			}
		}
		m_lastAttributeEvent = event;
		return (eventValue == null) ? NO_VALUE : eventValue;
	}
}
