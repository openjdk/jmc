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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jemmy.Point;
import org.jemmy.control.Wrap;
import org.jemmy.interfaces.Parent;
import org.jemmy.lookup.Lookup;
import org.jemmy.swt.ControlWrap;
import org.jemmy.swt.lookup.ByName;
import org.jemmy.swt.lookup.ByTextControlLookup;
import org.junit.Assert;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.fetchers.Fetcher;

/**
 * The Jemmy wrapper for Buttons
 */
public class MCButton extends MCJemmyBase {

	private MCButton(Wrap<? extends Button> button) {
		this.control = button;
	}

	/**
	 * Finds a button in the default Mission Control shell and returns it.
	 *
	 * @param label
	 *            the {@link MCButton} Label of the button
	 * @return a {@link MCButton} in the default shell matching the label
	 */
	public static MCButton getByLabel(Labels label) {
		return getByLabel(getShell(), label);
	}

	/**
	 * Finds a button in the default Mission Control shell and returns it.
	 *
	 * @param label
	 *            the {@link MCButton} Label of the button
	 * @param waitForIdle
	 *            {@code true} if supposed to wait for an idle UI before trying to find the Button
	 * @return a {@link MCButton} in the default shell matching the label
	 */
	public static MCButton getByLabel(Labels label, boolean waitForIdle) {
		return getByLabel(getShell(), label, waitForIdle);
	}

	/**
	 * Finds a button in the default Mission Control shell and returns it.
	 *
	 * @param label
	 *            the label string of the button
	 * @return a {@link MCButton} in the default shell matching the label
	 */
	public static MCButton getByLabel(String label) {
		return getByLabel(getShell(), label);
	}

	/**
	 * Finds a button by button label and returns it
	 *
	 * @param shell
	 *            the shell where to find the button
	 * @param label
	 *            the {@link MCButton} Label of the button
	 * @param waitForIdle
	 *            {@code true} if supposed to wait for an idle UI before trying to find the Button
	 * @return a {@link MCButton} in the correct shell matching the label
	 */
	public static MCButton getByLabel(Wrap<? extends Shell> shell, Labels label, boolean waitForIdle) {
		return getByLabel(shell, Labels.getButtonLabel(label), waitForIdle);
	}

	/**
	 * Finds a button by button label and returns it
	 *
	 * @param shell
	 *            the shell where to find the button
	 * @param label
	 *            the {@link MCButton} Label of the button
	 * @return a {@link MCButton} in the correct shell matching the label
	 */
	public static MCButton getByLabel(Wrap<? extends Shell> shell, Labels label) {
		return getByLabel(shell, Labels.getButtonLabel(label));
	}

	/**
	 * Finds a button by button label string and returns it
	 *
	 * @param shell
	 *            the shell where to find the button
	 * @param label
	 *            the label string of the button
	 * @return a {@link MCButton} in the correct shell matching the label
	 */
	public static MCButton getByLabel(Wrap<? extends Shell> shell, String label) {
		return getByLabel(shell, label, true);
	}

	/**
	 * Finds a button by button label string and returns it
	 *
	 * @param shell
	 *            the shell where to find the button
	 * @param label
	 *            the label string of the button
	 * @param waitForIdle
	 *            {@code true} if supposed to wait for an idle UI before trying to find the Button
	 * @return a {@link MCButton} in the correct shell matching the label
	 */
	@SuppressWarnings("unchecked")
	public static MCButton getByLabel(Wrap<? extends Shell> shell, String label, boolean waitForIdle) {
		Lookup<Button> lookup = shell.as(Parent.class, Button.class).lookup(Button.class,
				new ByTextControlLookup<Button>(label));
		return new MCButton(getVisible(lookup, waitForIdle).get(0));
	}

	/**
	 * Finds a button by button label string and returns it
	 *
	 * @param dialog
	 *            the {@link MCDialog} where to find the button
	 * @param label
	 *            the label string of the button
	 * @param waitForIdle
	 *            {@code true} if supposed to wait for an idle UI before trying to find the Button
	 * @return a {@link MCButton} in the correct dialog matching the label
	 */
	public static MCButton getByLabel(MCDialog dialog, String label, boolean waitForIdle) {
		return getByLabel(dialog.getDialogShell(), label, waitForIdle);
	}

	/**
	 * Finds a button by button label string and returns it
	 *
	 * @param dialog
	 *            the {@link MCDialog} where to find the button
	 * @param label
	 *            the {@link MCButton} Label of the button
	 * @param waitForIdle
	 *            {@code true} if supposed to wait for an idle UI before trying to find the Button
	 * @return a {@link MCButton} in the correct dialog matching the label
	 */
	public static MCButton getByLabel(MCDialog dialog, Labels label, boolean waitForIdle) {
		return getByLabel(dialog, Labels.getButtonLabel(label), waitForIdle);
	}

