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
package org.openjdk.jmc.rjmx.services.jfr.internal;

import java.io.IOException;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.services.internal.HotspotManagementToolkit;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;

/**
 * Helper class for facilitating communication with the FlightRecorderMBean. This class works with
 * R28 and HotSpot 7u4.
 */
final class FlightRecorderCommunicationHelperV1 implements IFlightRecorderCommunicationHelper {
	private final MBeanServerConnection server;
	private final static ObjectName JFR_MBEAN_OBJECT_NAME = ConnectionToolkit
			.createObjectName("com.oracle.jrockit:type=FlightRecorder"); //$NON-NLS-1$

	public FlightRecorderCommunicationHelperV1(MBeanServerConnection server) {
		this.server = server;
	}

	@Override
	public Object getAttribute(String attribute) throws FlightRecorderException {
		try {
			try {
				return getJfrAttribute(attribute);
			} catch (InstanceNotFoundException e) {
				HotspotManagementToolkit.registerMBeans(server);
				return getJfrAttribute(attribute);
			}
		} catch (IOException e) {
			throw new FlightRecorderException("Could not retrieve the attribute " + attribute + '!', e); //$NON-NLS-1$
		} catch (JMException e) {
			throw new FlightRecorderException("Could not retrieve the attribute " + attribute + '!', e); //$NON-NLS-1$
		}
	}

	private Object getJfrAttribute(String attribute) throws JMException, IOException {
		return server.getAttribute(JFR_MBEAN_OBJECT_NAME, attribute);
	}

	@Override
	public Object invokeOperation(String name, Object ... parameters) throws IOException, FlightRecorderException {
		try {
			try {
				return invokeJfrOperation(name, parameters);
			} catch (InstanceNotFoundException e) {
				HotspotManagementToolkit.registerMBeans(server);
				return invokeJfrOperation(name, parameters);
			}
		} catch (JMException e) {
			IOException throwMe = new IOException(e.getMessage());
			throwMe.initCause(e);
			throw throwMe;
		}
	}

	private Object invokeJfrOperation(String operation, Object ... parameters) throws JMException, IOException {
		return ConnectionToolkit.invokeOperation(server, JFR_MBEAN_OBJECT_NAME, operation, parameters);
	}

	@Override
	public void closeRecording(IRecordingDescriptor descriptor) throws FlightRecorderException {
		try {
			invokeOperation("close", descriptor.getObjectName()); //$NON-NLS-1$
		} catch (Exception e) {
			throw new FlightRecorderException("Could not close the recording!", e); //$NON-NLS-1$
		}
	}
}
