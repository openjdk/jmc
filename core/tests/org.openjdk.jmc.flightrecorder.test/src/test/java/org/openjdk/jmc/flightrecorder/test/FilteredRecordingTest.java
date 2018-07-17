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
package org.openjdk.jmc.flightrecorder.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.test.io.IOResourceSet;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.parser.IParserExtension;
import org.openjdk.jmc.flightrecorder.parser.ParserExtensionRegistry;
import org.openjdk.jmc.flightrecorder.parser.filter.FilterExtension;
import org.openjdk.jmc.flightrecorder.parser.filter.IOnLoadFilter;
import org.openjdk.jmc.flightrecorder.parser.filter.OnLoadFilters;
import org.openjdk.jmc.flightrecorder.test.util.PrintoutsToolkit;

/**
 * Test that checks that a recording can be filtered when loaded.
 */
@SuppressWarnings("nls")
public class FilteredRecordingTest {
	private static final String JVM_INFORMATION_REGEXP = ".*JVMInformation";
	private static final String COM_ORACLE_JDK_JVM_INFORMATION = "jdk.JVMInformation";

	@Test
	public void testIncludeEventTypeFilter() throws IOException, CouldNotLoadRecordingException {
		checkFilter(OnLoadFilters.includeEvents(Arrays.asList(COM_ORACLE_JDK_JVM_INFORMATION)),
				COM_ORACLE_JDK_JVM_INFORMATION, true,
				"Expected all event types except '" + COM_ORACLE_JDK_JVM_INFORMATION + "'");
	}

	@Test
	public void testExcludeEventTypeFilter() throws IOException, CouldNotLoadRecordingException {
		checkFilter(OnLoadFilters.excludeEvents(Arrays.asList(COM_ORACLE_JDK_JVM_INFORMATION)),
				COM_ORACLE_JDK_JVM_INFORMATION, false, "Expected event type '" + COM_ORACLE_JDK_JVM_INFORMATION + "'");
	}

	@Test
	public void testIncludeRegexpFilter() throws IOException, CouldNotLoadRecordingException {
		checkFilter(OnLoadFilters.includeEvents(Pattern.compile(JVM_INFORMATION_REGEXP)),
				COM_ORACLE_JDK_JVM_INFORMATION, true,
				"Expected all event types except those matching '" + JVM_INFORMATION_REGEXP + "'");
	}

	@Test
	public void testExcludeRegexpFilter() throws IOException, CouldNotLoadRecordingException {
		checkFilter(OnLoadFilters.excludeEvents(Pattern.compile(JVM_INFORMATION_REGEXP)),
				COM_ORACLE_JDK_JVM_INFORMATION, false,
				"Expected event types matching '" + JVM_INFORMATION_REGEXP + "'");
	}

	private void checkFilter(
		IOnLoadFilter onLoadFilter, String typeToCheck, boolean expect, String unexpectedAfterFilterString)
			throws IOException, CouldNotLoadRecordingException, AssertionError {
		for (IOResourceSet resourceSet : PrintoutsToolkit.getTestResources()) {
			try (InputStream recordingStream = resourceSet.getResource(0).open()) {
				List<IParserExtension> extensions = new ArrayList<>(ParserExtensionRegistry.getParserExtensions());
				extensions.add(new FilterExtension(onLoadFilter));
				IItemCollection items = JfrLoaderToolkit.loadEvents(recordingStream, extensions);
				Assert.assertTrue("Expected some events to pass through the filter", items.hasItems());
				for (IItemIterable ii : items) {
					if (expect != ii.getType().getIdentifier().equals(typeToCheck)) {
						Assert.fail(unexpectedAfterFilterString
								+ " to be filtered from recording, but the following event type was included: '"
								+ ii.getType().getIdentifier() + "'");
					}
				}
			} catch (AssertionError ae) {
				throw new AssertionError(ae.getMessage() + " (Recording: " + resourceSet.getResource(0).getName() + ")",
						ae);
			}
		}
	}

}
