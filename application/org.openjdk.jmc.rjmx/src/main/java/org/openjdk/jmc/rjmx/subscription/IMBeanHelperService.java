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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.openjdk.jmc.rjmx.subscription.MRI.Type;

/**
 * Little helper service to provide simple access to commonly used functionality when handling
 * MBeans.
 */
public interface IMBeanHelperService {

	/**
	 * Return a set of {@link ObjectName} containing the names of all available MBeans.
	 *
	 * @return a set of {@link ObjectName} containing the names of all available MBeans.
	 * @throws IOException
	 *             if a problem occurred with underlying connection.
	 */
	Set<ObjectName> getMBeanNames() throws IOException;

	/**
	 * Retrieves the value of an attribute. Note that the {@link Type} of the attribute MUST be
	 * {@link Type#ATTRIBUTE}.
	 *
	 * @param mri
	 *            the self containing description of the MBean Resource to retrieved (ObjectName +
	 *            attribute).
	 * @return the attribute value.
	 * @throws IOException
	 *             if a problem occurred with underlying connection.
	 * @throws JMException
	 *             if a JMX problem occurred.
	 * @throws MBeanException
	 *             Wraps an exception thrown by the MBean's getter or wraps a JMRuntimeException
	 *             which in turn might wrap a RuntimeException in the MBean's getter.
	 */
	Object getAttributeValue(MRI mri) throws IOException, JMException, MBeanException;

	/**
	 * Returns the bean information for all available MBeans.
	 *
	 * @return a map with the ObjectNames and their associated MBeanInfos.
	 * @throws IOException
	 *             if the connection failed or some other IO related problem occurred.
	 */
	Map<ObjectName, MBeanInfo> getMBeanInfos() throws IOException;

	/**
	 * Returns the MRI:s with associated metadata for the specified MBean.
	 *
	 * @param mbean
	 *            the MBean for which to return the information.
	 * @return the MRI:s with associated metadata
	 */
	Map<MRI, Map<String, Object>> getMBeanMetadata(ObjectName mbean);

	/**
	 * Returns the MBeanInfo for the specified MBean.
	 *
	 * @param mbean
	 *            the MBean for which to return the information.
	 * @return the MBeanInfo for the specified MBean
	 * @throws InstanceNotFoundException
	 *             The MBean specified was not found.
	 * @throws IntrospectionException
	 *             An exception occurred during introspection.
	 * @throws ReflectionException
	 *             An exception occurred when trying to invoke the getMBeanInfo of a Dynamic MBean.
	 * @throws IOException
	 *             A communication problem occurred when talking to the MBean server.
	 */
	MBeanInfo getMBeanInfo(ObjectName mbean)
			throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException;

	/**
	 * Takes a local time and tries to transform it to what it would have been in server time given
	 * the last measurements.
	 *
	 * @param currentTimeMillis
	 *            the time for which to derive the approximate server time.
	 * @return the approximate server time.
	 */
	long getApproximateServerTime(long currentTimeMillis);

	/**
	 * Adds a listener which will be executed when the MBean configuration in the MBean server
	 * changes.
	 *
	 * @param listener
	 *            the listener add. Should complete quickly.
	 */
	void addMBeanServerChangeListener(IMBeanServerChangeListener listener);

	/**
	 * Removes any listener that was added by
	 * {@link #addMBeanServerChangeListener(IMBeanServerChangeListener)}. If no listener has been
	 * added nothing happens.
	 *
	 * @param listener
	 *            the listener to remove
	 */
	void removeMBeanServerChangeListener(IMBeanServerChangeListener listener);
}
