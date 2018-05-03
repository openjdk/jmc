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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.internal.IDisposableService;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRISubscription;
import org.openjdk.jmc.rjmx.subscription.IMRITransformation;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;

/**
 * The default implementation for the attribute subscription service.
 */
public final class DefaultAttributeSubscriptionService
		implements ISubscriptionService, IDisposableService, ISubscriptionDebugService {

	// The logger.
	private final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.rjmx.subscription"); //$NON-NLS-1$

	private final IConnectionHandle handle;

	private final Map<MRI, AbstractAttributeSubscription> activeSubscriptions;
	// value is either an attribute descriptor or a set of them
	private final Map<IMRIValueListener, Object> activeListeners;
	private final DefaultAttributeSubscriptionThread subscriptionThread;
	private final DefaultNotificationSubscriptionManager notificationManager;

	public DefaultAttributeSubscriptionService(IConnectionHandle handle)
			throws ConnectionException, ServiceNotAvailableException {
		this.handle = handle;
		activeSubscriptions = new HashMap<>();
		activeListeners = new HashMap<>();
		subscriptionThread = new DefaultAttributeSubscriptionThread(handle);
		subscriptionThread.start();
		notificationManager = new DefaultNotificationSubscriptionManager(handle);
	}

	@Override
	public void collectDebugInformation(boolean collect) {
		subscriptionThread.collectDebugInformation(collect);
		notificationManager.collectDebugInformation(collect);
	}

	@Override
	public void clearDebugInformation() {
		subscriptionThread.clearDebugInformation();
		notificationManager.clearDebugInformation();
	}

	@Override
	public Collection<IMRISubscriptionDebugInformation> getDebugInformation() {
		Collection<IMRISubscriptionDebugInformation> debugInformation = new HashSet<>();
		debugInformation.addAll(subscriptionThread.getDebugInformation());
		debugInformation.addAll(notificationManager.getDebugInformation());
		return debugInformation;
	}

	/**
	 * Adds the given attribute to the given listener's set of attributes. The possible previous
	 * values are null (no previous attributes), an {@link MRI} (only one attribute) or a
	 * {@link Set} of attributes.
	 *
	 * @param listener
	 *            the listener to add attribute to.
	 * @param attributeDescriptor
	 *            the attribute to add.
	 */
	private void addAttributeToListener(IMRIValueListener listener, MRI attributeDescriptor) {
		Object listenerAttributes = activeListeners.get(listener);
		if (listenerAttributes == null) {
			activeListeners.put(listener, attributeDescriptor);
		} else if (listenerAttributes instanceof MRI) {
			Set<MRI> attributes = new HashSet<>(5);
			attributes.add((MRI) listenerAttributes);
			attributes.add(attributeDescriptor);
			activeListeners.put(listener, attributes);
		} else {
			@SuppressWarnings("unchecked")
			Set<MRI> listenerAttributesSet = (Set<MRI>) listenerAttributes;
			listenerAttributesSet.add(attributeDescriptor);
		}
	}

	/**
	 * Removes the given attribute from the given listener's set of attributes. The possible
	 * previous values are null (no previous attributes), an {@link MRI} (only one attribute) or a
	 * {@link Set} of attributes. Handles the case that given attribute is not part of the previous
	 * set.
	 *
	 * @param listener
	 *            the listener to remove attribute from.
	 * @param attributeDescriptor
	 *            the attribute to remove.
	 */
	private void removeAttributeFromListener(IMRIValueListener listener, MRI attributeDescriptor) {
		Object listenerAttributes = activeListeners.get(listener);
		if (listenerAttributes == null) {
			// nothing to do
		} else if (listenerAttributes instanceof MRI) {
			if (listenerAttributes.equals(attributeDescriptor)) {
				activeListeners.remove(listener);
			}
		} else {
			@SuppressWarnings("unchecked")
			Set<MRI> attributes = (Set<MRI>) listenerAttributes;
			attributes.remove(attributeDescriptor);
			if (attributes.isEmpty()) {
				activeListeners.remove(listener);
			}
		}
	}

	/**
	 * Returns all present attributes that the given listener is registered on.
	 *
	 * @param listener
	 *            the listener to get attributes for.
	 * @return the current set of attributes.
	 */
	private MRI[] getListenerAttributes(IMRIValueListener listener) {
		Object listenerAttributes = activeListeners.get(listener);
		if (listenerAttributes == null) {
			return new MRI[0];
		} else if (listenerAttributes instanceof MRI) {
			return new MRI[] {(MRI) listenerAttributes};
		} else {
			@SuppressWarnings("unchecked")
			Set<MRI> attributes = (Set<MRI>) listenerAttributes;
			return attributes.toArray(new MRI[attributes.size()]);
		}
	}

	/**
	 * Answers whether the given listener is registered with the given attribute.
	 *
	 * @param listener
	 *            the interesting listener.
	 * @param attributeDescriptor
	 *            the attribute to search for.
	 * @return true if the listeners is registered with the attribute, false otherwise.
	 */
	private boolean listensToAttribute(IMRIValueListener listener, MRI attributeDescriptor) {
		Object listenerAttributes = activeListeners.get(listener);
		if (listenerAttributes == null) {
			return false;
		} else if (listenerAttributes instanceof MRI) {
			return listenerAttributes.equals(attributeDescriptor);
		} else {
			@SuppressWarnings("unchecked")
			Set<MRI> listenerAttributesSet = (Set<MRI>) listenerAttributes;
			return listenerAttributesSet.contains(attributeDescriptor);
		}
	}

	/**
	 * Adds the given listener to the active subscription with given attribute (possible creating
	 * it).
	 *
	 * @param attributeDescriptor
	 *            the attribute to subscribe on.
	 * @param listener
	 *            the listener to use.
	 */
	private void addValueListenerToSubscription(MRI attributeDescriptor, IMRIValueListener listener) {
		synchronized (activeSubscriptions) {
			AbstractAttributeSubscription activeSubscription = activeSubscriptions.get(attributeDescriptor);
			if (activeSubscription == null) {
				activeSubscription = createAttributeSubscription(attributeDescriptor);
				activeSubscriptions.put(attributeDescriptor, activeSubscription);
			}
			activeSubscription.addAttributeValueListener(listener);
		}
	}

	/**
	 * Substitutes the the listeners on the subscription for given attribute.
	 *
	 * @param attributeDescriptor
	 *            the attribute for which it's subscription should be altered.
	 * @param oldListener
	 *            the listener to remove from the subscription.
	 * @param newListener
	 *            the listener to add to the subscription.
	 */
	private void substituteValueListenerForSubscription(
		MRI attributeDescriptor, IMRIValueListener oldListener, IMRIValueListener newListener) {
		AbstractAttributeSubscription activeSubscription = activeSubscriptions.get(attributeDescriptor);
		activeSubscription.substituteAttributeValueListener(oldListener, newListener);
	}

	/**
	 * Remove the given listener from the active subscription with given attribute (possible
	 * destroying it).
	 *
	 * @param attributeDescriptor
	 *            the attribute to stop subscription for.
	 * @param listener
	 *            the listener to remove.
	 */
	private void removeValueListenerFromSubscription(MRI attributeDescriptor, IMRIValueListener listener) {
		synchronized (activeSubscriptions) {
			AbstractAttributeSubscription activeSubscription = activeSubscriptions.get(attributeDescriptor);
			activeSubscription.removeAttributeValueListener(listener);
			if (!activeSubscription.hasAttributeValueListeners()) {
				destroyAttibuteSubscription(activeSubscription);
				activeSubscriptions.remove(attributeDescriptor);
			}
		}
	}

	@Override
	public void addMRIValueListener(MRI attributeDescriptor, IMRIValueListener listener) {
		synchronized (activeListeners) {
			if (!listensToAttribute(listener, attributeDescriptor)) {
				addValueListenerToSubscription(attributeDescriptor, listener);
				addAttributeToListener(listener, attributeDescriptor);
			}
		}
	}

	@Override
	public void substituteMRIValueListener(IMRIValueListener oldListener, IMRIValueListener newListener) {
		synchronized (activeListeners) {
			for (MRI attribute : getListenerAttributes(oldListener)) {
				substituteValueListenerForSubscription(attribute, oldListener, newListener);
				addAttributeToListener(newListener, attribute);
			}
			activeListeners.remove(oldListener);
		}
	}

	@Override
	public void removeMRIValueListener(IMRIValueListener listener) {
		synchronized (activeListeners) {
			for (MRI attribute : getListenerAttributes(listener)) {
				removeValueListenerFromSubscription(attribute, listener);
			}
			activeListeners.remove(listener);
		}
	}

	@Override
	public void removeMRIValueListener(MRI attributeDescriptor, IMRIValueListener listener) {
		synchronized (activeListeners) {
			if (listensToAttribute(listener, attributeDescriptor)) {
				removeValueListenerFromSubscription(attributeDescriptor, listener);
				removeAttributeFromListener(listener, attributeDescriptor);
			}
		}
	}

	private void destroyAttibuteSubscription(AbstractAttributeSubscription subscription) {
		switch (subscription.getMRIMetadata().getMRI().getType()) {
		case ATTRIBUTE:
			subscriptionThread.unregisterAttributeSubscription(subscription);
			break;
		case NOTIFICATION:
			notificationManager.unregisterNotificationAttributeSubscription(subscription);
			break;
		case TRANSFORMATION:
			if (subscription instanceof TransformationSubscription) {
				((TransformationSubscription) subscription).unregisterSubscription();
			}
			break;
		}
		if (subscription instanceof IMRIValueListener) {
			removeMRIValueListener((IMRIValueListener) subscription);
		}

	}

	private AbstractAttributeSubscription createAttributeSubscription(MRI attributeDescriptor) {
		if (attributeDescriptor == null) {
			throw new IllegalArgumentException("Can not subscribe to null!"); //$NON-NLS-1$
		}
		IMRIMetadata attributeInfo = handle.getServiceOrDummy(IMRIMetadataService.class)
				.getMetadata(attributeDescriptor);
		if (attributeInfo == null) {
			throw new IllegalArgumentException("Tried to get an AttributeSubscription for null!"); //$NON-NLS-1$
		}

		LOGGER.finest("Getting subscription for " + attributeDescriptor); //$NON-NLS-1$

		return createAttributeSubscriptionInternal(attributeInfo);
	}

	private AbstractAttributeSubscription createAttributeSubscriptionInternal(IMRIMetadata info) {
		if (info.getMRI().getObjectName() == null) {
			throw new IllegalArgumentException(
					"The attribute name associated with the attribute info must be resolved before being used to create subscriptions."); //$NON-NLS-1$
		}
		if (info.getMRI().getType() == Type.TRANSFORMATION) {
			IMRITransformation transformation = MRITransformationToolkit.createTransformation(info.getMRI());
			return new TransformationSubscription(handle, info, transformation);
		} else {
			AbstractAttributeSubscription subscription = new DefaultAttributeSubscription(handle, info);
			registerWithSubscriptionManagers(handle, subscription);
			return subscription;
		}
	}

	/**
	 * Checks if we're connected. If we are, we will check if there is a subscription thread. If
	 * there is one, the subscription will be registered with it, else a subscription thread will be
	 * created. If it is a notification based subscription, it will be registered with the
	 * notification manager instead of the subscription thread.
	 *
	 * @param connectionHandle
	 * @param subscription
	 */
	private void registerWithSubscriptionManagers(
		IConnectionHandle connectionHandle, AbstractAttributeSubscription subscription) {
		setUpPolicy(connectionHandle, subscription);
		if (!connectionHandle.isConnected()) {
			return;
		}
		if (subscription.getMRIMetadata().getMRI().getType() == Type.NOTIFICATION) {
			notificationManager.registerNotificationAttributeSubscription(subscription);
		} else {
			subscriptionThread.registerAttributeSubscription(subscription);
		}
	}

	private void setUpPolicy(IConnectionHandle connectionHandle, AbstractAttributeSubscription subscription) {
		subscription.setUpdatePolicy(
				UpdatePolicyToolkit.getUpdatePolicy(connectionHandle, subscription.getMRIMetadata().getMRI()));
	}

	/**
	 * Will shut down all subscription threads and clear all subscriptions for the the specified
	 * connector model, and stop listening on changes in the connector model.
	 * <p>
	 * No more events will ever be sent from the subscriptions previously created and associated
	 * with this service.
	 */
	@Override
	public synchronized void dispose() {
		subscriptionThread.shutdown();
		notificationManager.shutdown();
		activeSubscriptions.clear();
		activeListeners.clear();
	}

	@Override
	public IMRISubscription getMRISubscription(MRI attributeDescriptor) {
		// FIXME: Can be optimized if necessary.
		synchronized (activeSubscriptions) {
			return activeSubscriptions.get(attributeDescriptor);
		}
	}

	@Override
	public MRIValueEvent getLastMRIValueEvent(MRI attributeDescriptor) {
		IMRISubscription subscription = getMRISubscription(attributeDescriptor);
		if (subscription != null) {
			return subscription.getLastMRIValueEvent();
		} else {
			return null;
		}
	}

	@Override
	public boolean isMRIUnavailable(MRI attributeDescriptor) {
		return subscriptionThread.isAttributeUnavailable(attributeDescriptor);
	}
}
