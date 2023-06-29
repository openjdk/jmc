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

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;

import org.openjdk.jmc.test.jemmy.misc.fetchers.Fetcher;

/**
 * This is the composite class holding Mission Control and its sub-components
 */
public class MC {
	public static JvmBrowser jvmBrowser;
	public static JmxConsole jmxConsole;
	public static JfrUi jfrUi;
	public static MCMenu mcMenu;

	static {
		jvmBrowser = new JvmBrowser();
		jmxConsole = new JmxConsole();
		jfrUi = new JfrUi();
		mcMenu = new MCMenu();
	}

	private MC() {
	}

	/**
	 * Closes the welcome page
	 */
	public static void closeWelcome() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				IIntroManager intro = workbench.getIntroManager();
				intro.closeIntro(intro.getIntro());
			}
		});
	}

	/**
	 * Checks if Mission Control (including dialogs etc) has focus or not.
	 *
	 * @return {@code true} if Mission Control has focus
	 */
	public static boolean mcHasFocus() {
		Fetcher<Shell> fetcher = new Fetcher<Shell>() {
			@Override
			public void run() {
				setOutput(Display.getDefault().getActiveShell());
			}
		};
		Display.getDefault().syncExec(fetcher);
		return (null != fetcher.getOutput());
	}

	/**
	 * Sets the accessibility mode to the desired state.
	 *
	 * @param state
	 *            {@code true} if accessibility mode is to be turned on
	 */
	public static void setAccessibility(boolean state) {
		MCDialog preferences = MCMenu.openPreferencesDialog();
		preferences.selectTreeItem("JDK Mission Control");
		preferences.setButtonState("Use &accessibility mode", state);
		preferences.setButtonState(
				"Show text labels on buttons (takes effect on newly opened connections and recordings)", state);
		preferences.clickButton(MCButton.Labels.APPLY_AND_CLOSE);
	}

	/**
	 * Sets the JFR analysis mode to the desired state.
	 *
	 * @param enabled
	 *            the desired state
	 */
	public static void setRecordingAnalysis(boolean enabled) {
		MCDialog preferences = MCMenu.openPreferencesDialog();
		preferences.selectTreeItem("JDK Mission Control", "Flight Recorder");
		preferences.setButtonState("Enable flight recording analysis", enabled);
		preferences.clickButton(MCButton.Labels.APPLY_AND_CLOSE);
	}

	/**
	 * Navigates to the tab with matching name. Note: this is not the same as the title of the tab.
	 * 
	 * @param tabName
	 *            the name of the tab
	 */
	public static void selectMcTab(String tabName) {
		MCTabFolder.getByTabName(tabName).select(tabName);
	}
}
