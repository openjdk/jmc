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
package org.openjdk.jmc.test.jemmy.misc.wrappers;

import java.util.List;
import java.util.Map;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCButton.Labels;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JmxConsole.Tabs;

/**
 * The Jemmy wrapper for the Mission Control Triggers console page
 */
public class MCTriggersPage extends MCJemmyBase {
	private static final String SHOW_DIALOG_ON_ALERTS = org.openjdk.jmc.alert.Messages.AlertDialog_POP_UP_ON_ALERTS_TEXT;
	private static final String TRIGGER_ALERTS_DIALOG_TITLE = org.openjdk.jmc.alert.Messages.AlertDialog_DIALOG_TITLE;
	private static final String RULES_TREE_NAME = "triggers.RulesTree";
	private static final String LIMIT_PERIOD_TOOLTIP = org.openjdk.jmc.rjmx.triggers.condition.internal.Messages.TriggerCondition_LIMIT_PERIOD_TOOLTIP;
	private static final String MAX_TRIGGER_TOOLTIP = org.openjdk.jmc.rjmx.triggers.condition.internal.Messages.TriggerCondition_MAX_TRIGGER_TOOLTIP;
	private static final String SUSTAINED_TOOLTIP = org.openjdk.jmc.rjmx.triggers.condition.internal.Messages.TriggerCondition_SUSTAINED_TOOLTIP;
	private static final String WARN_IF_OVERWRITE_TEXT = org.openjdk.jmc.ui.wizards.Messages.ExportToFileWizardPage_WARN_IF_OVERWRITE_TEXT;
	private static MCTree rulesTree;

	private MCTriggersPage() {
	}

	/**
	 * Activates/Deactivates a trigger rule
	 *
	 * @param state
	 *            the desired state of the trigger rule. {@code true} activates and {@code false}
	 *            deactivates
	 * @param path
	 *            the path of the trigger rule
	 */
	public static void toggleTriggerRule(boolean state, String ... path) {
		selectTriggerRule(path);
		rulesTree.setSelectedItemState(state);
	}

	/**
	 * Selects a trigger rule
	 *
	 * @param path
	 *            the path of the trigger rule to select
	 */
	public static void selectTriggerRule(String ... path) {
		initializeRulesTree();
		rulesTree.select(path);
	}

	/**
	 * Removes the trigger rule
	 *
	 * @param path
	 *            the path of the trigger rule to remove
	 */
	public static void removeTriggerRule(String ... path) {
		initializeRulesTree();
		rulesTree.select(path);
		MCButton.getByLabel("Delete").click();
		MCDialog dialog = new MCDialog("Confirm remove");
		dialog.closeWithButton(Labels.OK);
	}

	/**
	 * Waits for a trigger alert dialog and closes it
	 *
	 * @param cleanUpAlerts
	 *            {@code true} if not to show up again on subsequent alerts and to clear the
	 *            previous alerts
	 */
	public static void closeTriggerAlertDialog(boolean cleanUpAlerts) {
		MCDialog dialog = new MCDialog(TRIGGER_ALERTS_DIALOG_TITLE);
		dialog.setButtonState(SHOW_DIALOG_ON_ALERTS, !cleanUpAlerts);
		if (cleanUpAlerts) {
			dialog.clickButton("Clear");
		}
		dialog.closeWithButton(Labels.CLOSE);
	}

	/**
	 * Reset the trigger rules tree to the default
	 */
	public static void resetTriggerRules() {
		initializeRulesTree();
		MCButton.getByLabel("Reset").click();
		MCDialog resetDialog = new MCDialog("Reset");
		resetDialog.closeWithButton(Labels.YES);
	}

	/**
	 * Renames the specified rule
	 *
	 * @param newName
	 *            the new name
	 * @param path
	 *            the complete path to the rule to rename
	 */
	public static void renameTriggerRule(String newName, String ... path) {
		selectTriggerRule(path);
		MCButton.getByLabel("Rename").click();
		MCDialog ruleShell = new MCDialog("Rename rule");
		MCText.getFirstVisible(ruleShell).setText(newName);
		ruleShell.closeWithButton(Labels.OK);
	}

	/**
	 * Find out if the rules tree contains the rule with the specified path
	 *
	 * @param path
	 *            the path of the rule to find
	 * @return {@code true} if found, otherwise {@code false}
	 */
	public static boolean hasTriggerRule(String ... path) {
		initializeRulesTree();
		return MCTriggersPage.rulesTree.hasItem(path);
	}

