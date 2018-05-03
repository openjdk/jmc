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
package org.openjdk.jmc.flightrecorder.ui.preferences;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.xml.sax.SAXException;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.ui.AttributeConfiguration;
import org.openjdk.jmc.flightrecorder.ui.BasicConfig;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.JfrEditor;
import org.openjdk.jmc.flightrecorder.ui.RulesUiToolkit;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;

public class RulesPage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String STATE = "state"; //$NON-NLS-1$
	public static final String RULE_ID = "id"; //$NON-NLS-1$
	public static final String IGNORED_RULES = "ignoredRules"; //$NON-NLS-1$
	private Tree topicTree;

	private Font topicFont;
	private BasicConfig config;
	private Set<String> ignoredRules;

	public RulesPage() {
		super();
		setPreferenceStore(FlightRecorderUI.getDefault().getPreferenceStore());
		setDescription(Messages.PREFERENCES_ENABLED_RULES);
		String configStateString = getPreferenceStore().getString(JfrEditor.RULE_CONFIGURATION_PREFERENCE_ID);
		IState configState = null;
		try {
			configState = (configStateString.length() != 0) ? StateToolkit.fromXMLString(configStateString) : null;
			ignoredRules = loadIgnoredRules(getPreferenceStore());
		} catch (SAXException saxe) {
			FlightRecorderUI.getDefault().getLogger().log(Level.WARNING, "Error reading configuration XML", saxe); //$NON-NLS-1$
		}
		config = new BasicConfig(configState);
	}

	@Override
	public void init(IWorkbench workbench) {
		FontData[] fontData = Display.getCurrent().getSystemFont().getFontData();
		FontData fd = new FontData(fontData[0].toString());
		fd.setStyle(SWT.ITALIC);
		topicFont = new Font(Display.getCurrent(), fd);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(1, false));
		topicTree = new Tree(composite, SWT.CHECK | SWT.MULTI | SWT.BORDER);
		topicTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Button configure = new Button(composite, SWT.PUSH);
		configure.setText(Messages.PREFERENCES_RULES_CONFIGURE_SELECTED);
		configure.setEnabled(false);
		topicTree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.detail == SWT.CHECK) {
					TreeItem item = (TreeItem) e.item;
					TreeItem parentItem = item.getParentItem();
					if (parentItem != null) {
						Supplier<Stream<TreeItem>> siblingItemSupplier = () -> Stream.of(parentItem.getItems());
						boolean anyChecked = siblingItemSupplier.get().anyMatch(TreeItem::getChecked);
						boolean allChecked = siblingItemSupplier.get().allMatch(TreeItem::getChecked);
						if (!allChecked) {
							parentItem.setGrayed(anyChecked);
						} else {
							parentItem.setGrayed(false);
						}
						item.getParentItem().setChecked(anyChecked);
					} else {
						if (item.getGrayed()) {
							item.setGrayed(false);
						}
						Stream.of(item.getItems()).forEach(i -> i.setChecked(item.getChecked()));
					}
				} else if (e.detail == 0) {
					configure.setEnabled(
							getSelectedRules().stream().anyMatch(r -> r.getConfigurationAttributes().size() > 0));
				}
			}
		});
		Collection<String> topics = RulesUiToolkit.getTopics();
		for (String topic : topics) {
			Collection<IRule> rules = RulesUiToolkit.getRules(topic);
			TreeItem topicItem = new TreeItem(topicTree, SWT.NONE);
			topicItem.setText(topic);
			topicItem.setFont(topicFont);
			int checkedRules = 0;
			for (IRule rule : rules) {
				TreeItem ruleItem = new TreeItem(topicItem, SWT.NONE);
				ruleItem.setText(rule.getName());
				if (!ignoredRules.contains(rule.getId())) {
					ruleItem.setChecked(true);
					checkedRules++;
				}
				ruleItem.setData(RULE_ID, rule.getId());
			}
			boolean anyChecked = checkedRules > 0 && checkedRules < rules.size();
			topicItem.setGrayed(anyChecked);
			topicItem.setChecked(checkedRules == rules.size() || anyChecked);
		}
		configure.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				List<IRule> filteredRules = getSelectedRules();
				AttributeConfiguration pageConfiguration = new AttributeConfiguration(filteredRules);
				IAction createOpenConfigAction = config.createOpenConfigAction(pageConfiguration, r -> {
					getPreferenceStore().setValue(JfrEditor.RULE_CONFIGURATION_PREFERENCE_ID,
							StateToolkit.toXMLString(config));
				});
				createOpenConfigAction.run();
			}
		});
		composite.addDisposeListener(e -> topicFont.dispose());
		return composite;
	}

	private List<IRule> getSelectedRules() {
		Stream<String> selectedTopics = Stream.of(topicTree.getSelection()).filter(ti -> ti.getParentItem() == null)
				.map(ti -> ti.getText());
		Stream<String> selectedRuleTopics = Stream.of(topicTree.getSelection()).filter(ti -> ti.getParentItem() != null)
				.map(ti -> ti.getParentItem().getText());
		String[] topics = Stream.concat(selectedRuleTopics, selectedTopics).distinct().toArray(String[]::new);
		Collection<IRule> rules = RulesUiToolkit.getRules(topics);
		List<IRule> filteredRules = rules.stream()
				.filter(rule -> Stream.of(topicTree.getSelection()).anyMatch(selected -> {
					if (selected.getParentItem() == null) {
						return true;
					}
					return selected.getData(RULE_ID).equals(rule.getId());
				})).collect(Collectors.toList());
		return filteredRules;
	}

	@Override
	protected void performApply() {
		Set<String> checkedRuleIDs = streamTopicTreeItems().filter(ti -> !ti.getChecked())
				.map(ti -> ti.getData(RULE_ID).toString()).collect(Collectors.toSet());
		try {
			IWritableState ignoredState = StateToolkit.createWriter(STATE);
			checkedRuleIDs.forEach(id -> ignoredState.createChild(IGNORED_RULES).putString(RULE_ID, id));
			getPreferenceStore().setValue(IGNORED_RULES, ignoredState.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean performOk() {
		performApply();
		return true;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		streamTopicTreeItems().forEach(ti -> {
			ti.setChecked(true);
			ti.getParentItem().setGrayed(false);
			ti.getParentItem().setChecked(true);
		});
	}

	// Only gets the children of the items in the root. Is this what is wanted?
	private Stream<TreeItem> streamTopicTreeItems() {
		return Stream.of(topicTree.getItems()).flatMap(i -> Stream.of((i.getItems())));
	}

	public static Set<String> loadIgnoredRules(IPreferenceStore preferenceStore) {
		String ignoredStateString = preferenceStore.getString(RulesPage.IGNORED_RULES);
		IState ignoredState;
		try {
			ignoredState = (ignoredStateString.length() != 0) ? StateToolkit.fromXMLString(ignoredStateString) : null;
			if (ignoredState != null) {
				return Stream.of(ignoredState.getChildren(RulesPage.IGNORED_RULES))
						.map(iS -> iS.getAttribute(RulesPage.RULE_ID)).collect(Collectors.toCollection(HashSet::new));
			}
		} catch (SAXException e) {
			FlightRecorderUI.getDefault().getLogger().log(Level.WARNING,
					"Could not read ignored rules from preferences.", e); //$NON-NLS-1$
		}
		return Collections.emptySet();
	}

}
