/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Red Hat Inc. All rights reserved.
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

import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jemmy.control.Wrap;
import org.jemmy.interfaces.Parent;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.fetchers.Fetcher;

/**
 * The Jemmy wrapper for the Mission Control SashForm.
 */
public class MCSashForm extends MCJemmyBase {

	private MCSashForm(Wrap<? extends SashForm> formWrap) {
		this.control = formWrap;
	}

	/**
	 * Returns all visible {@link MCSashForm} objects underneath the supplied shell
	 *
	 * @param shell
	 *            the shell from where to start the search for the SashForm object
	 * @return a {@link List} of {@link MCSashForm} objects
	 */
	@SuppressWarnings("unchecked")
	public static List<MCSashForm> getAll(Wrap<? extends Shell> shell) {
		List<Wrap<? extends SashForm>> list = getVisible(shell.as(Parent.class, SashForm.class).lookup(SashForm.class));
		List<MCSashForm> sashForms = new ArrayList<MCSashForm>();
		for (int i = 0; i < list.size(); i++) {
			sashForms.add(new MCSashForm(list.get(i)));
		}
		return sashForms;
	}

	/**
	 * Returns the first visible {@link MCSashForm} object underneath the supplied shell
	 *
	 * @param shell
	 *            the shell from where to start the search for the SashForm object
	 * @return a {@link MCSashForm} object
	 */
	public static MCSashForm getFirst(Wrap<? extends Shell> shell) {
		return getAll(shell).get(0);
	}

	/**
	 * Returns the first visible {@link MCSashForm} object underneath the Mission Control main shell
	 *
	 * @return a {@link MCSashForm} object
	 */
	public static MCSashForm getMCSashForm() {
		return getFirst(getShell());
	}

	/**
	 * Returns the current weights of the SashForm
	 *
	 * @return the Sash Weights
	 */
	public int[] getWeights() {
		Fetcher<int[]> fetcher = new Fetcher<int[]>() {
			@Override
			public void run() {
				setOutput(((SashForm) control.getControl()).getWeights());
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}
}
