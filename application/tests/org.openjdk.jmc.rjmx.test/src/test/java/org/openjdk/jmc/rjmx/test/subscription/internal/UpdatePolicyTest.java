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
package org.openjdk.jmc.rjmx.test.subscription.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMRISubscription;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.IUpdatePolicy;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.subscription.PolicyFactory;
import org.openjdk.jmc.rjmx.subscription.internal.IIntervalUpdatePolicy;
import org.openjdk.jmc.rjmx.subscription.internal.SimpleUpdatePolicy;
import org.openjdk.jmc.rjmx.subscription.internal.UpdatePolicyToolkit;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;
import org.openjdk.jmc.rjmx.test.testutil.TestToolkit;

public class UpdatePolicyTest extends RjmxTestCase implements IMRIValueListener {

	private static int TEST_TIMEOUT_TIME = 30000;
	private int m_counter = 0;

	@Test
	public void testUpdatePolicyLookup() throws Exception {
		helpUpdatePolicyLookup(getExistingAttribute());
	}

	@Test
	public void testNonExistingUpdatePolicyLookup() throws Exception {
		helpUpdatePolicyLookup(getNonExistingAttribute());
	}

	private void helpUpdatePolicyLookup(MRI mri) throws Exception {
		IUpdatePolicy policy = UpdatePolicyToolkit.getUpdatePolicy(getConnectionHandle(), mri);
		assertNotNull("Policy is null!", policy);
	}

	@Test
	public void testChangeUpdatePolicy() throws Exception {
		helpChangeUpdatePolicy(getExistingAttribute());
	}

	@Test
	public void testChangeNonExistsingUpdatePolicy() throws Exception {
		helpChangeUpdatePolicy(getNonExistingAttribute());
	}

	private void helpChangeUpdatePolicy(MRI mri) throws Exception {
		IUpdatePolicy oldPolicy = UpdatePolicyToolkit.getUpdatePolicy(getConnectionHandle(), mri);
		assertNotNull(oldPolicy);
		int oldUpdateTime = getUpdateTime(getConnectionHandle(), mri);
		int newUpdateTime = oldUpdateTime * 2;
		UpdatePolicyToolkit.setUpdatePolicy(getConnectionHandle(), mri,
				PolicyFactory.createSimpleUpdatePolicy(newUpdateTime));
		IUpdatePolicy newPolicy = UpdatePolicyToolkit.getUpdatePolicy(getConnectionHandle(), mri);
		assertNotNull(newPolicy);
		assertTrue(newPolicy != oldPolicy);
		assertTrue(newPolicy instanceof SimpleUpdatePolicy);
		assertTrue(getUpdateTime(getConnectionHandle(), mri) == newUpdateTime);
		UpdatePolicyToolkit.setUpdatePolicy(getConnectionHandle(), mri, oldPolicy);
		assertTrue(getUpdateTime(getConnectionHandle(), mri) == oldUpdateTime);
	}

