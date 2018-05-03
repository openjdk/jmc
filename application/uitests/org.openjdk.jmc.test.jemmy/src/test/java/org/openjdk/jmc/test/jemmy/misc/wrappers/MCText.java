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

import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jemmy.control.Wrap;
import org.jemmy.input.SelectionText;
import org.jemmy.interfaces.Keyboard.KeyboardButtons;
import org.jemmy.interfaces.Parent;
import org.jemmy.resources.StringComparePolicy;
import org.jemmy.swt.lookup.ByName;
import org.jemmy.swt.lookup.ByTextTextLookup;
import org.jemmy.swt.lookup.ByToolTipControl;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;

/**
 * The Jemmy base wrapper for text fields
 */
public class MCText extends MCJemmyBase {
	private static final StringComparePolicy EXACT_POLICY = StringComparePolicy.EXACT;
	private static final StringComparePolicy SUBSTRING_POLICY = StringComparePolicy.SUBSTRING;

	private MCText(Wrap<? extends Text> textWrap) {
		this.control = textWrap;
	}

	/**
	 * Finds and returns a {@link MCText}
	 * 
	 * @param shell
	 *            the shell from where to start searching for this text widget
	 * @param name
	 *            the name of the text widget
	 * @param policy
	 *            A {@link StringComparePolicy} for matching the name
	 * @return a {@link MCText} that matches the name
	 */
	@SuppressWarnings("unchecked")
	public static MCText getByName(Wrap<? extends Shell> shell, String name, StringComparePolicy policy) {
		return new MCText(shell.as(Parent.class, Text.class).lookup(Text.class, new ByName<>(name, policy)).wrap());
	}

	/**
	 * Finds and returns a {@link MCText}
	 * 
	 * @param shell
	 *            the shell from where to start searching for this text widget
	 * @param name
	 *            the name of the text widget (matched by using the default matching policy,
	 *            {@link StringComparePolicy.EXACT})
	 * @param maxWaitMs
	 *            the maximum amount of milliseconds to wait before giving up
	 * @return a {@link MCText} that matches the name or {@code null} if not found
	 */
	@SuppressWarnings("unchecked")
	public static MCText getVisibleByName(Wrap<? extends Shell> shell, String name, long maxWaitMs) {
		List<Wrap<? extends Text>> textWraps = getVisible(
				shell.as(Parent.class, Text.class).lookup(Text.class, new ByName<>(name, EXACT_POLICY)), false,
				maxWaitMs, false);
		if (textWraps.size() == 1) {
			return new MCText(textWraps.get(0));
		} else {
			return null;
		}

	}

	/**
	 * Finds and returns a {@link MCText}
	 * 
	 * @param shell
	 *            the shell from where to start searching for this text widget
	 * @param name
	 *            the name of the text widget (matched by using the default matching policy,
	 *            {@link StringComparePolicy.EXACT})
	 * @return a {@link MCText} that matches the name
	 */
	public static MCText getByName(Wrap<? extends Shell> shell, String name) {
		return getByName(shell, name, EXACT_POLICY);
	}

	/**
	 * Finds and returns a {@link MCText}
	 * 
	 * @param dialog
	 *            the {@link MCDialog} from where to start searching for this text widget
	 * @param name
	 *            the name of the text widget (matched by using the default matching policy,
	 *            {@link StringComparePolicy.EXACT})
	 * @return a {@link MCText} that matches the name
	 */
	public static MCText getByName(MCDialog dialog, String name) {
		return getByName(dialog.getDialogShell(), name, EXACT_POLICY);
	}

	/**
	 * Finds and returns a {@link MCText}
	 * 
	 * @param name
	 *            the name of the text widget (matched by using the default matching policy,
	 *            {@link StringComparePolicy.EXACT})
	 * @return a {@link MCText} that matches the name
	 */
	public static MCText getByName(String name) {
		return getByName(getShell(), name, EXACT_POLICY);
	}

	/**
	 * Finds and returns a {@link MCText}
	 * 
	 * @param dialog
	 *            the {@link MCDialog} from where to start searching for this text widget
	 * @param tooltip
	 *            the tooltip of the text widget
	 * @param policy
	 *            A {@link StringComparePolicy} for matching the name
	 * @return a {@link MCText} that matches the tooltip
	 */
	@SuppressWarnings("unchecked")
	public static MCText getByToolTip(MCDialog dialog, String tooltip, StringComparePolicy policy) {
		return new MCText(dialog.getDialogShell().as(Parent.class, Text.class)
				.lookup(Text.class, new ByToolTipControl<Text>(tooltip, policy)).wrap());
	}

