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

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.Form;
import org.jemmy.control.Wrap;
import org.jemmy.interfaces.Parent;
import org.jemmy.resources.StringComparePolicy;
import org.jemmy.swt.lookup.ByTextControlLookup;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.fetchers.Fetcher;

/**
 * The Jemmy base wrapper for Forms
 */
public class MCForm extends MCJemmyBase {
	private static final StringComparePolicy EXACT_POLICY = StringComparePolicy.EXACT;
	private static final StringComparePolicy SUBSTRING_POLICY = StringComparePolicy.SUBSTRING;

	private MCForm(Wrap<? extends Form> formWrap) {
		this.control = formWrap;
	}

	/**
	 * Finds a visible form with a matching title text
	 *
	 * @param title
	 *            the title text to match (exactly)
	 * @return the first matching {@link MCForm}
	 */
	public static MCForm getByTitle(String title) {
		return getByTitle(title, true);
	}

	/**
	 * Finds a visible form with a matching title text
	 *
	 * @param title
	 *            the title text to match
	 * @param exactMatching
	 *            {@code true} if exact matching is desired. Otherwise {@code false}
	 * @return the first matching {@link MCForm}
	 */
	public static MCForm getByTitle(String title, boolean exactMatching) {
		StringComparePolicy policy = exactMatching ? EXACT_POLICY : SUBSTRING_POLICY;
		return getByTitle(title, policy);
	}

	@SuppressWarnings("unchecked")
	private static MCForm getByTitle(String title, StringComparePolicy policy) {
		return new MCForm((Wrap<? extends Form>) getVisible(
				getShell().as(Parent.class, Form.class).lookup(Form.class, new ByTextControlLookup<>(title, policy)),
				false, true).get(0));
	}

	/**
	 * @return the title text of this form
	 */
	public String getTitle() {
		return String.class.cast(control.getProperty(Wrap.TEXT_PROP_NAME));
	}

	protected Image getImage() {
		Fetcher<Image> fetcher = new Fetcher<Image>() {
			@Override
			public void run() {
				Form form = Form.class.cast(control.getControl());
				Image result = (form != null) ? form.getImage() : null;
				setOutput(result);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Saves the form's image to the path provided
	 * 
	 * @param path
	 *            the desired path of the image file
	 */
	public void saveImageToFile(String path) {
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] {getImage().getImageData()};
		File parent = new File(path).getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		loader.save(path, SWT.IMAGE_PNG);
	}

	/**
	 * Compares the title region text and image to those of the supplied tab folder
	 *
	 * @param tabFolder
	 *            the tab folder for which to compare the selected tab with this form
	 * @return {@code true} if both the title text and image equals those of the currently selected tab
	 */
	public boolean titleRegionMatches(MCTabFolder tabFolder) {
		return getTitle().equals(tabFolder.getState()) && getImage().equals(tabFolder.getSelectedTabImage());
	}
}
