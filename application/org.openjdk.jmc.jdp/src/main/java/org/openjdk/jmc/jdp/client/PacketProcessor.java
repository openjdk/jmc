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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openjdk.jmc.jdp.client.DiscoveryEvent.Kind;
import org.openjdk.jmc.jdp.common.JDPPacket;

/**
 * The package private PacketProcessor will remember detected packets and transmit the appropriate
 * events (FOUND, CHANGED) to the registered listeners.
 */
final class PacketProcessor {
	private static final String KEY_BROADCAST_PERIOD = "BROADCAST_INTERVAL"; //$NON-NLS-1$

	private final List<DiscoveryListener> listeners = new ArrayList<>();
	final Map<String, DiscoverableInfo> infoMap = new HashMap<>();

	static class DiscoverableInfo {
		// The timestamp the packet was last discovered.
		long timestamp;
		// The calculated heart beat interval
		long heartBeat;
		// Heart beat in packet?
		boolean needToCalculateHeartBeat = true;
		// The packet discovered
		JDPPacket packet;
	}

	public synchronized void process(JDPPacket packet) {
		// Using same hb calculations as in the old JRMC client
		DiscoverableInfo info = infoMap.get(packet.getSessionId());
		long now = System.currentTimeMillis();

		if (info == null) {
			info = new DiscoverableInfo();
			info.timestamp = now;
			info.packet = packet;
			infoMap.put(packet.getSessionId(), info);
			long broadcastPeriod = getBroadcastPeriodFromPayload(packet.getPayload());
			if (broadcastPeriod > 0) {
				info.heartBeat = broadcastPeriod;
				info.needToCalculateHeartBeat = false;
			}
			JDPClient.LOGGER.fine("Found " + packet); //$NON-NLS-1$
			fireEvent(new DiscoveryEvent(Kind.FOUND, packet));
		} else if (!info.packet.equals(packet)) {
			JDPClient.LOGGER.fine("Changed " + packet); //$NON-NLS-1$
			fireEvent(new DiscoveryEvent(Kind.CHANGED, packet));
		}
		if (info.needToCalculateHeartBeat) {
			long newHB = now - info.timestamp;
			info.heartBeat = (info.heartBeat == 0 ? newHB : (info.heartBeat + newHB) / 2);
		}
		info.timestamp = now;
	}

	private long getBroadcastPeriodFromPayload(Map<String, String> payload) {
		if (!payload.containsKey(KEY_BROADCAST_PERIOD)) {
			return -1;
		}
		return Long.parseLong(payload.get(KEY_BROADCAST_PERIOD));
	}

	synchronized void fireEvent(DiscoveryEvent event) {
		for (DiscoveryListener listener : listeners) {
			listener.onDiscovery(event);
		}
	}

	/**
	 * @return a clone of the internal discoverables.
	 */
	public synchronized Set<Discoverable> getDiscoverables() {
		Set<Discoverable> discoverables = new HashSet<>();
		for (DiscoverableInfo info : infoMap.values()) {
			discoverables.add(info.packet);
		}
		return discoverables;
	}

	public synchronized void addDiscoveryListener(DiscoveryListener listener) {
		listeners.add(listener);
	}

	public synchronized void removeDiscoveryListener(DiscoveryListener listener) {
		listeners.remove(listener);
	}
}
