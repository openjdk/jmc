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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.ObjectName;

import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.subscription.IMBeanServerChangeListener;
import org.openjdk.jmc.rjmx.subscription.MRI;

/**
 * Class responsible for keeping track of subscriptions that for one reason or another have become
 * unavailable. This might be because the MBean they belong to has been unregistered or that an
 * attribute is simply not available yet. A typical example of that are composite children of the
 * GarbageCollectorMXBean.getLastGcInfo() attribute.
 */
public class UnavailableSubscriptionsRepository {

	private static class BackoffTimeInformation {
		private long backoffTime;
		private long lastTestTime;

		public BackoffTimeInformation() {
			this(1000, System.currentTimeMillis());
		}

		public BackoffTimeInformation(long backoff, long lastTest) {
			backoffTime = backoff;
			lastTestTime = lastTest;
		}
	}

	private final IConnectionHandle m_connectionHandle;
	private final IMBeanHelperService m_helperService;
	private final IMBeanServerChangeListener m_mbeanServerChangeListener;
	private final Set<ObjectName> m_unregisteredMBeans = new HashSet<>();

	private final Map<ObjectName, Set<AbstractAttributeSubscription>> m_unregisteredSubscriptions = new HashMap<>();
	private final Map<MRI, UnavailableChildSubscriptions> m_unavailableChildSubscriptions = new HashMap<>();
	private final Map<AbstractAttributeSubscription, BackoffTimeInformation> m_possibleSubscriptions = new HashMap<>();

	public UnavailableSubscriptionsRepository(IConnectionHandle connectionHandle)
			throws ConnectionException, ServiceNotAvailableException {
		m_connectionHandle = connectionHandle;
		m_helperService = connectionHandle.getServiceOrThrow(IMBeanHelperService.class);
		m_mbeanServerChangeListener = createMBeanServerChangeListener();
		getMBeanHelperService().addMBeanServerChangeListener(m_mbeanServerChangeListener);
	}

	public void dispose() {
		getMBeanHelperService().removeMBeanServerChangeListener(m_mbeanServerChangeListener);
		m_unregisteredMBeans.clear();
	}

	private IMBeanHelperService getMBeanHelperService() {
		return m_helperService;
	}

	private IMBeanServerChangeListener createMBeanServerChangeListener() {
		return new IMBeanServerChangeListener() {
			@Override
			public void mbeanUnregistered(ObjectName mbean) {
				if (unregisterMBean(mbean)) {
					moveUnregisteredMBeanSubscriptions(mbean);
				}
			}

			private boolean unregisterMBean(ObjectName mbean) {
				synchronized (m_unregisteredMBeans) {
					return m_unregisteredMBeans.add(mbean);
				}
			}

			@Override
			public void mbeanRegistered(ObjectName mbean) {
				if (registerMBean(mbean)) {
					moveRegisteredMBeanSubscriptions(mbean);
				}
			}

			private boolean registerMBean(ObjectName mbean) {
				synchronized (m_unregisteredMBeans) {
					return m_unregisteredMBeans.remove(mbean);
				}
			}
		};
	}

	synchronized void movePossibleChildSubscriptions(UnavailableChildSubscriptions possibleChildSubscriptions) {
		m_unavailableChildSubscriptions.remove(possibleChildSubscriptions.getParentMRI());
		for (AbstractAttributeSubscription subscription : possibleChildSubscriptions.getChildSubscriptions()) {
			m_possibleSubscriptions.put(subscription, new BackoffTimeInformation());
		}

	}

	private synchronized void moveRegisteredMBeanSubscriptions(ObjectName mbean) {
		Set<AbstractAttributeSubscription> subscriptions = m_unregisteredSubscriptions.remove(mbean);
		if (subscriptions != null) {
			for (AbstractAttributeSubscription subscription : subscriptions) {
				m_possibleSubscriptions.put(subscription, new BackoffTimeInformation());
			}
		}
	}

	private synchronized void moveUnregisteredMBeanSubscriptions(ObjectName mbean) {
		for (AbstractAttributeSubscription subscription : getPossibleSubscriptions()) {
			if (subscription.getMRIMetadata().getMRI().getObjectName().equals(mbean)) {
				m_possibleSubscriptions.remove(subscription);
				getUnregisteredSubscriptions(mbean).add(subscription);
			}
		}
	}

	private Set<AbstractAttributeSubscription> getUnregisteredSubscriptions(ObjectName mbean) {
		Set<AbstractAttributeSubscription> subscriptions = m_unregisteredSubscriptions.get(mbean);
		if (subscriptions == null) {
			subscriptions = new HashSet<>();
			m_unregisteredSubscriptions.put(mbean, subscriptions);
		}
		return subscriptions;
	}

	private Collection<AbstractAttributeSubscription> getPossibleSubscriptions() {
		return new ArrayList<>(m_possibleSubscriptions.keySet());
	}

