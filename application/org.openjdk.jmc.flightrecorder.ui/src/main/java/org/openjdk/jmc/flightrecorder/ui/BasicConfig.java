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
package org.openjdk.jmc.flightrecorder.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IStateful;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
import org.openjdk.jmc.flightrecorder.ui.AttributeConfiguration.AttributeGroup;
import org.openjdk.jmc.flightrecorder.ui.AttributeConfiguration.GroupEntry;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.CoreImages;
import org.openjdk.jmc.ui.misc.QuantityKindProposal;
import org.openjdk.jmc.ui.wizards.IPerformFinishable;
import org.openjdk.jmc.ui.wizards.OnePageWizardDialog;

public class BasicConfig implements IStateful {

	private static class ValueEntry<T> {
		T value;
		TypedPreference<T> preference;

		boolean parsePersisted(String persistedValue) throws QuantityConversionException {
			T parsedValue = preference.getPersister().parsePersisted(persistedValue);
			if (value.equals(parsedValue)) {
				return false;
			}
			value = parsedValue;
			return true;
		}

		boolean parseInteractive(String interactiveValue) throws QuantityConversionException {
			T parsedValue = preference.getPersister().parseInteractive(interactiveValue);
			if (value.equals(parsedValue)) {
				return false;
			}
			value = parsedValue;
			return true;
		}

		String persistableString() {
			return preference.getPersister().persistableString(value);
		}

		String interactiveFormat() {
			return preference.getPersister().interactiveFormat(value);
		}

	}

	public static final String STATE_ID = "config-attribute"; //$NON-NLS-1$
	private static final String KEY_ATTRIBUTE = "key"; //$NON-NLS-1$
	private static final String VALUE_ATTRIBUTE = "value"; //$NON-NLS-1$
	private final Map<String, ValueEntry<?>> entries = new HashMap<>();
	private Map<String, Collection<IRule>> prefRuleMap = new HashMap<>();

	public BasicConfig(IState state) {
		Collection<IRule> rules = RuleRegistry.getRules();
		for (IRule rule : rules) {
			for (TypedPreference<?> pref : rule.getConfigurationAttributes()) {
				prefRuleMap.computeIfAbsent(pref.getIdentifier(), k -> new ArrayList<>()).add(rule);
				entries.put(pref.getIdentifier(), createValueEntry(pref));
			}
		}
		update(state);
	}