	/**
	 * Finds and returns a {@link MCText}
	 * 
	 * @param dialog
	 *            the {@link MCDialog} from where to start searching for this text widget
	 * @param tooltip
	 *            the tooltip of the text widget (matched by using the default matching policy,
	 *            {@link StringComparePolicy.EXACT})
	 * @return a {@link MCText} that matches the tooltip
	 */
	public static MCText getByToolTip(MCDialog dialog, String tooltip) {
		return getByToolTip(dialog, tooltip, EXACT_POLICY);
	}

	/**
	 * Finds and returns a {@link MCText}
	 * 
	 * @param shell
	 *            the shell from where to start searching for this text widget
	 * @param tooltip
	 *            the tooltip of the text widget
	 * @param policy
	 *            a {@link StringComparePolicy} for matching the name
	 * @return a {@link MCText} that matches the tooltip
	 */
	@SuppressWarnings("unchecked")
	public static MCText getByToolTip(Wrap<? extends Shell> shell, String tooltip, StringComparePolicy policy) {
		return new MCText(shell.as(Parent.class, Text.class)
				.lookup(Text.class, new ByToolTipControl<Text>(tooltip, policy)).wrap());
	}

	/**
	 * Finds and returns a {@link MCText}
	 * 
	 * @param shell
	 *            the shell from where to start searching for this text widget
	 * @param tooltip
	 *            the tooltip of the text widget (matched by using the default matching policy,
	 *            {@link StringComparePolicy.EXACT})
	 * @return a {@link MCText} that matches the tooltip
	 */
	public static MCText getByToolTip(Wrap<? extends Shell> shell, String tooltip) {
		return getByToolTip(shell, tooltip, EXACT_POLICY);
	}

	/**
	 * Finds and returns a {@link MCText} (in the main Mission Control Window)
	 * 
	 * @param tooltip
	 *            the tooltip of the text widget (matched by using the default matching policy,
	 *            {@link StringComparePolicy.EXACT})
	 * @return a {@link MCText} that matches the tooltip
	 */
	public static MCText getByToolTip(String tooltip) {
		return getByToolTip(getShell(), tooltip, EXACT_POLICY);
	}

	/**
	 * Finds and returns a {@link MCText}
	 * 
	 * @param dialog
	 *            the {@link MCDialog} from where to start searching for this text widget
	 * @param text
	 *            the current text of the text widget
	 * @param policy
	 *            a {@link StringComparePolicy} for matching the text
	 * @return a {@link MCText} that matches the text
	 */
	@SuppressWarnings("unchecked")
	public static MCText getByText(MCDialog dialog, String text, StringComparePolicy policy) {
		return new MCText(dialog.getDialogShell().as(Parent.class, Text.class)
				.lookup(Text.class, new ByTextTextLookup<>(text, policy)).wrap());
	}

	/**
	 * Finds and returns a {@link MCText}
	 * 
	 * @param dialog
	 *            the {@link MCDialog} from where to start searching for this text widget
	 * @param text
	 *            the current text of the text widget (matched by using the default matching policy,
	 *            {@link StringComparePolicy.EXACT})
	 * @return a {@link MCText} that matches the text
	 */
	public static MCText getByText(MCDialog dialog, String text) {
		return getByText(dialog, text, EXACT_POLICY);
	}

	/**
	 * Finds and returns a {@link MCText}
	 * 
	 * @param shell
	 *            the shell from where to start searching for this text widget
	 * @param text
	 *            the current text of the text widget
	 * @param policy
	 *            a {@link StringComparePolicy} for matching the text
	 * @return a {@link MCText} that matches the text
	 */
	@SuppressWarnings("unchecked")
	public static MCText getByText(Wrap<? extends Shell> shell, String text, StringComparePolicy policy) {
		return new MCText(
				shell.as(Parent.class, Text.class).lookup(Text.class, new ByTextTextLookup<>(text, policy)).wrap());
	}

	/**
	 * Finds and returns a {@link MCText}
	 * 
	 * @param shell
	 *            the shell from where to start searching for this text widget
	 * @param text
	 *            the current text of the text widget (matched by using the default matching policy,
	 *            {@link StringComparePolicy.EXACT})
	 * @return a {@link MCText} that matches the text
	 */
	public static MCText getByText(Wrap<? extends Shell> shell, String text) {
		return getByText(shell, text, EXACT_POLICY);
	}

	/**
	 * Finds and returns a {@link MCText} (in the main Mission Control Window)
	 * 
	 * @param text
	 *            the current text of the text widget (matched by using the substring matching
	 *            policy, {@link StringComparePolicy.SUBSTRING})
	 * @return a {@link MCText} that matches the text
	 */
	public static MCText getByTextSubstring(String text) {
		return getByText(getShell(), text, SUBSTRING_POLICY);
	}

