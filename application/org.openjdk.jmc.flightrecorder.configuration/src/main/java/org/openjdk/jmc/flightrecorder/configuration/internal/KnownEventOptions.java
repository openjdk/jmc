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

import static org.openjdk.jmc.common.unit.UnitLookup.NANOSECOND;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.PERIOD_V1;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.PERIOD_V2;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.POSITIVE_TIMESPAN;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.configuration.OptionInfo;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;

public class KnownEventOptions {
	public final static String KEY_THRESHOLD = "threshold"; //$NON-NLS-1$
	public final static String KEY_STACKTRACE = "stackTrace"; //$NON-NLS-1$
	public final static String KEY_PERIOD = "period"; //$NON-NLS-1$
	public final static String KEY_ENABLED = "enabled"; //$NON-NLS-1$

	public final static Map<String, IOptionDescriptor<?>> EVENT_OPTIONS_BY_KEY_V1;
	public final static Map<String, IOptionDescriptor<?>> EVENT_OPTIONS_BY_KEY_V2;

	public final static IDescribedMap<EventOptionID> OPTION_DEFAULTS_V1;
	public final static IDescribedMap<EventOptionID> OPTION_DEFAULTS_V2;

	static {
		Map<String, IOptionDescriptor<?>> eventOptionsV1 = new LinkedHashMap<>();
		// Options identical between JFR V1 and V2
		eventOptionsV1.put(KEY_THRESHOLD, option(KEY_THRESHOLD, POSITIVE_TIMESPAN, NANOSECOND.quantity(20)));
		eventOptionsV1.put(KEY_STACKTRACE, option(KEY_STACKTRACE, UnitLookup.FLAG.getPersister(), Boolean.TRUE));
		eventOptionsV1.put(KEY_ENABLED, option(KEY_ENABLED, UnitLookup.FLAG.getPersister(), Boolean.TRUE));

		// Initialize V2 options from V1 options so far
		Map<String, IOptionDescriptor<?>> eventOptionsV2 = new LinkedHashMap<>(eventOptionsV1);

		// Option differing between JFR V1 and V2
		eventOptionsV1.put(KEY_PERIOD, option(KEY_PERIOD, PERIOD_V1, UnitLookup.MILLISECOND.quantity(20)));
		eventOptionsV2.put(KEY_PERIOD, option(KEY_PERIOD, PERIOD_V2, UnitLookup.MILLISECOND.quantity(20)));

		EVENT_OPTIONS_BY_KEY_V1 = Collections.unmodifiableMap(eventOptionsV1);
		EVENT_OPTIONS_BY_KEY_V2 = Collections.unmodifiableMap(eventOptionsV2);

		OPTION_DEFAULTS_V1 = new DefaultValueMap<>(
				new EventOptionDescriptorMapper(EventTypeIDV1.class, eventOptionsV1, false));
		OPTION_DEFAULTS_V2 = new DefaultValueMap<>(
				new EventOptionDescriptorMapper(EventTypeIDV2.class, eventOptionsV2, true));
	}

	private static <T> OptionInfo<T> option(String optionName, IConstraint<T> constraint, T defaultValue) {
		// Generate keys for translation lookup
		String baseName = "EventOption_" + optionName.toUpperCase(); //$NON-NLS-1$
		String label = Messages.getString(baseName + "_LABEL"); //$NON-NLS-1$
		String desc = Messages.getString(baseName + "_DESC"); //$NON-NLS-1$
		return new OptionInfo<>(label, desc, constraint, defaultValue);
	}

	private KnownEventOptions() {
		throw new AssertionError("Not to be instantiated!"); //$NON-NLS-1$
	}
}
