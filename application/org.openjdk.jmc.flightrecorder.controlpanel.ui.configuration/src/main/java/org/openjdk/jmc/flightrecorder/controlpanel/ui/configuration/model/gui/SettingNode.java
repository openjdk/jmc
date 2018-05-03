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

import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLTagInstance;

final class SettingNode extends Node {
	private final XMLModel m_model;
	private final XMLTagInstance m_settingElement;
	private Node m_source;

	public SettingNode(XMLModel model, XMLTagInstance settingElement) {
		m_model = model;
		m_settingElement = settingElement;
	}

	@Override
	protected void addTransmitter(Node transmitter) {
		super.addTransmitter(transmitter);
		setSource(transmitter);
	}

	private void setSource(Node source) {
		m_source = source;
		/*
		 * Do not attempt to update() here. The graph may not be fully built yet. In particular,
		 * evaluating condition nodes may result in incorrect changes being reported.
		 */
	}

	@Override
	protected void onChange() {
		update();
	}

	public void update() {
		String currentValue = m_settingElement.getContent();
		if (m_source != null) {
			Value newValue = m_source.getValue();
			if (newValue != null && !newValue.isNull()) {
				String newStringValue = newValue.toString();
				if (!newStringValue.equals(currentValue)) {
					m_settingElement.setContent(newStringValue);
					m_model.markDirty();
				}
			}
		}
	}

	/**
	 * Return the current value of this node
	 *
	 * @return the value
	 */
	@Override
	Value getValue() {
		throw new UnsupportedOperationException("Not implemented!"); //$NON-NLS-1$
	}
}
