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

import java.util.function.BiConsumer;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLTagInstance;
import org.openjdk.jmc.ui.misc.ControlDecorationToolkit;
import org.openjdk.jmc.ui.misc.QuantityKindProposal;

final class TextNode<T> extends WidgetNode {
	private IConstraint<T> constraint;

	public TextNode(XMLModel model, XMLTagInstance textElement, IConstraint<T> constraint) {
		super(model, textElement);
		this.constraint = constraint;
	}

	@Override
	public void create(Composite parent, int horisontalSpan, BiConsumer<Object, String> errorTracker) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(getLabel() + ':');
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		final Text text = new Text(parent, SWT.BORDER);

		GridData gdText = new GridData(SWT.FILL, SWT.FILL, true, false);
		gdText.horizontalSpan = horisontalSpan - 1;
		text.setLayoutData(gdText);

		text.setText(getInputElement().getContent());
		text.setToolTipText(getDescription());
		if (constraint != null) {
			final ControlDecoration error = ControlDecorationToolkit.createErrorDecorator(text, false);
			error.hide();
			QuantityKindProposal.install(text, constraint);
			text.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					try {
						T value = constraint.parseInteractive(text.getText());
						error.hide();
						errorTracker.accept(this, null);
						setValue(constraint.persistableString(value));
						return;

					} catch (QuantityConversionException qce) {
						error.setDescriptionText(qce.getLocalizedMessage());
					}
					error.show();
					errorTracker.accept(this, error.getDescriptionText());
				}
			});
		} else {
			text.addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(ModifyEvent event) {
					setValue(text.getText());
				}
			});
		}
	}

	@Override
	public void create(
		FormToolkit toolkit, Composite parent, int horisontalSpan, BiConsumer<Object, String> errorTracker) {
		create(parent, horisontalSpan, errorTracker);
	}
}
