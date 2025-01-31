/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024, 2025, Kantega AS. All rights reserved.
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
package org.openjdk.jmc.jolokia;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.common.jvm.JVMDescriptor;

/**
 * Provide data about JVMs accessed over Jolokia for the JVM browser
 */
public class JolokiaAgentDescriptor implements ServerConnectionDescriptor {

	private final JMXServiceURL serviceUrl;
	private final Map<String, ?> agentData;

	public JolokiaAgentDescriptor(Map<String, ?> agentData) throws URISyntaxException, MalformedURLException {
		super();
		URI uri = new URI((String) agentData.get("url")); //$NON-NLS-1$
		this.serviceUrl = new JMXServiceURL(
				String.format("service:jmx:jolokia://%s:%s%s", uri.getHost(), uri.getPort(), uri.getPath())); //$NON-NLS-1$
		this.agentData = agentData;
	}

	JMXServiceURL getServiceUrl() {
		return serviceUrl;
	}

	@Override
	public String getGUID() {
		return String.valueOf(agentData.get("agent_id")); //$NON-NLS-1$
	}

	@Override
	public String getDisplayName() {
		return String.valueOf(agentData.get("agent_id")); //$NON-NLS-1$
	}

	@Override
	public JVMDescriptor getJvmInfo() {
		//Note: From discovery we may know a bit about the target JVM, however the presence of JVM info
		//is interpreted as a local JVM by AgentJmxHelper.isLocalJvm() hence the JMC Agent will be available 
		//which does not make sense over this protocol
		return null;
	}

	@Override
	public JMXServiceURL createJMXServiceURL() throws IOException {
		return serviceUrl;
	}

	@Override
	public Map<String, Object> getEnvironment() {
		return new HashMap<>();
	}

	@Override
	public String getPath() {
		return null;
	}

	@Override
	public JMXServiceURL serviceUrl() {
		return this.serviceUrl;
	}

}
