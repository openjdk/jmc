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

import static org.openjdk.jmc.flightrecorder.configuration.internal.KnownRecordingOptions.DESCRIPTORS_BY_KEY_V2;

import java.util.Map;
import java.util.Map.Entry;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;

/**
 * Toolkit for handling marshalling of RecordingOptions for JFR 2.0 (JDK 9).
 */
public final class RecordingOptionsToolkitV2 {
	final static TabularType OPTIONS_TYPE;
	final static CompositeType OPTIONS_ROW_TYPE;

	static {
		OPTIONS_ROW_TYPE = createOptionsRowType();
		OPTIONS_TYPE = createOptionsType(OPTIONS_ROW_TYPE);
	}

	private static CompositeType createOptionsRowType() {
		String typeName = "java.util.Map<java.lang.String, java.lang.String>"; //$NON-NLS-1$
		String[] keyValue = new String[] {"key", "value"}; //$NON-NLS-1$ //$NON-NLS-2$
		OpenType<?>[] openTypes = new OpenType[] {SimpleType.STRING, SimpleType.STRING};
		try {
			return new CompositeType(typeName, typeName, keyValue, keyValue, openTypes);
		} catch (OpenDataException e) {
			// Will never happen
			return null;
		}
	}

	private static TabularType createOptionsType(CompositeType rowType) {
		try {
			return new TabularType(rowType.getTypeName(), rowType.getTypeName(), rowType, new String[] {"key"}); //$NON-NLS-1$
		} catch (OpenDataException e) {
			// Will never happen
			return null;
		}
	}

	private RecordingOptionsToolkitV2() {
		throw new AssertionError("Not to be instantiatied!"); //$NON-NLS-1$
	}

	public static TabularData createTabularData(Map<String, String> map) throws OpenDataException {
		TabularDataSupport tdata = new TabularDataSupport(OPTIONS_TYPE);
		for (Entry<String, String> entry : map.entrySet()) {
			tdata.put(new CompositeDataSupport(OPTIONS_ROW_TYPE, new String[] {"key", "value"}, //$NON-NLS-1$ //$NON-NLS-2$
					new Object[] {entry.getKey(), entry.getValue()}));
		}
		return tdata;
	}

	public static <K> TabularData toTabularData(IConstrainedMap<K> settings) throws OpenDataException {
		TabularDataSupport tdata = new TabularDataSupport(OPTIONS_TYPE);
		for (K key : settings.keySet()) {
			String value = settings.getPersistableString(key);
			if (value != null) {
				tdata.put(new CompositeDataSupport(OPTIONS_ROW_TYPE, new String[] {"key", "value"}, //$NON-NLS-1$ //$NON-NLS-2$
						new String[] {key.toString(), value}));
			}
		}
		return tdata;
	}

	public static Map<String, IOptionDescriptor<?>> getAvailableRecordingOptions() {
		return DESCRIPTORS_BY_KEY_V2;
	}

	/**
	 * By recording options builder key...
	 *
	 * @param propertyKey
	 */
	public static IConstraint<?> getRecordingOptionConstraint(String propertyKey) {
		IOptionDescriptor<?> desc = DESCRIPTORS_BY_KEY_V2.get(propertyKey);
		return (desc != null) ? desc.getConstraint() : null;
	}

	public static String getName(Map<String, ?> recordingOptions) {
		return (String) recordingOptions.get(RecordingOptionsBuilder.KEY_NAME);
	}
}
