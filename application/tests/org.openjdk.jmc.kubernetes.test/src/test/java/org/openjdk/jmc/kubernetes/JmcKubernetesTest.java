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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Duration;
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
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.jolokia.util.Base64Util;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.jolokia.preferences.PreferenceInitializer;
import org.openjdk.jmc.kubernetes.preferences.PreferenceConstants;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.rjmx.descriptorprovider.IDescriptorListener;
import org.openjdk.jmc.ui.common.jvm.JVMType;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.fabric8.kubernetes.client.Config;

/**
 * I test that JMX connections done with JmcKubernetesJmxConnectionProvider are
 * functional. In order to be able to test this in a contained environment, the
 * kubernetes API is mocked with wiremock.
 */
@SuppressWarnings("restriction")
public class JmcKubernetesTest {

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
		System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE, configFile.getAbsolutePath());
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
	public void resetPreferences() {
		new PreferenceInitializer().initializeDefaultPreferences();
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

		// Set config so that scanning takes place
		InstanceScope.INSTANCE.getNode(JmcKubernetesPlugin.PLUGIN_ID).put(PreferenceConstants.P_SCAN_FOR_INSTANCES,
				"true");

		testThatJvmIsFound();
	}

	@Test
	public void testDiscoverWithPathFromAnnotation() throws Exception {
		// Set config so that scanning takes place
		InstanceScope.INSTANCE.getNode(JmcKubernetesPlugin.PLUGIN_ID).put(PreferenceConstants.P_SCAN_FOR_INSTANCES,
				"true");
		InstanceScope.INSTANCE.getNode(JmcKubernetesPlugin.PLUGIN_ID).put(PreferenceConstants.P_JOLOKIA_PATH,
				"${kubernetes/annotation/jolokiaPath}");

		testThatJvmIsFound();
	}

	@Test
	public void testDiscoverWithPortFromAnnotation() throws Exception {
		// Set config so that scanning takes place
		InstanceScope.INSTANCE.getNode(JmcKubernetesPlugin.PLUGIN_ID).put(PreferenceConstants.P_SCAN_FOR_INSTANCES,
				"true");
		InstanceScope.INSTANCE.getNode(JmcKubernetesPlugin.PLUGIN_ID).put(PreferenceConstants.P_JOLOKIA_PORT,
				"${kubernetes/annotation/jolokiaPort}");

		testThatJvmIsFound();
	}

	@Test
	public void testDiscoverWithBasicAuthFromSecret() throws Exception {
		// Set config so that scanning takes place
		InstanceScope.INSTANCE.getNode(JmcKubernetesPlugin.PLUGIN_ID).put(PreferenceConstants.P_SCAN_FOR_INSTANCES,
				"true");
		InstanceScope.INSTANCE.getNode(JmcKubernetesPlugin.PLUGIN_ID).put(PreferenceConstants.P_USERNAME,
				"${kubernetes/secret/jolokia-auth/username}");
		InstanceScope.INSTANCE.getNode(JmcKubernetesPlugin.PLUGIN_ID).put(PreferenceConstants.P_PASSWORD,
				"${kubernetes/secret/jolokia-auth/password}");

		testThatJvmIsFound();
		//Verify that the expected authorization was picked up
		WireMock.verify(WireMock
				.postRequestedFor(
						WireMock.urlPathMatching("/kubernetes/api/v1/namespaces/ns1/pods/pod-abcdef.*/proxy/jolokia.*"))
				.withHeader("X-jolokia-authorization", WireMock.equalTo("Basic "+Base64Util.encode("admin:admin".getBytes()))));
	}

	@Test
	public void testDiscoverWithAuthFromProperties() throws Exception {
		// Set config so that scanning takes place
		InstanceScope.INSTANCE.getNode(JmcKubernetesPlugin.PLUGIN_ID).put(PreferenceConstants.P_SCAN_FOR_INSTANCES,
				"true");
		InstanceScope.INSTANCE.getNode(JmcKubernetesPlugin.PLUGIN_ID).put(PreferenceConstants.P_USERNAME,
				"${kubernetes/secret/jolokia-auth/username}");
		InstanceScope.INSTANCE.getNode(JmcKubernetesPlugin.PLUGIN_ID).put(PreferenceConstants.P_PASSWORD,
				"${kubernetes/secret/jolokia-auth/password}");

		testThatJvmIsFound();
		//Verify that the expected authorization was picked up
		WireMock.verify(WireMock
				.postRequestedFor(
						WireMock.urlPathMatching("/kubernetes/api/v1/namespaces/ns1/pods/pod-abcdef.*/proxy/jolokia.*"))
				.withHeader("X-jolokia-authorization", WireMock.equalTo("Basic "+Base64Util.encode("admin:secret".getBytes()))));
	}
	
	@Test
	public void testDiscoverWithAuthDirectlyFromSettings() throws Exception {
		// Set config so that scanning takes place
		InstanceScope.INSTANCE.getNode(JmcKubernetesPlugin.PLUGIN_ID).put(PreferenceConstants.P_SCAN_FOR_INSTANCES,
				"true");
		InstanceScope.INSTANCE.getNode(JmcKubernetesPlugin.PLUGIN_ID).put(PreferenceConstants.P_USERNAME,
				"user");
		InstanceScope.INSTANCE.getNode(JmcKubernetesPlugin.PLUGIN_ID).put(PreferenceConstants.P_PASSWORD,
				"***");

		testThatJvmIsFound();
		//Verify that the expected authorization was picked up
		WireMock.verify(WireMock
				.postRequestedFor(
						WireMock.urlPathMatching("/kubernetes/api/v1/namespaces/ns1/pods/pod-abcdef.*/proxy/jolokia.*"))
				.withHeader("X-jolokia-authorization", WireMock.equalTo("Basic "+Base64Util.encode("user:***".getBytes()))));
	}

	private void testThatJvmIsFound() throws Exception {
		final Map<String, IServerDescriptor> foundVms = new HashMap<>();
		IDescriptorListener descriptorListener = new IDescriptorListener() {
			public void onDescriptorDetected(IServerDescriptor serverDescriptor, String path, JMXServiceURL url,
					IConnectionDescriptor connectionDescriptor, IDescribable provider) {
				foundVms.put(serverDescriptor.getGUID(), serverDescriptor);
			}

			public void onDescriptorRemoved(String descriptorId) {
				foundVms.remove(descriptorId);
			}
		};
		KubernetesDiscoveryListener discoveryListener = new KubernetesDiscoveryListener();
		discoveryListener.addDescriptorListener(descriptorListener);
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
			discoveryListener.shutdown();
		}
	}
}
