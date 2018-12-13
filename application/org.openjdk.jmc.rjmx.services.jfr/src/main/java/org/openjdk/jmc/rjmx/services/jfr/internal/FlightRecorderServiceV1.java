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

import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_MS;
import static org.openjdk.jmc.common.unit.UnitLookup.toDate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.SimpleConstrainedMap;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.flightrecorder.configuration.ConfigurationToolkit;
import org.openjdk.jmc.flightrecorder.configuration.OptionInfo;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.configuration.internal.DefaultValueMap;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV1;
import org.openjdk.jmc.flightrecorder.configuration.internal.IMapper;
import org.openjdk.jmc.flightrecorder.configuration.internal.KnownRecordingOptions;
import org.openjdk.jmc.flightrecorder.configuration.internal.ValidationToolkit;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.JVMSupportToolkit;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.ICommercialFeaturesService;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderToolkit;
import org.openjdk.jmc.rjmx.services.jfr.IEventTypeInfo;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;

public class FlightRecorderServiceV1 implements IFlightRecorderService {
	final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.rjmx.services.jfr"); //$NON-NLS-1$
	private final static IMapper<EventOptionID, IOptionDescriptor<?>> DISALLOW_MAPPER = new IMapper<EventOptionID, IOptionDescriptor<?>>() {
		@Override
		public IOptionDescriptor<?> get(EventOptionID key) {
			return OptionInfo.DISALLOWED_OPTION;
		}
	};

	// FIXME: This is a very long time span used to find the longest recording. We could remove the need for this constant by changing the code that uses it.
	private static final ITypedQuantity<LinearUnit> MAX_REQUIRED_RECORDING_DURATION = UnitLookup.YEAR.quantity(10);

	private final IFlightRecorderCommunicationHelper helper;
	private long eventTypeMetaNextUpdate;
	private List<EventTypeMetadataV1> eventTypeMetas;
	private Map<Integer, EventTypeMetadataV1> eventTypeMetaByInt;
	private Map<EventTypeIDV1, EventTypeMetadataV1> eventTypeInfoById;
	// Optimization to do less JMX invocations. If, against all odds, it gets disabled,
	// after having been enabled, we get an exception, and will handle things there.
	private boolean wasEnabled;
	private final ICommercialFeaturesService cfs;
	private final IMBeanHelperService mbhs;
	private final String serverId;

	@Override
	public String getVersion() {
		return "1.0"; //$NON-NLS-1$
	}

	private boolean isDynamicFlightRecorderSupported(IConnectionHandle handle) {
		return ConnectionToolkit.isHotSpot(handle)
				&& ConnectionToolkit.isJavaVersionAboveOrEqual(handle, JavaVersionSupport.DYNAMIC_JFR_SUPPORTED);
	}

	private boolean isFlightRecorderDisabled(IConnectionHandle handle) {
		if (cfs != null) {
			return !cfs.isCommercialFeaturesEnabled() || JVMSupportToolkit.isFlightRecorderDisabled(handle, false);
		} else {
			return true;
		}
	}

	public FlightRecorderServiceV1(IConnectionHandle handle) throws ConnectionException, ServiceNotAvailableException {
		cfs = handle.getServiceOrThrow(ICommercialFeaturesService.class);
		if (!isDynamicFlightRecorderSupported(handle) && isFlightRecorderDisabled(handle)) {
			throw new ServiceNotAvailableException(""); //$NON-NLS-1$
		}
		if (JVMSupportToolkit.isFlightRecorderDisabled(handle, true)) {
			throw new ServiceNotAvailableException(""); //$NON-NLS-1$
		}
		helper = new FlightRecorderCommunicationHelperV1(handle.getServiceOrThrow(MBeanServerConnection.class));
		mbhs = handle.getServiceOrThrow(IMBeanHelperService.class);
		serverId = handle.getServerDescriptor().getGUID();
	}

