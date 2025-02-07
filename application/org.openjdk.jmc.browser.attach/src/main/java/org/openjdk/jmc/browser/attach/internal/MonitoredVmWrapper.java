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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

import org.openjdk.jmc.browser.attach.BrowserAttachPlugin;

public class MonitoredVmWrapper {
	private static Class<?> CLASS_MONITORED_VM;
	private static Class<?> CLASS_MONITORED_VM_UTIL;
	private static Class<?> CLASS_STRING_MONITOR;

	private Object monitoredVm;

	static {
		try {
			CLASS_MONITORED_VM = Class.forName("sun.jvmstat.monitor.MonitoredVm"); //$NON-NLS-1$
			CLASS_MONITORED_VM_UTIL = Class.forName("sun.jvmstat.monitor.MonitoredVmUtil"); //$NON-NLS-1$
			CLASS_STRING_MONITOR = Class.forName("sun.jvmstat.monitor.StringMonitor"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			CLASS_MONITORED_VM = null;
			CLASS_MONITORED_VM_UTIL = null;
			CLASS_STRING_MONITOR = null;
			BrowserAttachPlugin.getPluginLogger().log(Level.WARNING, MonitoredHostWrapper.ERROR_MESSAGE_ATTACH, e);
		}
	}

	public MonitoredVmWrapper(Object monitoredVm) {
		this.monitoredVm = monitoredVm;
	}

	public String commandLine() {
		if (CLASS_MONITORED_VM_UTIL != null) {
			Method methodCommandLine;
			try {
				methodCommandLine = CLASS_MONITORED_VM_UTIL.getMethod("commandLine", CLASS_MONITORED_VM); //$NON-NLS-1$
				Object commandLine = methodCommandLine.invoke(null, monitoredVm);
				return String.valueOf(commandLine);
			} catch (NoSuchMethodException
					| SecurityException
					| IllegalAccessException
					| IllegalArgumentException
					| InvocationTargetException e) {
				BrowserAttachPlugin.getPluginLogger().log(Level.FINEST, "Could not read command line!", e); //$NON-NLS-1$
			}
		}
		return null;
	}

	public String jvmArgs() {
		if (CLASS_MONITORED_VM_UTIL != null) {
			Method methodCommandLine;
			try {
				methodCommandLine = CLASS_MONITORED_VM_UTIL.getMethod("jvmArgs", CLASS_MONITORED_VM); //$NON-NLS-1$
				Object commandLine = methodCommandLine.invoke(null, monitoredVm);
				return String.valueOf(commandLine);
			} catch (NoSuchMethodException
					| SecurityException
					| IllegalAccessException
					| IllegalArgumentException
					| InvocationTargetException e) {
				BrowserAttachPlugin.getPluginLogger().log(Level.FINEST, "Could not read command line!", e); //$NON-NLS-1$
			}
		}
		return null;
	}

	public String jvmName() {
		return getPropertyStringValue("java.property.java.vm.name"); //$NON-NLS-1$
	}

	public String javaVersion() {
		return getPropertyStringValue("java.property.java.version"); //$NON-NLS-1$
	}

	public String jvmVersion() {
		return getPropertyStringValue("java.property.java.vm.version"); //$NON-NLS-1$
	}

	public String jvmVendor() {
		return getPropertyStringValue("java.property.java.vm.vendor");
	}

	public void detach() {
		try {
			Method methodDetach = CLASS_MONITORED_VM.getMethod("detach");
			methodDetach.invoke(monitoredVm);
		} catch (NoSuchMethodException
				| SecurityException
				| IllegalAccessException
				| IllegalArgumentException
				| InvocationTargetException e) {
			BrowserAttachPlugin.getPluginLogger().log(Level.FINEST, "Could not detach from monitored VM", e); //$NON-NLS-1$
		}
	}

	private String getPropertyStringValue(String propertyName) {
		Method methodFindByName;
		try {
			methodFindByName = CLASS_MONITORED_VM.getMethod("findByName", String.class); //$NON-NLS-1$
			Object stringMonitor = methodFindByName.invoke(monitoredVm, propertyName);
			Method methodStringValue = CLASS_STRING_MONITOR.getMethod("stringValue"); //$NON-NLS-1$
			return String.valueOf(methodStringValue.invoke(stringMonitor));
		} catch (NoSuchMethodException
				| SecurityException
				| IllegalAccessException
				| IllegalArgumentException
				| InvocationTargetException e) {
			BrowserAttachPlugin.getPluginLogger().log(Level.FINEST, "Could not read StringMonitor " + propertyName, e); //$NON-NLS-1$
		}
		return null;
	}
}
