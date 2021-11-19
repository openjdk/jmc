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
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.awaitility.Awaitility;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.jolokia.preferences.PreferenceConstants;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.rjmx.descriptorprovider.IDescriptorListener;

/**
 * I test that JMX connections done with JmcJolokiaJmxConnectionProvider are functional 
 */
@SuppressWarnings("restriction")
public class JolokiaTest {

	static String jolokiaUrl;

	private static Set<String> unsafeAttributes = new HashSet<>(
			Arrays.asList("BootClassPath", "UsageThreshold", "UsageThresholdExceeded", "UsageThresholdCount",
					"CollectionUsageThreshold", "CollectionUsageThresholdExceeded", "CollectionUsageThresholdCount"));

	private static JolokiaDiscoveryListener discoveryListener;

	private static MBeanServerConnection jolokiaConnection;

	@BeforeClass
	public static void startServer() throws Exception {
		// wait for Jolokia to be ready before commencing tests
		Awaitility.await().atMost(Duration.ofSeconds(15))//Note: hard code property to avoid module dependency on agent
				.until(() -> (jolokiaUrl = System.getProperty("jolokia.agent")) != null);
		discoveryListener = new JolokiaDiscoveryListener();
		jolokiaConnection=getJolokiaMBeanConnector();
		

	}

	@Test
	public void testReadAttributesOverJolokia() throws MalformedURLException, IOException, OperationsException,
			IntrospectionException, AttributeNotFoundException, ReflectionException, MBeanException {
		for (ObjectName objectName : jolokiaConnection.queryNames(null, null)) {
			for (MBeanAttributeInfo attributeInfo : getJolokiaMBeanConnector().getMBeanInfo(objectName).getAttributes()) {
				if (!unsafeAttributes.contains(attributeInfo.getName())) {
					System.out.println("Getting attribute " + objectName + "/" + attributeInfo.getName());
					getJolokiaMBeanConnector().getAttribute(objectName, attributeInfo.getName());
				}
			}
		}

	}
	
	@Test
	public void testExecuteOperation() throws InstanceNotFoundException, MalformedObjectNameException, MBeanException, ReflectionException, MalformedURLException, IOException {
		jolokiaConnection.invoke(new ObjectName("java.lang:type=Memory"), "gc", new Object[0], new String[0]);
	}
	
	@Test
	public void testWriteAttribute() throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MalformedObjectNameException, MBeanException, ReflectionException, MalformedURLException, IOException {
		ObjectName objectName = new ObjectName("jolokia:type=Config");
		String attribute = "Debug";
		jolokiaConnection.setAttribute(objectName, new Attribute(attribute, true));
		Assert.assertEquals(true, jolokiaConnection.getAttribute(objectName, attribute));
		
	}

	private static MBeanServerConnection getJolokiaMBeanConnector() throws IOException, MalformedURLException {
		JMXConnector connector = new JmcJolokiaJmxConnectionProvider().newJMXConnector(new JMXServiceURL(jolokiaUrl.replace("http", "service:jmx:jolokia")),
				Collections.emptyMap());
		connector.connect();
		MBeanServerConnection connection = connector.getMBeanServerConnection();
		return connection;
	}

	@Test
	public void testDiscover() {

		final AtomicInteger foundVms = new AtomicInteger(0);

		//Set config so that scanning takes place
		InstanceScope.INSTANCE.getNode(JmcJolokiaPlugin.PLUGIN_ID).put(PreferenceConstants.P_SCAN, "true");

		discoveryListener.addDescriptorListener(new IDescriptorListener() {
			public void onDescriptorDetected(IServerDescriptor serverDescriptor, String path, JMXServiceURL url,
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

	@AfterClass
	public static void stopServer() throws Exception {
		discoveryListener.shutdown();
	}

}
