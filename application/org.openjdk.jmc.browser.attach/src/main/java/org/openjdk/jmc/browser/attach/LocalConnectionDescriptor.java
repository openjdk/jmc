/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.ui.common.util.Environment;

import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

/**
 * The Connection Descriptor for a Local JVM.
 */
public class LocalConnectionDescriptor implements IConnectionDescriptor {

	private final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.browser.attach"); //$NON-NLS-1$
	private static final String SELF_HOST_NAME = "localhost"; //$NON-NLS-1$
	private static final String ATTACH_TIMED_OUT_ERROR_MESSAGE = "Timed out attempting to attach to target JVM!"; //$NON-NLS-1$
	private static final String COULD_NOT_RETRIEVE_URL_ERROR_MESSAGE = "Could not retrieve the in-memory service URL after starting the in-memory agent!"; //$NON-NLS-1$
	private final boolean isAutoStartAgent = fetchAutoStartAgentFromStore();
	private final int pid;
	private static final int TIMEOUT_THRESHOLD = 5;
	private final boolean attachable;
	private JMXServiceURL url;

	public LocalConnectionDescriptor(int pid, String address, boolean attachable) {
		this.pid = pid;
		this.attachable = attachable;
		setAddress(address);
	}

	private boolean isSelfMonitoring() {
		return pid == Environment.getThisPID();
	}

	/**
	 * Attaches to, and starts the management agent, on an attachable JVM. Sets up the service url
	 * accordingly.
	 *
	 * @throws IOException
	 *             if the management server could not be started.
	 */
	private void startManagementServer() throws IOException {
		if (isSelfMonitoring()) {
			return;
		}

		String pidStr = String.valueOf(pid);
		// Since starting the management server the Sun way is inherently
		// unsafe (they do System.exit() among other things if they fail
		// during start up), we always attempt to start it using our own
		// proprietary JCMD handler way.
		try {
			tryJCMDStyleStartingOfTheAgent(pidStr);
		} catch (Exception e) {
			tryAgentLoadingStyleOfStartingTheAgent(pidStr);
		}
	}

