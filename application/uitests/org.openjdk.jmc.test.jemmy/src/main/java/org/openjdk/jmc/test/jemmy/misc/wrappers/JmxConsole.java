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

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;

/**
 * The Jemmy wrapper for the Mission Control JMX Console
 */
public class JmxConsole extends MCJemmyBase {

	/**
	 * Selects the console tab (if not already selected)
	 *
	 * @param tab
	 *            the console tab to select
	 * @return the {@link MCTabFolder} that was selected
	 */
	public static MCTabFolder selectTab(Tabs tab) {
		return selectTab(Tabs.text(tab));
	}

	/**
	 * Selects the console tab (if not already selected)
	 *
	 * @param tab
	 *            the console tab (text) to select
	 * @return the {@link MCTabFolder} that was selected
	 */
	public static MCTabFolder selectTab(String tab) {
		MCTabFolder tabFolder = MCTabFolder.getByTabName(Tabs.text(Tabs.OVERVIEW));
		tabFolder.select(tab);
		clearFocus();
		return tabFolder;
	}

	/**
	 * Returns a list of all visible tabs (even non-standard ones)
	 *
	 * @return a {@link List} of {@link String} names for all the tabs
	 */
	public static List<String> getAllVisibleTabNames() {
		return MCTabFolder.getByTabName(Tabs.text(Tabs.OVERVIEW)).getVisibleItems();
	}

	/**
	 * An Enum for all the standard console tabs
	 */
	public static enum Tabs {
		OVERVIEW, MBEAN_BROWSER, TRIGGERS, SYSTEM, MEMORY, THREADS, DIAGNOSTIC_COMMANDS;

		public static String text(Tabs tab) {
			String text = "";

			switch (tab) {
			case OVERVIEW:
				text = "Overview";
				break;
			case MBEAN_BROWSER:
				text = "MBean Browser";
				break;
			case TRIGGERS:
				text = "Triggers";
				break;
			case SYSTEM:
				text = "System";
				break;
			case MEMORY:
				text = "Memory";
				break;
			case THREADS:
				text = "Threads";
				break;
			case DIAGNOSTIC_COMMANDS:
				text = "Diagnostic Commands";
			}
			return text;
		}
	}

	/**
	 * Clicks the specified toolbar button (a hyperlink).
	 *
	 * @param tooltip
	 *            the name of the tooltip for the button
	 */
	public void clickToolbarButton(String tooltip) {
		if (focusedSection != null) {
			MCHyperlink.getByTooltip(focusedSection, tooltip).click();
		} else {
			MCHyperlink.getByTooltip(getShell(), tooltip).click();
		}
	}

}
