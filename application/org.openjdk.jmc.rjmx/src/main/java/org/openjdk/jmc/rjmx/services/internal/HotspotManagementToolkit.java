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
package org.openjdk.jmc.rjmx.services.internal;

import java.io.IOException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.openjdk.jmc.rjmx.ConnectionToolkit;

public final class HotspotManagementToolkit {

	private final static String MC_BEAN_CLASS = "com.sun.management.MissionControl"; //$NON-NLS-1$
	private final static ObjectName MC_BEAN_NAME = ConnectionToolkit
			.createObjectName("com.sun.management:type=MissionControl"); //$NON-NLS-1$
	private final static ObjectName HS_DIAGNOSTICS_BEAN_NAME = ConnectionToolkit
			.createObjectName("com.sun.management:type=HotSpotDiagnostic"); //$NON-NLS-1$
	private final static String OPERATION_GET_VM_OPTION = "getVMOption"; //$NON-NLS-1$
	private final static String OPERATION_SET_VM_OPTION = "setVMOption"; //$NON-NLS-1$
	private final static String OPERATION_REGISTER_MBEANS = "registerMBeans"; //$NON-NLS-1$
	private final static String PARAMETER_STRING = String.class.getName();
	private final static String EXPLICIT_FLAG = "VM_CREATION"; //$NON-NLS-1$

	public static void setVMOption(MBeanServerConnection server, String flag, String value) throws Exception {
		server.invoke(HS_DIAGNOSTICS_BEAN_NAME, OPERATION_SET_VM_OPTION, new Object[] {flag, value},
				new String[] {PARAMETER_STRING, PARAMETER_STRING});
	}

	public static Object getVMOption(MBeanServerConnection server, String flag) throws Exception {
		CompositeData data = (CompositeData) server.invoke(HS_DIAGNOSTICS_BEAN_NAME, OPERATION_GET_VM_OPTION,
				new Object[] {flag}, new String[] {PARAMETER_STRING});
		return data.get("value"); //$NON-NLS-1$
	}

	public static boolean isVMOptionExplicit(MBeanServerConnection server, String flag) throws Exception {
		CompositeData data = (CompositeData) server.invoke(HS_DIAGNOSTICS_BEAN_NAME, OPERATION_GET_VM_OPTION,
				new Object[] {flag}, new String[] {PARAMETER_STRING});
		return data.get("origin").equals(EXPLICIT_FLAG); //$NON-NLS-1$
	}

	public static void registerMBeans(MBeanServerConnection server) throws JMException, IOException {
		try {
			server.createMBean(MC_BEAN_CLASS, MC_BEAN_NAME);
		} catch (InstanceAlreadyExistsException iaee) {
			// Ok, it already exists.
		} catch (MBeanException mbe) {
			// This catch is a workaround for https://github.com/javaee/glassfish/issues/20686
			if (mbe.getTargetException() instanceof InstanceAlreadyExistsException) {
				// Ok, it already exists.
			} else {
				throw mbe;
			}
		}
		server.invoke(MC_BEAN_NAME, OPERATION_REGISTER_MBEANS, new Object[0], new String[0]);
	}
}
