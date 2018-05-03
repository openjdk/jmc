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

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

import org.openjdk.jmc.rjmx.subscription.MRI;

public class AttributeConfiguratorWizardPage extends WizardPage {

	public final static String PAGE_NAME = "org.openjdk.jmc.rjmx.attribute.configuration"; //$NON-NLS-1$

	private static AttributeLabelProvider createNameLabelProvider(AttributeSelectionContentModel selectorModel) {
		return new AttributeLabelProvider(selectorModel.getMetadataService(), selectorModel.getMRIService());
	}

	private final AttributeSelectionViewModel m_viewModel;
	private final AttributeSelectionContentModel m_selectorModel;
	private TableViewer table;

	public AttributeConfiguratorWizardPage(AttributeSelectionViewModel viewModel,
			AttributeSelectionContentModel selectorModel) {
		super(PAGE_NAME, viewModel.getConfigureAttributePageTitle(), null);
		setDescription(viewModel.getConfigureAttributePageDescription());
		m_viewModel = viewModel;
		m_selectorModel = selectorModel;
	}

	@Override
	public void createControl(Composite parent) {
		table = AttributeConfiguratorTableFactory.createAttributeConfiguratorTable(parent,
				createNameLabelProvider(m_selectorModel), m_viewModel, m_selectorModel, new Runnable() {
					@Override
					public void run() {
						setPageComplete(isPageComplete());
					}
				});
		setControl(table.getTable());
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			table.setInput(m_selectorModel.getSelectedAttributes());
		}
		super.setVisible(visible);
	}

	@Override
	public boolean isPageComplete() {
		MRI[] selectedAttributes = m_selectorModel.getSelectedAttributes();
		if (selectedAttributes.length == 0) {
			return false;
		}
		if (m_viewModel.isNumericalOnly()) {
			for (MRI attribute : selectedAttributes) {
				if (m_selectorModel.getAttributeUnit(attribute) == null) {
					return false;
				}
			}
		}
		return true;
	}
}
