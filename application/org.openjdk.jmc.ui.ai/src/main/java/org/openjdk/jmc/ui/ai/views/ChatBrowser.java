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
package org.openjdk.jmc.ui.ai.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.openjdk.jmc.ui.ai.AIPlugin;
import org.openjdk.jmc.ui.ai.preferences.PreferenceConstants;
import org.openjdk.jmc.ui.common.util.ThemeUtils;

/**
 * Wraps an SWT Browser widget for rendering chat messages with markdown support. Handles batched
 * token streaming via Display.asyncExec coalescing.
 */
public class ChatBrowser {

	private static final String TEMPLATE_PATH = "/org/openjdk/jmc/ui/ai/views/chat.html"; //$NON-NLS-1$

	private final Browser browser;
	private boolean ready;
	private final StringBuilder pendingTokens = new StringBuilder();
	private boolean flushScheduled;
	private IPropertyChangeListener colorListener;

	public ChatBrowser(Composite parent) {
		browser = new Browser(parent, SWT.EDGE);
		browser.addProgressListener(new ProgressAdapter() {
			@Override
			public void completed(ProgressEvent event) {
				ready = true;
			}
		});
		loadTemplate();

		colorListener = event -> {
			String prop = event.getProperty();
			if (prop.startsWith("color.light.") || prop.startsWith("color.dark.")) { //$NON-NLS-1$ //$NON-NLS-2$
				browser.getDisplay().asyncExec(() -> {
					if (!browser.isDisposed()) {
						pushColors();
					}
				});
			}
		};
		AIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(colorListener);
		browser.addDisposeListener(e -> {
			AIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(colorListener);
		});
	}

	public Browser getControl() {
		return browser;
	}

	public boolean isDisposed() {
		return browser.isDisposed();
	}

	public void addUserMessage(String text) {
		if (ready && !browser.isDisposed()) {
			browser.execute("addUserMessage('" + escapeJS(text) + "');"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void startAssistantMessage(String senderName) {
		if (ready && !browser.isDisposed()) {
			browser.execute("startAssistantMessage('" + escapeJS(senderName) + "');"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void appendToken(String token) {
		synchronized (pendingTokens) {
			pendingTokens.append(token);
			if (!flushScheduled && !browser.isDisposed()) {
				flushScheduled = true;
				Display display = browser.getDisplay();
				display.asyncExec(this::flushTokens);
			}
		}
	}

	public void finishAssistantMessage() {
		flushTokensNow();
		if (ready && !browser.isDisposed()) {
			browser.execute("finishAssistantMessage();"); //$NON-NLS-1$
		}
	}

	public void addToolUse(String toolName, String detail) {
		flushTokensNow();
		if (ready && !browser.isDisposed()) {
			browser.execute("addToolUse('" + escapeJS(toolName) + "','" + escapeJS(detail) + "');"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	public void addErrorMessage(String text) {
		if (ready && !browser.isDisposed()) {
			browser.execute("addErrorMessage('" + escapeJS(text) + "');"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void flushTokens() {
		String batch;
		synchronized (pendingTokens) {
			batch = pendingTokens.toString();
			pendingTokens.setLength(0);
			flushScheduled = false;
		}
		if (!batch.isEmpty() && ready && !browser.isDisposed()) {
			browser.execute("appendToken('" + escapeJS(batch) + "');"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void flushTokensNow() {
		if (browser.getDisplay().getThread() == Thread.currentThread()) {
			flushTokens();
		}
	}

	private void loadTemplate() {
		String html = readTemplate();
		boolean dark = ThemeUtils.isDarkTheme();
		html = html.replace("${themeClass}", dark ? "dark" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		html = html.replace("${userColor}", getColorHex(dark, PreferenceConstants.P_COLOR_USER_DARK, //$NON-NLS-1$
				PreferenceConstants.P_COLOR_USER_LIGHT));
		html = html.replace("${assistantColor}", getColorHex(dark, PreferenceConstants.P_COLOR_ASSISTANT_DARK, //$NON-NLS-1$
				PreferenceConstants.P_COLOR_ASSISTANT_LIGHT));
		html = html.replace("${toolColor}", getColorHex(dark, PreferenceConstants.P_COLOR_TOOL_DARK, //$NON-NLS-1$
				PreferenceConstants.P_COLOR_TOOL_LIGHT));
		html = html.replace("${errorColor}", getColorHex(dark, PreferenceConstants.P_COLOR_ERROR_DARK, //$NON-NLS-1$
				PreferenceConstants.P_COLOR_ERROR_LIGHT));
		browser.setText(html, true);
	}

	private void pushColors() {
		if (!ready || browser.isDisposed()) {
			return;
		}
		boolean dark = ThemeUtils.isDarkTheme();
		String js = "var s=document.documentElement.style;" //$NON-NLS-1$
				+ "s.setProperty('--user-color','" //$NON-NLS-1$
				+ getColorHex(dark, PreferenceConstants.P_COLOR_USER_DARK, PreferenceConstants.P_COLOR_USER_LIGHT)
				+ "');" //$NON-NLS-1$
				+ "s.setProperty('--assistant-color','" //$NON-NLS-1$
				+ getColorHex(dark, PreferenceConstants.P_COLOR_ASSISTANT_DARK,
						PreferenceConstants.P_COLOR_ASSISTANT_LIGHT)
				+ "');" //$NON-NLS-1$
				+ "s.setProperty('--tool-color','" //$NON-NLS-1$
				+ getColorHex(dark, PreferenceConstants.P_COLOR_TOOL_DARK, PreferenceConstants.P_COLOR_TOOL_LIGHT)
				+ "');" //$NON-NLS-1$
				+ "s.setProperty('--error-color','" //$NON-NLS-1$
				+ getColorHex(dark, PreferenceConstants.P_COLOR_ERROR_DARK, PreferenceConstants.P_COLOR_ERROR_LIGHT)
				+ "');"; //$NON-NLS-1$
		browser.execute(js);
	}

	private static String getColorHex(boolean dark, String darkKey, String lightKey) {
		IPreferenceStore store = AIPlugin.getDefault().getPreferenceStore();
		String key = dark ? darkKey : lightKey;
		String rgb = store.getString(key);
		if (rgb == null || rgb.isEmpty()) {
			// Fall back to default value
			rgb = store.getDefaultString(key);
		}
		return rgbToHex(rgb);
	}

	static String rgbToHex(String rgb) {
		if (rgb != null && !rgb.isEmpty()) {
			String[] parts = rgb.split(","); //$NON-NLS-1$
			if (parts.length == 3) {
				try {
					int r = Integer.parseInt(parts[0].trim());
					int g = Integer.parseInt(parts[1].trim());
					int b = Integer.parseInt(parts[2].trim());
					return String.format("#%02x%02x%02x", r, g, b); //$NON-NLS-1$
				} catch (NumberFormatException e) {
					// fall through
				}
			}
		}
		return "#000000"; //$NON-NLS-1$
	}

	private String readTemplate() {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(getClass().getResourceAsStream(TEMPLATE_PATH), StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
			return sb.toString();
		} catch (IOException e) {
			AIPlugin.getLogger().log(Level.WARNING, "Failed to load chat template", e); //$NON-NLS-1$
			return "<html><body><div id='chat'></div></body></html>"; //$NON-NLS-1$
		}
	}

	static String escapeJS(String text) {
		if (text == null) {
			return ""; //$NON-NLS-1$
		}
		return text.replace("\\", "\\\\") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("'", "\\'") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\n", "\\n") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\r", "\\r") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\t", "\\t"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
