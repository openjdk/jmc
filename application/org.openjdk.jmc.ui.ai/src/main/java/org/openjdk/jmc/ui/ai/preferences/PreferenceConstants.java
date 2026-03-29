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
package org.openjdk.jmc.ui.ai.preferences;

public final class PreferenceConstants {

	public static final String P_CLAUDE_API_KEY = "claude.apiKey"; //$NON-NLS-1$
	public static final String P_CLAUDE_MODEL = "claude.model"; //$NON-NLS-1$
	public static final String P_CLAUDE_API_URL = "claude.apiUrl"; //$NON-NLS-1$
	public static final String P_SELECTED_PROVIDER = "selectedProvider"; //$NON-NLS-1$

	public static final String DEFAULT_CLAUDE_MODEL = "claude-sonnet-4-20250514"; //$NON-NLS-1$
	public static final String DEFAULT_CLAUDE_API_URL = "https://api.anthropic.com"; //$NON-NLS-1$

	public static final String P_OPENAI_API_KEY = "openai.apiKey"; //$NON-NLS-1$
	public static final String P_OPENAI_MODEL = "openai.model"; //$NON-NLS-1$
	public static final String P_OPENAI_API_URL = "openai.apiUrl"; //$NON-NLS-1$

	public static final String DEFAULT_OPENAI_MODEL = "o3-mini"; //$NON-NLS-1$
	public static final String DEFAULT_OPENAI_API_URL = "https://api.openai.com"; //$NON-NLS-1$

	public static final String P_COLOR_USER_LIGHT = "color.light.user"; //$NON-NLS-1$
	public static final String P_COLOR_ASSISTANT_LIGHT = "color.light.assistant"; //$NON-NLS-1$
	public static final String P_COLOR_TOOL_LIGHT = "color.light.tool"; //$NON-NLS-1$
	public static final String P_COLOR_ERROR_LIGHT = "color.light.error"; //$NON-NLS-1$

	public static final String P_COLOR_USER_DARK = "color.dark.user"; //$NON-NLS-1$
	public static final String P_COLOR_ASSISTANT_DARK = "color.dark.assistant"; //$NON-NLS-1$
	public static final String P_COLOR_TOOL_DARK = "color.dark.tool"; //$NON-NLS-1$
	public static final String P_COLOR_ERROR_DARK = "color.dark.error"; //$NON-NLS-1$

	public static final String DEFAULT_COLOR_USER_LIGHT = "0,0,0"; //$NON-NLS-1$
	public static final String DEFAULT_COLOR_ASSISTANT_LIGHT = "0,128,0"; //$NON-NLS-1$
	public static final String DEFAULT_COLOR_TOOL_LIGHT = "160,160,160"; //$NON-NLS-1$
	public static final String DEFAULT_COLOR_ERROR_LIGHT = "200,0,0"; //$NON-NLS-1$

	public static final String DEFAULT_COLOR_USER_DARK = "255,255,255"; //$NON-NLS-1$
	public static final String DEFAULT_COLOR_ASSISTANT_DARK = "80,220,80"; //$NON-NLS-1$
	public static final String DEFAULT_COLOR_TOOL_DARK = "120,120,120"; //$NON-NLS-1$
	public static final String DEFAULT_COLOR_ERROR_DARK = "255,100,100"; //$NON-NLS-1$

	private PreferenceConstants() {
	}
}
