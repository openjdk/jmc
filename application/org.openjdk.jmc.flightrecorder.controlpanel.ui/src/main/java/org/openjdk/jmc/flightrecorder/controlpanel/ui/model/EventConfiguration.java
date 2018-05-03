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

import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_CONTENT_TYPE;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_CONTROL_REFERENCE;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_DESCRIPTION;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_LABEL;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_LABEL_MANDATORY;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_NAME;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_PATH;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_URI;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_VERSION;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_CATEGORY;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_CONFIGURATION_V1;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_CONTROL;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_EVENTTYPE_V1;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_EVENTTYPE_V2;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_PRODUCER;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_SETTING;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.SimpleConstrainedMap;
import org.openjdk.jmc.flightrecorder.configuration.ConfigurationToolkit;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints;
import org.openjdk.jmc.flightrecorder.configuration.spi.IConfigurationStorageDelegate;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCXMLValidator;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLAttribute;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLTag;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLTagInstance;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.services.jfr.IEventTypeInfo;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Class that holds a JFR event configuration.
 */
// FIXME: Make two different subclasses of this, V1 and V2?
public final class EventConfiguration implements IEventConfiguration {

	private XMLModel xmlModel;
	private final IConfigurationStorageDelegate storageDelegate;
	private final SchemaVersion version;
	/**
	 * NOTE: This may be null. It is only non-null for working copies.
	 */
	private final IEventConfiguration original;

	public static void validate(InputStream xmlStream, String streamName, SchemaVersion version)
			throws ParseException, IOException {
		InputStream schemaStream = version.createSchemaStream();
		if (schemaStream != null) {
			try {
				SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema"); //$NON-NLS-1$
				XMLModel.validate(xmlStream, streamName, schemaFactory.newSchema(new StreamSource(schemaStream)));
			} catch (SAXException e) {
				throw new IOException("Trouble parsing schema for version " + version, e); //$NON-NLS-1$
			} finally {
				IOToolkit.closeSilently(schemaStream);
			}
		} else {
			throw new IOException("Could not locate schema for version " + version); //$NON-NLS-1$
		}
	}

	public static IEventConfiguration createEmpty(IConfigurationStorageDelegate delegate, SchemaVersion version) {
		Map<String, String> attributes = Collections.singletonMap(JFCGrammar.ATTRIBUTE_VERSION.getName(),
				version.attributeValue());
		XMLModel model = XMLModel.createEmpty(JFCXMLValidator.getValidator(), attributes);
		IEventConfiguration config = new EventConfiguration(model, delegate, null);
		config.setName(Messages.RECORDING_TEMPLATE_NEW_NAME);
		return config;
	}

	public static XMLModel createModel(String xmlText) throws ParseException, IOException {
		return XMLModel.create(new InputSource(new StringReader(xmlText)), JFCXMLValidator.getValidator());
	}

	public static XMLModel createModel(File file) throws FileNotFoundException, IOException, ParseException {
		return createModel(new FileInputStream(file));
	}

	public static XMLModel createModel(InputStream inStream) throws IOException, ParseException {
		XMLModel model;
		try {
			model = XMLModel.create(new InputSource(inStream), JFCXMLValidator.getValidator());
		} finally {
			IOToolkit.closeSilently(inStream);
		}
		return model;
	}

	public EventConfiguration(XMLModel xml) {
		this(xml, VolatileStorageDelegate.getWorkingCopyDelegate(), null);
	}

	public EventConfiguration(IConfigurationStorageDelegate delegate) throws IOException, ParseException {
		this(createModel(delegate.getContents()), delegate, null);
	}

	public EventConfiguration(XMLModel xml, IConfigurationStorageDelegate storageDelegate) {
		this(xml, storageDelegate, null);
	}

	public EventConfiguration(XMLModel xml, IConfigurationStorageDelegate storageDelegate,
			IEventConfiguration original) {
		xmlModel = xml;
		this.storageDelegate = storageDelegate;
		this.original = original;
		version = SchemaVersion.fromBeanVersion(getRoot().getValue(ATTRIBUTE_VERSION));
		if (version == null) {
			throw new IllegalArgumentException("Schema version cannot be null"); //$NON-NLS-1$
		}
	}

	/**
	 * Replace the contents of this configuration with that from {@code workingCopy}.
	 *
	 * @param workingCopy
	 *            a working copy that was returned by {@link #createWorkingCopy()} on this
	 *            configuration.
	 * @return true if the contents was successfully replaced, false otherwise.
	 */
	boolean replaceWithContentsFrom(IEventConfiguration workingCopy) {
		if ((workingCopy.getOriginal() == this) && (workingCopy instanceof EventConfiguration)) {
			xmlModel = ((EventConfiguration) workingCopy).getXMLModel().deepClone();
			return true;
		}
		return false;
	}

