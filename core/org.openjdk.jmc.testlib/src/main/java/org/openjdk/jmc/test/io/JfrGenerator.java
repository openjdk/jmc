/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.test.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.TimeZone;

public final class JfrGenerator {

	private JfrGenerator() {
	}

	public static SourceStage create() {
		return new SourceStage();
	}

	public static final class SourceStage {
		private SourceStage() {
		}

		/**
		 * Provide source code as a string for a valid <a href="https://openjdk.org/jeps/330">JEP
		 * 330</a> single-file source-code program, which will be profiled using JFR.
		 */
		public ConfigurationStage source(String source) {
			return new ConfigurationStage(Objects.requireNonNull(source, "Source is required"));
		}
	}

	public static final class ConfigurationStage {
		private final String source;

		private ConfigurationStage(String source) {
			this.source = source;
		}

		/**
		 * Name of a default configuration, for example {@code default} and {@code profile} are
		 * included in most OpenJDK builds.
		 */
		public ExecutionStage configurationName(String configuration) {
			return new ExecutionStage(source, Objects.requireNonNull(configuration, "Configuration is required"), null);
		}

		/**
		 * Contents of a JFR configuration file.
		 */
		public ExecutionStage configuration(String configuration) {
			return new ExecutionStage(source, null, Objects.requireNonNull(configuration, "Configuration is required"));
		}
	}

	public static final class ExecutionStage {

		private final String source;
		private final String configurationName;
		private final String configurationContents;

		private ExecutionStage(String source, String configurationName, String configurationContents) {
			this.source = source;
			this.configurationName = configurationName;
			this.configurationContents = configurationContents;
		}

		public void execute(RecordingConsumer consumer) throws IOException {
			Objects.requireNonNull(consumer, "Consumer is required");
			Path temp = Files.createTempDirectory("jfr-generator").toAbsolutePath();
			Path sourcePath = temp.resolve("Source.java");
			Path configPath = temp.resolve("custom-config.jfc");
			Path jfrPath = temp.resolve("recording.jfr");
			try {
				Files.writeString(sourcePath, source, StandardCharsets.UTF_8);
				if (configurationContents != null) {
					Files.writeString(configPath, configurationContents, StandardCharsets.UTF_8);
				}
				String osName = System.getProperty("os.name");
				boolean isWindows = osName != null && osName.startsWith("Windows");
				Path javaBinary = Paths.get(System.getProperty("java.home")).toAbsolutePath().resolve("bin")
						.resolve(isWindows ? "java.exe" : "java");
				Process process = new ProcessBuilder().directory(temp.toFile()).inheritIO().command(
						javaBinary.toString(),
						String.format("-XX:StartFlightRecording=settings=%s,filename=%s,dumponexit=true",
								configurationName != null ? configurationName : configPath.toAbsolutePath(), jfrPath),
						// Bound resource usage.
						// We may want to make the JVM args configurable later on.
						"-Xmx64m", "-Xms64m",
						// Match current JVM timezone
						"-Duser.timezone=" + TimeZone.getDefault().getID(), sourcePath.toString()).start();
				int exitStatus = process.waitFor();
				if (exitStatus != 0) {
					throw new RuntimeException("Process failed to exit successfully. Status: " + exitStatus);
				}
				if (!Files.isRegularFile(jfrPath)) {
					throw new RuntimeException("Failed to create a JFR");
				}
				consumer.acceptRecording(jfrPath);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				Files.deleteIfExists(sourcePath);
				Files.deleteIfExists(jfrPath);
				Files.deleteIfExists(configPath);
				Files.delete(temp);
			}
		}
	}

	@FunctionalInterface
	public interface RecordingConsumer {
		void acceptRecording(Path recording) throws IOException;
	}
}