	private void tryAgentLoadingStyleOfStartingTheAgent(String pid) throws LazyServiceURLResolveException {
		// This is hotspot style starting of the agent. We do this
		// pretty much the way JConsole does it.
		try {
			// Add a timeout here so we don't block forever if the JVM is busy/suspended. See JMC-5398
			ExecutorService service = Executors.newSingleThreadExecutor();
			Future<Void> future = service.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					VirtualMachine vm = VirtualMachine.attach(pid);
					String home = vm.getSystemProperties().getProperty("java.home"); //$NON-NLS-1$
					// Normally in ${java.home}/jre/lib/management-agent.jar but might
					// be in ${java.home}/lib in build environments.
					String agent = home + File.separator + "jre" + File.separator + "lib" + File.separator //$NON-NLS-1$ //$NON-NLS-2$
							+ "management-agent.jar"; //$NON-NLS-1$
					File f = new File(agent);
					if (!f.exists()) {
						agent = home + File.separator + "lib" + File.separator + "management-agent.jar"; //$NON-NLS-1$ //$NON-NLS-2$
						f = new File(agent);
						if (!f.exists()) {
							throw new LazyServiceURLResolveException("Management agent not found"); //$NON-NLS-1$
						}
					}
					agent = f.getCanonicalPath();
					vm.loadAgent(agent, "com.sun.management.jmxremote"); //$NON-NLS-1$
					Properties agentProps = vm.getAgentProperties();
					setAddress((String) agentProps.get(LocalJVMToolkit.LOCAL_CONNECTOR_ADDRESS_PROP));
					vm.detach();
					return null;
				}
			});
			future.get(TIMEOUT_THRESHOLD, TimeUnit.SECONDS);
		} catch (TimeoutException t) {
			throw new LazyServiceURLResolveException(ATTACH_TIMED_OUT_ERROR_MESSAGE, t);
		} catch (Exception x) {
			LazyServiceURLResolveException lsure = new LazyServiceURLResolveException(
					"Attach not supported for the JVM with PID " + pid //$NON-NLS-1$
							+ ". Try starting it with the jvm flag -Dcom.sun.management.jmxremote to start the local management agent", //$NON-NLS-1$
					x);
			throw lsure;
		}
	}

	private void setAddress(String address) {
		if (isSelfMonitoring() || address == null) {
			// If we're attempting to monitor ourselves, use the local
			// JVM (since the poor agent currently may go into infinite
			// recursion otherwise).
			try {
				url = ConnectionToolkit.createServiceURL(SELF_HOST_NAME, 0);
			} catch (MalformedURLException e) {
				// Not going to happen...
				LOGGER.log(Level.SEVERE, "Failed to parse url", e);
			}
		} else {
			try {
				url = new JMXServiceURL(address);
			} catch (MalformedURLException e) {
				BrowserAttachPlugin.getPluginLogger().log(Level.SEVERE,
						"Could not get create service URL from a local address!", e); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Tries firing up the management agent using JCMD style invocation.
	 *
	 * @throws AgentLoadException
	 * @throws IOException
	 * @throws AttachNotSupportedException
	 */
	private void tryJCMDStyleStartingOfTheAgent(String name) throws IOException, AgentLoadException {
		try {
			// Enforce a timeout here to ensure we don't block forever if the JVM is busy/suspended. See JMC-5398
			ExecutorService service = Executors.newSingleThreadExecutor();
			Future<Void> future = service.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					VirtualMachine vm = null;
					try {
						// First try getting some versioning information
						vm = VirtualMachine.attach(name);
						LocalJVMToolkit.executeCommandForPid(vm, name, "ManagementAgent.start_local"); //$NON-NLS-1$
						// Get in memory Service URL...
						JMXServiceURL inMemURL = LocalJVMToolkit.getInMemoryURLFromPID(Integer.parseInt(name));
						if (inMemURL == null) {
							BrowserAttachPlugin.getPluginLogger().log(Level.SEVERE,
									COULD_NOT_RETRIEVE_URL_ERROR_MESSAGE);
							throw new LazyServiceURLResolveException(COULD_NOT_RETRIEVE_URL_ERROR_MESSAGE);
						}
						url = inMemURL;
					} finally {
						if (vm != null) {
							vm.detach();
						}
					}
					return null;
				}
			});
			future.get(TIMEOUT_THRESHOLD, TimeUnit.SECONDS);
		} catch (TimeoutException t) {
			throw new LazyServiceURLResolveException(ATTACH_TIMED_OUT_ERROR_MESSAGE, t);
		} catch (Exception e) {
			throw new LazyServiceURLResolveException(COULD_NOT_RETRIEVE_URL_ERROR_MESSAGE);
		}
	}

	/**
	 * Overriddden to lazily establish a service URL if needed. If the JVM is attachable, it will
	 * attach to the JVM, start up the management server, and set up the URL. If the service URL is
	 * already established, it will be returned as is.
	 */
	@Override
	public JMXServiceURL createJMXServiceURL() throws IOException {
		if (url == null) {
			// First check if an agent has been started since last check...
			JMXServiceURL inMemURL = LocalJVMToolkit.getInMemoryURLFromPID(pid);
			if (inMemURL != null) {
				BrowserAttachPlugin.getPluginLogger().info("Found URL! No need to start an Agent!"); //$NON-NLS-1$
				url = inMemURL;
			} else if (isAutoStartAgent()) {
				if (!isAttachable()) {
					throw new LazyServiceURLResolveException(
							Messages.LocalConnectionDescriptor_ERROR_MESSAGE_ATTACH_NOT_SUPPORTED);
				}
				// Auto starting the agent to get the proper url...
				BrowserAttachPlugin.getPluginLogger().info("No URL found. Attempting to start the Agent!"); //$NON-NLS-1$
				startManagementServer();
			} else {
				throw new LazyServiceURLResolveException(
						Messages.LocalConnectionDescriptor_ERROR_AUTO_START_SWITCHED_OFF);
			}
		}
		return url;
	}

	private boolean isAutoStartAgent() {
		return isAutoStartAgent;
	}

	public int getPID() {
		return pid;
	}

	public boolean isAttachable() {
		return attachable;
	}

	private static final boolean fetchAutoStartAgentFromStore() {
		return BrowserAttachPlugin.getDefault().getPreferenceStore()
				.getBoolean(org.openjdk.jmc.browser.attach.preferences.PreferenceConstants.P_AUTO_START_AGENT);
	}

	@Override
	public Map<String, Object> getEnvironment() {
		return new HashMap<>(2);
	}

	@Override
	public String toString() {
		return "LocalConnectionDescriptor [PID=" + getPID() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
