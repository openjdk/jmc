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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.util.logging.Level;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;

import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;

public class ExtraRowTableViewer extends TableViewer {

	private String message;
	private TableItem extraRow;

	// FIXME: Would like to decorate the extra row with a font, but the table we use this for uses ViewerColumns with their own labelproviders

	public ExtraRowTableViewer(Composite parent) {
		super(parent);
	}

	public ExtraRowTableViewer(Composite composite, int style) {
		super(composite, style);
	}

	public void setExtraMessage(String message) {
		this.message = message;
	}

	@Override
	public void refresh(Object element) {
		if (message == null) {
			super.refresh(element);
		} else {
			removeExtraRow();
			super.refresh(element);
		}
	}

	@Override
	protected void inputChanged(Object input, Object oldInput) {
		if (message == null) {
			super.inputChanged(input, oldInput);
		} else {
			removeExtraRow();
			super.inputChanged(input, oldInput);
			createExtraRow();
		}
	}

	@Override
	public void refresh() {
		if (message == null) {
			super.refresh();
		} else {
			removeExtraRow();
			super.refresh();
			createExtraRow();
		}
	}

	private void createExtraRow() {
		extraRow = new TableItem(getTable(), SWT.NO_BACKGROUND | SWT.NO_FOCUS);
		extraRow.setText(message);
	}

	private void removeExtraRow() {
		if (extraRow != null) {
			try {
				extraRow.dispose();
			} catch (Exception e) {
				FlightRecorderUI.getDefault().getLogger().log(Level.WARNING,
						"Exception while disposing extra row in table", //$NON-NLS-1$
						e);
			}
		}
	}
}
