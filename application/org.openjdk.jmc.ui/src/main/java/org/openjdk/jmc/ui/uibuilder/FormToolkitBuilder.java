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
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Builder for creating some UI components on a form using a FormToolkit
 */
public class FormToolkitBuilder implements IUIBuilder {
	private final FormToolkit m_toolkit;
	private static final int MAX_SPAN = 10;
	private static final int MARGIN_THICKNESS = 1;
	private Composite m_mainContainer;

	private int m_span = MAX_SPAN;
	private GridData m_lastGridData = null;

	public FormToolkitBuilder(FormToolkit toolkit, Composite container) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = MARGIN_THICKNESS;
		layout.marginHeight = MARGIN_THICKNESS;
		layout.numColumns = MAX_SPAN;
		container.setLayout(layout);
		m_mainContainer = container;

		m_toolkit = toolkit;
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
	public Composite createComposite(Composite parent) {
		Composite composite = m_toolkit.createComposite(parent, SWT.NONE);
		composite.setBackground(parent.getBackground());
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
		int style = checked ? SWT.CHECK : SWT.NULL;
		Table table = m_toolkit.createTable(parent, style);
		return table;
	}

	@Override
	public Button createButton(String text, String toolTip) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);

		Button button = m_toolkit.createButton(m_mainContainer, text, SWT.NONE);
		button.setLayoutData(gd);
		button.setToolTipText(toolTip);
		return button;
	}

	@Override
	public Label createLabel(String text, String toolTip) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);
		Label label = m_toolkit.createLabel(m_mainContainer, text);
		label.setToolTipText(toolTip);
		label.setLayoutData(gd);
		return label;
	}

	@Override
	public Label createWrapLabel(String text, String toolTip) {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);
		gd.grabExcessHorizontalSpace = true;
		Label label = m_toolkit.createLabel(m_mainContainer, text, SWT.WRAP);
		label.setText(text);

		label.setLayoutData(gd);
		layout();
		return label;
	}

	@Override
	public Label createSeparator() {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);
		Label label = m_toolkit.createLabel(m_mainContainer, "", SWT.FILL | SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
		label.setLayoutData(gd);
		return label;
	}

	@Override
	public void createCaption(String text) {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);
		gd.grabExcessHorizontalSpace = true;

		Label label = m_toolkit.createLabel(m_mainContainer, text, SWT.NONE);
		label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		label.setLayoutData(gd);

		return;
	}

	@Override
	public Text createMultiText(String text, String tooltip) {
		GridData gd = new GridData(GridData.BEGINNING | GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_VERTICAL);
		gd.grabExcessHorizontalSpace = false;
		gd.grabExcessVerticalSpace = true;
		gd.minimumWidth = 20;
		gd.minimumHeight = 20;
		Text textUI = createTextWithStyle(text, tooltip, SWT.WRAP | SWT.MULTI);
		setSpan(gd);
		textUI.setLayoutData(gd);
		return textUI;
	}

	@Override
	public Text createText(String text, String toolTip, int style) {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.grabExcessHorizontalSpace = true;
		Text textUI = createTextWithStyle(text, toolTip, SWT.NONE | style);
		setSpan(gd);
		textUI.setLayoutData(gd);
		return textUI;
	}

	@Override
	public FormText createFormText(String text, String toolTip) {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.grabExcessHorizontalSpace = true;
		FormText ft = m_toolkit.createFormText(m_mainContainer, false);
		setSpan(gd);
		ft.setText(text, false, false);
		ft.setToolTipText(toolTip);

		ft.setLayoutData(gd);
		layout();

		return ft;
	}

	private Text createTextWithStyle(String text, String toolTip, int style) {
		Text textUI = m_toolkit.createText(m_mainContainer, text, style);
		textUI.setToolTipText(toolTip);

		return textUI;
	}

	@Override
	public void layout() {
		m_span = MAX_SPAN;
		m_lastGridData = null;
		m_mainContainer.layout();
		m_toolkit.paintBordersFor(m_mainContainer);
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
	public Button createCheckBox(String text, boolean checked, String toolTip) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);

		Button button = m_toolkit.createButton(m_mainContainer, text, SWT.CHECK);
		button.setLayoutData(gd);
		button.setSelection(checked);
		button.setToolTipText(toolTip);

		return button;
	}

	@Override
	public CCombo createCombo() {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
		setSpan(gd);
		CCombo combo = new CCombo(m_mainContainer, SWT.NONE);
		m_toolkit.adapt(combo);
		combo.setLayoutData(gd);
		return combo;
	}

}
