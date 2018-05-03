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
package org.openjdk.jmc.console.ui.notification.wizard;

import java.util.HashSet;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.ui.uibuilder.StandardUIBuilder;

/**
 * This is the wizard page for editing trigger conditions .
 */
public class NameWizardPage extends WizardPage {

	public final static String PAGE_NAME = "org.openjdk.jmc.notification.trigger.name"; //$NON-NLS-1$

	private final TriggerRule m_notificationRule;
	private Text text;
	private final String m_defaultName;
	private final NotificationRegistry notificationModel;

	/**
	 * Creates a page for editing the supplied model.
	 *
	 * @param model
	 *            the model to edit.
	 */
	public NameWizardPage(NotificationRegistry notificationModel, TriggerRule rule) {
		super(PAGE_NAME, Messages.NameWizardPage_TITLE, null);
		this.notificationModel = notificationModel;
		setDescription(Messages.NameWizardPage_DESCRIPTION);
		m_defaultName = rule.getName() == null ? Messages.NameWizardPage_DEFAULT_TRIGGER_NAME : rule.getName();
		m_notificationRule = rule;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (visible && text.getText().equals(m_defaultName)) {
			text.setText(((RuleWizard) getWizard()).getSuggestedName());
		}
	}

	@Override
	public void createControl(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);

		StandardUIBuilder suib = new StandardUIBuilder(c);

		suib.createLabel(Messages.NameWizardPage_RULE_GROUP_NAME_TEXT, null);
		final CCombo combo = suib.createCombo();

		final HashSet<String> names = new HashSet<>();
		HashSet<String> set = new HashSet<>();
		// Collect rules
		for (TriggerRule rule : notificationModel.getAvailableRules()) {
			set.add(rule.getRulePath());
			names.add(rule.getName());
		}

		// Add this rule group if not already added
		if (!set.contains(m_notificationRule.getRulePath())) {
			combo.add(m_notificationRule.getRulePath());
		}

		// Add items to ComboBox
		for (String path : set) {
			combo.add(path);
		}

		// Select the rule group of the edited rule
		for (int i = 0; i < combo.getItems().length; i++) {
			if (m_notificationRule.getRulePath().equals(combo.getItem(i))) {
				combo.select(i);
			}
		}

		combo.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				// ignore
			}

			@Override
			public void keyReleased(KeyEvent e) {
				m_notificationRule.setRulePath(combo.getText());
			}
		});
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = combo.getSelectionIndex();
				if (index >= 0) {
					m_notificationRule.setRulePath(combo.getText());
				}
			}
		});

		suib.layout();
		suib.createLabel(Messages.NameWizardPage_CAPTION_RULE_NAME_TEXT, null);
		text = suib.createText(m_defaultName, "", SWT.NONE); //$NON-NLS-1$

		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (e.getSource() instanceof Text) {
					Text t = (Text) e.getSource();
					String text = t.getText();
					if (names.contains(text)) {
						setPageComplete(false);
						setErrorMessage(
								org.openjdk.jmc.console.ui.notification.tab.Messages.TriggerSectionPart_DIALOG_RULE_EXISTS_MESSAGE_TEXT);
					} else {
						setPageComplete(true);
						setErrorMessage(null);
						m_notificationRule.setName(text);
					}
				}
			}
		});
		// Description header
		suib.layout();
		suib.createLabel(Messages.NameWizardPage_TRIGGER_DESCRIPTION_TEXT, null);

		suib.layout();
		String desc = m_notificationRule.getDescription() == null ? "" : m_notificationRule.getDescription(); //$NON-NLS-1$
		text = suib.createMultiText(desc, ""); //$NON-NLS-1$
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (e.getSource() instanceof Text) {
					Text t = (Text) e.getSource();
					m_notificationRule.setDescription(t.getText());
				}
			}
		});

		suib.layout();
		setControl(c);
	}

}
