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
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.logging.Level;

import org.openjdk.jmc.jdp.common.JDPPacket;

/**
 * This package private class listens for JDP packets, and sends them to the packet processor for
 * processing.
 */
final class PacketListener implements Runnable {
	private static final int BUFFER_LENGTH = 4096;
	private final MulticastSocket socket;
	private final PacketProcessor packetProcessor;
	private volatile boolean shutdown;

	PacketListener(MulticastSocket socket, PacketProcessor packetProcessor) {
		this.socket = socket;
		this.packetProcessor = packetProcessor;
	}

	@Override
	public void run() {
		byte[] buffer = new byte[BUFFER_LENGTH];
		DatagramPacket dgram = new DatagramPacket(buffer, buffer.length);

		while (!shutdown) {
			try {
				socket.receive(dgram);
			} catch (IOException e) {
				if (!shutdown) {
					JDPClient.LOGGER.log(Level.SEVERE, "Problem listening for JDP packets! Shutting down!", e); //$NON-NLS-1$
					socket.close();
				}
				return;
			}

			byte[] data = new byte[dgram.getLength()];
			System.arraycopy(dgram.getData(), dgram.getOffset(), data, 0, dgram.getLength());
			try {
				JDPPacket packet = new JDPPacket(data);
				packetProcessor.process(packet);
			} catch (Exception e) {
				JDPClient.LOGGER.log(Level.WARNING, "Could not decode JDP packet. Skipping!", e); //$NON-NLS-1$
			}
		}
	}

	public void stop() {
		shutdown = true;
		socket.close();
	}

	public boolean isAlive() {
		return !shutdown;
	}
}
