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
package org.openjdk.jmc.rjmx.ui.internal;

import org.openjdk.jmc.common.unit.ContentType;

public class AttributeSelectionViewModel {

	private final ContentType<?> m_contentType;
	private final boolean m_multiSelection;
	private final boolean m_numericalOnly;
	private final String m_wizardTitle;
	private final String m_selectAttributePageTitle;
	private final String m_selectAttributePageDescription;
	private final String m_configureAttributePageTitle;
	private final String m_configureAttributePageDescription;

	public AttributeSelectionViewModel(ContentType<?> contentType, boolean multiSelection, boolean numericalsOnly,
			String wizardTitle, String selectAttributePageTitle, String selectAttributePageDescription,
			String configureAttributePageTitle, String configureAttributePageDescription) {
		m_contentType = contentType;
		m_multiSelection = multiSelection;
		m_numericalOnly = numericalsOnly;
		m_wizardTitle = wizardTitle;
		m_selectAttributePageTitle = selectAttributePageTitle;
		m_selectAttributePageDescription = selectAttributePageDescription;
		m_configureAttributePageTitle = configureAttributePageTitle;
		m_configureAttributePageDescription = configureAttributePageDescription;
	}

	public ContentType<?> getContentType() {
		return m_contentType;
	}

	public boolean isNumericalOnly() {
		return m_numericalOnly;
	}

	public boolean isMultiSelectionAllowed() {
		return m_multiSelection;
	}

	public String getWizardTitle() {
		return m_wizardTitle;
	}

	public String getSelectAttributePageTitle() {
		return m_selectAttributePageTitle;
	}

	public String getSelectAttributePageDescription() {
		return m_selectAttributePageDescription;
	}

	public String getConfigureAttributePageTitle() {
		return m_configureAttributePageTitle;
	}

	public String getConfigureAttributePageDescription() {
		return m_configureAttributePageDescription;
	}
}
