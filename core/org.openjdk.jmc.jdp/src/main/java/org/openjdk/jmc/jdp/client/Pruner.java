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

import java.util.Collection;

import org.openjdk.jmc.jdp.client.DiscoveryEvent.Kind;
import org.openjdk.jmc.jdp.client.PacketProcessor.DiscoverableInfo;

/**
 * This package private class prunes packets that have timed out, discovering lost services. This
 * class is responsible for emitting {@link DiscoveryEvent.Type} LOST events.
 */
final class Pruner implements Runnable {
	/**
	 * How often to check for packets to mark as dead.
	 */
	final static long PRUNING_INTERVAL = 3000L;

	/**
	 * @see maxHBTime
	 */
	final static int DEFAULT_MAX_HB_TIME = 12000;

	/**
	 * The maximum time to wait for the next heart beat, no matter what.
	 */
	final int maxHBTime;

	/**
	 * This is how many heart beats to wait before considering the service down.
	 */
	static double HB_MISSED_BEFORE_DOWN = 2.5;

	private volatile boolean isRunning;
	private final PacketProcessor processor;

	public Pruner(PacketProcessor processor, int maxHBTime) {
		this.processor = processor;
		this.maxHBTime = maxHBTime;
	}

	@Override
	public void run() {
		JDPClient.LOGGER.fine("JDP prune thread started!"); //$NON-NLS-1$
		isRunning = true;
		while (isRunning) {
			try {
				Thread.sleep(PRUNING_INTERVAL);
			} catch (InterruptedException e) {
				// Ignore - don't mind being interrupted.
			}
			checkPackets();
		}
		JDPClient.LOGGER.info("JDP prune thread shutting down!"); //$NON-NLS-1$
	}

	private void checkPackets() {
		JDPClient.LOGGER.finer("JDP prune checking..."); //$NON-NLS-1$
		long now = System.currentTimeMillis();
		synchronized (processor) {
			Collection<DiscoverableInfo> values = processor.infoMap.values();
			DiscoverableInfo[] discoverableInfos = values.toArray(new DiscoverableInfo[values.size()]);
			for (PacketProcessor.DiscoverableInfo info : discoverableInfos) {
				if (info.heartBeat != 0) {
					// If we missed a few heart beats, we consider it down.
					if (now - info.timestamp > (info.heartBeat * HB_MISSED_BEFORE_DOWN)) {
						remove(info);
					}
				} else if ((now - info.timestamp) > maxHBTime) {
					remove(info);
				}
			}
		}
	}

	/**
	 * @param key
	 *            the descriptor to remove.
	 */
	private void remove(DiscoverableInfo info) {
		processor.infoMap.remove(info.packet.getSessionId());
		JDPClient.LOGGER.fine("Lost " + info.packet); //$NON-NLS-1$
		processor.fireEvent(new DiscoveryEvent(Kind.LOST, info.packet));
	}

	public void stop() {
		isRunning = false;
	}
}
