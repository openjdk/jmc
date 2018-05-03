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

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jemmy.control.Wrap;
import org.jemmy.interfaces.Parent;
import org.jemmy.lookup.Lookup;
import org.jemmy.swt.ShellWrap;
import org.jemmy.swt.Shells;
import org.jemmy.swt.lookup.ByTextControlLookup;
import org.jemmy.swt.lookup.ByTextShell;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;

/**
 * The Jemmy base wrapper for Dialogs
 */
public class MCDialog extends MCJemmyBase {
	private static final int DEAFULT_DIALOG_WAIT_TIMEOUT_MS = 3000;
	private boolean waitForIdleUi;
	private final String dialogTitle;

	MCDialog(Wrap<? extends Shell> dialogShell, boolean waitForIdleUi, String dialogTitle) {
		this.control = dialogShell;
		this.waitForIdleUi = waitForIdleUi;
		this.dialogTitle = dialogTitle;
	}

	/**
	 * Get the dialog with the matching title
	 * 
	 * @param dialogTitle
	 *            the title to match
	 */
	public MCDialog(String dialogTitle) {
		this(Shells.SHELLS.lookup(new ByTextShell<>(dialogTitle)).wrap(), true, dialogTitle);
	}

	/**
	 * Returns a dialog with the supplied title - or null if not found
	 * 
	 * @param exactMatching
	 *            {@code true} if exact matching should be used
	 * @param waitForIdleUi
	 *            {@code true} if the lookup should first wait for the UI update queue to be empty
	 * @param dialogTitles
	 *            one or more dialog titles that are valid for a match
	 * @return a matching {@link MCDialog} or {@code null} if not found
	 */
	public static MCDialog getByAnyDialogTitle(boolean exactMatching, boolean waitForIdleUi, String ... dialogTitles) {
		int maxRetries = 10;
		MCDialog result = null;
		while (result == null && maxRetries > 0) {
			for (Wrap<? extends Shell> thisShell : getVisible(Shells.SHELLS.lookup(Shell.class), waitForIdleUi,
					false)) {
				String thisDialogTitle = String.valueOf(thisShell.getProperty(Wrap.TEXT_PROP_NAME));
				for (String title : dialogTitles) {
					if ((exactMatching && thisDialogTitle.equals(title))
							|| (!exactMatching && thisDialogTitle.contains(title))) {
						result = new MCDialog(thisShell, waitForIdleUi, title);
					}
				}
			}
			maxRetries--;
		}
		return result;
	}

	/**
	 * Returns a dialog with the supplied title - or null if not found
	 * 
	 * @param dialogTitles
	 *            one or more dialog titles that are valid for a match
	 * @return a matching {@link MCDialog} or {@code null} if not found
	 */
	public static MCDialog getByAnyDialogTitle(String ... dialogTitles) {
		return getByAnyDialogTitle(true, true, dialogTitles);
	}

	/**
	 * Returns a dialog with the supplied title - or null if not found
	 * 
	 * @param waitForIdleUi
	 *            {@code true} if the lookup should first wait for the UI update queue to be empty
	 * @param dialogTitles
	 *            one or more dialog titles that are valid for a match
	 * @return a matching {@link MCDialog} or {@code null} if not found
	 */
	public static MCDialog getByAnyDialogTitle(boolean waitForIdleUi, String ... dialogTitles) {
		return getByAnyDialogTitle(true, waitForIdleUi, dialogTitles);
	}

	/**
	 * Returns a dialog with the supplied title <b>and</b> text - or null if not found
	 *
	 * @param dialogTitle
	 *            the title of the dialog
	 * @param dialogText
	 *            the text of the dialog
	 * @return a matching {@link MCDialog} or {@code null} if not found
	 */
	public static MCDialog getByDialogTitleAndText(String dialogTitle, String dialogText) {
		return getByDialogTitleAndText(dialogTitle, dialogText, false);
	}

