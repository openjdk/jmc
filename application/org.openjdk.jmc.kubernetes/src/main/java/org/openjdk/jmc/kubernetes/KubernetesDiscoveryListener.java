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
package org.openjdk.jmc.kubernetes;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.codec.binary.Base64;
import org.jolokia.client.J4pClient;
import org.jolokia.kubernetes.client.KubernetesJmxConnector;
import org.jolokia.server.core.osgi.security.AuthorizationHeaderParser;
import org.jolokia.server.core.util.Base64Util;
import org.openjdk.jmc.jolokia.AbstractCachedDescriptorProvider;
import org.openjdk.jmc.jolokia.JolokiaAgentDescriptor;
import org.openjdk.jmc.jolokia.ServerConnectionDescriptor;
import org.openjdk.jmc.kubernetes.preferences.KubernetesScanningParameters;
import org.openjdk.jmc.common.jvm.JVMDescriptor;
import org.openjdk.jmc.common.security.SecurityException;

import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.KubeConfigUtils;
import io.fabric8.kubernetes.client.utils.Utils;
import io.fabric8.kubernetes.client.dsl.AnyNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;

/**
 * This class attempts to connect to JVMs in pods running in kubernetes in a background thread.
 * Enablement and parameters for the scanning is given by user preferences.
 */
public class KubernetesDiscoveryListener extends AbstractCachedDescriptorProvider {

	private final static Pattern SECRET_PATTERN = Pattern
			.compile("\\$\\{kubernetes/secret/(?<secretName>[^/]+)/(?<itemName>[^\\}]+)}"); //$NON-NLS-1$
	private final static Pattern ATTRIBUTE_PATTERN = Pattern
			.compile("\\$\\{kubernetes/annotation/(?<annotationName>[^/]+)}"); //$NON-NLS-1$
	private final static Set<String> VALID_JOLOKIA_PROTOCOLS = new HashSet<>(Arrays.asList("http", "https")); //$NON-NLS-1$ //$NON-NLS-2$

	KubernetesScanningParameters settings;

	public KubernetesDiscoveryListener() {
		this(JmcKubernetesPlugin.getDefault());
	}

	//Public constructor in order for test plugin to be able to rig tests in an easier manner
	public KubernetesDiscoveryListener(KubernetesScanningParameters parameters) {
		this.settings = parameters;
	}

	public final String getDescription() {
		return Messages.KubernetesDiscoveryListener_Description;
	}

	@Override
	public String getName() {
		return "kubernetes"; //$NON-NLS-1$
	}

	boolean notEmpty(String value) {
		return value != null && value.length() > 0;
	}

	private List<String> contexts;
	private long contextsCached = 0L;