	/**
	 * Export the selected rules
	 * 
	 * @param filename
	 *            The filename to export to rules into
	 * @param rulePaths
	 *            the path(s) of the rule(s)
	 */
	public static void exportTriggerRules(String filename, List<String[]> rulePaths) {
		initializeRulesTree();
		MCButton.getByLabel("Export...").click();
		MCDialog exportDialog = new MCDialog("Export Rules");
		MCTree rulesExportTree = MCTree.getFirst(exportDialog);
		// navigate the tree to select the rules to export
		for (String[] rulePath : rulePaths) {
			rulesExportTree.select(rulePath);
			rulesExportTree.setSelectedItemState(true);
		}
		// turn off warning about overwriting file
		exportDialog.setButtonState(WARN_IF_OVERWRITE_TEXT, false);
		// set the filename
		exportDialog.enterText(filename);
		// finish the export
		exportDialog.closeWithButton(Labels.FINISH);
	}

	/**
	 * Import the rules contained in the specified filename
	 *
	 * @param filename
	 *            the filename
	 */
	public static void importTriggerRules(String filename) {
		JmxConsole.selectTab(JmxConsole.Tabs.TRIGGERS);
		MCButton.getByLabel("Import...").click();
		MCDialog importDialog = new MCDialog("Import Rules");
		importDialog.enterText(filename);
		importDialog.closeWithButton(Labels.FINISH);
	}

	/**
	 * Set a trigger's condition value
	 *
	 * @param tooltip
	 *            the tooltip of the condition to set
	 * @param value
	 *            the new value of the condition
	 * @param path
	 *            the path of the trigger rule
	 */
	public static void setTriggerCondition(String tooltip, String value, String ... path) {
		initializeRulesTree();
		selectTriggerRule(path);
		MCText.getByToolTip(tooltip).setText(value);
	}

	/**
	 * Create a new trigger rule
	 * 
	 * @param maxTrigger
	 *            the max trigger value
	 * @param sustainedPeriod
	 *            the sustained period
	 * @param limitPeriod
	 *            the limit period
	 * @param actionType
	 *            the type of action to trigger
	 * @param actionParams
	 *            A map of parameters to set for the action. The key is the string of the tooltip
	 *            for the parameter and the value should be either a string or boolean representing
	 *            the value to set. If a boolean it is assumed to be a button, otherwise a text
	 *            control. If no parameters are to be set this should be null
	 * @param ruleGroup
	 *            the name of the group for this rule
	 * @param ruleName
	 *            the name of the rule
	 * @param alertAttributePath
	 *            the MBean attribute for which to create a trigger rule
	 */
	public static void createTriggerRule(
		String maxTrigger, String sustainedPeriod, String limitPeriod, String actionType,
		Map<String, Comparable<?>> actionParams, String ruleGroup, String ruleName, String ... alertAttributePath) {
		// select the triggers tab
		JmxConsole.selectTab(Tabs.TRIGGERS);

		// Create a new application alert rule for CPULoad
		MCButton.getByLabel("Add...").click();

		// first page
		MCDialog newRuleDialog = new MCDialog("Add New Rule");
		MCTree attributeTree = MCTree.getFirst(newRuleDialog);
		attributeTree.select(alertAttributePath);
		newRuleDialog.clickButton(Labels.NEXT);

		// second page
		if (maxTrigger != null) {
			newRuleDialog.setToolTipText(MAX_TRIGGER_TOOLTIP, maxTrigger);
		}
		if (sustainedPeriod != null) {
			newRuleDialog.setToolTipText(SUSTAINED_TOOLTIP, sustainedPeriod);
		}
		if (limitPeriod != null) {
			newRuleDialog.setToolTipText(LIMIT_PERIOD_TOOLTIP, limitPeriod);
		}
		newRuleDialog.clickButton(Labels.NEXT);

		// third page
		MCTable actionTable = MCTable.getAll(newRuleDialog).get(0);
		actionTable.select(actionType);
		if (actionParams != null) {
			for (String controlText : actionParams.keySet()) {
				Object value = actionParams.get(controlText);
				if (value instanceof Boolean) {
					MCButton.getByLabel(newRuleDialog, controlText, false).setState(Boolean.class.cast(value));
				} else {
					newRuleDialog.setToolTipText(controlText, String.class.cast(value));
				}
			}
		}
		newRuleDialog.clickButton(Labels.NEXT);

		// fourth page
		// doing nothing at this page. Just click next to go to the next page
		newRuleDialog.clickButton(Labels.NEXT);

		// fifth page
		MCCCombo ruleGroupCombo = MCCCombo.getFirst(newRuleDialog);
		ruleGroupCombo.setText(ruleGroup);

		newRuleDialog.replaceText("My Rule", ruleName);
		// finish the new rule wizard
		newRuleDialog.closeWithButton(Labels.FINISH);
	}

	private static void initializeRulesTree() {
		JmxConsole.selectTab(JmxConsole.Tabs.TRIGGERS);
		rulesTree = MCTree.getFirstVisibleByName(MCTriggersPage.RULES_TREE_NAME);
	}
}
