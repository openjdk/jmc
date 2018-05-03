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
package org.openjdk.jmc.rjmx;

import java.net.MalformedURLException;

import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.rjmx.internal.JMXConnectionDescriptor;
import org.openjdk.jmc.ui.common.security.ICredentials;
import org.openjdk.jmc.ui.common.security.InMemoryCredentials;

/**
 * This class hides the complexities of building a default JMX over RMI ConnectionDescriptor in a
 * type safe way. It lets you specify various optional arguments, which are all checked upon
 * building the descriptor.
 */
public class ConnectionDescriptorBuilder {

	private JMXServiceURL url;
	private String hostName;
	private String username;
	private String password;
	private int port = DEFAULT_PORT;
	private ICredentials credentials;

	/**
	 * Port number designator meaning that the default port for the selected protocol should be
	 * used. It has the value -1.
	 */
	public final static int DEFAULT_PORT = -1;

	/**
	 * Creates a new builder initialized to default values.
	 */
	public ConnectionDescriptorBuilder() {
	}

	/**
	 * Sets the service URL.
	 *
	 * @param url
	 *            the {@link JMXServiceURL} to use. If the URL is set it will override any host and
	 *            port settings.
	 * @return the Builder currently being configured.
	 */
	public ConnectionDescriptorBuilder url(JMXServiceURL url) {
		this.url = url;
		return this;
	}

	/**
	 * Sets the host name.
	 *
	 * @param hostName
	 *            the host name to set.
	 * @return the Builder currently being configured.
	 */
	public ConnectionDescriptorBuilder hostName(String hostName) {
		this.hostName = hostName;
		return this;
	}

	/**
	 * Sets the port for the selected protocol. This will be the RMI Registry port for jmxrmi. For
	 * RMP it will be the management server port.
	 *
	 * @param port
	 *            port or {@link ConnectionDescriptorBuilder#DEFAULT_PORT} for the default port for
	 *            the selected protocol. Is {@link ConnectionDescriptorBuilder#DEFAULT_PORT} by
	 *            default.
	 * @return the Builder currently being configured.
	 */
	public ConnectionDescriptorBuilder port(int port) {
		this.port = port;
		return this;
	}

	/**
	 * Sets the credentials to use.
	 *
	 * @param credentials
	 *            the user credentials to use. Is by default {@code null}.
	 * @return the Builder currently being configured.
	 */
	public ConnectionDescriptorBuilder credentials(ICredentials credentials) {
		this.credentials = credentials;
		return this;
	}

	/**
	 * Sets the user name to use.
	 *
	 * @param username
	 *            the user name to use. Is by default {@code null}.
	 * @return the Builder currently being configured.
	 */
	public ConnectionDescriptorBuilder username(String username) {
		this.username = username;
		return this;
	}

	/**
	 * Sets the user password to use.
	 *
	 * @param password
	 *            the user password to use. Is by default {@code null}.
	 * @return the Builder currently being configured.
	 */
	public ConnectionDescriptorBuilder password(String password) {
		this.password = password;
		return this;
	}

	/**
	 * Builds the {@link IConnectionDescriptor}.
	 *
	 * @return a freshly created {@link IConnectionDescriptor} initialized as per the builder
	 *         settings.
	 * @throws IllegalStateException
	 *             if the settings were not sufficient to create a proper
	 *             {@link IConnectionDescriptor}.
	 */
	public IConnectionDescriptor build() throws IllegalStateException {
		if (credentials == null && username != null && password != null) {
			credentials = new InMemoryCredentials(username, password);
		}
		if (url == null && hostName == null) {
			throw new IllegalStateException("You must specify either the url or the host!"); //$NON-NLS-1$
		}
		if (url == null) {
			try {
				url = ConnectionToolkit.createServiceURL(hostName, port);
			} catch (MalformedURLException e) {
				IllegalStateException exception = new IllegalStateException(
						"Could not create a proper JMXServiceURL with the provided information."); //$NON-NLS-1$
				exception.initCause(e);
				throw exception;
			}
		}

		return new JMXConnectionDescriptor(url, credentials);
	}
}
