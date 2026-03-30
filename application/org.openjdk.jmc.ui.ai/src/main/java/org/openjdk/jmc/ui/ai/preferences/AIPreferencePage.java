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

import java.net.URI;
import java.util.List;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.openjdk.jmc.ui.ai.AIPlugin;
import org.openjdk.jmc.ui.ai.AIProviderRegistry;
import org.openjdk.jmc.ui.ai.IAIProvider;

public class AIPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final String ANTHROPIC_CONSOLE_URL = "https://console.anthropic.com/settings/keys"; //$NON-NLS-1$
	private static final String OPENAI_CONSOLE_URL = "https://platform.openai.com/api-keys"; //$NON-NLS-1$

	public AIPreferencePage() {
		super(GRID);
		setDescription(Messages.AIPreferencePage_DESCRIPTION);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(AIPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();

		Composite claudeGroup = createGroup(parent, Messages.AIPreferencePage_CLAUDE_GROUP);
		addField(new StringFieldEditor(PreferenceConstants.P_CLAUDE_API_KEY, Messages.AIPreferencePage_API_KEY,
				claudeGroup));
		addField(new ComboFieldEditor(PreferenceConstants.P_CLAUDE_MODEL, Messages.AIPreferencePage_MODEL,
				getModelEntries("org.openjdk.jmc.ui.ai.provider.claude"), claudeGroup)); //$NON-NLS-1$
		addField(new StringFieldEditor(PreferenceConstants.P_CLAUDE_API_URL, Messages.AIPreferencePage_API_URL,
				claudeGroup));
		Button getApiKeyButton = new Button(claudeGroup, SWT.PUSH);
		getApiKeyButton.setText(Messages.AIPreferencePage_GET_API_KEY);
		getApiKeyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openBrowser(ANTHROPIC_CONSOLE_URL);
			}
		});

		Composite openaiGroup = createGroup(parent, Messages.AIPreferencePage_OPENAI_GROUP);
		addField(new StringFieldEditor(PreferenceConstants.P_OPENAI_API_KEY, Messages.AIPreferencePage_OPENAI_API_KEY,
				openaiGroup));
		addField(new ComboFieldEditor(PreferenceConstants.P_OPENAI_MODEL, Messages.AIPreferencePage_OPENAI_MODEL,
				getModelEntries("org.openjdk.jmc.ui.ai.provider.openai"), openaiGroup)); //$NON-NLS-1$
		addField(new StringFieldEditor(PreferenceConstants.P_OPENAI_API_URL, Messages.AIPreferencePage_OPENAI_API_URL,
				openaiGroup));
		Button openaiKeyButton = new Button(openaiGroup, SWT.PUSH);
		openaiKeyButton.setText(Messages.AIPreferencePage_OPENAI_GET_API_KEY);
		openaiKeyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openBrowser(OPENAI_CONSOLE_URL);
			}
		});

		Composite colorsRow = new Composite(parent, SWT.NONE);
		GridLayout colorsLayout = new GridLayout(2, true);
		colorsLayout.marginWidth = 0;
		colorsLayout.marginHeight = 0;
		colorsRow.setLayout(colorsLayout);
		colorsRow.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));

		Composite lightGroup = createGroup(colorsRow, Messages.AIPreferencePage_COLORS_LIGHT_GROUP, 1);
		addField(new ColorFieldEditor(PreferenceConstants.P_COLOR_USER_LIGHT, Messages.AIPreferencePage_COLOR_USER,
				lightGroup));
		addField(new ColorFieldEditor(PreferenceConstants.P_COLOR_ASSISTANT_LIGHT,
				Messages.AIPreferencePage_COLOR_ASSISTANT, lightGroup));
		addField(new ColorFieldEditor(PreferenceConstants.P_COLOR_TOOL_LIGHT, Messages.AIPreferencePage_COLOR_TOOL,
				lightGroup));
		addField(new ColorFieldEditor(PreferenceConstants.P_COLOR_ERROR_LIGHT, Messages.AIPreferencePage_COLOR_ERROR,
				lightGroup));

		Composite darkGroup = createGroup(colorsRow, Messages.AIPreferencePage_COLORS_DARK_GROUP, 1);
		addField(new ColorFieldEditor(PreferenceConstants.P_COLOR_USER_DARK, Messages.AIPreferencePage_COLOR_USER,
				darkGroup));
		addField(new ColorFieldEditor(PreferenceConstants.P_COLOR_ASSISTANT_DARK,
				Messages.AIPreferencePage_COLOR_ASSISTANT, darkGroup));
		addField(new ColorFieldEditor(PreferenceConstants.P_COLOR_TOOL_DARK, Messages.AIPreferencePage_COLOR_TOOL,
				darkGroup));
		addField(new ColorFieldEditor(PreferenceConstants.P_COLOR_ERROR_DARK, Messages.AIPreferencePage_COLOR_ERROR,
				darkGroup));
	}

	private Composite createGroup(Composite parent, String title) {
		return createGroup(parent, title, 3);
	}

	private Composite createGroup(Composite parent, String title, int columnSpan) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(title);
		group.setLayout(new GridLayout(1, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, columnSpan, 1));

		Composite inner = new Composite(group, SWT.NONE);
		GridLayout innerLayout = new GridLayout(3, false);
		innerLayout.marginWidth = 4;
		innerLayout.marginHeight = 4;
		inner.setLayout(innerLayout);
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return inner;
	}

	private String[][] getModelEntries(String providerId) {
		IAIProvider provider = AIProviderRegistry.getInstance().getProvider(providerId);
		if (provider != null) {
			List<String> models = provider.getAvailableModels();
			String[][] entries = new String[models.size()][2];
			for (int i = 0; i < models.size(); i++) {
				entries[i][0] = models.get(i);
				entries[i][1] = models.get(i);
			}
			return entries;
		}
		return new String[][] {{PreferenceConstants.DEFAULT_CLAUDE_MODEL, PreferenceConstants.DEFAULT_CLAUDE_MODEL}};
	}

	private void openBrowser(String url) {
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URI(url).toURL());
		} catch (Exception e) {
			AIPlugin.getLogger().warning("Failed to open browser: " + e.getMessage()); //$NON-NLS-1$
		}
	}
}
