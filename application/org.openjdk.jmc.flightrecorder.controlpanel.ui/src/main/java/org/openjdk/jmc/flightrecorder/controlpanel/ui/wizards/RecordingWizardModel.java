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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards;

import static org.openjdk.jmc.common.unit.UnitLookup.BYTE;
import static org.openjdk.jmc.common.unit.UnitLookup.MEMORY;
import static org.openjdk.jmc.common.unit.UnitLookup.MINUTE;
import static org.openjdk.jmc.common.unit.UnitLookup.SECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.flightrecorder.configuration.IRecorderConfigurationService;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.ConfigurationRepositoryFactory;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationRepository;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.VolatileStorageDelegate;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.IEventTypeInfo;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.ui.common.idesupport.IDESupportToolkit;
import org.openjdk.jmc.ui.common.resource.MCFile;

/**
 * Metadata for configuring and starting a recording
 */
/*
 * FIXME: Would probably be best to remove model and keep everything in the RecordingWizardPage
 * (plus subclasses) again, to avoid problems with keeping model and gui in sync.
 */
public class RecordingWizardModel extends Observable {
	// Recording constants
	private static final String DEFAULT_SELECTED_TEMPLATE = "Profiling"; //$NON-NLS-1$

	protected static final IQuantity DEFAULT_DURATION = MINUTE.quantity(1);
	protected static final IQuantity DEFAULT_DELAY = SECOND.quantity(0);
	private static final Pattern NOT_ALLOWED_IN_NAME = Pattern.compile("[=:;,]"); //$NON-NLS-1$

	// FIXME: Check min values
	private static final IQuantity MIN_SIZE = BYTE.quantity(0);
	private static final IQuantity MIN_AGE = SECOND.quantity(0);
	private static final IQuantity MIN_DELAY = SECOND.quantity(0);
	public static final IQuantity MIN_USABLE_DELAY = SECOND.quantity(1);
	private static final IQuantity MIN_DURATION = SECOND.quantity(1);

	protected static final String JRE_VERSION_CHANGED = "jreVersionChanged"; //$NON-NLS-1$

	// State information
	private final boolean m_editing;
	private final EventConfigurationRepository m_templateRepository;
	private MCFile m_path;
	private String m_name;
	private boolean m_fixedRecording;
	private String m_durationString = ""; //$NON-NLS-1$
	private String m_delayString = ""; //$NON-NLS-1$
	private String m_maxSizeString = ""; //$NON-NLS-1$
	private String m_maxAgeString = ""; //$NON-NLS-1$

	// Captured from the flight recorder service
	private final IDescribedMap<String> m_recordingOptions;
	private final IDescribedMap<EventOptionID> m_eventDefaults;
	private final Map<? extends IEventTypeID, ? extends IEventTypeInfo> m_eventTypeInfos;

	private SchemaVersion m_version;

	/**
	 * A stack of progressively modified recording configurations. Starting with the current
	 * template at index 0, followed by the configuration state at each stage of the wizard, ending
	 * with the active recording configuration at the stage where the user last made a change.
	 */
	private final List<IEventConfiguration> m_activeConfigStack = new ArrayList<>();

	/**
	 * Common initializer for final fields. Only to be used from other (complete) constructors.
	 *
	 * @param configService
	 * @param editing
	 */
	private RecordingWizardModel(IRecorderConfigurationService configService, boolean editing, MCFile path) {
		m_recordingOptions = configService.getDefaultRecordingOptions();
		m_version = SchemaVersion.fromBeanVersion(configService.getVersion());
		if (m_version == null) {
			throw new IllegalArgumentException("Schema version cannot be null"); //$NON-NLS-1$
		}

		if (configService instanceof IFlightRecorderService) {
			IFlightRecorderService flrService = ((IFlightRecorderService) configService);
			Map<? extends IEventTypeID, ? extends IEventTypeInfo> eventTypeInfos;
			try {
				eventTypeInfos = flrService.getEventTypeInfoMapByID();
			} catch (FlightRecorderException e) {
				ControlPanel.getDefault().getLogger().log(Level.WARNING,
						"Could not get initial state from flight recorder", e); //$NON-NLS-1$
				eventTypeInfos = Collections.emptyMap();
			}
			m_eventTypeInfos = eventTypeInfos;
			m_templateRepository = createRepository(flrService);
		} else {
			m_eventTypeInfos = Collections.emptyMap();
			m_templateRepository = createRepository(m_version);
		}
		m_eventDefaults = configService.getDefaultEventOptions();
		m_editing = editing;
		m_path = path;
	}

