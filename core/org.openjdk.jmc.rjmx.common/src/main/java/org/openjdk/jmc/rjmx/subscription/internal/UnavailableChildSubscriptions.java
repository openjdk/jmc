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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.management.openmbean.CompositeData;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.IUpdatePolicy;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;

/**
 * When one or more subscriptions try to retrieve an non-existing composite child value we first
 * subscribe on the actual attribute and wait for a concrete value.
 */
class UnavailableChildSubscriptions {
	private final ISubscriptionService m_subscriptionService;
	private final MRI m_parentMRI;
	private final IMRIValueListener m_parentValueListener;
	private final UnavailableSubscriptionsRepository m_repository;
	private final Set<AbstractAttributeSubscription> m_unavailableChildSubscriptions = new HashSet<>();

	public UnavailableChildSubscriptions(IConnectionHandle connectionHandle, MRI parentMRI,
			UnavailableSubscriptionsRepository repository) {
		m_subscriptionService = connectionHandle.getServiceOrNull(ISubscriptionService.class);
		m_parentMRI = parentMRI;
		m_repository = repository;
		m_parentValueListener = createValueListener();
		if (m_subscriptionService != null) {
			m_subscriptionService.addMRIValueListener(m_parentMRI, m_parentValueListener);
		}
	}

	public MRI getParentMRI() {
		return m_parentMRI;
	}

	public void dispose() {
		if (m_subscriptionService != null) {
			m_subscriptionService.removeMRIValueListener(m_parentValueListener);
		}
	}

	public boolean addChildSubscription(AbstractAttributeSubscription subscription) {
		if (!subscription.getMRIMetadata().getMRI().toString().startsWith(m_parentMRI.toString())) {
			throw new IllegalArgumentException(
					"Child " + subscription.getMRIMetadata().getMRI() + " is not child of " + m_parentMRI); //$NON-NLS-1$ //$NON-NLS-2$
		}
		boolean result = m_unavailableChildSubscriptions.add(subscription);
		adjustUpdatePolicy(subscription);
		return result;
	}

	private void adjustUpdatePolicy(AbstractAttributeSubscription subscription) {
		IUpdatePolicy updatePolicy = UpdatePolicyToolkit.getUpdatePolicy(subscription.getMRIMetadata());
		if (updatePolicy instanceof IIntervalUpdatePolicy && m_subscriptionService != null) {
			int updateInterval = ((IIntervalUpdatePolicy) updatePolicy).getIntervalTime();
			IUpdatePolicy parentPolicy = m_subscriptionService.getMRISubscription(m_parentMRI).getUpdatePolicy();
			if (parentPolicy instanceof IIntervalUpdatePolicy
					&& updateInterval < ((IIntervalUpdatePolicy) parentPolicy).getIntervalTime()) {
				m_subscriptionService.getMRISubscription(m_parentMRI)
						.setUpdatePolicy(SimpleUpdatePolicy.newPolicy(updateInterval));
			}
		}
	}

	public Collection<AbstractAttributeSubscription> getChildSubscriptions() {
		return m_unavailableChildSubscriptions;
	}

	private IMRIValueListener createValueListener() {
		return new IMRIValueListener() {
			@Override
			public void valueChanged(MRIValueEvent event) {
				if (event.getValue() instanceof CompositeData) {
					// FIXME: change to a better test of containment?
					dispose();
					m_repository.movePossibleChildSubscriptions(UnavailableChildSubscriptions.this);
				}
			}

		};
	}

	public static boolean isCompositeChildSubscription(AbstractAttributeSubscription subscription) {
		MRI[] parents = subscription.getMRIMetadata().getMRI().getParentMRIs();
		return parents.length > 0;
	}

	public static MRI getParentMRI(AbstractAttributeSubscription subscription) {
		return subscription.getMRIMetadata().getMRI().getParentMRIs()[0];
	}

}
