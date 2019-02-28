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
package org.openjdk.jmc.rjmx.test.testutil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Simple, small test class to make a JVM stay awake. Takes one optional argument, which is the
 * time, in seconds, to stay alive. If no argument is specified, the JVM will stay alive
 * indefinitely.
 * <p>
 * It's meant to be used as a simple way to keep a JVM awake while testing the management console.
 * To start a JVM with the management server, use the following argument:
 * </p>
 * Use the system properties -Dkill.port=<port> to specify a port other than 4713 to listen for kill
 * commands.
 */
public class JVMKeepAlive {
	private static int m_aliveTime;
	private static long m_startTime = System.currentTimeMillis();
	public static final int DEFAULT_KILL_PORT = 4713;
	public static final byte[] KILL_MESSAGE = "KILL".getBytes();
	private static final String PROPERTY_KILL_PORT = "jmc.test.kill.port";

	/**
	 * Small server that listens for datagram packets on the specified port and kills the JVM when
	 * anything is received.
	 */
	private static class JVMKeepAliveSlayer implements Runnable {
		@Override
		public void run() {
			String portStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
				@Override
				public String run() {
					return System.getProperty(PROPERTY_KILL_PORT, String.valueOf(DEFAULT_KILL_PORT));
				}
			});

			int port = 0;
			try {
				port = Integer.parseInt(portStr);
			} catch (NumberFormatException nfe) {
				port = DEFAULT_KILL_PORT;
			}
			System.out.println(buildClassNamePrefix(JVMKeepAliveSlayer.class) + "Send kill command to port " + port
					+ " to kill me.");

			try {
				try (DatagramSocket s = new DatagramSocket(port)) {
					DatagramPacket p = new DatagramPacket(new byte[25], 25);
					s.receive(p);
					System.exit(0);
				}

			} catch (IOException e) {
				System.out.println(buildClassNamePrefix(JVMKeepAliveSlayer.class) + e.getMessage());
				System.out.println(buildClassNamePrefix(JVMKeepAliveSlayer.class)
						+ "Proceeding without JRockitKeepAliveSlayer...");
				return;
			}
		}

	}

	private static String buildClassNamePrefix(Class<?> clazz) {
		return '[' + clazz.getName() + "] ";
	}

	/**
	 * Loops for a preset time...
	 *
	 * @param args
	 *            none needed
	 */
	public static void main(String[] args) {
		if (args.length == 1) {
			m_aliveTime = Integer.parseInt(args[0]);
		}

		// Start the JRockitKeepAliveSlayer that listens for kill commands
		Thread t = new Thread(new JVMKeepAliveSlayer(), buildClassNamePrefix(JVMKeepAliveSlayer.class));
		t.start();

		System.out.println(buildClassNamePrefix(JVMKeepAlive.class) + "Started...");

		while (m_aliveTime == 0 || (System.currentTimeMillis() - m_startTime) / 1000 <= m_aliveTime) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// ignore
			}
			System.gc();
			Thread.yield();
		}
	}
}
