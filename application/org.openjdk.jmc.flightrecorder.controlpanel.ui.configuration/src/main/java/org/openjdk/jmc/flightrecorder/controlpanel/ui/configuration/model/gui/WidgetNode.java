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

import java.util.function.BiConsumer;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLTagInstance;

public abstract class WidgetNode extends Node {
	private final XMLTagInstance m_inputElement;
	private final XMLModel m_model;

	private String m_description = ""; //$NON-NLS-1$
	private String m_label = ""; //$NON-NLS-1$

	protected WidgetNode(XMLModel model, XMLTagInstance inputElement) {
		m_inputElement = inputElement;
		m_model = model;
	}

	final protected XMLTagInstance getInputElement() {
		return m_inputElement;
	}

	public abstract void create(
		FormToolkit toolkit, Composite parent, int horisontalSpan, BiConsumer<Object, String> errorConsumer);

	public abstract void create(Composite parent, int horisontalSpan, BiConsumer<Object, String> errorConsumer);

	public final String getDescription() {
		return m_description;
	}

	public final String getLabel() {
		return m_label;
	}

	protected final void markDirty() {
		m_model.markDirty();
	}

	final void setDescription(String description) {
		m_description = description;
	}

	final void setLabel(String label) {
		m_label = label;
	}

	@Override
	public void fireChange() {
		super.fireChange();
	}

	@Override
	Value getValue() {
		return Value.valueOf(m_inputElement.getContent());
	}

	protected void setValue(String newValue) {
		String oldValue = getValue().toString();
		if (!newValue.equals(oldValue)) {
			m_inputElement.setContent(newValue);
			fireChange();
			markDirty();
		}
	}
}
