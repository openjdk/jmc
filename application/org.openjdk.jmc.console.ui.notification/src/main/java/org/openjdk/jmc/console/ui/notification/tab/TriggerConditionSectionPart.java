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
package org.openjdk.jmc.console.ui.notification.tab;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.console.ui.notification.widget.ConditionChooser;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.uibuilder.FormToolkitBuilder;

/**
 */
public class TriggerConditionSectionPart {

	private final Observer m_conditionMetadataObserver;
	private final IMRIMetadataService m_metadataService;

	public TriggerConditionSectionPart(Composite parent, FormToolkit toolkit, NotificationRegistry model,
			TriggerRule rule, IConnectionHandle ch) {

		Label d = toolkit.createLabel(parent, Messages.TriggerConditionSectionPart_DESCRIPTION_TITLE, SWT.NONE);
		d.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		d.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));

		Label l1 = toolkit.createLabel(parent, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
		l1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		if (rule.getDescription() != null) {
			FormText t = createFormTextDescription(parent, toolkit, rule);
			t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}

		Label l2 = toolkit.createLabel(parent, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
		l2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Composite condition = toolkit.createComposite(parent);
		FormToolkitBuilder fuib = new FormToolkitBuilder(toolkit, condition);
		final ConditionChooser conditionChooser = new ConditionChooser(true, fuib, rule, model, ch, condition);
		conditionChooser.select(rule.getTrigger());
		m_conditionMetadataObserver = new Observer() {

			@Override
			public void update(Observable o, Object arg) {
				DisplayToolkit.safeAsyncExec(conditionChooser::updateUnit);
			}
		};
		m_metadataService = ch.getServiceOrDummy(IMRIMetadataService.class);
		condition.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				m_metadataService.deleteObserver(m_conditionMetadataObserver);
			}
		});
		m_metadataService.addObserver(m_conditionMetadataObserver);
		condition.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	private static FormText createFormTextDescription(Composite parent, FormToolkit toolkit, TriggerRule rule) {
		String r = rule.getDescription();

		String result = htmlify(r);
		FormText text = toolkit.createFormText(parent, false);
		try {
			text.setText("<form><p>" + result + "</p></form>", true, false); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (RuntimeException exp) {
			text.setText(NLS.bind(Messages.TriggerConditionSectionPart_ERROR_MESSAGE_COULD_NOT_PARSE_DESCRIPTION, r),
					false, false);
		}

		return text;
	}

	private static String htmlify(String r) {
		StringBuilder resultBuilder = new StringBuilder();
		for (int n = 0; n < r.length(); n++) {
			resultBuilder.append(transform(r.charAt(n)));
		}
		return resultBuilder.toString();
	}

	private static String transform(char c) {
		if (c == '\r') {
			return "<br/>"; //$NON-NLS-1$
		}
		if (c == '\n') {
			return ""; //$NON-NLS-1$
		}
		return Character.toString(c);
	}
}
