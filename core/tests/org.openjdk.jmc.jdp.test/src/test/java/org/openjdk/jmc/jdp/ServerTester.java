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
package org.openjdk.jmc.jdp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.jdp.client.TestToolkit;
import org.openjdk.jmc.jdp.common.Configuration;
import org.openjdk.jmc.jdp.jmx.JMXDataKeys;
import org.openjdk.jmc.jdp.server.jmx.JMXJDPServer;

@SuppressWarnings("nls")
public class ServerTester {
	private static final String BROADCAST_PERIOD = "-period";
	private static final String MULTICAST_PORT = "-port";
	private static final String MULTICAST_ADDRESS = "-address";
	private static final String TTL = "-ttl";
	private static final String JMXPORT = "-jmxport";
	private static final String JMXHOST = "-jmxhost";
	private static final String PID = "-pid";
	private static final String COMMAND = "-command";
	// Will automatically start a number of servers derived from the settings.
	private static final String AUTO = "-n";
	// Will put the JVMs in separate groups.
	private static final String GROUPS = "-groups";
	// Will disable explicit naming
	private static final String DISABLE_NAMING = "-nonaming";

	public static void main(String[] args) throws UnknownHostException {
		Map<String, String> commands = parseArguments(args);
		Collection<JMXJDPServer> servers = createServers(commands);
		try {
			for (JMXJDPServer server : servers) {
				server.start();
				TestToolkit.printServerSettings(server);
			}
			System.out.println("Press enter to quit");
			System.in.read();
			for (JMXJDPServer server : servers) {
				System.out.println("Stopping server...");
				server.stop();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
	}

	public static Configuration createConfiguration(
		final short ttl, final int period, final int port, final InetAddress address) {
		return new Configuration() {

			@Override
			public short getTTL() {
				return ttl;
			}

			@Override
			public int getMulticastPort() {
				return port;
			}

			@Override
			public InetAddress getMulticastAddress() {
				return address;
			}

			@Override
			public int getBroadcastPeriod() {
				return period;
			}

			@Override
			public int getMaxHeartBeatTimeout() {
				return Configuration.DEFAULT_MAX_HEART_BEAT_TIMEOUT;
			}
		};
	}

	private static Collection<JMXJDPServer> createServers(Map<String, String> commands) throws UnknownHostException {
		List<JMXJDPServer> servers = new ArrayList<>();
		int auto = Integer.parseInt(commands.get(AUTO));
		int groups = Integer.parseInt(commands.get(GROUPS));
		boolean disableNaming = Boolean.parseBoolean(commands.get(DISABLE_NAMING));
		for (int n = 0; n < auto; n++) {
			String name = disableNaming ? null : createName(n, groups);
			servers.add(new JMXJDPServer(createConfiguration(n, commands), createData(n, commands, name)));
		}
		return servers;
	}

	private static String createName(int n, int groups) {
		if (groups == 0) {
			return String.format("JVM %d", n);
		}
		int group = n / groups;
		int jvmNo = n % groups;
		return String.format("Cluster %d/JVM %d", group, jvmNo);
	}

	private static JMXServiceURL createAgentUrl(int n, Map<String, String> commands) {
		String host = commands.get(JMXHOST);
		int port = Integer.parseInt(commands.get(JMXPORT));
		if (n > 0) {
			host += n;
			port += n;
		}
		try {
			return TestToolkit.createServiceURL(host, port);
		} catch (Exception e) {
			System.out.println("Could not create service URL!");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	private static Configuration createConfiguration(int n, Map<String, String> commands) throws UnknownHostException {
		short ttl = (short) Integer.parseInt(commands.get(TTL));
		int period = Integer.parseInt(commands.get(BROADCAST_PERIOD));
		int port = Integer.parseInt(commands.get(MULTICAST_PORT));
		InetAddress address = InetAddress.getByName(commands.get(MULTICAST_ADDRESS));

		return createConfiguration(ttl, period, port, address);
	}

	private static HashMap<String, String> parseArguments(String[] args) {
		HashMap<String, String> commandMap = new HashMap<>();
		commandMap.put(MULTICAST_ADDRESS, Configuration.DEFAULT_MULTICAST_ADDRESS);
		commandMap.put(MULTICAST_PORT, Integer.toString(Configuration.DEFAULT_MULTICAST_PORT));
		commandMap.put(JMXHOST, "localhost");
		commandMap.put(JMXPORT, "7095");
		commandMap.put(TTL, Integer.toString(Configuration.DEFAULT_TTL));
		commandMap.put(BROADCAST_PERIOD, Integer.toString(Configuration.DEFAULT_BROADCAST_PERIOD));
		commandMap.put(AUTO, Integer.toString(1));
		commandMap.put(GROUPS, Integer.toString(0));
		commandMap.put(PID, JMXJDPServer.getPID());

		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-")) {
				StringBuilder buf = new StringBuilder();
				int j = i + 1;
				while (j < args.length && !args[j].startsWith("-")) {
					buf.append(" ");
					buf.append(args[j++]);
				}
				commandMap.put(args[i], buf.toString().trim());
				i = j - 1;
			}
		}
		return commandMap;
	}

	private static Map<String, String> createData(int n, Map<String, String> commands, String name) {
		Map<String, String> discoveryData = new HashMap<>();
		discoveryData.put(JMXDataKeys.KEY_INSTANCE_NAME, name);
		discoveryData.put(JMXDataKeys.KEY_JMX_SERVICE_URL, createAgentUrl(n, commands).toString());
		discoveryData.put(JMXDataKeys.KEY_JAVA_COMMAND, createCommand(n, commands));
		discoveryData.put(JMXDataKeys.KEY_PID, createPID(n, commands));
		return discoveryData;
	}

	private static String createPID(int n, Map<String, String> commands) {
		int base = Integer.parseInt(commands.get(PID));
		return Integer.toString(base + n);
	}

	private static String createCommand(int n, Map<String, String> commands) {
		String command = commands.get(COMMAND);
		if (command == null) {
			return JMXJDPServer.getJavaCommand();
		}
		return command;
	}
}
