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
package org.openjdk.jmc.jdp.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Set;
import java.util.logging.Logger;

import org.openjdk.jmc.jdp.common.Configuration;
import org.openjdk.jmc.jdp.common.ConfigurationFactory;

/**
 * <p>
 * Client for discovering JVM services on the network.
 * </p>
 * <p>
 * <b>Note:</b> This client is also able to parse packets sent using the legacy JRockit Discovery
 * Protocol.
 * </p>
 */
public final class JDPClient {
	static final Logger LOGGER = Logger.getLogger("org.openjdk.jmc.jdp.client"); //$NON-NLS-1$
	private final InetAddress address;
	private final int port;
	private PacketListener listener;
	private final PacketProcessor processor = new PacketProcessor();
	private final Pruner pruner;

	public JDPClient() {
		this(ConfigurationFactory.DEFAULT_MULTICAST_ADDRESS, Configuration.DEFAULT_MULTICAST_PORT,
				Pruner.DEFAULT_MAX_HB_TIME);
	}

	JDPClient(InetAddress address, int port) {
		this(address, port, Pruner.DEFAULT_MAX_HB_TIME);
	}

	public JDPClient(InetAddress address, int port, int heartBeatTimeout) {
		this.address = address;
		this.port = port;
		pruner = new Pruner(processor, heartBeatTimeout);
	}

	public void addDiscoveryListener(DiscoveryListener listener) {
		processor.addDiscoveryListener(listener);
	}

	public void removeDiscoveryListener(DiscoveryListener listener) {
		processor.removeDiscoveryListener(listener);
	}

	public synchronized void start() throws IOException {
		MulticastSocket socket = new MulticastSocket(port);
		listener = new PacketListener(socket, processor);
		socket.joinGroup(address);
		ThreadGroup jdpThreads = new ThreadGroup("JDP Client"); //$NON-NLS-1$
		startThread(jdpThreads, listener, "(JDP Packet Listener)"); //$NON-NLS-1$
		startThread(jdpThreads, pruner, "(JDP Client Pruner)"); //$NON-NLS-1$
	}

	private static void startThread(ThreadGroup group, Runnable r, String name) {
		Thread t = new Thread(group, r, name);
		t.setDaemon(true);
		t.start();
	}

	public synchronized void stop() {
		pruner.stop();
		if (listener != null) {
			listener.stop();
		}
	}

	public Set<Discoverable> getDiscoverables() {
		return processor.getDiscoverables();
	}
}
