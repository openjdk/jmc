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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openjdk.jmc.ui.ai.AIPlugin;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = AIPlugin.getDefault().getPreferenceStore();
		store.setDefault(PreferenceConstants.P_CLAUDE_MODEL, PreferenceConstants.DEFAULT_CLAUDE_MODEL);
		store.setDefault(PreferenceConstants.P_CLAUDE_API_URL, PreferenceConstants.DEFAULT_CLAUDE_API_URL);
		store.setDefault(PreferenceConstants.P_CLAUDE_API_KEY, ""); //$NON-NLS-1$
		store.setDefault(PreferenceConstants.P_OPENAI_MODEL, PreferenceConstants.DEFAULT_OPENAI_MODEL);
		store.setDefault(PreferenceConstants.P_OPENAI_API_URL, PreferenceConstants.DEFAULT_OPENAI_API_URL);
		store.setDefault(PreferenceConstants.P_OPENAI_API_KEY, ""); //$NON-NLS-1$
		store.setDefault(PreferenceConstants.P_SELECTED_PROVIDER, "org.openjdk.jmc.ui.ai.provider.claude"); //$NON-NLS-1$

		store.setDefault(PreferenceConstants.P_COLOR_USER_LIGHT, PreferenceConstants.DEFAULT_COLOR_USER_LIGHT);
		store.setDefault(PreferenceConstants.P_COLOR_ASSISTANT_LIGHT,
				PreferenceConstants.DEFAULT_COLOR_ASSISTANT_LIGHT);
		store.setDefault(PreferenceConstants.P_COLOR_TOOL_LIGHT, PreferenceConstants.DEFAULT_COLOR_TOOL_LIGHT);
		store.setDefault(PreferenceConstants.P_COLOR_ERROR_LIGHT, PreferenceConstants.DEFAULT_COLOR_ERROR_LIGHT);
		store.setDefault(PreferenceConstants.P_COLOR_USER_DARK, PreferenceConstants.DEFAULT_COLOR_USER_DARK);
		store.setDefault(PreferenceConstants.P_COLOR_ASSISTANT_DARK, PreferenceConstants.DEFAULT_COLOR_ASSISTANT_DARK);
		store.setDefault(PreferenceConstants.P_COLOR_TOOL_DARK, PreferenceConstants.DEFAULT_COLOR_TOOL_DARK);
		store.setDefault(PreferenceConstants.P_COLOR_ERROR_DARK, PreferenceConstants.DEFAULT_COLOR_ERROR_DARK);
	}
}
