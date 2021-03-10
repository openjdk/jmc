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
package org.openjdk.jmc.flightrecorder.configuration.internal;

import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.internal.EventAppearance;

/**
 * Identifier class needed since currently you cannot from a single event URI identify where the
 * descriptive event path begins.
 */
public final class EventTypeIDV1 implements IEventTypeID {

	// FIXME: When usage has settled, revise stored state and convenience methods.
	private final String producerURI;
	private final String eventURI;
	private final String eventPath;
	private String[] cachedFallbackHierarchy;

	/**
	 * By (accidental?) convention, {@code producerURI} should end in a slash. {@code eventPath}
	 * should not begin, nor end, in a slash.
	 *
	 * @param producerURI
	 * @param eventPath
	 */
	public EventTypeIDV1(String producerURI, String eventPath) {
		this(producerURI + eventPath, producerURI.length());
	}

	public EventTypeIDV1(String eventURI, int producerEndPos) {
		// This ensures that the various strings are consistent (and reuses the same char[], which may be non-optimal).
		this.eventURI = eventURI;
		eventPath = eventURI.substring(producerEndPos);
		producerURI = eventURI.substring(0, producerEndPos);
	}

	/**
	 * May be null.
	 *
	 * @return
	 */
	@Override
	public String getProducerKey() {
		return producerURI;
	}

	@Override
	public String getRelativeKey() {
		return eventPath;
	}

	@Override
	public String[] getFallbackHierarchy() {
		if (cachedFallbackHierarchy == null) {
			cachedFallbackHierarchy = EventAppearance.getHumanSegmentArray(eventPath);
		}
		return cachedFallbackHierarchy;
	}

	@Override
	public String getFullKey() {
		return eventURI;
	}

	@Override
	public String getFullKey(String optionKey) {
		// FIXME: Use slash instead?
		return eventURI + ':' + optionKey;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof EventTypeIDV1) {
			EventTypeIDV1 otherID = (EventTypeIDV1) other;
			return eventURI.equals(otherID.eventURI) && eventPath.equals(otherID.eventPath);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return eventURI.hashCode();
	}

	@Override
	public String toString() {
		return eventURI;
	}
}
