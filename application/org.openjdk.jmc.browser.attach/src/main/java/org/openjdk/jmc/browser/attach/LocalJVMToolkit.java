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
package org.openjdk.jmc.browser.attach;

import static org.openjdk.jmc.ui.common.jvm.Connectable.ATTACHABLE;
import static org.openjdk.jmc.ui.common.jvm.Connectable.MGMNT_AGENT_STARTED;
import static org.openjdk.jmc.ui.common.jvm.Connectable.NO;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;

import javax.management.remote.JMXServiceURL;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.openjdk.jmc.attach.AttachToolkit;
import org.openjdk.jmc.browser.attach.internal.ExecuteTunnler;
import org.openjdk.jmc.browser.attach.preferences.PreferenceConstants;
import org.openjdk.jmc.common.version.JavaVMVersionToolkit;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.ui.common.jvm.Connectable;
import org.openjdk.jmc.ui.common.jvm.JVMArch;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;
import org.openjdk.jmc.ui.common.jvm.JVMType;

import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.StringMonitor;
import sun.jvmstat.monitor.VmIdentifier;
import sun.tools.attach.HotSpotVirtualMachine;

/**
 * The activator class controls the plug-in life cycle
 */
public class LocalJVMToolkit {
	public static class DiscoveryEntry {
		private final IServerDescriptor serverDescriptor;
		private final IConnectionDescriptor connectionDescriptor;

		public DiscoveryEntry(IServerDescriptor serverDescriptor, IConnectionDescriptor descriptor) {
			this.serverDescriptor = serverDescriptor;
			connectionDescriptor = descriptor;
		}

		public IConnectionDescriptor getConnectionDescriptor() {
			return connectionDescriptor;
		}

		public IServerDescriptor getServerDescriptor() {
			return serverDescriptor;
		}
	}

	private static long SEQ_NUMBER = 0;
	private static boolean isErrorMessageSent = false;
	private static boolean m_unconnectableInited = false;
	private static boolean m_showUnconnectable = false;

	private static Map<Object, DiscoveryEntry> last = new WeakHashMap<>();

	static final String LOCAL_CONNECTOR_ADDRESS_PROP = "com.sun.management.jmxremote.localConnectorAddress"; //$NON-NLS-1$
	static final String JVM_ARGS_PROP = "sun.jvm.args"; //$NON-NLS-1$
	static final String JVM_FLAGS_PROP = "sun.jvm.flags"; //$NON-NLS-1$
	static final String JAVA_COMMAND_PROP = "sun.java.command"; //$NON-NLS-1$

	private LocalJVMToolkit() {
		// Toolkit
	}

	/**
	 * @return returns the local JVM's that could be discovered.
	 */
	public static DiscoveryEntry[] getLocalConnections() {
		HashMap<Object, DiscoveryEntry> map = new HashMap<>();
		populateAttachableVMs(map);
		populateMonitoredVMs(map, showUnconnectableJvms());
		last = map;
		ArrayList<DiscoveryEntry> list = new ArrayList<>(map.values());
		return list.toArray(new DiscoveryEntry[list.size()]);
	}

