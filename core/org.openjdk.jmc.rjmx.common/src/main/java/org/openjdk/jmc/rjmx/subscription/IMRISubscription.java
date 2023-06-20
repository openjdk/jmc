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
package org.openjdk.jmc.rjmx.subscription;

import org.openjdk.jmc.rjmx.IConnectionHandle;

/**
 * Interface for all MRI subscriptions.
 */
public interface IMRISubscription {
	/**
	 * Returns the last value event from the subscription.
	 *
	 * @return the last value event from the subscription.
	 */
	MRIValueEvent getLastMRIValueEvent();

	/**
	 * Return the connection handle this subscription is bound to.
	 *
	 * @return the associated connection handle.
	 */
	IConnectionHandle getConnectionHandle();

	/**
	 * Returns the metadata associated with this subscription.
	 *
	 * @return the metadata associated with this subscription.
	 */
	IMRIMetadata getMRIMetadata();

	/**
	 * Sets the update policy of this subscription. An update policy defines when an attribute is to
	 * be updated.
	 * <p>
	 * Note that no promises are made as to when this update will be acted upon by the subscription
	 * service.
	 *
	 * @param policy
	 *            the new update police.
	 */
	void setUpdatePolicy(IUpdatePolicy policy);

	/**
	 * Returns the update policy.
	 *
	 * @return the update policy.
	 */
	IUpdatePolicy getUpdatePolicy();
}