	public synchronized Collection<AbstractAttributeSubscription> getBackoffedSubscriptions() {
		Collection<AbstractAttributeSubscription> subscriptions = new ArrayList<>();
		long currentTime = System.currentTimeMillis();
		for (Entry<AbstractAttributeSubscription, BackoffTimeInformation> entry : m_possibleSubscriptions.entrySet()) {
			BackoffTimeInformation info = entry.getValue();
			if (info.lastTestTime + info.backoffTime < currentTime) {
				info.lastTestTime = currentTime;
				info.backoffTime *= 2;
				subscriptions.add(entry.getKey());
			}
		}
		return subscriptions;
	}

	private synchronized Collection<AbstractAttributeSubscription> getUnregisteredSubscriptions() {
		Collection<AbstractAttributeSubscription> subscriptions = new ArrayList<>();
		for (Set<AbstractAttributeSubscription> mbeanSubscriptions : m_unregisteredSubscriptions.values()) {
			subscriptions.addAll(mbeanSubscriptions);
		}
		return subscriptions;
	}

	public synchronized Collection<AbstractAttributeSubscription> getAllSubscriptions() {
		Collection<AbstractAttributeSubscription> subscriptions = getUnregisteredSubscriptions();
		subscriptions.addAll(getUnavailableChildSubscriptions());
		subscriptions.addAll(getPossibleSubscriptions());
		return subscriptions;
	}

	private synchronized Collection<AbstractAttributeSubscription> getUnavailableChildSubscriptions() {
		Collection<AbstractAttributeSubscription> subscriptions = new ArrayList<>();
		for (UnavailableChildSubscriptions unavailableChildSubscriptions : m_unavailableChildSubscriptions.values()) {
			subscriptions.addAll(unavailableChildSubscriptions.getChildSubscriptions());
		}
		return subscriptions;
	}

	public synchronized boolean add(AbstractAttributeSubscription subscription) {
		ObjectName mbean = subscription.getMRIMetadata().getMRI().getObjectName();
		if (isMBeanUnregistered(mbean)) {
			return getUnregisteredSubscriptions(mbean).add(subscription);
		} else if (UnavailableChildSubscriptions.isCompositeChildSubscription(subscription)) {
			return getUnavailableSubscriptions(subscription).addChildSubscription(subscription);
		} else {
			return m_possibleSubscriptions.put(subscription, new BackoffTimeInformation()) == null;
		}
	}

	private boolean isMBeanUnregistered(ObjectName mbean) {
		synchronized (m_unregisteredMBeans) {
			return m_unregisteredMBeans.contains(mbean);
		}
	}

	private UnavailableChildSubscriptions getUnavailableSubscriptions(AbstractAttributeSubscription subscription) {
		MRI parentMRI = UnavailableChildSubscriptions.getParentMRI(subscription);
		UnavailableChildSubscriptions unavailableChildSubscriptions = m_unavailableChildSubscriptions.get(parentMRI);
		if (unavailableChildSubscriptions == null) {
			unavailableChildSubscriptions = new UnavailableChildSubscriptions(m_connectionHandle, parentMRI, this);
			m_unavailableChildSubscriptions.put(parentMRI, unavailableChildSubscriptions);
		}
		return unavailableChildSubscriptions;
	}

	public synchronized boolean remove(AbstractAttributeSubscription subscription) {
		return m_possibleSubscriptions.remove(subscription) != null || hasUnavilableChildSubscription(subscription)
				|| getUnregisteredSubscriptions(subscription.getMRIMetadata().getMRI().getObjectName())
						.remove(subscription);
	}

	private boolean hasUnavilableChildSubscription(AbstractAttributeSubscription subscription) {
		if (UnavailableChildSubscriptions.isCompositeChildSubscription(subscription)) {
			MRI parentMRI = UnavailableChildSubscriptions.getParentMRI(subscription);
			UnavailableChildSubscriptions unavailableChildSubscriptions = m_unavailableChildSubscriptions
					.get(parentMRI);
			if (unavailableChildSubscriptions != null) {
				return removeSubscription(unavailableChildSubscriptions, subscription, parentMRI);
			}
		}
		return false;
	}

	private boolean removeSubscription(
		UnavailableChildSubscriptions unavailableChildSubscriptions, AbstractAttributeSubscription subscription,
		MRI parentMRI) {
		boolean result = unavailableChildSubscriptions.getChildSubscriptions().remove(subscription);
		if (unavailableChildSubscriptions.getChildSubscriptions().size() == 0) {
			unavailableChildSubscriptions.dispose();
			m_unavailableChildSubscriptions.remove(parentMRI);
		}
		return result;
	}

	public synchronized boolean contains(MRI mri) {
		for (AbstractAttributeSubscription subscription : getAllSubscriptions()) {
			if (mri.equals(subscription.getMRIMetadata().getMRI())) {
				return true;
			}
		}
		return false;
	}
}
