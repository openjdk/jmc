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

/**
 * A settings data-class for a {@linkplain Recording} instance
 */
public final class RecordingSettings {
	private final long startTimestamp;
	private final long startTicks;
	private final long duration;
	private final boolean initializeJDKTypes;

	/**
	 * @param startTimestamp
	 *            the recording start timestamp in epoch nanoseconds (nanoseconds since 1970-01-01)
	 *            or -1 to use {@linkplain System#currentTimeMillis()} * 1_000_000
	 * @param startTicks
	 *            the recording start timestamp in ticks or -1 to use {@linkplain System#nanoTime()}
	 * @param duration
	 *            the recording duration in ticks or -1 to use the current
	 *            {@linkplain System#nanoTime()} to compute the diff from {@linkplain #startTicks}
	 * @param initializeJDKTypes
	 *            should the {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Types.JDK} types
	 *            be initialized
	 */
	public RecordingSettings(long startTimestamp, long startTicks, long duration, boolean initializeJDKTypes) {
		this.startTimestamp = startTimestamp;
		this.startTicks = startTicks;
		this.duration = duration;
		this.initializeJDKTypes = initializeJDKTypes;
	}

	/**
	 * @param startTimestamp
	 *            the recording start timestamp in epoch nanoseconds (nanoseconds since 1970-01-01)
	 * @param initializeJDKTypes
	 *            should the {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Types.JDK} types
	 *            be initialized
	 */
	public RecordingSettings(long startTimestamp, boolean initializeJDKTypes) {
		this(startTimestamp, -1, -1, initializeJDKTypes);
	}

	/**
	 * Recording will use current time as its start timestamp
	 * 
	 * @param initializeJDKTypes
	 *            should the {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Types.JDK} types
	 *            be initialized
	 */
	public RecordingSettings(boolean initializeJDKTypes) {
		this(-1, -1, -1, initializeJDKTypes);
	}

	/**
	 * Recording will initialize {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Types.JDK}
	 * types.
	 * 
	 * @param startTimestamp
	 *            the recording start timestamp in epoch nanoseconds (nanoseconds since 1970-01-01)
	 */
	public RecordingSettings(long startTimestamp) {
		this(startTimestamp, -1, -1, true);
	}

	/**
	 * Recording will use current time as its start timestamp and will initialize
	 * {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Types.JDK} types.
	 */
	public RecordingSettings() {
		this(-1, -1, -1, true);
	}

	/**
	 * @return recording timestamp in epoch nanoseconds (nanoseconds since 1970-01-01)
	 */
	public long getStartTimestamp() {
		return startTimestamp;
	}

	/**
	 * @return recording timestamp in ticks or -1 to use the current value of
	 *         {@linkplain System#nanoTime()}
	 */
	public long getStartTicks() {
		return startTicks;
	}

	/**
	 * @return recording duration in ticks or -1 to derive it from {@linkplain System#nanoTime()}
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * @return {@literal true} if {@linkplain org.openjdk.jmc.flightrecorder.writer.api.Types.JDK}
	 *         types are to be initialized
	 */
	public boolean shouldInitializeJDKTypes() {
		return initializeJDKTypes;
	}
}
