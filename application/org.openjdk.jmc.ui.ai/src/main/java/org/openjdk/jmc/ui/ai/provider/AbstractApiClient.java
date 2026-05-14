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
package org.openjdk.jmc.ui.ai.provider;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractApiClient {

	protected static final int MAX_TOOL_ROUNDS = 25;
	protected static final Pattern ERROR_MESSAGE_PATTERN = Pattern
			.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$

	private final HttpClient httpClient;
	private final ExecutorService executor;

	protected AbstractApiClient() {
		httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
		executor = Executors.newVirtualThreadPerTaskExecutor();
	}

	protected HttpClient httpClient() {
		return httpClient;
	}

	protected ExecutorService executor() {
		return executor;
	}

	public void close() {
		executor.shutdown();
		httpClient.close();
	}

	/**
	 * Extracts a JSON string value for the given key from a single SSE data line. Returns
	 * {@code null} if the key is absent, the value is JSON {@code null}, the value is not a string,
	 * or the string is unterminated (malformed input).
	 */
	protected String extractJsonString(String key, String json) {
		int searchFrom = 0;
		int keyIdx;
		while (true) {
			keyIdx = json.indexOf(key, searchFrom);
			if (keyIdx < 0) {
				return null;
			}
			if (isJsonKeyPosition(json, keyIdx)) {
				break;
			}
			searchFrom = keyIdx + 1;
		}
		int colonIdx = json.indexOf(':', keyIdx + key.length());
		if (colonIdx < 0) {
			return null;
		}
		int i = colonIdx + 1;
		while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
			i++;
		}
		if (i >= json.length()) {
			return null;
		}
		if (json.charAt(i) == 'n') { // JSON null
			return null;
		}
		if (json.charAt(i) != '"') {
			return null;
		}
		i++; // skip opening quote
		StringBuilder result = new StringBuilder();
		for (; i < json.length(); i++) {
			char c = json.charAt(i);
			if (c == '\\' && i + 1 < json.length()) {
				char next = json.charAt(i + 1);
				switch (next) {
				case '"':
					result.append('"');
					break;
				case '\\':
					result.append('\\');
					break;
				case 'n':
					result.append('\n');
					break;
				case 'r':
					result.append('\r');
					break;
				case 't':
					result.append('\t');
					break;
				default:
					result.append('\\').append(next);
				}
				i++;
			} else if (c == '"') {
				return result.toString();
			} else {
				result.append(c);
			}
		}
		return null; // unterminated string — do not return partial content
	}

	private static boolean isJsonKeyPosition(String json, int keyIdx) {
		int i = keyIdx - 1;
		while (i >= 0 && Character.isWhitespace(json.charAt(i))) {
			i--;
		}
		if (i < 0) {
			return true;
		}
		char c = json.charAt(i);
		return c == '{' || c == ',';
	}

	protected String extractErrorMessage(String body) {
		Matcher matcher = ERROR_MESSAGE_PATTERN.matcher(body);
		if (matcher.find()) {
			return unescapeJson(matcher.group(1));
		}
		return body.length() > 200 ? body.substring(0, 200) : body;
	}

	public static String escapeJson(String text) {
		if (text == null) {
			return ""; //$NON-NLS-1$
		}
		return text.replace("\\", "\\\\") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\"", "\\\"") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\n", "\\n") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\r", "\\r") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\t", "\\t"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static String unescapeJson(String text) {
		if (text == null || text.indexOf('\\') < 0) {
			return text;
		}
		StringBuilder sb = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\\' && i + 1 < text.length()) {
				char next = text.charAt(++i);
				switch (next) {
				case '"':
					sb.append('"');
					break;
				case '\\':
					sb.append('\\');
					break;
				case 'n':
					sb.append('\n');
					break;
				case 'r':
					sb.append('\r');
					break;
				case 't':
					sb.append('\t');
					break;
				default:
					sb.append('\\').append(next);
					break;
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