	public RecordingWizardModel(IRecorderConfigurationService configService, MCFile filename) {
		this(configService, false, filename);
		m_name = Messages.RECORDING_DESCRIPTOR_DEFAULT_NAME;
		if (configService instanceof IFlightRecorderService) {
			// FIXME: Base name on template? "My Default Recording"...?
			m_name = findUniqueName((IFlightRecorderService) configService, m_name);
		}
		setDelay(DEFAULT_DELAY.interactiveFormat());
		setDuration(DEFAULT_DURATION.interactiveFormat());
		m_fixedRecording = true;
		initActive();
	}

	private void initActive() {
		List<IEventConfiguration> templates = m_templateRepository.getTemplates(m_version);
		if (!templates.isEmpty()) {
			IEventConfiguration best = templates.get(0);
			for (IEventConfiguration config : templates) {
				if (DEFAULT_SELECTED_TEMPLATE.equals(config.getName())
						&& Messages.VOLATILE_CONFIGURATION_ON_SERVER.equals(config.getLocationInfo())) {
					best = config;
					break;
				}
			}

			for (IEventConfiguration config : templates) {
				// FIXME: Ugly dependency on localized string, but we do not want to expose the storage delegate either.
				if (Messages.VOLATILE_CONFIGURATION_LAST_STARTED.equals(config.getLocationInfo())) {
					best = config;
				}
			}
			setActiveConfigurationTemplate(best);
		}
	}

	public RecordingWizardModel(IFlightRecorderService flrService, IRecordingDescriptor recordingDescriptor,
			MCFile filename) {
		this(flrService, true, filename);
		setActiveConfigurationTemplate(createRunningConfig(flrService, recordingDescriptor));
		// Make sure templates are sorted.
		m_templateRepository.notifyObservers();

		m_name = recordingDescriptor.getName();
		setDuration(recordingDescriptor.getDuration().interactiveFormat());
		setMaxSize(recordingDescriptor.getMaxSize().interactiveFormat());
		setMaxAge(recordingDescriptor.getMaxAge().interactiveFormat());
		m_fixedRecording = !recordingDescriptor.isContinuous();
	}

	private IEventConfiguration createRunningConfig(
		IFlightRecorderService flrService, IRecordingDescriptor recordingDescriptor) {
		IEventConfiguration config = EventConfiguration
				.createEmpty(VolatileStorageDelegate.getRunningRecordingDelegate(), m_version);
		try {
			IConstrainedMap<EventOptionID> settings = flrService.getEventSettings(recordingDescriptor);
			for (EventOptionID key : settings.keySet()) {
				String persisted = settings.getPersistableString(key);
				if (persisted != null) {
					config.putPersistedString(key, persisted);
				}
			}
			config.setName(recordingDescriptor.getName());
		} catch (FlightRecorderException e) {
			// Fall through using config name "New Template".
		}
		config.setDescription(Messages.EDIT_RECORDING_WIZARD_RUNNING_CONFIGURATION_DESCRIPTION);
		// NOTE: Not ensuring name is unique, since the config won't be kept when the wizard is closed.
		m_templateRepository.add(config);
		return config;
	}

