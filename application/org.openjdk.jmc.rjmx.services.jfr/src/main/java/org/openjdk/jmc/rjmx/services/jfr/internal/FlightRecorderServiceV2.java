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
import static org.openjdk.jmc.rjmx.services.jfr.internal.RecordingOptionsToolkitV2.toTabularData;

import java.io.IOException;
import java.io.InputStream;
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
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import org.eclipse.osgi.util.NLS;
import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.flightrecorder.configuration.ConfigurationToolkit;
import org.openjdk.jmc.flightrecorder.configuration.OptionInfo;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.configuration.internal.DefaultValueMap;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV2;
import org.openjdk.jmc.flightrecorder.configuration.internal.KnownEventOptions;
import org.openjdk.jmc.flightrecorder.configuration.internal.KnownRecordingOptions;
import org.openjdk.jmc.flightrecorder.configuration.internal.ValidationToolkit;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.JVMSupportToolkit;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.ICommercialFeaturesService;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;

public class FlightRecorderServiceV2 implements IFlightRecorderService {
	final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.rjmx.services.jfr"); //$NON-NLS-1$
	final private FlightRecorderCommunicationHelperV2 helper;
	private long eventTypeMetaNextUpdate;
	private List<EventTypeMetadataV2> eventTypeMetas;
	private Map<EventTypeIDV2, EventTypeMetadataV2> eventTypeInfoById;
	private Map<org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID, OptionInfo<?>> optionInfoById;
	private final ICommercialFeaturesService cfs;
	private final IMBeanHelperService mbhs;
	private final String serverId;
	private final IConnectionHandle connection;

	@Override
	public String getVersion() {
		return "2.0"; //$NON-NLS-1$
	}

	private boolean isDynamicFlightRecorderSupported(IConnectionHandle handle) {
		return ConnectionToolkit.isHotSpot(handle)
				&& ConnectionToolkit.isJavaVersionAboveOrEqual(handle, JavaVersionSupport.DYNAMIC_JFR_SUPPORTED);
	}

	private boolean isFlightRecorderCommercial() {
		return ConnectionToolkit.isHotSpot(connection)
				&& !ConnectionToolkit.isJavaVersionAboveOrEqual(connection, JavaVersionSupport.JFR_NOT_COMMERCIAL);
	}

	private boolean isFlightRecorderDisabled(IConnectionHandle handle) {
		if (cfs != null && isFlightRecorderCommercial()) {
			return !cfs.isCommercialFeaturesEnabled() || JVMSupportToolkit.isFlightRecorderDisabled(handle, false);
		} else {
			return JVMSupportToolkit.isFlightRecorderDisabled(handle, false);
		}
	}

	public static boolean isAvailable(IConnectionHandle handle) {
		return FlightRecorderCommunicationHelperV2.isAvailable(handle);
	}

	public FlightRecorderServiceV2(IConnectionHandle handle) throws ConnectionException, ServiceNotAvailableException {
		cfs = handle.getServiceOrThrow(ICommercialFeaturesService.class);
		if (!isDynamicFlightRecorderSupported(handle) && isFlightRecorderDisabled(handle)) {
			throw new ServiceNotAvailableException(""); //$NON-NLS-1$
		}
		if (JVMSupportToolkit.isFlightRecorderDisabled(handle, true)) {
			throw new ServiceNotAvailableException(""); //$NON-NLS-1$
		}
		connection = handle;
		helper = new FlightRecorderCommunicationHelperV2(handle.getServiceOrThrow(MBeanServerConnection.class));
		mbhs = handle.getServiceOrThrow(IMBeanHelperService.class);
		serverId = handle.getServerDescriptor().getGUID();
	}

	@Override
	public void stop(IRecordingDescriptor descriptor) throws FlightRecorderException {
		stop(descriptor.getId());
	}

