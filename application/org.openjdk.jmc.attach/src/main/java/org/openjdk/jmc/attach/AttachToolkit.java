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
package org.openjdk.jmc.attach;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.management.counter.perf.InstrumentationException;

/**
 * Checks for local attach availability
 */
public class AttachToolkit {
	// True if we have local attach features available.
	private static final boolean isLocalAttachAvailable;
	private static final Class<?> connectorAddressLink;
	private static final Logger LOGGER = Logger.getLogger("org.openjdk.jmc.attach"); //$NON-NLS-1$

	static {
		boolean available;
		Class<?> cal = null;
		try {
			Class.forName("com.sun.tools.attach.VirtualMachine"); //$NON-NLS-1$
			Class.forName("sun.tools.attach.HotSpotVirtualMachine"); //$NON-NLS-1$
			Class.forName("sun.jvmstat.monitor.MonitorException"); //$NON-NLS-1$
			cal = lookupConnectorAddressLink();
			available = true;
		} catch (Throwable t) {
			available = false;
			LOGGER.log(Level.FINE, "Attach functionality will not be enabled.", t); //$NON-NLS-1$
		}
		isLocalAttachAvailable = available;
		connectorAddressLink = cal;
	}

	/**
	 * @return if we have local attachment capabilities available.
	 */
	public static boolean isLocalAttachAvailable() {
		return isLocalAttachAvailable;
	}

	private static Class<?> lookupConnectorAddressLink() throws Throwable {
		try {
			return Class.forName("jdk.internal.agent.ConnectorAddressLink"); //$NON-NLS-1$
		} catch (Throwable t) {
			// NOTE: Assuming this occurred because we are running Java 7/8, try next class name.
			LOGGER.log(Level.INFO, "Could not load ConnectorAddressLink class, probably because JMC is run with Java 8", //$NON-NLS-1$
					t);
		}
		return Class.forName("sun.management.ConnectorAddressLink"); //$NON-NLS-1$
	}

	public static String importFromPid(Integer pid) {
		try {
			Method importFrom = connectorAddressLink.getMethod("importFrom", int.class); //$NON-NLS-1$
			return (String) importFrom.invoke(null, pid);
		} catch (NullPointerException e) {
			// This can happen if the JVM dies on us during the call.
		} catch (InstrumentationException e) {
			// This can happen when connecting to a 1.4 Sun JVM.
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Could not get connector address for pid", t); //$NON-NLS-1$
		}
		return null;
	}
}
