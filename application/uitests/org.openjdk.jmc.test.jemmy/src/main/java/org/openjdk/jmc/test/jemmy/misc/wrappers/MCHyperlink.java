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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.jemmy.control.Wrap;
import org.jemmy.interfaces.Parent;
import org.jemmy.swt.lookup.ByToolTipControl;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;

/**
 * Wrapper class for the Jemmy wrapper of SWT hyperlinks. This will mostly be used for the custom
 * toolbars in Mission Control (i.e. the toolbars above the tables in greychart et c.)
 */
public class MCHyperlink extends MCJemmyBase {

	private MCHyperlink(Wrap<? extends Hyperlink> hyperlink) {
		this.control = hyperlink;
	}

	/**
	 * Finds and returns a hyperlink in the default shell by means of one of the hyperlink tooltips
	 *
	 * @param toolTip
	 *            the tooltip of any of the hyperlinks
	 * @return a {@link MCHyperlink}
	 */
	public static MCHyperlink getByTooltip(String toolTip) {
		return getByTooltip(getShell(), toolTip);
	}

	/**
	 * Finds and returns a hyperlink by means of one of the hyperlink tooltips
	 *
	 * @param shell
	 *            the shell where to look for the hyperlink
	 * @param toolTip
	 *            the tooltip of any of the hyperlinks
	 * @return a {@link MCHyperlink}
	 */
	public static MCHyperlink getByTooltip(Wrap<? extends Shell> shell, String toolTip) {
		return getByTooltip(shell, toolTip, 0);
	}

	/**
	 * @param section
	 *            the {@link MCSection} to use as the starting point of the lookup
	 * @param toolTip
	 *            the tooltip of any of the hyperlinks
	 * @return a {@link MCHyperlink}
	 */
	public static MCHyperlink getByTooltip(MCSection section, String toolTip) {
		return getByTooltip(section.getSectionShell(), toolTip, 0);
	}

	/**
	 * Finds and returns a hyperlink by means of one of the hyperlink tooltips
	 *
	 * @param composite
	 *            the shell where to look for the hyperlink
	 * @param toolTip
	 *            the tooltip of any of the hyperlinks
	 * @param index
	 *            the index of the hyperlink (if there are more than one with the same tooltip)
	 * @return a {@link MCHyperlink}
	 */
	@SuppressWarnings("unchecked")
	public static MCHyperlink getByTooltip(Wrap<? extends Composite> composite, String toolTip, int index) {
		return new MCHyperlink(composite.as(Parent.class, Hyperlink.class)
				.lookup(Hyperlink.class, new ByToolTipControl<Hyperlink>(toolTip)).wrap(index));
	}
}
