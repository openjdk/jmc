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
package org.openjdk.jmc.console.ui.notification.widget;

import java.util.HashMap;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.console.ui.notification.uicomponents.FieldRenderer;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.triggers.ITrigger;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.condition.internal.TriggerCondition;
import org.openjdk.jmc.rjmx.triggers.fields.internal.Field;
import org.openjdk.jmc.rjmx.triggers.fields.internal.FieldHolder;
import org.openjdk.jmc.rjmx.triggers.fields.internal.QuantityField;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.rjmx.triggers.internal.ValueEvaluatorNumberMax;
import org.openjdk.jmc.rjmx.triggers.internal.ValueEvaluatorNumberMin;
import org.openjdk.jmc.rjmx.triggers.internal.ValueEvaluatorStringMatch;
import org.openjdk.jmc.rjmx.ui.internal.AttributeSelectionViewModel;
import org.openjdk.jmc.rjmx.ui.internal.AttributeSelectorWizardDialog;
import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer;
import org.openjdk.jmc.ui.uibuilder.IUIBuilder;

/*******************************************************************************
 * Class responsible for attribute selection/value evaluator
 */
public class ConditionChooser {
	public class AttributeSelectionListener implements SelectionListener {
		@Override
		public void widgetSelected(SelectionEvent e) {
			IMRIService mriService = m_connectionHandle.getServiceOrDummy(IMRIService.class);
			IMRIMetadataService metadataService = m_connectionHandle.getServiceOrDummy(IMRIMetadataService.class);
			MRI[] attributes = new MRI[] {m_notificationRule.getTrigger().getAttributeDescriptor()};
			AttributeSelectionViewModel viewModel = new AttributeSelectionViewModel(null, false, false,
					Messages.ConditionChooser_SELECTION_DIALOG_TITLE,
					Messages.ConditionChooser_ATTRIBUTE_SELECTION_TITLE,
					Messages.ConditionChooser_ATTRIBUTE_SELECTION_DESCRIPTION,
					Messages.ConditionChooser_ATTRIBUTE_CONFIGURATION_TITLE,
					Messages.ConditionChooser_ATTRIBUTE_CONFIGURATION_DESCRIPTION);
			AttributeSelectorWizardDialog dialog = new AttributeSelectorWizardDialog(
					Display.getCurrent().getActiveShell(), viewModel);
			if (dialog.open(mriService, metadataService, attributes, attributes) != Window.OK) {
				return;
			}
			MRI[] selectedAttributes = dialog.getSelectedAttributes();
			if (selectedAttributes.length == 1) {
				final MRI selectedAttribute = selectedAttributes[0];
				final TriggerCondition trigger = (TriggerCondition) m_notificationRule.getTrigger();

				m_notificationModel.performCriticalRuleChange(m_notificationRule, new Runnable() {
					@Override
					public void run() {
						trigger.setAttributeDescriptor(selectedAttribute);
					}
				});
				updateUnit();

				String s = selectedAttribute.getDataPath();
				m_text.setText(s);
				m_pathText.setText(getPath(m_parent, selectedAttribute));
				updateSubscription();
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}

	static class CurrentValueUpdater implements IMRIValueListener {
		private final Text m_valueText;
		private IUnit unit;

		public CurrentValueUpdater(Text text) {
			m_valueText = text;
		}

		public void setUnit(IUnit unit) {
			this.unit = unit;
		}

		@Override
		public void valueChanged(final MRIValueEvent event) {
			if (!m_valueText.isDisposed()) {
				m_valueText.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (!m_valueText.isDisposed()) {
							if (unit != null && event.getValue() instanceof Number) {
								m_valueText.setText(KindOfQuantity.format((Number) event.getValue(), unit));
							} else {
								m_valueText.setText(String.valueOf(event.getValue()));
							}
						}
					}
				});
			}
			;
		};
	}

	private final StackLayout m_stackLayout;
	private Composite m_conditionContainer;
	private final IUIBuilder m_builder;
	private final HashMap<Class<?>, Composite> m_controlEvaluatorMapping = new HashMap<>();
	private Text m_text;
	private final IConnectionHandle m_connectionHandle;
	private final boolean m_showAttributeSelector;
	private final TriggerRule m_notificationRule;
	private CurrentValueUpdater m_currentValueUpdater;
	private Text m_pathText;
	private final Composite m_parent;
	private KindOfQuantity<?> m_currentKind;
	private final NotificationRegistry m_notificationModel;

	public ConditionChooser(boolean showAttributeSelector, IUIBuilder builder, TriggerRule rule,
			NotificationRegistry model, IConnectionHandle connectionHandle, Composite parent) {
		m_showAttributeSelector = showAttributeSelector;
		m_builder = builder;
		m_connectionHandle = connectionHandle;
		m_stackLayout = new StackLayout();
		m_notificationRule = rule;
		m_notificationModel = model;
		m_parent = parent;

		create(parent);
		initializeUnit();
		select(m_notificationRule.getTrigger());
	}

	public void select(ITrigger trigger) {
		Control c = m_controlEvaluatorMapping.get(trigger.getValueEvaluator().getClass());
		getStackLayout().topControl = c;

		m_conditionContainer.layout();
	}

	private void create(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		parent.setLayout(layout);

		if (m_showAttributeSelector) {
			GridData gdAttribute = new GridData(SWT.FILL, SWT.FILL, true, false);
			Composite pathName = createAttributeSelector(parent);
			pathName.setLayoutData(gdAttribute);
		}

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false);
		m_conditionContainer = getUIBuilder().createComposite(parent);
		getUIBuilder().setContainer(m_conditionContainer);
		createStacks(m_conditionContainer);
		m_conditionContainer.setLayoutData(gd2);

		updateSubscription();
	}

	private void updateSubscription() {
		MRI descriptor = m_notificationRule.getTrigger().getAttributeDescriptor();
		ISubscriptionService attributeSubscriptionService = m_connectionHandle
				.getServiceOrDummy(ISubscriptionService.class);

		attributeSubscriptionService.removeMRIValueListener(m_currentValueUpdater);
		if (m_showAttributeSelector && descriptor != null) {
			attributeSubscriptionService.addMRIValueListener(descriptor, m_currentValueUpdater);
		}
	}

	private Composite createCurrentValue(Composite container) {
		getUIBuilder().createLabel(Messages.ConditionChooser_CURRENT_TRIGGER_VALUE_TEXT, null);
		Text text = getUIBuilder().createText(Messages.ConditionChooser_TRIGGER_VALUE_NOT_UPDATED, "", SWT.NONE); //$NON-NLS-1$
		text.setEditable(false);
		m_currentValueUpdater = new CurrentValueUpdater(text);
		getUIBuilder().layout();

		return container;
	}

	String getPath(Composite tempComposite, MRI selectedAttribute) {
		String path = MBeanPropertiesOrderer.getMBeanPath(selectedAttribute.getObjectName());
		return (path == null) ? "" : path; //$NON-NLS-1$
	}

	private Composite createAttributeSelector(Composite parent) {
		Composite attributeSelectorContainer = getUIBuilder().createComposite(parent);
		getUIBuilder().setContainer(attributeSelectorContainer);
		String name = Messages.AttributeSelectionSectionPart_UNKNOWN_NAME_TEXT;
		String description = null;
		String path = ""; //$NON-NLS-1$

		IMRIMetadataService manager = m_connectionHandle.getServiceOrDummy(IMRIMetadataService.class);
		IMRIMetadata info = manager.getMetadata(m_notificationRule.getTrigger().getAttributeDescriptor());
		if (info != null) {
			name = info.getMRI().getDataPath();
			description = info.getDescription();
			path = getPath(parent, info.getMRI());
		}

		getUIBuilder().createLabel(Messages.ConditionChooser_MBEAN_PATH_LABEL_TEXT, null);
		m_pathText = getUIBuilder().createText(path, "", SWT.NONE); //$NON-NLS-1$
		m_pathText.setEditable(false);
		getUIBuilder().layout();

		getUIBuilder().createLabel(Messages.ConditionChooser_LABEL_ATTRIBUTE, null);
		m_text = getUIBuilder().createText(name, description, SWT.NONE);
		m_text.setEditable(false);
		Button button = getUIBuilder().createButton(Messages.ConditionChooser_BUTTON_BROWSE_TEXT,
				Messages.ConditionChooser_BUTTON_BROWSE_TOOLTIP);
		getUIBuilder().layout();
		button.addSelectionListener(new AttributeSelectionListener());
		createCurrentValue(attributeSelectorContainer);
		return attributeSelectorContainer;
	}

	private StackLayout getStackLayout() {
		return m_stackLayout;
	}

	private IUIBuilder getUIBuilder() {
		return m_builder;
	}

	private void createStacks(Composite container) {
		container.setLayout(m_stackLayout);

		FieldHolder stringMatch = new FieldHolder();
		FieldHolder numberMax = new FieldHolder();
		FieldHolder numberMin = new FieldHolder();

		// FIXME: Refactor to avoid casting.
		Field[] tempFields = ((TriggerCondition) m_notificationRule.getTrigger()).getFieldHolder().getFields();
		for (Field field : tempFields) {
			if (field.getId().equals(TriggerCondition.FIELD_EVAL_NUM_MAX)) {
				numberMax.addField(field);
			} else if (field.getId().equals(TriggerCondition.FIELD_EVAL_NUM_MIN)) {
				numberMin.addField(field);
			} else if (field.getId().equals(TriggerCondition.FIELD_EVAL_STRING)) {
				stringMatch.addField(field);
			} else {
				numberMax.addField(field);
				numberMin.addField(field);
				stringMatch.addField(field);
			}
		}
		createStack(ValueEvaluatorNumberMax.class, container, numberMax);
		createStack(ValueEvaluatorNumberMin.class, container, numberMin);
		createStack(ValueEvaluatorStringMatch.class, container, stringMatch);
	}

	private Composite createStack(Class<?> clazz, Composite container, FieldHolder fieldHolder) {
		Composite stackContainer = getUIBuilder().createComposite(container);
		getUIBuilder().setContainer(stackContainer);
		FieldRenderer tcr = new FieldRenderer(fieldHolder, getUIBuilder());
		tcr.render();
		m_controlEvaluatorMapping.put(clazz, stackContainer);
		return stackContainer;
	}

	private void initializeUnit() {
		TriggerCondition trigger = (TriggerCondition) m_notificationRule.getTrigger();
		if (trigger.getAttributeDescriptor() != null) {
			Field field = null;
			if (trigger.getValueEvaluator() instanceof ValueEvaluatorNumberMin) {
				field = trigger.getFieldHolder().getField(TriggerCondition.FIELD_EVAL_NUM_MIN);
			} else if (trigger.getValueEvaluator() instanceof ValueEvaluatorNumberMax) {
				field = trigger.getFieldHolder().getField(TriggerCondition.FIELD_EVAL_NUM_MAX);
			}
			if (field instanceof QuantityField) {
				QuantityField qField = (QuantityField) field;
				m_currentKind = qField.getKind();
			} else {
				m_currentKind = null;
			}
			updateUnit();
		}
	}

	public void updateUnit() {
		final TriggerCondition trigger = (TriggerCondition) m_notificationRule.getTrigger();
		IMRIMetadataService metadataService = m_connectionHandle.getServiceOrDummy(IMRIMetadataService.class);
		IMRIMetadata attribute = metadataService.getMetadata(trigger.getAttributeDescriptor());
		IUnit unit = null;
		if (MRIMetadataToolkit.isNumerical(attribute)) {
			unit = UnitLookup.getUnitOrDefault(attribute.getUnitString());
		}
		if (unit != null) {
			if (!unit.getContentType().equals(m_currentKind)) {
				IQuantity zeroQuantity = unit.quantity(0);

				if (trigger.getValueEvaluator() instanceof ValueEvaluatorNumberMin) {
					QuantityField field = (QuantityField) trigger.getFieldHolder()
							.getField(TriggerCondition.FIELD_EVAL_NUM_MIN);
					field.setKind(unit.getContentType());
				} else if (trigger.getValueEvaluator() instanceof ValueEvaluatorNumberMax) {
					QuantityField field = (QuantityField) trigger.getFieldHolder()
							.getField(TriggerCondition.FIELD_EVAL_NUM_MAX);
					field.setKind(unit.getContentType());
				} else {
					// Switch to a max value evaluator
					String currentValue = getCurrentField(trigger).getValue();
					QuantityField field = (QuantityField) trigger.getFieldHolder()
							.getField(TriggerCondition.FIELD_EVAL_NUM_MAX);
					trigger.setValueEvaluator(new ValueEvaluatorNumberMax(zeroQuantity));
					field.setKind(unit.getContentType());
					field.setUncheckedValue(currentValue);
				}
			}
		} else if (m_currentKind != null) {
			// Switch to a string evaluator
			String currentValue = getCurrentField(trigger).getValue();
			trigger.setValueEvaluator(new ValueEvaluatorStringMatch("*")); //$NON-NLS-1$
			Field field = trigger.getFieldHolder().getField(TriggerCondition.FIELD_EVAL_STRING);
			field.setValue(currentValue);
		}
		getCurrentField(trigger).updateListener();

		if (m_currentValueUpdater != null) {
			m_currentValueUpdater.setUnit(unit);
		}
		m_currentKind = unit != null ? unit.getContentType() : null;
		select(trigger);
	}

	private Field getCurrentField(TriggerCondition trigger) {
		if (trigger.getValueEvaluator() instanceof ValueEvaluatorNumberMin) {
			return trigger.getFieldHolder().getField(TriggerCondition.FIELD_EVAL_NUM_MIN);
		}
		if (trigger.getValueEvaluator() instanceof ValueEvaluatorNumberMax) {
			return trigger.getFieldHolder().getField(TriggerCondition.FIELD_EVAL_NUM_MAX);
		}
		return trigger.getFieldHolder().getField(TriggerCondition.FIELD_EVAL_STRING);
	}
}
