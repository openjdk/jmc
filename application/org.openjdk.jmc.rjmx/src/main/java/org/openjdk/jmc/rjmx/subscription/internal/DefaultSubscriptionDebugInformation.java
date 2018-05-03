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

import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;

public class DefaultSubscriptionDebugInformation implements IMRISubscriptionDebugInformation {

	public MRI m_mri;
	public SubscriptionState m_state;
	public int m_connectionCount;
	public int m_disconnectionCount;
	public int m_eventCount;
	public int m_retainedEventCount;
	public MRIValueEvent m_lastEvent;
	public int m_connectionLostCount;
	public int m_triedReconnectionCount;
	public int m_succeededReconnectionCount;

	public DefaultSubscriptionDebugInformation(MRI mri, SubscriptionState state) {
		m_mri = mri;
		m_state = state;
	}

	@Override
	public MRI getMRI() {
		return m_mri;
	}

	@Override
	public SubscriptionState getState() {
		return m_state;
	}

	@Override
	public int getConnectionCount() {
		return m_connectionCount;
	}

	@Override
	public int getDisconnectionCount() {
		return m_disconnectionCount;
	}

	@Override
	public int getEventCount() {
		return m_eventCount;
	}

	@Override
	public int getRetainedEventCount() {
		return m_retainedEventCount;
	}

	@Override
	public MRIValueEvent getLastEvent() {
		return m_lastEvent;
	}

	@Override
	public int getConnectionLostCount() {
		return m_connectionLostCount;
	}

	@Override
	public int getTriedReconnectionsCount() {
		return m_triedReconnectionCount;
	}

	@Override
	public int getSucceededReconnectionsCount() {
		return m_succeededReconnectionCount;
	}
}
