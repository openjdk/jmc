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
package org.openjdk.jmc.rjmx;

import javax.management.MBeanServerConnection;
import javax.management.modelmbean.ModelMBeanNotificationBroadcaster;
import javax.management.openmbean.OpenType;

/**
 * Base interface to implement for synthetic notifications. Used with the
 * {@code org.openjdk.jmc.rjmx.syntheticnotification} extension point.
 */
public interface ISyntheticNotification {

	/**
	 * This method returns the value to include in the notification.
	 *
	 * @return the value to include in the notification.
	 */
	Object getValue();

	/**
	 * Override this method if you have any cleanup that needs be done when the MBeanServer
	 * connection is about to be closed down.
	 */
	void stop();

	/**
	 * Whether the resources that the synthetic notification depends on are present.
	 *
	 * @param connection
	 *            the MBean server to use for resource lookup
	 * @return <tt>true</tt> if resources are present, <tt>false</tt> otherwise
	 */
	boolean hasResolvedDependencies(MBeanServerConnection connection);

	/**
	 * This method can be overridden to set up dependencies on other notifications. It can also be
	 * used to set up external timers or any other mechanism that will send notifications. Note that
	 * no notification will be triggered unless one is explicit created and sent to
	 * {@link ModelMBeanNotificationBroadcaster#sendNotification(javax.management.Notification)}. It
	 * is the subclass' responsibility to ensure this happens.
	 *
	 * @param connection
	 *            the MBean connection that this notification is used for.
	 * @param type
	 *            the type of notifications that this synthetic notification is expected to
	 *            generate.
	 * @param message
	 *            the message that will be provided in the generated notification by default.
	 */
	void init(MBeanServerConnection connection, String type, String message);

	/**
	 * This method is used by the framework to inject the broadcaster.
	 *
	 * @param broadcaster
	 *            the broadcaster responsible for sending the notifications.
	 */
	void init(ModelMBeanNotificationBroadcaster broadcaster);

	/**
	 * This method is used by the framework to access the broadcaster.
	 *
	 * @return the broadcaster of this synthetic notification
	 */
	ModelMBeanNotificationBroadcaster getBroadcaster();

	/**
	 * Returns the MBean {@link OpenType} for this synthetic notification.
	 *
	 * @return this notifications type
	 */
	OpenType<?> getValueType();
}
