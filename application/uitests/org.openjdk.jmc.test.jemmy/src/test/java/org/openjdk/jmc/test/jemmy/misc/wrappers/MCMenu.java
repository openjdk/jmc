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

import java.util.Arrays;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.jemmy.control.Wrap;
import org.jemmy.input.StringMenuOwner;
import org.jemmy.interfaces.Focusable;
import org.jemmy.interfaces.Keyboard.KeyboardButtons;
import org.jemmy.interfaces.Keyboard.KeyboardModifiers;
import org.jemmy.interfaces.Selectable;
import org.jemmy.resources.StringComparePolicy;
import org.junit.Assert;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.fetchers.Fetcher;

/**
 * The Jemmy wrapper for the Mission Control menu.
 */
@SuppressWarnings({"unchecked", "restriction"})
public class MCMenu extends MCJemmyBase {
	private static final int MAX_DIALOG_OPEN_RETRIES = 5;
	private static final String MC_UI_STACKTRACE_VIEW = "org.openjdk.jmc.flightrecorder.ui.StacktraceView";
	private static final String TEMPLATE_MANAGER_DIALOG_TITLE = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.TEMPLATE_MANAGER_DIALOG_TITLE;
	private static final String IMPORT_DIALOG_TITLE = WorkbenchMessages.ImportWizard_title;
	private static final String EXPORT_DIALOG_TITLE = WorkbenchMessages.ExportWizard_title;
	private static final String[] PROGRESS_VIEW_MENU_TOKENS = {"&Window", "Show View", "Progress"};
	private static final String[] OPEN_FILE_MENU_TOKENS = {"&File", "Open File..."};
	private static final String[] PREFERENCES_MENU_TOKENS = {"&Window", "&Preferences"};
	private static final String[] WELCOME_MENU_TOKENS = {"&Help", "Welcome"};
	private static final String[] TEMPLATE_MANAGER_MENU_TOKENS = {"&Window", TEMPLATE_MANAGER_DIALOG_TITLE};
	private static final String[] IMPORT_MENU_TOKENS = {"&File", "&Import"};
	private static final String[] EXPORT_MENU_TOKENS = {"&File", "Exp&ort"};
	private static final String[] JVM_BROWSER_VIEW_MENU_TOKENS = {"&Window", "Show View",
			org.openjdk.jmc.browser.views.Messages.JVMBrowserView_DIALOG_MESSAGE_TITLE};
	private static final String[] JFR_NAVIGATION_VIEW_MENU_TOKENS = {"&Window", "Show View", "Outline"};
	private static final String[] ABOUT_MENU_TOKENS = {"&Help", "&About"};
	private static final String[] INSTALL_NEW_SOFTWARE_MENU_TOKENS = {"&Help", "&Software"};
	private static final String[] STACK_TRACE_VIEW_TOKENS = {"&Window", "Show View", "Stack Trace"};

	private static StringMenuOwner<Shell> getMenu() {
		StringMenuOwner<Shell> menu = getShell().as(StringMenuOwner.class);
		menu.setPolicy(StringComparePolicy.SUBSTRING);
		return menu;
	}

	private static MCDialog getDialog(MenuKeys keys, String[] path, String dialogTitle) {
		MCDialog dialog = null;
		int retries = MAX_DIALOG_OPEN_RETRIES;
		do {
			if (retries < MAX_DIALOG_OPEN_RETRIES) {
				System.out.println("Retrying menu operation to open dialog \"" + dialogTitle + "\" (at menu path: "
						+ Arrays.asList(path).toString() + ")");
				getShell().keyboard().pushKey(KeyboardButtons.ESCAPE);
				MCJemmyBase.focusMc();
			}
			chooseMenuItem(keys, path);
			dialog = MCDialog.getByAnyDialogTitle(false, true, dialogTitle);
			retries--;
		} while (dialog == null && retries > 0);
		if (dialog == null) {
			Assert.fail("Could not find dialog matching " + dialogTitle + " when requesting menu at path: "
					+ Arrays.asList(path).toString());
		}
		return dialog;
	}

	/**
	 * Opens the Export dialog
	 * 
	 * @return a {@link MCDialog} for the export shell
	 */
	public static MCDialog openExportDialog() {
		return getDialog(MenuKeys.EXPORT, EXPORT_MENU_TOKENS, EXPORT_DIALOG_TITLE);
	}

	/**
	 * Opens the Import dialog
	 * 
	 * @return a {@link MCDialog} for the import shell
	 */
	public static MCDialog openImportDialog() {
		return getDialog(MenuKeys.IMPORT, IMPORT_MENU_TOKENS, IMPORT_DIALOG_TITLE);
	}

	/**
	 * Opens the Preferences dialog
	 * 
	 * @return a {@link MCDialog} for the preferences shell
	 */
	public static MCDialog openPreferencesDialog() {
		return getDialog(MenuKeys.PREFERENCES, PREFERENCES_MENU_TOKENS, "Preferences");

	}

