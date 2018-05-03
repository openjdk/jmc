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

/**
 * Interface for Flight Recorder parser extensions. Implementations are normally stateless and their
 * prime responsibility is to create and link {@link IEventSinkFactory} instances.
 * <p>
 * See {@link org.openjdk.jmc.flightrecorder.parser the package documentation} for a longer
 * discussion on how parser extensions work.
 */
public interface IParserExtension {

	/**
	 * Override the value interpretation. If not overridden the values types are automatically
	 * selected from the read metadata. This is only used for a special case in Mission Control code
	 * so normally {@code null} should be returned.
	 * 
	 * @param eventTypeId
	 *            event type ID to get interpretation for
	 * @param fieldId
	 *            field ID within the event type to get interpretation for
	 * @return the identifier of the value interpretation or {@code null} to use the default
	 *         interpretation
	 */
	String getValueInterpretation(String eventTypeId, String fieldId);

	/**
	 * Get a new event sink factory for use during the reading of one Flight Recording.
	 * <p>
	 * Note that it is the implementor's responsibility to make sure that the subfactory is used by
	 * the event sink factory. If the
	 * {@link IEventSinkFactory#create(String, String, String[], String, java.util.List)
	 * IEventSinkFactory.create} call is not chained to the subfactory, then events will be lost.
	 * 
	 * @param subFactory
	 *            Subfactory to nest. Events created by the returned factory will normally be passed
	 *            on to sinks created by the nested subfactory.
	 * @return a new event sink factory
	 */
	IEventSinkFactory getEventSinkFactory(IEventSinkFactory subFactory);
}
