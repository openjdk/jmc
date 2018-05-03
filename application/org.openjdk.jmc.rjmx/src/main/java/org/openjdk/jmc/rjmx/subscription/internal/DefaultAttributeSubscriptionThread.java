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
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ReflectionException;

import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.IAttributeStorageService;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.subscription.IMRISubscription;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.subscription.internal.IMRISubscriptionDebugInformation.SubscriptionState;

/**
 * This is the core subscription engine. It is responsible for periodically, depending on the policy
 * of the AttributeSubscriptions, schedule calls for retrieving the current value of the
 * corresponding attribute.
 * <p>
 * Note that some attributes are not handled by this subscription thread, for instance notification
 * based ones.
 */
public class DefaultAttributeSubscriptionThread extends Thread {
	// The logger.
	private final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.rjmx.subscription"); //$NON-NLS-1$

	private final IConnectionHandle connectionHandle;
	private IAttributeStorageService attributeStorageService;
	private final IMBeanHelperService helperService;
	private final MBeanServerConnection server;
	private final Map<MRI, AbstractAttributeSubscription> attributeSubscriptions = new HashMap<>();

	private final Map<IMRISubscription, SubscriptionStats> subscriptionStats = new HashMap<>();
	private volatile boolean isRunning = true;
	private long lastTimestamp;
	private final static long MAX_SLEEP_TIME = 2000;
	private final static long MIN_SLEEP_TIME = 100;
	private final Set<AbstractAttributeSubscription> recentlyAddedSubscriptions = new HashSet<>();
	private final Set<AbstractAttributeSubscription> recentlyRemovedSubscriptions = new HashSet<>();
	private final UnavailableSubscriptionsRepository unavailableSubscriptionsRepository;

	private boolean sendNulls;

	private volatile boolean collectDebugInfo = false;
	private Map<MRI, DefaultSubscriptionDebugInformation> subscriptionDebugInfo;

	public static class SubscriptionStats {
		public long lastUpdate = Long.MIN_VALUE;
	}

	public DefaultAttributeSubscriptionThread(IConnectionHandle connectionHandle)
			throws ConnectionException, ServiceNotAvailableException {
		super("RJMX Subscription thread on " + connectionHandle.getServerDescriptor().getDisplayName()); //$NON-NLS-1$
		this.connectionHandle = connectionHandle;
		helperService = connectionHandle.getServiceOrThrow(IMBeanHelperService.class);
		server = connectionHandle.getServiceOrThrow(MBeanServerConnection.class);
		unavailableSubscriptionsRepository = new UnavailableSubscriptionsRepository(connectionHandle);
		clearDebugInformation();
	}

	/**
	 * This is where all the action is. Starting this thread will start the subscription of the
	 * registered subscriptions.
	 */
	@Override
	public void run() {
		while (isRunning) {
			unregisterSubscriptionsQueuedForRemove();
			reregisterPreviouslyBadSubscriptions();
			registerSubscriptionsQueuedForAdd();
			long sleepTime = retrieveAndDispatchValues();
			try {
				sleep(sleepTime);
			} catch (InterruptedException e) {
				// We were interrupted - close down shop.
				isRunning = false;
			}
		}
		synchronized (recentlyRemovedSubscriptions) {
			recentlyRemovedSubscriptions.addAll(attributeSubscriptions.values());
			recentlyRemovedSubscriptions.addAll(unavailableSubscriptionsRepository.getAllSubscriptions());
			unavailableSubscriptionsRepository.dispose();
		}
		unregisterSubscriptionsQueuedForRemove();
	}

	private long retrieveAndDispatchValues() {
		if (attributeSubscriptions.isEmpty()) {
			return MIN_SLEEP_TIME;
		}
		long now = System.currentTimeMillis();
		long nextUpdate = Long.MAX_VALUE;
		List<MRI> normalAttributes = new ArrayList<>();
		for (AbstractAttributeSubscription subscription : attributeSubscriptions.values()) {
			SubscriptionStats stats = subscriptionStats.get(subscription);
			long targetTime = subscription.getUpdatePolicy().getNextUpdate(stats.lastUpdate);
			if (targetTime <= now) {
				normalAttributes.add(subscription.getMRIMetadata().getMRI());
				stats.lastUpdate = now;
				targetTime = subscription.getUpdatePolicy().getNextUpdate(now);
			}
			nextUpdate = Math.min(nextUpdate, targetTime);
		}
		retrieveAndDispatchNormalAttributes(normalAttributes);
		return Math.max(MIN_SLEEP_TIME, Math.min(nextUpdate - now, MAX_SLEEP_TIME));
	}

