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

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Text;

import org.openjdk.jmc.rjmx.triggers.fields.internal.Field;
import org.openjdk.jmc.rjmx.triggers.fields.internal.QuantityField;
import org.openjdk.jmc.ui.misc.QuantityKindProposal;
import org.openjdk.jmc.ui.uibuilder.IUIBuilder;

public class QuantityInputItem extends TextInputItem {
	private QuantityKindProposal m_proposal;

	public QuantityInputItem(Field field, IUIBuilder builder) {
		super(field, builder);
	}

	@Override
	protected void createUI() {
		super.createUI();
		if (getField() instanceof QuantityField) {
			QuantityField qField = (QuantityField) getField();
			m_proposal = QuantityKindProposal.install(getText(), qField.getKind());
			m_proposal.addContentProposalListener(new IContentProposalListener2() {

				@Override
				public void proposalPopupOpened(ContentProposalAdapter adapter) {
				}

				@Override
				public void proposalPopupClosed(ContentProposalAdapter adapter) {
					if (!getText().isFocusControl()) {
						/*
						 * The text widget loses focus when the proposal popup opens but focus does
						 * not return to the text widget when the popup closes. Since we inhibit the
						 * focusLost behavior while proposal is open we need to commit the values
						 * now instead.
						 */
						if (!getField().setValue(getText().getText())) {
							getText().setText(getField().getValue());
						}
					}
				}
			});
		}
	}

	@Override
	public void onChange(Field freshField) {
		super.onChange(freshField);
		if (freshField instanceof QuantityField) {
			QuantityField qField = (QuantityField) freshField;
			m_proposal.setKind(qField.getKind());
		}
	}

	@Override
	protected FieldFocusListener createFieldFocusListener() {
		return new QuantityFieldFocusListener(getText(), getField());
	}

	private class QuantityFieldFocusListener extends FieldFocusListener {

		public QuantityFieldFocusListener(Text text, Field field) {
			super(text, field);
		}

		@Override
		public void focusLost(FocusEvent e) {
			if (!m_proposal.isPopupOpen()) {
				// Focus lost deactivates the field editor.
				// This must not happen if focus lost was caused by activating the completion proposal popup.
				m_proposal.setEnabled(false);
				super.focusLost(e);
				m_proposal.setEnabled(true);
			}
		}
	}
}
