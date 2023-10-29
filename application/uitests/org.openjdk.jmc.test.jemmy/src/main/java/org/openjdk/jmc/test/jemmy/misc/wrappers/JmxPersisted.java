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

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;

/**
 * The Jemmy wrapper for the Persisted JMX Data view
 */
public class JmxPersisted extends MCJemmyBase {
	private static final String SELECT_ATTRIBUTES_TITLE = org.openjdk.jmc.rjmx.ui.messages.internal.Messages.SELECT_ATTRIBUTES_TITLE;
	private static final String CONFIGURE_PERSISTENCE_ACTION_TOOLTIP = org.openjdk.jmc.rjmx.ui.internal.Messages.ConfigurePersistenceAction_TOOLTIP_TEXT;
	private static final String ADD_ATTRIBUTES_ACTION_TOOLTIP = org.openjdk.jmc.rjmx.ui.internal.Messages.ADD_ATTIBUTES_ACTION_TOOLTIP;

	private static List<MCTable> tables;

	private JmxPersisted() {
	}

	/**
	 * Updates the tables in the persisted jmx data screen.
	 */
	private static void updateTables() {
		tables = new ArrayList<>();
		tables.addAll(MCTable.getAll(getShell()));
	}

	private static void ensureOverviewPage() {
		JmxConsole.selectTab(JmxConsole.Tabs.OVERVIEW);
	}

	/**
	 * Attempts to locate an attribute in one of the tables.
	 *
	 * @param attribute
	 *            the attribute to find
	 * @return {@code true} if found, otherwise {@code false}
	 */
	public static boolean findAttribute(String attribute) {
		return findTableByAttribute(attribute) != null;
	}

	private static MCTable findTableByAttribute(String attribute) {
		updateTables();
		for (MCTable table : tables) {
			if (table.hasItem(attribute)) {
				return table;
			}
		}
		return null;
	}

	/**
	 * Removes the given attribute from the table containing it. If the attribute is in several
	 * tables, this will need to be run several times.
	 * 
	 * @param attribute
	 *            the attribute to remove
	 */
	public static void removeAttribute(String attribute) {
		updateTables();
		MCTable table = findTableByAttribute(attribute);
		table.select(attribute);
		table.contextChoose("Delete");
	}

	/**
	 * Adds the specified attribute to the persistence. N.B. this doesn't actually start persisting
	 * until the switchPersistence() method is called.
	 *
	 * @param attributePath
	 *            The path to the attribute to persist. ("java.lang", "Runtime", [some
	 *            attributename]) et c.
	 */
	public static void selectAttributeToPersist(String ... attributePath) {
		ensureOverviewPage();
		MCHyperlink.getByTooltip(getShell(), ADD_ATTRIBUTES_ACTION_TOOLTIP, 0).click();
		MCDialog dialog = new MCDialog(SELECT_ATTRIBUTES_TITLE);
		sleep(1000);
		MCTree.getFirst(dialog.getDialogShell()).select(attributePath);
		dialog.closeWithButton(MCButton.Labels.FINISH);
	}

	/**
	 * Enables/Disables the persistence of historical data. Note that this is stateless, we cannot
	 * know the state of the persistence, so the test needs to do some book keeping.
	 */
	public static void switchPersistence() {
		ensureOverviewPage();
		MCHyperlink.getByTooltip(getShell(), CONFIGURE_PERSISTENCE_ACTION_TOOLTIP).click();
	}
}
