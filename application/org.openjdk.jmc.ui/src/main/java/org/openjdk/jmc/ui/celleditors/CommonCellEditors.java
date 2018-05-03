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
package org.openjdk.jmc.ui.celleditors;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.openjdk.jmc.common.unit.IPersister;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.RangeContentType;
import org.openjdk.jmc.ui.misc.ControlDecorationToolkit;
import org.openjdk.jmc.ui.misc.QuantityKindProposal;

// FIXME: JMC-5904 - Should be merged with cell editors in org.openjdk.jmc.rjmx.ui.celleditors
public class CommonCellEditors {

	public static class MultiLineTextCellEditor extends TextCellEditor {
		private final ControlDecoration infoDecorator;

		public MultiLineTextCellEditor(Composite parent) {
			super(parent, SWT.MULTI | SWT.V_SCROLL);
			infoDecorator = ControlDecorationToolkit.createInfoDecorator(text);
		}

		@Override
		public void activate() {
			super.activate();
			String info = getInfoText();
			if (info != null) {
				infoDecorator.setDescriptionText(info);
				infoDecorator.show();
			} else {
				infoDecorator.hide();
			}
		}

		public String getInfoText() {
			return Messages.MultiLineTextCellEditor_SHIFT_RETURN;
		}

		@Override
		protected void keyReleaseOccured(KeyEvent keyEvent) {
			if (keyEvent.character == '\u001b') { // Escape character
				fireCancelEditor();
			} else if (keyEvent.character == '\r' && (keyEvent.stateMask & SWT.SHIFT) == 0) { // Return key
				fireApplyEditorValue();
				deactivate();
			}
		}
	}

	private static class PersisterCellEditor extends MultiLineTextCellEditor {
		private final ControlDecoration errorDecorator;
		private final IPersister<?> persister;
		private Object value;

		PersisterCellEditor(Composite parent, IPersister<?> persister) {
			super(parent);
			this.persister = persister;
			errorDecorator = ControlDecorationToolkit.createErrorDecorator(text);
		}

		@Override
		public void activate() {
			super.activate();
			errorDecorator.hide();
		}

		@Override
		protected void editOccured(ModifyEvent e) {
			try {
				String str = text.getText();
				value = persister.parseInteractive(str);
				setValueValid(true);
				errorDecorator.hide();
			} catch (QuantityConversionException ex) {
				errorDecorator.setDescriptionText(ex.getLocalizedMessage());
				errorDecorator.show();
				setValueValid(false);
			}
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		protected void doSetValue(Object value) {
			this.value = value;
			super.doSetValue(((IPersister) persister).interactiveFormat(value != null ? value : "")); //$NON-NLS-1$
		}

		@Override
		protected Object doGetValue() {
			return value;
		}
	}

	private static class QuantityCellEditor extends PersisterCellEditor {
		private QuantityKindProposal proposal;

		private QuantityCellEditor(Composite parent, KindOfQuantity<?> kind) {
			super(parent, kind.getPersister());
			proposal = QuantityKindProposal.install(text, kind);
		}

		@Override
		protected void focusLost() {
			if (!proposal.isPopupOpen()) {
				// Focus lost deactivates the cell editor.
				// This must not happen if focus lost was caused by activating the completion proposal popup.
				super.focusLost();
			}
		}

		@Override
		protected boolean dependsOnExternalFocusListener() {
			// Always return false.
			// Otherwise, the ColumnViewerEditor will install an additional focus listener that cancels cell editing on
			// focus lost, even if focus gets lost due to activation of the completion proposal popup.
			// See also https://bugs.eclipse.org/bugs/show_bug.cgi?id=58777
			return false;
		}
	}

	public static CellEditor create(Composite parent, IPersister<?> persister) {
		return new PersisterCellEditor(parent, persister);
	}

	public static CellEditor create(Composite parent, KindOfQuantity<?> kind) {
		return new QuantityCellEditor(parent, kind);
	}

	public static CellEditor create(Composite parent, RangeContentType<?> rangeType) {
		// FIXME: Is this possible to implement as a single CellEditor, or do we need to use two cells, horizontally or vertically?
		return null;
	}
}
