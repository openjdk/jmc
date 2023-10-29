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

import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.swt.widgets.Shell;
import org.jemmy.control.Wrap;
import org.jemmy.interfaces.Parent;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;

/**
 * The Jemmy base wrapper for progress indicators
 */
public class MCProgressIndicator extends MCJemmyBase {

	private MCProgressIndicator(Wrap<? extends ProgressIndicator> pIndicator) {
		this.control = pIndicator;
	}

	/**
	 * Finds all visible {@link ProgressIndicator} objects in the supplied {@link Wrap} of
	 * {@link Shell} and returns a {@link List} of these
	 *
	 * @param shell
	 *            the {@link Wrap} where to start the search for the ProgressIndicators
	 * @return a {@link List} of {@link MCProgressIndicator} (possibly empty)
	 */
	@SuppressWarnings("unchecked")
	public static List<MCProgressIndicator> getVisible(Wrap<? extends Shell> shell) {
		List<Wrap<? extends ProgressIndicator>> allVisibleProgressIndicatorWraps = getVisible(
				shell.as(Parent.class, ProgressIndicator.class).lookup(ProgressIndicator.class), false, false);
		List<MCProgressIndicator> allVisibleMcProgressIndicators = new ArrayList<>();
		for (Wrap<? extends ProgressIndicator> progressIndicatorWrap : allVisibleProgressIndicatorWraps) {
			allVisibleMcProgressIndicators.add(new MCProgressIndicator(progressIndicatorWrap));
		}
		return allVisibleMcProgressIndicators;
	}

	/**
	 * Finds all visible {@link ProgressIndicator} objects in the supplied {@link Wrap} of
	 * {@link Shell} and returns a {@link List} of these
	 *
	 * @param shell
	 *            the {@link Wrap} where to start the search for the ProgressIndicators
	 * @param waitForIdle
	 *            {@code true} if supposed to wait for the UI to be idle before ending the lookup
	 * @return a {@link List} of {@link MCProgressIndicator} (possibly empty)
	 */
	@SuppressWarnings("unchecked")
	public static List<MCProgressIndicator> getVisible(Wrap<? extends Shell> shell, boolean waitForIdle) {
		List<Wrap<? extends ProgressIndicator>> allVisibleProgressIndicatorWraps = getVisible(
				shell.as(Parent.class, ProgressIndicator.class).lookup(ProgressIndicator.class), waitForIdle, false);
		List<MCProgressIndicator> allVisibleMcProgressIndicators = new ArrayList<>();
		for (Wrap<? extends ProgressIndicator> progressIndicatorWrap : allVisibleProgressIndicatorWraps) {
			allVisibleMcProgressIndicators.add(new MCProgressIndicator(progressIndicatorWrap));
		}
		return allVisibleMcProgressIndicators;
	}

}
