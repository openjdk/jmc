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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.console.ui.notification.NotificationPlugin;
import org.openjdk.jmc.console.ui.notification.widget.ConditionChooser;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.condition.internal.TriggerCondition;
import org.openjdk.jmc.rjmx.triggers.fields.internal.Field;
import org.openjdk.jmc.rjmx.triggers.fields.internal.QuantityField;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.rjmx.triggers.internal.ValueEvaluatorNumberMax;
import org.openjdk.jmc.rjmx.triggers.internal.ValueEvaluatorStringMatch;
import org.openjdk.jmc.rjmx.ui.internal.AttributeSelectionContentModel;
import org.openjdk.jmc.rjmx.ui.internal.IAttributeSelectionContentListener;
import org.openjdk.jmc.ui.uibuilder.StandardUIBuilder;

/**
 * This is the wizard page for editing trigger conditions .
 */
public class TriggerConditionWizardPage extends WizardPage {

	public final static String PAGE_NAME = "org.openjdk.jmc.notification.trigger.condition"; //$NON-NLS-1$

	private final AttributeSelectionContentModel m_selectorModel;
	private final NotificationRegistry m_notificationModel;
	private final TriggerRule m_notificationRule;
	private final IConnectionHandle m_connectionHandle;
	private ConditionChooser m_conditionChooser;

	public TriggerConditionWizardPage(AttributeSelectionContentModel selectorModel, NotificationRegistry model,
			IConnectionHandle connectionHandle, TriggerRule notificationRule) {
		super(PAGE_NAME, Messages.TriggerConditionWizardPage_TITLE, null);
		setDescription(Messages.TriggerConditionWizardPage_DESCRIPTION);

		m_selectorModel = selectorModel;
		m_notificationModel = model;
		m_notificationRule = notificationRule;
		m_connectionHandle = connectionHandle;
	}

	@Override
	public void createControl(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		StandardUIBuilder suib = new StandardUIBuilder(control);

		if (m_notificationRule.getTrigger() instanceof TriggerCondition) {
			m_conditionChooser = new ConditionChooser(false, suib, m_notificationRule, m_notificationModel,
					m_connectionHandle, control);
			m_conditionChooser.select(m_notificationRule.getTrigger());
			m_selectorModel.addListener(new IAttributeSelectionContentListener() {

				@Override
				public void selectionChanged(AttributeSelectionContentModel selectorModel) {
					final MRI[] selectedAttributes = selectorModel.getSelectedAttributes();
					if (selectedAttributes.length > 0) {

						final IUnit unit = selectorModel.getAttributeUnit(selectedAttributes[0]);
						final TriggerCondition trigger = (TriggerCondition) m_notificationRule.getTrigger();

						m_notificationModel.performCriticalRuleChange(m_notificationRule, new Runnable() {

							@Override
							public void run() {
								m_notificationRule.getTrigger().setAttributeDescriptor(selectedAttributes[0]);
								if (unit != null) {
									IQuantity zeroQuantity = unit.quantity(0);
									// TODO: Allow choosing of min field
									trigger.setValueEvaluator(new ValueEvaluatorNumberMax(zeroQuantity));
									Field field = trigger.getFieldHolder()
											.getField(TriggerCondition.FIELD_EVAL_NUM_MAX);
									if (field instanceof QuantityField) {
										((QuantityField) field).initKind(unit.getContentType(),
												zeroQuantity.interactiveFormat(), null, null);
									}
									field.setValue(zeroQuantity.interactiveFormat());
								} else {
									trigger.setValueEvaluator(new ValueEvaluatorStringMatch("*")); //$NON-NLS-1$
								}
							}
						});

					}
				}
			});
		} else {
			NotificationPlugin.getDefault().getLogger().severe("Create control: wrong type!"); //$NON-NLS-1$
		}

		setControl(control);
	}

	@Override
	public void setVisible(boolean visble) {
		super.setVisible(visble);
		if (m_notificationRule.getTrigger() instanceof TriggerCondition) {
			m_conditionChooser.select(m_notificationRule.getTrigger());
		} else {
			NotificationPlugin.getDefault().getLogger().severe("Set visible: wrong type!"); //$NON-NLS-1$
		}
	}
}
