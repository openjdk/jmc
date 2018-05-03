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
package org.openjdk.jmc.ui.misc;

import java.util.Observable;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * <p>
 * MCSectionPart contains common functionality you might want to have in your SectionPart. This is
 * also the base classed used for {@link SectionPart}s in Mission Control.
 * </p>
 * <h2>Design</h2> This section describes how a {@link SectionPart} works and how it's life cycle
 * looks.
 * <h3>Initialization</h3>
 * <p>
 * The initialization a SectionPart process can be divided into these phases:
 * </p>
 * <ul>
 * <p>
 * <b>Object construction</b><br>
 * This is when the constructor of the SectionPart is called. During this phase minimal
 * initialization should be done. Only, e.g., light weight and immutable member variables/constants
 * should be constructed. No widgets should be created and no models should be initialized. The
 * rationale for this is that we want to allow for lazy initialization and avoid initializations
 * bugs when we're constructor chaining. It's also much easier to debug if all the widgets are
 * created in one place. See next phase.
 * </p>
 * <p>
 * <b>Form initialization</b><br>
 * This is when {@link SectionPart#initialize(IManagedForm)} is called. During this phase
 * SWT-widgets should be created. At this moment we have the input from the {@link ManagedForm}
 * available so it's possible to access the model, but because model initialization could be
 * expensive it should be avoided, if possible. During this phase the {@link SectionSite} for the
 * part is constructed and should be available for classes that want to subclass. Note, you need to
 * call super.initialize(IManangedForm) in {@link SectionPart#initialize(IManagedForm)} method if
 * you override.
 * </p>
 * <p>
 * <b>Model initialization</b><br>
 * This happens when {@link MCSectionPart#initializePart()} is called and it typcally occurs the
 * first time a user flips to a page containing the {@link SectionPart}. During this phase model
 * initialization should be done. In this phase you should set {@link IContentProvider}s for viewers
 * and fill widgets with data. If necessary in a job thread, posting widget updates to the
 * ui-thread.
 * </p>
 * </ul>
 * <h2>Usage</h2>
 * <p>
 * SectionParts has a mechanism for keeping the user interface and the model in sync. Typically you
 * get an update from your model listener, e.g {@link Observable}, saying that the model has
 * changed. At this time you should not update your view but instead call
 * {@link SectionPart#markStale()} and return. The next time the {@link SectionPart} becomes visible
 * {@link MCSectionPart#refreshPart()} is called if the part is stale. At this time the model data
 * is fetched into the widgets, If you modify your model in a way that requires data to be saved you
 * should call {@link SectionPart#markDirty()} and use the {@link SectionPart#commit(boolean)}
 * function.
 * </p>
 * <p>
 * A {@link SectionPart} can choose to implement {@link IPartSelectionListener} if the part wants to
 * be informed of selection changes that occur on the {@link IManagedForm}. There is no need to add
 * the listener. Just implement the interface. Typically used when you have master/detail dependency
 * between parts.
 * </p>
 * <h3>End of life</h3>
 * <p>
 * When the user closes the editor the {@link IManagedForm} and the editor will check if the
 * {@link SectionPart} is dirty and if so {@link SectionPart#commit(boolean)} will be called. If
 * everything works out OK the dispose method ( {@link SectionPart#dispose()}) will be called. In
 * the dispose method resources should be cleaned up. Call super.dispose() the last thing in dispose
 * method. This ensures that resource are cleaned up the in reversed order that they we're
 * initialized.
 * </p>
 * </ul>
 */
public abstract class MCSectionPart extends SectionPart {

	/**
	 * Default section style having a title.
	 */
	public static final int DEFAULT_TITLE_STYLE = ExpandableComposite.TITLE_BAR;

	/**
	 * Default section style having a title and a description.
	 */
	public static final int DEFAULT_TITLE_DESCRIPTION_STYLE = DEFAULT_TITLE_STYLE | Section.DESCRIPTION;

	/**
	 * Default section style for a twistie (expandable/shrinkable) section having a title.
	 */
	public static final int DEFAULT_TWISTIE_STYLE = DEFAULT_TITLE_STYLE | ExpandableComposite.TWISTIE
			| ExpandableComposite.EXPANDED;

	private final FormToolkit toolkit;
	private final MCToolBarManager toolBarManager;

	/**
	 * Constructor for a {@link MCSectionPart}
	 *
	 * @param parent
	 *            the parent composite
	 * @param toolkit
	 *            the toolkit to create the SWT section with
	 * @param style
	 *            the SWT style
	 */
	public MCSectionPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		this.toolkit = toolkit;

		toolBarManager = new MCToolBarManager(getSection());
		getSection().setTextClient(toolBarManager.getToolBar());
	}

	public MCSectionPart(Composite parent, FormToolkit toolkit, String title) {
		this(parent, toolkit, MCSectionPart.DEFAULT_TWISTIE_STYLE);
		getSection().setText(title);
	}

	protected Composite createSectionBody(Layout layout) {
		Composite body = toolkit.createComposite(getSection());
		body.setLayout(layout);
		getSection().setClient(body);
		return body;
	}

	public MCToolBarManager getMCToolBarManager() {
		return toolBarManager;
	}

	@Override
	public void setFocus() {
		if (getSection() != null && !getSection().isDisposed()) {
			getSection().setFocus();
		}
	}

	@Override
	public void dispose() {
		toolBarManager.dispose();
		super.dispose();
	}
}
