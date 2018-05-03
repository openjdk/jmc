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

import static org.openjdk.jmc.common.unit.UnitLookup.BYTE;
import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_MS;
import static org.openjdk.jmc.common.unit.UnitLookup.SECOND;

import java.util.Map;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;

/**
 * This class represents a handle to a server side JDK Flight Recorder recording. It is immutable,
 * and thus thread safe.
 */
@SuppressWarnings("nls")
public final class RecordingDescriptorV2 implements IRecordingDescriptor {
	private static final String KEY_NAME = "name";
	private static final String KEY_ID = "id";
	private static final String KEY_START_TIME = "startTime";
	private static final String KEY_STOP_TIME = "stopTime";
	private static final String KEY_STATE = "state";
	private static final String KEY_MAX_AGE = "maxAge";
	private static final String KEY_MAX_SIZE = "maxSize";
	// This key does not seem to correspond with the one used to set recording options.
	private static final String KEY_TO_DISK = "toDisk";
	private static final String KEY_DESTINATION = "destination";
	private static final String KEY_DURATION = "duration";

	private final String serverId;
	private final long id;
	private final String name;
	private final RecordingState state;
	private final IQuantity startTime;
	private final IQuantity stopTime;
	private final IQuantity duration;
	private final IQuantity maxAge;
	private final IQuantity maxSize;
	private final boolean toDisk;
	private final String destination;

	public RecordingDescriptorV2(String serverId, CompositeData data) {
		this.serverId = serverId;
		id = (Long) data.get(KEY_ID);
		name = (String) data.get(KEY_NAME);
		state = decideState((String) data.get(KEY_STATE));
		startTime = EPOCH_MS.quantity((Long) data.get(KEY_START_TIME));
		stopTime = EPOCH_MS.quantity((Long) data.get(KEY_STOP_TIME));
		duration = OpenTypeConverter.inGuessedUnit(SECOND.quantity((Long) data.get(KEY_DURATION)));
		maxAge = OpenTypeConverter.inGuessedUnit(SECOND.quantity((Long) data.get(KEY_MAX_AGE)));
		maxSize = OpenTypeConverter.inGuessedUnit(BYTE.quantity((Long) data.get(KEY_MAX_SIZE)));
		toDisk = (Boolean) data.get(KEY_TO_DISK);
		destination = (String) data.get(KEY_DESTINATION);
	}

	private static RecordingState decideState(String state) {
		if ("NEW".equals(state)) {
			return RecordingState.CREATED;
		} else if ("RUNNING".equals(state) || "DELAYED".equals(state) || "STARTING".equals(state)) {
			return RecordingState.RUNNING;
		} else if ("STOPPED".equals(state)) {
			return RecordingState.STOPPED;
		} else if ("STOPPING".equals(state)) {
			return RecordingState.STOPPING;
		} else {
			// FIXME: CLOSED would fit better but there is no such enum value at the moment
			return RecordingState.STOPPED;
		}
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public RecordingState getState() {
		return state;
	}

	@Override
	public Map<String, ?> getOptions() {
		return null;
	}

	@Override
	public ObjectName getObjectName() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof RecordingDescriptorV2) {
			RecordingDescriptorV2 that = (RecordingDescriptorV2) o;
			return that.id == id && that.serverId.equals(serverId);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (int) id ^ serverId.hashCode();
	}

	@Override
	public String toString() {
		return "RecordingDescriptor@" + serverId + '[' + getName() + '(' + getId() + "), " + getState() + ", "
				+ getOptions() + ']';
	}

	@Override
	public IQuantity getDataStartTime() {
		return startTime;
	}

	@Override
	public IQuantity getDataEndTime() {
		return stopTime;
	}

	@Override
	public IQuantity getStartTime() {
		return startTime;
	}

	@Override
	public IQuantity getDuration() {
		return duration;
	}

	@Override
	public boolean isContinuous() {
		return (duration == null) || (duration.doubleValue() == 0.0);
	}

	@Override
	public boolean getToDisk() {
		return toDisk;
	}

	@Override
	public IQuantity getMaxAge() {
		return maxAge;
	}

	@Override
	public IQuantity getMaxSize() {
		return maxSize;
	}

	public String getDestination() {
		return destination;
	}
}
