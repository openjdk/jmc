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
package org.openjdk.jmc.flightrecorder.parser;

import java.util.List;

/**
 * Event sink factories are responsible for handling event types during parsing.
 * <p>
 * See {@link org.openjdk.jmc.flightrecorder.parser the package documentation} for a longer
 * discussion on how parser extensions work.
 */
public interface IEventSinkFactory {

	/**
	 * Create a new event sink for an event type. This method will be called one or more times for
	 * every event type present in the Flight Recording.
	 * <p>
	 * The implementation should have a subfactory that has been saved during instantiation with the
	 * {@link IParserExtension#getEventSinkFactory(IEventSinkFactory)
	 * IParserExtension.getEventSinkFactory} call.
	 * <p>
	 * The create call takes event type metadata which can optionally be modified before calling the
	 * create method of the subfactory to get a subsink. Note that {@code create} may be called
	 * multiple times on the subfactory to set up a case where a single input event type can be
	 * split into multiple output event types.
	 * <p>
	 * The returned event sink is used to receive and optionally modify event data. This data is
	 * then passed on to the subsink.
	 * <p>
	 * Implementations of this method must be thread safe.
	 *
	 * @param identifier
	 *            event type ID
	 * @param label
	 *            human readable name for the event type
	 * @param category
	 *            a category path for the event type
	 * @param description
	 *            human readable description of the event type
	 * @param dataStructure
	 *            metadata for the event fields
	 * @return an event sink
	 */
	IEventSink create(
		String identifier, String label, String[] category, String description, List<ValueField> dataStructure);

	/**
	 * Called when all events have been sent to the event sinks. Make sure that all pending events
	 * have been processed before returning from this call.
	 * <p>
	 * The implementation should have a subfactory. {@code flush} must be called on that just before
	 * returning.
	 * <p>
	 * If no flush operations need to be done, then simply call flush on the subfactory.
	 */
	// FIXME: The subfactory flushing should perhaps be handled outside of this interface
	void flush();
}