	/**
	 * Returns a dialog with the supplied title <b>and</b> text - or null if not found
	 *
	 * @param dialogTitle
	 *            the title of the dialog
	 * @param dialogText
	 *            the text of the dialog
	 * @param waitForIdleUi
	 *            {@code true} if supposed to wait for an idle UI, otherwise {@code false}
	 * @return a matching {@link MCDialog} (or null)
	 */
	public static MCDialog getByDialogTitleAndText(String dialogTitle, String dialogText, boolean waitForIdleUi) {
		Wrap<? extends Shell> dialogShell = null;
		for (Wrap<? extends Shell> thisShell : getVisible(Shells.SHELLS.lookup(new ByTextShell<>(dialogTitle)),
				waitForIdleUi, DEAFULT_DIALOG_WAIT_TIMEOUT_MS, false)) {
			if (hasLabelText(thisShell, dialogText, true, waitForIdleUi)) {
				dialogShell = thisShell;
				break;
			}
		}
		if (dialogShell != null) {
			return new MCDialog(dialogShell, waitForIdleUi, dialogTitle);
		} else {
			return null;
		}
	}

	/**
	 * Closes the dialog by clicking once on the specified button
	 * 
	 * @param buttonLabel
	 *            the label of the button to click
	 */
	public void closeWithButton(MCButton.Labels buttonLabel) {
		clickButton(buttonLabel);
		waitForClose(dialogTitle, false, DEAFULT_DIALOG_WAIT_TIMEOUT_MS);
	}

	@SuppressWarnings("unchecked")
	private static boolean hasLabelText(
		Wrap<? extends Shell> shell, String labelText, boolean visibleOnly, boolean waitForIdleUi) {
		int numOfLabels = 0;
		Lookup<Label> labelLookup = shell.as(Parent.class, Label.class).lookup(Label.class,
				new ByTextControlLookup<Label>(labelText));
		if (visibleOnly) {
			numOfLabels = getVisible(labelLookup, waitForIdleUi).size();
		} else {
			numOfLabels = labelLookup.size();
		}
		return (numOfLabels > 0) ? true : false;
	}

	/**
	 * @param labelText
	 *            the label text
	 * @return {@code true} if this dialog contains a visible label with the provided text
	 */
	public boolean hasLabelText(String labelText) {
		return hasLabelText(getDialogShell(), labelText, true, waitForIdleUi);
	}

	/**
	 * @return the shell of the dialog
	 */
	@SuppressWarnings("unchecked")
	protected Wrap<? extends Shell> getDialogShell() {
		return control.as(ShellWrap.class);
	}

	/**
	 * Finds a tree item and attempts to select the item with the given path
	 *
	 * @param path
	 *            the path to the tree item
	 */
	public void selectTreeItem(String ... path) {
		MCTree tree = MCTree.getFirstVisible(getDialogShell(), waitForIdleUi);
		tree.select(path);
	}

	/**
	 * Clicks a button with the specified label.
	 *
	 * @param label
	 *            the button label
	 */
	public void clickButton(MCButton.Labels label) {
		MCButton.getByLabel(getDialogShell(), label, waitForIdleUi).click();
	}

	/**
	 * Clicks a button with the specified text label
	 *
	 * @param label
	 *            the button label
	 */
	public void clickButton(String label) {
		MCButton.getByLabel(getDialogShell(), label, waitForIdleUi).click();
	}

	/**
	 * Attempts to set the state of a button/checkbox to the specified state.
	 *
	 * @param text
	 *            the label of the button
	 * @param state
	 *            the state the button should be set to
	 */
	public void setButtonState(String text, boolean state) {
		MCButton.getByLabel(getDialogShell(), text, waitForIdleUi).setState(state);
	}

	/**
	 * Queries the state of a button/checkbox
	 *
	 * @param text
	 *            the text label of the button/checkbox
	 * @return the current state of the button
	 */
	public boolean getButtonState(String text) {
		return MCButton.getByLabel(getDialogShell(), text, waitForIdleUi).getSelection();
	}

