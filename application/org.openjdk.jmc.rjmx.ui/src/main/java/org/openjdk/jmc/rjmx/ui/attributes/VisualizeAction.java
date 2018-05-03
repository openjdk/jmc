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
package org.openjdk.jmc.rjmx.ui.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.SelectionProviderAction;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRITransformationFactory;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;
import org.openjdk.jmc.rjmx.ui.internal.IconConstants;
import org.openjdk.jmc.rjmx.ui.internal.SectionPartManager;
import org.openjdk.jmc.ui.wizards.OnePageWizardDialog;

/**
 * Action that lets the user visualize attributes in another tab.
 */
public class VisualizeAction extends SelectionProviderAction {

	final private IConnectionHandle connection;
	private List<MRI> numericals = Collections.emptyList();
	private final SectionPartManager sectionPartManager;
	private final IMRITransformationFactory transformationFactory;

	public VisualizeAction(String text, SectionPartManager sectionPartManager, IConnectionHandle connection,
			ISelectionProvider selectionProvider) {
		this(text, sectionPartManager, connection, selectionProvider, null);
	}

	public VisualizeAction(String text, SectionPartManager sectionPartManager, IConnectionHandle connection,
			ISelectionProvider selectionProvider, IMRITransformationFactory transformationFactory) {
		super(selectionProvider, text);
		this.sectionPartManager = sectionPartManager;
		this.connection = connection;
		this.transformationFactory = transformationFactory;
		setEnabled(false);
	}

	@Override
	public void selectionChanged(IStructuredSelection selection) {
		numericals = filterForNumericalAttributeDescriptors(selection.toList());
		if (transformationFactory != null) {
			numericals = transformMRIs(numericals);
		}
		setEnabled(!numericals.isEmpty());
	}

	public List<MRI> transformMRIs(List<MRI> mris) {
		List<MRI> transformed = new ArrayList<>();
		for (MRI mri : mris) {
			transformed.add(transformationFactory.createTransformationMRI(mri));
		}
		return transformed;
	}

	private List<MRI> filterForNumericalAttributeDescriptors(List<?> descriptors) {
		List<MRI> numericals = new ArrayList<>();
		IMRIMetadataService mriMetadataService = connection.getServiceOrNull(IMRIMetadataService.class);
		if (mriMetadataService == null) {
			return numericals;
		}
		for (Object object : descriptors) {
			if (object instanceof ReadOnlyMRIAttribute && MRIMetadataToolkit
					.isNumerical(mriMetadataService.getMetadata(((ReadOnlyMRIAttribute) object).getMRI()))) {
				numericals.add(((ReadOnlyMRIAttribute) object).getMRI());
			}
		}
		return numericals;
	}

	@Override
	public void run() {
		VisualizeWizardPage wizardPage = new VisualizeWizardPage(sectionPartManager, numericals, connection);
		OnePageWizardDialog dialog = new OnePageWizardDialog(Display.getCurrent().getActiveShell(), wizardPage,
				RJMXUIPlugin.getDefault().getImage(IconConstants.ICON_ADD_GRAPH_ATTRIBUTE));
		dialog.open();
	}
}
