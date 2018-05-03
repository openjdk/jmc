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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.management.openmbean.CompositeData;

import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.flightrecorder.configuration.OptionInfo;
import org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV2;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.services.jfr.IEventTypeInfo;

/**
 * Provides information about an event type for JFR 2.0.
 */
public final class EventTypeMetadataV2 implements IEventTypeInfo {
	private static final String JFR_SETTINGS_PERIOD = "com.oracle.jfr.settings.Period"; //$NON-NLS-1$
	private final Long id;
	private final EventTypeIDV2 eventTypeID;
	private final String label;
	private final String description;
	private final String[] category;
	private final Map<String, OptionInfo<?>> optionInfoByKey;

	@SuppressWarnings("nls")
	private static OptionInfo<?> optionInfoFrom(CompositeData data) {
		String label = (String) data.get("label");
		String description = (String) data.get("description");
		String contentType = (String) data.get("contentType");
		String defaultValue = (String) data.get("defaultValue");

		final String jfrPkg = "jdk.jfr.";
		final String jfrPeriod = jfrPkg + "Period";
		final String jfrFlag = jfrPkg + "Flag";
		if (contentType == null) {
			/*
			 * This is the best way JFR currently wants to communicate which "content type" this is.
			 * Doing this separately from the below check to skip the printout
			 */
			String typeName = (String) data.get("typeName");
			if (JFR_SETTINGS_PERIOD.equals(typeName)) {
				contentType = jfrPeriod;
			}
			if (contentType != null) {
				FlightRecorderServiceV2.LOGGER.fine("Inferred content type '" + contentType + "' for option " + label);
			}
		}
		if (contentType == null) {
			// FIXME: Remove this, as something similar already is in CommonConstraints.
			// Patch for beta builds of JFR.next without content types for everything. (Hopefully fixed by release.)
			String key = (String) data.get("name");
			if ("period".equals(key)) {
				contentType = jfrPeriod;
			} else if ("true".equals(defaultValue) || "false".equals(defaultValue)) {
				contentType = jfrFlag;
			}
			if (contentType != null) {
				FlightRecorderServiceV2.LOGGER
						.warning("Inferred content type '" + contentType + "' for option " + label);
			}
		}
		IConstraint<?> constraint = CommonConstraints.forContentTypeV2(contentType, defaultValue);
		return optionInfoFrom(label, description, constraint, contentType, defaultValue);
	}

	private static <T> OptionInfo<T> optionInfoFrom(
		String label, String description, IConstraint<T> constraint, String jdkContentType,
		String defaultPersistedValue) {
		T defaultValue;
		try {
			defaultValue = constraint.parsePersisted(defaultPersistedValue);
		} catch (QuantityConversionException e) {
			// Should not happen here. If it does, it has already been detected and then T can only be String.
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, e.getMessage(), e);
			@SuppressWarnings("unchecked")
			T strDef = (T) Messages.EventTypeMetadataV2_BAD_DEFAULT_VALUE;
			defaultValue = strDef;
		}
		return new OptionInfo<>(label, description, constraint, defaultValue);
	}

	@SuppressWarnings("nls")
	static EventTypeMetadataV2 from(CompositeData data) {
		Long id = (Long) data.get("id");
		String name = (String) data.get("name");
		String label = (String) data.get("label");
		String description = (String) data.get("description");
		String[] category = (String[]) data.get("categoryNames");

		CompositeData[] settings = (CompositeData[]) data.get("settingDescriptors");
		EventTypeIDV2 eventTypeID = new EventTypeIDV2(name);
		Map<String, OptionInfo<?>> infoMap = new HashMap<>(settings.length);
		for (CompositeData setting : settings) {
			String key = (String) setting.get("name");
			OptionInfo<?> info = optionInfoFrom(setting);
			infoMap.put(key, info);
		}
		return new EventTypeMetadataV2(id, eventTypeID, label, description, category, infoMap);
	}

	EventTypeMetadataV2(Long id, EventTypeIDV2 eventTypeID, String label, String description, String[] category,
			Map<String, OptionInfo<?>> optionInfoMap) {
		this.id = id;
		this.eventTypeID = eventTypeID;
		this.label = label;
		this.description = description;
		this.category = category;
		optionInfoByKey = Collections.unmodifiableMap(optionInfoMap);
	}

	public Long getId() {
		return id;
	}

	@Override
	public String[] getHierarchicalCategory() {
		return category;
	}

	@Override
	public EventTypeIDV2 getEventTypeID() {
		return eventTypeID;
	}

	@Override
	public String getName() {
		return label;
	}

	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Get the names and constraints of the parameters accepted by this event type.
	 */
	@Override
	public Map<String, OptionInfo<?>> getOptionDescriptors() {
		return optionInfoByKey;
	}

	@Override
	public IOptionDescriptor<?> getOptionInfo(String optionKey) {
		return optionInfoByKey.get(optionKey);
	}

	@Override
	@SuppressWarnings("nls")
	public String toString() {
		return "EventTypeMetadataV2 [type=" + eventTypeID + ", id=" + id + ", category=" + String.join(" / ", category)
				+ ", label=" + label + ", options=" + optionInfoByKey.keySet() + ']';
	}
}
