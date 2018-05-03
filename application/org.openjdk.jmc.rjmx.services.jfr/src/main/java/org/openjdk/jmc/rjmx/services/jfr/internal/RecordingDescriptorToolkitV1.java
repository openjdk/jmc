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

import static org.openjdk.jmc.common.unit.UnitLookup.fromDate;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;

/**
 * Toolkit for managing recording descriptors.
 */
@SuppressWarnings("nls")
public final class RecordingDescriptorToolkitV1 {
	private static final String KEY_NAME = "name";
	private static final String KEY_ID = "id";
	private static final String KEY_STARTED = "started";
	private static final String KEY_STOPPED = "stopped";
	private static final String KEY_RUNNING = "running";
	private static final String KEY_OPTIONS = "options";
	private static final String KEY_DATA_START_TIME = "dataStartTime";
	private static final String KEY_DATA_END_TIME = "dataEndTime";
	private static final String KEY_OBJECT_NAME = "objectName";

	private RecordingDescriptorToolkitV1() {
		throw new AssertionError("Not to be instantiated!"); //$NON-NLS-1$
	}

	/**
	 * Creates a recording descriptor from composite data.
	 *
	 * @param serverId
	 * @param data
	 * @return the created recording descriptor
	 */
	public static IRecordingDescriptor createRecordingDescriptor(String serverId, CompositeData data) {
		Map<String, Object> options = createOptions((CompositeData) data.get(KEY_OPTIONS));
		Long id = (Long) data.get(KEY_ID);
		String name = (String) data.get(KEY_NAME);
		Date dataStartTime = (Date) data.get(KEY_DATA_START_TIME);
		Date dataEndTime = (Date) data.get(KEY_DATA_END_TIME);
		ObjectName objectName = (ObjectName) data.get(KEY_OBJECT_NAME);
		return new RecordingDescriptorV1(serverId, id, name, getBooleanKey(data, KEY_STARTED),
				getBooleanKey(data, KEY_STOPPED), getBooleanKey(data, KEY_RUNNING), options, fromDate(dataStartTime),
				fromDate(dataEndTime), objectName);
	}

	public static CompositeData createRecordingOptions(Map<String, ?> map) {
		throw new IllegalArgumentException("To be implemented!"); //$NON-NLS-1$
	}

	/**
	 * Must not be called with an empty recording!
	 *
	 * @param id
	 * @param recordings
	 * @return the recording descriptor with given id or <tt>null</tt>
	 */
	public static IRecordingDescriptor getRecordingById(long id, IRecordingDescriptor[] recordings) {
		for (IRecordingDescriptor recording : recordings) {
			if (recording.getId().longValue() == id) {
				return recording;
			}
		}
		return null;
	}

	private static boolean getBooleanKey(CompositeData data, String key) {
		Boolean value = (Boolean) data.get(key);
		return value == null ? false : value.booleanValue();
	}

	// FIXME: Why not reuse the code in RecordingOptionsToolkit? The options are the same.
	private static Map<String, Object> createOptions(CompositeData compositeData) {
		Map<String, Object> options = new HashMap<>();
		for (Object o : compositeData.getCompositeType().keySet()) {
			String key = (String) o;
			options.put(key, compositeData.get(key));
		}
		return options;
	}

	public static IRecordingDescriptor getRecordingByDescriptor(
		ObjectName objectName, List<IRecordingDescriptor> recordings) {
		for (IRecordingDescriptor recording : recordings) {
			if (objectName.equals(recording.getObjectName())) {
				return recording;
			}
		}
		return null;
	}
}