	@Test
	public void testChangeSubscribedUpdatePolicy() throws Exception {
		try {
			getAttributeSubscriptionService().addMRIValueListener(getExistingAttribute(), this);
			IUpdatePolicy oldUpdatePolicy = UpdatePolicyToolkit.getUpdatePolicy(getConnectionHandle(),
					getExistingAttribute());
			int oldUpdateTime = getUpdateTime(getConnectionHandle(), getExistingAttribute());
			IMRISubscription subscription = getAttributeSubscriptionService()
					.getMRISubscription(getExistingAttribute());
			if (subscription.getUpdatePolicy() instanceof IIntervalUpdatePolicy) {
				IIntervalUpdatePolicy subscriptionPolicy = (IIntervalUpdatePolicy) subscription.getUpdatePolicy();
				int oldSubscriptionUpdateTime = subscriptionPolicy.getIntervalTime();
				assertTrue(oldUpdateTime == oldSubscriptionUpdateTime);
			}
			synchronized (this) {
				int oldCounter = m_counter;
				for (int i = 0; i < 2; i++) {
					this.wait(TEST_TIMEOUT_TIME);
				}
				assertTrue(m_counter > oldCounter);
			}
			UpdatePolicyToolkit.setUpdatePolicy(getConnectionHandle(), getExistingAttribute(),
					PolicyFactory.createSimpleUpdatePolicy(oldUpdateTime * 10));
			int newUpdateTime = getUpdateTime(getConnectionHandle(), getExistingAttribute());
			assertTrue(oldUpdateTime * 10 == newUpdateTime);
			assertTrue(subscription.getUpdatePolicy() instanceof IIntervalUpdatePolicy);
			IIntervalUpdatePolicy subscriptionPolicy = (IIntervalUpdatePolicy) subscription.getUpdatePolicy();
			int newSubscriptionUpdateTime = subscriptionPolicy.getIntervalTime();
			assertTrue(newUpdateTime == newSubscriptionUpdateTime);
			synchronized (this) {
				int oldCounter = m_counter;
				for (int i = 0; i < 2; i++) {
					this.wait(TEST_TIMEOUT_TIME);
				}
				assertTrue(m_counter > oldCounter);
			}
			UpdatePolicyToolkit.setUpdatePolicy(getConnectionHandle(), getExistingAttribute(), oldUpdatePolicy);
		} finally {
			getAttributeSubscriptionService().removeMRIValueListener(this);
		}
	}

	@Test
	public void testDifferentSubscriptionTimes() throws Exception {
		try {
			getAttributeSubscriptionService().addMRIValueListener(getExistingAttribute(), this);
			IUpdatePolicy oldUpdatePolicy = UpdatePolicyToolkit.getUpdatePolicy(getConnectionHandle(),
					getExistingAttribute());
//			System.out.println("[start] - " + System.currentTimeMillis());
			for (int time = 500; time <= 4000; time *= 2) {
				UpdatePolicyToolkit.setUpdatePolicy(getConnectionHandle(), getExistingAttribute(),
						PolicyFactory.createSimpleUpdatePolicy(time));
				int newUpdateTime = getUpdateTime(getConnectionHandle(), getExistingAttribute());
				assertTrue(newUpdateTime == time);
				IIntervalUpdatePolicy subscriptionPolicy = (IIntervalUpdatePolicy) getAttributeSubscriptionService()
						.getMRISubscription(getExistingAttribute()).getUpdatePolicy();
				int newSubscriptionUpdateTime = subscriptionPolicy.getIntervalTime();
				assertTrue(newUpdateTime == newSubscriptionUpdateTime);
				synchronized (this) {
					int oldCounter = m_counter;
					for (int i = 0; i < 3; i++) {
						this.wait(time * 2);
//						System.out.println("[" + time + "-" + i + "] - " + System.currentTimeMillis());
					}
					assertTrue(m_counter > oldCounter);
				}
			}
			UpdatePolicyToolkit.setUpdatePolicy(getConnectionHandle(), getExistingAttribute(), oldUpdatePolicy);
		} finally {
			getAttributeSubscriptionService().removeMRIValueListener(this);
		}
	}

	private MRI getExistingAttribute() {
		return new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "UsedPhysicalMemorySize");
	}

	private MRI getNonExistingAttribute() {
		return new MRI(Type.ATTRIBUTE, "this.could.possible.not:really=exist,as=an", "attribute");
	}

	@Override
	public void valueChanged(MRIValueEvent event) {
		synchronized (this) {
			TestToolkit.println(event);
			if (event.getValue() != null) {
				m_counter += 1;
				notifyAll();
			}
		}
	}

	private static int getUpdateTime(IConnectionHandle handle, MRI attributeDescriptor) {
		return ((IIntervalUpdatePolicy) UpdatePolicyToolkit.getUpdatePolicy(handle, attributeDescriptor))
				.getIntervalTime();
	}
}