	/**
	 * Enters text at the specified named text field, this is the only way to ensure that the text
	 * is entered in the right place
	 *
	 * @param name
	 *            the name of the text field instance
	 * @param text
	 *            the text to enter
	 */
	public void enterText(String name, String text) {
		if (text == null) {
			return;
		}
		MCText.getByName(getDialogShell(), name).setText(text);
	}

	/**
	 * Enters text at the first visible text field found
	 *
	 * @param text
	 *            the text to be entered
	 */
	public void enterText(String text) {
		MCText.getFirstVisible(this).setText(text);
	}

	/**
	 * Finds a text input field with the supplied old text and replaces that text with the new one.
	 *
	 * @param oldText
	 *            the old text to replace
	 * @param newText
	 *            the new text replacing the old one
	 */
	public void replaceText(String oldText, String newText) {
		MCText.getByText(getDialogShell(), oldText).setText(newText);
	}

	/**
	 * Finds a text input field with the supplied tooltip text and replaces that text.
	 *
	 * @param tooltip
	 *            the tooltip text of the text field
	 * @param text
	 *            the text to set to this field
	 */
	public void setToolTipText(String tooltip, String text) {
		MCText.getByToolTip(getDialogShell(), tooltip).setText(text);
	}

	/**
	 * Backwards compatibility. Only to be used for old style Jemmy tests to get hold of the actual
	 * shell wrap
	 *
	 * @return a wrapped shell of the dialog
	 */
	public Wrap<? extends Shell> getWrap() {
		return getDialogShell();
	}

	/**
	 * Waits for a dialog shell to close (5 seconds)
	 *
	 * @param dialogTitle
	 *            the title of the dialog
	 * @return {@code true} if closed within the time limit. Otherwise {@code false}
	 */
	public static boolean waitForClose(String dialogTitle) {
		return waitForClose(dialogTitle, DEAFULT_DIALOG_WAIT_TIMEOUT_MS);
	}

	/**
	 * Wait for a dialog shell to close
	 *
	 * @param dialogTitle
	 *            the title of the dialog
	 * @param maxWaitMs
	 *            the max amount of milliseconds to wait for the dialog to close
	 * @return {@code true} if closed within the time limit. Otherwise {@code false}
	 */
	public static boolean waitForClose(String dialogTitle, long maxWaitMs) {
		return waitForClose(dialogTitle, true, maxWaitMs);
	}

	/**
	 * Sets the wait behavior of this dialog
	 *
	 * @param wait
	 *            {@code true} if idle wait is desired. Otherwise {@code false}
	 */
	public void setIdleUiWait(boolean wait) {
		waitForIdleUi = wait;
	}

	/**
	 * @return the first visible table in this dialog
	 */
	public MCTable getFirstTable() {
		return getTable(0);
	}

	/**
	 * @param index
	 *            the index of the table to return
	 * @return the table with the provided index from the lookup
	 */
	public MCTable getTable(int index) {
		return MCTable.getAll(this).get(index);
	}
	
	/**
	 * Returns the first {@link MCTree} in the SWT hierarchy for this {@link MCDialog}
	 *
	 * @return a {@link MCTree}
	 */
	public MCTree getFirstTree() {
		return MCTree.getFirst(getDialogShell());
	}

	/**
	 * Returns all currently visible tables as {@link MCTable} in a list.
	 *
	 * @return a {@link List} of {@link MCTable}
	 */
	public List<MCTable> getAllTables() {
		return MCTable.getAll(getDialogShell());
	}
	
	private static boolean waitForClose(String dialogTitle, boolean waitForIdle, long maxWaitMs) {
		long lookupEndTime = System.currentTimeMillis() + maxWaitMs;
		do {
			if (getVisible(Shells.SHELLS.lookup(new ByTextShell<>(dialogTitle)), waitForIdle, maxWaitMs, false)
					.size() == 0) {
				return true;
			}
			sleep(MCJemmyBase.LOOKUP_SLEEP_TIME_MS);
		} while (lookupEndTime > System.currentTimeMillis());
		return false;
	}

}
