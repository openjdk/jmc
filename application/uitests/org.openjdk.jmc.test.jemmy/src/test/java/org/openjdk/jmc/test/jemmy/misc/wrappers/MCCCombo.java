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

import org.eclipse.swt.custom.CCombo;
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
 * The Jemmy wrapper for CCombo objects
 */
public class MCCCombo extends MCJemmyBase {
	private static final StringComparePolicy DEFAULT_POLICY = StringComparePolicy.EXACT;

	private MCCCombo(Wrap<? extends CCombo> ccomboWrap) {
		this.control = ccomboWrap;
	}

	/**
	 * Returns a {@link MCCCombo} object named according to the supplied name
	 *
	 * @param name
	 *            the name of the CCombo to match
	 * @return a {@link MCCCombo} object that matches the name
	 */
	public static MCCCombo getByName(String name) {
		return getByName(getShell(), name);
	}

	/**
	 * Returns a {@link MCCCombo} object named according to the supplied name
	 *
	 * @param shell
	 *            the shell from where to start the search for the CCombo object
	 * @param name
	 *            the name of the CCombo to match
	 * @return a {@link MCCCombo} object that matches the name
	 */
	@SuppressWarnings("unchecked")
	public static MCCCombo getByName(Wrap<? extends Shell> shell, String name) {
		return new MCCCombo(shell.as(Parent.class, CCombo.class).lookup(CCombo.class, new ByName<>(name)).wrap());
	}

	/**
	 * Returns a {@link MCCCombo} object having at least one selectable item that matches the text provided
	 *
	 * @param text
	 *            the text string of the item to match
	 * @return a {@link MCCCombo} object that matches the text
	 */
	public static MCCCombo getByText(String text) {
		return getByText(getShell(), text);
	}

	/**
	 * Returns a {@link MCCCombo} object having at least one selectable item that matches the text provided
	 *
	 * @param shell
	 *            the shell from where to start the search for the CCombo object
	 * @param text
	 *            the text string of the item to match
	 * @return a {@link MCCCombo} object that matches the text
	 */
	public static MCCCombo getByText(Wrap<? extends Shell> shell, String text) {
		return getByText(shell, text, DEFAULT_POLICY);
	}

	/**
	 * Returns a {@link MCCCombo} object having at least one selectable item that matches the text provided
	 *
	 * @param shell
	 *            the shell from where to start the search for the CCombo object
	 * @param text
	 *            the text string of the item to match
	 * @param policy
	 *            the policy to use when matching
	 * @return a {@link MCCCombo} object that matches the text
	 */
	@SuppressWarnings("unchecked")
	public static MCCCombo getByText(Wrap<? extends Shell> shell, String text, StringComparePolicy policy) {
		return new MCCCombo(
				shell.as(Parent.class, CCombo.class).lookup(CCombo.class, new ByItemLookup<>(text, policy)).wrap());
	}

	/**
	 * Returns all visible {@link MCCCombo} objects underneath the supplied shell
	 *
	 * @param shell
	 *            the shell from where to start the search for the CCombo object
	 * @return a {@link List} of {@link MCCCombo} objects
	 */
	@SuppressWarnings("unchecked")
	public static List<MCCCombo> getAll(Wrap<? extends Shell> shell) {
		List<Wrap<? extends CCombo>> list = getVisible(shell.as(Parent.class, CCombo.class).lookup(CCombo.class));
		List<MCCCombo> ccombos = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			ccombos.add(new MCCCombo(list.get(i)));
		}
		return ccombos;
	}

	/**
	 * Returns all visible CCombo objects underneath the supplied {@link MCDialog}
	 *
	 * @param dialog
	 *            the {@link MCDialog} from where to start the search for CCombo objects
	 * @return a {@link List} of {@link MCCCombo} objects
	 */
	public static List<MCCCombo> getAll(MCDialog dialog) {
		return getAll(dialog.getDialogShell());
	}

	/**
	 * Returns the first visible {@link MCCCombo} object underneath the supplied shell
	 *
	 * @param shell
	 *            the shell from where to start the search for the CCombo object
	 * @return a {@link MCCCombo} object
	 */
	public static MCCCombo getFirst(Wrap<? extends Shell> shell) {
		return getAll(shell).get(0);
	}

	/**
	 * Returns the first visible {@link MCCCombo} object underneath the supplied {@link MCDialog}
	 *
	 * @param dialog
	 *            the {@link MCDialog} from where to start the search for CCombo objects
	 * @return a {@link MCCCombo} object
	 */
	public static MCCCombo getFirst(MCDialog dialog) {
		return getFirst(dialog.getDialogShell());
	}

	/**
	 * Returns the first visible {@link MCCCombo} object underneath the Mission Control main shell
	 * 
	 * @return a {@link MCCCombo} object
	 */
	public static MCCCombo getFirst() {
		return getFirst(getShell());
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
	 * @return a list of all items
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
	 * Sets the text of this {@link MCCCombo}
	 *
	 * @param text
	 *            the text to set
	 */
	public void setText(String text) {
		SelectionText textField = control.as(SelectionText.class);
		click();
		textField.clear();
		textField.type(text);
		if (isContentAssistPresent()) {
			control.keyboard().pushKey(KeyboardButtons.ESCAPE);
		}
	}
}
