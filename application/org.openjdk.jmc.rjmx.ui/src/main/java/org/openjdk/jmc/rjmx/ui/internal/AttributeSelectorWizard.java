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

import org.eclipse.jface.wizard.Wizard;

import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;

public class AttributeSelectorWizard extends Wizard {

	private final AttributeSelectionViewModel m_viewModel;
	private AttributeSelectionContentModel m_selectorModel;

	public AttributeSelectorWizard(AttributeSelectionViewModel viewModel) {
		setDialogSettings(RJMXUIPlugin.getDefault().getDialogSettings());
		setWindowTitle(viewModel.getWizardTitle());
		m_viewModel = viewModel;
	}

	public void setInput(AttributeSelectionContentModel selectorModel) {
		m_selectorModel = selectorModel;
	}

	@Override
	public void addPages() {
		addPage(new AttributeSelectorWizardPage(m_viewModel, m_selectorModel));
		addPage(new AttributeConfiguratorWizardPage(m_viewModel, m_selectorModel));

		m_selectorModel.addListener(new IAttributeSelectionContentListener() {
			@Override
			public void selectionChanged(AttributeSelectionContentModel selectorModel) {
				if (m_selectorModel != null) {
					MRI[] selectedAttributes = selectorModel.getSelectedAttributes();
					if (selectedAttributes.length > 0) {
						boolean linkConfigurePage = false;
						IMRIMetadataService metadataService = m_selectorModel.getMetadataService();
						for (MRI mri : selectedAttributes) {
							IMRIMetadata metadata = metadataService.getMetadata(mri);
							if (MRIMetadataToolkit.isNumerical(metadata)) {
								IUnit unit = UnitLookup.getUnitOrNull(metadata.getUnitString());
								if (unit == null) {
									linkConfigurePage = true;
								}
							}
						}
						if (linkConfigurePage) {
							getAttributeSelectorWizardPage().setNextPage(getAttributeConfiguratorWizardPage());
						} else {
							getAttributeSelectorWizardPage().setNextPage(null);
						}
					}
				}
			}
		});
	}

	private AttributeSelectorWizardPage getAttributeSelectorWizardPage() {
		return (AttributeSelectorWizardPage) getPage(AttributeSelectorWizardPage.PAGE_NAME);
	}

	private AttributeConfiguratorWizardPage getAttributeConfiguratorWizardPage() {
		return (AttributeConfiguratorWizardPage) getPage(AttributeConfiguratorWizardPage.PAGE_NAME);
	}

	public MRI[] getSelectedAttributes() {
		return m_selectorModel.getSelectedAttributes();
	}

	@Override
	public boolean performFinish() {
		m_selectorModel.commitUnitChanges();
		return true;
	}
}
