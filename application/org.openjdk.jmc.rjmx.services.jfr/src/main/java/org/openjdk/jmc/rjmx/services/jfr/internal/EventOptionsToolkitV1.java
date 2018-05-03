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
package org.openjdk.jmc.rjmx.services.jfr.internal;

import static org.openjdk.jmc.flightrecorder.configuration.internal.KnownEventOptions.EVENT_OPTIONS_BY_KEY_V1;
import static org.openjdk.jmc.flightrecorder.configuration.internal.KnownEventOptions.KEY_ENABLED;
import static org.openjdk.jmc.flightrecorder.configuration.internal.KnownEventOptions.KEY_PERIOD;
import static org.openjdk.jmc.flightrecorder.configuration.internal.KnownEventOptions.KEY_STACKTRACE;
import static org.openjdk.jmc.flightrecorder.configuration.internal.KnownEventOptions.KEY_THRESHOLD;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.rjmx.RJMXPlugin;

/**
 * Toolkit for marshalling JFR 1.0 (JDK7/8) event options.
 */
public final class EventOptionsToolkitV1 {
	private final static String KEY_ID = "id"; //$NON-NLS-1$
	private final static String KEY_STACKTRACE_SERVER = "stacktrace"; //$NON-NLS-1$
	private final static String KEY_PERIOD_SERVER = "requestPeriod"; //$NON-NLS-1$

	private final static Map<String, OpenTypeConverter<?, ?>> CONVERTERS_BY_EVENT_OPTION_KEY;
	private final static Map<String, String> CAPABILITY_KEYS_BY_OPTION_KEY;

	private final static String[] SERVER_NAMES = new String[] {KEY_ID, KEY_THRESHOLD, KEY_STACKTRACE_SERVER,
			KEY_PERIOD_SERVER, KEY_ENABLED};
	private final static OpenType<?>[] OPEN_TYPES = new OpenType[] {SimpleType.INTEGER, SimpleType.LONG,
			SimpleType.BOOLEAN, SimpleType.LONG, SimpleType.BOOLEAN};

	public final static CompositeType OPTIONS_COMPOSITE_TYPE;

	static {
		Map<String, OpenTypeConverter<?, ?>> converters = new LinkedHashMap<>();
		Map<String, String> capabilities = new HashMap<>();

		converters.put(KEY_THRESHOLD, OpenTypeConverter.NANOSECONDS);
		capabilities.put(KEY_THRESHOLD, "isTimed"); //$NON-NLS-1$

		converters.put(KEY_STACKTRACE, OpenTypeConverter.BOOLEAN);
		capabilities.put(KEY_STACKTRACE, "isStackTraceAvailable"); //$NON-NLS-1$

		converters.put(KEY_PERIOD, OpenTypeConverter.MILLIS_PERIODICITY);
		capabilities.put(KEY_PERIOD, "isRequestable"); //$NON-NLS-1$

		converters.put(KEY_ENABLED, OpenTypeConverter.BOOLEAN);

		CONVERTERS_BY_EVENT_OPTION_KEY = converters;
		CAPABILITY_KEYS_BY_OPTION_KEY = capabilities;
		OPTIONS_COMPOSITE_TYPE = generateOptionsType();
	}

	@SuppressWarnings("nls")
	private static CompositeType generateOptionsType() {
		try {
			return new CompositeType("EventOptions", "Event Options", SERVER_NAMES, SERVER_NAMES, OPEN_TYPES);
		} catch (Exception e) {
			// Will not ever happen!
		}
		return null;
	}

	@SuppressWarnings("nls")
	private EventOptionsToolkitV1() {
		throw new AssertionError("Not to be instantiated!");
	}

