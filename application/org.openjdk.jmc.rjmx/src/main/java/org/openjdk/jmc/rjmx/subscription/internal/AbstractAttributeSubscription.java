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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.common.IPredicate;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRISubscription;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.IUpdatePolicy;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;

/**
 * Default implementation for AttributeSubscriptions.
 */
public abstract class AbstractAttributeSubscription implements IMRISubscription {
	private static final Logger LOGGER = Logger.getLogger("org.openjdk.jmc.rjmx.subscription"); //$NON-NLS-1$

	private final IConnectionHandle m_connectionHandle;
	private final Set<IMRIValueListener> m_attributeListenerList = Collections
			.synchronizedSet(new HashSet<IMRIValueListener>(1));
	private final IMRIMetadata m_attributeInfo;
	private MRIValueEvent m_lastEvent;
	private IUpdatePolicy m_updatePolicy;
	private final IPredicate<Object> m_valueFilter;

	/**
	 * Constructor for AbstractAttribute.
	 *
	 * @param connectionHandle
	 *            the ConnectorModel to bind to. Needs to have the {@link IMBeanHelperService} and
	 *            {@link IMRIMetadataService} service available.
	 * @param info
	 *            the {@link IMRIMetadata} for the subscription attribute, may not be null!
	 * @throws IllegalAccessException
	 */
	public AbstractAttributeSubscription(IConnectionHandle connectionHandle, IMRIMetadata info) {
		m_connectionHandle = connectionHandle;
		if (info == null || info.getMRI().getObjectName() == null) {
			throw new IllegalArgumentException("Subscriptions may not be created from attribute name references."); //$NON-NLS-1$
		} else if (!m_connectionHandle.hasService(IMRIMetadataService.class)) {
			throw new IllegalArgumentException(
					"Connection handle must have the IMBeanHelperService for subscriptions to work!"); //$NON-NLS-1$
		} else {
			m_attributeInfo = info;
		}
		m_valueFilter = getValueFilter(info);
	}

	/**
	 * Adds the specified listener to the list of objects to be notified whenever a change has
	 * occurred.
	 *
	 * @param listener
	 *            the listener interested in change information.
	 */
	public void addAttributeValueListener(IMRIValueListener listener) {
		if (listener == null) {
			throw new NullPointerException("Listener may not be null!"); //$NON-NLS-1$
		}
		if (m_attributeListenerList.contains(listener)) {
			throw new IllegalArgumentException("This listener has already been added!"); //$NON-NLS-1$
		}
		m_attributeListenerList.add(listener);
	}

	/**
	 * Removes the old listener and adds the new.
	 *
	 * @param oldListener
	 *            the old listener already subscribing.
	 * @param newListener
	 *            the new listener that should subscribe instead.
	 */
	public void substituteAttributeValueListener(IMRIValueListener oldListener, IMRIValueListener newListener) {
		if (oldListener == null || newListener == null) {
			throw new NullPointerException("Listeners may not be null!"); //$NON-NLS-1$
		}
		if (oldListener != newListener) {
			synchronized (m_attributeListenerList) {
				removeAttributeValueListener(oldListener);
				addAttributeValueListener(newListener);
			}
		}
	}

	/**
	 * Removes the specified listener. Change events will no longer be transmitted to the specified
	 * listener after this call returns.
	 *
	 * @param listener
	 *            the listener to be removed.
	 */
	public void removeAttributeValueListener(IMRIValueListener listener) {
		m_attributeListenerList.remove(listener);
	}

	/**
	 * Return whether the subscription has any value listeners or not.
	 *
	 * @return <tt>true</tt> if subscription has listeners, <tt>false</tt> otherwise.
	 */
	public boolean hasAttributeValueListeners() {
		return m_attributeListenerList.size() > 0;
	}

	/**
	 * Fires an attribute value change from this attribute.
	 *
	 * @param attributeEvent
	 *            the attribute value change to fire.
	 */
	protected void fireAttributeChange(MRIValueEvent attributeEvent) {
		if (attributeEvent == null) {
			return;
		}

		IMRIValueListener[] tmpListeners;
		synchronized (m_attributeListenerList) {
			tmpListeners = m_attributeListenerList.toArray(new IMRIValueListener[m_attributeListenerList.size()]);
		}
		for (IMRIValueListener l : tmpListeners) {
			l.valueChanged(attributeEvent);
		}

	}

	@Override
	public IConnectionHandle getConnectionHandle() {
		return m_connectionHandle;
	}

	@Override
	public MRIValueEvent getLastMRIValueEvent() {
		return m_lastEvent;
	}

	/**
	 * This must be called in subclasses to store and fire a newly created event.
	 *
	 * @param event
	 *            the newly created event.
	 */
	protected void storeAndFireEvent(MRIValueEvent event) {
		if (m_valueFilter != null && m_valueFilter.evaluate(event.getValue())) {
			LOGGER.log(Level.INFO, "Subscription filtered out value: " + event.getValue()); //$NON-NLS-1$
			return;
		}
		if ((m_lastEvent != null) && (event.getTimestamp() < m_lastEvent.getTimestamp())) {
			LOGGER.log(Level.INFO, "Subscription dropped attribute event because timestamp was older than last event."); //$NON-NLS-1$
			return;
		}
		m_lastEvent = event;
		fireAttributeChange(event);
	}

	@Override
	public IMRIMetadata getMRIMetadata() {
		return m_attributeInfo;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AbstractAttributeSubscription) {
			AbstractAttributeSubscription other = (AbstractAttributeSubscription) obj;
			return m_connectionHandle.getServerDescriptor().getGUID()
					.equals(other.m_connectionHandle.getServerDescriptor().getGUID())
					&& getMRIMetadata().equals(other.getMRIMetadata());
		}
		return false;
	}

	// FIXME: Calculation speed could be improved.
	@Override
	public int hashCode() {
		if (cachedHashCode == null) {
			cachedHashCode = (m_connectionHandle.getServerDescriptor().getGUID()
					+ getMRIMetadata().getMRI().getQualifiedName()).hashCode();
		}
		return cachedHashCode.intValue();
	}

	private Integer cachedHashCode;

	@Override
	public IUpdatePolicy getUpdatePolicy() {
		return m_updatePolicy;
	}

	@Override
	public void setUpdatePolicy(IUpdatePolicy updatePolicy) {
		m_updatePolicy = updatePolicy;
	}

	@Override
	public String toString() {
		return getClass().getName() + '[' + getMRIMetadata() + ']';
	}

	private static IPredicate<Object> getValueFilter(IMRIMetadata metadata) {
		// FIXME: We hard code the filters for now, but this should be handled along with content types
		String mri = metadata.getMRI().getQualifiedName();
		if ("attribute://java.lang:type=OperatingSystem/ProcessCpuLoad".equals(mri) //$NON-NLS-1$
				|| "attribute://java.lang:type=OperatingSystem/SystemCpuLoad".equals(mri) //$NON-NLS-1$
				|| "attribute://java.lang:type=OperatingSystem/SystemLoadAverage".equals(mri)) { //$NON-NLS-1$
			return new IPredicate<Object>() {

				@Override
				public boolean evaluate(Object value) {
					return !(value instanceof Number) || ((Number) value).doubleValue() < 0;
				}
			};
		}
		return null;
	}
}
