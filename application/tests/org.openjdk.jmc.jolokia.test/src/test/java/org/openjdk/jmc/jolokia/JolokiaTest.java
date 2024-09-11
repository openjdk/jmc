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
package org.openjdk.jmc.jolokia;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.ReflectionException;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.awaitility.Awaitility;
import org.jolokia.server.core.config.ConfigKey;
import org.jolokia.server.core.config.StaticConfiguration;
import org.jolokia.server.core.detector.ServerDetector;
import org.jolokia.server.core.restrictor.AllowAllRestrictor;
import org.jolokia.server.core.service.JolokiaServiceManagerFactory;
import org.jolokia.server.core.service.api.JolokiaContext;
import org.jolokia.server.core.service.api.JolokiaServiceManager;
import org.jolokia.server.core.service.impl.StdoutLogHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.jolokia.preferences.PreferenceConstants;
import org.openjdk.jmc.rjmx.common.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.common.IServerDescriptor;
import org.openjdk.jmc.rjmx.descriptorprovider.IDescriptorListener;

/**
 * Tests that JMX connections done with JmcJolokiaJmxConnectionProvider are functional
 */
@SuppressWarnings("restriction")
public class JolokiaTest implements JolokiaDiscoverySettings, PreferenceConstants {

	static String jolokiaUrl;

	private static Set<String> unsafeAttributes = new HashSet<>(Arrays.asList("BootClassPath", "UsageThreshold",
			"UsageThresholdExceeded", "UsageThresholdCount", "CollectionUsageThreshold",
			"CollectionUsageThresholdExceeded", "CollectionUsageThresholdCount", "Config", "MBeanServerId"));

	private JolokiaDiscoveryListener discoveryListener;

	private static MBeanServerConnection jolokiaConnection, localConnection;

	@BeforeClass
	public static void startServer() throws Exception {
		// wait for Jolokia to be ready before commencing tests
		Awaitility.await().atMost(Duration.ofSeconds(15))//Note: hard code property to avoid module dependency on agent
				.until(() -> (jolokiaUrl = System.getProperty("jolokia.agent")) != null);
		jolokiaConnection = getJolokiaMBeanConnector();
		localConnection = MBeanServerFactory.createMBeanServer();
	}

	@Test
	public void testReadAttributesOverJolokia() throws MalformedURLException, IOException, OperationsException,
			IntrospectionException, AttributeNotFoundException, ReflectionException, MBeanException {
		int fetched = 0, compared = 0, unavailable = 0;
		for (ObjectName objectName : jolokiaConnection.queryNames(null, null)) {
			for (MBeanAttributeInfo attributeInfo : getJolokiaMBeanConnector().getMBeanInfo(objectName)
					.getAttributes()) {
				String attributeName = attributeInfo.getName();
				if (!unsafeAttributes.contains(attributeName)) {
					Object attribute = getJolokiaMBeanConnector().getAttribute(objectName, attributeName);
					fetched++;
					if (attribute instanceof String || attribute instanceof Boolean) { // Assume strings and booleans are safe to compare directly
						try {
							Object locallyRetrievedAttribute = localConnection.getAttribute(objectName, attributeName);
							compared++;
							Assert.assertEquals("Comparing returned value of " + objectName + "." + attributeName,
									locallyRetrievedAttribute, attribute);
						} catch (InstanceNotFoundException e) {
							unavailable++;
						}
					}
				}
			}
		}
		System.out.println("  attribute test stats: fetched: " + fetched + ", compared: " + compared + ", unavailable: "
				+ unavailable);
	}

	@Test
	public void testExecuteOperation() throws InstanceNotFoundException, MalformedObjectNameException, MBeanException,
			ReflectionException, MalformedURLException, IOException {
		jolokiaConnection.invoke(new ObjectName("java.lang:type=Memory"), "gc", new Object[0], new String[0]);
	}

	@Test
	public void testWriteAttribute()
			throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException,
			MalformedObjectNameException, MBeanException, ReflectionException, MalformedURLException, IOException {
		ObjectName objectName = new ObjectName("java.lang:type=Memory");
		String attribute = "Verbose";
		jolokiaConnection.setAttribute(objectName, new Attribute(attribute, true));
		Assert.assertEquals(true, jolokiaConnection.getAttribute(objectName, attribute));
		//set it back as it otherwise generates a lot of noise in the output
		jolokiaConnection.setAttribute(objectName, new Attribute(attribute, false));
	}

	private static MBeanServerConnection getJolokiaMBeanConnector() throws IOException, MalformedURLException {
		JMXConnector connector = new JmcJolokiaJmxConnectionProvider().newJMXConnector(
				new JMXServiceURL(jolokiaUrl.replace("http", "service:jmx:jolokia")), Collections.emptyMap());
		connector.connect();
		MBeanServerConnection connection = connector.getMBeanServerConnection();
		return connection;
	}

	@Test
	public void testDiscover() {
		if("true".equals(System.getProperty("skipJDPMulticastTests"))) {
			//In certain situations multicast will not work 
			// 'D> --> Couldnt send discovery message from /127.0.0.1: java.net.BindException: Can't assign requested address
			//  D> --> Exception during lookup: java.util.concurrent.ExecutionException: 
			//    org.jolokia.service.discovery.MulticastUtil$CouldntSendDiscoveryPacketException: 
			//    Can't send discovery UDP packet from /127.0.0.1: Can't assign requested address'
			// We get test coverage on both Linux and Windows
			return;

		}
		discoveryListener = new JolokiaDiscoveryListener(this);

		final AtomicInteger foundVms = new AtomicInteger(0);

		discoveryListener.addDescriptorListener(new IDescriptorListener() {
			public void onDescriptorDetected(
				IServerDescriptor serverDescriptor, String path, JMXServiceURL url,
				IConnectionDescriptor connectionDescriptor, IDescribable provider) {
				foundVms.getAndIncrement();
			}

			public void onDescriptorRemoved(String descriptorId) {
				foundVms.getAndDecrement();
			}

		});
		// Test that at least one VM (the one running the test was discovered)
		Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> foundVms.get() > 0);
	}

	@After
	public void stopListener() throws Exception {
		if (discoveryListener != null) {
			discoveryListener.shutdown();
		}
	}

	@Override
	public boolean shouldRunDiscovery() {
		return true;
	}

	@Override
	public JolokiaContext getJolokiaContext() {
		StaticConfiguration configuration = new StaticConfiguration(ConfigKey.AGENT_ID, "jolokiatest");
		JolokiaServiceManager serviceManager = JolokiaServiceManagerFactory.createJolokiaServiceManager(configuration,
				new StdoutLogHandler(true), new AllowAllRestrictor(),
				() -> new TreeSet<ServerDetector>(Arrays.asList(ServerDetector.FALLBACK)));
		return serviceManager.start();
	}

	@Override
	public String getMulticastGroup() {
		return ConfigKey.MULTICAST_GROUP.getDefaultValue();
	}

	@Override
	public int getMulticastPort() {
		return Integer.parseInt(ConfigKey.MULTICAST_PORT.getDefaultValue());
	}

	@Override
	public int getDiscoveryTimeout() {
		return 1000;
	}

}
