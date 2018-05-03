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

import java.util.logging.Level;

import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.modelmbean.ModelMBeanNotificationBroadcaster;

import org.openjdk.jmc.rjmx.ISyntheticNotification;
import org.openjdk.jmc.rjmx.RJMXPlugin;

/**
 * This is the base class to extend from for synthetic notifications.
 */
public abstract class AbstractSyntheticNotification implements ISyntheticNotification {
	private String m_type;
	private String m_message;
	private long m_sequenceNumber = 0;
	private ModelMBeanNotificationBroadcaster broadcaster;

	@Override
	public abstract Object getValue();

	/**
	 * This method will by default return the message that is provided by the extension point.
	 * Override this method to provide your own dynamic message.
	 *
	 * @return the message.
	 */
	protected String getMessage() {
		return m_message;
	}

	@Override
	public void stop() {
	}

	/**
	 * Call this method to trigger a notification. By default this method will call getValue to get
	 * the value to include in the trigger.
	 */
	protected final void triggerNotification() {
		Object value = getValue();
		Notification notification = new Notification(m_type, this, m_sequenceNumber++, getMessage());
		notification.setUserData(value);
		try {
			broadcaster.sendNotification(notification);
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Unable to trigger notification!", e); //$NON-NLS-1$
			e.printStackTrace();
		}
	}

	@Override
	public void init(MBeanServerConnection connection, String type, String message) {
		m_type = type;
		m_message = message;
	}

	@Override
	public final void init(ModelMBeanNotificationBroadcaster broadcaster) {
		this.broadcaster = broadcaster;
	}

	@Override
	public final ModelMBeanNotificationBroadcaster getBroadcaster() {
		return broadcaster;
	}
}