	private void stop(Long id) throws FlightRecorderException {
		try {
			helper.invokeOperation("stopRecording", id); //$NON-NLS-1$
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
		Long id;
		try {
			validateOptions(recordingOptions);
			id = (Long) helper.invokeOperation("newRecording"); //$NON-NLS-1$
		} catch (Exception e) {
			throw new FlightRecorderException("Could not create a recording!", e); //$NON-NLS-1$
		}
		try {
			updateRecordingOptions(id, recordingOptions);
			if (eventOptions != null) {
				updateEventOptions(id, eventOptions);
			}
			helper.invokeOperation("startRecording", id); //$NON-NLS-1$
			return getUpdatedRecordingDescriptor(id);
		} catch (Exception e) {
			try {
				helper.invokeOperation("closeRecording", id); //$NON-NLS-1$
			} catch (IOException ioe) {
				e.addSuppressed(ioe);
				throw new FlightRecorderException(
						"Could not start the recording! Could not remove the unstarted recording.", e); //$NON-NLS-1$
			}
			throw new FlightRecorderException("Could not start the recording! Removed the unstarted recording.", e); //$NON-NLS-1$
		}
	}

	private IMutableConstrainedMap<String> getEmptyRecordingOptions() {
		return ConfigurationToolkit.getRecordingOptions(JavaVersionSupport.JDK_9).emptyWithSameConstraints();
	}

	@Override
	public IDescribedMap<String> getDefaultRecordingOptions() {
		return KnownRecordingOptions.OPTION_DEFAULTS_V2;
	}

	@Override
	public IConstrainedMap<String> getRecordingOptions(IRecordingDescriptor recording) throws FlightRecorderException {
		try {
			return getRecordingOptions(recording.getId());
		} catch (Exception e) {
			throw new FlightRecorderException("Could not retrieve recording options.", e); //$NON-NLS-1$
		}
	}

	private IConstrainedMap<String> getRecordingOptions(Long id) throws FlightRecorderException, IOException {
		IMutableConstrainedMap<String> options = getEmptyRecordingOptions();
		for (Object o : ((TabularData) helper.invokeOperation("getRecordingOptions", id)).values()) { //$NON-NLS-1$
			CompositeData row = (CompositeData) o;
			String key = (String) row.get("key"); //$NON-NLS-1$
			String value = (String) row.get("value"); //$NON-NLS-1$
			IConstraint<?> constraint = RecordingOptionsToolkitV2.getRecordingOptionConstraint(key);
			// FIXME: Use generic string constraint if nothing better was found.
			if (constraint != null) {
				try {
					options.putPersistedString(key, constraint, value);
				} catch (QuantityConversionException e) {
					// Shouldn't happen, but I want to know if it does.
					LOGGER.log(Level.FINE, "Recording option conversion problem", e); //$NON-NLS-1$
				}
			}
		}
		return options;
	}

	@Override
	public IConstrainedMap<EventOptionID> getEventSettings(IRecordingDescriptor recording)
			throws FlightRecorderException {
		try {
			TabularData tabularData = (TabularData) helper.invokeOperation("getRecordingSettings", //$NON-NLS-1$
					recording.getId());
			IMutableConstrainedMap<EventOptionID> settings = getDefaultEventOptions().emptyWithSameConstraints();
			for (Object row : tabularData.values()) {
				CompositeData data = (CompositeData) row;
				String key = (String) data.get("key"); //$NON-NLS-1$
				String value = (String) data.get("value"); //$NON-NLS-1$
				int hashPos = key.lastIndexOf('#');
				if (hashPos > 0) {
					// FIXME: Deal with numerically specified event type (instance).
					EventTypeIDV2 type = new EventTypeIDV2(key.substring(0, hashPos));
					EventOptionID option = new EventOptionID(type, key.substring(hashPos + 1));
					// FIXME: Try/catch and ignore?
					settings.putPersistedString(option, value);
				}
			}
			return settings;
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
		return RecordingOptionsToolkitV2.getAvailableRecordingOptions();
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
		return new JfrRecordingInputStreamV2(helper, streamDescriptor, clone | removeOnClose);
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
		return new JfrRecordingInputStreamV2(helper, streamDescriptor, toDate(startTime), toDate(endTime),
				clone | removeOnClose);
	}

	@Override
	public Collection<EventTypeMetadataV2> getAvailableEventTypes() throws FlightRecorderException {
		return updateEventTypeMetadataMaps(true);
	}

	@Override
	public List<IRecordingDescriptor> getAvailableRecordings() throws FlightRecorderException {
		CompositeData[] attribute = (CompositeData[]) helper.getAttribute("Recordings"); //$NON-NLS-1$
		List<IRecordingDescriptor> recordings = new ArrayList<>();
		for (CompositeData data : attribute) {
			recordings.add(new RecordingDescriptorV2(serverId, data));
		}
		return Collections.unmodifiableList(recordings);
	}

	@Override
	public IRecordingDescriptor getSnapshotRecording() throws FlightRecorderException {
		try {
			Long id = (Long) helper.invokeOperation("takeSnapshot", new Object[0]); //$NON-NLS-1$
			return getUpdatedRecordingDescriptor(id);
		} catch (Exception e) {
			throw new FlightRecorderException("Could not take a snapshot of the flight recorder", e); //$NON-NLS-1$
		}
	}

	@Override
	public IDescribedMap<EventOptionID> getCurrentEventTypeSettings() throws FlightRecorderException {
		updateEventTypeMetadataMaps(true);
		return new DefaultValueMap<>(optionInfoById, new ExcludingEventOptionMapper(eventTypeInfoById.keySet(),
				EventTypeIDV2.class, KnownEventOptions.EVENT_OPTIONS_BY_KEY_V2));
	}

	@Override
	public IDescribedMap<EventOptionID> getDefaultEventOptions() {
		try {
			return getCurrentEventTypeSettings();
		} catch (FlightRecorderException e) {
			LOGGER.log(Level.WARNING, "Couldn't get event settings", e); //$NON-NLS-1$
			return ConfigurationToolkit.getEventOptions(SchemaVersion.V2);
		}
	}

	@Override
	public IRecordingDescriptor getUpdatedRecordingDescription(IRecordingDescriptor descriptor)
			throws FlightRecorderException {
		return getUpdatedRecordingDescriptor(descriptor.getId());
	}

	@Override
	public List<String> getServerTemplates() throws FlightRecorderException {
		CompositeData[] compositeData = (CompositeData[]) helper.getAttribute("Configurations"); //$NON-NLS-1$
		return RecordingTemplateToolkit.getServerTemplatesV2(compositeData);
	}

	@Override
	public void updateEventOptions(IRecordingDescriptor descriptor, IConstrainedMap<EventOptionID> options)
			throws FlightRecorderException {
		try {
			updateEventOptions(descriptor.getId(), options);
		} catch (Exception e) {
			throw new FlightRecorderException("Failed updating the event options for " + descriptor.getName(), e); //$NON-NLS-1$
		}
	}

	private IRecordingDescriptor getUpdatedRecordingDescriptor(Long id) throws FlightRecorderException {
		// getRecordingOptions doesn't quite contain all we need, so retrieve
		// everything and filter out what we need...
		for (IRecordingDescriptor recording : getAvailableRecordings()) {
			if (id.equals(recording.getId())) {
				return recording;
			}
		}
		return null;
	}

	private void validateOptions(IConstrainedMap<String> recordingOptions) throws FlightRecorderException {
		try {
			ValidationToolkit.validate(recordingOptions);
		} catch (Exception e) {
			throw new FlightRecorderException("Could not validate options!\n" + e.getMessage()); //$NON-NLS-1$
		}
	}

	@Override
	public Map<EventTypeIDV2, EventTypeMetadataV2> getEventTypeInfoMapByID() throws FlightRecorderException {
		updateEventTypeMetadataMaps(false);
		return eventTypeInfoById;
	}

	private Collection<EventTypeMetadataV2> updateEventTypeMetadataMaps(boolean force) throws FlightRecorderException {
		long timestamp = System.currentTimeMillis();
		if (force || (timestamp > eventTypeMetaNextUpdate)) {

			CompositeData[] compositeList = (CompositeData[]) helper.getAttribute("EventTypes"); //$NON-NLS-1$

			List<EventTypeMetadataV2> metadataList = new ArrayList<>(compositeList.length);
			Map<EventTypeIDV2, EventTypeMetadataV2> byId = new HashMap<>();
			Map<EventOptionID, OptionInfo<?>> optionById = new HashMap<>();
			for (CompositeData data : compositeList) {
				EventTypeMetadataV2 typeInfo = EventTypeMetadataV2.from(data);
				metadataList.add(typeInfo);
				EventTypeIDV2 typeID = typeInfo.getEventTypeID();
				byId.put(typeID, typeInfo);
				for (Entry<String, OptionInfo<?>> entry : typeInfo.getOptionDescriptors().entrySet()) {
					optionById.put(new EventOptionID(typeID, entry.getKey()), entry.getValue());
				}
			}

			// Do not update more often than every minute.
			// FIXME: Use JMX notifications instead?
			eventTypeMetaNextUpdate = timestamp + 60 * 1000;
			eventTypeMetas = Collections.unmodifiableList(metadataList);
			eventTypeInfoById = Collections.unmodifiableMap(byId);
			optionInfoById = Collections.unmodifiableMap(optionById);
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
			Long id = (Long) helper.invokeOperation("cloneRecording", //$NON-NLS-1$
					descriptor.getId(), Boolean.TRUE);
			IMutableConstrainedMap<String> options = getEmptyRecordingOptions();
			options.put(RecordingOptionsBuilder.KEY_NAME,
					NLS.bind(Messages.FlightRecorderServiceV2_CLONE_OF_RECORDING_NAME, descriptor.getName()));
			helper.invokeOperation("setRecordingOptions", id, toTabularData(options)); //$NON-NLS-1$
			return getUpdatedRecordingDescriptor(id);
		} catch (Exception e) {
			throw new FlightRecorderException("Could not clone the " + descriptor.getName() + " recording ", e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void updateEventOptions(Long id, IConstrainedMap<EventOptionID> options)
			throws OpenDataException, IOException, FlightRecorderException {
		helper.invokeOperation("setRecordingSettings", id, //$NON-NLS-1$
				toTabularData(options));
	}

	@Override
	public void updateRecordingOptions(IRecordingDescriptor descriptor, IConstrainedMap<String> options)
			throws FlightRecorderException {
		validateOptions(options);
		// Currently (2016-06-01), in some states, JFR complains about the presence of certain
		// options even if unchanged. So, just send the delta.
		IConstrainedMap<String> current = getRecordingOptions(descriptor);
		IConstrainedMap<String> deltaOptions = ConfigurationToolkit.extractDelta(options, current);
		try {
			updateRecordingOptions(descriptor.getId(), deltaOptions);
		} catch (Exception e) {
			throw new FlightRecorderException("Failed updating the recording options for " + descriptor.getName(), e); //$NON-NLS-1$
		}
	}

	private void updateRecordingOptions(Long id, IConstrainedMap<String> options)
			throws OpenDataException, IOException, FlightRecorderException {
		helper.invokeOperation("setRecordingOptions", id, //$NON-NLS-1$
				toTabularData(options));
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
//		return new JfrRecordingInputStreamV2(helper, streamDescriptor, toDate(startTime), toDate(endTime), clone | removeOnClose);

		long serverTime = mbhs.getApproximateServerTime(System.currentTimeMillis());
		IQuantity endDate = EPOCH_MS.quantity(serverTime);
		IQuantity startDate = endDate.subtract(lastPartDuration);
		return openStream(descriptor, startDate, endDate, removeOnClose);
	}

	@Override
	public boolean isEnabled() {
		return isFlightRecorderCommercial()
				? cfs.isCommercialFeaturesEnabled()
				: isAvailable(connection);
	}

	@Override
	public void enable() throws FlightRecorderException {
		try {
			cfs.enableCommercialFeatures();
		} catch (Exception e) {
			throw new FlightRecorderException("Failed to enable commercial features", e); //$NON-NLS-1$
		}
	}
}
