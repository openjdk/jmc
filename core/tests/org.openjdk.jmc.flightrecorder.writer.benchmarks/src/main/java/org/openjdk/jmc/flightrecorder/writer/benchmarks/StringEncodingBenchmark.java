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
package org.openjdk.jmc.flightrecorder.writer.benchmarks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmc.flightrecorder.writer.api.Recording;
import org.openjdk.jmc.flightrecorder.writer.api.Recordings;
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.Types;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmark for UTF-8 string encoding performance.
 * <p>
 * Measures the impact of repeated string encoding, which is a hotspot identified in the analysis.
 * This benchmark validates the effectiveness of UTF-8 caching optimizations.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
public class StringEncodingBenchmark {

	private Recording recording;
	private Type eventType;
	private Path tempFile;

	// Common strings that would benefit from caching
	private static final String COMMON_STRING_1 = "common.event.type";
	private static final String COMMON_STRING_2 = "java.lang.Thread";
	private static final String COMMON_STRING_3 = "org.example.MyClass";
	private static final String COMMON_STRING_4 = "/usr/local/bin/java";

	// Unique strings to test uncached path
	private int counter = 0;

	@Setup(Level.Trial)
	public void setup() throws Exception {
		tempFile = Files.createTempFile("jfr-bench-string-", ".jfr");
		recording = Recordings.newRecording(tempFile);

		eventType = recording.registerEventType("bench.StringEvent", builder -> {
			builder.addField("str1", Types.Builtin.STRING).addField("str2", Types.Builtin.STRING)
					.addField("str3", Types.Builtin.STRING).addField("str4", Types.Builtin.STRING);
		});
	}

	@TearDown(Level.Trial)
	public void teardown() throws Exception {
		if (recording != null) {
			recording.close();
		}
		if (tempFile != null) {
			Files.deleteIfExists(tempFile);
		}
	}

	@Benchmark
	public void encodeRepeatedStrings() throws Exception {
		// Test caching effectiveness - same strings repeatedly encoded
		recording.writeEvent(eventType.asValue(builder -> {
			builder.putField("str1", COMMON_STRING_1).putField("str2", COMMON_STRING_2)
					.putField("str3", COMMON_STRING_3).putField("str4", COMMON_STRING_4);
		}));
	}

	@Benchmark
	public void encodeUniqueStrings() throws Exception {
		// Test uncached path - unique strings each time
		recording.writeEvent(eventType.asValue(builder -> {
			builder.putField("str1", "unique-string-" + counter++).putField("str2", "another-unique-" + counter++)
					.putField("str3", "yet-another-" + counter++).putField("str4", "final-unique-" + counter++);
		}));
	}

	@Benchmark
	public void encodeMixedStrings() throws Exception {
		// Test mix of cached and uncached
		recording.writeEvent(eventType.asValue(builder -> {
			builder.putField("str1", COMMON_STRING_1).putField("str2", "unique-" + counter++)
					.putField("str3", COMMON_STRING_3).putField("str4", "another-unique-" + counter++);
		}));
	}

	@Benchmark
	public void encodeUtf8Strings() throws Exception {
		// Test UTF-8 encoding with multi-byte characters
		recording.writeEvent(eventType.asValue(builder -> {
			builder.putField("str1", "Hello 世界").putField("str2", "Привет мир").putField("str3", "مرحبا بالعالم")
					.putField("str4", "🌍🌎🌏");
		}));
	}
}