	private List<String> allContexts() throws IOException {
		final String path = Utils.getSystemPropertyOrEnvVar(Config.KUBERNETES_KUBECONFIG_FILE,
				new File(System.getProperty("user.home"), ".kube" + File.separator + "config").toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		File configPath = new File(path);
		if (contexts != null && contextsCached > configPath.lastModified()) {// the YAML parsing is soo incredibly
																				// sloow, hence cache context names for
																				// later
																				// runs
			return contexts;
		}
		// reload config if kubeconfig has been modified since we cached the config
		io.fabric8.kubernetes.api.model.Config config = KubeConfigUtils.parseConfig(configPath);
		this.contextsCached = System.currentTimeMillis();
		KubernetesJmxConnector.resetKubernetesConfig();
		return contexts = config.getContexts().stream().map(NamedContext::getName).collect(Collectors.toList());
	}

	@Override
	protected Map<String, ServerConnectionDescriptor> discoverJvms() {
		return scanForJvms();
	}

	Map<String, ServerConnectionDescriptor> scanForJvms() {
		Map<String, ServerConnectionDescriptor> found = new HashMap<>();
		if (!isEnabled()) {
			return found;
		}
		boolean hasScanned = false;

		if (settings.scanAllContexts()) {
			try {
				for (final String context : allContexts()) {
					hasScanned = true;
					scanContext(found, settings, context);
				}
			} catch (IOException e) {
				settings.logError(Messages.KubernetesDiscoveryListener_UnableToFindContexts, e);
			}
		}
		if (!hasScanned) {// scan default context
			return scanContext(found, settings, null);
		}
		return found;
	}

	private Map<String, ServerConnectionDescriptor> scanContext(
		Map<String, ServerConnectionDescriptor> found, KubernetesScanningParameters parameters, String context) {
		try {
			scanContextUnsafe(found, parameters, context);
		} catch (Exception e) {
			parameters.logError(Messages.KubernetesDiscoveryListener_UnableToScan + context, e);
		}
		return found;
	}

	private Map<String, ServerConnectionDescriptor> scanContextUnsafe(
		Map<String, ServerConnectionDescriptor> found, KubernetesScanningParameters parameters, String context) {
		String pathLabel = parameters.requireLabel();
		KubernetesClient client = KubernetesJmxConnector.getApiClient(context);

		AnyNamespaceOperation<Pod, PodList, PodResource> query = client.pods().inAnyNamespace();
		List<Pod> podList;
		boolean hasPathLabel = notEmpty(pathLabel);
		if (hasPathLabel) {
			podList = query.withLabel(pathLabel).list().getItems();
		} else {
			podList = query.list().getItems();
		}
		// can consider parallelization for big contexts, however since it is the
		// background await the situation a bit
		podList.stream().forEach(pod -> scanPod(found, parameters, context, client, pod));
		return found;
	}

	private void scanPod(
		Map<String, ServerConnectionDescriptor> found, KubernetesScanningParameters parameters, String context,
		KubernetesClient client, Pod pod) {

		final ObjectMeta metadata = pod.getMetadata();
		HashMap<String, String> headers = new HashMap<>();
		Map<String, Object> env = new HashMap<>();
		try {
			if (notEmpty(parameters.username())) {
				if (!notEmpty(parameters.password())) {
					throw new IllegalArgumentException(Messages.KubernetesDiscoveryListener_MustProvidePassword);
				}
				authorize(headers, client, parameters.username(), parameters.password(), metadata.getNamespace(), env);
			}
		} catch (SecurityException e) {
			// skipping authorization if anything fails
		}
		final StringBuilder url = new StringBuilder("/api/").append(pod.getApiVersion()).append("/namespaces/")
				.append(metadata.getNamespace()).append("/pods/");
		// JMX url must be reverse constructed, so that we can connect from the
		// resulting node in the JVM browser
		final StringBuilder jmxUrl = new StringBuilder("service:jmx:kubernetes:///").append(metadata.getNamespace()) //$NON-NLS-1$
				.append('/');

		final String protocol = getValueOrAttribute(parameters.jolokiaProtocol(), metadata);
		final String podName = metadata.getName();
		if (notEmpty(protocol)) {
			if (!VALID_JOLOKIA_PROTOCOLS.contains(protocol)) {
				throw new IllegalArgumentException(Messages.KubernetesDiscoveryListener_JolokiaProtocol + protocol
						+ Messages.KubernetesDiscoveryListener_HttpOrHttps);
			}
			// a bit clumsy, need to inject protocol _before_ podname in selflink
			url.append(protocol).append(':');
			jmxUrl.append(protocol).append(':');
		}

		jmxUrl.append(podName);
		url.append(podName);

		final String port = getValueOrAttribute(parameters.jolokiaPort(), metadata);
		if (port != null) {
			url.append(":").append(port); //$NON-NLS-1$
			jmxUrl.append(':').append(port);
		}

		url.append("/proxy"); //$NON-NLS-1$

		final String path = getValueOrAttribute(parameters.jolokiaPath(), metadata);

		if (!path.startsWith("/")) { //$NON-NLS-1$
			url.append('/');
			jmxUrl.append('/');
		}
		url.append(path);
		jmxUrl.append(path);

		if (context != null) {
			env.put(KubernetesJmxConnector.KUBERNETES_CLIENT_CONTEXT, context);
		}
		J4pClient jvmClient = KubernetesJmxConnector.probeProxyPath(env, client, url, headers);
		if (jvmClient != null) {
			JmcKubernetesJmxConnection connection;
			try {
				connection = new JmcKubernetesJmxConnection(jvmClient);
				JVMDescriptor jvmDescriptor = JolokiaAgentDescriptor.attemptToGetJvmInfo(connection);
				JMXServiceURL jmxServiceURL = new JMXServiceURL(jmxUrl.toString());
				KubernetesJvmDescriptor descriptor = new KubernetesJvmDescriptor(metadata, jvmDescriptor, jmxServiceURL,
						env);
				found.put(descriptor.getGUID(), descriptor);
			} catch (IOException e) {
				parameters.logError(Messages.KubernetesDiscoveryListener_ErrConnectingToJvm, e);

			}
		}
	}

	private String getValueOrAttribute(String configValue, ObjectMeta metadata) {
		if (notEmpty(configValue)) {
			Matcher pattern = ATTRIBUTE_PATTERN.matcher(configValue);
			if (pattern.find()) {
				return metadata.getAnnotations().get(pattern.group("annotationName")); //$NON-NLS-1$
			} else {
				return configValue;// the default is to use config value as is
			}
		}
		return null;
	}

	private void authorize(
		HashMap<String, String> headers, KubernetesClient client, String username, String password, String namespace,
		Map<String, Object> jmxEnv) {

		final Matcher userNameMatcher = SECRET_PATTERN.matcher(username);
		String secretName = null;
		Map<String, String> secretValues = null;
		if (userNameMatcher.find()) {
			secretName = userNameMatcher.group("secretName"); //$NON-NLS-1$
			secretValues = findSecret(client, namespace, secretName);
			username = secretValues.get(userNameMatcher.group("itemName")); //$NON-NLS-1$
		}

		final Matcher passwordMatcher = SECRET_PATTERN.matcher(password);
		if (passwordMatcher.find()) {
			if (!secretName.equals(passwordMatcher.group("secretName"))) { //$NON-NLS-1$
				secretValues = findSecret(client, namespace, passwordMatcher.group("secretName")); //$NON-NLS-1$
			}
			password = secretValues.get(passwordMatcher.group("itemName")); //$NON-NLS-1$
		}

		headers.put(AuthorizationHeaderParser.JOLOKIA_ALTERNATE_AUTHORIZATION_HEADER,
				"Basic " + Base64Util.encode((username + ":" + password).getBytes())); //$NON-NLS-1$ //$NON-NLS-2$
		jmxEnv.put(JMXConnector.CREDENTIALS, new String[] {username, password});

	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private Map<String, String> findSecret(KubernetesClient client, String namespace, String secretName) {

		for (Secret secret : client.secrets().inNamespace(namespace).list().getItems()) {
			if (secret.getMetadata().getName().equals(secretName)) {
				if ("kubernetes.io/basic-auth".equals(secret.getType())) { //$NON-NLS-1$
					Map<String, String> data = secret.getData();
					data.replaceAll((key, value) -> new String(Base64.decodeBase64(value)));
					return data;
				} else if ("Opaque".equals(secret.getType())) { //$NON-NLS-1$
					for (Entry<String, String> entry : secret.getData().entrySet()) {
						if (entry.getKey().endsWith(".properties")) { //$NON-NLS-1$
							try {
								Properties properties = new Properties();
								properties.load(new ByteArrayInputStream(Base64.decodeBase64(entry.getValue())));
								return (Map) properties;
							} catch (IOException ignore) {
							}
						}
					}
				}
			}

		}
		throw new NoSuchElementException(Messages.KubernetesDiscoveryListener_CouldNotFindSecret + secretName
				+ Messages.KubernetesDiscoveryListener_InNamespace + namespace);

	}

	@Override
	protected boolean isEnabled() {
		return this.settings.scanForInstances();
	}
}
