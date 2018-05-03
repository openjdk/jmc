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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.model;

import static org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PathElement.PathElementKind.IN_CONFIGURATION;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PathElement.PathElementKind.IN_SERVER;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PathElement.PathElementKind.UNKNOWN;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.flightrecorder.configuration.ConfigurationToolkit;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.EventConfigurationPart;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PathElement.PathElementKind;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards.RecordingWizardModel;
import org.openjdk.jmc.rjmx.services.jfr.IEventTypeInfo;

/**
 * Groups a recording event configuration with settings found on a server. Used as input for a
 * {@link EventConfigurationPart}.
 */
// FIXME: Rename to something more informative, along the lines of the class javadoc description.
public class EventConfigurationModel implements Consumer<EventOptionVisitor> {
	private final EventConfiguration configuration;
	private final IDescribedMap<EventOptionID> serverDefaults;
	private final Map<? extends IEventTypeID, ? extends IEventTypeInfo> serverEventTypeInfos;

	public EventConfigurationModel createEditableClone() {
		EventConfigurationModel clone = create(configuration.createClone(), serverDefaults, serverEventTypeInfos);
		return clone;
	}

	/**
	 * @param configuration
	 * @param recordingWizardModel
	 *            model to extract server settings from, if the configuration and model versions are
	 *            compatible.
	 */
	public static EventConfigurationModel create(
		IEventConfiguration configuration, RecordingWizardModel recordingWizardModel) {
		boolean compatibleVersion = recordingWizardModel.isCompatibleVersion(configuration);
		if (compatibleVersion) {
			return create(configuration, recordingWizardModel.getEventOptions(),
					recordingWizardModel.getEventTypeInfoMap());
		}
		return create(configuration, ConfigurationToolkit.getEventOptions(configuration.getVersion()),
				Collections.emptyMap());

	}

	/**
	 * @param configuration
	 *            event configuration
	 * @param serverDefaults
	 *            event option defaults, ideally from the server
	 * @param serverEventTypeInfos
	 *            event type info from server, or empty map for legacy "offline" mode.
	 * @return
	 */
	public static EventConfigurationModel create(
		IEventConfiguration configuration, IDescribedMap<EventOptionID> serverDefaults,
		Map<? extends IEventTypeID, ? extends IEventTypeInfo> serverEventTypeInfos) {
		return new EventConfigurationModel(configuration, serverDefaults, serverEventTypeInfos);
	}

	private EventConfigurationModel(IEventConfiguration configuration, IDescribedMap<EventOptionID> serverDefaults,
			Map<? extends IEventTypeID, ? extends IEventTypeInfo> serverEventTypeInfos) {
		// FIXME: Throw if this is not an EventConfiguration?
		this.configuration = (EventConfiguration) configuration;
		this.serverDefaults = serverDefaults;
		this.serverEventTypeInfos = serverEventTypeInfos;
	}

	public IEventConfiguration getConfiguration() {
		return configuration;
	}

	public IDescribedMap<EventOptionID> getServerDefaults() {
		return serverDefaults;
	}

	public Map<? extends IEventTypeID, ? extends IEventTypeInfo> getServerEventTypeInfos() {
		return serverEventTypeInfos;
	}

	/*
	 * The notion of an entire model being offline is a legacy concept that we don't need to
	 * maintain. In JDK 9 you are always more or less offline.
	 */
	public boolean isOffline() {
		return serverDefaults == null || serverDefaults.keySet() == null || serverDefaults.keySet().isEmpty();
	}

	public static EventConfigurationModel pushServerMetadataToLocalConfiguration(
		IEventConfiguration configuration, IDescribedMap<EventOptionID> serverDefaults,
		Map<? extends IEventTypeID, ? extends IEventTypeInfo> serverEventTypeInfos, boolean override) {
		EventConfigurationModel model = EventConfigurationModel.create(configuration, serverDefaults,
				serverEventTypeInfos);
		model.pushServerMetadataToLocalConfiguration(override);
		return model;
	}

	public void pushServerMetadataToLocalConfiguration(boolean override) {
		// FIXME: When we do override, should we clear the categories that only exist in the JFC?
		// FIXME: Can we make sure we put the control part last, even though we recreate all the rest?
		for (Entry<? extends IEventTypeID, ? extends IEventTypeInfo> entry : serverEventTypeInfos.entrySet()) {
			IEventTypeID eventTypeID = entry.getKey();
			if (configuration.getVersion() == SchemaVersion.V2) {
				String[] configCategory = configuration.getEventCategory(eventTypeID);
				if (override || configCategory == null || configCategory.length == 0) {
					IEventTypeInfo serverEventTypeInfo = entry.getValue();
					String[] categories = serverEventTypeInfo.getHierarchicalCategory();
					configuration.putEventInCategory(eventTypeID, categories);
				}
			}
			IEventTypeInfo serverEventTypeInfo = entry.getValue();
			configuration.populateEventMetadata(eventTypeID, serverEventTypeInfo, override);
		}
		for (EventOptionID optionKey : serverDefaults.keySet()) {
			IEventTypeInfo serverEventTypeInfo = serverEventTypeInfos.get(optionKey.getEventTypeID());
			IOptionDescriptor<?> serverOptionInfo = serverEventTypeInfo.getOptionInfo(optionKey.getOptionKey());
			configuration.populateOption(optionKey, serverOptionInfo, serverDefaults.getPersistableString(optionKey),
					override);
		}
	}

