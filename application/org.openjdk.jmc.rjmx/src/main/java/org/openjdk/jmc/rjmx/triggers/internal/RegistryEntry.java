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
package org.openjdk.jmc.rjmx.triggers.internal;

/**
 * Data class for registry entries.
 */
public class RegistryEntry {

	private final Class<?> m_registeredClass;
	private final String m_listName;
	private final String m_description;
	private final Class<?> m_editorClass;

	/**
	 * Constructor.
	 *
	 * @param registeredClass
	 *            the class to register (usually an action or constraint).
	 * @param listName
	 *            the name visible in the list.
	 * @param description
	 *            the description of the class.
	 * @param editorClass
	 *            the class used to edit attributes of this class.
	 */
	public RegistryEntry(Class<?> registeredClass, String listName, String description, Class<?> editorClass) {
		assert (editorClass != null);
		m_registeredClass = registeredClass;
		m_listName = listName;
		m_description = description;
		m_editorClass = editorClass;
	}

	/**
	 * Gets the registeredClass.
	 *
	 * @return Returns a Class
	 */
	public Class<?> getRegisteredClass() {
		return m_registeredClass;
	}

	/**
	 * Gets the description.
	 *
	 * @return Returns a String
	 */
	public String getDescription() {
		return m_description;
	}

	/**
	 * Gets the listName.
	 *
	 * @return Returns a String
	 */
	public String getListName() {
		return m_listName;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getListName();
	}

	/**
	 * Returns the editorClass.
	 *
	 * @return Class
	 */
	public Class<?> getEditorClass() {
		return m_editorClass;
	}
}