	@Override
	public void stop(IRecordingDescriptor descriptor) throws FlightRecorderException {
		stop(descriptor.getObjectName());
	}

	private void stop(ObjectName objectName) throws FlightRecorderException {
		try {
			helper.invokeOperation("stop", objectName); //$NON-NLS-1$
		} catch (Exception e) {
			throw new FlightRecorderException("Could not stop the recording!", e); //$NON-NLS-1$
		}
	}

	@Override
	public void close(IRecordingDescriptor descriptor) throws FlightRecorderException {
		helper.closeRecording(descriptor);
	}

	@Override
	public IRecordingDescriptor start(
		IConstrainedMap<String> recordingOptions, IConstrainedMap<EventOptionID> eventOptions)
			throws FlightRecorderException {
		try {
			validateOptions(recordingOptions);
			String name = RecordingOptionsToolkitV1.getName(recordingOptions);
			ObjectName recording = (ObjectName) helper.invokeOperation("createRecording", name); //$NON-NLS-1$
			helper.invokeOperation("setRecordingOptions", recording, //$NON-NLS-1$
					RecordingOptionsToolkitV1.getRecordingOptions(recordingOptions));
			if (eventOptions != null) {
				updateEventOptions(recording, eventOptions);
			}
			helper.invokeOperation("start", recording); //$NON-NLS-1$
			for (IRecordingDescriptor descriptor : getAvailableRecordings()) {
				if (recording.equals(descriptor.getObjectName())) {
					return descriptor;
				}
			}
		} catch (Exception e) {
			throw new FlightRecorderException("Could not start the recording!", e); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public IDescribedMap<String> getDefaultRecordingOptions() {
		return KnownRecordingOptions.OPTION_DEFAULTS_V1;
	}

	@Override
	public IConstrainedMap<String> getRecordingOptions(IRecordingDescriptor recording) throws FlightRecorderException {
		try {
			return getRecordingOptions(recording.getObjectName());
		} catch (Exception e) {
			throw new FlightRecorderException("Could not retrieve recording options.", e); //$NON-NLS-1$
		}
	}

	private IConstrainedMap<String> getRecordingOptions(ObjectName name) throws FlightRecorderException, IOException {
		return RecordingOptionsToolkitV1
				.toRecordingOptions((CompositeData) helper.invokeOperation("getRecordingOptions", name)); //$NON-NLS-1$
	}

	@Override
	public IConstrainedMap<EventOptionID> getEventSettings(IRecordingDescriptor recording)
			throws FlightRecorderException {
		try {
			Map<Integer, EventTypeMetadataV1> map = getEventTypeByIntMap();
			@SuppressWarnings("unchecked")
			List<CompositeData> compositeList = (List<CompositeData>) helper.invokeOperation("getEventSettings", //$NON-NLS-1$
					recording.getObjectName());
			return toEventOptionMap(map, compositeList);
		} catch (Exception e) {
			FlightRecorderException flr = new FlightRecorderException(
					"Could not retrieve recording options for recording " + recording.getName() + '.'); //$NON-NLS-1$
			flr.initCause(e);
			throw flr;
		}
	}

	// FIXME: This should _really_ be retrieved from the server, but the server API does not allow that at the moment.
	@Override
	public Map<String, IOptionDescriptor<?>> getAvailableRecordingOptions() throws FlightRecorderException {
		return RecordingOptionsToolkitV1.getAvailableRecordingOptions();
	}

	@Override
	public String toString() {
		return helper.toString();
	}

	@Override
	public InputStream openStream(IRecordingDescriptor descriptor, boolean removeOnClose)
			throws FlightRecorderException {
		IRecordingDescriptor streamDescriptor = descriptor;
		boolean clone = isStillRunning(descriptor);
		if (clone) {
			streamDescriptor = clone(descriptor);
		}
		return new JfrRecordingInputStreamV1(helper, streamDescriptor, clone | removeOnClose);
	}

	@Override
	public InputStream openStream(
		IRecordingDescriptor descriptor, IQuantity startTime, IQuantity endTime, boolean removeOnClose)
			throws FlightRecorderException {
		IRecordingDescriptor streamDescriptor = descriptor;
		boolean clone = isStillRunning(descriptor);
		if (clone) {
			streamDescriptor = clone(descriptor);
		}
		return new JfrRecordingInputStreamV1(helper, streamDescriptor, toDate(startTime), toDate(endTime),
				clone | removeOnClose);
	}

	@Override
	public Collection<EventTypeMetadataV1> getAvailableEventTypes() throws FlightRecorderException {
		return updateEventTypeMetadataMaps(true);
	}

	@Override
	public List<IRecordingDescriptor> getAvailableRecordings() throws FlightRecorderException {
		@SuppressWarnings("unchecked")
		List<CompositeData> attribute = (List<CompositeData>) helper.getAttribute("Recordings"); //$NON-NLS-1$
		List<IRecordingDescriptor> recordings = new ArrayList<>();
		for (CompositeData data : attribute) {
			recordings.add(RecordingDescriptorToolkitV1.createRecordingDescriptor(serverId, data));
		}
		return Collections.unmodifiableList(recordings);
	}

	@Override
	public IRecordingDescriptor getSnapshotRecording() throws FlightRecorderException {
		return FlightRecorderToolkit.getDescriptorByTimerange(getAvailableRecordings(),
				MAX_REQUIRED_RECORDING_DURATION);
	}

	@Override
	public IConstrainedMap<EventOptionID> getCurrentEventTypeSettings() throws FlightRecorderException {
		Map<Integer, EventTypeMetadataV1> byIntMap = getEventTypeByIntMap();
		@SuppressWarnings("unchecked")
		List<CompositeData> compositeList = (List<CompositeData>) helper.getAttribute("EventSettings"); //$NON-NLS-1$
		return toEventOptionMap(byIntMap, compositeList);
	}

	@Override
	public IDescribedMap<EventOptionID> getDefaultEventOptions() {
		try {
			updateEventTypeMetadataMaps(true);
			// FIXME: Calculate this in the updateEventTypeMetadataMaps() method as for V2?
			Map<EventOptionID, IOptionDescriptor<?>> optionInfoById = new HashMap<>();
			for (Entry<EventTypeIDV1, EventTypeMetadataV1> typeEntry : eventTypeInfoById.entrySet()) {
				for (Entry<String, IOptionDescriptor<?>> optionEntry : typeEntry.getValue().getOptionDescriptors()
						.entrySet()) {
					EventOptionID optionID = new EventOptionID(typeEntry.getKey(), optionEntry.getKey());
					optionInfoById.put(optionID, optionEntry.getValue());
				}
			}
			return new DefaultValueMap<>(optionInfoById, DISALLOW_MAPPER);
		} catch (FlightRecorderException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Couldn't get default event options", e); //$NON-NLS-1$
			return ConfigurationToolkit.getEventOptions(SchemaVersion.V1);
		}
	}

	@Override
	public IRecordingDescriptor getUpdatedRecordingDescription(IRecordingDescriptor descriptor)
			throws FlightRecorderException {
		return getUpdatedRecordingDescriptor(descriptor.getObjectName());
	}

	@Override
	public List<String> getServerTemplates() throws FlightRecorderException {
		@SuppressWarnings("unchecked")
		List<CompositeData> compositeData = (List<CompositeData>) helper.getAttribute("AvailablePresets"); //$NON-NLS-1$
		return RecordingTemplateToolkit.getServerTemplatesV1(compositeData);
	}

	@Override
	public void updateEventOptions(IRecordingDescriptor descriptor, IConstrainedMap<EventOptionID> options)
			throws FlightRecorderException {
		try {
			updateEventOptions(descriptor.getObjectName(), options);
		} catch (Exception e) {
			throw new FlightRecorderException("Failed updating the event options for " + descriptor.getName(), e); //$NON-NLS-1$
		}
	}

	private IRecordingDescriptor getUpdatedRecordingDescriptor(ObjectName name) throws FlightRecorderException {
		// getRecordingOptions doesn't quite contain all we need, so retrieve
		// everything and filter out what we need...
		return RecordingDescriptorToolkitV1.getRecordingByDescriptor(name, getAvailableRecordings());
	}

	private void validateOptions(IConstrainedMap<String> recordingOptions) throws FlightRecorderException {
		try {
			ValidationToolkit.validate(recordingOptions);
		} catch (Exception e) {
			throw new FlightRecorderException("Could not validate options!\n" + e.getMessage()); //$NON-NLS-1$
		}
	}

	@Override
	public Map<? extends IEventTypeID, ? extends IEventTypeInfo> getEventTypeInfoMapByID()
			throws FlightRecorderException {
		updateEventTypeMetadataMaps(false);
		return eventTypeInfoById;
	}

	private Map<Integer, EventTypeMetadataV1> getEventTypeByIntMap() throws FlightRecorderException {
		updateEventTypeMetadataMaps(false);
		return eventTypeMetaByInt;
	}

	private Collection<EventTypeMetadataV1> updateEventTypeMetadataMaps(boolean force) throws FlightRecorderException {
		long timestamp = System.currentTimeMillis();
		if (force || (timestamp > eventTypeMetaNextUpdate)) {

			@SuppressWarnings("unchecked")
			List<CompositeData> compositeList = (List<CompositeData>) helper.getAttribute("EventDescriptors"); //$NON-NLS-1$

			List<EventTypeMetadataV1> metadataList = new ArrayList<>(compositeList.size());
			Map<Integer, EventTypeMetadataV1> byInt = new HashMap<>();
			Map<EventTypeIDV1, EventTypeMetadataV1> byId = new HashMap<>();
			for (CompositeData data : compositeList) {
				try {
					EventTypeMetadataV1 element = toEventMetaDataV1(data);
					metadataList.add(element);
					byInt.put(element.getId(), element);
					byId.put(element.getEventTypeID(), element);
				} catch (URISyntaxException e) {
					RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
							"Could not create event metadata for composite data!", e); //$NON-NLS-1$
				}
			}

			// Do not update more often than every minute.
			// FIXME: Use JMX notifications instead?
			eventTypeMetaNextUpdate = timestamp + 60 * 1000;
			eventTypeMetas = Collections.unmodifiableList(metadataList);
			eventTypeMetaByInt = Collections.unmodifiableMap(byInt);
			eventTypeInfoById = Collections.unmodifiableMap(byId);
		}
		return eventTypeMetas;
	}

	private boolean isStillRunning(IRecordingDescriptor descriptor) throws FlightRecorderException {
		IRecordingDescriptor updatedDescriptor = getUpdatedRecordingDescription(descriptor);
		return updatedDescriptor != null
				&& IRecordingDescriptor.RecordingState.RUNNING.equals(updatedDescriptor.getState());
	}

	// creates a stopped clone
	private IRecordingDescriptor clone(IRecordingDescriptor descriptor) throws FlightRecorderException {
		try {
			ObjectName name = (ObjectName) helper.invokeOperation("cloneRecording", //$NON-NLS-1$
					descriptor.getObjectName(), "Clone of " + descriptor.getName(), Boolean.TRUE); //$NON-NLS-1$
			return getUpdatedRecordingDescriptor(name);
		} catch (IOException e) {
			throw new FlightRecorderException("Could not clone the " + descriptor.getName() + " recording ", e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void updateEventOptions(ObjectName recording, IConstrainedMap<EventOptionID> eventOptions)
			throws OpenDataException, IOException, FlightRecorderException {
		helper.invokeOperation("updateEventSettings", recording, //$NON-NLS-1$
				EventOptionsToolkitV1.encodeAllEventSettings(getAvailableEventTypes(), eventOptions));
	}

	@Override
	public void updateRecordingOptions(IRecordingDescriptor descriptor, IConstrainedMap<String> options)
			throws FlightRecorderException {
		validateOptions(options);
		try {
			helper.invokeOperation("setRecordingOptions", descriptor.getObjectName(), //$NON-NLS-1$
					RecordingOptionsToolkitV1.getRecordingOptions(options));
		} catch (Exception e) {
			throw new FlightRecorderException("Failed updating the recording options for " + descriptor.getName(), e); //$NON-NLS-1$
		}
	}

	@Override
	public InputStream openStream(IRecordingDescriptor descriptor, IQuantity lastPartDuration, boolean removeOnClose)
			throws FlightRecorderException {
		/*
		 * FIXME: JMC-4270 - Server time approximation is not reliable. Can perhaps get a better
		 * time by cloning the recording and getting the end time from there like in the commented
		 * out code below.
		 */
//		IRecordingDescriptor streamDescriptor = descriptor;
//		boolean clone = isStillRunning(descriptor);
//		if (clone) {
//			streamDescriptor = clone(descriptor);
//		}
//		IQuantity endTime = streamDescriptor.getDataEndTime();
//		IQuantity startTime = endTime.subtract(lastPartDuration);
//		return new JfrRecordingInputStreamV1(helper, streamDescriptor, toDate(startTime), toDate(endTime), clone | removeOnClose);

		long serverTime = mbhs.getApproximateServerTime(System.currentTimeMillis());
		IQuantity endDate = EPOCH_MS.quantity(serverTime);
		IQuantity startDate = endDate.subtract(lastPartDuration);
		return openStream(descriptor, startDate, endDate, removeOnClose);
	}

	@Override
	public boolean isEnabled() {
		if (!wasEnabled) {
			boolean isEnabled = cfs.isCommercialFeaturesEnabled();
			if (isEnabled) {
				wasEnabled = true;
			}
			return isEnabled;
		} else {
			return wasEnabled;
		}
	}

	@Override
	public void enable() throws FlightRecorderException {
		try {
			cfs.enableCommercialFeatures();
		} catch (Exception e) {
			throw new FlightRecorderException("Failed to enable commercial features", e); //$NON-NLS-1$
		}
	}

	/**
	 * @param typeByInt
	 *            a map from {@link Integer} to {@link EventTypeMetadataV1}.
	 * @param compositeDatas
	 *            a list of composite data representing the actual settings.
	 * @return the event type settings.
	 */
	@SuppressWarnings("nls")
	private static IConstrainedMap<EventOptionID> toEventOptionMap(
		Map<Integer, EventTypeMetadataV1> typeByInt, List<CompositeData> compositeDatas) {
		SimpleConstrainedMap<EventOptionID> options = new SimpleConstrainedMap<>();
		for (CompositeData data : compositeDatas) {
			Integer intID = (Integer) data.get("id");
			EventTypeMetadataV1 metadata = typeByInt.get(intID);
			EventOptionsToolkitV1.addOptionsToV1(options, metadata, data);
		}
		return options;
	}

	@SuppressWarnings("nls")
	public static EventTypeMetadataV1 toEventMetaDataV1(CompositeData data) throws URISyntaxException {
		String uri = (String) data.get("uri");
		Integer id = (Integer) data.get("id");
		String path = (String) data.get("path");
		String label = (String) data.get("name");
		String description = (String) data.get("description");
		// For now, verify URI correctness, as before.
		new URI(uri);
		// NOTE: Assuming that uri ends in path.
		assert uri.endsWith(path);
		EventTypeIDV1 eventTypeID = new EventTypeIDV1(uri, uri.length() - path.length());
		return new EventTypeMetadataV1(id, eventTypeID, label, description,
				EventOptionsToolkitV1.getConfigurableOptions(data));
	}
}
