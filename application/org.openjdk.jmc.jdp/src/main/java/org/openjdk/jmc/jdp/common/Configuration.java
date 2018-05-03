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

/**
 * Interface providing the network settings for a JDP server.
 */
public interface Configuration {
	public static final int DEFAULT_MULTICAST_PORT = 7095;
	public static final int DEFAULT_BROADCAST_PERIOD = 5000;
	public static final short DEFAULT_TTL = 0;
	public static final String DEFAULT_MULTICAST_ADDRESS = "224.0.23.178"; //$NON-NLS-1$
	public static final int DEFAULT_MAX_HEART_BEAT_TIMEOUT = 12000;

	/**
	 * The multicast group to join.
	 *
	 * @return the {@link InetAddress} for the multicast group to join.
	 */
	InetAddress getMulticastAddress();

	/**
	 * The multicast port to use.
	 *
	 * @return the multicast port to use.
	 */
	int getMulticastPort();

	/**
	 * The time to wait between broadcasts, in milliseconds.
	 * <p>
	 * Note: the server will need to be restarted for any changes to take effect.
	 * </p>
	 */
	int getBroadcastPeriod();

	/**
	 * The "time to live" for the JDP packets. The time to live is by default 0, which means that no
	 * JDP packets will escape the subnet.
	 * <p>
	 * Note: the server will need to be restarted for any changes to take effect.
	 * </p>
	 */
	short getTTL();

	/**
	 * @return the max time to wait for a new heart beat. Used for old style JDP packets that do not
	 *         provide their broadcast interval, to timeout if the broadcaster is shut down after
	 *         sending the first packet.
	 */
	int getMaxHeartBeatTimeout();
}
