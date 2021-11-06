/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Kantega AS. All rights reserved. 
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
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularDataSupport;
import javax.management.remote.JMXServiceURL;

import org.jolokia.client.jmxadapter.RemoteJmxAdapter;
import org.json.simple.JSONObject;
import org.openjdk.jmc.ui.common.jvm.Connectable;
import org.openjdk.jmc.ui.common.jvm.JVMArch;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;
import org.openjdk.jmc.ui.common.jvm.JVMType;

public class JolokiaAgentDescriptor implements ServerConnectionDescriptor {

	public static final JVMDescriptor NULL_DESCRIPTOR = new JVMDescriptor(null, null, null,null, null, null, null, null, false,
			Connectable.UNKNOWN);
	private final JMXServiceURL serviceUrl;
	private final JSONObject agentData;
	private final JVMDescriptor jvmDescriptor;

	public JolokiaAgentDescriptor(JSONObject agentData, JVMDescriptor jvmDescriptor)
			throws URISyntaxException, MalformedURLException {
		super();
		URI uri = new URI((String) agentData.get("url")); //$NON-NLS-1$
		this.serviceUrl = new JMXServiceURL(
				String.format("service:jmx:jolokia://%s:%s%s", uri.getHost(), uri.getPort(), uri.getPath())); //$NON-NLS-1$
		this.agentData = agentData;
		this.jvmDescriptor = jvmDescriptor;
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
		return this.jvmDescriptor;
	}

	/**
	 * Best effort to extract JVM information from a connection if everything works.
	 * Can be adjusted to support different flavors of JVM
	 */
	public static JVMDescriptor attemptToGetJvmInfo(RemoteJmxAdapter adapter) {

		try {
			AttributeList attributes = adapter.getAttributes(new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME),
					new String[] { "Pid", "Name", "InputArguments", "SystemProperties" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			Integer pid = null;
			String arguments=null, javaCommand=null, javaVersion=null, vmName=null, vmVendor = null;
			boolean isDebug = false;
			JVMType type = JVMType.UNKNOWN;
			JVMArch arch = JVMArch.UNKNOWN;
			for (Attribute attribute : attributes.asList()) {
				// newer JVM have pid as separate attribute, older have to parse from name
				if (attribute.getName().equalsIgnoreCase("Pid")) { //$NON-NLS-1$
					try {
						pid = Integer.valueOf(String.valueOf(attribute.getValue()));
					} catch (NumberFormatException ignore) {
					}
				} else if (attribute.getName().equalsIgnoreCase("Name") && pid == null) { //$NON-NLS-1$
					String pidAndHost = String.valueOf(attribute.getValue());
					int separator = pidAndHost.indexOf('@');
					if (separator > 0) {
						try {
							pid = Integer.valueOf(pidAndHost.substring(0, separator));
						} catch (NumberFormatException e) {
						}
					}
				} else if (attribute.getName().equalsIgnoreCase("InputArguments")) { //$NON-NLS-1$

					if (attribute.getValue() instanceof String[]) {
						arguments = Arrays.toString((String[]) attribute.getValue());
					} else {
						arguments = String.valueOf(attribute.getValue());
					}
					if (arguments.contains("-agentlib:jdwp")) { //$NON-NLS-1$
						isDebug = true;
					}
				} else if (attribute.getName().equalsIgnoreCase("SystemProperties") //$NON-NLS-1$
						&& attribute.getValue() instanceof TabularDataSupport) {
					TabularDataSupport systemProperties = (TabularDataSupport) attribute.getValue();

					// quite clumsy: iterate over properties as we need to use the exact key, which is non trivial
					// to reproduce
					for (Object entry : systemProperties.values()) {
						String key = ((CompositeDataSupport) entry).get("key").toString(); //$NON-NLS-1$
						String value = ((CompositeDataSupport) entry).get("value").toString(); //$NON-NLS-1$
						if (key.equalsIgnoreCase("sun.management.compiler")) { //$NON-NLS-1$
							if (value.toLowerCase().contains("hotspot")) { //$NON-NLS-1$
								type = JVMType.HOTSPOT;
							}
						} else if (key.equalsIgnoreCase("sun.arch.data.model")) { //$NON-NLS-1$
							String archIndicator = value;
							if (archIndicator.contains("64")) { //$NON-NLS-1$
								arch = JVMArch.BIT64;
							} else if (archIndicator.contains("32")) { //$NON-NLS-1$
								arch = JVMArch.BIT32;
							}
						} else if (key.equalsIgnoreCase("sun.java.command")) { //$NON-NLS-1$
							javaCommand = value;
						} else if(key.equalsIgnoreCase("java.version")) { //$NON-NLS-1$
							javaVersion = value;
						} else if (key.equalsIgnoreCase("java.vm.name")) { //$NON-NLS-1$
							vmName=value;
						} else if(key.equalsIgnoreCase("java.vm.vendor")) { //$NON-NLS-1$
							vmVendor=value;
						}
					}

				}

			}
			return new JVMDescriptor(javaVersion, type, arch, javaCommand, arguments, vmName, vmVendor, pid, isDebug,
					Connectable.UNKNOWN);

		} catch (RuntimeException|IOException|InstanceNotFoundException|MalformedObjectNameException ignore) {
			return NULL_DESCRIPTOR;
		}

	}

	@Override
	public JMXServiceURL createJMXServiceURL() throws IOException {
		return serviceUrl;
	}

	@Override
	public Map<String, Object> getEnvironment() {
		return null;
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