	private void retrieveAndDispatchNormalAttributes(List<MRI> normalAttributes) {
		// Attempt to retrieve all attributes.
		// Will automatically remove all failing attribute subscriptions.
		try {
			List<MRIValueEvent> attributeValues = sampleAttributes(normalAttributes);
			dispatchEvents(attributeValues);
			if (attributeValues.size() != normalAttributes.size()) {
				removeBadAttributes(normalAttributes, attributeValues);
			}
		} catch (InstanceNotFoundException e) {
			searchAndRemoveBadAttributes(normalAttributes);
		} catch (ReflectionException e) {
			searchAndRemoveBadAttributes(normalAttributes);
		} catch (InvoluntaryDisconnectException e) {
			LOGGER.warning("Subscription thread is terminating due to loss of connection!"); //$NON-NLS-1$
			dispatchConnectionLostEvents();
			shutdown();
		} catch (ConnectException e) {
			LOGGER.warning("Subscription thread is terminating due to loss of connection!"); //$NON-NLS-1$
			dispatchConnectionLostEvents();
			shutdown();
		} catch (IOException e) {
			searchAndRemoveBadAttributes(normalAttributes);
		} catch (RuntimeException e) {
			if (isRunning) {
				throw e;
			} else {
				LOGGER.fine("Failed to get attributes, probably since the subscription thread is terminating"); //$NON-NLS-1$
			}
		}
	}

	private List<MRIValueEvent> sampleAttributes(Iterable<MRI> attributes)
			throws IOException, InstanceNotFoundException, ReflectionException {
		long before = System.currentTimeMillis();
		Map<MRI, Object> values = AttributeValueToolkit.getAttributes(server, attributes);
		// FIXME: JMC-4270 - Server time approximation is not reliable
//		long timestamp = helperService.getApproximateServerTime((System.currentTimeMillis() + before) / 2);
		long timestamp = (System.currentTimeMillis() + before) / 2;

		List<MRIValueEvent> results = new ArrayList<>();
		for (Entry<MRI, Object> entry : values.entrySet()) {
			results.add(new MRIValueEvent(entry.getKey(), timestamp, entry.getValue()));
		}
		return results;
	}

	private void dispatchConnectionLostEvents() {
		for (AbstractAttributeSubscription subscription : attributeSubscriptions.values()) {
			// Desperate times call for desperate measures. We time stamp these with the client system time.
			ConnectionLostEvent event = new ConnectionLostEvent(subscription, System.currentTimeMillis());
			subscription.storeAndFireEvent(event);
		}
	}

	/**
	 * @return the connection handle associated with this subscription thread.
	 */
	public IConnectionHandle getConnectionHandle() {
		return connectionHandle;
	}

	/**
	 * @param subscription
	 *            the subscription to register.
	 */
	public void registerAttributeSubscription(IMRISubscription subscription) {
		if (!(subscription instanceof AbstractAttributeSubscription)) {
			throw new IllegalArgumentException(
					"This version of the subscription service can only handle AbstractAttributeSubscriptions."); //$NON-NLS-1$
		}
		synchronized (recentlyAddedSubscriptions) {
			recentlyAddedSubscriptions.add((AbstractAttributeSubscription) subscription);
		}
	}

	/**
	 * This method is only to be called from the subscription thread!
	 */
	private void registerSubscriptionsQueuedForAdd() {
		if (Thread.currentThread() != this) {
			LOGGER.warning("registerQueuedSubscriptions abused in DefaultAttributeSubscriptionThread!"); //$NON-NLS-1$
		}

		List<AbstractAttributeSubscription> recentlyAdded = new ArrayList<>();
		synchronized (recentlyAddedSubscriptions) {
			recentlyAdded.addAll(recentlyAddedSubscriptions);
			recentlyAddedSubscriptions.clear();
		}

		for (AbstractAttributeSubscription subscription : recentlyAdded) {
			registerSubscription(subscription);
		}
	}

	private void registerSubscription(AbstractAttributeSubscription subscription) {
		if (subscriptionStats.get(subscription) == null) {
			sendNull(subscription);
			attributeSubscriptions.put(subscription.getMRIMetadata().getMRI(), subscription);
			recordConnected(subscription.getMRIMetadata().getMRI());
			subscriptionStats.put(subscription, new SubscriptionStats());
		}
	}

