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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;

import org.jolokia.client.JolokiaClient;
import org.jolokia.kubernetes.client.KubernetesJmxConnector;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.http.HttpResponse;

public class JmcKubernetesJmxConnector extends KubernetesJmxConnector {

	private static final Pattern POD_PATTERN = Pattern
			.compile("/?(?<namespace>[^/]+)/(?<protocol>https?:)?(?<podPattern>[^/^:]+)(?<port>:[^/]+)?/(?<path>.+)");

	public JmcKubernetesJmxConnector(JMXServiceURL serviceURL, Map<String, ?> environment) throws IOException {
		super(serviceURL, environment);
	}

	@Override
	public MBeanServerConnection getMBeanServerConnection() {
		return new JmcKubernetesJmxConnection(super.getMBeanServerConnection());
	}

	@Override
	public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) {
		return new JmcKubernetesJmxConnection(super.getMBeanServerConnection(delegationSubject));
	}

	@Override
	protected JolokiaClient expandAndProbeUrl(KubernetesClient client, Map<String, Object> env)
			throws MalformedURLException {
		String proxyPath = this.serviceUrl.getURLPath();
		JolokiaClient connection;
		final HashMap<String, String> headersForProbe = createHeadersForProbe(env);
		try {
			if (POD_PATTERN.matcher(proxyPath).matches()) {
				final Matcher matcher = POD_PATTERN.matcher(proxyPath);
				if (matcher.find()) {
					String namespace = matcher.group("namespace");
					String podPattern = matcher.group("podPattern");
					String path = matcher.group("path");
					String protocol = matcher.group("protocol");
					String port = matcher.group("port");
					final Pod exactPod = client.pods().inNamespace(namespace).withName(podPattern).get();
					// check if podname pans out directly
					if (exactPod != null && (connection = probeProxyPath(env, client,
							buildProxyPath(exactPod, protocol, port, path), headersForProbe)) != null) {
						return connection;
					} else { // scan through pods in namespace if podname is a pattern

						for (final Pod pod : client.pods().inNamespace(namespace).list().getItems()) {
							if (pod.getMetadata().getName().matches(podPattern)) {
								if ((connection = probeProxyPath(env, client, buildProxyPath(pod, protocol, port, path),
										headersForProbe)) != null) {
									return connection;
								}
							}
						}
					}
				}
			}
		} catch (KubernetesClientException ignore) {
		}
		throw new MalformedURLException("Unable to connect to proxypath " + proxyPath);

	}

	private static HashMap<String, String> createHeadersForProbe(Map<String, Object> env) {
		final HashMap<String, String> headers = new HashMap<>();
		String[] credentials = (String[]) env.get(JMXConnector.CREDENTIALS);
		if (credentials != null) {
			MinimalHttpClientAdapter.authenticate(headers, credentials[0], credentials[1]);
		}
		return headers;
	}

	/**
	 * Probe whether we find Jolokia in given namespace, pod and path
	 */
	public static JolokiaClient probeProxyPath(
		Map<String, Object> env, KubernetesClient client, StringBuilder url, HashMap<String, String> headers) {
		try {
			final String proxyPath = url.toString();
			HttpResponse<byte[]> response = MinimalHttpClientAdapter.performRequest(client, proxyPath,
					"{\"type\":\"version\"}".getBytes(), null, headers);
			if (response.isSuccessful()) {
				// JDK HttpRequest.newBuilder().uri(...) requires an absolute URI with a scheme,
				// even though the adapter routes through fabric8 (urlPath) and ignores the URI.
				String master = client.getMasterUrl().toString();
				if (master.endsWith("/")) {
					master = master.substring(0, master.length() - 1);
				}
				URI proxyUri = URI.create(master + proxyPath);
				return new JolokiaClient(proxyUri, new MinimalHttpClientAdapter(client, proxyPath, env));
			}
		} catch (IOException | InterruptedException | ExecutionException | URISyntaxException ignore) {
		}
		return null;
	}
}
