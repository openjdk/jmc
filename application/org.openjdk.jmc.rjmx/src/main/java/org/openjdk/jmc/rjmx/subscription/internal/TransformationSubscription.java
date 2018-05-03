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

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRITransformation;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;

/**
 * Will encapsulate an attribute transformation and set up subscriptions of all attributes that the
 * transformation reports as used. Will propagate updated values for the transformation.
 */
public class TransformationSubscription extends AbstractAttributeSubscription {

	private final IMRITransformation m_transformation;
	private final IMRIValueListener m_mriListener;
	private final ISubscriptionService m_subscriptionService;

	public TransformationSubscription(IConnectionHandle connectionHandle, IMRIMetadata info,
			IMRITransformation transformation) {
		super(connectionHandle, info);

		m_transformation = transformation;
		m_mriListener = createListener();
		m_subscriptionService = getConnectionHandle().getServiceOrDummy(ISubscriptionService.class);
		for (MRI mri : transformation.getAttributes()) {
			m_subscriptionService.addMRIValueListener(mri, m_mriListener);
		}
		transformation.extendMetadata(connectionHandle.getServiceOrDummy(IMRIMetadataService.class), info);
	}

	private IMRIValueListener createListener() {
		return new IMRIValueListener() {
			@Override
			public void valueChanged(MRIValueEvent event) {
				Object eventValue = m_transformation.createSubscriptionValue(event);
				if (eventValue != IMRITransformation.NO_VALUE) {
					MRIValueEvent newEvent = new MRIValueEvent(getMRIMetadata().getMRI(), event.getTimestamp(),
							eventValue);
					fireAttributeChange(newEvent);
				}
			}
		};
	}

	/**
	 * Unsubscribes to the transformation attributes.
	 */
	public void unregisterSubscription() {
		m_subscriptionService.removeMRIValueListener(m_mriListener);
	}
}
