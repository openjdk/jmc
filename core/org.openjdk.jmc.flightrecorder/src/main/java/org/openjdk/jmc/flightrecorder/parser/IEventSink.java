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
 * Event sinks are responsible for receiving and optionally modifying event data.
 * <p>
 * IEventSink implementations are permitted to be non thread safe.
 * <p>
 * See {@link org.openjdk.jmc.flightrecorder.parser the package documentation} for a longer
 * discussion on how parser extensions work.
 */
public interface IEventSink {

	/**
	 * Add a new event to the sink for processing. The sink may modify the event values as it sees
	 * fit.
	 * <p>
	 * The implementation should have one or more subsinks created during the
	 * {@link IEventSinkFactory#create(String, String, String[], String, java.util.List)
	 * IEventSinkFactory.create} call. Call addEvent on a subsink to continue the processing of the
	 * event. Note that the passed on value array must match the data structure used by the subsink.
	 * <p>
	 * If no {@code addEvent} call is made to a subsink, then the event will be effectively filtered
	 * out.
	 * <p>
	 * {@code addEvent} calls to subsinks may be delayed until later calls of this method or in an
	 * implementation specific flush method that can be called by {@link IEventSinkFactory#flush()
	 * IEventSinkFactory.flush}.
	 *
	 * @param values
	 *            Event values. The order and data type of the values must match the
	 *            {@code dataStructure} parameter to the
	 *            {@link IEventSinkFactory#create(String, String, String[], String, java.util.List)
	 *            IEventSinkFactory.create} call.
	 */
	void addEvent(Object[] values);
}
