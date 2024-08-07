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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.jvm.JVMType;
import org.openjdk.jmc.common.security.ICredentials;
import org.openjdk.jmc.common.security.InMemoryCredentials;
import org.openjdk.jmc.common.security.SecurityException;
import org.openjdk.jmc.kubernetes.preferences.KubernetesScanningParameters;
import org.openjdk.jmc.rjmx.common.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.common.IServerDescriptor;
import org.openjdk.jmc.rjmx.descriptorprovider.IDescriptorListener;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * Test that JMX connections done with JmcKubernetesJmxConnectionProvider are functional. In order
 * to be able to test this in a contained environment, the kubernetes API is mocked with wiremock.
 */
@SuppressWarnings("restriction")
public class JmcKubernetesTest {

	static class TestParameters implements KubernetesScanningParameters {
		public boolean scanForInstances, scanAllContexts;
		public String jolokiaPort, jolokiaPath = "/jolokia/", jolokiaProtocol, requireLabel;
		public InMemoryCredentials credentials;

		@Override
		public boolean scanForInstances() {
			return this.scanForInstances;
		}

		@Override
		public boolean scanAllContexts() {
			return this.scanAllContexts;
		}

		@Override
		public String jolokiaPort() {
			return this.jolokiaPort;
		}

		@Override
		public String username() throws SecurityException {
			return this.credentials == null ? null : this.credentials.getUsername();
		}

		@Override
		public String password() throws SecurityException {
			return this.credentials == null ? null : this.credentials.getPassword();
		}

		@Override
		public String jolokiaPath() {
			return this.jolokiaPath;
		}

		@Override
		public String jolokiaProtocol() {
			return this.jolokiaProtocol;
		}

		@Override
		public String requireLabel() {
			return this.requireLabel;
		}

		@Override
		public ICredentials storeCredentials(String username, String password) throws SecurityException {
			return this.credentials = new InMemoryCredentials(username, password);
		}

		@Override
		public void logError(String message, Throwable error) {
			System.out.println(message);
			error.printStackTrace(System.out);
		}
	}

	@ClassRule
	public static WireMockRule wiremock = new WireMockRule(
			WireMockConfiguration.options().extensions(new ResponseTemplateTransformer(false)).port(0));

	static final String jolokiaUrl = "service:jmx:kubernetes:///ns1/pod-abcdef/jolokia";

	private static MBeanServerConnection jolokiaConnection;

	@BeforeClass
	public static void connect() throws Exception {
		CloseableHttpResponse configResponse = HttpClients.createDefault()
				.execute(new HttpGet(wiremock.baseUrl() + "/mock-kube-config.yml"));
		Assert.assertEquals(configResponse.getStatusLine().getStatusCode(), 200);
		File configFile = File.createTempFile("mock-kube-config", ".yml");
		configResponse.getEntity().writeTo(new FileOutputStream(configFile));
		// we set this so the KubernetesDiscoveryListener will work
		//Setting taken from: https://github.com/fabric8io/kubernetes-client/blob/77a65f7d40f31a5dc37492cd9de3c317c2702fb4/kubernetes-client-api/src/main/java/io/fabric8/kubernetes/client/Config.java#L120, unlikely to change
		System.setProperty("kubeconfig", configFile.getAbsolutePath());
		jolokiaConnection = getKubernetesMBeanConnector();
	}

	@Test
	public void testExecuteOperation() throws InstanceNotFoundException, MalformedObjectNameException, MBeanException,
			ReflectionException, MalformedURLException, IOException {
		jolokiaConnection.invoke(new ObjectName("java.lang:type=Memory"), "gc", new Object[0], new String[0]);
	}

	@Test
	public void testReadAttribute()
			throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException,
			MalformedObjectNameException, MBeanException, ReflectionException, MalformedURLException, IOException {
		MBeanServerConnection jmxConnection = jolokiaConnection;
		assertOneSingleAttribute(jmxConnection);

	}

