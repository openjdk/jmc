/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Red Hat Inc. All rights reserved.
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

import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jemmy.control.Wrap;
import org.jemmy.interfaces.Parent;
import org.jemmy.resources.StringComparePolicy;
import org.jemmy.swt.lookup.ByName;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.fetchers.Fetcher;

public class MCScrolledComposite extends MCJemmyBase {
	private static final StringComparePolicy EXACT_POLICY = StringComparePolicy.EXACT;

	private MCScrolledComposite(Wrap<? extends ScrolledComposite> sc) {
		this.control = sc;
	}

	/**
	 * Finds and returns a {@link MCScrolledComposite}
	 * 
	 * @param shell
	 *            the shell from where to start searching for this widget
	 * @param name
	 *            the name of the widget (matched by using the default matching policy,
	 *            {@link StringComparePolicy.EXACT})
	 * @return a {@link MCScrolledComposite} that matches the name
	 */
	public static MCScrolledComposite getByName(String name) {
		return getByName(getShell(), name, EXACT_POLICY);

	}

	/**
	 * Finds and returns a {@link MCScrolledComposite}
	 * 
	 * @param shell
	 *            the shell from where to start searching for this widget
	 * @param name
	 *            the name of the widget
	 * @param policy
	 *            A {@link StringComparePolicy} for matching the name
	 * @return a {@link MCScrolledComposite} that matches the name
	 */
	@SuppressWarnings("unchecked")
	public static MCScrolledComposite getByName(Wrap<? extends Shell> shell, String name, StringComparePolicy policy) {
		return new MCScrolledComposite(shell.as(Parent.class, ScrolledComposite.class)
				.lookup(ScrolledComposite.class, new ByName<>(name, policy)).wrap());
	}

	/**
	 * Returns the visibility of the horizontal scrollbar
	 * 
	 * @return true if the horizontal scrollbar is visible
	 */
	public boolean isHorizontalBarVisible() {
		final ScrolledComposite sc = (ScrolledComposite) control.getControl();
		Fetcher<Boolean> fetcher = new Fetcher<Boolean>() {
			@Override
			public void run() {
				setOutput(sc.getHorizontalBar().isVisible());
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}
}