	private XMLTagInstance getRoot() {
		return xmlModel.getRoot();
	}

	@Override
	public String getName() {
		if (getRoot().getTag() == TAG_CONFIGURATION_V1) {
			return getRoot().getValue(ATTRIBUTE_NAME);
		} else {
			return getRoot().getValue(ATTRIBUTE_LABEL_MANDATORY);
		}
	}

	@Override
	public void setName(String name) {
		if (getRoot().getTag() == TAG_CONFIGURATION_V1) {
			getRoot().setValue(ATTRIBUTE_NAME, name);
		} else {
			getRoot().setValue(ATTRIBUTE_LABEL_MANDATORY, name);
		}
	}

	@Override
	public String getDescription() {
		return getRoot().getValue(ATTRIBUTE_DESCRIPTION);
	}

	@Override
	public void setDescription(String description) {
		getRoot().setValue(ATTRIBUTE_DESCRIPTION, description);
	}

	@Override
	public SchemaVersion getVersion() {
		return version;
	}

	@Override
	public IConstrainedMap<EventOptionID> getEventOptions(IMutableConstrainedMap<EventOptionID> options) {
		switch (version) {
		case V1:
			for (XMLTagInstance producer : getRoot().getTagsInstances(TAG_PRODUCER)) {
				String producerURI = producer.getValue(ATTRIBUTE_URI);
				for (XMLTagInstance event : producer.getTagsInstances(TAG_EVENTTYPE_V1)) {
					String eventPath = event.getValue(ATTRIBUTE_PATH);
					IEventTypeID eventTypeID = ConfigurationToolkit.createEventTypeID(producerURI, eventPath);
					getOptionsFromEventTo(eventTypeID, event, options);
				}
			}
			break;

		case V2:
			getNestedEventOptions(getRoot(), options);
			break;

		default:
			break;
		}
		return options;
	}

	private void getNestedEventOptions(XMLTagInstance element, IMutableConstrainedMap<EventOptionID> options) {
		for (XMLTagInstance child : element.getTagsInstances()) {
			XMLTag childTag = child.getTag();
			if (childTag == TAG_EVENTTYPE_V2) {
				String eventName = child.getValue(ATTRIBUTE_NAME);
				IEventTypeID eventTypeID = ConfigurationToolkit.createEventTypeID(eventName);
				getOptionsFromEventTo(eventTypeID, child, options);
			} else if (childTag == TAG_CATEGORY) {
				getNestedEventOptions(child, options);
			}
		}
	}

	private void getOptionsFromEventTo(
		IEventTypeID eventTypeID, XMLTagInstance event, IMutableConstrainedMap<EventOptionID> options) {
		for (XMLTagInstance option : event.getTagsInstances(TAG_SETTING)) {
			EventOptionID optionID = new EventOptionID(eventTypeID, option.getValue(ATTRIBUTE_NAME));
			String contentType = option.getExplicitValue(ATTRIBUTE_CONTENT_TYPE);
			// Only add options the map can handle. (JFRv2 should handle almost everything.)
			try {
				if (contentType != null) {
					IConstraint<?> constraint = CommonConstraints.forContentTypeV2(contentType);
					options.putPersistedString(optionID, constraint, option.getContent());
				} else {
					options.putPersistedString(optionID, option.getContent());
				}
			} catch (QuantityConversionException | IllegalArgumentException e) {
				ControlPanel.getDefault().getLogger().log(Level.FINE, e.getMessage(), e);
			}
		}
	}

	@Override
	public String getPersistableString(EventOptionID optionID) {
		XMLTagInstance settingElement = findOption(optionID, false);
		return (settingElement != null) ? settingElement.getContent() : null;
	}

	public boolean hasOption(EventOptionID eventOptionID) {
		return findOption(eventOptionID, false) != null;
	}

	private XMLTagInstance findProducer(String producerURI, boolean create) {
		XMLTagInstance producer = getRoot().findTagWithAttribute(TAG_PRODUCER, ATTRIBUTE_URI, producerURI);
		if (create && (producer == null)) {
			producer = getRoot().create(TAG_PRODUCER);
			producer.setValue(ATTRIBUTE_URI, producerURI);
		}
		return producer;
	}

