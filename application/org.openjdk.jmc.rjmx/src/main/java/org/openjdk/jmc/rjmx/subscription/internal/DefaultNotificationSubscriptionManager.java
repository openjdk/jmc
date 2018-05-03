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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;

import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRISubscription;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.subscription.internal.IMRISubscriptionDebugInformation.SubscriptionState;

/**
 * Handles the notification based subscriptions. Since we're relying on the MBeanServerConnection to
 * deliver the events to us, this is not running in a separate thread. Hence the name "Manager".
 */
// FIXME: The Notification manager will currently not send out any null events on creation/destruction.
public final class DefaultNotificationSubscriptionManager {
	// The logger.
	private final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.rjmx.subscription"); //$NON-NLS-1$

	// NOTE: Commented out because only used by commented out code in handleNotification
//	private final IMBeanHelperService service;
	private final MBeanServerConnection mbeanServer;

	private volatile boolean collectDebugInfo = false;
	private Map<MRI, DefaultSubscriptionDebugInformation> subscriptionDebugInfo;

	/**
	 * HashSet used to keep track of what NotificationHandlers have been registered, to avoid too
	 * many registrations or too many deletions. <AbstractAttributeSubscription,
	 * NotificationHandler>
	 */
	private final Map<AbstractAttributeSubscription, NotificationHandler> registeredHandlers = Collections
			.synchronizedMap(new HashMap<AbstractAttributeSubscription, NotificationHandler>());

	/**
	 * An instance of this class is used to intercept a certain notification attribute.
	 */
	private class NotificationHandler implements NotificationListener {
		private final AbstractAttributeSubscription m_subscription;
		private final NotificationFilterSupport m_filter;

		/**
		 * Constructor.
		 *
		 * @param subscription
		 *            the subscription that this notification handler corresponds to.
		 */
		public NotificationHandler(AbstractAttributeSubscription subscription) {
			m_subscription = subscription;
			String type = AttributeValueToolkit.getAttributeName(subscription.getMRIMetadata().getMRI().getDataPath());
			m_filter = new NotificationFilterSupport();
			m_filter.enableType(type);
		}

		/**
		 * @throws MalformedObjectNameException
		 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification,
		 *      java.lang.Object)
		 */
		@Override
		public void handleNotification(Notification notification, Object callbackObj) {
			if (registeredHandlers.isEmpty()) {
				return;
			}
			try {
				MRI mri = m_subscription.getMRIMetadata().getMRI();
				String dataPath = mri.getDataPath();
				Object value = AttributeValueToolkit.lookupValue(dataPath, notification.getUserData());
				// FIXME: JMC-4270 - Server time approximation is not reliable
//				MRIValueEvent event = new MRIValueEvent(mri,
//						service.getApproximateServerTime(System.currentTimeMillis()), value);
				MRIValueEvent event = new MRIValueEvent(mri, System.currentTimeMillis(), value);
				recordEventRecieved(event);
				m_subscription.storeAndFireEvent(event);
			} catch (AttributeNotFoundException e) {
				LOGGER.log(Level.WARNING, "Notification object doesn't match declared structure.", e); //$NON-NLS-1$
			}
		}

		/**
		 * Registers this notification handler with the MBeanServerConnection, effectively starting
		 * the subscription.
		 */
		public void registerWithMBeanServer() {
			// Ensure that the handler has not been successfully registered yet
			try {
				IMRIMetadata info = m_subscription.getMRIMetadata();
				LOGGER.log(Level.FINE, "Adding listener " + info); //$NON-NLS-1$
				mbeanServer.addNotificationListener(info.getMRI().getObjectName(), this, m_filter, null);
				recordConnected(info.getMRI());
			} catch (IOException e) {
				// Silently fail. We we're probably just disconnected.
				LOGGER.log(Level.INFO, "Exception occured when registering notification handler.", e); //$NON-NLS-1$
			} catch (JMException e) {
				// Debug.exception(e);
				LOGGER.log(Level.WARNING, "Exception occured when registering notification handler.", e); //$NON-NLS-1$
			}
		}

