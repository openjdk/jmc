/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2025, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.console.agent.editor.sections;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.console.agent.manager.model.IPreset;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.misc.AbstractStructuredContentProvider;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalConfigSection extends MCSectionPart {
	private static final String SECTION_TITLE = "Global Configurations";
	private static final String HEADER_OPTION = "Option";
	private static final String HEADER_VALUE = "Value";
	private static final String ALLOW_TO_STRING = "Allow toString()";
	private static final String ALLOW_CONVERTER = "Allow converters";
	private static final String CLASS_PREFIX = "Class prefix";
	private static final String NO_CONFIG_APPLIED = "No configuration applied";

	private final Composite stack;
	private final StackLayout stackLayout;
	private final Composite message;
	private final TableViewer viewer;

	public GlobalConfigSection(Composite parent, FormToolkit toolkit) {
		super(parent, toolkit, DEFAULT_TITLE_STYLE);

		getSection().setText(SECTION_TITLE);

		Composite body = createSectionBody(MCLayoutFactory.createMarginFreeFormPageLayout());
		stack = toolkit.createComposite(body);
		stack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		stackLayout = new StackLayout();
		stack.setLayout(stackLayout);

		message = toolkit.createComposite(stack);
		message.setLayout(new GridLayout(1, false));
		toolkit.createLabel(message, NO_CONFIG_APPLIED).setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

		viewer = createViewer(stack, toolkit);
		viewer.getControl()
				.setLayoutData(MCLayoutFactory.createFormPageLayoutData(SWT.DEFAULT, SWT.DEFAULT, true, true));

		stackLayout.topControl = message;
	}

	private TableViewer createViewer(Composite parent, FormToolkit formToolkit) {
		Table table = formToolkit.createTable(parent, SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		TableViewer viewer = new TableViewer(table);
		viewer.setContentProvider(new ConfigContentProvider());

		List<IColumn> columns = new ArrayList<>(2);
		columns.add(new ColumnBuilder(HEADER_OPTION, HEADER_OPTION, new ColumnLabelProvider() {
			@SuppressWarnings("unchecked")
			@Override
			public String getText(Object element) {
				return ((Map.Entry<String, String>) element).getKey();
			}
		}).build());
		columns.add(new ColumnBuilder(HEADER_VALUE, HEADER_VALUE, new ColumnLabelProvider() {
			@SuppressWarnings("unchecked")
			@Override
			public String getText(Object element) {
				return ((Map.Entry<String, String>) element).getValue();
			}
		}).build());
		ColumnManager.build(viewer, columns, null);

		return viewer;
	}

	public void setInput(IPreset preset) {
		viewer.setInput(preset);
		stackLayout.topControl = preset == null ? message : viewer.getControl();
		stack.layout();
	}

	private static class ConfigContentProvider extends AbstractStructuredContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement == null) {
				return new Object[0];
			}

			if (!(inputElement instanceof IPreset)) {
				throw new IllegalArgumentException("input element must be an IPreset"); // $NON-NLS-1$
			}

			IPreset preset = (IPreset) inputElement;

			Map<String, String> entries = new HashMap<>(3);
			entries.put(ALLOW_TO_STRING, String.valueOf(preset.getAllowToString()));
			entries.put(ALLOW_CONVERTER, String.valueOf(preset.getAllowConverter()));
			entries.put(CLASS_PREFIX, preset.getClassPrefix());

			return entries.entrySet().toArray(new Map.Entry[0]);
		}
	}
}
