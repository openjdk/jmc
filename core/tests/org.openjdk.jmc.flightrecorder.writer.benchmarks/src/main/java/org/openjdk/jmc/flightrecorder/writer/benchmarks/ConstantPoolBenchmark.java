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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmark for constant pool buildup performance.
 * <p>
 * Measures the performance of constant pool operations, including HashMap growth, value
 * deduplication, and lookup performance. This validates optimizations around HashMap initial
 * capacity and hash computation.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class ConstantPoolBenchmark {

	@Param({"100", "500", "1000"})
	private int poolSize;

	private Recording recording;
	private Type eventType;
	private Path tempFile;

	@Setup(Level.Trial)
	public void setup() throws Exception {
		tempFile = Files.createTempFile("jfr-bench-pool-", ".jfr");
		recording = Recordings.newRecording(tempFile);

		eventType = recording.registerEventType("bench.PoolTest", builder -> {
			builder.addField("str1", Types.Builtin.STRING).addField("str2", Types.Builtin.STRING).addField("value",
					Types.Builtin.LONG);
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
	public void buildConstantPoolWithUniqueStrings() throws Exception {
		// Populate constant pool with unique strings
		// This tests HashMap growth and rehashing behavior
		for (int i = 0; i < poolSize; i++) {
			final int index = i;
			recording.writeEvent(eventType.asValue(builder -> {
				builder.putField("str1", "unique-string-" + index).putField("str2", "another-unique-" + index)
						.putField("value", (long) index);
			}));
		}
	}

	@Benchmark
	public void buildConstantPoolWithRepeatedStrings() throws Exception {
		// Populate with repeated strings to test deduplication
		// This tests constant pool lookup performance
		for (int i = 0; i < poolSize; i++) {
			final int index = i;
			recording.writeEvent(eventType.asValue(builder -> {
				builder.putField("str1", "common-string-" + (index % 10))
						.putField("str2", "another-common-" + (index % 20)).putField("value", (long) index);
			}));
		}
	}

	@Benchmark
	public void buildConstantPoolMixed() throws Exception {
		// Mix of unique and repeated strings
		for (int i = 0; i < poolSize; i++) {
			final int index = i;
			final boolean useCommon = index % 3 == 0;
			recording.writeEvent(eventType.asValue(builder -> {
				if (useCommon) {
					builder.putField("str1", "common-" + (index % 50)).putField("str2", "shared-" + (index % 30))
							.putField("value", (long) index);
				} else {
					builder.putField("str1", "unique-" + index).putField("str2", "solo-" + index).putField("value",
							(long) index);
				}
			}));
		}
	}
}