		/**
		 * @param info
		 */
		private void unregisterWithMBeanServer() {
			// Ensure that this handler is really successfully registered,
			// before trying to unregister it
			IMRIMetadata info = m_subscription.getMRIMetadata();
			try {
				mbeanServer.removeNotificationListener(info.getMRI().getObjectName(), this, m_filter, null);
			} catch (JMException e) {
				// Ignore. Was most likely removed with other filter.
				LOGGER.log(Level.FINEST,
						"Got exception whilst removing notification listener. It was most likely already removed by some other handler.", //$NON-NLS-1$
						e);
			} catch (IOException e) {
				// Silently fail. Connection is down.
				LOGGER.log(Level.FINER,
						"Got exception whilst shutting down notification listeners. We were probably disconnected too early!", //$NON-NLS-1$
						e);
			}
			recordDisconnected(info.getMRI());
		}
	}

	public DefaultNotificationSubscriptionManager(IConnectionHandle handle)
			throws ConnectionException, ServiceNotAvailableException {
		mbeanServer = handle.getServiceOrThrow(MBeanServerConnection.class);
		// NOTE: Commented out because only used by commented out code in handleNotification
//		service = handle.getServiceOrThrow(IMBeanHelperService.class);
		clearDebugInformation();
	}

	public void shutdown() {
		// Unregister all the handlers...
		NotificationHandler[] handlers;
		synchronized (registeredHandlers) {
			Collection<NotificationHandler> handlersCol = registeredHandlers.values();
			handlers = handlersCol.toArray(new NotificationHandler[handlersCol.size()]);
			registeredHandlers.clear();
		}
		for (NotificationHandler handler : handlers) {
			handler.unregisterWithMBeanServer();
		}

	}

	public void registerNotificationAttributeSubscription(AbstractAttributeSubscription attributeSubscription) {
		if (attributeSubscription.getMRIMetadata().getMRI().getType() != Type.NOTIFICATION) {
			throw new IllegalArgumentException(
					"This subscription manager only handles notification based attributes that extends AbstractAttributeSubscription."); //$NON-NLS-1$
		}
		NotificationHandler handler = new NotificationHandler(attributeSubscription);
		handler.registerWithMBeanServer();
		NotificationHandler oldHandler = registeredHandlers.put(attributeSubscription, handler);
		if (oldHandler != null) {
			oldHandler.unregisterWithMBeanServer();
		}
	}

	public void unregisterNotificationAttributeSubscription(IMRISubscription attributeSubscription) {
		NotificationHandler handler = registeredHandlers.remove(attributeSubscription);
		if (handler != null) {
			handler.unregisterWithMBeanServer();
		}
	}

	public void collectDebugInformation(boolean collect) {
		collectDebugInfo = collect;
	}

	public void clearDebugInformation() {
		subscriptionDebugInfo = new HashMap<>();
	}

	public Collection<? extends IMRISubscriptionDebugInformation> getDebugInformation() {
		return subscriptionDebugInfo.values();
	}

	private void recordConnected(MRI mri) {
		if (collectDebugInfo) {
			DefaultSubscriptionDebugInformation info = getDebugInformation(mri, SubscriptionState.SUBSCRIBED);
			info.m_connectionCount += 1;
		}
	}

	private void recordDisconnected(MRI mri) {
		if (collectDebugInfo) {
			DefaultSubscriptionDebugInformation info = getDebugInformation(mri, SubscriptionState.UNSUBSCRIBED);
			info.m_disconnectionCount += 1;
		}
	}

	private void recordEventRecieved(MRIValueEvent event) {
		if (collectDebugInfo) {
			DefaultSubscriptionDebugInformation info = getDebugInformation(event.getMRI(),
					SubscriptionState.SUBSCRIBED);
			info.m_eventCount += 1;
			info.m_lastEvent = event;
		}
	}

	private DefaultSubscriptionDebugInformation getDebugInformation(MRI attribute, SubscriptionState state) {
		synchronized (subscriptionDebugInfo) {
			DefaultSubscriptionDebugInformation info = subscriptionDebugInfo.get(attribute);
			if (info == null) {
				info = new DefaultSubscriptionDebugInformation(attribute, state);
				subscriptionDebugInfo.put(attribute, info);
			} else {
				info.m_state = state;
			}
			return info;
		}
	}
}