	/**
	 * This method is only to be called from the subscription thread!
	 */
	private void unregisterSubscriptionsQueuedForRemove() {
		if (Thread.currentThread() != this) {
			LOGGER.warning("unregisterQueuedSubscriptions abused in DefaultAttributeSubscriptionThread!"); //$NON-NLS-1$
		}

		List<AbstractAttributeSubscription> recentlyRemoved = new ArrayList<>();
		synchronized (recentlyRemovedSubscriptions) {
			if (recentlyRemovedSubscriptions.isEmpty()) {
				return;
			}
			recentlyRemoved.addAll(recentlyRemovedSubscriptions);
			recentlyRemovedSubscriptions.clear();
		}
		for (AbstractAttributeSubscription subscription : recentlyRemoved) {
			unregisterSubscription(subscription);
			recordDisconnected(subscription.getMRIMetadata().getMRI());
			unavailableSubscriptionsRepository.remove(subscription);
		}
	}

	private void unregisterSubscription(AbstractAttributeSubscription subscription) {
		if (subscriptionStats.get(subscription) != null) {
			attributeSubscriptions.remove(subscription.getMRIMetadata().getMRI());
			subscriptionStats.remove(subscription);
			sendNull(subscription);
		}
	}

	private void sendNull(AbstractAttributeSubscription subscription) {
		if (isSendNulls()) {
			if (getLastTimestamp() == 0) {
				// FIXME: JMC-4270 - Server time approximation is not reliable
//				setLastTimestamp(helperService.getApproximateServerTime(System.currentTimeMillis()));
				setLastTimestamp(System.currentTimeMillis());
			}
			subscription.storeAndFireEvent(
					new MRIValueEvent(subscription.getMRIMetadata().getMRI(), getLastTimestamp(), null));
		}
	}

	/**
	 * Check if we can now add back subscriptions gone bad.
	 */
	private void reregisterPreviouslyBadSubscriptions() {
		for (AbstractAttributeSubscription subscription : unavailableSubscriptionsRepository
				.getBackoffedSubscriptions()) {
			if (hasSubscriptionBecomeAvailable(subscription)) {
				recordSucceededReconnection(subscription.getMRIMetadata().getMRI());
				registerSubscription(subscription);
				unavailableSubscriptionsRepository.remove(subscription);
				subscription.fireAttributeChange(new AttributeReregisteredEvent(subscription, getLastTimestamp()));
			}
		}
	}

	private boolean hasSubscriptionBecomeAvailable(AbstractAttributeSubscription subscription) {
		return getBadAttributeError(subscription.getMRIMetadata().getMRI()) == null;
	}

	/**
	 * @param subscription
	 *            the subscription to unregister.
	 */
	public void unregisterAttributeSubscription(IMRISubscription subscription) {
		if (!(subscription instanceof AbstractAttributeSubscription)) {
			throw new IllegalArgumentException(
					"This version of the subscription service can only handle AbstractAttributeSubscriptions."); //$NON-NLS-1$
		}
		synchronized (recentlyRemovedSubscriptions) {
			recentlyRemovedSubscriptions.add((AbstractAttributeSubscription) subscription);
		}
	}

	/**
	 *
	 */
	public void shutdown() {
		isRunning = false;
		interrupt();
	}

	/**
	 * Tests each individual attribute in the list and removes any subscriptions that corresponds to
	 * attributes that it fails to access.
	 *
	 * @param attributesToFetch
	 *            A list of MRI objects to test.
	 */
	private void searchAndRemoveBadAttributes(List<MRI> attributesToFetch) {
		for (MRI mri : attributesToFetch) {
			if (!getConnectionHandle().isConnected()) {
				return;
			}
			Exception e = getBadAttributeError(mri);
			if (e != null) {
				recordConnectionLost(mri);
				removeBadAttribute(mri, e);
			} else {
				recordEventPolled(mri);
			}
		}
	}

