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

import static org.openjdk.jmc.common.unit.UnitLookup.BYTE;
import static org.openjdk.jmc.common.unit.UnitLookup.FLAG;
import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;
import static org.openjdk.jmc.common.unit.UnitLookup.SECOND;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.POINT_IN_TIME;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.POSITIVE_MEMORY;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.POSITIVE_TIMESPAN;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_DESTINATION_COMPRESSED;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_DESTINATION_FILE;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_DUMP_ON_EXIT;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_DURATION;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_MAX_AGE;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_MAX_SIZE;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_NAME;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_START_TIME;
import static org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder.KEY_TO_DISK;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.configuration.OptionInfo;

public class KnownRecordingOptions {
	public final static Map<String, IOptionDescriptor<?>> DESCRIPTORS_BY_KEY_V1;
	public final static Map<String, IOptionDescriptor<?>> DESCRIPTORS_BY_KEY_V2;

	public final static IDescribedMap<String> OPTION_DEFAULTS_V1;
	public final static IDescribedMap<String> OPTION_DEFAULTS_V2;

	private final static String KEY_TO_DISK_V2 = "disk"; //$NON-NLS-1$

	static {
		Map<String, IOptionDescriptor<?>> recOptionsV1 = new LinkedHashMap<>();
		// Options common to JFR V1 and V2
		recOptionsV1.put(KEY_NAME, option("RECORDING_NAME", PLAIN_TEXT.getPersister(), //$NON-NLS-1$
				Messages.getString(Messages.RecordingOption_DEFAULT_RECORDING_NAME)));
		recOptionsV1.put(KEY_DURATION, option("DURATION", POSITIVE_TIMESPAN, SECOND.quantity(30))); //$NON-NLS-1$
		recOptionsV1.put(KEY_MAX_SIZE, option("MAXIMUM_SIZE", POSITIVE_MEMORY, BYTE.quantity(0))); //$NON-NLS-1$
		recOptionsV1.put(KEY_MAX_AGE, option("MAXIMUM_AGE", POSITIVE_TIMESPAN, SECOND.quantity(0))); //$NON-NLS-1$

		// Initialize V2 options from V1 options so far
		Map<String, IOptionDescriptor<?>> recOptionsV2 = new LinkedHashMap<>(recOptionsV1);

		// Options unique to JFR V1
		recOptionsV1.put(KEY_DESTINATION_FILE,
				option("DESTINATION_FILE", UnitLookup.PLAIN_TEXT.getPersister(), "recording.jfr")); //$NON-NLS-1$ //$NON-NLS-2$
		recOptionsV1.put(KEY_START_TIME, option("START_TIME", POINT_IN_TIME, null)); //$NON-NLS-1$
		recOptionsV1.put(KEY_DESTINATION_COMPRESSED,
				option("DESTINATION_COMPRESSED", FLAG.getPersister(), Boolean.FALSE)); //$NON-NLS-1$
		// Option renamed from JFR V1 to V2
		OptionInfo<Boolean> diskOption = option("TO_DISK", FLAG.getPersister(), Boolean.FALSE); //$NON-NLS-1$
		recOptionsV1.put(KEY_TO_DISK, diskOption);
		recOptionsV2.put(KEY_TO_DISK_V2, diskOption);

		// Option unique to JFR V2
		recOptionsV2.put(KEY_DUMP_ON_EXIT, option("DUMP_ON_EXIT", FLAG.getPersister(), Boolean.FALSE)); //$NON-NLS-1$

		DESCRIPTORS_BY_KEY_V1 = Collections.unmodifiableMap(recOptionsV1);
		DESCRIPTORS_BY_KEY_V2 = Collections.unmodifiableMap(recOptionsV2);

		OPTION_DEFAULTS_V1 = new DefaultValueMap<>(recOptionsV1);
		OPTION_DEFAULTS_V2 = new KeyTranslatingMap.Described<>(new DefaultValueMap<>(recOptionsV2),
				Collections.singletonMap(KEY_TO_DISK, KEY_TO_DISK_V2));
	}

	private static <T> OptionInfo<T> option(String optionName, IConstraint<T> constraint, T defaultValue) {
		// Generate keys for translation lookup
		String baseName = "RecordingOption_" + optionName.toUpperCase(); //$NON-NLS-1$
		String label = Messages.getString(baseName + "_LABEL"); //$NON-NLS-1$
		String desc = Messages.getString(baseName + "_DESC"); //$NON-NLS-1$
		return new OptionInfo<>(label, desc, constraint, defaultValue);
	}
}
