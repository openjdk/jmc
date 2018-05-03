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
package org.openjdk.jmc.console.persistence.editors;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.ScrolledFormText;

import org.openjdk.jmc.console.persistence.PersistencePlugin;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;

/**
 * showing information about JMX persistence for new users.
 */
public class InformationSectionPart {
	public static void fill(Composite body, FormToolkit formToolkit) {
		ScrolledForm scrolledForm = formToolkit.createScrolledForm(body);
		Composite parent = scrolledForm.getForm().getBody();
		ScrolledFormText helpText = new ScrolledFormText(parent, SWT.V_SCROLL | SWT.H_SCROLL, true);
		helpText.setText(getInfo());
		helpText.setLayoutData(MCLayoutFactory.createFormPageLayoutData(470, 270, true, false));
		formToolkit.adapt(helpText);
		helpText.getFormText().addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
						Display.getCurrent().getActiveShell(), "org.openjdk.jmc.console.ui.preferences.PersistencePage", //$NON-NLS-1$
						null, null);
				dialog.open();
			}
		});

		Label imageLabel = formToolkit.createLabel(parent, ""); //$NON-NLS-1$
		imageLabel.setImage(
				PersistencePlugin.getDefault().getImage(PersistencePlugin.ICON_SCREENSHOT_ENABLE_PERSISTENCE));
		imageLabel.setLayoutData(MCLayoutFactory.createFormPageLayoutData(SWT.LEFT, SWT.FILL, 832, 244));
		imageLabel.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		formToolkit.paintBordersFor(parent);
		parent.setLayout(MCLayoutFactory.createFormPageLayout(15, 15));
		body.setLayout(new FillLayout());
	}

	private static String getInfo() {
		String s = "<form><p>"; //$NON-NLS-1$
		s += "<b>" + Messages.InformationSectionPart_WHAT_IS_PERSISTENCE_TITLE + "</b><br/><br/>"; //$NON-NLS-1$ //$NON-NLS-2$
		s += Messages.InformationSectionPart_PERSISTENCE_DESCRIPTION_BEGINNING_TEXT;
		s += "<br/><br/>"; //$NON-NLS-1$
		s += "<b>\u00a0\u2022\u00a0</b>" + Messages.InformationSectionPart_IT_IS_SEE_TRENDS_TEXT + "<br/>"; //$NON-NLS-1$ //$NON-NLS-2$
		s += "<b>\u00a0\u2022\u00a0</b>" + Messages.InformationSectionPart_IT_IS_OFFLINE_VIEW_TEXT + "<br/>"; //$NON-NLS-1$ //$NON-NLS-2$
		s += "<b>\u00a0\u2022\u00a0</b>" + Messages.InformationSectionPart_IT_IS_SHARE_DATA_TEXT + "<br/>"; //$NON-NLS-1$ //$NON-NLS-2$
		s += "<br/>"; //$NON-NLS-1$
		s += Messages.InformationSectionPart_PERSISTENCE_DESCRIPTION_END_TEXT;
		s += "<br/>"; //$NON-NLS-1$
		s += "<br/><b>" + Messages.InformationSectionPart_HOW_DO_I_USE_TITLE + "</b><br/><br/>"; //$NON-NLS-1$ //$NON-NLS-2$
		s += Messages.InformationSectionPart_HOW_DO_I_USE_DESCRIPTION_TEXT;
		s += "<br/>"; //$NON-NLS-1$
		s += "</p></form>"; //$NON-NLS-1$
		return s;
	}

}
