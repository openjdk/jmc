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

import java.util.Collection;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.console.ui.notification.NotificationPlugin;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;
import org.openjdk.jmc.rjmx.triggers.ITriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.condition.internal.TriggerCondition;
import org.openjdk.jmc.rjmx.triggers.extension.internal.TriggerFactory;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationTrigger;
import org.openjdk.jmc.rjmx.triggers.internal.ValueEvaluatorStringMatch;
import org.openjdk.jmc.rjmx.ui.internal.AttributeConfiguratorWizardPage;
import org.openjdk.jmc.rjmx.ui.internal.AttributeSelectionContentModel;
import org.openjdk.jmc.rjmx.ui.internal.AttributeSelectionViewModel;
import org.openjdk.jmc.rjmx.ui.internal.AttributeSelectorWizardPage;
import org.openjdk.jmc.rjmx.ui.internal.IAttributeSelectionContentListener;

/**
 * Wizard for creating and editing a notification rule
 */
public class RuleWizard extends Wizard {
	private final TriggerRule m_noticationRule;
	private final IConnectionHandle m_connectionHandle;
	private final NotificationRegistry m_notificationModel;
	private final String NAME_NOT_SET = Messages.RuleWizard_DEFAULT_RULE_NAME;
	private AttributeSelectionContentModel m_selectorModel;

	public RuleWizard(IConnectionHandle connectionHandle, TriggerRule rule, String ruleGroupName,
			NotificationRegistry notificationModel) {
		super();
		initializeDefaultPageImageDescriptor();

		m_connectionHandle = connectionHandle;
		setDialogSettings(NotificationPlugin.getDefault().getDialogSettings());
		if (rule == null) {
			m_noticationRule = createRule((TriggerFactory) notificationModel.getFactory());
			m_noticationRule.setRulePath(ruleGroupName);
			setWindowTitle(Messages.RuleWizard_WINDOW_TITLE);
		} else {
			m_noticationRule = rule;
			setWindowTitle(NLS.bind(Messages.RuleWizard_WINDOW_TITLE_EDIT, m_noticationRule.getName()));
		}
		m_notificationModel = notificationModel;
	}

	@Override
	public void addPages() {
		AttributeSelectionViewModel viewModel = new AttributeSelectionViewModel(null, false, false,
				Messages.RuleWizard_WINDOW_TITLE, Messages.AttributeSelectionWizardPage_TITLE,
				Messages.AttributeSelectionWizardPage_DESCRIPTION, Messages.AttributeConfigurationWizardPage_TITLE,
				Messages.AttributeConfigurationWizardPage_DESCRIPTION);
		m_selectorModel = new AttributeSelectionContentModel(m_connectionHandle.getServiceOrDummy(IMRIService.class),
				m_connectionHandle.getServiceOrDummy(IMRIMetadataService.class), null, null);

		addPage(new AttributeSelectorWizardPage(viewModel, m_selectorModel));
		addPage(new AttributeConfiguratorWizardPage(viewModel, m_selectorModel));
		addPage(new TriggerConditionWizardPage(m_selectorModel, m_notificationModel, m_connectionHandle,
				m_noticationRule));
		addPage(new ActionWizardPage(m_notificationModel, m_connectionHandle, m_noticationRule));
		addPage(new ConstraintWizardPage(m_notificationModel, m_noticationRule));
		addPage(new NameWizardPage(m_notificationModel, m_noticationRule));

		m_selectorModel.addListener(new IAttributeSelectionContentListener() {
			@Override
			public void selectionChanged(AttributeSelectionContentModel selectorModel) {
				MRI[] selectedAttributes = selectorModel.getSelectedAttributes();
				if (selectedAttributes.length > 0) {
					boolean linkConfigurePage = false;
					IMRIMetadataService metadataService = selectorModel.getMetadataService();
					for (MRI mri : selectedAttributes) {
						IMRIMetadata metadata = metadataService.getMetadata(mri);
						if (MRIMetadataToolkit.isNumerical(metadata)) {
							IUnit unit = UnitLookup.getUnitOrNull(metadata.getUnitString());
							if (unit == null) {
								linkConfigurePage = true;
							}
						}
					}
					if (linkConfigurePage) {
						getAttributeSelectorWizardPage().setNextPage(getAttributeConfiguratorWizardPage());
					} else {
						getAttributeSelectorWizardPage().setNextPage(getTriggerConditionWizardPage());
					}
				}
			}
		});
	}