	@Override
	public void accept(EventOptionVisitor visitor) {
		Set<IEventTypeID> eventTypeIDs = new HashSet<>();
		eventTypeIDs.addAll(configuration.getConfigEventTypes());
		eventTypeIDs.addAll(serverEventTypeInfos.keySet());

		for (IEventTypeID eventTypeID : eventTypeIDs) {
			IEventTypeInfo serverInfo = serverEventTypeInfos.get(eventTypeID);

			String[] category = getCategory(eventTypeID, serverInfo);
			String label = getLabel(eventTypeID, serverInfo);
			String description = getDescription(eventTypeID, serverInfo);
			visitEventType(visitor, eventTypeID, category, label, description, configuration.hasEvent(eventTypeID),
					serverInfo != null);
			// FIXME: Get options from config and from server for this event type, and do the addOptions call once for each event type.
		}
		// FIXME: Would like to avoid doing two calls to addOptions, so send in both serverDefaults and configuration.getEventOptions(ConfigurationToolkit.getEventOptions(version))
		addOptions(visitor,
				(IDescribedMap<EventOptionID>) configuration.getEventOptions(serverDefaults.emptyWithSameConstraints()),
				IN_CONFIGURATION, serverEventTypeInfos);
		addOptions(visitor, serverDefaults, IN_SERVER, serverEventTypeInfos);
	}

	private void visitEventType(
		EventOptionVisitor visitor, IEventTypeID eventTypeID, String[] category, String label, String description,
		boolean inConfig, boolean onServer) {
		PathElementKind kind = UNKNOWN;
		if (inConfig) {
			kind = kind.add(IN_CONFIGURATION);
		}
		if (onServer) {
			kind = kind.add(IN_SERVER);
		}

		visitor.visitEventType(kind, eventTypeID, category, label, description);
	}

	private void addOptions(
		EventOptionVisitor visitor, IDescribedMap<EventOptionID> options, PathElementKind kind,
		Map<? extends IEventTypeID, ? extends IEventTypeInfo> serverEventTypeInfos) {
		for (EventOptionID eventOptionID : options.keySet()) {
			IEventTypeID eventTypeID = eventOptionID.getEventTypeID();
			IEventTypeInfo serverInfo = serverEventTypeInfos.get(eventTypeID);
			IOptionDescriptor<?> serverOptionDescriptor = serverInfo != null
					? serverInfo.getOptionInfo(eventOptionID.getOptionKey()) : null;

			String optionValue = options.getPersistableString(eventOptionID);
			IConstraint<?> constraint = options.getConstraint(eventOptionID);
			String[] eventCategory = getCategory(eventTypeID, serverInfo);
			String eventLabel = getLabel(eventTypeID, serverInfo);
			String eventDescription = getDescription(eventTypeID, serverInfo);
			IDescribable optionDescribable = options.getDescribable(eventOptionID);
			String optionLabel = getOptionLabel(eventOptionID, optionDescribable, serverOptionDescriptor);
			String optionDescription = getOptionDescription(eventOptionID, optionDescribable, serverOptionDescriptor);

			visitor.visitOption(optionValue, constraint, kind, eventOptionID, eventCategory, optionLabel,
					optionDescription, eventLabel, eventDescription);
		}
	}

	private String[] getCategory(IEventTypeID eventTypeID, IEventTypeInfo serverInfo) {
		String path[] = configuration.getEventCategory(eventTypeID);

		if (path.length == 0 && serverInfo != null) {
			path = serverInfo.getHierarchicalCategory();
		}
		if (path.length == 0) {
			String[] fallback = eventTypeID.getFallbackHierarchy();
			if (fallback != null && fallback.length > 1) {
				path = Arrays.copyOf(fallback, fallback.length - 1);
			}
		}
		return path;
	}

	private String getLabel(IEventTypeID eventTypeID, IEventTypeInfo serverInfo) {
		String label = configuration.getEventLabel(eventTypeID);
		if (label == null && serverInfo != null) {
			label = serverInfo.getName();
		}
		if (label == null) {
			String[] hierarchy = eventTypeID.getFallbackHierarchy();
			label = hierarchy.length > 1 ? hierarchy[hierarchy.length - 1] : eventTypeID.getRelativeKey();
		}
		return label;
	}

	private String getDescription(IEventTypeID eventTypeID, IEventTypeInfo serverInfo) {
		String description = configuration.getEventDescription(eventTypeID);
		if (description == null && serverInfo != null) {
			description = serverInfo.getDescription();
		}
		String fullKey = "[" + eventTypeID.getFullKey() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		return (description != null ? description + " " + fullKey : fullKey); //$NON-NLS-1$
	}

	private String getOptionLabel(
		EventOptionID eventOptionID, IDescribable optionDescribable, IOptionDescriptor<?> serverOptionDescriptor) {
		String optionLabel = null;
		optionLabel = configuration.getConfigOptionLabel(eventOptionID);
		if (optionLabel == null && optionDescribable != null) {
			optionLabel = optionDescribable.getName();
		}
		if (optionLabel == null && serverOptionDescriptor != null) {
			optionLabel = serverOptionDescriptor.getName();
		}
		if (optionLabel == null) {
			optionLabel = eventOptionID.getOptionKey();
		}
		// FIXME: Potentially translate the four known option labels (Enabled, Period, Threshold, Stack Trace) here
		return optionLabel;
	}

	private String getOptionDescription(
		EventOptionID eventOptionID, IDescribable optionDescribable, IOptionDescriptor<?> serverOptionDescriptor) {
		String optionDescription = null;
		optionDescription = configuration.getConfigOptionDescription(eventOptionID);
		if (optionDescription == null && optionDescribable != null) {
			optionDescription = optionDescribable.getDescription();
		}
		if (optionDescription == null && serverOptionDescriptor != null) {
			optionDescription = serverOptionDescriptor.getDescription();
		}
		String optiondId = "[" + eventOptionID.getOptionKey() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		return (optionDescription != null ? optionDescription + " " + optiondId : optiondId); //$NON-NLS-1$
	}
}