	private static final boolean showUnconnectableJvms() {
		if (!m_unconnectableInited) {
			IPreferenceStore store = BrowserAttachPlugin.getDefault().getPreferenceStore();
			if (store != null) {
				m_showUnconnectable = store.getBoolean(PreferenceConstants.P_SHOW_UNCONNECTABLE);
				store.addPropertyChangeListener(new IPropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent event) {
						if (event.getProperty().equals(PreferenceConstants.P_SHOW_UNCONNECTABLE)) {
							m_showUnconnectable = ((Boolean) event.getNewValue()).booleanValue();
						}
					}
				});
				m_unconnectableInited = true;
			}
		}
		return m_showUnconnectable;
	}

	private static void populateMonitoredVMs(HashMap<Object, DiscoveryEntry> map, boolean includeUnconnectables) {
		MonitoredHost host = getMonitoredHost();
		Set<?> vms;
		try {
			vms = host.activeVms();
		} catch (MonitorException mx) {
			throw new InternalError(mx.getMessage());
		}
		for (Object vmid : vms) {
			if (vmid instanceof Integer) {
				// Check if the map already contains a descriptor for this
				if (map.containsKey(vmid)) {
					continue;
				}
				// Check if we already have a descriptor *first*, to avoid unnecessary attach which may leak handles
				DiscoveryEntry connDesc = last.get(vmid);
				if (connDesc == null) {
					connDesc = createMonitoredJvmDescriptor(host, (Integer) vmid);
				}

				if (includeUnconnectables
						|| (connDesc != null && !connDesc.getServerDescriptor().getJvmInfo().isUnconnectable())) {
					map.put(vmid, connDesc);
				}
			}
		}
	}

	private static DiscoveryEntry createMonitoredJvmDescriptor(MonitoredHost host, Integer vmid) {
		DiscoveryEntry connDesc;
		int pid = vmid.intValue();
		String name = vmid.toString(); // default to pid if name not available
		Connectable connectable = NO;
		JVMType type = JVMType.OTHER;
		JVMArch jvmArch = JVMArch.OTHER;
		boolean isDebug = false;
		String address = null;
		String version = null;
		String jvmArgs = null;
		try {
			// This used to leak one \BaseNamedObjects\hsperfdata_* Section handle on Windows
			MonitoredVm mvm = host.getMonitoredVm(new VmIdentifier(name));
			try {
				// use the command line as the display name
				name = MonitoredVmUtil.commandLine(mvm);
				jvmArgs = MonitoredVmUtil.jvmArgs(mvm);
				StringMonitor sm = (StringMonitor) mvm.findByName("java.property.java.vm.name"); //$NON-NLS-1$
				if (sm != null) {
					type = getJVMType(sm.stringValue());
				}

				sm = (StringMonitor) mvm.findByName("java.property.java.version"); //$NON-NLS-1$
				if (sm != null) {
					version = sm.stringValue();
				}

				if (version == null) {
					// Use java.vm.version when java.version is not exposed as perfcounter (HotSpot 1.5 and JRockit)
					sm = (StringMonitor) mvm.findByName("java.property.java.vm.version"); //$NON-NLS-1$
					if (sm != null) {
						String vmVersion = sm.stringValue();
						if (type == JVMType.JROCKIT) {
							version = JavaVMVersionToolkit.decodeJavaVersion(vmVersion);
						} else {
							version = JavaVMVersionToolkit.parseJavaVersion(vmVersion);
						}
					}
				}
				if (version == null) {
					version = "0"; //$NON-NLS-1$
				}

				if (sm != null) {
					isDebug = isDebug(sm.stringValue());
				}
				// NOTE: isAttachable seems to return true even if a real attach is not possible.
				// attachable = MonitoredVmUtil.isAttachable(mvm);

				jvmArch = getArch(vmid);
				// Check if the in-memory agent has been started, in that case we can connect anyway
				JMXServiceURL inMemURL = null;
				try {
					inMemURL = LocalJVMToolkit.getInMemoryURLFromPID(vmid);
				} catch (IOException e) {
					BrowserAttachPlugin.getPluginLogger().log(Level.WARNING,
							"Got exception when trying to get in-memory url for jvm with PID " + vmid, e); //$NON-NLS-1$
				}
				if (inMemURL != null) {
					connectable = MGMNT_AGENT_STARTED;
				}

				// This used to leak one \BaseNamedObjects\hsperfdata_* Section handle on Windows
				address = AttachToolkit.importFromPid(pid);
			} finally {
				// Although the current implementation of LocalMonitoredVm for Windows doesn't do much here, we should always call detach.
				mvm.detach();
			}
		} catch (Exception x) {
			// ignore
		}
		connDesc = createDescriptor(name, jvmArgs, vmid, connectable, type, jvmArch, address, version, isDebug);
		return connDesc;
	}

	/*
	 * Try to attach to get info from the AttachNotSupportedException.
	 */
	private static JVMArch getArch(Integer vmid) throws IOException {
		JVMArch jvmArch = JVMArch.OTHER;
		List<VirtualMachineDescriptor> vms = VirtualMachine.list();
		if (vms != null) {
			for (VirtualMachineDescriptor vmd : vms) {
				if (vmid == Integer.parseInt(vmd.id())) {
					try {
						VirtualMachine vm = VirtualMachine.attach(vmd);
						try {
							jvmArch = JVMArch.getJVMArch(vm.getSystemProperties());
						} finally {
							vm.detach();
						}
					} catch (AttachNotSupportedException x) {
						if (x.getMessage().contains("Unable to attach to 32-bit process")) { //$NON-NLS-1$
							jvmArch = JVMArch.BIT32;
						} else if (x.getMessage().contains("Unable to attach to 64-bit process")) { //$NON-NLS-1$
							jvmArch = JVMArch.BIT64;
						}
					}
					break;
				}
			}
		}
		return jvmArch;
	}

	private static JVMType getJVMType(String jvmName) {
		if (JavaVMVersionToolkit.isJRockitJVMName(jvmName)) {
			return JVMType.JROCKIT;
		} else if (JavaVMVersionToolkit.isHotspotJVMName(jvmName)) {
			return JVMType.HOTSPOT;
		}
		return JVMType.OTHER;
	}

	private static boolean isDebug(String stringValue) {
		return stringValue.toUpperCase().contains("DEBUG"); //$NON-NLS-1$
	}

	private static void populateAttachableVMs(Map<Object, DiscoveryEntry> map) {
		// This used to leak \BaseNamedObjects\hsperfdata_* Section handles on Windows
		List<VirtualMachineDescriptor> vms = VirtualMachine.list();
		if (vms == null) {
			return;
		}

		for (VirtualMachineDescriptor vmd : vms) {
			try {
				Integer vmid = Integer.valueOf(vmd.id());
				if (!map.containsKey(vmid)) {
					BrowserAttachPlugin.getPluginLogger().finest("Local attach resolving PID " + vmid); //$NON-NLS-1$
					// Check if we already have a descriptor *first* to avoid unnecessary attach which may leak handles
					DiscoveryEntry connDesc = last.get(vmid);
					if (connDesc == null) {
						connDesc = createAttachableJvmDescriptor(vmd);
					}

					if (connDesc != null && !connDesc.getServerDescriptor().getJvmInfo().isUnconnectable()) {
						map.put(vmid, connDesc);
					}
				}
			} catch (NumberFormatException e) {
				// do not support vmid different than pid
			}
		}
	}

	private static DiscoveryEntry createAttachableJvmDescriptor(VirtualMachineDescriptor vmd) {
		DiscoveryEntry connDesc = null;
		Connectable connectable;
		boolean isDebug = false;
		JVMType jvmType = JVMType.OTHER;
		JVMArch jvmArch = JVMArch.OTHER;
		String address = null;
		String version = null;
		String javaArgs = null;
		String jvmArgs = null;
		String jvmVersion = null;

		try {
			// Attach creates one process handle on Windows.
			// This leaks one thread handle due to Sun bug in j2se/src/windows/native/sun/tools/attach/WindowsVirtualMachine.c
			VirtualMachine vm = VirtualMachine.attach(vmd);
			try {
				connectable = ATTACHABLE;
				// This leaks one thread handle due to Sun bug in j2se/src/windows/native/sun/tools/attach/WindowsVirtualMachine.c
				Properties props = null;
				try {
					props = vm.getSystemProperties();
				} catch (IOException e) {
					BrowserAttachPlugin.getPluginLogger().log(Level.FINER,
							"Got the following exception message when getting system properties from vm with PID " //$NON-NLS-1$
									+ vmd + ": " + e.getMessage()); //$NON-NLS-1$

				}
				if (props != null) {
					String vmName = props.getProperty("java.vm.name"); //$NON-NLS-1$
					jvmType = getJVMType(vmName);
					version = props.getProperty("java.version"); //$NON-NLS-1$
					jvmVersion = props.getProperty("java.vm.version"); //$NON-NLS-1$
					isDebug = isDebug(jvmVersion);
					jvmArch = JVMArch.getJVMArch(props);
				}
				Properties agentProps = vm.getAgentProperties();
				address = (String) agentProps.get(LOCAL_CONNECTOR_ADDRESS_PROP);
				javaArgs = resolveCommandLine(vm, vmd.displayName(), props, agentProps);
				jvmArgs = (String) agentProps.get("sun.jvm.args"); //$NON-NLS-1$

			} finally {
				// Always detach. Releases one process handle on Windows.
				vm.detach();
			}
		} catch (AttachNotSupportedException x) {
			// Not attachable
			connectable = NO;
		} catch (Throwable t) {
			// Serious problem for this JVM, let's skip this one.
			if (!isErrorMessageSent) {
				BrowserAttachPlugin.getPluginLogger().log(Level.FINER,
						"Scanning using attach/getAgentProperties failed on " //$NON-NLS-1$
								+ vmd
								+ ". This message will only be printed once, so errors for subsequent PIDs will not be logged...", //$NON-NLS-1$
						t);
				isErrorMessageSent = true;
			}
			return null;
		}
		if (connectable.isAttachable()) {
			connDesc = createDescriptor(javaArgs, jvmArgs, Integer.parseInt(vmd.id()), connectable, jvmType, jvmArch,
					address, version, isDebug);
		}
		BrowserAttachPlugin.getPluginLogger().info("Done resolving PID " + vmd); //$NON-NLS-1$
		return connDesc;
	}

	private static MonitoredHost getMonitoredHost() {
		try {
			return MonitoredHost.getMonitoredHost(new HostIdentifier((String) null));
		} catch (MonitorException e) {
			throw new InternalError(e.getMessage());
		} catch (URISyntaxException e) {
			throw new InternalError(e.getMessage());
		}
	}

	// Workaround to resolve command line when Eclipse is launched with -vm ... jvm.dll
	private static String resolveCommandLine(
		VirtualMachine vm, String displayName, Properties vmProps, Properties agentProps) {
		if (isValidDisplayName(displayName)) {
			return displayName;
		}
		if (vmProps != null) {
			String eclipseVmargs = vmProps.getProperty("eclipse.vmargs"); //$NON-NLS-1$
			if (eclipseVmargs != null) {
				String[] parts = eclipseVmargs.split("java.class.path="); //$NON-NLS-1$
				return parts.length == 2 ? parts[1] : eclipseVmargs;
			}
		}
		if (agentProps != null) {
			String jvmCmd = (String) agentProps.get(JAVA_COMMAND_PROP);
			if (jvmCmd == null || jvmCmd.length() == 0) {
				jvmCmd = (String) agentProps.get(JVM_ARGS_PROP);
			}
			if (jvmCmd == null || jvmCmd.length() == 0) {
				jvmCmd = (String) agentProps.get(JVM_FLAGS_PROP);
			}
			if (jvmCmd != null && jvmCmd.length() > 0) {
				return jvmCmd;
			}
		}
		return displayName;
	}

	private static boolean isValidDisplayName(String displayName) {
		return displayName != null && !displayName.equals("") && !displayName.equals("Unknown"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static DiscoveryEntry createDescriptor(
		String javaCommand, String jvmArgs, int pid, Connectable connectable, JVMType type, JVMArch arch,
		String address, String version, boolean isDebug) {
		JVMDescriptor jvmInfo = new JVMDescriptor(version, type, arch, javaCommand, jvmArgs, pid, isDebug, connectable);
		LocalConnectionDescriptor lcd = new LocalConnectionDescriptor(pid, address, connectable == ATTACHABLE);
		String guid = "Local-[PID:" + pid + ", seq:" + (SEQ_NUMBER++) + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		IServerDescriptor sd = IServerDescriptor.create(guid, null, jvmInfo);
		return new DiscoveryEntry(sd, lcd);
	}

	/**
	 * @return descriptors for all discovered JVM's.
	 */
	public static synchronized DiscoveryEntry[] getAttachableJVMs() {
		return getLocalConnections();
	}

	/**
	 * Runs a jcmd in the specified HotSpot.
	 *
	 * @param pid
	 * @param command
	 * @return the result from running the jcmd.
	 * @throws AttachNotSupportedException
	 * @throws IOException
	 * @throws AgentLoadException
	 */
	public static String executeCommandForPid(String pid, String command)
			throws AttachNotSupportedException, IOException, AgentLoadException {
		return executeCommandForPid(pid, command, false);
	}

	/**
	 * Runs a jcmd in the specified HotSpot.
	 *
	 * @param pid
	 * @param command
	 * @param getCausingInformation
	 * @return the result from running the jcmd.
	 * @throws AttachNotSupportedException
	 * @throws IOException
	 * @throws AgentLoadException
	 */
	public static String executeCommandForPid(String pid, String command, boolean getCausingInformation)
			throws AttachNotSupportedException, IOException, AgentLoadException {
		VirtualMachine vm = VirtualMachine.attach(pid);
		String result = executeCommandForPid(vm, pid, command, getCausingInformation);
		vm.detach();
		return result;
	}

	/**
	 * Runs a jcmd in the specified HotSpot.
	 *
	 * @param vm
	 * @param pid
	 * @param command
	 * @return the result from running the jcmd.
	 * @throws AttachNotSupportedException
	 * @throws IOException
	 * @throws AgentLoadException
	 */
	public static String executeCommandForPid(VirtualMachine vm, String pid, String command)
			throws AttachNotSupportedException, IOException, AgentLoadException {
		return executeCommandForPid(vm, pid, command, false);
	}

	/**
	 * Runs a jcmd in the specified HotSpot.
	 *
	 * @param vm
	 * @param pid
	 * @param command
	 * @param throwCausingException
	 *            If the target cause of an eventual exception should be returned as the result.
	 * @return the result from running the jcmd.
	 * @throws AttachNotSupportedException
	 * @throws IOException
	 * @throws AgentLoadException
	 */
	public static String executeCommandForPid(
		VirtualMachine vm, String pid, String command, boolean throwCausingException)
			throws AttachNotSupportedException, IOException, AgentLoadException {
		HotSpotVirtualMachine hvm = (HotSpotVirtualMachine) vm;
		InputStream in = ExecuteTunnler.execute(hvm, "jcmd", new Object[] {command}, throwCausingException); //$NON-NLS-1$
		byte b[] = new byte[256];
		int n;
		StringBuffer buf = new StringBuffer();
		do {
			n = in.read(b);
			if (n > 0) {
				String s = new String(b, 0, n, "UTF-8"); //$NON-NLS-1$
				buf.append(s);
			}
		} while (n > 0);

		try {
			in.close();
		} catch (IOException ex) {
			/* Don't care */
		}
		return buf.toString();
	}

	/**
	 * @param pid
	 *            the process ID of the process to communicate with.
	 * @return the JMXServiceURL for communicating with the in memory agent having the specified
	 *         pid.
	 * @throws IOException
	 */
	public static JMXServiceURL getInMemoryURLFromPID(int pid) throws IOException {
		JMXServiceURL inMemURL = null;
		String address = AttachToolkit.importFromPid(pid);
		if (address != null) {
			inMemURL = new JMXServiceURL(address);
		}
		return inMemURL;
	}
}
