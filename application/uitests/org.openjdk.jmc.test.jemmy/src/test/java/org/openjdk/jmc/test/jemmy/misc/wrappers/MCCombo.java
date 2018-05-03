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
import java.util.stream.Collectors;

import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Shell;
import org.jemmy.control.Wrap;
import org.jemmy.input.SelectionText;
import org.jemmy.interfaces.Keyboard.KeyboardButtons;
import org.jemmy.interfaces.Parent;
import org.jemmy.interfaces.Selectable;
import org.jemmy.resources.StringComparePolicy;
import org.jemmy.swt.lookup.ByItemLookup;
import org.jemmy.swt.lookup.ByName;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;

/**
 * The Jemmy wrapper for Combo objects
 */
public class MCCombo extends MCJemmyBase {
	private static final StringComparePolicy DEFAULT_POLICY = StringComparePolicy.EXACT;

	private MCCombo(Wrap<? extends Combo> comboWrap) {
		this.control = comboWrap;
	}

	/**
	 * Returns a Combo object named according to the supplied name
	 *
	 * @param name
	 *            the name of the Combo to match
	 * @return a {@link MCCombo} object that matches the name
	 */
	public static MCCombo getByName(String name) {
		return getByName(getShell(), name);
	}

	/**
	 * Returns a Combo object named according to the supplied name
	 *
	 * @param shell
	 *            the shell from where to start the search for the Combo object
	 * @param name
	 *            the name of the Combo to match
	 * @return a {@link MCCombo} object that matches the name
	 */
	@SuppressWarnings("unchecked")
	public static MCCombo getByName(Wrap<? extends Shell> shell, String name) {
		return new MCCombo(shell.as(Parent.class, Combo.class).lookup(Combo.class, new ByName<>(name)).wrap());
	}

	/**
	 * Returns a Combo object named according to the supplied name
	 *
	 * @param shell
	 *            the shell from where to start the search for the Combo object
	 * @param name
	 *            the name of the Combo to match
	 * @param maxWaitMs
	 *            the timeout in milliseconds before ending the lookup
	 * @return a {@link MCCombo} object that matches the name (or null)
	 */
	@SuppressWarnings("unchecked")
	public static MCCombo getVisibleByName(Wrap<? extends Shell> shell, String name, long maxWaitMs) {
		List<Wrap<? extends Combo>> mcCombos = getVisible(
				shell.as(Parent.class, Combo.class).lookup(Combo.class, new ByName<>(name)), false, maxWaitMs, false);
		if (mcCombos.size() == 1) {
			return new MCCombo(mcCombos.get(0));
		} else {
			return null;
		}
	}

	/**
	 * Returns a Combo object having at least one selectable item that matches the text provided
	 *
	 * @param text
	 *            the text string of the item to match
	 * @return a {@link MCCombo} object that matches the text
	 */
	public static MCCombo getByText(String text) {
		return getByText(getShell(), text);
	}

	/**
	 * Returns a Combo object having at least one selectable item that matches the text provided
	 *
	 * @param shell
	 *            the shell from where to start the search for the Combo object
	 * @param text
	 *            the text string of the item to match
	 * @return a {@link MCCombo} object that matches the text
	 */
	public static MCCombo getByText(Wrap<? extends Shell> shell, String text) {
		return getByText(shell, text, DEFAULT_POLICY);
	}

	/**
	 * Returns a Combo object having at least one selectable item that matches the text provided
	 *
	 * @param shell
	 *            the shell underneath where to search for Combo the object
	 * @param text
	 *            the text string of the item to match
	 * @param policy
	 *            the policy to use when matching
	 * @return a {@link MCCombo} object that matches the text
	 */
	@SuppressWarnings("unchecked")
	public static MCCombo getByText(Wrap<? extends Shell> shell, String text, StringComparePolicy policy) {
		return new MCCombo(
				shell.as(Parent.class, Combo.class).lookup(Combo.class, new ByItemLookup<>(text, policy)).wrap());
	}

	/**
	 * Returns all visible Combo objects underneath the supplied shell
	 *
	 * @param shell
	 *            the shell underneath where to search for Combo objects
	 * @return a {@link List} of {@link MCCombo} objects
	 */
	public static List<MCCombo> getVisible(Wrap<? extends Shell> shell) {
		return getVisible(shell, true, 10000);
	}

	/**
	 * Returns all visible Combo objects underneath the supplied shell
	 *
	 * @param shell
	 *            the shell underneath where to search for Combo objects
	 * @param waitForIdle
	 *            {@code true} if supposed to wait for idle UI before looking up
	 * @return a {@link List} of {@link MCCombo} objects
	 */
	public static List<MCCombo> getVisible(Wrap<? extends Shell> shell, boolean waitForIdle) {
		return getVisible(shell, waitForIdle, 10000);
	}

	/**
	 * Returns all visible Combo objects underneath the supplied shell
	 *
	 * @param shell
	 *            the shell underneath where to search for Combo objects
	 * @param waitForIdle
	 *            {@code true} if supposed to wait for idle UI before looking up
	 * @param maxWaitMs
	 *            milliseconds to wait before timing out
	 * @return a {@link List} of {@link MCCombo} objects
	 */
	@SuppressWarnings("unchecked")
	public static List<MCCombo> getVisible(Wrap<? extends Shell> shell, boolean waitForIdle, long maxWaitMs) {
		List<Wrap<? extends Combo>> allVisibleCombos = getVisible(
				shell.as(Parent.class, Combo.class).lookup(Combo.class), waitForIdle, maxWaitMs, false);
		return allVisibleCombos.stream().map(w -> new MCCombo(w)).collect(Collectors.toList());
	}

	/**
	 * Selects the item matching (exactly) the supplied name
	 *
	 * @param name
	 *            the text string of the item to select
	 */
	@SuppressWarnings("unchecked")
	public void select(String name) {
		control.as(Selectable.class).selector().select(name);
	}

	/**
	 * @return a list of all items texts
	 */
	@SuppressWarnings("unchecked")
	public List<String> getStates() {
		return control.getProperty(List.class, Selectable.STATES_PROP_NAME);
	}

	/**
	 * @return the currently selected item
	 */
	public String getState() {
		return control.getProperty(String.class, Selectable.STATE_PROP_NAME);
	}

	/**
	 * Sets the text of this {@link MCCombo}
	 *
	 * @param text
	 *            the text to set
	 */
	public void setText(String text) {
		SelectionText textField = control.as(SelectionText.class);
		click();
		/*
		 * Not using clear() as the combo just fills up with the default choice as soon as the text
		 * gets emptied. Instead, we select all and then type the desired value
		 */
		control.keyboard().pushKey(KeyboardButtons.A, SHORTCUT_MODIFIER);
		textField.type(text);
		if (isContentAssistPresent()) {
			control.keyboard().pushKey(KeyboardButtons.ESCAPE);
		}
	}
}
