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

import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ReflectionException;

/**
 * Defines a client side evaluated attribute. These attributes can be mounted in the existing MBean
 * tree using the {@code org.openjdk.jmc.rjmx.syntheticattribute} extension point. It is even
 * possible to add attributes, and override attributes, in already existing MBeans. Synthetic
 * attributes will take precedence. If several synthetic attributes have the same attribute name,
 * only one will be registered, and a warning will be logged for the rest.
 */
public interface ISyntheticAttribute {
	/**
	 * Returns the value of the synthetic attribute.
	 *
	 * @param connection
	 *            the MBean server to use for resource lookup
	 * @return the value of the synthetic attribute.
	 * @throws MBeanException
	 *             Wraps an exception thrown by the MBean's getter.
	 * @throws ReflectionException
	 *             Wraps an exception thrown while trying to invoke the getter.
	 */
	Object getValue(MBeanServerConnection connection) throws MBeanException, ReflectionException;

	/**
	 * Sets the value of the synthetic attribute.
	 *
	 * @param connection
	 *            the MBean server to use for resource lookup
	 * @param value
	 *            the value to set.
	 * @throws MBeanException
	 *             Wraps an exception thrown by the MBean's setter.
	 * @throws ReflectionException
	 *             Wraps an exception thrown while trying to invoke the MBean's setter.
	 */
	void setValue(MBeanServerConnection connection, Object value) throws MBeanException, ReflectionException;

	/**
	 * Whether the resources that the synthetic attribute depends on are present.
	 *
	 * @param connection
	 *            the MBean server to use for resource lookup
	 * @return {@code true} if resources are present, {@code false} otherwise
	 */
	boolean hasResolvedDependencies(MBeanServerConnection connection);

	/**
	 * Use this method to set up dependencies on notifications. It can also be used to set up
	 * external timers or other initializations.
	 *
	 * @param connection
	 *            the MBean connection that this attribute is used for.
	 */
	void init(MBeanServerConnection connection);

	/**
	 * Use this method if you have any cleanup that needs be done when the MBeanServer connection is
	 * about to be closed down.
	 */
	void stop();
}
