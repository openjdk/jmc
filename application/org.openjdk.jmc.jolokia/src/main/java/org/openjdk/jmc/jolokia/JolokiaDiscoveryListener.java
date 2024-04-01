/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024, Kantega AS. All rights reserved.
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
package org.openjdk.jmc.jolokia;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.jolokia.client.jmxadapter.RemoteJmxAdapter;
import org.jolokia.service.discovery.JolokiaDiscovery;
import org.openjdk.jmc.common.jvm.JVMDescriptor;
import org.openjdk.jmc.jolokia.preferences.PreferenceConstants;

/**
 * Add found Jolokia instances to the JVM browser using the Jolokia discovery mechanism.
 * https://jolokia.org/reference/html/protocol.html#discovery
 */
public class JolokiaDiscoveryListener extends AbstractCachedDescriptorProvider implements PreferenceConstants {

	JolokiaDiscoverySettings settings;

	public JolokiaDiscoveryListener(JolokiaDiscoverySettings settings) {
		this.settings = settings;
	}

	public JolokiaDiscoveryListener() {
		this(JmcJolokiaPlugin.getDefault());
	}

	@Override
	protected Map<String, ServerConnectionDescriptor> discoverJvms() {
		Map<String, ServerConnectionDescriptor> found = new HashMap<>();
		if (!this.settings.shouldRunDiscovery()) {
			return found;
		}
		try {
			JolokiaDiscovery jolokiaDiscovery = new JolokiaDiscovery();
			jolokiaDiscovery.init(this.settings.getJolokiaContext());
			for (Object object : jolokiaDiscovery.lookupAgentsWithTimeoutAndMulticastAddress(
					this.settings.getDiscoveryTimeout(), this.settings.getMulticastGroup(),
					this.settings.getMulticastPort())) {
				try {

					@SuppressWarnings("unchecked")
					Map<String, ?> response = (Map<String, ?>) object;
					JVMDescriptor jvmInfo;
					try {// if it is connectable, see if we can get info from connection
						jvmInfo = JolokiaAgentDescriptor
								.attemptToGetJvmInfo(new RemoteJmxAdapter(String.valueOf(response.get("url")))); //$NON-NLS-1$
					} catch (Exception ignore) {
						jvmInfo = JolokiaAgentDescriptor.NULL_DESCRIPTOR;
					}
					JolokiaAgentDescriptor agentDescriptor = new JolokiaAgentDescriptor(response, jvmInfo);
					found.put(agentDescriptor.getGUID(), agentDescriptor);

				} catch (URISyntaxException ignore) {
				}
			}
		} catch (IOException ignore) {
		}
		return found;
	}

	@Override
	public String getDescription() {
		return Messages.JolokiaDiscoveryListener_Description;
	}

	@Override
	public String getName() {
		return "jolokia"; //$NON-NLS-1$
	}

	@Override
	protected boolean isEnabled() {
		return true;
	}

}
