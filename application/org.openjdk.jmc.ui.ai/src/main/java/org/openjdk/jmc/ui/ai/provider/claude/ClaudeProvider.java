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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.openjdk.jmc.common.security.ISecurityManager;
import org.openjdk.jmc.common.security.SecurityException;
import org.openjdk.jmc.common.security.SecurityManagerFactory;
import org.openjdk.jmc.ui.ai.AIPlugin;
import org.openjdk.jmc.ui.ai.AIStreamHandler;
import org.openjdk.jmc.ui.ai.ChatMessage;
import org.openjdk.jmc.ui.ai.IAIProvider;
import org.openjdk.jmc.ui.ai.IAITool;
import org.openjdk.jmc.ui.ai.ToolCall;
import org.openjdk.jmc.ui.ai.preferences.Messages;
import org.openjdk.jmc.ui.ai.preferences.SecureApiKeyFieldEditor;

public class ClaudeProvider implements IAIProvider {

	static final String P_API_KEY = "claude.apiKey"; //$NON-NLS-1$
	static final String P_MODEL = "claude.model"; //$NON-NLS-1$
	static final String P_API_URL = "claude.apiUrl"; //$NON-NLS-1$
	static final String DEFAULT_MODEL = "claude-sonnet-4-20250514"; //$NON-NLS-1$
	static final String DEFAULT_API_URL = "https://api.anthropic.com"; //$NON-NLS-1$

	private static final String ID = "org.openjdk.jmc.ui.ai.provider.claude"; //$NON-NLS-1$
	private static final String DISPLAY_NAME = "Claude (Anthropic)"; //$NON-NLS-1$
	private static final String CONSOLE_URL = "https://console.anthropic.com/settings/keys"; //$NON-NLS-1$
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
		return P_MODEL;
	}

	@Override
	public List<String> getAvailableModels() {
		String apiKey = getApiKey();
		if (!apiKey.isEmpty()) {
			IPreferenceStore store = AIPlugin.getDefault().getPreferenceStore();
			String apiUrl = store.getString(P_API_URL);
			List<String> fetched = apiClient.fetchModels(apiUrl, apiKey);
			if (!fetched.isEmpty()) {
				return fetched;
			}
		}
		return MODELS;
	}

	@Override
	public void createPreferenceFields(Composite parent, Consumer<FieldEditor> fieldAdder) {
		fieldAdder.accept(new SecureApiKeyFieldEditor(P_API_KEY, Messages.AIPreferencePage_API_KEY, parent));
		fieldAdder.accept(new ComboFieldEditor(P_MODEL, Messages.AIPreferencePage_MODEL,
				MODELS.stream().map(m -> new String[] {m, m}).toArray(String[][]::new), parent));
		fieldAdder.accept(new StringFieldEditor(P_API_URL, Messages.AIPreferencePage_API_URL, parent));
		Button button = new Button(parent, SWT.PUSH);
		button.setText(Messages.AIPreferencePage_GET_API_KEY);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser()
							.openURL(new URI(CONSOLE_URL).toURL());
				} catch (Exception ex) {
					AIPlugin.getLogger().warning("Failed to open browser: " + ex.getMessage()); //$NON-NLS-1$
				}
			}
		});
	}

	@Override
	public void initializeDefaultPreferences(IPreferenceStore store) {
		store.setDefault(P_MODEL, DEFAULT_MODEL);
		store.setDefault(P_API_URL, DEFAULT_API_URL);
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
		String model = store.getString(P_MODEL);
		String apiUrl = store.getString(P_API_URL);

		return apiClient.sendStreaming(apiUrl, apiKey, model, history, tools, toolExecutor, handler);
	}

	@Override
	public void dispose() {
		apiClient.close();
	}

	private String getApiKey() {
		try {
			ISecurityManager sm = SecurityManagerFactory.getSecurityManager();
			if (sm != null && sm.hasKey(P_API_KEY)) {
				Object val = sm.get(P_API_KEY);
				if (val instanceof String[]) {
					String[] arr = (String[]) val;
					return arr.length > 0 && arr[0] != null ? arr[0] : ""; //$NON-NLS-1$
				}
			}
		} catch (SecurityException e) {
			AIPlugin.getLogger().log(Level.WARNING, "Failed to load Claude API key from secure store", e); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}
}