	private void assertOneSingleAttribute(MBeanServerConnection jmxConnection) throws MalformedObjectNameException,
			MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
		ObjectName objectName = new ObjectName("java.lang:type=Memory");
		String attribute = "Verbose";
		Assert.assertEquals(false, jmxConnection.getAttribute(objectName, attribute));
	}

	@Before
	public void reset() {
		wiremock.resetAll();
		wiremock.resetRequests();
	}

	private static MBeanServerConnection getKubernetesMBeanConnector() throws IOException, MalformedURLException {
		JMXConnector connector = new JmcKubernetesJmxConnectionProvider().newJMXConnector(new JMXServiceURL(jolokiaUrl),
				Collections.emptyMap());
		connector.connect();
		MBeanServerConnection connection = connector.getMBeanServerConnection();
		return connection;
	}

	@Test
	public void testDiscoverWithMostlyDefaultSettings() throws Exception {

		TestParameters parameters = new TestParameters();
		// Set config so that scanning takes place
		parameters.scanForInstances = true;
		testThatJvmIsFound(parameters);
	}

	@Test
	public void testDiscoverWithPathFromAnnotation() throws Exception {
		TestParameters parameters = new TestParameters();
		parameters.scanForInstances = true;
		parameters.jolokiaPath = "${kubernetes/annotation/jolokiaPath}";
		testThatJvmIsFound(parameters);
	}

	@Test
	public void testDiscoverWithPortFromAnnotation() throws Exception {
		TestParameters parameters = new TestParameters();
		parameters.scanForInstances = true;
		parameters.jolokiaPort = "${kubernetes/annotation/jolokiaPort}";

		testThatJvmIsFound(parameters);
	}

	@Test
	public void testDiscoverWithBasicAuthFromSecret() throws Exception {
		TestParameters parameters = new TestParameters();
		parameters.scanForInstances = true;
		parameters.credentials = new InMemoryCredentials("${kubernetes/secret/jolokia-auth/username}",
				"${kubernetes/secret/jolokia-auth/password}");

		testThatJvmIsFound(parameters);
		// Verify that the expected authorization was picked up
		WireMock.verify(WireMock
				.postRequestedFor(WireMock.urlPathMatching("/api/v1/namespaces/ns1/pods/pod-abcdef.*/proxy/jolokia.*"))
				.withHeader("X-jolokia-authorization",
						WireMock.equalTo("Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes()))));
	}

	@Test
	public void testDiscoverWithAuthFromProperties() throws Exception {

		TestParameters parameters = new TestParameters();
		parameters.scanForInstances = true;
		parameters.credentials = new InMemoryCredentials("${kubernetes/secret/jolokia-properties/user}",
				"${kubernetes/secret/jolokia-properties/password}");

		testThatJvmIsFound(parameters);
		// Verify that the expected authorization was picked up
		WireMock.verify(WireMock
				.postRequestedFor(WireMock.urlPathMatching("/api/v1/namespaces/ns1/pods/pod-abcdef.*/proxy/jolokia.*"))
				.withHeader("X-jolokia-authorization",
						WireMock.equalTo("Basic " + Base64.getEncoder().encodeToString("admin:secret".getBytes()))));
	}

	@Test
	public void testDiscoverWithAuthDirectlyFromSettings() throws Exception {

		TestParameters parameters = new TestParameters();
		parameters.scanForInstances = true;
		parameters.credentials = new InMemoryCredentials("user", "***");
		testThatJvmIsFound(parameters);
		// Verify that the expected authorization was picked up
		WireMock.verify(WireMock
				.postRequestedFor(WireMock.urlPathMatching("/api/v1/namespaces/ns1/pods/pod-abcdef.*/proxy/jolokia.*"))
				.withHeader("X-jolokia-authorization",
						WireMock.equalTo("Basic " + Base64.getEncoder().encodeToString("user:***".getBytes()))));
	}

	private void testThatJvmIsFound(TestParameters parameters) throws Exception {

		final KubernetesDiscoveryListener scanner = new KubernetesDiscoveryListener(parameters);
		final Map<String, IServerDescriptor> foundVms = new HashMap<>();
		IDescriptorListener descriptorListener = new IDescriptorListener() {
			public void onDescriptorDetected(
				IServerDescriptor serverDescriptor, String path, JMXServiceURL url,
				IConnectionDescriptor connectionDescriptor, IDescribable provider) {
				foundVms.put(serverDescriptor.getGUID(), serverDescriptor);
			}

			public void onDescriptorRemoved(String descriptorId) {
				foundVms.remove(descriptorId);
			}
		};
		scanner.addDescriptorListener(descriptorListener);

		try {
			// Test that at least one VM (the one running the test was discovered)

			Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> !foundVms.isEmpty());
			IServerDescriptor descriptor = foundVms.get("pod-abcdef");
			Assert.assertNotNull(descriptor);
			Assert.assertEquals(
					"[JVMDescriptor] Java command: /Users/marska/Downloads/hawtio-app-2.9.1.jar --port 9090 PID: 88774",
					descriptor.getJvmInfo().toString());
			Assert.assertEquals(JVMType.HOTSPOT, descriptor.getJvmInfo().getJvmType());
			Assert.assertEquals("18.0.1", descriptor.getJvmInfo().getJavaVersion());
			Assert.assertTrue(descriptor instanceof IConnectionDescriptor);
			IConnectionDescriptor connectDescriptor = (IConnectionDescriptor) descriptor;
			JMXConnector connector = new JmcKubernetesJmxConnectionProvider()
					.newJMXConnector(connectDescriptor.createJMXServiceURL(), connectDescriptor.getEnvironment());
			connector.connect();
			assertOneSingleAttribute(connector.getMBeanServerConnection());

		} finally {
			// Tell scanner thread to exit
			scanner.shutdown();
		}
	}
}
