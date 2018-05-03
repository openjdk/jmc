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
package org.openjdk.jmc.flightrecorder.uitest;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.SortedMap;
import java.util.TreeMap;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.flightrecorder.internal.EventArray;
import org.openjdk.jmc.flightrecorder.internal.FlightRecordingLoader;

@SuppressWarnings("restriction")
public class JfrMetadataToolkit {

	protected static SortedMap<String, SortedMap<String, String>> parseRecordingFile(File recordingFile) {
		SortedMap<String, SortedMap<String, String>> eventTypeMap = new TreeMap<>();
		InputStream stream = null;
		try {
			stream = IOToolkit.openUncompressedStream(recordingFile);
			EventArray[] eventArrays = FlightRecordingLoader.loadStream(stream, false, false);
			for (EventArray entry : eventArrays) {
				SortedMap<String, String> attrs = new TreeMap<>();
				for (IAccessorKey<?> a : entry.getType().getAccessorKeys().keySet()) {
					attrs.put(a.getIdentifier(), a.getContentType().getIdentifier());
				}
				String eventTypeId = entry.getType().getIdentifier();
				eventTypeMap.put(eventTypeId, attrs);
			}
		} catch (Exception e) {
			IOToolkit.closeSilently(stream);
			throw new RuntimeException(e);
		}
		return eventTypeMap;
	}

	protected static void writeMap(SortedMap<String, SortedMap<String, String>> map, PrintStream ps) {
		for (String event : map.keySet()) {
			StringBuffer sb = new StringBuffer(event);
			for (String attribute : map.get(event).keySet()) {
				sb.append(MetadataTestBase.ATTR_DELIMITER + attribute + MetadataTestBase.ATTR_SEPARATOR
						+ map.get(event).get(attribute));
			}
			ps.print(sb + "\n");
		}
	}

	/**
	 * Run as standalone java program to be able to easily extract JFR metadata from a recording,
	 * for use in manual analysis.
	 *
	 * @param args
	 *            Program args, first arg should be a recording file
	 */
	public static void main(String[] args) {
		if (args.length > 0) {
			File recordingFile = new File(args[0]);
			SortedMap<String, SortedMap<String, String>> eventTypeMap = parseRecordingFile(recordingFile);
			writeMap(eventTypeMap, System.out);
		} else {
			System.out.println("Usage: java " + JfrMetadataToolkit.class.getSimpleName() + " <jfr file>");
		}
	}
}
