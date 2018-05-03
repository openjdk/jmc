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
package org.openjdk.jmc.ui.misc;

import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class CompositeToolkit {

	public static ScrolledComposite createVerticalScrollComposite(Composite parent) {
		ScrolledComposite scrolled = new ScrolledComposite(parent, SWT.V_SCROLL);
		scrolled.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				Control content = scrolled.getContent();
				if (DisplayToolkit.isSafe(content)) {
					scrolled.setMinHeight(content.computeSize(scrolled.getClientArea().width, SWT.DEFAULT).y);
				}
			}
		});
		scrolled.setExpandVertical(true);
		scrolled.setExpandHorizontal(true);
		return scrolled;
	}

	public static FormText createInfoFormText(Composite parent) {
		Display display = parent.getDisplay();
		FormText formText = new FormText(parent, SWT.WRAP);
		formText.marginHeight = 5;
		formText.marginWidth = 5;
		formText.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		formText.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		return formText;
	}

	public static ProgressIndicator createWaitIndicator(Composite parent, FormToolkit toolkit) {
		parent.setLayout(new GridLayout());
		Composite centerComposite = toolkit.createComposite(parent, SWT.NONE);
		centerComposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		centerComposite.setLayout(new GridLayout());
		ProgressIndicator progressIndicator = new ProgressIndicator(centerComposite);
		progressIndicator.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		toolkit.adapt(progressIndicator);
		progressIndicator.beginAnimatedTask();
		Label label = toolkit.createLabel(centerComposite, Messages.ProgressComposite_PLEASE_WAIT);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		return progressIndicator;

	}

	public static Section createSection(Composite parent, FormToolkit toolkit, String text) {
		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
		section.setText(text);
		toolkit.adapt(section);
		return section;
	}
}
