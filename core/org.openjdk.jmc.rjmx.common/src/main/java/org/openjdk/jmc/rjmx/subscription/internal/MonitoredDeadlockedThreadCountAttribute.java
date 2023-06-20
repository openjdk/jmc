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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.logging.Level;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.openjdk.jmc.rjmx.RJMXPlugin;

/**
 * Wrapping attribute://java.lang:type=Threading/findMonitoredDeadlockedThreads invocations as a
 * numeric attribute.
 */
public class MonitoredDeadlockedThreadCountAttribute extends AbstractSyntheticAttribute {

	private ThreadMXBean m_threadMBean;

	@Override
	public Object getValue(MBeanServerConnection connection) {
		ThreadMXBean threadMBean = getThreadMBean(connection);
		if (threadMBean == null) {
			return null;
		}
		long[] monitorDeadlockedThreads = threadMBean.findMonitorDeadlockedThreads();
		return (monitorDeadlockedThreads == null) ? 0 : monitorDeadlockedThreads.length;
	}

	private ThreadMXBean getThreadMBean(MBeanServerConnection connection) {
		if (m_threadMBean == null) {
			try {
				m_threadMBean = ManagementFactory.newPlatformMXBeanProxy(connection,
						ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
			} catch (IOException e) {
				RJMXPlugin.getDefault().getLogger().log(Level.SEVERE, "Unable to look up threading MX bean!", e); //$NON-NLS-1$
			}
		}
		return m_threadMBean;
	}

	@Override
	public void setValue(MBeanServerConnection connection, Object value) {
		// value can not be set
	}

	@Override
	public boolean hasResolvedDependencies(MBeanServerConnection connection) {
		try {
			return connection.getMBeanInfo(new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME)) != null;
		} catch (Exception e) {
		}
		return false;
	}
}