	/**
	 * This helper does the ad-hoc mapping from capabilities to parameter names and constraints. The
	 * capabilities are used to check for which parameters are actually accepted. The parameter
	 * space to check is currently static - we can't get this data from the server today.
	 *
	 * @param capabilities
	 *            the capabilities {@link Map} from the event type that describes (in an ad-hoc way)
	 *            which parameters are accepted.
	 * @return the names and constraints for the parameters that actually are accepted.
	 */
	public static Map<String, IOptionDescriptor<?>> getConfigurableOptions(CompositeData data) {
		Map<String, IOptionDescriptor<?>> optionMap = new HashMap<>();
		for (Entry<String, IOptionDescriptor<?>> entry : EVENT_OPTIONS_BY_KEY_V1.entrySet()) {
			String capKey = CAPABILITY_KEYS_BY_OPTION_KEY.get(entry.getKey());
			if ((capKey == null) || Boolean.TRUE.equals(data.get(capKey))) {
				optionMap.put(entry.getKey(), entry.getValue());
			}
		}
		return optionMap;
	}

	/**
	 * Converts the event settings to a list of composite data, usable by the MBean API.
	 *
	 * @throws OpenDataException
	 */
	public static List<CompositeData> encodeAllEventSettings(
		Collection<EventTypeMetadataV1> availableEventTypes, IConstrainedMap<EventOptionID> settings)
			throws OpenDataException {
		List<CompositeData> eventSettings = new ArrayList<>();
		for (EventTypeMetadataV1 eventType : availableEventTypes) {
			Object[] values = new Object[] {eventType.getId(), -1L, Boolean.FALSE, -1L, Boolean.FALSE};
			int i = 1;
			for (Entry<String, OpenTypeConverter<?, ?>> entry : CONVERTERS_BY_EVENT_OPTION_KEY.entrySet()) {
				// Only change options the event type is supposed to have.
				if (eventType.getOptionInfo(entry.getKey()) != null) {
					EventOptionID optionID = new EventOptionID(eventType.getEventTypeID(), entry.getKey());
					Object value = settings.get(optionID);
					// FIXME: Check exact semantics of JFR 1.0 here, when to override (0 vs. -1).
					if (value != null) {
						try {
							values[i] = RecordingOptionsToolkitV1.toOpenTypeWithCast(entry.getValue(), value);
						} catch (QuantityConversionException e) {
							RJMXPlugin.getDefault().getLogger().log(Level.WARNING, e.getMessage(), e);
						}
					}
				}
				i++;
			}
			eventSettings.add(new CompositeDataSupport(OPTIONS_COMPOSITE_TYPE, SERVER_NAMES, values));
		}
		return eventSettings;
	}

	/**
	 * Adds options from the composite data to the constrained map, filtered by the given event type
	 * metadata.
	 */
	@SuppressWarnings("nls")
	public static void addOptionsToV1(
		IMutableConstrainedMap<EventOptionID> options, EventTypeMetadataV1 eventType, CompositeData data) {
		for (String serverKey : data.getCompositeType().keySet()) {
			String localKey = serverKey;
			if (serverKey.equals(KEY_PERIOD_SERVER)) {
				localKey = KEY_PERIOD;
			} else if (localKey.equals(KEY_STACKTRACE_SERVER)) {
				localKey = KEY_STACKTRACE;
			}
			IOptionDescriptor<?> optionInfo = eventType.getOptionInfo(localKey);
			OpenTypeConverter<?, ?> converter = CONVERTERS_BY_EVENT_OPTION_KEY.get(localKey);
			if ((optionInfo != null) && (converter != null)) {
				assert optionInfo.getConstraint() == converter.constraint;
				EventOptionID optionID = new EventOptionID(eventType.getEventTypeID(), localKey);
				try {
					putWithCast(options, optionID, converter, data.get(serverKey));
				} catch (QuantityConversionException e) {
					// This should not happen
					RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Problem with value for option " + optionID,
							e);
				}
			}
		}
	}

	/**
	 * @throws QuantityConversionException
	 */
	static <K, P, T> void putWithCast(
		IMutableConstrainedMap<K> map, K key, OpenTypeConverter<P, T> converter, Object openValue)
			throws QuantityConversionException {
		T value = converter.fromOpenType(converter.getType().cast(openValue));
		map.put(key, converter.constraint, value);
	}
}
