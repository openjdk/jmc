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
package org.openjdk.jmc.ui.accessibility;

import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.accessibility.AccessibleListener;

/**
 * Class that can be used to give information for an accessibility client
 */
public class MCAccessibleListener extends AccessibleAdapter {
	private String m_name;
	private String m_description;
	private String m_help;
	private String m_componentType;

	/**
	 * Creates an {@link AccessibleListener} with a name and a description.
	 *
	 * @param name
	 *            the name, of the control. Eg, table
	 * @param description
	 * @return
	 */
	public static AccessibleListener createNameHelp(String name, String help) {
		MCAccessibleListener accessibleListener = new MCAccessibleListener();
		accessibleListener.setName(name);
		accessibleListener.setHelp(help);
		return accessibleListener;
	}

	/**
	 * Sets the name. E.g. "Table", "ExpandableComposite".
	 *
	 * @param name
	 *            of the control
	 */
	final public void setName(String name) {
		m_name = name;
	}

	/**
	 * Sets the description. E.g. "A graph showing heap usage."
	 *
	 * @param name
	 *            of the control
	 */
	final public void setDescription(String description) {
		m_description = description;
	}

	/**
	 * Set the help text. Typically the tooltip text.
	 *
	 * @param help
	 *            the help text
	 */
	final public void setHelp(String help) {
		m_help = help;
	}

	/* See {@link AccessibleAdapter#getDescription(AccessibleEvent)} */
	@Override
	final public void getDescription(AccessibleEvent e) {
		if (m_description != null) {
			e.result = m_description;
		}
	}

	/* See {@link AccessibleAdapter#getHelp(AccessibleEvent)} */
	@Override
	final public void getHelp(AccessibleEvent e) {
		if (m_help != null) {
			e.result = m_help;
		}
	}

	/**
	 * Sets what kind of type the control is. This is typically read after the name.
	 */
	public void setComponentType(String componentType) {
		m_componentType = componentType;
	}

	/* See {@link AccessibleAdapter#getName(AccessibleEvent)} */
	@Override
	final public void getName(AccessibleEvent e) {
		if (m_name != null) {
			e.result = m_name;
			if (m_componentType != null) {
				e.result += ' ' + m_componentType;
			}
		}
	}

	/**
	 * Returns the name this listener will use.
	 *
	 * @return the name, or null if not set
	 */
	public String getName() {
		return m_name;
	}
}
