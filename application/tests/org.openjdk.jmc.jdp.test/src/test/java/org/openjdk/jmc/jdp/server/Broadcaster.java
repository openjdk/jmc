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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.logging.Level;

import org.openjdk.jmc.jdp.common.Configuration;
import org.openjdk.jmc.jdp.common.JDPPacket;

/**
 * Class that will simply broadcast the provided data over the supplied socket until shut down.
 */
@SuppressWarnings("nls")
final class Broadcaster implements Runnable {
	private final byte[] data;
	private final MulticastSocket socket;
	private final InetAddress addr;
	private final int port;
	private final int period;
	private final JDPPacket packet;
	private volatile boolean isRunning = true;

	public Broadcaster(Configuration configuration, JDPPacket packet) throws IOException {
		this(createSocket(configuration), configuration.getMulticastAddress(), configuration.getMulticastPort(), packet,
				configuration.getBroadcastPeriod());
	}

	public Broadcaster(MulticastSocket socket, InetAddress addr, int port, JDPPacket packet, int period) {
		this.socket = socket;
		this.addr = addr;
		this.port = port;
		this.packet = packet;
		data = packet.getDiscoveryDataAsByteArray();
		this.period = period;
	}

	@Override
	public void run() {
		final DatagramPacket dp = new DatagramPacket(data, data.length, addr, port);
		while (isRunning) {
			long now = System.currentTimeMillis();

			try {
				socket.send(dp);
				JDPServer.LOGGER.fine("Sent JDP packet with contents:" + packet.getPayload());
			} catch (IOException ioe) {
				if (isRunning) {
					JDPServer.LOGGER.log(Level.SEVERE, "Could not send JDP packet!", ioe);
					shutDown();
				} else {
					JDPServer.LOGGER.log(Level.INFO,
							"Could not send JDP packet, most likely ok since Broadcaster had been shutdown and the socket closed.!",
							ioe);
				}
			}
			try {
				Thread.sleep(Math.max(0, period - (System.currentTimeMillis() - now)));
			} catch (InterruptedException e) {
				// Ignore
			}
		}
	}

	public void shutDown() {
		JDPServer.LOGGER.fine("Shutting down JDP broadcaster!");
		isRunning = false;
		socket.close();
	}

	private static MulticastSocket createSocket(Configuration configuration) throws IOException {
		MulticastSocket socket = new MulticastSocket(configuration.getMulticastPort());
		socket.setTimeToLive(configuration.getTTL());
		socket.joinGroup(configuration.getMulticastAddress());
		return socket;
	}

}
