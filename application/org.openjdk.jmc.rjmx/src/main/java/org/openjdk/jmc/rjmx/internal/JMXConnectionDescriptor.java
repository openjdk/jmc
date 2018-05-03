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
package org.openjdk.jmc.rjmx.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.openjdk.jmc.rjmx.ConnectionDescriptorBuilder;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.ui.common.security.ICredentials;
import org.openjdk.jmc.ui.common.security.SecurityException;

/**
 * This is the default implementation of {@link IConnectionDescriptor}.
 * <p>
 * The Connection Descriptor encapsulates the knowledge for how to connect to another JVM. The
 * connection information is immutable and will not change.
 * <p>
 * The extended properties collection is a set of application specific flags that will be persisted
 * with the connection. It is free to use and change at the application's discretion.
 * <p>
 * If you need to use the extended properties, please note that the key namespace org.openjdk.jmc.*
 * is reserved for JRPG applications.
 * <p>
 * A DefaultJMXConnectionDescriptor has many different properties that can be set. To facilitate the
 * creation of descriptors in an as easy way as possible, an internal builder class is provided.
 * Here is an example on how to create a descriptor that will simply communicate using a reference
 * to the local platform MBean server:
 * <p>
 * Please see {@link ConnectionDescriptorBuilder} for more information on the various options
 * available.
 */
public final class JMXConnectionDescriptor implements IConnectionDescriptor {

	/**
	 * The JMX service URL.
	 */
	private final JMXServiceURL url;

	private final ICredentials credentials;

	/**
	 * Full constructor.
	 */
	public JMXConnectionDescriptor(JMXServiceURL url, ICredentials credentials) {
		this.url = url;
		this.credentials = credentials;
	}

	@Override
	public JMXServiceURL createJMXServiceURL() throws IOException {
		return url;
	}

	@Override
	public Map<String, Object> getEnvironment() {
		Map<String, Object> env = new HashMap<>();
		try {
			String user = ""; //$NON-NLS-1$
			String pwd = ""; //$NON-NLS-1$
			if (credentials != null) {
				if (credentials.getUsername() != null) {
					user = credentials.getUsername();
				}
				if (credentials.getPassword() != null) {
					pwd = credentials.getPassword();
				}
			}
			String[] creArray = new String[2];
			creArray[0] = user;
			creArray[1] = pwd;
			env.put(JMXConnector.CREDENTIALS, creArray);

			// This is here for properly supporting t3 authentication...
			env.put(Context.SECURITY_PRINCIPAL, user);
			env.put(Context.SECURITY_CREDENTIALS, pwd);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		}
		return env;
	}

	@Override
	public String toString() {
		return url.toString();
	}
}
