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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.gui.ErrorTracker;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.gui.GUIModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.gui.ProducerEnvironment;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.gui.WidgetNode;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;

/**
 * Creates a view of the control in a recording template/configuration.
 */
public final class RecordingTemplateControlView {

	private final Composite m_container;
	private ErrorTracker m_errorTracker;

	public RecordingTemplateControlView(Composite parent, Consumer<String> errorConsumer) {
		m_container = parent;
		m_errorTracker = new ErrorTracker(errorConsumer);
	}

	public void cleanCreate(XMLModel xmlModel) {
		cleanControls();
		createProducerControls(xmlModel, m_container);
		m_container.layout();
	}

	public void create(XMLModel xmlModel) {
		createProducerControls(xmlModel, m_container);
	}

	private void cleanControls() {
		for (Control control : m_container.getChildren()) {
			control.dispose();
		}
	}

	private void createProducerControls(XMLModel xmlModel, Composite parent) {
		GUIModel uiModel = new GUIModel(xmlModel);
		uiModel.evaluate();
		for (ProducerEnvironment producer : uiModel.getProducers()) {
			if (!producer.getWidgets().isEmpty()) {
				createProducer(parent, producer);
			}
		}
	}

	private void createProducer(Composite parent, ProducerEnvironment producer) {
		Group g = getProducerGroup(parent, producer);
		GridLayout layout = new GridLayout(3, false);
		layout.verticalSpacing = 7;
		g.setLayout(layout);

		addProducerDescription(producer, g);
		for (WidgetNode widget : producer.getWidgets()) {
			widget.create(g, layout.numColumns, m_errorTracker::trackError);
		}
	}

	private void addProducerDescription(ProducerEnvironment producer, Group g) {
		String prodDesc = producer.getDescription();
		if (prodDesc != null && prodDesc.length() > 0) {
			Label prodDescLabel = new Label(g, SWT.NONE);
			prodDescLabel.setText(producer.getDescription());
			GridData gd = new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1);
			prodDescLabel.setLayoutData(gd);
		}
	}

	private Group getProducerGroup(Composite parent, ProducerEnvironment producer) {

		String producerText = getDescriptionName(producer);
		Group g = new Group(parent, SWT.NONE);
		g.setText(producerText);
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return g;
	}

	private String getDescriptionName(ProducerEnvironment producer) {
		if (producer.getName().length() > 0) {
			return producer.getName();
		} else {
			return producer.getURI();
		}
	}

}
