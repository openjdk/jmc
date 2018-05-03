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

import static org.openjdk.jmc.flightrecorder.configuration.internal.KnownRecordingOptions.DESCRIPTORS_BY_KEY_V1;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_DESTINATION_COMPRESSED;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_DESTINATION_FILE;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_DURATION;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_MAX_AGE;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_MAX_SIZE;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_NAME;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_START_TIME;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_TO_DISK;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.flightrecorder.configuration.ConfigurationToolkit;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;

/**
 * Toolkit for handling marshalling of RecordingOptions for JFR 1.0 (JDK 7/8).
 */
public final class RecordingOptionsToolkitV1 {
	private final static Map<String, OpenTypeConverter<?, ?>> CONVERTERS_BY_REC_OPTION_KEY;

	private final static String[] NAMES;

	private final static CompositeType OPTIONS_TYPE;

	static {
		Map<String, OpenTypeConverter<?, ?>> converters = new LinkedHashMap<>();

		// Maintain order with KnownRecordingOptions
		// FIXME: Add test to verify that keys are the same?
		converters.put(KEY_NAME, OpenTypeConverter.TEXT);
		converters.put(KEY_TO_DISK, OpenTypeConverter.BOOLEAN);
		converters.put(KEY_DURATION, OpenTypeConverter.MILLISECONDS);
		converters.put(KEY_MAX_SIZE, OpenTypeConverter.BYTES);
		converters.put(KEY_MAX_AGE, OpenTypeConverter.MILLISECONDS);
		converters.put(KEY_DESTINATION_FILE, OpenTypeConverter.FILE_NAME);
		converters.put(KEY_START_TIME, OpenTypeConverter.DATE);
		converters.put(KEY_DESTINATION_COMPRESSED, OpenTypeConverter.BOOLEAN);

		CONVERTERS_BY_REC_OPTION_KEY = converters;

		NAMES = new String[converters.size()];
		OPTIONS_TYPE = createCompositeType("RecordingOptions", "Recording Options", DESCRIPTORS_BY_KEY_V1, NAMES); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Create a CompositeType from {@link IOptionDescriptor}s and populate an array with item names
	 * to be used when creating {@link CompositeData}s.
	 *
	 * @param name
	 * @param description
	 * @param descriptorMap
	 * @param names
	 *            an empty array with the length descriptors.{@link Map#size() size()} to be
	 *            populated
	 * @return
	 */
	private static CompositeType createCompositeType(
		String name, String description, Map<String, IOptionDescriptor<?>> descriptorMap, String[] names) {
		int len = descriptorMap.size();
		String[] descriptions = new String[len];
		OpenType<?>[] openTypes = new OpenType<?>[len];

		int i = 0;
		for (Entry<String, IOptionDescriptor<?>> descriptorEntry : descriptorMap.entrySet()) {
			String key = descriptorEntry.getKey();
			names[i] = key;
			descriptions[i] = descriptorEntry.getValue().getDescription();
			openTypes[i] = CONVERTERS_BY_REC_OPTION_KEY.get(key).getOpenType();
			i++;
		}

		try {
			return new CompositeType("RecordingOptions", "Recording Options", names, descriptions, openTypes); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e) {
			// Will not ever happen!
			e.printStackTrace();
			return null;
		}
	}

	private RecordingOptionsToolkitV1() {
		throw new AssertionError("Not to be instantiatied!"); //$NON-NLS-1$
	}

	public static CompositeData getRecordingOptions(IConstrainedMap<String> options) throws OpenDataException {
		Object[] values = new Object[NAMES.length];
		for (int i = 0; i < NAMES.length; i++) {
			String key = NAMES[i];
			OpenTypeConverter<?, ?> converter = CONVERTERS_BY_REC_OPTION_KEY.get(key);
			Object value = options.get(key);
			if (value == null) {
				value = DESCRIPTORS_BY_KEY_V1.get(key).getDefault();
			}
			if (value != null) {
				try {
					values[i] = toOpenTypeWithCast(converter, value);
				} catch (QuantityConversionException e) {
					// FIXME: Add proper logging here
					e.printStackTrace();
				}
			}
		}
		return new CompositeDataSupport(OPTIONS_TYPE, NAMES, values);
	}

	@SuppressWarnings("unchecked")
	static <P, T> P toOpenTypeWithCast(OpenTypeConverter<P, T> converter, Object value)
			throws QuantityConversionException {
		return converter.toOpenType((T) value);
	}

	public static Map<String, IOptionDescriptor<?>> getAvailableRecordingOptions() {
		return DESCRIPTORS_BY_KEY_V1;
	}

	/**
	 * By recording options builder key...
	 *
	 * @param propertyKey
	 */
	public static IConstraint<?> getRecordingOptionConstraint(String propertyKey) {
		IOptionDescriptor<?> desc = DESCRIPTORS_BY_KEY_V1.get(propertyKey);
		return (desc != null) ? desc.getConstraint() : null;
	}

	public static IConstrainedMap<String> toRecordingOptions(CompositeData recordingOptions)
			throws FlightRecorderException {
		IMutableConstrainedMap<String> options = ConfigurationToolkit.getRecordingOptions(JavaVersionSupport.JDK_8)
				.emptyWithSameConstraints();
		for (String key : recordingOptions.getCompositeType().keySet()) {
			OpenTypeConverter<?, ?> converter = CONVERTERS_BY_REC_OPTION_KEY.get(key);
			if (converter == null) {
				converter = OpenTypeConverter.TEXT;
			}
			try {
				Object openValue = recordingOptions.get(key);
				// FIXME: May need to rethink how to handle null values. Store constraint but not value?
				// The problem is if null would be allowed but not default, like for recording start time.
				if (openValue != null) {
					EventOptionsToolkitV1.putWithCast(options, key, converter, openValue);
				}
			} catch (QuantityConversionException e) {
				// FIXME: Add proper logging here
				e.printStackTrace();
			}
		}
		return options;
	}

	public static String getName(IConstrainedMap<String> recordingOptions) {
		return (String) recordingOptions.get(RecordingOptionsBuilder.KEY_NAME);
	}
}
