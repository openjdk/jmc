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
package org.openjdk.jmc.ui.ai;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.eclipse.swt.widgets.Shell;

/**
 * Extension point interface for AI providers. Implementations are registered via the
 * {@code org.openjdk.jmc.ui.ai.aiProvider} extension point.
 */
public interface IAIProvider {

	/**
	 * @return unique identifier for this provider
	 */
	String getId();

	/**
	 * @return human-readable name for display in the UI
	 */
	String getDisplayName();

	/**
	 * @return {@code true} if the provider has been configured (e.g. API key set)
	 */
	boolean isConfigured();

	/**
	 * Opens configuration UI for this provider (e.g. API key dialog, browser login).
	 *
	 * @param shell
	 *            the parent shell for any dialogs
	 */
	void configure(Shell shell);

	/**
	 * Returns the list of known model identifiers for this provider. If the provider is configured,
	 * this may dynamically fetch models from the API; otherwise a static fallback list is returned.
	 *
	 * @return list of available model identifiers
	 */
	List<String> getAvailableModels();

	/**
	 * @return the preference key used to store the selected model for this provider
	 */
	String getModelPreferenceKey();

	/**
	 * Sends a message to the AI provider with streaming response and tool calling support. All
	 * response events (tokens, tool calls, status, completion, errors) are delivered via the
	 * {@link AIStreamHandler}. The provider handles the tool-call loop internally.
	 *
	 * @param history
	 *            the conversation history
	 * @param tools
	 *            available tools the model can invoke
	 * @param toolExecutor
	 *            function that executes a tool call and returns the result string
	 * @param handler
	 *            event handler for streaming response events
	 * @return a future that can be used to cancel the request
	 */
	CompletableFuture<Void> sendMessageStreaming(
		List<ChatMessage> history, List<IAITool> tools, Function<ToolCall, String> toolExecutor,
		AIStreamHandler handler);
}
