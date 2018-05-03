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
package org.openjdk.jmc.flightrecorder.configuration.recording;

import java.util.Date;
import java.util.Properties;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.configuration.IRecorderConfigurationService;
import org.openjdk.jmc.flightrecorder.configuration.internal.ValidationToolkit;

/**
 * Builder to help build recording options. These are typically the options relevant to the
 * recording other than the event type options, such as the duration or when to start the recording.
 */
public class RecordingOptionsBuilder {
	public final static String KEY_NAME = "name"; //$NON-NLS-1$
	/** The requested duration of a recording */
	public final static String KEY_DURATION = "duration"; //$NON-NLS-1$
	public final static String KEY_DESTINATION_FILE = "destinationFile"; //$NON-NLS-1$
	public final static String KEY_DESTINATION_COMPRESSED = "destinationCompressed"; //$NON-NLS-1$
	public final static String KEY_START_TIME = "startTime"; //$NON-NLS-1$
	public final static String KEY_MAX_SIZE = "maxSize"; //$NON-NLS-1$
	public final static String KEY_MAX_AGE = "maxAge"; //$NON-NLS-1$
	public final static String KEY_TO_DISK = "toDisk"; //$NON-NLS-1$
	public final static String KEY_DUMP_ON_EXIT = "dumpOnExit"; //$NON-NLS-1$
	private final static String DEFAULT_NAME = "Unnamed Recording"; //$NON-NLS-1$
	private final IMutableConstrainedMap<String> map;

	public RecordingOptionsBuilder(IRecorderConfigurationService service) throws QuantityConversionException {
		this(service.getDefaultRecordingOptions().emptyWithSameConstraints());
	}

	public RecordingOptionsBuilder(IMutableConstrainedMap<String> map) throws QuantityConversionException {
		this.map = map;
		name(DEFAULT_NAME);
	}

	public RecordingOptionsBuilder duration(long duration) throws QuantityConversionException {
		return duration(UnitLookup.MILLISECOND.quantity(duration));
	}

	public RecordingOptionsBuilder duration(IQuantity duration) throws QuantityConversionException {
		map.put(KEY_DURATION, duration);
		return this;
	}

	public RecordingOptionsBuilder destinationFile(String fileName) throws QuantityConversionException {
		map.put(KEY_DESTINATION_FILE, fileName);
		return this;
	}

	public RecordingOptionsBuilder destinationCompressed(boolean compress) throws QuantityConversionException {
		map.put(KEY_DESTINATION_COMPRESSED, Boolean.valueOf(compress));
		return this;
	}

	public RecordingOptionsBuilder startTime(Date startTime) throws QuantityConversionException {
		return startTime(UnitLookup.fromDate(startTime));
	}

	public RecordingOptionsBuilder startTime(IQuantity startTime) throws QuantityConversionException {
		map.put(KEY_START_TIME, startTime);
		return this;
	}

	public RecordingOptionsBuilder maxSize(long maxSize) throws QuantityConversionException {
		return maxSize(UnitLookup.BYTE.quantity(maxSize));
	}

	public RecordingOptionsBuilder maxSize(IQuantity maxSize) throws QuantityConversionException {
		map.put(KEY_MAX_SIZE, maxSize);
		return this;
	}

	public RecordingOptionsBuilder maxAge(long maxAge) throws QuantityConversionException {
		return maxAge(UnitLookup.SECOND.quantity(maxAge));
	}

	public RecordingOptionsBuilder maxAge(IQuantity maxAge) throws QuantityConversionException {
		map.put(KEY_MAX_AGE, maxAge);
		return this;
	}

	public RecordingOptionsBuilder name(String name) throws QuantityConversionException {
		map.put(KEY_NAME, name);
		return this;
	}

	public RecordingOptionsBuilder toDisk(boolean toDisk) throws QuantityConversionException {
		map.put(KEY_TO_DISK, Boolean.valueOf(toDisk));
		return this;
	}

	/**
	 * Will initialize the builder from a properties collection. Typing of individual content will
	 * be attempted.
	 */
	public RecordingOptionsBuilder fromProperties(Properties props) throws QuantityConversionException {
		for (Object element : props.keySet()) {
			String key = (String) element;
			addByKey(key, props.getProperty(key));
		}
		return this;
	}

	public RecordingOptionsBuilder addByKey(String key, String property) throws QuantityConversionException {
		map.putPersistedString(key, property);
		return this;
	}

	public IConstrainedMap<String> build() throws QuantityConversionException {
		ValidationToolkit.validate(map);
		return map;
	}
}
