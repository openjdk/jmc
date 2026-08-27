/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.mcp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Sanity test for the native image binary. Drives the native executable over STDIO and checks the
 * things that native image most easily breaks: the JFR parser, which materializes structured values
 * reflectively, and the analysis rules, which are found through ServiceLoader and get their text
 * from resource bundles. A plain "does it start" check would pass even with those broken.
 * <p>
 * Skipped unless {@code native.image.path} is set, e.g.:
 *
 * <pre>
 *   mvn verify -Dnative -Dnative.image.path=target/mcp-1.0.0-SNAPSHOT-runner
 * </pre>
 */
class NativeImageSanityIT {

	private static final Path RECORDING = Paths.get("..", "core", "tests",
			"org.openjdk.jmc.flightrecorder.serializers.test", "src", "main", "resources", "recordings",
			"hotmethods.jfr");

	@Test
	@EnabledIfSystemProperty(named = "native.image.path", matches = ".+")
	void nativeBinaryListsTools() throws Exception {
		Process process = startNative();
		try {
			OutputStream stdin = process.getOutputStream();
			InputStream stdout = process.getInputStream();

			handshake(stdin, stdout);

			send(stdin, "{\"method\":\"tools/list\",\"params\":{},\"jsonrpc\":\"2.0\",\"id\":1}");
			String tools = readResponse(stdout, 15_000);
			assertNotNull(tools, "No response for tools/list");
			assertTrue(tools.contains("loadRecording"), tools);
			assertTrue(tools.contains("getRuleResults"), tools);
		} finally {
			stop(process);
		}
	}

	@Test
	@EnabledIfSystemProperty(named = "native.image.path", matches = ".+")
	void nativeBinaryParsesRecordingAndRunsRules() throws Exception {
		Assumptions.assumeTrue(Files.isReadable(RECORDING), "Test recording not available: " + RECORDING);
		Process process = startNative();
		try {
			OutputStream stdin = process.getOutputStream();
			InputStream stdout = process.getInputStream();

			handshake(stdin, stdout);

			String path = RECORDING.toAbsolutePath().normalize().toString().replace("\\", "\\\\");
			send(stdin, "{\"method\":\"tools/call\",\"params\":{\"name\":\"loadRecording\",\"arguments\":{\"path\":\""
					+ path + "\"}},\"jsonrpc\":\"2.0\",\"id\":2}");
			String loaded = readResponse(stdout, 60_000);
			assertNotNull(loaded, "No response for loadRecording");
			assertTrue(loaded.contains("Loaded recording"), loaded);
			assertTrue(loaded.contains("event types"), loaded);

			// Stack trace aggregation exercises the reflectively built frames and methods.
			send(stdin, "{\"method\":\"tools/call\",\"params\":{\"name\":\"getStackTrace\",\"arguments\":"
					+ "{\"limit\":3}},\"jsonrpc\":\"2.0\",\"id\":3}");
			String trace = readResponse(stdout, 60_000);
			assertNotNull(trace, "No response for getStackTrace");
			assertTrue(trace.contains("countIntersection"), trace);

			// Rules come from ServiceLoader, and their summaries from resource bundles.
			send(stdin, "{\"method\":\"tools/call\",\"params\":{\"name\":\"getRuleResults\",\"arguments\":"
					+ "{\"minSeverity\":\"WARNING\",\"verbose\":false}},\"jsonrpc\":\"2.0\",\"id\":4}");
			String rules = readResponse(stdout, 300_000);
			assertNotNull(rules, "No response for getRuleResults");
			assertTrue(rules.contains("MethodProfiling"), rules);
			// A populated message proves the resource bundles made it into the binary.
			assertTrue(rules.contains("most sampled method"), rules);
		} finally {
			stop(process);
		}
	}

	private Process startNative() throws Exception {
		Path binary = Path.of(System.getProperty("native.image.path"));
		assertTrue(Files.exists(binary), "Native binary not found at: " + binary);
		return new ProcessBuilder(binary.toAbsolutePath().toString(), "-Dquarkus.mcp.server.stdio.enabled=true")
				.redirectErrorStream(false).start();
	}

	private void handshake(OutputStream stdin, InputStream stdout) throws Exception {
		send(stdin, "{\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},"
				+ "\"clientInfo\":{\"name\":\"sanity-test\",\"version\":\"1.0\"}},\"jsonrpc\":\"2.0\",\"id\":0}");
		String response = readResponse(stdout, 15_000);
		assertNotNull(response, "No response for initialize");
		assertTrue(response.contains("serverInfo"), response);
		send(stdin, "{\"method\":\"notifications/initialized\",\"jsonrpc\":\"2.0\"}");
	}

	private void send(OutputStream stdin, String json) throws Exception {
		stdin.write((json + "\n").getBytes(StandardCharsets.UTF_8));
		stdin.flush();
	}

	private void stop(Process process) throws Exception {
		process.destroyForcibly();
		process.waitFor();
	}

	/**
	 * Reads a single JSON-RPC response line, with a timeout so a hung server fails the test instead
	 * of the build.
	 */
	private String readResponse(InputStream stdout, long timeoutMs) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		StringBuilder sb = new StringBuilder();
		while (System.currentTimeMillis() < deadline) {
			if (stdout.available() > 0) {
				int b = stdout.read();
				if (b == -1) {
					break;
				}
				if (b == '\n') {
					String line = sb.toString().trim();
					if (!line.isEmpty()) {
						return line;
					}
					sb.setLength(0);
				} else {
					sb.append((char) b);
				}
			} else {
				Thread.sleep(50);
			}
		}
		String remaining = sb.toString().trim();
		return remaining.isEmpty() ? null : remaining;
	}
}
