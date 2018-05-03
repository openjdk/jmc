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

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;

/**
 * Interface to expose additional information of an event type, like a human readable name. This
 * should not include implementation details.
 */
public interface IEventTypeInfo extends IDescribable {
	/**
	 * The persistable identifier for the event type that this instance contains information about.
	 */
	IEventTypeID getEventTypeID();

	/**
	 * A human readable categorization for this event type. (This does not include the event type
	 * itself.) It may be an empty array, but never {@code null}.
	 */
	String[] getHierarchicalCategory();

	/**
	 * A human readable label for this event type.
	 */
	@Override
	String getName();

	/**
	 * A human readable description for this event type. May be {@code null}.
	 */
	@Override
	String getDescription();

	/**
	 * Get the names and constraints of the parameters accepted by this event type.
	 */
	Map<String, ? extends IOptionDescriptor<?>> getOptionDescriptors();

	/**
	 * Get info about any option with the given key.
	 *
	 * @param optionKey
	 *            an unqualified option key, such as from {@link EventOptionID#getOptionKey()}
	 * @return option info or {@code null}
	 */
	IOptionDescriptor<?> getOptionInfo(String optionKey);
}
