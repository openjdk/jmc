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

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jemmy.control.Wrap;
import org.jemmy.interfaces.Parent;
import org.jemmy.resources.StringComparePolicy;
import org.jemmy.swt.lookup.ByName;
import org.jemmy.swt.lookup.ByTextLabelLookup;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;

/**
 * The Jemmy base wrapper for labels
 */
public class MCLabel extends MCJemmyBase {
	private static final StringComparePolicy EXACT_POLICY = StringComparePolicy.EXACT;
	private static final StringComparePolicy SUBSTRING_POLICY = StringComparePolicy.SUBSTRING;

	private MCLabel(Wrap<? extends Label> labelWrap) {
		this.control = labelWrap;
	}

	/**
	 * @param shell
	 *            The shell from where to start searching for this Label widget
	 * @param name
	 *            The name of the Label widget
	 * @param policy
	 *            A matching policy to be used when searching.
	 * @return A {@link MCLabel} that matches the name
	 */
	@SuppressWarnings("unchecked")
	public static MCLabel getByName(Wrap<? extends Shell> shell, String name, StringComparePolicy policy) {
		return new MCLabel(shell.as(Parent.class, Label.class).lookup(Label.class, new ByName<>(name, policy)).wrap());
	}

	/**
	 * @param shell
	 *            The shell from where to start searching for this Label widget
	 * @param name
	 *            The name of the Label widget (matched by using the default matching policy,
	 *            StringComparePolicy.EXACT)
	 * @return A {@link MCLabel} that matches the name
	 */
	public static MCLabel getByName(Wrap<? extends Shell> shell, String name) {
		return getByName(shell, name, EXACT_POLICY);
	}

	/**
	 * @param name
	 *            The name of the Label widget (matched by using the default matching policy,
	 *            StringComparePolicy.EXACT)
	 * @return A {@link MCLabel} that matches the name
	 */
	public static MCLabel getByName(String name) {
		return getByName(getShell(), name, EXACT_POLICY);
	}

	/**
	 * @param shell
	 *            The shell from where to start searching for this Label widget
	 * @param label
	 *            The current Label of the Label widget
	 * @param policy
	 *            A matching policy to be used when searching.
	 * @return A {@link MCLabel} that matches the Label
	 */
	@SuppressWarnings("unchecked")
	public static MCLabel getByLabel(Wrap<? extends Shell> shell, String label, StringComparePolicy policy) {
		return new MCLabel(
				shell.as(Parent.class, Label.class).lookup(Label.class, new ByTextLabelLookup<>(label, policy)).wrap());
	}

	/**
	 * @param shell
	 *            The shell from where to start searching for this Label widget
	 * @param label
	 *            The current Label of the Label widget (matched by using the default matching
	 *            policy, StringComparePolicy.EXACT)
	 * @return A {@link MCLabel} that matches the Label
	 */
	public static MCLabel getByLabel(Wrap<? extends Shell> shell, String label) {
		return getByLabel(shell, label, EXACT_POLICY);
	}

	/**
	 * Searches for a label in the main shell
	 *
	 * @param label
	 *            The substring of the label
	 * @return A {@link MCLabel} that matches the Label
	 */
	public static MCLabel getByLabelSubstring(String label) {
		return getByLabel(getShell(), label, SUBSTRING_POLICY);
	}

	/**
	 * @param label
	 *            The label
	 * @return A {@link MCLabel} that matches the Label
	 */
	public static MCLabel getByLabel(String label) {
		return getByLabel(getShell(), label, EXACT_POLICY);
	}

	/**
	 * @param shell
	 *            The shell from where to start searching for this Label widget
	 * @return A {@link MCLabel} that is visible and shows up first in the lookup
	 */
	public static MCLabel getFirstVisible(Wrap<? extends Shell> shell) {
		return getVisible(shell, 0);
	}

	/**
	 * @param shell
	 *            The shell from where to start searching for this Label widget
	 * @param index
	 *            The index of the visible Label widget to return
	 * @return A {@link MCLabel} that is visible and shows up in the lookup
	 */
	@SuppressWarnings("unchecked")
	public static MCLabel getVisible(Wrap<? extends Shell> shell, int index) {
		return new MCLabel(
				(Wrap<? extends Label>) getVisible(shell.as(Parent.class, Label.class).lookup(Label.class)).get(index));
	}

	/**
	 * Finds all visible Label items underneath the supplied shell
	 *
	 * @param shell
	 *            the shell where to search for Label items
	 * @return a {@link List} of {@link MCLabel} (possibly empty)
	 */
	@SuppressWarnings("unchecked")
	public static List<MCLabel> getAllVisible(Wrap<? extends Shell> shell) {
		List<Wrap<? extends Label>> allVisibleLabelWraps = getVisible(
				shell.as(Parent.class, Label.class).lookup(Label.class));
		List<MCLabel> allVisibleMcLabels = new ArrayList<>();
		for (Wrap<? extends Label> labelWrap : allVisibleLabelWraps) {
			allVisibleMcLabels.add(new MCLabel(labelWrap));
		}
		return allVisibleMcLabels;
	}

	/**
	 * Finds all visible Label items underneath the Mission Control main shell
	 *
	 * @return a {@link List} of {@link MCLabel} (possibly empty)
	 */
	public static List<MCLabel> getAllVisible() {
		return getAllVisible(getShell());
	}
}
