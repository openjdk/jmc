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
package org.openjdk.jmc.console.ui.tabs.system;

import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;

public class ServerInformationModelBuilder {

	private static String getConnectionInformation(IConnectionHandle ch) {
		String serverName = ch.getServerDescriptor().getDisplayName();
		return NLS.bind(Messages.CONNECTION_INFORMATION_VALUE, serverName, ch.toString());
	}

	private static String getOsVersion(Map<String, String> systemProperties) {
		return systemProperties.get("os.name") + ' ' + systemProperties.get("os.version"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static IQuantity getTotalPhysicalMemory(MBeanServerConnection server) {
		Object memory = getAttributeValue(server, "java.lang:type=OperatingSystem", "TotalPhysicalMemorySize"); //$NON-NLS-1$ //$NON-NLS-2$
		return memory instanceof Number ? UnitLookup.BYTE.quantity((Number) memory) : null;
	}

	private static Integer getPID(RuntimeMXBean runtimeBean) {
		try {
			return Integer.valueOf(runtimeBean.getName().split("@")[0]); //$NON-NLS-1$
		} catch (Exception e1) {
			return null;
		}
	}

	private static String getVmVersion(Map<String, String> systemProperties) {
		String[] values = new String[] {systemProperties.get("java.vm.name"), //$NON-NLS-1$
				systemProperties.get("java.vm.version"), systemProperties.get("java.runtime.version")}; //$NON-NLS-1$ //$NON-NLS-2$
		return (values[0].isEmpty()) ? "" : NLS.bind(Messages.VM_VERSION_VALUE, values); //$NON-NLS-1$
	}

	private static String getVmArguments(List<String> arguments) {
		StringBuilder argBuilder = new StringBuilder();
		for (String argument : arguments) {
			if (argBuilder.length() > 0) {
				argBuilder.append(' ');
			}
			argBuilder.append(argument);
		}
		return argBuilder.toString();
	}

	private static String getApplicationArguments(
		IServerDescriptor serverDesc, MBeanServerConnection server, Map<String, String> systemProperties) {
		Object a = getAttributeValue(server, "oracle.jrockit.management:type=PerfCounters", //$NON-NLS-1$
				"sun.rt.javaCommand"); //$NON-NLS-1$
		if (a != null) {
			return a.toString();
		}
		String arguments = systemProperties.get("sun.java.command"); //$NON-NLS-1$
		if (arguments == null || arguments.length() == 0) {
			// MBean unavailable on Hotspot - instead use connection information.
			JVMDescriptor jvmInfo = serverDesc.getJvmInfo();
			if (jvmInfo != null && jvmInfo.getJavaCommand() != null) {
				arguments = jvmInfo.getJavaCommand();
			}
		}
		return arguments == null ? "" : arguments; //$NON-NLS-1$
	}

	private static Object getAttributeValue(MBeanServerConnection server, String mBean, String attribute) {
		try {
			return server.getAttribute(new ObjectName(mBean), attribute);
		} catch (Exception e) {
			return null;
		}
	}

	public static Object[][] build(IConnectionHandle connection) {
		try {
			IServerDescriptor serverDescriptor = connection.getServerDescriptor();
			MBeanServerConnection server = connection.getServiceOrThrow(MBeanServerConnection.class);
			RuntimeMXBean rtBean = ConnectionToolkit.getRuntimeBean(server);
			OperatingSystemMXBean osBean = ConnectionToolkit.getOperatingSystemBean(server);
			Map<String, String> props = rtBean.getSystemProperties();

			return new Object[][] {
					buildDataRow(Messages.CONNECTION_INFORMATION_LABEL, getConnectionInformation(connection)),
					buildDataRow(Messages.OPERATING_SYSTEM_LABEL, getOsVersion(props)),
					buildDataRow(Messages.OPERATING_SYSTEM_ARCHITECTURE_LABEL, props.get("os.arch")), //$NON-NLS-1$
					buildDataRow(Messages.NUMBER_OF_PROCESSORS_LABEL,
							UnitLookup.NUMBER_UNITY.quantity(osBean.getAvailableProcessors())),
					buildDataRow(Messages.TOTAL_PHYSICAL_MEMORY_LABEL, getTotalPhysicalMemory(server)),
					buildDataRow(Messages.PROCESS_ID_LABEL, getPID(rtBean)),
					buildDataRow(Messages.VM_VERSION_LABEL, getVmVersion(props)),
					buildDataRow(Messages.VM_VENDOR_LABEL, props.get("java.vm.vendor")), //$NON-NLS-1$
					buildDataRow(Messages.START_TIME_LABEL, UnitLookup.EPOCH_MS.quantity(rtBean.getStartTime())),
					buildDataRow(Messages.CLASS_PATH_LABEL, props.get("java.class.path")), //$NON-NLS-1$
					buildDataRow(Messages.VM_ARGUMENTS_LABEL, getVmArguments(rtBean.getInputArguments())),
					buildDataRow(Messages.APPLICATION_ARGUMENTS_LABEL,
							getApplicationArguments(serverDescriptor, server, props)),
					buildDataRow(Messages.LIBRARY_PATH_LABEL, props.get("java.library.path")), //$NON-NLS-1$
					buildDataRow(Messages.BOOT_CLASS_PATH_LABEL, props.get("sun.boot.class.path"))}; //$NON-NLS-1$
		} catch (Exception e) {
			UIPlugin.getDefault().getLogger().log(Level.WARNING, "Could not build ServerInformationModel", e); //$NON-NLS-1$
			return new Object[0][];
		}
	}

	private static Object[] buildDataRow(String label, Object value) {
		return new Object[] {label, value};
	}
}
