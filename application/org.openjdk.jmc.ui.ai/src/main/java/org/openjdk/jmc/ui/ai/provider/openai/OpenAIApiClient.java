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
package org.openjdk.jmc.ui.ai.provider.openai;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.openjdk.jmc.ui.ai.provider.AbstractApiClient;

public class OpenAIApiClient extends AbstractApiClient {

	private static final Logger LOGGER = Logger.getLogger(OpenAIApiClient.class.getName());
	private static final Pattern MODEL_ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern TOOL_CALL_INDEX_PATTERN = Pattern.compile("\"index\"\\s*:\\s*(\\d+)"); //$NON-NLS-1$

	public List<String> fetchModels(String apiUrl, String apiKey) {
		List<String> models = new ArrayList<>();
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl + "/v1/models")) //$NON-NLS-1$
					.header("Authorization", "Bearer " + apiKey) //$NON-NLS-1$ //$NON-NLS-2$
					.GET().build();
			HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				Matcher matcher = MODEL_ID_PATTERN.matcher(response.body());
				while (matcher.find()) {
					String id = matcher.group(1);
					if (id.startsWith("gpt-") || id.startsWith("o")) { //$NON-NLS-1$ //$NON-NLS-2$
						models.add(id);
					}
				}
				models.sort(String::compareTo);
			}
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "Failed to fetch OpenAI models", e); //$NON-NLS-1$
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
		}, executor());
	}

	private void runStreamingToolLoop(
		String apiUrl, String apiKey, String model, List<ChatMessage> history, List<IAITool> tools,
		Function<ToolCall, String> toolExecutor, AIStreamHandler handler) throws Exception {

		String toolsJson = buildToolsJson(tools);
		List<String> apiMessages = buildApiMessages(history);

		for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
			String requestBody = buildRequestBody(model, apiMessages, toolsJson);
			int estimatedTokens = requestBody.length() / 4;
			handler.onStatus(round == 0 ? "Sending request (~" + estimatedTokens + " tokens)..." //$NON-NLS-1$ //$NON-NLS-2$
					: "Tool round " + (round + 1) + " (~" + estimatedTokens + " tokens)..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl + "/v1/chat/completions")) //$NON-NLS-1$
					.header("Content-Type", "application/json") //$NON-NLS-1$ //$NON-NLS-2$
					.header("Authorization", "Bearer " + apiKey) //$NON-NLS-1$ //$NON-NLS-2$
					.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();

			HttpResponse<java.util.stream.Stream<String>> response = httpClient().send(request,
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

			if (state.finishReason != null && state.finishReason.equals("tool_calls")) { //$NON-NLS-1$
				apiMessages.add(state.buildAssistantMessage());

				for (Map.Entry<Integer, ToolCallAccumulator> entry : state.toolCalls.entrySet()) {
					ToolCallAccumulator tc = entry.getValue();
					String args = tc.arguments.toString();
					handler.onToolCallStart(tc.name, args);
					String result = toolExecutor.apply(new ToolCall(tc.id, tc.name, args));
					handler.onToolCallComplete(tc.name, result);
					apiMessages.add("{\"role\":\"tool\",\"tool_call_id\":\"" + escapeJson(tc.id) //$NON-NLS-1$
							+ "\",\"content\":\"" + escapeJson(result) + "\"}"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			} else {
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
		if ("[DONE]".equals(data)) { //$NON-NLS-1$
			return;
		}

		if (data.contains("\"delta\"") && data.contains("\"content\"")) { //$NON-NLS-1$ //$NON-NLS-2$
			String content = extractJsonString("\"content\"", data); //$NON-NLS-1$
			if (content != null && data.indexOf("\"delta\"") < data.indexOf("\"content\"")) { //$NON-NLS-1$ //$NON-NLS-2$
				handler.onToken(content);
				state.textContent.append(content);
			}
		}

		if (data.contains("\"tool_calls\"")) { //$NON-NLS-1$
			parseToolCallDelta(data, state);
		}

		if (data.contains("\"finish_reason\"")) { //$NON-NLS-1$
			String reason = extractJsonString("\"finish_reason\"", data); //$NON-NLS-1$
			if (reason != null) {
				state.finishReason = reason;
			}
		}
	}

	private void parseToolCallDelta(String data, StreamState state) {
		Matcher indexMatcher = TOOL_CALL_INDEX_PATTERN.matcher(data);
		if (!indexMatcher.find()) {
			return;
		}
		int index = Integer.parseInt(indexMatcher.group(1));

		ToolCallAccumulator tc = state.toolCalls.computeIfAbsent(index, k -> new ToolCallAccumulator());

		String id = extractJsonString("\"id\"", data); //$NON-NLS-1$
		if (id != null && id.startsWith("call_")) { //$NON-NLS-1$
			tc.id = id;
		}

		if (data.contains("\"function\"") && data.contains("\"name\"")) { //$NON-NLS-1$ //$NON-NLS-2$
			String name = extractJsonString("\"name\"", data); //$NON-NLS-1$
			if (name != null) {
				tc.name = name;
			}
		}

		if (data.contains("\"arguments\"")) { //$NON-NLS-1$
			String args = extractJsonString("\"arguments\"", data); //$NON-NLS-1$
			if (args != null) {
				tc.arguments.append(args);
			}
		}
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
			sb.append("{\"type\":\"function\",\"function\":{"); //$NON-NLS-1$
			sb.append("\"name\":\"").append(escapeJson(tool.getName())).append("\","); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("\"description\":\"").append(escapeJson(tool.getDescription())).append("\","); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("\"parameters\":").append(tool.getParameterSchema()); //$NON-NLS-1$
			sb.append("}}"); //$NON-NLS-1$
		}
		sb.append("]"); //$NON-NLS-1$
		return sb.toString();
	}

	private List<String> buildApiMessages(List<ChatMessage> history) {
		List<String> messages = new ArrayList<>();
		for (ChatMessage msg : history) {
			String role;
			switch (msg.getRole()) {
			case USER:
				role = "user"; //$NON-NLS-1$
				break;
			case ASSISTANT:
				role = "assistant"; //$NON-NLS-1$
				break;
			case SYSTEM:
				role = "system"; //$NON-NLS-1$
				break;
			default:
				role = "user"; //$NON-NLS-1$
			}
			messages.add("{\"role\":\"" + role + "\",\"content\":\"" + escapeJson(msg.getContent()) + "\"}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return messages;
	}

	private String buildRequestBody(String model, List<String> apiMessages, String toolsJson) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"model\":\"").append(escapeJson(model)).append("\","); //$NON-NLS-1$ //$NON-NLS-2$
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
		if (toolsJson != null) {
			sb.append(",\"tools\":").append(toolsJson); //$NON-NLS-1$
		}
		sb.append("}"); //$NON-NLS-1$
		return sb.toString();
	}

	private static class ToolCallAccumulator {
		String id;
		String name;
		final StringBuilder arguments = new StringBuilder();
	}

	private static class StreamState {
		String finishReason;
		final Map<Integer, ToolCallAccumulator> toolCalls = new HashMap<>();
		final StringBuilder textContent = new StringBuilder();

		String buildAssistantMessage() {
			StringBuilder sb = new StringBuilder();
			sb.append("{\"role\":\"assistant\",\"content\":null,\"tool_calls\":["); //$NON-NLS-1$
			boolean first = true;
			for (Map.Entry<Integer, ToolCallAccumulator> entry : toolCalls.entrySet()) {
				if (!first) {
					sb.append(","); //$NON-NLS-1$
				}
				first = false;
				ToolCallAccumulator tc = entry.getValue();
				sb.append("{\"id\":\"").append(AbstractApiClient.escapeJson(tc.id)); //$NON-NLS-1$
				sb.append("\",\"type\":\"function\",\"function\":{\"name\":\"") //$NON-NLS-1$
						.append(AbstractApiClient.escapeJson(tc.name));
				sb.append("\",\"arguments\":\"").append(AbstractApiClient.escapeJson(tc.arguments.toString())); //$NON-NLS-1$
				sb.append("\"}}"); //$NON-NLS-1$
			}
			sb.append("]}"); //$NON-NLS-1$
			return sb.toString();
		}
	}
}
