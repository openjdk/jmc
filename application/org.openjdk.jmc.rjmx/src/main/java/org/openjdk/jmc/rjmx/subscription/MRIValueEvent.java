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
package org.openjdk.jmc.rjmx.subscription;

import java.util.Objects;

import org.openjdk.jmc.rjmx.messages.internal.Messages;

/**
 * Event mainly used by the subscription engine. Contains information on what attribute was
 * retrieved, the value of the attribute and the time when the data was collected.
 */
public class MRIValueEvent {

	private static class UnavailableValue {
		@Override
		public String toString() {
			return '[' + Messages.LABEL_NOT_AVAILABLE + ']';
		}
	}

	public final static Object UNAVAILABLE_VALUE = new UnavailableValue();

	private final long m_timestamp;
	private final Object m_value;
	private final MRI m_mri;

	/**
	 * Constructor.
	 *
	 * @param mri
	 *            the subscription attribute from which the value originated. (Can be an aggregated
	 *            composite.)
	 * @param timestamp
	 *            the timestamp when the value was created.
	 * @param value
	 *            the actual value.
	 */
	public MRIValueEvent(MRI mri, long timestamp, Object value) {
		m_mri = mri;
		m_timestamp = timestamp;
		m_value = value;
	}

	/**
	 * Returns the subscription value source.
	 *
	 * @return the subscription value source.
	 */
	public final MRI getMRI() {
		return m_mri;
	}

	/**
	 * Returns the timestamp.
	 *
	 * @return long the timestamp for the event.
	 */
	public final long getTimestamp() {
		return m_timestamp;
	}

	/**
	 * Returns the value.
	 *
	 * @return Object
	 */
	public final Object getValue() {
		return m_value;
	}

	@Override
	public String toString() {
		return m_mri.getQualifiedName() + ' ' + getValue() + '@' + m_timestamp;
	}

	@Override
	public boolean equals(Object theOtherGuy) {
		if (theOtherGuy == null || getClass() != theOtherGuy.getClass()) {
			return false;
		}
		MRIValueEvent otherEvent = (MRIValueEvent) theOtherGuy;
		if (m_timestamp != otherEvent.m_timestamp || m_mri.equals(otherEvent.m_mri)) {
			return false;
		}
		return Objects.equals(m_value, otherEvent.m_value);
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + (int) (m_timestamp ^ (m_timestamp >>> 32));
		hash = 31 * hash + m_mri.hashCode();
		hash = 31 * hash + ((m_value == null) ? 0 : m_value.hashCode());
		return hash;
	}
}
