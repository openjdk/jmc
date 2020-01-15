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
package org.openjdk.jmc.jdp.server;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import org.openjdk.jmc.jdp.client.JDPClientTest;
import org.openjdk.jmc.jdp.common.Configuration;
import org.openjdk.jmc.jdp.common.ConfigurationFactory;
import org.openjdk.jmc.jdp.common.JDPPacket;

/**
 * <p>
 * The JDP server provides a light weight means to multicast a heart beat on the network, making it
 * possible for client applications to detect the presence a service. The life cycle of the JDP
 * server is normally handled by the JVM, and is kept in sync with the external JMX management
 * agent. It is possible to use the JDP server as a stand-alone utility to broadcast information
 * about other agents than the JVM managed server, however the life cycle will need be managed
 * manually. Use one JDPServer per service to broadcast information about.
 * </p>
 * <p>
 * The information broadcasted by the JDP server can be configured using the #setDiscoveryData()
 * method.
 * </p>
 */
@SuppressWarnings("nls")
public class JDPServer {
	public final static String KEY_PERIOD = "BROADCAST_INTERVAL";
	public final static String KEY_DISCOVERABLE_ID = "DISCOVERABLE_SESSION_UUID";
	final static Logger LOGGER = JDPClientTest.LOGGER;
	private final Configuration configuration;
	private final String discoverableID;
	private volatile boolean isStarted;
	private Broadcaster broadcaster;
	private Map<String, String> discoveryData;

	/**
	 * Creates a JDP server with the default settings.
	 *
	 * @param discoverableID
	 *            a String uniquely identifying the service instance. Must not be null!
	 */
	public JDPServer(String discoverableID) {
		this(discoverableID, ConfigurationFactory.createConfiguration());
	}

	/**
	 * Creates a JDP server. Note that this JDP server will be using a random server ID.
	 *
	 * @param discoverableID
	 *            a String uniquely identifying the service instance. Must not be null!
	 * @param configuration
	 *            the network configuration to use.
	 */
	public JDPServer(String discoverableID, Configuration configuration) {
		if (discoverableID == null) {
			throw new NullPointerException("A unique identifier for the discoverable must be provided!");
		}
		this.discoverableID = discoverableID;
		this.configuration = configuration;
	}

	/**
	 * This method starts the JDP server.
	 *
	 * @throws IOException
	 */
	public synchronized void start() throws IOException {
		if (isAlive()) {
			return;
		}
		JDPPacket packet = createPacket();
		broadcaster = new Broadcaster(configuration, packet);
		new Thread(broadcaster, "(JDP autodiscovery)").start();
		isStarted = true;
		LOGGER.info("JDP Server started at " + configuration.getMulticastAddress() + ":"
				+ configuration.getMulticastPort());
	}

	/**
	 * Calling this method will cause the JDP server to stop transmitting.
	 */
	public synchronized void stop() {
		isStarted = false;
		broadcaster.shutDown();
		broadcaster = null;
	}

	/**
	 * @return true if the JDP server is up and running, false otherwise.
	 */
	public synchronized boolean isAlive() {
		return isStarted;
	}

	/**
	 * Sets the properties to broadcast. This is a convenience method for those still using the old
	 * java.util.Properties class.
	 *
	 * @param props
	 *            the properties to broadcast.
	 */
	public synchronized void setDiscoveryData(Properties props) {
		discoveryData = convert(props);
		restartIfAlive();
	}

	/**
	 * Sets the properties to broadcast.
	 *
	 * @param props
	 *            the properties to broadcast.
	 */
	public synchronized void setDiscoveryData(Map<String, String> props) {
		discoveryData = props;
	}

	/**
	 * @return the properties to broadcast.
	 */
	public synchronized Map<String, String> getDiscoveryData() {
		if (discoveryData == null) {
			return Collections.emptyMap();
		}
		return discoveryData;
	}

	/**
	 * Stops and then starts the server again.
	 *
	 * @throws IOException
	 */
	public void restart() throws IOException {
		stop();
		start();
	}

	private JDPPacket createPacket() {
		Map<String, String> data = new HashMap<>(getDiscoveryData());
		data.put(KEY_PERIOD, String.valueOf(configuration.getBroadcastPeriod()));
		data.put(KEY_DISCOVERABLE_ID, discoverableID);
		return new JDPPacket(data);
	}

	private void restartIfAlive() {
		if (isAlive()) {
			try {
				restart();
			} catch (IOException e) {
				// It was already alive, so unlikely to happen.
				// If it does, shutdown to cleanup.
				stop();
			}
		}
	}

	/**
	 * Helper method to convert from properties to map.
	 */
	private static Map<String, String> convert(Properties props) {
		Map<String, String> result = new HashMap<>();
		for (Entry<Object, Object> entry : props.entrySet()) {
			result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
		}
		return result;
	}

	/**
	 * @return the configuration settings for the server.
	 */
	public Configuration getConfiguration() {
		return configuration;
	}
}
