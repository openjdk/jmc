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
package org.openjdk.jmc.ui.ai.provider.claude;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.ui.ai.AIStreamHandler;
import org.openjdk.jmc.ui.ai.ChatMessage;
import org.openjdk.jmc.ui.ai.IAITool;
import org.openjdk.jmc.ui.ai.ToolCall;

public class ClaudeApiClient {

	private static final Logger LOGGER = Logger.getLogger(ClaudeApiClient.class.getName());
	private static final String ANTHROPIC_VERSION = "2023-06-01"; //$NON-NLS-1$
	private static final int MAX_TOOL_ROUNDS = 25;
	private static final Pattern MODEL_ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern ERROR_MESSAGE_PATTERN = Pattern
			.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$

	private final HttpClient httpClient;

	public ClaudeApiClient() {
		httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
	}

	public List<String> fetchModels(String apiUrl, String apiKey) {
		List<String> models = new ArrayList<>();
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl + "/v1/models")) //$NON-NLS-1$
					.header("x-api-key", apiKey) //$NON-NLS-1$
					.header("anthropic-version", ANTHROPIC_VERSION) //$NON-NLS-1$
					.GET().build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				Matcher matcher = MODEL_ID_PATTERN.matcher(response.body());
				while (matcher.find()) {
					String id = matcher.group(1);
					if (id.startsWith("claude-")) { //$NON-NLS-1$
						models.add(id);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "Failed to fetch Claude models", e); //$NON-NLS-1$
		}
		return models;
	}

	public CompletableFuture<Void> sendStreaming(
		String apiUrl, String apiKey, String model, List<ChatMessage> history, List<IAITool> tools,
		Function<ToolCall, String> toolExecutor, AIStreamHandler handler) {

		return CompletableFuture.runAsync(() -> {
			try {
				runStreamingToolLoop(apiUrl, apiKey, model, history, tools, toolExecutor, handler);
				handler.onComplete();
			} catch (Exception e) {
				handler.onError(e);
			}
		});
	}

	private void runStreamingToolLoop(
		String apiUrl, String apiKey, String model, List<ChatMessage> history, List<IAITool> tools,
		Function<ToolCall, String> toolExecutor, AIStreamHandler handler) throws Exception {

		String toolsJson = buildToolsJson(tools);
		List<String> apiMessages = buildApiMessages(history);

		for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
			String requestBody = buildRequestBody(model, apiMessages, history, toolsJson);
			int estimatedTokens = requestBody.length() / 4;
			handler.onStatus(round == 0 ? "Sending request (~" + estimatedTokens + " tokens)..." //$NON-NLS-1$ //$NON-NLS-2$
					: "Tool round " + (round + 1) + " (~" + estimatedTokens + " tokens)..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl + "/v1/messages")) //$NON-NLS-1$
					.header("content-type", "application/json") //$NON-NLS-1$ //$NON-NLS-2$
					.header("x-api-key", apiKey) //$NON-NLS-1$
					.header("anthropic-version", ANTHROPIC_VERSION) //$NON-NLS-1$
					.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();

			HttpResponse<java.util.stream.Stream<String>> response = httpClient.send(request,
					HttpResponse.BodyHandlers.ofLines());

			StreamState state = new StreamState();
			try (java.util.stream.Stream<String> body = response.body()) {
				if (response.statusCode() != 200) {
					StringBuilder errorBody = new StringBuilder();
					body.forEach(errorBody::append);
					throw new Exception("API error " + response.statusCode() + ": " //$NON-NLS-1$ //$NON-NLS-2$
							+ extractErrorMessage(errorBody.toString()));
				}
				body.forEach(line -> processSSELine(line, state, handler));
			}

			if (state.stopReason != null && state.stopReason.equals("tool_use")) { //$NON-NLS-1$
				// Build assistant message with all content blocks for the API history
				apiMessages.add(state.buildAssistantMessage());

				// Execute tool calls
				StringBuilder toolResultMsg = new StringBuilder();
				toolResultMsg.append("{\"role\":\"user\",\"content\":["); //$NON-NLS-1$
				boolean first = true;
				for (ToolCallAccumulator tc : state.toolCalls) {
					String args = tc.inputJson.toString();
					handler.onToolCallStart(tc.name, args);
					String result = toolExecutor.apply(new ToolCall(tc.id, tc.name, args));
					handler.onToolCallComplete(tc.name, result.length());
					if (!first) {
						toolResultMsg.append(","); //$NON-NLS-1$
					}
					first = false;
					toolResultMsg.append("{\"type\":\"tool_result\",\"tool_use_id\":\"") //$NON-NLS-1$
							.append(escapeJson(tc.id)).append("\",\"content\":\"") //$NON-NLS-1$
							.append(escapeJson(result)).append("\"}"); //$NON-NLS-1$
				}
				toolResultMsg.append("]}"); //$NON-NLS-1$
				apiMessages.add(toolResultMsg.toString());
			} else {
				// Final response - text was already streamed via onToken
				return;
			}
		}
		handler.onToken("\n[Max tool call rounds reached]"); //$NON-NLS-1$
	}

	private void processSSELine(String line, StreamState state, AIStreamHandler handler) {
		if (!line.startsWith("data: ")) { //$NON-NLS-1$
			return;
		}
		String data = line.substring(6);

		if (data.contains("\"content_block_start\"")) { //$NON-NLS-1$
			if (data.contains("\"tool_use\"")) { //$NON-NLS-1$
				// Tool use block starting
				ToolCallAccumulator tc = new ToolCallAccumulator();
				tc.id = extractJsonString("\"id\"", data); //$NON-NLS-1$
				tc.name = extractJsonString("\"name\"", data); //$NON-NLS-1$
				state.currentToolCall = tc;
				state.toolCalls.add(tc);
				state.currentBlockType = "tool_use"; //$NON-NLS-1$
			} else if (data.contains("\"text\"")) { //$NON-NLS-1$
				state.currentBlockType = "text"; //$NON-NLS-1$
			}
		} else if (data.contains("\"content_block_delta\"")) { //$NON-NLS-1$
			if (data.contains("\"text_delta\"")) { //$NON-NLS-1$
				String text = extractJsonString("\"text\"", data); //$NON-NLS-1$
				if (text != null) {
					handler.onToken(text);
					state.textContent.append(text);
				}
			} else if (data.contains("\"input_json_delta\"")) { //$NON-NLS-1$
				String partial = extractJsonString("\"partial_json\"", data); //$NON-NLS-1$
				if (partial != null && state.currentToolCall != null) {
					state.currentToolCall.inputJson.append(partial);
				}
			}
		} else if (data.contains("\"content_block_stop\"")) { //$NON-NLS-1$
			state.currentBlockType = null;
			state.currentToolCall = null;
		} else if (data.contains("\"message_delta\"")) { //$NON-NLS-1$
			String stopReason = extractJsonString("\"stop_reason\"", data); //$NON-NLS-1$
			if (stopReason != null) {
				state.stopReason = stopReason;
			}
		}
	}

	private String extractJsonString(String key, String json) {
		// Find key:"value" pattern
		int keyIdx = json.indexOf(key);
		if (keyIdx < 0) {
			return null;
		}
		int colonIdx = json.indexOf(':', keyIdx + key.length());
		if (colonIdx < 0) {
			return null;
		}
		// Find the opening quote
		int start = json.indexOf('"', colonIdx + 1);
		if (start < 0) {
			return null;
		}
		start++; // skip the quote
		// Find closing quote, handling escapes
		StringBuilder result = new StringBuilder();
		for (int i = start; i < json.length(); i++) {
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
		return result.toString();
	}

	private String buildToolsJson(List<IAITool> tools) {
		if (tools == null || tools.isEmpty()) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("["); //$NON-NLS-1$
		boolean first = true;
		for (IAITool tool : tools) {
			if (!first) {
				sb.append(","); //$NON-NLS-1$
			}
			first = false;
			sb.append("{\"name\":\"").append(escapeJson(tool.getName())).append("\","); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("\"description\":\"").append(escapeJson(tool.getDescription())).append("\","); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("\"input_schema\":").append(tool.getParameterSchema()).append("}"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sb.append("]"); //$NON-NLS-1$
		return sb.toString();
	}

	private List<String> buildApiMessages(List<ChatMessage> history) {
		List<String> messages = new ArrayList<>();
		for (ChatMessage msg : history) {
			if (msg.getRole() == ChatMessage.Role.SYSTEM) {
				continue;
			}
			String role = msg.getRole() == ChatMessage.Role.USER ? "user" : "assistant"; //$NON-NLS-1$ //$NON-NLS-2$
			messages.add("{\"role\":\"" + role + "\",\"content\":\"" + escapeJson(msg.getContent()) + "\"}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return messages;
	}

	private String buildRequestBody(
		String model, List<String> apiMessages, List<ChatMessage> history, String toolsJson) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"model\":\"").append(escapeJson(model)).append("\","); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("\"max_tokens\":4096,"); //$NON-NLS-1$
		sb.append("\"stream\":true,"); //$NON-NLS-1$
		sb.append("\"messages\":["); //$NON-NLS-1$
		boolean first = true;
		for (String msg : apiMessages) {
			if (!first) {
				sb.append(","); //$NON-NLS-1$
			}
			first = false;
			sb.append(msg);
		}
		sb.append("]"); //$NON-NLS-1$

		for (ChatMessage msg : history) {
			if (msg.getRole() == ChatMessage.Role.SYSTEM) {
				sb.append(",\"system\":\"").append(escapeJson(msg.getContent())).append("\""); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			}
		}

		if (toolsJson != null) {
			sb.append(",\"tools\":").append(toolsJson); //$NON-NLS-1$
		}

		sb.append("}"); //$NON-NLS-1$
		return sb.toString();
	}

	private String extractErrorMessage(String body) {
		Matcher matcher = ERROR_MESSAGE_PATTERN.matcher(body);
		if (matcher.find()) {
			return unescapeJson(matcher.group(1));
		}
		return body.length() > 200 ? body.substring(0, 200) : body;
	}

	static String escapeJson(String text) {
		if (text == null) {
			return ""; //$NON-NLS-1$
		}
		return text.replace("\\", "\\\\") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\"", "\\\"") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\n", "\\n") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\r", "\\r") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\t", "\\t"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	static String unescapeJson(String text) {
		return text.replace("\\n", "\n") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\\r", "\r") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\\t", "\t") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\\\"", "\"") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\\\\", "\\"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static class ToolCallAccumulator {
		String id;
		String name;
		final StringBuilder inputJson = new StringBuilder();
	}

	private static class StreamState {
		String stopReason;
		String currentBlockType;
		ToolCallAccumulator currentToolCall;
		final List<ToolCallAccumulator> toolCalls = new ArrayList<>();
		final StringBuilder textContent = new StringBuilder();

		String buildAssistantMessage() {
			StringBuilder sb = new StringBuilder();
			sb.append("{\"role\":\"assistant\",\"content\":["); //$NON-NLS-1$
			boolean first = true;
			if (textContent.length() > 0) {
				sb.append("{\"type\":\"text\",\"text\":\"").append(escapeJson(textContent.toString())).append("\"}"); //$NON-NLS-1$ //$NON-NLS-2$
				first = false;
			}
			for (ToolCallAccumulator tc : toolCalls) {
				if (!first) {
					sb.append(","); //$NON-NLS-1$
				}
				first = false;
				sb.append("{\"type\":\"tool_use\",\"id\":\"").append(escapeJson(tc.id)); //$NON-NLS-1$
				sb.append("\",\"name\":\"").append(escapeJson(tc.name)); //$NON-NLS-1$
				sb.append("\",\"input\":").append(tc.inputJson.length() > 0 ? tc.inputJson.toString() : "{}"); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("}"); //$NON-NLS-1$
			}
			sb.append("]}"); //$NON-NLS-1$
			return sb.toString();
		}
	}
}
