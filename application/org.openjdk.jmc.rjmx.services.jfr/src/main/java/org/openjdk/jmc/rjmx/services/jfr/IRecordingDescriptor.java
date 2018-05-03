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
package org.openjdk.jmc.rjmx.services.jfr;

import java.util.Map;

import javax.management.ObjectName;

import org.openjdk.jmc.common.unit.IQuantity;

/**
 * Interface that describe a JDK Flight Recorder recording.
 */
public interface IRecordingDescriptor {
	/**
	 * Defines the possible states a recording can be in. {@link RecordingState#CREATED}
	 * {@link RecordingState#RUNNING} {@link RecordingState#STOPPED}
	 */
	public enum RecordingState {
		/**
		 * The Recording has been created but not yet started.
		 */
		CREATED,
		/**
		 * The recording is running, i.e. it has been started, but not yet stopped.
		 */
		RUNNING,
		/**
		 * The recording has been started, and is stopping, but has not fully completed.
		 */
		STOPPING,
		/**
		 * The recording has been started, and then stopped. Either because the recording duration
		 * timed out, or because it was forced to stop.
		 */
		STOPPED
	}

	/**
	 * Returns the id value of the recording.
	 *
	 * @return the id value of the recording.
	 */
	Long getId();

	/**
	 * Returns the symbolic name of the recording.
	 *
	 * @return the symbolic name of the recording.
	 */
	String getName();

	/**
	 * Returns the state of the recording when this {@link IRecordingDescriptor} was created.
	 *
	 * @return the state of the recording when this {@link IRecordingDescriptor} was created.
	 */
	RecordingState getState();

	/**
	 * Returns a Map&lt;String, Object&gt; with values that describes the various options in the
	 * recording. Options can, for instance, be duration and destFile.
	 *
	 * @return a Map&lt;String, Object&gt; with values that describes the various options in the
	 *         recording.
	 */
	Map<String, ?> getOptions();

	/**
	 * Returns the object name used to locate the MBean that is used to manage this recording.
	 *
	 * @return the object name used to locate the MBean that is used to manage this recording.
	 */
	ObjectName getObjectName();

	/**
	 * Returns the data start time for this recording.
	 *
	 * @return the data start time for the recording
	 */
	public IQuantity getDataStartTime();

	/**
	 * Returns the data end time for this recording.
	 *
	 * @return the data end time for the recording
	 */
	public IQuantity getDataEndTime();

	/**
	 * Returns the start time for this recording.
	 *
	 * @return the start time for this recording, or <tt>null</tt> if not available
	 */
	public IQuantity getStartTime();

	/**
	 * Returns the duration the recording was created with.
	 *
	 * @return the duration of the recording in ms (0 means continuous), or -1 if unavailable
	 */
	public IQuantity getDuration();

	/**
	 * Returns the recording was created as continuous or not.
	 *
	 * @return <tt>true</tt> if the recording was created continuous, or <tt>false</tt> if not
	 */
	public boolean isContinuous();

	/**
	 * Returns whether the recording is stored to disk.
	 *
	 * @return <tt>true</tt> if the recording is stored to disk, <tt>false</tt> otherwise
	 */
	public boolean getToDisk();

	/**
	 * Returns the recordings maximum size.
	 *
	 * @return the maximum size of the recording
	 */
	public IQuantity getMaxSize();

	/**
	 * Returns the recordings maximum event age.
	 *
	 * @return the maximum event age of the recording
	 */
	public IQuantity getMaxAge();
}
