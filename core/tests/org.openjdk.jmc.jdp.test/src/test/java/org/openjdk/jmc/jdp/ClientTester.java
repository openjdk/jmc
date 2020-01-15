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
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openjdk.jmc.jdp.client.DiscoveryEvent;
import org.openjdk.jmc.jdp.client.DiscoveryListener;
import org.openjdk.jmc.jdp.client.JDPClient;
import org.openjdk.jmc.jdp.common.Configuration;

/**
 * Prints whatever JDP packets that are discovered on stdout.
 */
@SuppressWarnings("nls")
public class ClientTester {
	private static final String MULTICAST_PORT = "-port";
	private static final String MULTICAST_ADDRESS = "-address";

	static class Listener implements DiscoveryListener {
		@Override
		public void onDiscovery(DiscoveryEvent event) {
			System.out.println(String.format("Event: %s session %s", event.getKind().toString(),
					event.getDiscoverable().getSessionId()));
			System.out.println(String.format("Data:%n%s", printMap(event.getDiscoverable().getPayload())));
		}

		private String printMap(Map<String, String> map) {
			StringWriter sw = new StringWriter();
			for (Entry<String, String> entry : map.entrySet()) {
				sw.append(String.format("\t%-26s\t%-20s%n", entry.getKey(), entry.getValue()));
			}
			return sw.toString();
		}
	}

	public static void main(String[] args) throws IOException {
		Map<String, String> commands = parseArguments(args);
		JDPClient client = createClient(commands);
		client.addDiscoveryListener(new Listener());
		client.start();
		System.out.println("Press enter to quit");
		System.in.read();
		client.stop();
	}

	private static JDPClient createClient(Map<String, String> commands) throws UnknownHostException {
		int port = Integer.parseInt(commands.get(MULTICAST_PORT));
		InetAddress address = InetAddress.getByName(commands.get(MULTICAST_ADDRESS));
		return new JDPClient(address, port, Configuration.DEFAULT_MAX_HEART_BEAT_TIMEOUT);
	}

	private static HashMap<String, String> parseArguments(String[] args) {
		HashMap<String, String> commandMap = new HashMap<>();
		commandMap.put(MULTICAST_ADDRESS, Configuration.DEFAULT_MULTICAST_ADDRESS);
		commandMap.put(MULTICAST_PORT, Integer.toString(Configuration.DEFAULT_MULTICAST_PORT));

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
}
