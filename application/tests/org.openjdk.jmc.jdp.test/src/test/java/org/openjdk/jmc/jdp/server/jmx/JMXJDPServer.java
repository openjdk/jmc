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
package org.openjdk.jmc.jdp.server.jmx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.jdp.common.Configuration;
import org.openjdk.jmc.jdp.jmx.JMXDataKeys;
import org.openjdk.jmc.jdp.server.JDPServer;

/**
 * Specialization of the general JDP Server to broadcast the JMX service URL and information needed
 * to support Mission Control.
 */
@SuppressWarnings("nls")
public class JMXJDPServer extends JDPServer {
	private final static String JAVA_COMMAND = retrieveJavaCommand();

	public JMXJDPServer(String discoverableID, Configuration configuration, Map<String, String> discoveryData) {
		super(discoverableID, configuration);
		setDiscoveryData(discoveryData);
	}

	public JMXJDPServer(Configuration configuration, Map<String, String> discoveryData) {
		this(generateUniqueID(), configuration, discoveryData);
	}

	public JMXJDPServer(String discoverableID, Configuration configuration, JMXServiceURL agentURL, String name) {
		super(discoverableID, configuration);
		setDiscoveryData(createData(agentURL, name));
	}

	public JMXJDPServer(Configuration configuration, JMXServiceURL agentURL, String name) {
		this(generateUniqueID(), configuration, agentURL, name);
	}

	private static String generateUniqueID() {
		return UUID.randomUUID().toString();
	}

	private Map<String, String> createData(JMXServiceURL agentURL, String name) {
		Map<String, String> discoveryData = new HashMap<>();
		discoveryData.put(JMXDataKeys.KEY_PID, getPID());
		discoveryData.put(JMXDataKeys.KEY_JAVA_COMMAND, JAVA_COMMAND);
		discoveryData.put(JMXDataKeys.KEY_JMX_SERVICE_URL, agentURL.toString());
		if (name != null) {
			discoveryData.put(JMXDataKeys.KEY_INSTANCE_NAME, name);
		}
		return discoveryData;
	}

	private static String retrieveJavaCommand() {
		// This one is usually missing when running with a custom launcher...
		String javaCommand = System.getProperty("sun.java.command");

		// ... so let's use the class path as backup. This is testing, and we just need something indicative of what the heck is running...
		if (javaCommand == null) {
			javaCommand = System.getProperty("java.class.path");
		}

		return javaCommand;
	}

	public static String getJavaCommand() {
		return JAVA_COMMAND;
	}

	public static String getPID() {
		return PIDHelper.getPID();
	}
}
