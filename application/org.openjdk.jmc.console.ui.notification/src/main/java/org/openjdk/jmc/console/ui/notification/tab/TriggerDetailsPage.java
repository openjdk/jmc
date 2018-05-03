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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import org.openjdk.jmc.console.ui.notification.widget.ActionChooser;
import org.openjdk.jmc.console.ui.notification.widget.ConstraintChooser;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.ui.misc.CompositeToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.IRefreshable;
import org.openjdk.jmc.ui.uibuilder.FormToolkitBuilder;

public class TriggerDetailsPage {
	private final NotificationRegistry model;
	private final IConnectionHandle connection;
	private final IRefreshable onUpdateCallback;
	private final FormToolkit toolkit;
	private final CTabItem conditionsTab;
	private final CTabItem actionsTab;
	private final CTabItem constranitTab;

	public TriggerDetailsPage(Composite parent, NotificationRegistry model, IConnectionHandle connection,
			IRefreshable onUpdateCallback, FormToolkit toolkit) {
		this.model = model;
		this.connection = connection;
		this.onUpdateCallback = onUpdateCallback;
		this.toolkit = toolkit;

		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
		section.setText(Messages.RuleDetailsTabSectionPart_SECTION_TEXT);

		CTabFolder tabFolder = new CTabFolder(section, SWT.NONE);
		toolkit.adapt(tabFolder);
		section.setClient(tabFolder);

		conditionsTab = createTab(tabFolder, Messages.RuleDetailsTabSectionPart_CONDITION_TAB_TEXT);
		actionsTab = createTab(tabFolder, Messages.RuleDetailsTabSectionPart_ACTION_TAB_TEXT);
		constranitTab = createTab(tabFolder, Messages.RuleDetailsTabSectionPart_CONSTRAINTS_TAB_TEXT);
		tabFolder.setSelection(0);
	}

	public void showRule(final TriggerRule rule) {
		Composite cond = createTabContainer(conditionsTab);
		new TriggerConditionSectionPart(cond, toolkit, model, rule, connection);
		setAsTabContent(cond, conditionsTab);

		Composite actions = createTabContainer(actionsTab);
		FormToolkitBuilder ab = new FormToolkitBuilder(toolkit, actions);
		ActionComponentFactory factory = new ActionComponentFactory(toolkit, connection);
		ActionChooser chooser = new ActionChooser(ab, rule, connection, model, actions, true, true, true, factory);
		chooser.createAndAddActionSelectionChangedListener(onUpdateCallback);
		setAsTabContent(actions, actionsTab);

		Composite constr = createTabContainer(constranitTab);
		FormToolkitBuilder constraintsBuilder = new FormToolkitBuilder(toolkit, constr);
		new ConstraintChooser(constraintsBuilder, rule, model, constr, false, false);
		setAsTabContent(constr, constranitTab);
	}

	private Composite createTabContainer(CTabItem tab) {
		ScrolledComposite scrolled = CompositeToolkit.createVerticalScrollComposite(tab.getParent());
		Composite client = toolkit.createComposite(scrolled);
		scrolled.setContent(client);
		client.setLayout(new GridLayout());
		toolkit.paintBordersFor(client);
		return client;
	}

	private static void setAsTabContent(Composite tabContainer, CTabItem tab) {
		Control oldControl = tab.getControl();
		tab.setControl(tabContainer.getParent());
		DisplayToolkit.dispose(oldControl);
	}

	private static CTabItem createTab(CTabFolder tabFolder, String text) {
		CTabItem tab = new CTabItem(tabFolder, SWT.NONE);
		tab.setText(text);
		return tab;
	}

}
