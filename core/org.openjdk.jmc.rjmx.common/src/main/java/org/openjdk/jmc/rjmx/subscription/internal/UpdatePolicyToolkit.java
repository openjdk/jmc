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

import java.util.logging.Level;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.preferences.PreferencesKeys;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataProvider;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRISubscription;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.IUpdatePolicy;
import org.openjdk.jmc.rjmx.subscription.MRI;

public final class UpdatePolicyToolkit {

	private static final String DEFAULT_UPDATE_INTERVAL = "defaultUpdateInterval"; //$NON-NLS-1$

	private UpdatePolicyToolkit() throws InstantiationException {
		throw new InstantiationException("Should not be instantiated!"); //$NON-NLS-1$
	}

	public static int getDefaultUpdateInterval() {
		return RJMXPlugin.getDefault().getRJMXPreferences().getInt(PreferencesKeys.PROPERTY_UPDATE_INTERVAL,
				PreferencesKeys.DEFAULT_UPDATE_INTERVAL);
	}

	/**
	 * Sets the {@link IUpdatePolicy} for an {@link MRI} .
	 *
	 * @param handle
	 *            the connection handle for which to set to policy.
	 * @param attributeDescriptor
	 *            the descriptor for which to set the policy.
	 * @param policy
	 *            the policy to set.
	 * @throws UnsupportedOperationException
	 *             if the policy is unknown to the system.
	 */
	public static void setUpdatePolicy(IConnectionHandle handle, MRI attributeDescriptor, IUpdatePolicy policy) {
		IMRIMetadataService metadataService = getMetadataService(handle);
		if (metadataService == null) {
			return;
		}
		if (policy instanceof OneShotUpdatePolicy) {
			metadataService.setMetadata(attributeDescriptor, IMRIMetadataProvider.KEY_UPDATE_TIME, Integer.toString(0));
		} else if (policy instanceof SimpleUpdatePolicy) {
			SimpleUpdatePolicy s = (SimpleUpdatePolicy) policy;
			metadataService.setMetadata(attributeDescriptor, IMRIMetadataProvider.KEY_UPDATE_TIME,
					Integer.toString(s.getIntervalTime()));
		} else if (policy instanceof DefaultUpdatePolicy) {
			metadataService.setMetadata(attributeDescriptor, IMRIMetadataProvider.KEY_UPDATE_TIME,
					DEFAULT_UPDATE_INTERVAL);
		} else {
			throw new UnsupportedOperationException(policy.getClass() + "is not supported!"); //$NON-NLS-1$
		}
		updateExistingSubscription(handle, attributeDescriptor);
	}

	private static void updateExistingSubscription(IConnectionHandle handle, MRI attributeDescriptor) {
		ISubscriptionService service = handle.getServiceOrNull(ISubscriptionService.class);
		if (service == null) {
			return;
		}
		IMRISubscription attributeSubscription = service.getMRISubscription(attributeDescriptor);
		if (attributeSubscription != null) {
			attributeSubscription.setUpdatePolicy(getUpdatePolicy(handle, attributeDescriptor));
		}
	}

	/**
	 * Returns the update policy for an {@link MRI}. If no policy is set the default policy will be
	 * returned.
	 *
	 * @param handle
	 *            the handle for which to get the policy.
	 * @param attributeDescriptor
	 *            the descriptor for which to set the policy.
	 * @return an {@link IUpdatePolicy}.
	 */
	public static IUpdatePolicy getUpdatePolicy(IConnectionHandle handle, MRI attributeDescriptor) {
		IMRIMetadataService metadataService = getMetadataService(handle);
		if (metadataService != null) {
			return getUpdatePolicy(metadataService.getMetadata(attributeDescriptor));
		}
		return DefaultUpdatePolicy.newPolicy();
	}

	public static IUpdatePolicy getUpdatePolicy(IMRIMetadataProvider info) {
		String update = (String) info.getMetadata(IMRIMetadataProvider.KEY_UPDATE_TIME);
		if (update != null && !update.equals(DEFAULT_UPDATE_INTERVAL)) {
			try {
				int interval = Integer.parseInt(update);
				return interval == 0 ? OneShotUpdatePolicy.newPolicy() : SimpleUpdatePolicy.newPolicy(interval);
			} catch (NumberFormatException e) {
				RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
						"Warning: The update_interval specified for attribute " + info.getMRI() //$NON-NLS-1$
								+ " is malformed. The default update time (" //$NON-NLS-1$
								+ Integer.toString(UpdatePolicyToolkit.getDefaultUpdateInterval())
								+ ") will be used instead.", //$NON-NLS-1$
						e);
			}
		}
		return DefaultUpdatePolicy.newPolicy();
	}

	private static IMRIMetadataService getMetadataService(IConnectionHandle handle) {
		return handle.getServiceOrNull(IMRIMetadataService.class);
	}
}