	/**
	 * Finds and returns a {@link MCText} (in the main Mission Control Window)
	 * 
	 * @param text
	 *            the current text of the text widget (matched by using the default matching policy,
	 *            {@link StringComparePolicy.EXACT})
	 * @return a {@link MCText} that matches the text
	 */
	public static MCText getByText(String text) {
		return getByText(getShell(), text, EXACT_POLICY);
	}

	/**
	 * Returns the first found {@link MCText}
	 * 
	 * @param shell
	 *            the shell from where to start searching for this text widget
	 * @return a {@link MCText} that is visible and shows up first in the lookup
	 */
	public static MCText getFirstVisible(Wrap<? extends Shell> shell) {
		return getVisible(shell, 0);
	}

	/**
	 * Returns the first found {@link MCText}
	 * 
	 * @param dialog
	 *            the {@link MCDialog} from where to start searching for this text widget
	 * @return a {@link MCText} that is visible and shows up first in the lookup
	 */
	public static MCText getFirstVisible(MCDialog dialog) {
		return getVisible(dialog.getDialogShell(), 0);
	}

	/**
	 * Returns the N'th visible {@link MCText}
	 * 
	 * @param shell
	 *            the shell from where to start searching for this text widget
	 * @param index
	 *            the index of the visible text widget to return
	 * @return a {@link MCText} that is visible and shows up in the lookup at the index of the
	 *         given parameter
	 */
	@SuppressWarnings("unchecked")
	public static MCText getVisible(Wrap<? extends Shell> shell, int index) {
		return new MCText(
				(Wrap<? extends Text>) getVisible(shell.as(Parent.class, Text.class).lookup(Text.class)).get(index));
	}

	/**
	 * Finds all visible text items underneath the supplied shell
	 *
	 * @param shell
	 *            the shell from where to start searching for this text widget
	 * @return a {@link List} of {@link MCText} (possibly empty)
	 */
	@SuppressWarnings("unchecked")
	public static List<MCText> getVisible(Wrap<? extends Shell> shell) {
		List<Wrap<? extends Text>> allVisibleTextWraps = getVisible(
				shell.as(Parent.class, Text.class).lookup(Text.class));
		List<MCText> allVisibleMcTexts = new ArrayList<>();
		for (Wrap<? extends Text> textWrap : allVisibleTextWraps) {
			allVisibleMcTexts.add(new MCText(textWrap));
		}
		return allVisibleMcTexts;
	}

	/**
	 * Finds all visible text items underneath the supplied shell
	 *
	 * @param shell
	 *            the shell from where to start searching for this text widget
	 * @param waitForIdle
	 *            {@code true} if supposed to wait for the UI to be idle before performing the
	 *            lookup
	 * @return a {@link List} of {@link MCText} (possibly empty)
	 */
	@SuppressWarnings("unchecked")
	public static List<MCText> getVisible(Wrap<? extends Shell> shell, boolean waitForIdle) {
		List<Wrap<? extends Text>> allVisibleTextWraps = getVisible(
				shell.as(Parent.class, Text.class).lookup(Text.class), waitForIdle);
		List<MCText> allVisibleMcTexts = new ArrayList<>();
		for (Wrap<? extends Text> textWrap : allVisibleTextWraps) {
			allVisibleMcTexts.add(new MCText(textWrap));
		}
		return allVisibleMcTexts;
	}

	/**
	 * Returns the N'th visible {@link MCText} (in the main Mission Control Window)
	 * 
	 * @param index
	 *            the index of the visible text widget to return
	 * @return a {@link MCText} that is visible and shows up in the lookup at the index of the
	 *         given parameter
	 */
	public static MCText getVisible(int index) {
		return getVisible(getShell(), index);
	}

	/**
	 * Returns all visible {@link MCText} (in the main Mission Control Window)
	 * 
	 * @return a {@link List} of {@link MCText} (possibly empty)
	 */
	public static List<MCText> getVisible() {
		return getVisible(getShell());
	}

	/**
	 * Returns all visible {@link MCText} (in the main Mission Control Window)
	 * 
	 * @param waitForIdle
	 *            {@code true} if supposed to wait for the UI to be idle before performing the
	 *            lookup
	 * @return a {@link List} of {@link MCText} (possibly empty)
	 */
	public static List<MCText> getVisible(boolean waitForIdle) {
		return getVisible(getShell(), waitForIdle);
	}

	/**
	 * Sets the text of this {@link MCText}
	 * 
	 * @param newText
	 *            the text to set for this text widget
	 */
	public void setText(String newText) {
		SelectionText textField = control.as(SelectionText.class);
		click();
		control.keyboard().pushKey(KeyboardButtons.A, SHORTCUT_MODIFIER);
		control.keyboard().pushKey(KeyboardButtons.DELETE);
		textField.type(newText);
		if (isContentAssistPresent()) {
			control.keyboard().pushKey(KeyboardButtons.ESCAPE);
		}
	}
}
