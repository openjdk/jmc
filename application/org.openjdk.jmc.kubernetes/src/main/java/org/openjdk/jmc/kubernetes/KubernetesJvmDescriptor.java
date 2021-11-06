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
package org.openjdk.jmc.kubernetes;

import java.io.IOException;
import java.util.Map;

import javax.management.remote.JMXServiceURL;

import org.jolokia.kubernetes.client.KubernetesJmxConnector;
import org.openjdk.jmc.jolokia.ServerConnectionDescriptor;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;

import io.fabric8.kubernetes.api.model.ObjectMeta;

public class KubernetesJvmDescriptor implements ServerConnectionDescriptor {
	
	private final JVMDescriptor jvmDescriptor;
	private final ObjectMeta metadata;
	private final Map<String, Object> env;
	private final JMXServiceURL connectUrl;
	
	public KubernetesJvmDescriptor(ObjectMeta metadata, JVMDescriptor jvmDescriptor, JMXServiceURL connectUrl, Map<String, Object> env) {
		this.jvmDescriptor = jvmDescriptor;
		this.metadata = metadata;	
		this.env = env;
		this.connectUrl = connectUrl;
	}

	@Override
	public String getGUID() {
		return this.metadata.getName();
	}

	@Override
	public String getDisplayName() {
		return this.metadata.getName();
	}

	@Override
	public JVMDescriptor getJvmInfo() {
		return this.jvmDescriptor;
	}


	public String getPath() {
		String namespace = metadata.getNamespace();
		final Object context=this.env.get(KubernetesJmxConnector.KUBERNETES_CLIENT_CONTEXT);
		if(context!=null) {
			return context + "/" + namespace; //$NON-NLS-1$
		}
		return namespace;
	}


	@Override
	public JMXServiceURL createJMXServiceURL() throws IOException {
		return this.connectUrl;
	}


	@Override
	public Map<String, Object> getEnvironment() {
		return this.env;
	}


	@Override
	public JMXServiceURL serviceUrl() {
		return this.connectUrl;
	}

}
