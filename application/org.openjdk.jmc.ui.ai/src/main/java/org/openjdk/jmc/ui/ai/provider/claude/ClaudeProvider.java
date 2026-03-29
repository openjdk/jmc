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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.openjdk.jmc.ui.ai.AIPlugin;
import org.openjdk.jmc.ui.ai.AIStreamHandler;
import org.openjdk.jmc.ui.ai.ChatMessage;
import org.openjdk.jmc.ui.ai.IAIProvider;
import org.openjdk.jmc.ui.ai.IAITool;
import org.openjdk.jmc.ui.ai.ToolCall;
import org.openjdk.jmc.ui.ai.preferences.PreferenceConstants;

public class ClaudeProvider implements IAIProvider {

	private static final String ID = "org.openjdk.jmc.ui.ai.provider.claude"; //$NON-NLS-1$
	private static final String DISPLAY_NAME = "Claude (Anthropic)"; //$NON-NLS-1$
	private static final String PREFERENCE_PAGE_ID = "org.openjdk.jmc.ui.ai.preferences.AIPreferencePage"; //$NON-NLS-1$
	private static final List<String> MODELS = List.of( //
			"claude-sonnet-4-20250514", //$NON-NLS-1$
			"claude-opus-4-20250514", //$NON-NLS-1$
			"claude-haiku-4-20250506"); //$NON-NLS-1$

	private final ClaudeApiClient apiClient = new ClaudeApiClient();

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return DISPLAY_NAME;
	}

	@Override
	public boolean isConfigured() {
		return !getApiKey().isEmpty();
	}

	@Override
	public String getModelPreferenceKey() {
		return PreferenceConstants.P_CLAUDE_MODEL;
	}

	@Override
	public List<String> getAvailableModels() {
		if (isConfigured()) {
			IPreferenceStore store = AIPlugin.getDefault().getPreferenceStore();
			String apiUrl = store.getString(PreferenceConstants.P_CLAUDE_API_URL);
			List<String> fetched = apiClient.fetchModels(apiUrl, getApiKey());
			if (!fetched.isEmpty()) {
				return fetched;
			}
		}
		return MODELS;
	}

	@Override
	public void configure(Shell shell) {
		PreferencesUtil.createPreferenceDialogOn(shell, PREFERENCE_PAGE_ID, null, null).open();
	}

	@Override
	public CompletableFuture<Void> sendMessageStreaming(
		List<ChatMessage> history, List<IAITool> tools, Function<ToolCall, String> toolExecutor,
		AIStreamHandler handler) {

		String apiKey = getApiKey();
		if (apiKey.isEmpty()) {
			handler.onError(new IllegalStateException("Claude API key is not configured")); //$NON-NLS-1$
			return CompletableFuture.completedFuture(null);
		}

		IPreferenceStore store = AIPlugin.getDefault().getPreferenceStore();
		String model = store.getString(PreferenceConstants.P_CLAUDE_MODEL);
		String apiUrl = store.getString(PreferenceConstants.P_CLAUDE_API_URL);

		return apiClient.sendStreaming(apiUrl, apiKey, model, history, tools, toolExecutor, handler);
	}

	private String getApiKey() {
		IPreferenceStore store = AIPlugin.getDefault().getPreferenceStore();
		return store.getString(PreferenceConstants.P_CLAUDE_API_KEY);
	}
}
