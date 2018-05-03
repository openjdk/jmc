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
package org.openjdk.jmc.browser.jdp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Level;

import javax.management.remote.JMXServiceURL;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.openjdk.jmc.browser.jdp.preferences.PreferenceConstants;
import org.openjdk.jmc.jdp.client.DiscoveryEvent;
import org.openjdk.jmc.jdp.client.DiscoveryListener;
import org.openjdk.jmc.jdp.client.JDPClient;
import org.openjdk.jmc.jdp.jmx.JMXDataKeys;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.rjmx.descriptorprovider.AbstractDescriptorProvider;
import org.openjdk.jmc.rjmx.descriptorprovider.IDescriptorListener;
import org.openjdk.jmc.ui.common.jvm.Connectable;
import org.openjdk.jmc.ui.common.jvm.JVMArch;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;
import org.openjdk.jmc.ui.common.jvm.JVMType;
import org.openjdk.jmc.ui.misc.DialogToolkit;

/**
 * Provides JDP discovered descriptors.
 */
public class JDPDescriptorProvider extends AbstractDescriptorProvider implements IPropertyChangeListener {
	private JDPClient jdpClient;
	private final JDPDiscoveryListener discoveryListener = new JDPDiscoveryListener();

	public JDPDescriptorProvider() {
		JDPPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}

	/**
	 * Class handling the incoming JDP events.
	 */
	private class JDPDiscoveryListener implements DiscoveryListener {
		private static final String PATH_SEPARATOR = "/"; //$NON-NLS-1$

		@Override
		public void onDiscovery(DiscoveryEvent event) {
			switch (event.getKind()) {
			case LOST:
				onDescriptorRemoved(event.getDiscoverable().getSessionId());
				break;
			case CHANGED:
			case FOUND:
			default:
				Map<String, String> map = event.getDiscoverable().getPayload();
				String name = map.get(JMXDataKeys.KEY_INSTANCE_NAME);
				String url = map.get(JMXDataKeys.KEY_JMX_SERVICE_URL);
				String commandLine = map.get(JMXDataKeys.KEY_JAVA_COMMAND);
				String pid = map.get(JMXDataKeys.KEY_PID);
				// NOTE: We would like to have the JVM type and architecture included in the JDP payload. We should probably file an enhancement request on JDK for this.
				JVMDescriptor jvmInfo = new JVMDescriptor(null, JVMType.UNKNOWN, JVMArch.UNKNOWN, commandLine, null,
						pid == null ? null : Integer.parseInt(pid), false, Connectable.MGMNT_AGENT_STARTED);
				String path = null;
				if (name == null) {
				} else if (name.endsWith(PATH_SEPARATOR)) {
					path = name;
					name = null;
				} else {
					int index = name.lastIndexOf('/');
					if (index != -1) {
						path = name.substring(0, index);
						name = name.substring(index + 1);
					}
				}
				IServerDescriptor sd = IServerDescriptor.create(event.getDiscoverable().getSessionId(), name, jvmInfo);
				try {
					onDescriptorDetected(sd, path, new JMXServiceURL(url), null);
				} catch (Exception e) {
					JDPPlugin.getDefault().getLogger().log(Level.SEVERE, "Got broken event from JDP: " + url, e); //$NON-NLS-1$
				}
			}
		}
	}

	@Override
	public String getName() {
		return Messages.JDPDescriptorProvider_PROVIDER_NAME;
	}

	@Override
	public String getDescription() {
		return Messages.JDPDescriptorProvider_PROVIDER_DESCRIPTION;
	}

	private synchronized void startClient() {
		if (jdpClient == null) {
			String addressStr = JDPPlugin.getDefault().getPreferenceStore()
					.getString(PreferenceConstants.PROPERTY_KEY_JDP_ADDRESS);
			InetAddress address;
			try {
				address = InetAddress.getByName(addressStr);
			} catch (UnknownHostException e) {
				DialogToolkit.showWarningDialogAsync(PlatformUI.getWorkbench().getDisplay(),
						Messages.JDPDescriptorProvider_COULD_NOT_RESOLVE_HOST_TITLE,
						NLS.bind(Messages.JDPDescriptorProvider_COULD_NOT_RESOLVE_HOST_TEXT, addressStr));
				JDPPlugin.getDefault().getLogger().info("Could not resolve address for JDP: " + addressStr); //$NON-NLS-1$
				return;
			}
			jdpClient = new JDPClient(address, getPort(), getHeartBeatTimeout() * 1000);
			jdpClient.addDiscoveryListener(discoveryListener);
			try {
				jdpClient.start();
			} catch (IOException e) {
				jdpClient.stop();
				jdpClient = null;
				JDPPlugin.getDefault().getLogger().log(Level.SEVERE,
						"Could not start the JDP client. JDP discovery will not be possible for this session. One possible reason for this error can be that no network is available.", //$NON-NLS-1$
						e);
			}
		}
	}

	private synchronized void shutDownClient() {
		if (jdpClient != null) {
			jdpClient.removeDiscoveryListener(discoveryListener);
			jdpClient.stop();
			jdpClient = null;
		}
	}

	private int getHeartBeatTimeout() {
		return JDPPlugin.getDefault().getPreferenceStore().getInt(PreferenceConstants.PROPERTY_KEY_HEART_BEAT_TIMEOUT);
	}

	private int getPort() {
		return JDPPlugin.getDefault().getPreferenceStore().getInt(PreferenceConstants.PROPERTY_KEY_JDP_PORT);
	}

	@Override
	public void addDescriptorListener(IDescriptorListener l) {
		synchronized (m_descriptorListeners) {
			if (m_descriptorListeners.size() == 0) {
				super.addDescriptorListener(l);
				startClient();
				return;
			}
			super.addDescriptorListener(l);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		restartJDPClient();
	}

	private synchronized void restartJDPClient() {
		shutDownClient();
		startClient();
	}

}
