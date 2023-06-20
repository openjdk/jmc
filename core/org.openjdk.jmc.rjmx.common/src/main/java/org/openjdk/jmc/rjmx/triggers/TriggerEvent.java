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
package org.openjdk.jmc.rjmx.triggers;

import java.util.Date;

import org.openjdk.jmc.rjmx.IConnectionHandle;

/**
 * Event sent to registered TriggerActions when a certain condition is met (after a constraint and
 * value check).
 */
public class TriggerEvent {
	private TriggerRule m_rule;
	private IConnectionHandle m_source;
	private Object m_value;
	private Date m_creationTime = new Date();
	private int m_sustainTime;
	private boolean m_wasTriggered;

	/**
	 * Warning! This constructor should be used with care. The only valid use is for unit testing.
	 *
	 * @param creationTime
	 *            the time of creation.
	 */
	TriggerEvent(Date creationTime) {
		m_creationTime = creationTime;
	}

	/**
	 * Constructor.
	 *
	 * @param source
	 *            The connector model that sent the event. want to pass references to such large
	 *            objects in an event that might be passed over a network in the future.
	 * @param rule
	 *            The rule that resulted in this event.
	 * @param triggerValue
	 *            The value on which we triggered.
	 * @param wasTriggered
	 *            True if we were triggered, false if we recovered.
	 */
	public TriggerEvent(IConnectionHandle source, TriggerRule rule, Object triggerValue, boolean wasTriggered) {
		this(source, rule, triggerValue, wasTriggered, 0);
	}

	/**
	 * Constructor.
	 *
	 * @param source
	 *            The ConnectorModel that sent the event.
	 * @param rule
	 *            The rule that resulted in this event.
	 * @param triggerValue
	 *            The value on which we triggered.
	 * @param wasTriggered
	 *            True if we were triggered, false if we recovered.
	 * @param sustainTime
	 *            &gt; 0 if this was a result of triggering from a certain level being sustained
	 *            over a certain time.
	 */
	public TriggerEvent(IConnectionHandle source, TriggerRule rule, Object triggerValue, boolean wasTriggered,
			int sustainTime) {
		m_source = source;
		m_value = triggerValue;
		m_rule = rule;
		m_wasTriggered = wasTriggered;
		m_sustainTime = sustainTime;
	}

	/**
	 * Returns the value that triggered the rule.
	 *
	 * @return the value that triggered the rule.
	 */
	public Object getTriggerValue() {
		return m_value;
	}

	/**
	 * Returns the time (date) the event was created.
	 *
	 * @return the time (date) the event was created.
	 */
	public Date getCreationTime() {
		return m_creationTime;
	}

	/**
	 * Gets the rule.
	 *
	 * @return NotificationRule the NotificationRule that triggered the event.
	 */
	public TriggerRule getRule() {
		return m_rule;
	}

	/**
	 * Returns true if the event was Triggered.
	 *
	 * @return boolean true if it was triggered.
	 */
	public boolean wasTriggered() {
		return m_wasTriggered;
	}

	/**
	 * Returns true if the event was Triggered.
	 *
	 * @return boolean true if it was recovering.
	 */
	public boolean wasRecovered() {
		return !m_wasTriggered;
	}

	/**
	 * Gets the connectorSourceDescription.
	 *
	 * @return Returns a String
	 */
	public String getConnectorSourceDescription() {
		return m_source.getServerDescriptor().getDisplayName();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getConnectorSourceDescription() + '/' + getRule().getName() + " - " + getCreationTime(); //$NON-NLS-1$
	}

	/**
	 * Returns the time this value was sustained. If this event is not the result of a rule having a
	 * sustain value &gt; 0, this value will be 0.
	 *
	 * @return the time this value was sustained. If this event is not the result of a rule having a
	 *         sustain value &gt; 0, this value will be 0.
	 */
	public int getSustainTime() {
		return m_sustainTime;
	}

	/**
	 * Returns the {@link IConnectionHandle} that was the source of the event.
	 *
	 * @return the {@link IConnectionHandle} that was the source of the event.
	 */
	public IConnectionHandle getSource() {
		return m_source;
	}
}
