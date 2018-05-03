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
import static org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.fromDate;

import java.util.Date;
import java.util.Map;

import javax.management.ObjectName;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;

/**
 * This class represents a handle to a server side JDK Flight Recorder recording. It is immutable,
 * and thus thread safe.
 */
@SuppressWarnings("nls")
public final class RecordingDescriptorV1 implements IRecordingDescriptor {
	private final String serverId;
	private final ObjectName objectName;
	private final Long id;
	private final String name;
	private final Map<String, ?> options;
	private final RecordingState state;
	private final IQuantity dataStartTime;
	private final IQuantity dataEndTime;

	/**
	 * Horrid constructor. Not visible to the end user.
	 *
	 * @param serverId
	 *            the id of the server where the recording originated from.
	 * @param id
	 *            the id of the recording.
	 * @param name
	 *            the name of the recording.
	 * @param isStarted
	 *            has the recording been started?
	 * @param isStopped
	 *            has the recording been stopped?
	 * @param isRunning
	 *            is the recording running?
	 * @param options
	 *            the recording options.
	 * @param dataStartTime
	 *            start time of data
	 * @param dataEndTime
	 *            end time of data
	 * @param objectName
	 *            the object name used to locate the mbean managing the recording.
	 */
	RecordingDescriptorV1(String serverId, Long id, String name, boolean isStarted, boolean isStopped,
			boolean isRunning, Map<String, ?> options, IQuantity dataStartTime, IQuantity dataEndTime,
			ObjectName objectName) {
		this.serverId = serverId;
		this.id = id;
		this.name = name;
		state = decideState(isStarted, isStopped, isRunning);
		this.options = options;
		this.objectName = objectName;
		this.dataStartTime = dataStartTime;
		this.dataEndTime = dataEndTime;
	}

	private RecordingState decideState(boolean isStarted, boolean isStopped, boolean isRunning) {
		if (!isStarted) {
			return RecordingState.CREATED;
		} else if (isRunning) {
			return RecordingState.RUNNING;
		} else if (isStopped) {
			return RecordingState.STOPPED;
		} else {
			return RecordingState.STOPPING;
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
		return options;
	}

	@Override
	public ObjectName getObjectName() {
		return objectName;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof RecordingDescriptorV1) {
			RecordingDescriptorV1 that = (RecordingDescriptorV1) o;
			return that.id.equals(id) && that.serverId.equals(serverId);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return id.intValue() ^ serverId.hashCode();
	}

	@Override
	public String toString() {
		return "RecordingDescriptor@" + serverId + '[' + getName() + '(' + getId() + "), " + getState() + ", "
				+ getOptions() + ']';
	}

	@Override
	public IQuantity getDataStartTime() {
		return dataStartTime;
	}

	@Override
	public IQuantity getDataEndTime() {
		return dataEndTime;
	}

	@Override
	public IQuantity getStartTime() {
		Object startTime = getOptions().get(RecordingOptionsBuilder.KEY_START_TIME);
		if (startTime instanceof Date) {
			return fromDate((Date) startTime);
		}
		return null;
	}

	@Override
	public IQuantity getDuration() {
		return getLongQuantity(MILLISECOND, RecordingOptionsBuilder.KEY_DURATION);
	}

	@Override
	public boolean isContinuous() {
		IQuantity duration = getDuration();
		return (duration == null) || (duration.doubleValue() == 0.0);
	}

	@Override
	public boolean getToDisk() {
		Object object = getOptions().get(RecordingOptionsBuilder.KEY_TO_DISK);
		if (object instanceof Boolean) {
			return ((Boolean) object).booleanValue();
		}
		return false;
	}

	@Override
	public IQuantity getMaxAge() {
		return getLongQuantity(MILLISECOND, RecordingOptionsBuilder.KEY_MAX_AGE);
	}

	@Override
	public IQuantity getMaxSize() {
		return getLongQuantity(BYTE, RecordingOptionsBuilder.KEY_MAX_SIZE);
	}

	private IQuantity getLongQuantity(IUnit unit, String optionKey) {
		Object object = getOptions().get(optionKey);
		if (object instanceof Long) {
			return OpenTypeConverter.inGuessedUnit(unit.quantity((Long) object));
		}
		return null;
	}
}
