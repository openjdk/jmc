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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV1;
import org.openjdk.jmc.rjmx.services.jfr.IEventTypeInfo;

/**
 * Provides information about an event type.
 */
public final class EventTypeMetadataV1 implements IEventTypeInfo {
	private final Integer id;
	private final EventTypeIDV1 eventTypeID;
	private final String label;
	private final String description;
	private final Map<String, IOptionDescriptor<?>> optionInfoByKey;
	private String[] hierarchy;

	public EventTypeMetadataV1(Integer id, EventTypeIDV1 eventTypeID, String label, String description,
			Map<String, IOptionDescriptor<?>> optionInfoByKey) {
		this.id = id;
		this.eventTypeID = eventTypeID;
		this.label = label;
		this.description = description;
		this.optionInfoByKey = Collections.unmodifiableMap(optionInfoByKey);

		String[] fallback = eventTypeID.getFallbackHierarchy();
		if (fallback != null && fallback.length > 1) {
			hierarchy = Arrays.copyOf(fallback, fallback.length - 1);
		}
	}

	public Integer getId() {
		return id;
	}

	public String getPath() {
		return eventTypeID.getRelativeKey();
	}

	@Override
	public String[] getHierarchicalCategory() {
		return hierarchy;
	}

	@Override
	public EventTypeIDV1 getEventTypeID() {
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
	public Map<String, IOptionDescriptor<?>> getOptionDescriptors() {
		return optionInfoByKey;
	}

	@Override
	public IOptionDescriptor<?> getOptionInfo(String optionKey) {
		return optionInfoByKey.get(optionKey);
	}

	@Override
	@SuppressWarnings("nls")
	public String toString() {
		return "EventTypeMetadataV1 [type=" + eventTypeID + ", id=" + id + ", label=" + label + ", options="
				+ optionInfoByKey.keySet() + ']';
	}
}
