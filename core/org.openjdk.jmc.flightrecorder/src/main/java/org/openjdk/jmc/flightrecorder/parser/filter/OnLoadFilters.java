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
package org.openjdk.jmc.flightrecorder.parser.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility methods for creating {@link IOnLoadFilter}s
 */
public class OnLoadFilters {

	public static final IOnLoadFilter ALLOW_ALL_FILTER = new IOnLoadFilter() {

		@Override
		public boolean allowEventType(String eventId) {
			return true;
		}
	};

	/**
	 * Filter that allows a specified set of events.
	 *
	 * @param includedTypeIds
	 *            the event type ids to include
	 * @return a filter
	 */
	public static IOnLoadFilter includeEvents(Collection<String> includedTypeIds) {
		final Set<String> includedSet = new HashSet<>(includedTypeIds);
		return new IOnLoadFilter() {

			@Override
			public boolean allowEventType(String typeId) {
				return includedSet.contains(typeId);
			}
		};
	}

	/**
	 * Filter that disallows a specified set of events.
	 *
	 * @param excludedTypeIds
	 *            the event type ids to exclude
	 * @return a filter
	 */
	public static IOnLoadFilter excludeEvents(Collection<String> excludedTypeIds) {
		final Set<String> excludedSet = new HashSet<>(excludedTypeIds);
		return new IOnLoadFilter() {

			@Override
			public boolean allowEventType(String typeId) {
				return !excludedSet.contains(typeId);
			}
		};
	}

	/**
	 * Filter that allows a specified set of events.
	 *
	 * @param includeRegexp
	 *            regular expression for which event type ids to include
	 * @return a filter
	 */
	public static IOnLoadFilter includeEvents(final Pattern includeRegexp) {
		return new IOnLoadFilter() {

			@Override
			public boolean allowEventType(String typeId) {
				return includeRegexp.matcher(typeId).matches();
			}
		};
	}

	/**
	 * Filter that disallows a specified set of events.
	 *
	 * @param excludeRegexp
	 *            regular expression for which event type ids to exclude
	 * @return a filter
	 */
	public static IOnLoadFilter excludeEvents(final Pattern excludeRegexp) {
		return new IOnLoadFilter() {

			@Override
			public boolean allowEventType(String typeId) {
				return !excludeRegexp.matcher(typeId).matches();
			}
		};
	}
}
