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
package org.openjdk.jmc.rjmx.test.triggers;

import org.w3c.dom.Element;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.triggers.ITriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;

/**
 * Simple action that just calls the NotificationActionCallbackReceiver with the event. This class
 * primarily exists to facilitate unit testing of the notification framework.
 */
public class NotificationActionCallback implements ITriggerAction {
	/**
	 * Action name.
	 */
	public static final String NAME = "Callback action";

	/**
	 * Description. Won't be used for this class.
	 */
	public static final String DESCRIPTION = "Blablablablablablabla";

	private final NotificationActionCallbackReceiver m_receiver;

	/**
	 * Notification event callback interface.
	 */
	public interface NotificationActionCallbackReceiver {
		/**
		 * The callback method.
		 *
		 * @param e
		 *            the event.
		 */
		void onNotificationAction(TriggerEvent e);
	}

	/**
	 * Constructor.
	 *
	 * @param receiver
	 *            the callback implementation to be called when the action fires.
	 */
	public NotificationActionCallback(NotificationActionCallbackReceiver receiver) {
		m_receiver = receiver;
	}

	/**
	 * @see ITriggerAction#handleNotificationEvent(TriggerEvent)
	 */
	@Override
	public void handleNotificationEvent(TriggerEvent e) {
		m_receiver.onNotificationAction(e);
	}

	/**
	 * @see AbstractNotificationAction#getName()
	 */
	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * @see AbstractNotificationAction#getDescription()
	 */
	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}

	@Override
	public void initializeFromXml(Element node) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToXml(Element parentNode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsAction(IConnectionHandle connection) {
		return true;
	}

	@Override
	public boolean isReady() {
		return true;
	}
}
