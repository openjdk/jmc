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
package org.openjdk.jmc.flightrecorder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.internal.EventArray;
import org.openjdk.jmc.flightrecorder.internal.FlightRecordingLoader;
import org.openjdk.jmc.flightrecorder.parser.IParserExtension;
import org.openjdk.jmc.flightrecorder.parser.ParserExtensionRegistry;

/**
 * A Java 1.7 compatible collection of methods used to load binary JFR data into
 * {@link IItemCollection} implementations.
 */
public class JfrLoaderToolkit {

	/**
	 * @param files
	 *            the files to read the recording from
	 * @param extensions
	 *            the extensions to use when parsing the file
	 * @return an array of EventArrays (one event type per EventArray)
	 */
	private static EventArray[] loadFile(List<File> files, List<? extends IParserExtension> extensions)
			throws IOException, CouldNotLoadRecordingException {
		List<InputStream> streams = new ArrayList<>(files.size());
		for (File file : files) {
			streams.add(IOToolkit.openUncompressedStream(file));
		}
		InputStream stream = new SequenceInputStream(Collections.enumeration(streams));
		try {
			return FlightRecordingLoader.loadStream(stream, extensions, false, true);
		} finally {
			IOToolkit.closeSilently(stream);
		}
	}

	/**
	 * Loads a potentially zipped or gzipped input stream using the parser extensions loaded from
	 * the java service loader
	 *
	 * @param stream
	 *            the input stream to read the recording from
	 * @return the events in the recording
	 */
	public static IItemCollection loadEvents(InputStream stream) throws IOException, CouldNotLoadRecordingException {
		return loadEvents(stream, ParserExtensionRegistry.getParserExtensions());
	}

	/**
	 * Loads a potentially zipped or gzipped input stream using the parser extensions loaded from
	 * the java service loader
	 *
	 * @param stream
	 *            the input stream to read the recording from
	 * @param extensions
	 *            the extensions to use when parsing the file
	 * @return the events in the recording
	 */
	public static IItemCollection loadEvents(InputStream stream, List<? extends IParserExtension> extensions)
			throws IOException, CouldNotLoadRecordingException {
		InputStream in = IOToolkit.openUncompressedStream(stream);
		return EventCollection.build(FlightRecordingLoader.loadStream(in, extensions, false, true));
	}

	/**
	 * Loads a potentially zipped or gzipped file using the parser extensions loaded from the java
	 * service loader
	 *
	 * @param file
	 *            the file to read the recording from
	 * @return the events in the recording
	 */
	public static IItemCollection loadEvents(File file) throws IOException, CouldNotLoadRecordingException {
		List<File> files = new ArrayList<>();
		files.add(file);
		return loadEvents(files);
	}

	/**
	 * Loads a recording from a sequence of potentially zipped or gzipped files using the parser
	 * extensions loaded from the java service loader
	 *
	 * @param files
	 *            the files to read the recording from
	 * @return the events in the recording
	 */
	public static IItemCollection loadEvents(List<File> files) throws IOException, CouldNotLoadRecordingException {
		return loadEvents(files, ParserExtensionRegistry.getParserExtensions());
	}

	/**
	 * Loads a recording from a sequence of potentially zipped or gzipped file using the supplied
	 * parser extensions
	 *
	 * @param files
	 *            the files to read the recording from
	 * @param extensions
	 *            the extensions to use when parsing the file
	 * @return the events in the recording
	 */
	public static IItemCollection loadEvents(List<File> files, List<? extends IParserExtension> extensions)
			throws IOException, CouldNotLoadRecordingException {
		return EventCollection.build(loadFile(files, extensions));
	}

}
