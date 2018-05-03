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
package org.openjdk.jmc.rjmx.test.subscription;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import org.openjdk.jmc.rjmx.subscription.IMRISubscription;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;
import org.openjdk.jmc.rjmx.test.testutil.TestToolkit;

/**
 * Tests the automatic (synthetic) subscriptions on composite data.
 */
public class CompositeDataSubscriptionTest extends RjmxTestCase implements IMRIValueListener {
	private final static int SLEEP_TIME = 10000;

	/**
	 * @see org.openjdk.jmc.rjmx.subscription.IMRIValueListener#valueChanged(org.openjdk.jmc.rjmx.subscription.MRIValueEvent)
	 */
	@Override
	public void valueChanged(MRIValueEvent event) {
		TestToolkit.println(event);
		synchronized (this) {
			notifyAll();
		}
	}

	@Test
	public void testCompositeDataSubscription() {
		try {
			// Starting up a subscription on a one shot attribute.
			MRI attributeDescriptor = new MRI(Type.ATTRIBUTE, "java.lang:type=Memory", //$NON-NLS-1$
					"HeapMemoryUsage/used"); //$NON-NLS-1$
			getAttributeSubscriptionService().addMRIValueListener(attributeDescriptor, this);
			IMRISubscription subscription = getAttributeSubscriptionService().getMRISubscription(attributeDescriptor);

			assertNotNull(subscription);

			synchronized (this) {
				this.wait(SLEEP_TIME);
			}
			assertNotNull(getAttributeSubscriptionService().getLastMRIValueEvent(attributeDescriptor));
			getAttributeSubscriptionService().removeMRIValueListener(this);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