	/**
	 * Opens the About dialog
	 * 
	 * @return a {@link MCDialog} for the about shell
	 */
	public static MCDialog openAboutDialog() {
		return getDialog(MenuKeys.ABOUT, ABOUT_MENU_TOKENS, "About");

	}

	/**
	 * Opens the software installation dialog
	 * 
	 * @return a {@link MCDialog} for the software installation shell
	 */
	public static MCDialog openInstallNewSoftwareDialog() {
		return getDialog(MenuKeys.PREFERENCES, INSTALL_NEW_SOFTWARE_MENU_TOKENS, "Install");

	}

	/**
	 * Opens the Open File dialog
	 * 
	 * @return a {@link MCDialog} for the Open File shell
	 */
	public static MCDialog openFileDialog() {
		return getDialog(MenuKeys.OPEN_FILE, OPEN_FILE_MENU_TOKENS, "Open File");

	}

	/**
	 * Opens the Template Manager dialog
	 * 
	 * @return a {@link MCDialog} for the template manager shell
	 */
	public static MCDialog openTemplateManagerDialog() {
		return getDialog(MenuKeys.TEMPLATE_MANAGER, TEMPLATE_MANAGER_MENU_TOKENS,
				TEMPLATE_MANAGER_MENU_TOKENS[TEMPLATE_MANAGER_MENU_TOKENS.length - 1]);
	}

	/**
	 * Opens the Welcome dialog
	 */
	public static void openWelcome() {
		chooseMenuItem(MenuKeys.WELCOME, WELCOME_MENU_TOKENS);
	}

	/**
	 * Ensures that the JVM Browser is visible and is focused
	 */
	public static void ensureJvmBrowserVisible() {
		ensureView(MenuKeys.JVM_BROWSER, JVM_BROWSER_VIEW_MENU_TOKENS);
	}

	/**
	 * Ensures that the Outline is visible and is focused
	 */
	public static void ensureJfrNavigationVisible() {
		ensureView(MenuKeys.JFR_NAVIGATION, JFR_NAVIGATION_VIEW_MENU_TOKENS);
	}

	/**
	 * Ensures that the Progress View is visible and is focused
	 */
	public static void ensureProgressViewVisible() {
		ensureView(MenuKeys.PROGRESS_VIEW, PROGRESS_VIEW_MENU_TOKENS);
	}

	/**
	 * Ensures that the Stack Trace view is visible and is focused
	 */
	public static void ensureStackTraceViewVisible() {
		ensureView(MenuKeys.STACK_TRACE, STACK_TRACE_VIEW_TOKENS);
	}

	/**
	 * The close operation is always performed using the key accelerators since the menu options
	 * aren't always available, causing menu navigation failure(s)
	 */
	public static void closeAllEditors() {
		pushKeys(MenuKeys.CLOSE_ALL);
	}

	/**
	 * The close operation is always performed using the key accelerators since the menu options
	 * aren't always available, causing menu navigation failure(s)
	 */
	public static void closeActiveEditor() {
		pushKeys(MenuKeys.CLOSE);
	}

	/**
	 * Minimizes the Stack Trace view
	 * 
	 * @return the previous state (integer)
	 */
	public static int minimizeStackTraceView() {
		return setViewPartState(IWorkbenchPage.STATE_MINIMIZED, MC_UI_STACKTRACE_VIEW);
	}

	/**
	 * Restores the Stack Trace view. Usually used to restore from a minimized state
	 * 
	 * @return the previous state (integer)
	 */
	public static int restoreStackTraceView() {
		return setViewPartState(IWorkbenchPage.STATE_RESTORED, MC_UI_STACKTRACE_VIEW);
	}

	private static void chooseMenuItem(MenuKeys keys, String[] pathTokens) {
		if (OS_NAME.contains("os x")) {
			pushKeys(keys);
		} else {
			getShell().as(Focusable.class).focuser().focus();
			getMenu().push(pathTokens);
		}
	}

	private static void pushKeys(MenuKeys keys) {
		focusMc();
		getShell().keyboard().pushKey(keys.getButton(), keys.getModifiers());
	}

	private static void ensureView(MenuKeys view, String[] pathTokens) {
		String viewName = pathTokens[pathTokens.length - 1];
		Wrap<? extends CTabFolder> viewTabFolder = findWrap(CTabFolder.class, viewName, Selectable.STATES_PROP_NAME,
				StringComparePolicy.SUBSTRING);
		if (viewTabFolder == null) {
			chooseMenuItem(view, pathTokens);
		} else {
			if (!viewTabFolder.getProperty(Selectable.STATE_PROP_NAME).equals(viewName)) {
				viewTabFolder.as(Selectable.class).selector().select(viewName);
			}
		}
	}

