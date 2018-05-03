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

import org.openjdk.jmc.flightrecorder.ui.JfrOutlinePage;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;

/**
 * The Jemmy wrapper for the Flight Recorder UI navigator
 */
public class JfrNavigator extends MCJemmyBase {
	private static MCTree jfrNavigatorTree;

	private JfrNavigator() {
	}

	/**
	 * Selects and expands each of the input strings to hierarchically open the items in the tree.
	 * For tabs installed by plugins, use the <method>selectTab(String ... path)</method> method
	 * instead.
	 *
	 * @param tab
	 *            the tab to open.
	 */
	public static void selectTab(JfrUi.Tabs tab) {
		selectTab(JfrUi.Tabs.text(tab));
	}

	/**
	 * Selects and expands each of the input strings to hierarchically open the items in the tree.
	 *
	 * @param path
	 *            the text(s) of the item(s) to select and expand
	 */
	public static void selectTab(String ... path) {
		initialize();
		jfrNavigatorTree.select(path);
	}

	/**
	 * Returns the complete list of tabs for the flight recording UI.
	 *
	 * @return a {@link List} of {@link String} representing the tabs.
	 */
	public static List<String> getTabs() {
		initialize();
		return jfrNavigatorTree.getItemsText();
	}

	/**
	 * Initializes the JFR navigation tree. The navigation tree is disposed as soon
	 * as all recordings have been closed so this needs to be checked every time
	 */
	private static void initialize() {
		MCMenu.ensureJfrNavigationVisible();
		if (jfrNavigatorTree == null || jfrNavigatorTree.isDisposed() || !jfrNavigatorTree.isVisible()) {
			jfrNavigatorTree = MCTree.getFirstVisibleByName(getShell(), JfrOutlinePage.Outline_TREE_NAME, false);

		}
	}
}
