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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;

public class AddAttibutesAction extends Action {

	private static final MRI[] EMPTY = new MRI[0];
	private final IMRIService availableAttributes;
	private final IMRIMetadataService mds;
	private final IAttributeSet attributSet;
	private final boolean numericalsOnly;

	public AddAttibutesAction(IMRIMetadataService mds, IMRIService availableAttributes, IAttributeSet attributSet) {
		this(mds, availableAttributes, attributSet, true);
	}

	public AddAttibutesAction(IMRIMetadataService mds, IMRIService availableAttributes, IAttributeSet attributSet,
			boolean numericalsOnly) {
		super(Messages.ADD_ATTIBUTES_ACTION_TEXT,
				RJMXUIPlugin.getDefault().getMCImageDescriptor(IconConstants.ICON_ADD_OBJECT));
		this.attributSet = attributSet;
		this.numericalsOnly = numericalsOnly;
		setToolTipText(Messages.ADD_ATTIBUTES_ACTION_TOOLTIP);
		setDisabledImageDescriptor(
				RJMXUIPlugin.getDefault().getMCImageDescriptor(IconConstants.ICON_ADD_OBJECT_DISABLED));
		setId("add"); //$NON-NLS-1$
		this.mds = mds;
		this.availableAttributes = availableAttributes;
	}

	@Override
	public void run() {
		AttributeSelectionViewModel viewModel = (allowMultiple()) ? createSelectSeveralAttributesViewModel()
				: createSelectOneAttributeViewModel();
		AttributeSelectorWizardDialog d = new AttributeSelectorWizardDialog(Display.getCurrent().getActiveShell(),
				viewModel);
		if (d.open(availableAttributes, mds, EMPTY, attributSet.elements()) == Window.OK) {
			attributSet.add(d.getSelectedAttributes());
			return;
		}
	}

	private AttributeSelectionViewModel createSelectOneAttributeViewModel() {
		return new AttributeSelectionViewModel(getContentType(), false, numericalsOnly,
				org.openjdk.jmc.rjmx.ui.messages.internal.Messages.SELECT_ATTRIBUTE_TITLE,
				org.openjdk.jmc.rjmx.ui.messages.internal.Messages.SELECT_ATTRIBUTE_TITLE,
				org.openjdk.jmc.rjmx.ui.messages.internal.Messages.SELECT_ATTRIBUTE_DESCRIPTION,
				org.openjdk.jmc.rjmx.ui.messages.internal.Messages.CONFIGURE_ATTRIBUTE_TITLE,
				org.openjdk.jmc.rjmx.ui.messages.internal.Messages.CONFIGURE_ATTRIBUTE_DESCRIPTION);
	}

	private AttributeSelectionViewModel createSelectSeveralAttributesViewModel() {
		return new AttributeSelectionViewModel(getContentType(), allowMultiple(), numericalsOnly,
				org.openjdk.jmc.rjmx.ui.messages.internal.Messages.SELECT_ATTRIBUTES_TITLE,
				org.openjdk.jmc.rjmx.ui.messages.internal.Messages.SELECT_ATTRIBUTES_TITLE,
				org.openjdk.jmc.rjmx.ui.messages.internal.Messages.SELECT_ATTRIBUTES_DESCRIPTION,
				org.openjdk.jmc.rjmx.ui.messages.internal.Messages.CONFIGURE_ATTRIBUTES_TITLE,
				org.openjdk.jmc.rjmx.ui.messages.internal.Messages.CONFIGURE_ATTRIBUTES_DESCRIPTION);
	}

	protected ContentType<?> getContentType() {
		return null;
	}

	protected boolean allowMultiple() {
		return true;
	}
}