	/**
	 * Searches through the wanted and returned attributes to find which attributes where
	 * unavailable.
	 *
	 * @param attributesToFetch
	 *            the attributes that where requested
	 * @param returnedAttributeValues
	 *            the attribute values that where returned
	 */
	private void removeBadAttributes(List<MRI> attributesToFetch, List<MRIValueEvent> returnedAttributeValues) {
		int missing = attributesToFetch.size() - returnedAttributeValues.size();
		for (MRI attributeToFetch : attributesToFetch) {
			boolean found = false;
			for (MRIValueEvent returnedAttributeValue : returnedAttributeValues) {
				if (attributeToFetch.equals(returnedAttributeValue.getMRI())) {
					found = true;
					break;
				}
			}
			if (!found) {
				recordConnectionLost(attributeToFetch);
				removeBadAttribute(attributeToFetch, null);
				missing -= 1;
				if (missing <= 0) {
					return;
				}
			}
		}
	}

	private void logError(Exception e, MRI mri) {
		LOGGER.info("The attribute " + mri //$NON-NLS-1$
				+ " could not be found in the specified JVM, and has been removed from the subscription engine!"); //$NON-NLS-1$
	}

	private void removeBadAttribute(MRI mri, Exception e) {
		AbstractAttributeSubscription subscription = getSubscription(mri);
		unregisterSubscription(subscription);
		unavailableSubscriptionsRepository.add(subscription);
		subscription.fireAttributeChange(new AttributeExceptionEvent(subscription, getLastTimestamp(), e));
		logError(e, mri);
	}

	/**
	 * Note that we can not use the subscription service for this as we may deadlock!
	 *
	 * @param mri
	 * @return
	 */
	private AbstractAttributeSubscription getSubscription(MRI mri) {
		return attributeSubscriptions.get(mri);
	}

	/**
	 * Tries retrieving an attribute and returns any exceptions caused by the operation, or null if
	 * the retrieval went ok.
	 *
	 * @param mri
	 *            The attribute to test.
	 * @return true if the attribute is bad, false if it is seemingly ok.
	 */
	private Exception getBadAttributeError(MRI mri) {
		try {
			recordTriedReconnection(mri);
			helperService.getAttributeValue(mri);
			return null;
		} catch (Exception e) {
			return e;
		}
	}

	private void dispatchEvents(List<MRIValueEvent> timestampedDataList) {
		for (MRIValueEvent event : timestampedDataList) {
			AbstractAttributeSubscription subscription = getSubscription(event.getMRI());
			setLastTimestamp(Math.max(event.getTimestamp(), getLastTimestamp()));
			recordEventRecieved(event);
			subscription.storeAndFireEvent(event);
		}
	}

	public boolean isAttributeUnavailable(MRI descriptor) {
		return unavailableSubscriptionsRepository.contains(descriptor);
	}

	public synchronized long getLastTimestamp() {
		return lastTimestamp;
	}

	public synchronized void setLastTimestamp(long timestamp) {
		lastTimestamp = timestamp;
	}

	public void setSendNulls(boolean sendNulls) {
		this.sendNulls = sendNulls;
	}

	public boolean isSendNulls() {
		return sendNulls;
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
			info.m_retainedEventCount = getRetainedLength(event.getMRI());
		}
	}

	private int getRetainedLength(MRI mri) {
		if (attributeStorageService == null) {
			attributeStorageService = getConnectionHandle().getServiceOrNull(IAttributeStorageService.class);
		}
		if (attributeStorageService != null) {
			return attributeStorageService.getRetainedLength(mri);
		}
		return 0;
	}

	private void recordEventPolled(MRI mri) {
		if (collectDebugInfo) {
			DefaultSubscriptionDebugInformation info = getDebugInformation(mri, SubscriptionState.SUBSCRIBED);
			info.m_eventCount += 1;
			info.m_retainedEventCount = getRetainedLength(mri);
		}
	}

	private void recordConnectionLost(MRI mri) {
		if (collectDebugInfo) {
			DefaultSubscriptionDebugInformation info = getDebugInformation(mri, SubscriptionState.LOST);
			info.m_connectionLostCount += 1;
		}
	}

	private void recordTriedReconnection(MRI mri) {
		if (collectDebugInfo) {
			DefaultSubscriptionDebugInformation info = getDebugInformation(mri, SubscriptionState.LOST);
			info.m_triedReconnectionCount += 1;
		}
	}

	private void recordSucceededReconnection(MRI mri) {
		if (collectDebugInfo) {
			DefaultSubscriptionDebugInformation info = getDebugInformation(mri, SubscriptionState.SUBSCRIBED);
			info.m_succeededReconnectionCount += 1;
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