	public Set<IRule> update(IState newState) {
		List<String> configurationIDs = new ArrayList<>();
		if (newState != null) {
			for (IState config : newState.getChildren(STATE_ID)) {
				String key = config.getAttribute(KEY_ATTRIBUTE);
				String value = config.getAttribute(VALUE_ATTRIBUTE);
				if (key == null || value == null) {
					FlightRecorderUI.getDefault().getLogger().warning("Key or value not specified"); //$NON-NLS-1$
				} else {
					ValueEntry<?> ve = entries.get(key);
					if (ve == null) {
						FlightRecorderUI.getDefault().getLogger().warning("Attribute for " + key + " is missing"); //$NON-NLS-1$ //$NON-NLS-2$
					} else {
						try {
							boolean parsePersisted = ve.parsePersisted(value);
							if (parsePersisted) {
								configurationIDs.add(key);
							}
						} catch (QuantityConversionException e) {
							FlightRecorderUI.getDefault().getLogger()
									.warning("Value '" + value + "' is not a valid for " + key); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				}
			}
		}
		return getRulesByConfigurations(configurationIDs);
	}

	@SuppressWarnings("unchecked")
	<T> T getValue(TypedPreference<T> attr) {
		ValueEntry<?> ve = entries.get(attr.getIdentifier());
		if (ve != null) {
			return (T) ve.value;
		}
		return attr.getDefaultValue();
	}

	@Override
	public void saveTo(IWritableState state) {
		for (ValueEntry<?> ve : entries.values()) {
			IWritableState c = state.createChild(STATE_ID);
			c.putString(KEY_ATTRIBUTE, ve.preference.getIdentifier());
			c.putString(VALUE_ATTRIBUTE, ve.persistableString());
		}
	}

	public IAction createOpenConfigAction(AttributeConfiguration pageConfiguration, Consumer<Set<IRule>> finisher) {
		return new Action(Messages.CONFIGURATION_EDIT_ACTION, CoreImages.TABLE_SETTINGS) {
			@Override
			public void run() {
				ConfigPage page = new ConfigPage(pageConfiguration.getTitle(), pageConfiguration.getDescription(),
						pageConfiguration.getPageAttributes());
				if (OnePageWizardDialog.open(page, 500, 400) == Window.OK) {
					List<String> changed = page.getChanged();
					Set<IRule> rules = getRulesByConfigurations(changed);
					finisher.accept(rules);
				}
			}
		};
	}

	private Set<IRule> getRulesByConfigurations(List<String> configurationAttributes) {
		Set<IRule> rules = new HashSet<>();
		for (String id : configurationAttributes) {
			rules.addAll(prefRuleMap.get(id));
		}
		return rules;
	}

	private class ConfigPage extends WizardPage implements IPerformFinishable {

		private final List<AttributeGroup> groups;
		private List<String> changedPreferences;

		protected ConfigPage(String title, String description, List<AttributeGroup> groups) {
			super("BasicConfigPage"); //$NON-NLS-1$
			setTitle(title);
			setDescription(description);
			this.groups = groups;
			changedPreferences = new ArrayList<>();
		}

		List<String> getChanged() {
			return changedPreferences;
		}

		@Override
		public void createControl(Composite parent) {
			ScrolledComposite container = new ScrolledComposite(parent, SWT.V_SCROLL);
			container.setExpandHorizontal(true);
			container.setExpandVertical(true);
			container.setLayout(new GridLayout(1, false));
			container.setShowFocusedControl(true);
			Composite configContainer = new Composite(container, SWT.NONE);
			container.setContent(configContainer);
			configContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			configContainer.setLayout(new GridLayout(1, false));
			for (AttributeGroup group : groups) {
				Group attributeGroup = new Group(configContainer, SWT.NONE);
				attributeGroup.setText(group.getTitle());
				attributeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				attributeGroup.setLayout(new GridLayout(2, false));
				for (GroupEntry entry : group.getEntries()) {
					TypedPreference<?> preference = entry.getPreference();
					Label labelText = new Label(attributeGroup, SWT.READ_ONLY);
					labelText.setText(entry.getName());
					labelText.setToolTipText(preference.getDescription());
					Text text = new Text(attributeGroup, SWT.NONE);
					text.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
					ValueEntry<?> valueEntry = entries.get(preference.getIdentifier());
					text.setData(valueEntry);
					text.setText(valueEntry.interactiveFormat());
					text.setToolTipText(preference.getDescription());
					QuantityKindProposal.install(text, preference.getPersister());
				}
			}
			container.setMinSize(configContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			setControl(container);
		}

		@Override
		public boolean performFinish() {
			Control container = ((Composite) getControl()).getChildren()[0];
			for (Control group : ((Composite) container).getChildren()) {
				for (Control child : ((Composite) group).getChildren()) {
					try {
						if (child instanceof Text) {
							Text text = (Text) child;
							ValueEntry<?> ve = (ValueEntry<?>) (text.getData());
							boolean valueChanged = ve.parseInteractive(text.getText());
							if (valueChanged) {
								changedPreferences.add(ve.preference.getIdentifier());
							}
						}
					} catch (QuantityConversionException e) {
						setErrorMessage(e.getLocalizedMessage());
						return false;
					}
				}
			}
			return true;
		}
	}

	private static <T> ValueEntry<T> createValueEntry(TypedPreference<T> preference) {
		ValueEntry<T> ve = new ValueEntry<>();
		ve.preference = preference;
		ve.value = preference.getDefaultValue();
		return ve;
	}

}