	private AttributeSelectorWizardPage getAttributeSelectorWizardPage() {
		return (AttributeSelectorWizardPage) getPage(AttributeSelectorWizardPage.PAGE_NAME);
	}

	private AttributeConfiguratorWizardPage getAttributeConfiguratorWizardPage() {
		return (AttributeConfiguratorWizardPage) getPage(AttributeConfiguratorWizardPage.PAGE_NAME);
	}

	private TriggerConditionWizardPage getTriggerConditionWizardPage() {
		return (TriggerConditionWizardPage) getPage(TriggerConditionWizardPage.PAGE_NAME);
	}

	private TriggerRule createRule(TriggerFactory factory) {
		// create trigger
		TriggerCondition condition = (TriggerCondition) factory.createTrigger();
		condition.setValueEvaluator(new ValueEvaluatorStringMatch("*")); //$NON-NLS-1$
		if (condition.getFieldHolder() == null) {
			condition.initFieldHolder();
		}

		ITriggerAction action = null;

		// create action
		Collection<?> actions = factory.getActionExtensions().getPrototypes();
		// Try to select ApplicationAlet by default
		for (Object protoAction : actions) {
			String className = protoAction.getClass().getName();
			if (className.endsWith("ApplicationAlert")) //$NON-NLS-1$
			{
				try {
					action = factory.createAction(protoAction.getClass().getName());
				} catch (Exception e) {
					NotificationPlugin.getDefault().getLogger().warning(
							"Could not instantiate the default application alert action! Looking for any action next."); //$NON-NLS-1$
				}
			}
		}
		// no application alerts found, use the first found when discovering the
		// extension points.
		if (action == null && actions.size() > 0) {
			try {
				action = factory.createAction(actions.iterator().next().getClass().getName());
			} catch (Exception e) {
				NotificationPlugin.getDefault().getLogger()
						.severe("Could not instantiate any default action when creating the rule!"); //$NON-NLS-1$
			}
		}

		return new TriggerRule(NAME_NOT_SET, condition, action);
	}

	public TriggerRule getRule() {
		return m_noticationRule;
	}

	@Override
	public boolean performFinish() {
		m_selectorModel.commitUnitChanges();
		if (m_noticationRule.getName().equals(NAME_NOT_SET)) {
			m_noticationRule.setName(getSuggestedName());
		}

		return true;
	}

	public String getSuggestedName() {
		NotificationTrigger trigger = m_noticationRule.getTrigger();
		IMRIMetadataService manager = m_connectionHandle.getServiceOrDummy(IMRIMetadataService.class);
		IMRIMetadata info = manager.getMetadata(trigger.getAttributeDescriptor());
		String attributeName = MRIMetadataToolkit.getDisplayName(m_connectionHandle, info.getMRI());
		String evalCondition = trigger.getValueEvaluator().getEvaluationConditionString();
		String sugggestedName = attributeName + ' ' + evalCondition;
		for (int i = 2; !m_notificationModel.isNameAvailable(sugggestedName); i++) {
			sugggestedName = attributeName + ' ' + evalCondition + ' ' + i;
		}

		return sugggestedName;
	}

	private void initializeDefaultPageImageDescriptor() {
		setDefaultPageImageDescriptor(
				NotificationPlugin.getDefault().getMCImageDescriptor(NotificationPlugin.IMG_RULE_WIZRD));
	}
}
