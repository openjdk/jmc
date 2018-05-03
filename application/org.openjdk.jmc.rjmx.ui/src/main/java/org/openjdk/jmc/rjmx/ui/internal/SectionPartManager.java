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
package org.openjdk.jmc.rjmx.ui.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;
import org.openjdk.jmc.ui.misc.MCToolBarManager;

/**
 * Manager that controls the {@link SectionPart} that is on {@link IManagedForm}. Typically used for
 * creating chart in a tab that lives in other plug-ins.
 */
public class SectionPartManager {
	final private Composite m_container;
	final private IManagedForm m_managedForm;

	/**
	 * Creates a {@link SectionPartManager}
	 *
	 * @param managedForm
	 *            the {@link IManagedForm} that should back the this manager
	 * @param guiBuilder
	 *            the GUI-builder to use when creating parts with this {@link SectionPartManager}
	 */
	public SectionPartManager(IManagedForm managedForm) {
		m_managedForm = managedForm;
		m_container = managedForm.getForm().getBody();
		m_container.setLayout(new GridLayout());
	}

	public void add(MCSectionPart part, final boolean grab, boolean enableRemove) throws IllegalArgumentException {
		m_managedForm.addPart(part);
		if (grab) {
			MCLayoutFactory.addGrabOnExpandLayoutData(part.getSection());
		} else {
			part.getSection().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		if (enableRemove) {
			part.getMCToolBarManager().add(new RemoveAction(this, part), MCToolBarManager.ALIGN_RIGHT);
		}
		// FIXME: force refresh of parts if some tab is reverted
		part.refresh();
		m_managedForm.reflow(true);
	}

	/**
	 * Returns a list of {@link SectionPart} that are managed.
	 *
	 * @return
	 */
	public List<IFormPart> getParts() {
		ArrayList<IFormPart> list = new ArrayList<>();
		list.addAll(Arrays.asList(m_managedForm.getParts()));
		return Collections.unmodifiableList(list);
	}

	/**
	 * Destroy all parts managed by this manager.
	 */
	public void destroyAllParts() {
		while (m_managedForm.getParts().length > 0) {
			destroyPart((MCSectionPart) m_managedForm.getParts()[0]);
		}
		m_managedForm.getForm().setFocus();
	}

	/**
	 * Destroy the part.
	 *
	 * @param part
	 *            the part to destroy
	 */
	public void destroyPart(MCSectionPart part) {
		m_managedForm.removePart(part);
		part.dispose();
		part.getSection().dispose();
		m_managedForm.reflow(true);
	}

	public Composite getContainer() {
		return m_container;
	}

	public FormToolkit getFormToolkit() {
		return m_managedForm.getToolkit();
	}

	/**
	 * Creates a unique title for a {@link SectionPart}.
	 *
	 * @param title
	 *            the prefix for the title. E.g. "My Section"
	 * @return a title, e.g. "My Section 1",. "My Section 2"...
	 */
	public String createUniqueSectionPartTitle(String title) {
		int n = 1;
		title += ' ';
		for (IFormPart part : getParts()) {
			// FIXME: Refactor to make this type safe
			String s = ((SectionPart) part).getSection().getText();
			if (s.startsWith(title)) {
				try {
					int chartNum = Integer.parseInt(s.substring(title.length()));
					if (chartNum >= n) {
						n = chartNum + 1;
					}
				} catch (NumberFormatException ignore) {
					// Just keep looking for charts that match the pattern
				}
			}
		}

		return title + Integer.toString(n);
	}

	/**
	 * Checks whether a section part with given title already exists.
	 *
	 * @param title
	 *            the title to check for
	 * @return <tt>true</tt> if section part with given title exists, <tt>false</tt> otherwise
	 */
	public boolean hasSectionPartTitle(String title) {
		for (IFormPart part : getParts()) {
			String name = ((SectionPart) part).getSection().getText();
			if (name.equals(title)) {
				return true;
			}
		}
		return false;
	}
}
