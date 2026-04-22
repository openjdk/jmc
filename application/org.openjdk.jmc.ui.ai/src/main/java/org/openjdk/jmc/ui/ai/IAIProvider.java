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
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Extension point interface for AI providers. Implementations are registered via the
 * {@code org.openjdk.jmc.ui.ai.aiProvider} extension point.
 */
public interface IAIProvider {

	String AI_PREFERENCE_PAGE_ID = "org.openjdk.jmc.ui.ai.preferences.AIPreferencePage"; //$NON-NLS-1$

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
	 * Opens the AI preferences page so the user can configure this provider.
	 *
	 * @param shell
	 *            the parent shell for any dialogs
	 */
	default void configure(Shell shell) {
		PreferencesUtil.createPreferenceDialogOn(shell, AI_PREFERENCE_PAGE_ID, null, null).open();
	}

	/**
	 * Returns the list of known model identifiers for this provider. If the provider is configured,
	 * this may dynamically fetch models from the API; otherwise a static fallback list is returned.
	 *
	 * @return list of available model identifiers
	 */
	List<String> getAvailableModels();

	/**
	 * Creates provider-specific preference field editors in the AI preferences page. The
	 * {@code fieldAdder} consumer must be called for each {@link FieldEditor} so that the
	 * preference page can manage their lifecycle. Additional SWT widgets (e.g. buttons) may be
	 * added directly to {@code parent}.
	 *
	 * @param parent
	 *            the composite to add controls to (uses a 3-column GridLayout)
	 * @param fieldAdder
	 *            consumer that registers a field editor with the preference page
	 */
	default void createPreferenceFields(Composite parent, Consumer<FieldEditor> fieldAdder) {
	}

	/**
	 * Initializes default preference values for this provider in the shared preference store.
	 *
	 * @param store
	 *            the preference store to initialize
	 */
	default void initializeDefaultPreferences(IPreferenceStore store) {
	}

	/**
	 * Converts {@link #getAvailableModels()} into the two-column array required by
	 * {@link org.eclipse.jface.preference.ComboFieldEditor}.
	 */
	default String[][] buildModelEntries() {
		List<String> models = getAvailableModels();
		String[][] entries = new String[models.size()][2];
		for (int i = 0; i < models.size(); i++) {
			entries[i][0] = models.get(i);
			entries[i][1] = models.get(i);
		}
		return entries;
	}

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

	/**
	 * Releases resources held by this provider (e.g. HTTP client, thread pool). Called once when
	 * the plugin is stopped.
	 */
	default void dispose() {
	}
}