	private XMLTagInstance findCategory(XMLTagInstance parent, String category, boolean create) {
		XMLTagInstance categoryInstance = parent.findTagWithAttribute(TAG_CATEGORY, ATTRIBUTE_LABEL_MANDATORY,
				category);
		if (categoryInstance == null) {
			for (XMLTagInstance subcategory : parent.getTagsInstances(TAG_CATEGORY)) {
				categoryInstance = findCategory(subcategory, category, false);
				if (categoryInstance != null) {
					return categoryInstance;
				}
			}
		}
		if (create && (categoryInstance == null)) {
			categoryInstance = parent.create(TAG_CATEGORY);
			categoryInstance.setValue(ATTRIBUTE_LABEL_MANDATORY, category);
		}
		return categoryInstance;
	}

	public boolean hasEvent(IEventTypeID eventTypeID) {
		return findEvent(eventTypeID, false) != null;
	}

	XMLTagInstance findEvent(IEventTypeID eventTypeID, boolean create) {
		XMLTagInstance origin;
		XMLTag eventTag;
		XMLAttribute keyAttribute;
		XMLTagInstance eventType;
		if (eventTypeID.getProducerKey() != null) {
			origin = findProducer(eventTypeID.getProducerKey(), create);
			eventTag = TAG_EVENTTYPE_V1;
			keyAttribute = ATTRIBUTE_PATH;
			eventType = (origin != null)
					? origin.findTagWithAttribute(eventTag, keyAttribute, eventTypeID.getRelativeKey()) : null;
		} else {
			origin = getRoot();
			eventTag = TAG_EVENTTYPE_V2;
			keyAttribute = ATTRIBUTE_NAME;
			eventType = origin.findNestedTagWithAttribute(TAG_CATEGORY, eventTag, keyAttribute,
					eventTypeID.getRelativeKey());
		}

		if (create && (eventType == null)) {
			eventType = origin.create(eventTag);
			eventType.setValue(keyAttribute, eventTypeID.getRelativeKey());
		}
		return eventType;
	}

	private XMLTagInstance findOption(EventOptionID eventOptionID, boolean create) {
		XMLTagInstance eventType = findEvent(eventOptionID.getEventTypeID(), create);
		XMLTagInstance option = (eventType != null)
				? eventType.findTagWithAttribute(TAG_SETTING, ATTRIBUTE_NAME, eventOptionID.getOptionKey()) : null;
		if (create && (option == null)) {
			option = eventType.create(TAG_SETTING);
			option.setValue(ATTRIBUTE_NAME, eventOptionID.getOptionKey());
		}
		return option;
	}

	@Override
	public void putPersistedString(EventOptionID optionID, String persisted) {
		XMLTagInstance optionTag = findOption(optionID, true);
		optionTag.setContent(persisted);
	}

	/**
	 * Removes an option
	 *
	 * @param optionID
	 */
	public void removeOption(EventOptionID optionID) {
		XMLTagInstance optionTag = findOption(optionID, false);
		if (optionTag != null) {
			optionTag.getParent().remove(optionTag);
		}
	}

	@Override
	public boolean isCloneable() {
		return true;
	}

	@Override
	public IEventConfiguration createClone() {
		return createCloneWithStorage(storageDelegate);
	}

	@Override
	public IEventConfiguration createCloneWithStorage(IConfigurationStorageDelegate storageDelegate) {
		return new EventConfiguration(xmlModel.deepClone(), storageDelegate, original);
	}

	@Override
	public IEventConfiguration createWorkingCopy() {
		return new EventConfiguration(xmlModel.deepClone(), VolatileStorageDelegate.getWorkingCopyDelegate(), this);
	}

	@Override
	public IEventConfiguration getOriginal() {
		return original;
	}

	@Override
	public boolean isExportable() {
		return true;
	}

	@Override
	public void exportToFile(File file) throws IOException {
		xmlModel.saveToFile(file);
	}

	@Override
	public boolean hasControlElements() {
		final XMLTagInstance root = getRoot();
		// JFR 1.0
		for (XMLTagInstance producerTag : root.getTagsInstances(TAG_PRODUCER)) {
			if (producerTag.getTagsInstances(TAG_CONTROL).size() > 0) {
				return true;
			}
		}
		// JFR 2.0
		if (root.getTagsInstances(TAG_CONTROL).size() > 0) {
			return true;
		}
		return false;
	}

