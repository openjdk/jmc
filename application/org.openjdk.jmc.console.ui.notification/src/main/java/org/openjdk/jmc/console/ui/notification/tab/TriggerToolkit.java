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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.console.ui.notification.NotificationPlugin;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.ui.common.util.StatusFactory;

/**
 * Toolkit for triggers
 */
public class TriggerToolkit {
	private static String PREF_KEY_DEFAULT_TRIGGER_RULES_INITIALIZED = "org.openjdk.jmc.console.ui.notification.table.default.triggers.initialized"; //$NON-NLS-1$
	private static String PREF_KEY_EXPANDED_TRIGGERS = "org.openjdk.jmc.console.ui.notification.table.expanded.rule.groups"; //$NON-NLS-1$
	private static String DUMMY_GROUP_NAME = "Dummy, because IDialogSettings doesn't returns null for an empty array"; //$NON-NLS-1$

	/**
	 * Returns true if the default trigger has been loaded. Typically they are loaded the first time
	 * the tab is shown, or the plug-in is loaded.
	 */
	private static boolean hasDefaultTriggersBeenLoaded() {
		return NotificationPlugin.getDefault().getPreferenceStore()
				.getBoolean(PREF_KEY_DEFAULT_TRIGGER_RULES_INITIALIZED);
	}

	/**
	 * Sets a flag in the preference store that indicates that the default triggers has been loaded.
	 */
	private static void setDefaultTriggersLoaded() {
		NotificationPlugin.getDefault().getPreferenceStore().setValue(PREF_KEY_DEFAULT_TRIGGER_RULES_INITIALIZED, true);
	}

	public static NotificationRegistry getDefaultModel() {
		NotificationRegistry notificationRegistry = RJMXPlugin.getDefault().getNotificationRegistry();
		if (!hasDefaultTriggersBeenLoaded()) {
			IStatus status = TriggerToolkit.resetTriggers(notificationRegistry);
			setDefaultTriggersLoaded();
			if (status.getSeverity() != IStatus.OK) {
				NotificationPlugin.getDefault().getLogger().severe(status.getMessage());
			}
		}
		return notificationRegistry;
	}

	/**
	 * Resets the trigger to default ones that are available if mission Control was started the
	 * first time.
	 *
	 * @param model
	 *            the notification model
	 * @return {@link IStatus} with severity {@link IStatus#OK} if the trigger were loaded
	 *         successfully
	 */
	public static IStatus resetTriggers(NotificationRegistry model) {
		InputStream stream = null;
		try {
			// Load DOM for default triggers
			stream = NotificationPlugin.class.getResourceAsStream(NotificationPlugin.DEFAULT_TRIGGER_FILE);
			Document doc = XmlToolkit.loadDocumentFromStream(new BufferedInputStream(stream));
			Collection<TriggerRule> c = model.getAvailableRules();

			// Remove all rules
			TriggerRule[] rules = c.toArray(new TriggerRule[c.size()]);
			for (TriggerRule rule : rules) {
				model.removeNotificationRule(rule);
			}

			// Import the default rules
			Element documentElement = doc.getDocumentElement();
			ResourceBundle bundle = ResourceBundle.getBundle(NotificationPlugin.DEFAULT_TRIGGER_FILE_BUNDLE);
			translateStringValues(documentElement, bundle, NotificationPlugin.getDefault().getBundle());
			model.importFromXML(documentElement);
		} catch (Exception exc) {
			return StatusFactory.createErr(NLS.bind(Messages.TriggerToolkit_ERROR_COULD_NOT_READ_DEFAULT_TEMPLATE_FILE,
					NotificationPlugin.DEFAULT_TRIGGER_FILE), exc, false);
		} finally {
			IOToolkit.closeSilently(stream);
		}
		return StatusFactory.createOk(Messages.TriggerToolkit_MESSAGE_DEFAULT_TRIGGERS_LOADED);
	}

	private static void translateStringValues(Element e, ResourceBundle bundle, Bundle plugin) {
		String text = XmlToolkit.getStringValue(e);
		if (text != null) {
			XmlToolkit.setStringValue(e, Platform.getResourceString(plugin, text, bundle));
		}
		NodeList children = e.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n instanceof Element) {
				translateStringValues((Element) n, bundle, plugin);
			}
		}
	}

	/**
	 * Store the expansions state for expanded {@link RuleGroup}s in the given viewer
	 *
	 * @param viewer
	 *            the viewer
	 */
	public static void storeExpansionState(TreeViewer viewer) {
		ArrayList<String> expandThese = new ArrayList<>();
		expandThese.add(DUMMY_GROUP_NAME);
		for (Object element : viewer.getExpandedElements()) {
			if (element instanceof RuleGroup) {
				RuleGroup group = (RuleGroup) element;
				if (group.getName() != null) {
					expandThese.add(group.getName());
				}
			}
		}
		NotificationPlugin.getDefault().getDialogSettings().put(PREF_KEY_EXPANDED_TRIGGERS,
				expandThese.toArray(new String[expandThese.size()]));
	}

	/**
	 * Retrieves the expansions state and sets it for a {@link TreeViewer}
	 *
	 * @param viewer
	 *            the viewer with the items to expand
	 * @param model
	 *            the notification model
	 */
	public static void retrieveExpansionState(TreeViewer viewer, NotificationRegistry model) {
		String[] expanded = NotificationPlugin.getDefault().getDialogSettings().getArray(PREF_KEY_EXPANDED_TRIGGERS);
		if (expanded == null) {
			expandRuleGroupWithNames(viewer, getRuleGroupNames(3, model));
		} else {
			expandRuleGroupWithNames(viewer, expanded);
		}
	}

	private static void expandRuleGroupWithNames(TreeViewer viewer, String[] names) {
		ArrayList<RuleGroup> list = new ArrayList<>();
		for (int n = 0; n < names.length; n++) {
			if (names[n] != null && !names[n].equals(DUMMY_GROUP_NAME)) {
				list.add(new RuleGroup(names[n]));
			}
		}
		viewer.setExpandedElements(list.toArray());
	}

	private static String[] getRuleGroupNames(int maxCount, NotificationRegistry model) {
		List<String> list = new ArrayList<>();
		TriggerRule[] rules = model.getAvailableRules().toArray(new TriggerRule[model.getAvailableRules().size()]);
		for (int n = 0; n < rules.length; n++) {
			if (rules[n].getRulePath() != null) {
				if (!list.contains(rules[n].getRulePath())) {
					list.add(rules[n].getRulePath());
				}
			}
		}
		return list.toArray(new String[list.size()]);
	}

}
