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
package org.openjdk.jmc.flightrecorder.writer.api;

import org.openjdk.jmc.flightrecorder.writer.RecordingImpl;
import org.openjdk.jmc.flightrecorder.writer.RecordingSettingsBuilderImpl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * A factory class to create new {@linkplain Recording} instances
 */
public final class Recordings {
	/**
	 * Create a new recording that will be automatically stored in the given file. The recording
	 * start timestamp is set to the current time and the recording will automatically initialize
	 * JDK types.
	 * 
	 * @param path
	 *            the path to the recording file
	 * @return a new {@linkplain Recording} instance
	 * @throws IOException
	 */
	public static Recording newRecording(String path) throws IOException {
		return newRecording(Paths.get(path));
	}

	/**
	 * Create a new recording that will be automatically stored in the given file.
	 * 
	 * @param path
	 *            the path to the recording file
	 * @param settingsCallback
	 *            settings callback
	 * @return a new {@linkplain Recording} instance
	 * @throws IOException
	 */
	public static Recording newRecording(String path, Consumer<RecordingSettingsBuilder> settingsCallback)
			throws IOException {
		return newRecording(Paths.get(path), settingsCallback);
	}

	/**
	 * Create a new recording that will be automatically stored in the given path. The recording
	 * start timestamp is set to the current time and the recording will automatically initialize
	 * JDK types.
	 * 
	 * @param path
	 *            the path to the recording file
	 * @return a new {@linkplain Recording} instance
	 * @throws IOException
	 */
	public static Recording newRecording(Path path) throws IOException {
		return newRecording(path.toFile());
	}

	/**
	 * Create a new recording that will be automatically stored in the given path.
	 * 
	 * @param path
	 *            the path to the recording file
	 * @param settingsCallback
	 *            settings callback
	 * @return a new {@linkplain Recording} instance
	 * @throws IOException
	 */
	public static Recording newRecording(Path path, Consumer<RecordingSettingsBuilder> settingsCallback)
			throws IOException {
		return newRecording(path.toFile(), settingsCallback);
	}

	/**
	 * Create a new recording that will be automatically stored in the given file. The recording
	 * start timestamp is set to the current time and the recording will automatically initialize
	 * JDK types.
	 * 
	 * @param path
	 *            the path to the recording file
	 * @return a new {@linkplain Recording} instance
	 * @throws IOException
	 */
	public static Recording newRecording(File path) throws IOException {
		return newRecording(new FileOutputStream(path));
	}

	/**
	 * Create a new recording that will be automatically stored in the given file.
	 * 
	 * @param path
	 *            the path to the recording file
	 * @param settingsCallback
	 *            settings callback
	 * @return a new {@linkplain Recording} instance
	 * @throws IOException
	 */
	public static Recording newRecording(File path, Consumer<RecordingSettingsBuilder> settingsCallback)
			throws IOException {
		return newRecording(new FileOutputStream(path), settingsCallback);
	}

	/**
	 * Create a new recording that will be automatically stored in the given stream. The recording
	 * start timestamp is set to the current time and the recording will automatically initialize
	 * JDK types.
	 * 
	 * @param recordingStream
	 *            recording output stream
	 * @return a new {@linkplain Recording} instance
	 */
	public static Recording newRecording(OutputStream recordingStream) {
		return newRecording(recordingStream, RecordingSettingsBuilder::withJdkTypeInitialization);
	}

	/**
	 * Create a new recording that will be automatically stored in the given stream.
	 * 
	 * @param recordingStream
	 *            recording output stream
	 * @param settingsCallback
	 *            settings callback
	 * @return a new {@linkplain Recording} instance
	 */
	public static Recording newRecording(
		OutputStream recordingStream, Consumer<RecordingSettingsBuilder> settingsCallback) {
		RecordingSettingsBuilderImpl builder = new RecordingSettingsBuilderImpl();
		if (settingsCallback != null) {
			settingsCallback.accept(builder);
		}
		return new RecordingImpl(new BufferedOutputStream(recordingStream), builder.build());
	}
}
