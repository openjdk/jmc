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
package org.openjdk.jmc.jdp.common;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class with the network configuration settings default for the JDP protocol.
 */
public final class ConfigurationFactory {
	public static final InetAddress DEFAULT_MULTICAST_ADDRESS;

	static {
		InetAddress tmp = null;
		try {
			tmp = InetAddress.getByName(Configuration.DEFAULT_MULTICAST_ADDRESS);
		} catch (UnknownHostException e) {
			// Multicast address by IP, should never happen!
			Logger.getLogger("org.openjdk.jmc.jdp.common").log(Level.SEVERE, "Could not create default mulitcast address!", //$NON-NLS-1$ //$NON-NLS-2$
					e);
		}
		DEFAULT_MULTICAST_ADDRESS = tmp;
	}

	public static Configuration createConfiguration() {
		// CMH - get values properly from the JVM settings here!
		return new Configuration() {
			@Override
			public int getMulticastPort() {
				return DEFAULT_MULTICAST_PORT;
			}

			@Override
			public InetAddress getMulticastAddress() {
				return ConfigurationFactory.DEFAULT_MULTICAST_ADDRESS;
			}

			@Override
			public int getBroadcastPeriod() {
				return DEFAULT_BROADCAST_PERIOD;
			}

			@Override
			public short getTTL() {
				return DEFAULT_TTL;
			}

			@Override
			public int getMaxHeartBeatTimeout() {
				return DEFAULT_MAX_HEART_BEAT_TIMEOUT;
			}
		};
	}
}
