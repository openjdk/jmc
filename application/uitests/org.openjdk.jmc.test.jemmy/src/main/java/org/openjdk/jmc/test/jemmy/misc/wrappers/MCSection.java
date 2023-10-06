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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.Section;
import org.jemmy.control.Wrap;
import org.jemmy.interfaces.Parent;
import org.jemmy.resources.StringComparePolicy;
import org.jemmy.swt.CompositeWrap;
import org.jemmy.swt.lookup.ByName;
import org.jemmy.swt.lookup.ByTextControlLookup;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.fetchers.Fetcher;

/**
 * The Jemmy base wrapper for sections
 */
public class MCSection extends MCJemmyBase {
	private static final StringComparePolicy EXACT_POLICY = StringComparePolicy.EXACT;
	private static final StringComparePolicy SUBSTRING_POLICY = StringComparePolicy.SUBSTRING;

	private MCSection(Wrap<? extends Section> sectionWrap) {
		this.control = sectionWrap;
	}

	/**
	 * @param shell
	 *            the shell from where to start searching for this Section widget
	 * @param name
	 *            the name of the Section widget
	 * @param policy
	 *            a {@link StringComparePolicy} to be used when searching.
	 * @return a {@link MCSection} that matches the name
	 */
	@SuppressWarnings("unchecked")
	public static MCSection getByName(Wrap<? extends Shell> shell, String name, StringComparePolicy policy) {
		return new MCSection(
				shell.as(Parent.class, Section.class).lookup(Section.class, new ByName<>(name, policy)).wrap());
	}

	/**
	 * @param shell
	 *            the shell from where to start searching for this Section widget
	 * @param name
	 *            the name of the Section widget (matched by using the default matching policy,
	 *            {@link StringComparePolicy.EXACT})
	 * @return a {@link MCSection} that matches the name
	 */
	public static MCSection getByName(Wrap<? extends Shell> shell, String name) {
		return getByName(shell, name, EXACT_POLICY);
	}

	/**
	 * @param name
	 *            the name of the Section widget (matched by using the default matching policy,
	 *            {@link StringComparePolicy.EXACT})
	 * @return a {@link MCSection} that matches the name
	 */
	public static MCSection getByName(String name) {
		return getByName(getShell(), name, EXACT_POLICY);
	}

	/**
	 * @param shell
	 *            the shell from where to start searching for this Section widget
	 * @param label
	 *            the current label of the Section widget
	 * @param policy
	 *            a {@link StringComparePolicy} to be used when searching.
	 * @return a {@link MCSection} that matches the name
	 */
	@SuppressWarnings("unchecked")
	public static MCSection getByLabel(Wrap<? extends Shell> shell, String label, StringComparePolicy policy) {
		return new MCSection(shell.as(Parent.class, Section.class)
				.lookup(Section.class, new ByTextControlLookup<>(label, policy)).wrap());
	}

	/**
	 * @param shell
	 *            the shell from where to start searching for this Section widget
	 * @param label
	 *            the current Label of the Section widget (matched by using the default matching
	 *            policy, {@link StringComparePolicy.EXACT})
	 * @return a {@link MCSection} that matches the name
	 */
	public static MCSection getByLabel(Wrap<? extends Shell> shell, String label) {
		return getByLabel(shell, label, EXACT_POLICY);
	}

	/**
	 * Searches for a Section in the main shell
	 *
	 * @param label
	 *            the substring of the label
	 * @return a {@link MCSection} that matches the name
	 */
	public static MCSection getByLabelSubstring(String label) {
		return getByLabel(getShell(), label, SUBSTRING_POLICY);
	}

	/**
	 * @param label
	 *            the label
	 * @return a {@link MCSection} that matches the name
	 */
	public static MCSection getByLabel(String label) {
		return getByLabel(getShell(), label, EXACT_POLICY);
	}

	/**
	 * @param shell
	 *            the shell from where to start searching for this Section widget
	 * @return a {@link MCSection} that is visible and shows up first in the lookup
	 */
	public static MCSection getFirstVisible(Wrap<? extends Shell> shell) {
		return getVisible(shell, 0);
	}

	/**
	 * @param shell
	 *            the shell from where to start searching for this Section widget
	 * @param index
	 *            the index of the visible Section widget to return
	 * @return a {@link MCSection} that is visible and shows up first in the lookup
	 */
	@SuppressWarnings("unchecked")
	public static MCSection getVisible(Wrap<? extends Shell> shell, int index) {
		return new MCSection(
				(Wrap<? extends Section>) getVisible(shell.as(Parent.class, Section.class).lookup(Section.class))
						.get(index));
	}

	/**
	 * Finds all visible Section items underneath the supplied shell
	 *
	 * @param shell
	 *            the shell where to search for Section items
	 * @return a {@link List} of {@link MCSection} (possibly empty)
	 */
	@SuppressWarnings("unchecked")
	public static List<MCSection> getVisible(Wrap<? extends Shell> shell) {
		List<Wrap<? extends Section>> allVisibleSectionWraps = getVisible(
				shell.as(Parent.class, Section.class).lookup(Section.class));
		List<MCSection> allVisibleMcSections = new ArrayList<>();
		for (Wrap<? extends Section> sectionWrap : allVisibleSectionWraps) {
			allVisibleMcSections.add(new MCSection(sectionWrap));
		}
		return allVisibleMcSections;
	}

	/**
	 * Expands the section
	 */
	public void expand() {
		setSectionExpansionState(true);
	}

	/**
	 * Collapses the section
	 */
	public void collapse() {
		setSectionExpansionState(false);
	}

	/**
	 * Finds and returns a (the first) hyperlink by means of the tooltip
	 *
	 * @param toolTip
	 *            the tooltip of the hyperlink
	 * @return a {@link MCHyperlink}
	 */
	public MCHyperlink getHyperlink(String toolTip) {
		return getHyperlink(toolTip, 0);
	}

	/**
	 * Finds and returns a hyperlink by means of the tooltip
	 *
	 * @param toolTip
	 *            the tooltip of the hyperlink
	 * @param index
	 *            the index of the hyperlink (if there are more than one with the same tooltip)
	 * @return a {@link MCHyperlink}
	 */
	public MCHyperlink getHyperlink(String toolTip, int index) {
		return MCHyperlink.getByTooltip(getSectionShell(), toolTip, index);
	}

	/**
	 * @return The shell of the Section
	 */
	@SuppressWarnings("unchecked")
	protected Wrap<? extends Composite> getSectionShell() {
		return control.as(CompositeWrap.class);
	}

	@SuppressWarnings("unchecked")
	private void setSectionExpansionState(boolean expanded) {
		if (expanded != isExpanded()) {
			control.as(Parent.class, Label.class).lookup(Label.class, new ByTextControlLookup<Label>(getText())).wrap()
					.mouse().click();
		}
	}

	private boolean isExpanded() {
		Fetcher<Boolean> fetcher = new Fetcher<Boolean>() {
			@Override
			public void run() {
				Section ec = Section.class.cast(control.getControl());
				setOutput(ec.isExpanded());
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

}
