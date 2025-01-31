/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023, 2025, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.flamegraph.views;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.forms.widgets.FormText;

/**
 * This tool tip extends the Jface implementation and relies on the {@link FormText} control to
 * render the text.
 * 
 * @author brice.dutheil
 * @see FormText
 */
public class StyledToolTip extends DefaultToolTip {
	public StyledToolTip(Control control) {
		super(control);
	}

	public StyledToolTip(Control control, int style, boolean manualActivation) {
		super(control, style, manualActivation);
	}

	@Override
	protected Composite createToolTipContentArea(Event event, Composite parent) {
		final Composite container = setDefaultLayout(new Composite(parent, SWT.NULL), event);
		GridLayoutFactory.fillDefaults().margins(2, 2).generateLayout(container);
		FormText formText = setDefaultLayout(new FormText(container, SWT.NONE), event);

		String pseudoHtml = getText(event);

		formText.setText(pseudoHtml, true, false);
		return parent;
	}

	private <T extends Control> T setDefaultLayout(T control, Event event) {
		control.setBackground(getBackgroundColor(event));
		control.setForeground(getForegroundColor(event));
		control.setFont(getFont(event));
		return control;
	}
}