	/**
	 * Enumeration for the "short keys" to access various Mission Control menu items. Primarily to be used with
	 * Mac OS X where the menu bar is rendered with native code and inaccessible to Jemmy. Other
	 * platforms should still use the "menu.push()" mechanism to ensure that the menu actually holds
	 * the items. Note: Each mapping first needs to be configured in plugin.xml.
	 */

	private static enum MenuKeys {
		PROGRESS_VIEW(KeyboardButtons.R, new KeyboardModifiers[] {KeyboardModifiers.SHIFT_DOWN_MASK, SHORTCUT_MODIFIER,
				KeyboardModifiers.ALT_DOWN_MASK}),
		JFR_NAVIGATION(KeyboardButtons.O, new KeyboardModifiers[] {KeyboardModifiers.SHIFT_DOWN_MASK, SHORTCUT_MODIFIER,
				KeyboardModifiers.ALT_DOWN_MASK}),
		JVM_BROWSER(KeyboardButtons.J, new KeyboardModifiers[] {KeyboardModifiers.SHIFT_DOWN_MASK, SHORTCUT_MODIFIER,
				KeyboardModifiers.ALT_DOWN_MASK}),
		OPEN_FILE(KeyboardButtons.O, new KeyboardModifiers[] {SHORTCUT_MODIFIER}),
		PREFERENCES(KeyboardButtons.P, new KeyboardModifiers[] {KeyboardModifiers.SHIFT_DOWN_MASK, SHORTCUT_MODIFIER,
				KeyboardModifiers.ALT_DOWN_MASK}),
		IMPORT(KeyboardButtons.I, new KeyboardModifiers[] {KeyboardModifiers.SHIFT_DOWN_MASK, SHORTCUT_MODIFIER,
				KeyboardModifiers.ALT_DOWN_MASK}),
		EXPORT(KeyboardButtons.E, new KeyboardModifiers[] {KeyboardModifiers.SHIFT_DOWN_MASK, SHORTCUT_MODIFIER,
				KeyboardModifiers.ALT_DOWN_MASK}),
		TEMPLATE_MANAGER(KeyboardButtons.T, new KeyboardModifiers[] {KeyboardModifiers.SHIFT_DOWN_MASK,
				SHORTCUT_MODIFIER, KeyboardModifiers.ALT_DOWN_MASK}),
		WELCOME(KeyboardButtons.W, new KeyboardModifiers[] {KeyboardModifiers.SHIFT_DOWN_MASK, SHORTCUT_MODIFIER,
				KeyboardModifiers.ALT_DOWN_MASK}),
		ABOUT(KeyboardButtons.A, new KeyboardModifiers[] {SHORTCUT_MODIFIER, KeyboardModifiers.ALT_DOWN_MASK}),
		INSTALL_NEW_SOFTWARE(KeyboardButtons.X, new KeyboardModifiers[] {KeyboardModifiers.SHIFT_DOWN_MASK,
				SHORTCUT_MODIFIER, KeyboardModifiers.ALT_DOWN_MASK}),
		CLOSE_ALL(CLOSE_BUTTON, new KeyboardModifiers[] {SHORTCUT_MODIFIER, KeyboardModifiers.SHIFT_DOWN_MASK}),
		CLOSE(CLOSE_BUTTON, new KeyboardModifiers[] {SHORTCUT_MODIFIER}),
		NEW_CONNECTION(KeyboardButtons.C, new KeyboardModifiers[] {KeyboardModifiers.SHIFT_DOWN_MASK, SHORTCUT_MODIFIER,
				KeyboardModifiers.ALT_DOWN_MASK}),
		STACK_TRACE(KeyboardButtons.S, new KeyboardModifiers[] {KeyboardModifiers.SHIFT_DOWN_MASK, SHORTCUT_MODIFIER,
				KeyboardModifiers.ALT_DOWN_MASK});

		private final KeyboardButtons button;
		private final KeyboardModifiers[] modifiers;

		private MenuKeys(KeyboardButtons button, KeyboardModifiers ... modifiers) {
			this.button = button;
			this.modifiers = modifiers;
		}

		public KeyboardButtons getButton() {
			return button;
		}

		public KeyboardModifiers[] getModifiers() {
			return modifiers;
		}
	}

	/**
	 * Sets the state of a ViewPart (if not already set to that)
	 *
	 * @param state
	 *            The state to set the view to
	 * @param viewID
	 *            The Id of the view
	 * @return the previous state (integer)
	 */
	private static int setViewPartState(final int state, final String viewID) {
		Fetcher<Integer> fetcher = new Fetcher<Integer>() {
			@Override
			public void run() {
				IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IWorkbenchPartReference theView = workbenchPage.findViewReference(viewID);
				int currentState = workbenchPage.getPartState(theView);
				setOutput(currentState);
				if (state != currentState) {
					workbenchPage.setPartState(theView, state);
				}
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

}
