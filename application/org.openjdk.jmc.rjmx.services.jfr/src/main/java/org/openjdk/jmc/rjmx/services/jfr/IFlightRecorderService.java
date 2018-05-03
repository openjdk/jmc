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

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.configuration.IRecorderConfigurationService;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.rjmx.IConnectionHandle;

/**
 * This is the interface for the JDK Flight Recorder controller.
 * <p>
 * Implementation, if available, can be retrieved from an
 * {@link IConnectionHandle#getServiceOrNull(Class)}
 */
public interface IFlightRecorderService extends IRecorderConfigurationService {
	/**
	 * Returns the available JDK Flight Recorder recordings. The immutable descriptor represents
	 * the state at the time the method was called. To get an updated representation the method must
	 * be called again.
	 *
	 * @return the available Flight Recording recordings.
	 * @throws FlightRecorderException
	 */
	List<IRecordingDescriptor> getAvailableRecordings() throws FlightRecorderException;

	/**
	 * Get a recording that best represents all the previously recorded data.
	 *
	 * @return a recording descriptor.
	 * @throws FlightRecorderException
	 */
	IRecordingDescriptor getSnapshotRecording() throws FlightRecorderException;

	/**
	 * Returns the updated recording descriptor for the specified recording.
	 *
	 * @param descriptor
	 *            the recording for which to get the recording descriptor.
	 * @return the recording descriptor for the specified recording id.
	 */
	IRecordingDescriptor getUpdatedRecordingDescription(IRecordingDescriptor descriptor) throws FlightRecorderException;

	/**
	 * Starts a new JDK Flight Recorder recording.
	 *
	 * @param recordingOptions
	 *            the recording options. Use {@link RecordingOptionsBuilder} to create.
	 * @param eventOptions
	 *            the event options.
	 * @return the {@link IRecordingDescriptor} representing the started recording.
	 * @throws FlightRecorderException
	 *             if there was a problem starting the recording.
	 */
	IRecordingDescriptor start(IConstrainedMap<String> recordingOptions, IConstrainedMap<EventOptionID> eventOptions)
			throws FlightRecorderException;

	/**
	 * Stops the recording represented by the {@link IRecordingDescriptor}.
	 *
	 * @param descriptor
	 *            the recording to stop.
	 * @throws FlightRecorderException
	 *             if there was a problem stopping the recording.
	 */
	void stop(IRecordingDescriptor descriptor) throws FlightRecorderException;

	/**
	 * Closes the recording represented by the {@link IRecordingDescriptor}. A closed recording will
	 * no longer be listed among the available recordings. It's corresponding MBean will be removed.
	 *
	 * @param descriptor
	 *            the recording to close.
	 * @throws FlightRecorderException
	 *             if there was a problem closing the recording.
	 */
	void close(IRecordingDescriptor descriptor) throws FlightRecorderException;

	/**
	 * Returns the descriptors for the available recording options.
	 *
	 * @return the available recording option descriptors.
	 * @throws FlightRecorderException
	 *             if there was a problem retrieving the recording options.
	 */
	Map<String, IOptionDescriptor<?>> getAvailableRecordingOptions() throws FlightRecorderException;

	/**
	 * Returns the recording options for the specified recording. Note that options can be changed
	 * over time. The {@link IConstrainedMap} is immutable - call again to get the updated settings
	 * for a particular recording.
	 *
	 * @param recording
	 *            the recording for which to retrieve the recording options.
	 * @return the {@link IConstrainedMap} for the specified recording.
	 * @throws FlightRecorderException
	 *             if there was a problem retrieving the options.
	 */
	IConstrainedMap<String> getRecordingOptions(IRecordingDescriptor recording) throws FlightRecorderException;

	/**
	 * @return the metadata for all known event types.
	 * @throws FlightRecorderException
	 *             if there was a problem retrieving the metadata.
	 */
	Collection<? extends IEventTypeInfo> getAvailableEventTypes() throws FlightRecorderException;

	/**
	 * @return a mapping from event type id to info
	 * @throws FlightRecorderException
	 *             if there was a problem retrieving the metadata.
	 */
	Map<? extends IEventTypeID, ? extends IEventTypeInfo> getEventTypeInfoMapByID() throws FlightRecorderException;