	/**
	 * Finds a button, visible or not, by button label string and returns it
	 *
	 * @param shell
	 *            the shell where to find the button
	 * @param label
	 *            the label string of the button
	 * @return a {@link MCButton} in the correct shell matching the label, {@code null} if not
	 *         found
	 */
	@SuppressWarnings("unchecked")
	public static MCButton getAnyByLabel(Wrap<? extends Shell> shell, String label) {
		Lookup<Button> lookup = shell.as(Parent.class, Button.class).lookup(Button.class,
				new ByTextControlLookup<Button>(label));
		if (lookup.size() > 0) {
			return new MCButton(lookup.wrap(0));
		} else {
			return null;
		}
	}

	/**
	 * Finds a button, visible or not, by name
	 *
	 * @param shell
	 *            the shell where to find the button
	 * @param name
	 *            the name of the button
	 * @return a {@link MCButton} matching the name, {@code null} if not found
	 */
	@SuppressWarnings("unchecked")
	public static MCButton getByName(Wrap<? extends Shell> shell, String name) {
		return new MCButton(shell.as(Parent.class, Button.class).lookup(Button.class, new ByName<>(name)).wrap());
	}

	/**
	 * Finds a button, visible or not, by name (in the main shell of Mission Control)
	 *
	 * @param name
	 *            the name of the button
	 * @return a {@link MCButton} matching the name, {@code null} if not found
	 */
	public static MCButton getByName(String name) {
		return getByName(getShell(), name);
	}

	/**
	 * Finds all visible buttons in the supplied shell and returns a {@link List} of these
	 *
	 * @param shell
	 *            the shell where to search for buttons
	 * @return a {@link List} of {@link MCButton} (possibly empty)
	 */
	@SuppressWarnings("unchecked")
	public static List<MCButton> getVisible(Wrap<? extends Shell> shell) {
		List<Wrap<? extends Button>> allVisibleButtonWraps = getVisible(
				shell.as(Parent.class, Button.class).lookup(Button.class));
		List<MCButton> allVisibleMcButtons = new ArrayList<>();
		for (Wrap<? extends Button> buttonWrap : allVisibleButtonWraps) {
			allVisibleMcButtons.add(new MCButton(buttonWrap));
		}
		return allVisibleMcButtons;
	}

	/**
	 * Finds all visible buttons in the supplied shell and returns a {@link List} of these
	 *
	 * @param shell
	 *            the shell where to search for buttons
	 * @param waitForIdle
	 *            {@code true} if supposed to wait for the UI to be idle before ending the lookup
	 * @return a {@link List} of {@link MCButton} (possibly empty)
	 */
	@SuppressWarnings("unchecked")
	public static List<MCButton> getVisible(Wrap<? extends Shell> shell, boolean waitForIdle) {
		List<Wrap<? extends Button>> allVisibleButtonWraps = getVisible(
				shell.as(Parent.class, Button.class).lookup(Button.class), waitForIdle);
		List<MCButton> allVisibleMcButtons = new ArrayList<>();
		for (Wrap<? extends Button> buttonWrap : allVisibleButtonWraps) {
			allVisibleMcButtons.add(new MCButton(buttonWrap));
		}
		return allVisibleMcButtons;
	}

	/**
	 * Gets the selection state of the button.
	 *
	 * @return {@code true} if selected, otherwise {@code false}
	 */
	public boolean getSelection() {
		Fetcher<Boolean> fetcher = new Fetcher<Boolean>() {
			@Override
			public void run() {
				setOutput(getWrap().getControl().getSelection());
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Sets the state of the button/checkbox with retries in case it is a checkbox that may be grey.
	 * Sets the click point very close to the origin of the button instead of centered in order to
	 * ensure that Mac OS X will work as well
	 *
	 * @param state
	 *            the desired state of the button/checkbox
	 */
	public void setState(boolean state) {
		int maxRetries = 10;
		int currentRetry = 0;
		while (getSelection() != state && maxRetries > currentRetry) {
			currentRetry++;
			control.mouse().click(1, new Point(1, 1));
			sleep(200);
		}
		Assert.assertTrue("Unable to set Button state to " + state, getSelection() == state);
	}

	public static enum Labels {
		OK, FINISH, CANCEL, CLOSE, YES, NEXT, NO, APPLY_AND_CLOSE;

		public static String getButtonLabel(Labels buttonLabel) {
			String labelString = "";

			switch (buttonLabel) {
			case YES:
				labelString = IDialogConstants.YES_LABEL;
				break;
			case CANCEL:
				labelString = IDialogConstants.CANCEL_LABEL;
				break;
			case CLOSE:
				labelString = IDialogConstants.CLOSE_LABEL;
				break;
			case FINISH:
				labelString = IDialogConstants.FINISH_LABEL;
				break;
			case NEXT:
				labelString = IDialogConstants.NEXT_LABEL;
				break;
			case OK:
				labelString = IDialogConstants.OK_LABEL;
				break;
			case NO:
				labelString = IDialogConstants.NO_LABEL;
				break;
			case APPLY_AND_CLOSE:
				labelString = "Apply and Close";
			}
			return labelString;
		}
	}

	@SuppressWarnings("unchecked")
	private Wrap<? extends Button> getWrap() {
		return control.as(ControlWrap.class);
	}
}
