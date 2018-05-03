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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLTagInstance;

final class SelectionNode extends WidgetNode {
	private final List<XMLTagInstance> m_optionElements = new ArrayList<>();

	private ComboViewer m_viewer;

	private static class ComboLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			XMLTagInstance optionElement = (XMLTagInstance) element;
			return optionElement.getValue(JFCGrammar.ATTRIBUTE_LABEL_MANDATORY);
		}
	}

	public SelectionNode(XMLModel model, XMLTagInstance selectionElement) {
		super(model, selectionElement);
	}

	private String getDefaultIdentifier() {
		return getInputElement().getValue(JFCGrammar.ATTRIBUTE_DEFAULT);
	}

	public void addItem(XMLTagInstance optionElement) {
		m_optionElements.add(optionElement);
	}

	@Override
	public void create(
		FormToolkit toolkit, Composite parent, int horisontalSpan, BiConsumer<Object, String> errorTracker) {
		Label label = toolkit.createLabel(parent, getLabel() + ':');
		adaptLabel(label);
		m_viewer = createViewer(parent, horisontalSpan);
		setViewerSelection();
	}

	@Override
	public void create(Composite parent, int horisontalSpan, BiConsumer<Object, String> errorTracker) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(getLabel() + ':');
		adaptLabel(label);

		m_viewer = createViewer(parent, horisontalSpan);
		setViewerSelection();
	}

	private void setViewerSelection() {
		XMLTagInstance selected = getSelected();
		if (selected != null) {
			m_viewer.setSelection(new StructuredSelection(selected));
		}
	}

	private void adaptLabel(Label label) {
		GridData gd1 = new GridData(SWT.FILL, SWT.CENTER, false, false);
		label.setLayoutData(gd1);
		label.setToolTipText(getDescription());

	}

	private ComboViewer createViewer(Composite parent, int horisontalSpan) {
		ComboViewer viewer = new ComboViewer(parent);
		viewer.setContentProvider(new ArrayContentProvider());

		viewer.getControl().setToolTipText(getDescription());
		viewer.setLabelProvider(new ComboLabelProvider());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ss = ((IStructuredSelection) event.getSelection());
				select((XMLTagInstance) ss.getFirstElement());
			}
		});
		viewer.setInput(m_optionElements);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd2.horizontalSpan = horisontalSpan - 1;
		viewer.getControl().setLayoutData(gd2);

		return viewer;
	}

	private XMLTagInstance getSelected() {
		for (XMLTagInstance optionElement : m_optionElements) {
			if (getDefaultIdentifier().equalsIgnoreCase(optionElement.getValue(JFCGrammar.ATTRIBUTE_NAME))) {
				return optionElement;
			}
		}
		return m_optionElements.size() > 0 ? m_optionElements.get(0) : null;
	}

	@Override
	Value getValue() {
		String valueId = getInputElement().getValue(JFCGrammar.ATTRIBUTE_DEFAULT);
		String value = null;
		// FIXME: Why not use m_optionElements? Otherwise, why keep it?
		for (XMLTagInstance optionElement : getInputElement().getTagsInstances()) {
			if (value == null) {
				value = optionElement.getContent();
			}

			if (valueId.equalsIgnoreCase(optionElement.getValue(JFCGrammar.ATTRIBUTE_NAME))) {
				value = optionElement.getContent();
			}
		}
		if (value == null) {
			value = ""; //$NON-NLS-1$
		}
		return Value.valueOf(value);
	}

	private void select(XMLTagInstance optionElement) {
		if (optionElement != null) {
			m_viewer.getControl().setToolTipText(optionElement.getValue(JFCGrammar.ATTRIBUTE_DESCRIPTION));
			String currentIdentifier = getDefaultIdentifier();
			String newIdentifier = optionElement.getValue(JFCGrammar.ATTRIBUTE_NAME);
			if (!currentIdentifier.equalsIgnoreCase(newIdentifier)) {
				getInputElement().setValue(JFCGrammar.ATTRIBUTE_DEFAULT, newIdentifier);
				fireChange();
				markDirty();
			}
		}
	}
}