	/**
	 * Returns the currently active settings for all event types, if the JDK Flight Recorder
	 * version supports this notion. Otherwise, empty settings will be returned.
	 *
	 * @return the current settings for the event types.
	 * @throws FlightRecorderException
	 *             if there was a problem retrieving the settings.
	 */
	IConstrainedMap<EventOptionID> getCurrentEventTypeSettings() throws FlightRecorderException;

	/**
	 * Returns the event settings for the specified recording.
	 *
	 * @param recording
	 *            the recording for which to return the settings.
	 * @return the event settings for the specified recording.
	 * @throws FlightRecorderException
	 */
	IConstrainedMap<EventOptionID> getEventSettings(IRecordingDescriptor recording) throws FlightRecorderException;

	/**
	 * Opens a stream from the specified recording. Will read all available data. The content of the
	 * stream is gzipped. You would normally want to wrap it in a {@link GZIPInputStream}.
	 *
	 * @param descriptor
	 *            the recording from which to retrieve the data.
	 * @param removeOnClose
	 *            whether the recording should be removed when the stream is closed or not
	 * @return an input stream from which to read the recording data.
	 * @throws FlightRecorderException
	 *             if there was a problem reading the recording data.
	 */
	InputStream openStream(IRecordingDescriptor descriptor, boolean removeOnClose) throws FlightRecorderException;

	/**
	 * Opens a stream from the specified recording between the specified times. The content of the
	 * stream is gzipped. You would normally want to wrap it in a {@link GZIPInputStream}.
	 * <p>
	 * Note that the dates should be in server side time. Special care should be taken to make sure
	 * that server side timestamps are used.
	 *
	 * @param descriptor
	 *            the recording from which to retrieve the data.
	 * @param startTime
	 *            the start time.
	 * @param endTime
	 *            the end time.
	 * @param removeOnClose
	 *            whether the recording should be removed when the stream is closed or not
	 * @return an input stream from which to read the recording data.
	 * @throws FlightRecorderException
	 *             if there was a problem reading the recording data.
	 */
	InputStream openStream(
		IRecordingDescriptor descriptor, IQuantity startTime, IQuantity endTime, boolean removeOnClose)
			throws FlightRecorderException;

	/**
	 * Opens a stream from the specified recording for the past "time" milliseconds. The content of
	 * the stream is gzipped. You would normally want to wrap it in a {@link GZIPInputStream}.
	 *
	 * @param descriptor
	 *            the recording from which to retrieve the data.
	 * @param lastPartDuration
	 *            the duration of data to retrieve.
	 * @param removeOnClose
	 *            whether the recording should be removed when the stream is closed or not
	 * @return an input stream from which to read the recording data.
	 * @throws FlightRecorderException
	 *             if there was a problem reading the recording data.
	 */
	InputStream openStream(IRecordingDescriptor descriptor, IQuantity lastPartDuration, boolean removeOnClose)
			throws FlightRecorderException;

	/**
	 * @return the server templates for event settings found on the server.
	 */
	List<String> getServerTemplates() throws FlightRecorderException;

	/**
	 * Updates the event options for the specified descriptor.
	 *
	 * @param descriptor
	 *            the recording to update the event options for.
	 * @param options
	 *            the new, overriding, event options. If null, the current options will be used.
	 */
	void updateEventOptions(IRecordingDescriptor descriptor, IConstrainedMap<EventOptionID> options)
			throws FlightRecorderException;

	/**
	 * Updates the recording options for the specified recording.
	 *
	 * @param descriptor
	 *            the recording to update the event settings for.
	 * @param options
	 *            the new options to set.
	 */
	void updateRecordingOptions(IRecordingDescriptor descriptor, IConstrainedMap<String> options)
			throws FlightRecorderException;

	/**
	 * @return true if the flight recorder is enabled, false otherwise.
	 */
	boolean isEnabled();

	/**
	 * Enables the recorder
	 *
	 * @throws FlightRecorderException
	 *             if there was a problem to enable the recorder
	 */
	void enable() throws FlightRecorderException;
}
