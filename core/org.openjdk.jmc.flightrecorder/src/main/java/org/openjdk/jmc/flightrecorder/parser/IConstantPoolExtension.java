/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2025, Datadog, Inc. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.parser;

import java.util.Map;

import org.openjdk.jmc.common.collection.FastAccessNumberMap;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.IParserStats;

/**
 * Interface for Flight Recorder constant pool extensions. Implementation are created by
 * {@link IParserExtension#createConstantPoolExtension()} each time a recording is starting to be
 * parsed Provides callbacks for constant pools reads, referencing, simple resolution, full
 * resolution and parsing is finished
 */
public interface IConstantPoolExtension {

	/**
	 * @return id of the extension, by default the simple class name. This id will be exposed in the
	 *         Map returned by {@link IParserStats#getConstantPoolExtensions()}
	 */
	default String getId() {
		return getClass().getSimpleName();
	}

	/**
	 * Called when a constant is read from the Metadata to put into the constant pool.
	 * 
	 * @param constantIndex
	 *            index inside the metadata that is used to reference from other places inside the
	 *            recording.
	 * @param constant
	 *            actual value of the constant.
	 * @param eventTypeId
	 *            type id of the constant pool.
	 * @return actual value of the constant. Could be a new value to be replaced by.
	 */
	default Object constantRead(long constantIndex, Object constant, String eventTypeId) {
		return constant;
	}

	/**
	 * Called when a referenced constant is read from another constant pool or actual recording
	 * events.
	 * 
	 * @param constant
	 *            actual constant value.
	 * @param poolName
	 *            name of the constant pool.
	 * @param eventTypeId
	 *            type id if the event referencing the constant.
	 * @return actual value of the constant. Could be a new value to be replaced by.
	 */
	default Object constantReferenced(Object constant, String poolName, String eventTypeId) {
		return constant;
	}

	/**
	 * Called when a referenced constant is resolved from another pool or actual recording events.
	 * 
	 * @param constant
	 *            actual constant value.
	 * @param poolName
	 *            name of the constant pool.
	 * @param eventTypeId
	 *            type id if the event referencing the constant.
	 * @return actual value of the constant. Could be a new value to be replaced by.
	 */
	default Object constantResolved(Object constant, String poolName, String eventTypeId) {
		return constant;
	}

	/**
	 * Called when all constant pools are resolved.
	 * 
	 * @param constantPools
	 *            map of all constant pools by name.
	 */
	default void allConstantPoolsResolved(Map<String, FastAccessNumberMap<Object>> constantPools) {

	}

	/**
	 * Called when all events are loaded (end of parsing)
	 */
	default void eventsLoaded() {

	}

	/**
	 * @return collection of items built by the extension
	 */
	default IItemCollection getItemCollection() {
		return null;
	}
}