	@Override
	public boolean removeControlElements() {
		boolean changed = false;
		final XMLTagInstance root = getRoot();
		switch (version) {
		case V1:
			for (XMLTagInstance producerElement : root.getTagsInstances(TAG_PRODUCER)) {
				for (XMLTagInstance controlElement : producerElement.getTagsInstances(TAG_CONTROL)) {
					producerElement.remove(controlElement);
					changed = true;
				}
				for (XMLTagInstance eventElement : producerElement.getTagsInstances(TAG_EVENTTYPE_V1)) {
					for (XMLTagInstance settingElement : eventElement.getTagsInstances(TAG_SETTING)) {
						changed |= settingElement.setValue(ATTRIBUTE_CONTROL_REFERENCE, null);
					}
				}
			}
			break;

		case V2:
			for (XMLTagInstance controlElement : root.getTagsInstances(TAG_CONTROL)) {
				root.remove(controlElement);
				changed = true;
			}
			changed |= removeNestedControlReferences(root);
			break;

		default:
			break;
		}
		return changed;
	}

	private boolean removeNestedControlReferences(XMLTagInstance parent) {
		boolean changed = false;
		for (XMLTagInstance child : parent.getTagsInstances()) {
			XMLTag childTag = child.getTag();
			if (childTag == TAG_EVENTTYPE_V2) {
				for (XMLTagInstance settingElement : child.getTagsInstances(TAG_SETTING)) {
					changed |= settingElement.setValue(ATTRIBUTE_CONTROL_REFERENCE, null);
				}
			} else if (childTag == TAG_CATEGORY) {
				changed |= removeNestedControlReferences(child);
			}
		}
		return changed;
	}

	public XMLModel getXMLModel() {
		return xmlModel;
	}

	@Override
	public String getLocationPath() {
		return storageDelegate.getLocationPath();
	}

	@Override
	public String getLocationInfo() {
		return storageDelegate.getLocationInfo();
	}

	@Override
	public boolean isDeletable() {
		return storageDelegate.isDeletable();
	}

	@Override
	public boolean delete() {
		return storageDelegate.delete();
	}

	@Override
	public boolean isSaveable() {
		return storageDelegate.isSaveable();
	}

	@Override
	public boolean save() {
		Writer writer = new StringWriter(2000); // Below 2048 to keep initial char array within 4kB, next within 8kB,
												// and so on.
		if (xmlModel.writeTo(writer)) {
			try {
				return storageDelegate.save(writer.toString());
			} catch (IOException e) {
				ControlPanel.getDefault().getLogger().log(Level.WARNING, "Cannot save configuration.", e); //$NON-NLS-1$
			}
		}
		return false;
	}

	@Override
	public boolean equalSettings(IEventConfiguration other) {
		if (other == this) {
			return true;
		}

		IMutableConstrainedMap<EventOptionID> ourOptions = new SimpleConstrainedMap<>(PLAIN_TEXT.getPersister());
		getEventOptions(ourOptions);
		for (EventOptionID key : ourOptions.keySet()) {
			// FIXME: Check for null?
			if (!ourOptions.getPersistableString(key).equals(other.getPersistableString(key))) {
				return false;
			}
		}
		/*
		 * All our settings were in the other holder. Must now check size to ensure the opposite.
		 * We're doing this last as it might be expensive. Otherwise, we could just have called
		 * equals() on the maps.
		 */
		return ourOptions.keySet().size() == other.getEventOptions(ourOptions.emptyWithSameConstraints())
				.keySet().size();
	}

	public Set<IEventTypeID> getConfigEventTypes() {
		Set<IEventTypeID> eventTypes = new HashSet<>();
		collectConfigEventTypes(getRoot(), eventTypes);
		return eventTypes;
	}

	private void collectConfigEventTypes(XMLTagInstance tagInstance, Set<IEventTypeID> eventTypes) {
		for (XMLTagInstance childCategory : tagInstance.getTagsInstances(TAG_CATEGORY)) {
			collectConfigEventTypes(childCategory, eventTypes);
		}
		for (XMLTagInstance childCategory : tagInstance.getTagsInstances(TAG_PRODUCER)) {
			collectConfigEventTypes(childCategory, eventTypes);
		}
		XMLTag eventTag = SchemaVersion.V2.equals(version) ? TAG_EVENTTYPE_V2 : TAG_EVENTTYPE_V1;
		for (XMLTagInstance event : tagInstance.getTagsInstances(eventTag)) {
			IEventTypeID eventTypeID = createEventTypeID(event);
			eventTypes.add(eventTypeID);
		}
	}

	private IEventTypeID createEventTypeID(XMLTagInstance event) {
		if (event.getParent().getTag().equals(TAG_PRODUCER)) {
			return ConfigurationToolkit.createEventTypeID(event.getParent().getValue(ATTRIBUTE_URI),
					event.getValue(ATTRIBUTE_PATH));
		}
		return ConfigurationToolkit.createEventTypeID(event.getValue(ATTRIBUTE_NAME));
	}

