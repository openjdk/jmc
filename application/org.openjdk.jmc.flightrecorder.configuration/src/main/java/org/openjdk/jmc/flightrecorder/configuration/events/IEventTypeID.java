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
package org.openjdk.jmc.flightrecorder.configuration.events;

/**
 * Abstraction of a persistable identifier for an event type in JFR.
 */
public interface IEventTypeID {

	/**
	 * The identifying key of the producer of this event type. This is only relevant for some JFR
	 * versions. For all other versions, this is {@code null}. The format of the key is JFR version
	 * specific.
	 *
	 * @return a producer key or {@code null}
	 */
	String getProducerKey();

	/**
	 * The persistable key identifying this event type, within its {@link #getProducerKey()
	 * producer}, if applicable. The format of the key is JFR version specific.
	 *
	 * @return a event type key, never {@code null}
	 */
	String getRelativeKey();

	/**
	 * A string array representing some sort of categorization to be used only when the
	 * corresponding {@link IEventTypeInfo} is unavailable. Note that, unlike in
	 * {@link IEventTypeInfo#getHierarchicalCategory()}, the last element in the array represents
	 * the event type itself.
	 *
	 * @return
	 */
	String[] getFallbackHierarchy();

	/**
	 * The persistable key uniquely identifying this event type. The format of the key is JFR
	 * version specific.
	 *
	 * @return a qualified event type key, never {@code null}
	 */
	String getFullKey();

	/**
	 * The persistable key uniquely identifying the event option specified by {@code optionKey} in
	 * this event type. The format of the key is JFR version specific.
	 *
	 * @return a qualified event option key, never {@code null}
	 */
	String getFullKey(String optionKey);
}
