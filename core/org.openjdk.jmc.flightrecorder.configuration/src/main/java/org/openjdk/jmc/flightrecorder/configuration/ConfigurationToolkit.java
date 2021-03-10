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
package org.openjdk.jmc.flightrecorder.configuration;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV1;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV2;
import org.openjdk.jmc.flightrecorder.configuration.internal.KnownEventOptions;
import org.openjdk.jmc.flightrecorder.configuration.internal.KnownRecordingOptions;

/**
 * Provisional entry point to obtain Flight Recorder meta data.
 */
public class ConfigurationToolkit {
	private final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.flightrecorder.configuration"); //$NON-NLS-1$

	public static Logger getLogger() {
		return LOGGER;
	}

	public static IDescribedMap<String> getRecordingOptions(JavaVersion version) {
		if (version.isGreaterOrEqualThan(JavaVersionSupport.JDK_9)) {
			return KnownRecordingOptions.OPTION_DEFAULTS_V2;
		} else if (version.isGreaterOrEqualThan(JavaVersionSupport.JDK_7_U_4)) {
			return KnownRecordingOptions.OPTION_DEFAULTS_V1;
		}
		return null;
	}

	public static IDescribedMap<String> getRecordingOptions(SchemaVersion version) {
		switch (version) {
		case V1:
			return KnownRecordingOptions.OPTION_DEFAULTS_V1;
		case V2:
			return KnownRecordingOptions.OPTION_DEFAULTS_V2;
		default:
		}
		return null;
	}

	public static IDescribedMap<EventOptionID> getEventOptions(SchemaVersion version) {
		switch (version) {
		case V1:
			return KnownEventOptions.OPTION_DEFAULTS_V1;
		case V2:
			return KnownEventOptions.OPTION_DEFAULTS_V2;
		default:
		}
		return null;
	}

	public static IEventTypeID createEventTypeID(String producerURI, String eventPath) {
		return new EventTypeIDV1(producerURI, eventPath);
	}

	public static IEventTypeID createEventTypeID(String eventName) {
		return new EventTypeIDV2(eventName);
	}

	public static <K> IConstrainedMap<K> extractDelta(IConstrainedMap<K> options, IConstrainedMap<K> baseline) {
		IMutableConstrainedMap<K> deltas = options.emptyWithSameConstraints();
		for (K key : options.keySet()) {
			Object value = options.get(key);
			if ((value != null) && !value.equals(baseline.get(key))) {
				try {
					deltas.put(key, value);
				} catch (QuantityConversionException e) {
					// Shouldn't really happen.
					LOGGER.log(Level.FINE, "Couldn't convert when extracting delta.", e); //$NON-NLS-1$
				}
			}
		}
		return deltas;
	}
}