	public String[] getEventCategory(IEventTypeID eventTypeID) {
		List<String> categories = new ArrayList<>();
		XMLTagInstance eventTagInstance = findEvent(eventTypeID, false);
		if (eventTagInstance != null) {
			XMLTagInstance current = eventTagInstance.getParent();
			while (current.getTag().equals(TAG_CATEGORY)) {
				categories.add(0, current.getValue(ATTRIBUTE_LABEL_MANDATORY));
				current = current.getParent();
			}
		}
		return categories.toArray(new String[categories.size()]);
	}

	public String getEventLabel(IEventTypeID eventTypeID) {
		XMLTagInstance eventTagInstance = findEvent(eventTypeID, false);
		return eventTagInstance != null ? eventTagInstance.getExplicitValue(ATTRIBUTE_LABEL) : null;
	}

	public String getEventDescription(IEventTypeID eventTypeID) {
		XMLTagInstance eventTagInstance = findEvent(eventTypeID, false);
		return eventTagInstance != null ? eventTagInstance.getExplicitValue(ATTRIBUTE_DESCRIPTION) : null;
	}

	public Set<EventOptionID> getOptionIDs(IEventTypeID eventTypeID) {
		XMLTagInstance eventType = findEvent(eventTypeID, false);
		Set<EventOptionID> options = new HashSet<>();
		if (eventType != null) {
			for (XMLTagInstance setting : eventType.getTagsInstances(TAG_SETTING)) {
				EventOptionID optionID = new EventOptionID(eventTypeID, setting.getExplicitValue(ATTRIBUTE_NAME));
				options.add(optionID);
			}
		}
		return options;
	}

	public String getConfigOptionLabel(EventOptionID eventOptionID) {
		XMLTagInstance option = findOption(eventOptionID, false);
		return option != null ? option.getExplicitValue(ATTRIBUTE_LABEL) : null;
	}

	public String getConfigOptionDescription(EventOptionID eventOptionID) {
		XMLTagInstance option = findOption(eventOptionID, false);
		return option != null ? option.getExplicitValue(ATTRIBUTE_DESCRIPTION) : null;
	}

	public String getConfigOptionContentType(EventOptionID eventOptionID) {
		XMLTagInstance option = findOption(eventOptionID, false);
		return option != null ? option.getExplicitValue(ATTRIBUTE_CONTENT_TYPE) : null;
	}

	void populateOption(
		EventOptionID optionKey, IOptionDescriptor<?> serverOptionInfo, String value, boolean override) {
		XMLTagInstance configOption = findOption(optionKey, true);

		if (override || configOption.getExplicitValue(ATTRIBUTE_LABEL) == null) {
			configOption.setValue(ATTRIBUTE_LABEL, serverOptionInfo.getName());
		}
		if (override || configOption.getExplicitValue(ATTRIBUTE_DESCRIPTION) == null) {
			configOption.setValue(ATTRIBUTE_DESCRIPTION, serverOptionInfo.getDescription());
		}
		if (override || configOption.getExplicitValue(ATTRIBUTE_CONTENT_TYPE) == null) {
			configOption.setValue(ATTRIBUTE_CONTENT_TYPE,
					CommonConstraints.toMatchingContentTypeV2(serverOptionInfo.getConstraint()));
		}
		if (configOption.getContent() == null || configOption.getContent().length() == 0) {
			configOption.setContent(value);
		}

	}

	void populateEventMetadata(IEventTypeID eventTypeID, IEventTypeInfo serverEventTypeInfo, boolean override) {
		XMLTagInstance event = findEvent(eventTypeID, true);
		String configEventLabel = getEventLabel(eventTypeID);
		String configEventDescription = getEventLabel(eventTypeID);
		if (override || configEventLabel == null) {
			event.setValue(ATTRIBUTE_LABEL, serverEventTypeInfo.getName());
		}
		if (override || configEventDescription == null) {
			event.setValue(ATTRIBUTE_DESCRIPTION, serverEventTypeInfo.getDescription());
		}
	}

	void putEventInCategory(IEventTypeID eventTypeID, String[] categories) {
		XMLTagInstance categoryParent = getRoot();
		for (String category : categories) {
			categoryParent = findCategory(categoryParent, category, true);
		}
		XMLTagInstance event = findEvent(eventTypeID, false);
		if (event != null) {
			categoryParent.adopt(event);
		} else {
			event = categoryParent.create(TAG_EVENTTYPE_V2);
			event.setValue(ATTRIBUTE_NAME, eventTypeID.getFullKey());
		}
	}
}
