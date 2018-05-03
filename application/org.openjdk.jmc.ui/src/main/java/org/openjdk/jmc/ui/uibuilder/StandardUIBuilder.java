/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.ui.uibuilder;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormText;

/**
 * Builder for creating some standard UI components
 */
public class StandardUIBuilder implements IUIBuilder {
	private Composite m_mainContainer;
	private static final int MAX_SPAN = 10;
	private int m_span = MAX_SPAN;

	private GridData m_lastGridData = null;

	public StandardUIBuilder(Composite container) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = MAX_SPAN;
		container.setLayout(layout);
		m_mainContainer = container;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.WRAP);
		return composite;
	}

	@Override
	public void setCompositeLayout(Composite composite) {
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		setSpan(gd);
		composite.setLayoutData(gd);
	}

	@Override
	public Table createTable(Composite parent, boolean checked) {
		int style = checked ? SWT.CHECK : SWT.NONE;
		Table table = new Table(parent, style | SWT.BORDER);
		return table;
	}

	@Override
	public void createCaption(String text) {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);
		gd.grabExcessHorizontalSpace = true;

		Label label = new Label(getContainer(), SWT.NONE);
		label.setFont(JFaceResources.getBannerFont());
		label.setText(text);
		label.setLayoutData(gd);
	}

	@Override
	public Label createSeparator() {
		GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		setSpan(gd);
		Label label = new Label(getContainer(), SWT.SEPARATOR | SWT.HORIZONTAL);

		label.setLayoutData(gd);
		return label;
	}

	public void setSpan(GridData gridData) {
		if (m_lastGridData != null) {
			m_lastGridData.horizontalSpan = 1;
		}
		gridData.horizontalSpan = m_span;
		m_span--;
		m_lastGridData = gridData;
	}

	@Override
	public Button createCheckBox(String text, boolean checked, String toolTip) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);

		Button button = new Button(getContainer(), SWT.CHECK);
		button.setText(text);
		button.setLayoutData(gd);
		button.setSelection(checked);
		return button;
	}

	@Override
	public Button createButton(String text, String toolTip) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);

		Button button = new Button(getContainer(), SWT.NONE);
		button.setText(text);
		button.setLayoutData(gd);

		return button;
	}

	@Override
	public Label createWrapLabel(String text, String toolTip) {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);
		gd.grabExcessHorizontalSpace = true;
		Label label = new Label(getContainer(), SWT.WRAP);
		label.setText(text);

		label.setLayoutData(gd);
		layout();
		return label;
	}

	@Override
	public Label createLabel(String text, String toolTip) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);
		Label label = new Label(getContainer(), SWT.WRAP);
		label.setText(text);
		label.setToolTipText(toolTip);
		label.setLayoutData(gd);

		return label;
	}

	@Override
	public Text createText(String text, String toolTip, int style) {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);
		gd.grabExcessHorizontalSpace = true;

		Text textUI = new Text(getContainer(), SWT.BORDER | style);
		textUI.setText(text);
		textUI.setToolTipText(toolTip);
		textUI.setLayoutData(gd);

		return textUI;
	}

	private Composite getContainer() {
		return m_mainContainer;
	}

	@Override
	public void layout() {
		m_span = MAX_SPAN;
		m_lastGridData = null;
		m_mainContainer.layout();
	}

	@Override
	public void setContainer(Composite composite) {
		GridLayout layout = new GridLayout();
		layout.numColumns = MAX_SPAN;
		layout.horizontalSpacing += 5; // make sure control decorations fit
		composite.setLayout(layout);
		m_mainContainer = composite;
	}

	@Override
	public CCombo createCombo() {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);
		gd.minimumWidth = 200;
		CCombo c = new CCombo(m_mainContainer, SWT.BORDER | SWT.DROP_DOWN | SWT.SINGLE | SWT.H_SCROLL);
		c.setLayoutData(gd);
		return c;
	}

	@Override
	public Text createMultiText(String text, String tooltip) {
		GridData gd = new GridData(
				GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		Text textUI = new Text(getContainer(), SWT.WRAP | SWT.MULTI | SWT.BORDER);
		textUI.setText(text);
		textUI.setToolTipText(tooltip);
		textUI.setLayoutData(gd);
		return textUI;
	}

	@Override
	public FormText createFormText(String text, String toolTip) {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.grabExcessHorizontalSpace = true;
		FormText ft = new FormText(getContainer(), SWT.WRAP);
		setSpan(gd);
		ft.setText(text, false, false);
		ft.setToolTipText(toolTip);

		ft.setLayoutData(gd);
		layout();

		return ft;
	}

}
