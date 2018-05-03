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
import java.util.LinkedList;
import java.util.List;

public final class ProducerEnvironment {
	private final List<Node> m_nodes = new LinkedList<>();

	private String m_description = ""; //$NON-NLS-1$
	private String m_name = ""; //$NON-NLS-1$
	private String m_uri = ""; //$NON-NLS-1$

	public List<WidgetNode> getWidgets() {
		ArrayList<WidgetNode> widgets = new ArrayList<>();
		for (Node node : m_nodes) {
			if (node instanceof WidgetNode) {
				widgets.add((WidgetNode) node);
			}
		}
		return widgets;
	}

	public String getName() {
		return m_name;
	}

	public String getDescription() {
		return m_description;
	}

	void addNode(Node node) {
		m_nodes.add(node);
	}

	void setName(String name) {
		m_name = name;
	}

	void setDescription(String description) {
		m_description = description;
	}

	/**
	 * Sets the URI of this producer, adding a trailing slash if missing.
	 *
	 * @param uri
	 */
	void setURI(String uri) {
		if (!uri.endsWith("/")) { //$NON-NLS-1$
			uri += '/';
		}
		m_uri = uri;
	}

	/**
	 * Get the URI of this producer. This is guaranteed to end in a slash.
	 *
	 * @return the URI of this producer, ending with a slash
	 */
	public String getURI() {
		return m_uri;
	}

	@Override
	public String toString() {
		return getURI();
	}
}