	private EventConfigurationRepository createRepository(IFlightRecorderService flrService) {
		SchemaVersion version = SchemaVersion.fromBeanVersion(flrService.getVersion());

		EventConfigurationRepository repo = createRepository(version);
		try {
			for (String templateXML : flrService.getServerTemplates()) {
				try {
					repo.add(new EventConfiguration(EventConfiguration.createModel(templateXML),
							VolatileStorageDelegate.getOnServerDelegate()));
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (FlightRecorderException e) {
			e.printStackTrace();
		}
		// Force sorting and changes to be cleared. No one is actually listening.
		repo.notifyObservers();
		return repo;
	}

	protected EventConfigurationRepository createRepository(SchemaVersion version) {
		EventConfigurationRepository repo = ConfigurationRepositoryFactory.create();
		repo.setPrototypeTemplate(
				EventConfiguration.createEmpty(VolatileStorageDelegate.getWorkingCopyDelegate(), version));
		return repo;
	}

	/**
	 * Get the default event options, as they were when the model was created. It is preferred in
	 * most circumstances to use the return value from this method rather than calling
	 * {@link #getFlightRecorderService()} and then
	 * {@link IFlightRecorderService#getDefaultEventOptions()}.
	 *
	 * @return an {@link IDescribedMap IDescribedMap&lt;EventOptionID&gt;}, never null
	 */
	public IDescribedMap<EventOptionID> getEventOptions() {
		return m_eventDefaults;
	}

	/**
	 * Get the event type infos of all event types, as they were when the model was created. It is
	 * preferred in most circumstances to use the return value from this method rather than calling
	 * {@link #getFlightRecorderService()} and then
	 * {@link IFlightRecorderService#getEventTypeInfoMapByID()}.
	 *
	 * @return a {@link Map}, possibly empty, but never null
	 */
	public Map<? extends IEventTypeID, ? extends IEventTypeInfo> getEventTypeInfoMap() {
		return m_eventTypeInfos;
	}

	private String findUniqueName(IFlightRecorderService flrService, final String base) {
		boolean hasFoundUniqueName = false;
		String name = base;
		List<IRecordingDescriptor> descriptors = getDescriptors(flrService);
		if (descriptors == null) {
			return name;
		}

		int count = 2; // MyRecording, MyRecording2, MyRecording3 a.s.o.
		while (!hasFoundUniqueName) {
			try {
				boolean exists = existsRecordingName(descriptors, name);
				if (exists) {
					name = base + ' ' + count++;
				} else {
					hasFoundUniqueName = true;
				}
			} catch (Exception e) {
				// Could not find recording information, return what we have
				break;
			}
		}
		return name;
	}

	private boolean existsRecordingName(List<IRecordingDescriptor> descriptors, String name) {
		for (IRecordingDescriptor descriptor : descriptors) {
			if (name.equals(descriptor.getName())) {
				return true;
			}
		}
		return false;
	}

	private List<IRecordingDescriptor> getDescriptors(IFlightRecorderService flrService) {
		try {
			return flrService.getAvailableRecordings();
		} catch (FlightRecorderException e) {
			ControlPanel.getDefault().getLogger().log(Level.WARNING, "Could not retrieve recording descriptors!", e); //$NON-NLS-1$
			return null;
		}
	}

	public MCFile getPath() {
		return m_path;
	}

	public boolean isFixedRecording() {
		return m_fixedRecording;
	}

	public boolean isContinuous() {
		return !isFixedRecording();
	}

	public String getDurationString() {
		return m_durationString;
	}

	public IQuantity getDuration() throws QuantityConversionException {
		if (m_durationString.length() > 0) {
			return doGetDuration();
		}
		return null;
	}

	private IQuantity doGetDuration() throws QuantityConversionException {
		IQuantity duration = TIMESPAN.parseInteractive(m_durationString);
		if (duration.compareTo(MIN_DURATION) < 0) {
			throw QuantityConversionException.tooLow(duration, MIN_DURATION);
		}
		return duration;
	}

	public IQuantity getDelay() throws QuantityConversionException {
		if (m_delayString.length() > 0) {
			IQuantity delay = TIMESPAN.parseInteractive(m_delayString);
			if (delay.compareTo(MIN_DELAY) < 0) {
				throw QuantityConversionException.tooLow(delay, MIN_DELAY);
			}
			if (delay.compareTo(MIN_USABLE_DELAY) < 0) {
				return MIN_DELAY;
			}
			return delay;
		}
		return null;
	}

	public String getDelayString() {
		return m_delayString;
	}

	private IQuantity getMaxSize() throws QuantityConversionException {
		if (m_maxSizeString.length() > 0) {
			IQuantity size = MEMORY.parseInteractive(m_maxSizeString);
			if (size.compareTo(MIN_SIZE) < 0) {
				throw QuantityConversionException.tooLow(size, MIN_SIZE);
			}
			return size;
		}
		return null;
	}

	public String getMaxAgeString() {
		return m_maxAgeString;
	}

	private IQuantity getMaxAge() throws QuantityConversionException {
		if (m_maxAgeString.length() > 0) {
			IQuantity age = TIMESPAN.parseInteractive(m_maxAgeString);
			if (age.compareTo(MIN_AGE) < 0) {
				throw QuantityConversionException.tooLow(age, MIN_AGE);
			}
			return age;
		}
		return null;
	}

	public String getMaxSizeString() {
		return m_maxSizeString;
	}

	public IConstrainedMap<String> buildOptions() throws QuantityConversionException {
		RecordingOptionsBuilder builder = new RecordingOptionsBuilder(m_recordingOptions.emptyWithSameConstraints());
		builder.name(getName());
		if (isFixedRecording()) {
			builder.duration(getDuration()).maxSize(0).maxAge(0).toDisk(true);
		} else {
			builder.duration(0);
			IQuantity maxSize = getMaxSize();
			if (maxSize != null) {
				builder.maxSize(maxSize).toDisk(true);
			}
			IQuantity maxAge = getMaxAge();
			if (maxAge != null) {
				builder.maxAge(maxAge).toDisk(true);
			}
		}
		return builder.build();
	}

	/**
	 * Get the currently active configuration in order to start (or modify) a recording using it.
	 * Additionally, the configuration will be saved as the "last started" configuration, with the
	 * name and description suitably adjusted.
	 */
	public IEventConfiguration getAndSaveActiveConfiguration() {
		IEventConfiguration config = getActiveConfiguration();
		if (config != null) {
			IEventConfiguration lastStarted = config.createWorkingCopy();
			if (getName() != null) {
				String name = NLS.bind(Messages.RECORDING_WIZARD_LAST_STARTED_SETTINGS_FOR_NAME_MSG, getName());
				lastStarted.setName(name);
			}
			if (getPath() != null) {
				String desc = NLS.bind(Messages.RECORDING_WIZARD_LAST_STARTED_DESCRIPTION_MSG, getPath().getPath());
				lastStarted.setDescription(desc);
			}
			ConfigurationRepositoryFactory.saveAsLastStarted(lastStarted);
		}
		return config;
	}

	/**
	 * Get the currently active configuration in order to start (or modify) a recording using it.
	 * Additionally, the configuration will be saved as the "last started" configuration, with the
	 * name and description suitably adjusted.
	 */
	public IConstrainedMap<EventOptionID> getAndSaveEventSettings() {
		IEventConfiguration config = getAndSaveActiveConfiguration();
		return config.getEventOptions(m_eventDefaults.emptyWithSameConstraints());
	}

	/**
	 * Set the recording configuration template to be used as a basis for configurations at later
	 * wizard stages. The template should not be modified.
	 *
	 * @param template
	 *            a recording configuration template
	 */
	public void setActiveConfigurationTemplate(IEventConfiguration template) {
		m_activeConfigStack.clear();
		if (template != null) {
			m_activeConfigStack.add(template);
		}
		onChange();
	}

	/**
	 * The currently active configuration that would be sent to the server if the wizard was
	 * finished now. When obtained from this method, it should not be modified.
	 */
	public IEventConfiguration getActiveConfiguration() {
		if (!m_activeConfigStack.isEmpty()) {
			return m_activeConfigStack.get(m_activeConfigStack.size() - 1);
		}
		return null;
	}

	/**
	 * Obtain the current recording configuration for the wizard stage {@code wizardStage}, deriving
	 * from configurations of earlier stages if needed. If {@code wizardStage} is greater than zero,
	 * the returned configuration may be modified. But when modifying a configuration, be sure to
	 * call {@link #flushConfigurationsBeyond(int)} with that {@code wizardStage}.
	 */
	public IEventConfiguration getCurrentConfigurationAt(int wizardStage) {
		if (m_activeConfigStack.size() > wizardStage) {
			return m_activeConfigStack.get(wizardStage);
		} else if (m_activeConfigStack.isEmpty()) {
			return null;
		} else if (wizardStage > 0) {
			IEventConfiguration base = getCurrentConfigurationAt(wizardStage - 1);
			assert m_activeConfigStack.size() == wizardStage;
			IEventConfiguration derived = base.createWorkingCopy();
			m_activeConfigStack.add(derived);
			return derived;
		}

		return null;
	}

	/**
	 * Discard all derived configurations beyond {@code wizardStage}, making the configuration at
	 * {@code wizardStage} the active one.
	 *
	 * @param wizardStage
	 */
	public void flushConfigurationsBeyond(int wizardStage) {
		int maxSize = wizardStage + 1;
		while (m_activeConfigStack.size() > maxSize) {
			m_activeConfigStack.remove(maxSize);
		}
	}

	public boolean isEditing() {
		return m_editing;
	}

	public void setMaxSize(String maxSizeText) {
		m_maxSizeString = maxSizeText;
		onChange();
	}

	public void setMaxAge(String maxAgeText) {
		m_maxAgeString = maxAgeText;
		onChange();
	}

	public String getName() {
		return m_name;
	}

	public EventConfigurationRepository getTemplateRepository() {
		return m_templateRepository;
	}

	public void setPath(MCFile path) {
		m_path = path;
		onChange();
	}

	public void setName(String name) {
		m_name = name;
		onChange();
	}

	public void setFixedRecording(boolean fixedRecording) {
		m_fixedRecording = fixedRecording;
		onChange();
	}

	public void setDuration(String durationText) {
		m_durationString = durationText;
		onChange();
	}

	public void setDelay(String delayText) {
		m_delayString = delayText;
		onChange();
	}

	public IDescribedMap<String> getAvailableRecordingOptions() {
		return m_recordingOptions;
	}

	protected void onChange() {
		setChanged();
		notifyObservers();
	}

	/**
	 * Check all settings to see if they are valid.
	 *
	 * @return error message for the first setting that did not pass validation, null if everything
	 *         was valid.
	 */
	protected String checkForErrors(boolean hasExtraTemplate) {

		if (m_fixedRecording) {
			try {
				getDelay();
			} catch (QuantityConversionException e) {
				return NLS.bind(Messages.RECORDING_WIZARD_PAGE_DELAY_TIME_ERROR_MSG, e.getLocalizedMessage());
			}
			try {
				// Call private method to raise error if duration is missing
				doGetDuration();
			} catch (QuantityConversionException e) {
				return NLS.bind(Messages.RECORDING_WIZARD_PAGE_RECORDING_TIME_ERROR_MSG, e.getLocalizedMessage());
			}
		}

		// Name
		if (m_name.length() == 0) {
			return Messages.RECORDING_WIZARD_PAGE_RECORDING_NAME_ERROR_MSG;
		} else if (NOT_ALLOWED_IN_NAME.matcher(m_name).find()) {
			return NLS.bind(Messages.RECORDING_WIZARD_PAGE_RECORDING_NAME_INVALID_CHARS, NOT_ALLOWED_IN_NAME);
		}

		// Template
		if (getActiveConfiguration() == null) {
			EventConfigurationRepository repo = getTemplateRepository();
			// NOTE: It is a very real possibility that the JVM doesn't have any templates. (Users can add or remove them.)
			if (repo.isEmpty() && !hasExtraTemplate) {
				// FIXME: Just add an empty template instead
				return Messages.RECORDING_WIZARD_PAGE_NO_TEMPLATES_IN_MANAGER_ERROR_MSG;
			} else {
				return Messages.RECORDING_WIZARD_PAGE_NO_TEMPLATE_SELECTED_ERROR_MSG;
			}
		}

		if (!m_fixedRecording) {
			try {
				getMaxSize();
			} catch (QuantityConversionException e) {
				return NLS.bind(Messages.RECORDING_WIZARD_PAGE_MAX_SIZE_ERROR_MSG, e.getLocalizedMessage());
			}
			try {
				getMaxAge();
			} catch (QuantityConversionException e) {
				return NLS.bind(Messages.RECORDING_WIZARD_PAGE_MAX_AGE_ERROR_MSG, e.getLocalizedMessage());
			}
		}
		IStatus validation = IDESupportToolkit.validateFileResourcePath(m_path.getPath());
		if (validation.getSeverity() == IStatus.ERROR) {
			return validation.getMessage();
		}
		return null;
	}

	public String getWarningMessage() {
		IStatus validation = IDESupportToolkit.validateFileResourcePath(m_path.getPath());
		return ControlPanel.getRecordingFileValidationMessage(validation);
	}

	public final SchemaVersion getVersion() {
		return m_version;
	}

	protected void setVersion(SchemaVersion version) {
		m_version = version;
	}

	public boolean isCompatibleVersion(IEventConfiguration configuration) {
		return m_version.equals(configuration.getVersion());
	}
}
