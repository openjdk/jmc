/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Kantega AS. All rights reserved.
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
package org.openjdk.jmc.kubernetes;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

import javax.management.InstanceNotFoundException;

import org.jolokia.client.JolokiaClient;
import org.jolokia.client.exception.JolokiaException;
import org.jolokia.client.exception.JolokiaRemoteException;
import org.jolokia.client.response.JolokiaResponse;
import org.openjdk.jmc.jolokia.JmcJolokiaJmxConnection;
import org.openjdk.jmc.rjmx.common.ConnectionException;

/**
 * Jolokia based MBeanServerConnector tailored for JMC needs
 */
public class JmcKubernetesJmxConnection extends JmcJolokiaJmxConnection {

	static final Collection<Pattern> DISCONNECT_SIGNS = Arrays.asList(Pattern.compile("Error: pods \".+\" not found")); //$NON-NLS-1$

	public JmcKubernetesJmxConnection(JolokiaClient client) throws IOException {
		super(client);
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected JolokiaResponse unwrapException(JolokiaException e) throws IOException, InstanceNotFoundException {
		// recognize signs of disconnect and signal to the application for better handling
		if (isKnownDisconnectException(e)) {
			throw new ConnectionException(e.getMessage());
		} else {
			return super.unwrapException(e);
		}
	}

	private boolean isKnownDisconnectException(JolokiaException e) {
		if (!(e instanceof JolokiaRemoteException)) {
			return false;
		}
		if (!"io.fabric8.kubernetes.client.KubernetesClientException" //$NON-NLS-1$
				.equals(((JolokiaRemoteException) e).getErrorType())) {
			return false;
		}
		return DISCONNECT_SIGNS.stream().anyMatch(pattern -> pattern.matcher(e.getMessage()).matches());
	}

}
