/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024, 2025, Datadog, Inc. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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

package org.openjdk.jmc.browser.attach.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;

import org.openjdk.jmc.browser.attach.BrowserAttachPlugin;

public class MonitoredHostWrapper {
	public static final String ERROR_MESSAGE_ATTACH = "Could not find attach related classes. This can affect discovery of locally running JVMs.";

	private static Class<?> CLASS_HOST_IDENTIFIER;
	private static Class<?> CLASS_VM_IDENTIFIER;
	static Class<?> CLASS_MONITORED_HOST;

	private Object host;

	static {
		try {
			CLASS_MONITORED_HOST = Class.forName("sun.jvmstat.monitor.MonitoredHost");
			CLASS_HOST_IDENTIFIER = Class.forName("sun.jvmstat.monitor.HostIdentifier");
			CLASS_VM_IDENTIFIER = Class.forName("sun.jvmstat.monitor.VmIdentifier");
		} catch (ClassNotFoundException e) {
			CLASS_MONITORED_HOST = null;
			CLASS_HOST_IDENTIFIER = null;
			CLASS_VM_IDENTIFIER = null;
			BrowserAttachPlugin.getPluginLogger().log(Level.WARNING, ERROR_MESSAGE_ATTACH, e);
		}
	}

	public static MonitoredHostWrapper getMonitoredHost() {
		return new MonitoredHostWrapper();
	}

	public MonitoredHostWrapper() {
		if (CLASS_MONITORED_HOST != null) {
			try {
				Object hostIdentifier = createHostIdentifier(null);
				Method method;
				method = CLASS_MONITORED_HOST.getMethod("getMonitoredHost", CLASS_HOST_IDENTIFIER);
				host = method.invoke(null, hostIdentifier);
			} catch (NoSuchMethodException
					| SecurityException
					| IllegalAccessException
					| IllegalArgumentException
					| InvocationTargetException
					| InstantiationException e) {
				BrowserAttachPlugin.getPluginLogger().log(Level.FINEST,
						"Could not create monitored host. This could make local attach problematic.", e);
			}
		}
	}

	private Object createHostIdentifier(String id) throws NoSuchMethodException, SecurityException,
			InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Constructor<?> constructor = CLASS_HOST_IDENTIFIER.getConstructor(String.class);
		return constructor.newInstance(id);
	}

	public Set<?> activeVms() {
		if (host != null) {
			Method method;
			try {
				method = CLASS_MONITORED_HOST.getMethod("activeVms");
				return (Set<?>) method.invoke(host);
			} catch (NoSuchMethodException
					| SecurityException
					| IllegalAccessException
					| IllegalArgumentException
					| InvocationTargetException e) {
				BrowserAttachPlugin.getPluginLogger().log(Level.FINEST,
						"Problem getting active VMs. This could make local attach problematic.", e);
			}
		}
		return Collections.emptySet();
	}

	public MonitoredVmWrapper getMonitoredVm(String name) {
		if (host != null) {
			if (CLASS_VM_IDENTIFIER != null) {
				Constructor<?> constructor;
				try {
					constructor = CLASS_VM_IDENTIFIER.getConstructor(String.class);
					Object vmIdentifier = constructor.newInstance(name);
					Method getMonitoredVm = CLASS_MONITORED_HOST.getMethod("getMonitoredVm", CLASS_VM_IDENTIFIER);
					Object mvm = getMonitoredVm.invoke(host, vmIdentifier);
					if (mvm != null) {
						return new MonitoredVmWrapper(mvm);
					}
				} catch (InstantiationException
						| IllegalAccessException
						| IllegalArgumentException
						| InvocationTargetException
						| NoSuchMethodException
						| SecurityException e) {
					BrowserAttachPlugin.getPluginLogger().log(Level.FINEST, MonitoredHostWrapper.ERROR_MESSAGE_ATTACH,
							e);
				}
			}
		}
		return null;
	}
}
