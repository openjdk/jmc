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
package org.openjdk.jmc.console.ui.notification.uicomponents;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;

import org.openjdk.jmc.rjmx.triggers.fields.internal.Field;
import org.openjdk.jmc.ui.common.CorePlugin;
import org.openjdk.jmc.ui.common.idesupport.IDESupportToolkit;
import org.openjdk.jmc.ui.common.resource.MCFile;
import org.openjdk.jmc.ui.misc.ControlDecorationToolkit;
import org.openjdk.jmc.ui.uibuilder.IUIBuilder;

/**
 * Input item for files
 */
public class FileInputItem extends TextInputItem {
	private final class BrowseSelectionListener implements SelectionListener {
		public BrowseSelectionListener() {

		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			MCFile file = onBrowse();
			if (file != null) {
				getField().setValue(file.getPath());
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// not used
		}

		private MCFile onBrowse() {
			return CorePlugin.getDefault().getIDESupport().browseForSaveAsFile(
					NLS.bind(Messages.FileInputItem_SELECT_FIELD_TEXT, getField().getName()), getField().getValue(),
					null, ""); //$NON-NLS-1$
		}

	}

	public FileInputItem(Field field, IUIBuilder builder) {
		super(field, builder);
	}

	@Override
	protected void createUI() {
		getUIBuilder().createLabel(getField().getName() + InputItem.FIELD_LABEL_SUFFIX, ""); //$NON-NLS-1$
		m_text = getUIBuilder().createText(getField().getValue(), getField().getDescription(), SWT.NONE);
		Button browseButton = getUIBuilder().createButton(Messages.FileInputItem_BUTTON_BROWSE_TEXT,
				Messages.FileInputItem_BUTTON_BROWSE_TOOLTIP);
		browseButton.addSelectionListener(new BrowseSelectionListener());
		getUIBuilder().layout();
		final ControlDecoration infoDecorator = ControlDecorationToolkit.createDecorator(m_text, SWT.TOP | SWT.RIGHT,
				FieldDecorationRegistry.DEC_INFORMATION);
		infoDecorator.setShowOnlyOnFocus(false);
		m_text.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				IStatus validateFile = IDESupportToolkit.validateFileResourcePath(m_text.getText());
				if (validateFile.isOK()) {
					infoDecorator.hide();
				} else {
					infoDecorator.setDescriptionText(validateFile.getMessage());
					infoDecorator.show();
				}
			}
		});
		m_text.setText(getField().getValue());
	}
}
