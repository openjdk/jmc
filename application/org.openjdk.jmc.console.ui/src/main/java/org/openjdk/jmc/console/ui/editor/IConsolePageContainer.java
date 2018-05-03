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
package org.openjdk.jmc.console.ui.editor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;

/**
 * Console pages are contained in form pages. This interface allows for accessing some parts of the
 * surrounding form page.
 * <p>
 * See {@link org.openjdk.jmc.console.ui.editor} for an example of usage.
 */
public interface IConsolePageContainer {

	/**
	 * Place holder for the "first" action items on a Console page tool bar. All other items are
	 * added after this.
	 */
	static final String TB_FIRST_GROUP = "first"; //$NON-NLS-1$
	/**
	 * Place holder for help item on a Console page tool bar. All others items are added before
	 * this.
	 */
	static final String TB_HELP_GROUP = "help"; //$NON-NLS-1$

	/**
	 * Returns the Console page editor.
	 *
	 * @return parent Console page editor instance
	 */
	FormEditor getEditor();

	/**
	 * Returns the managed form owned by this page.
	 *
	 * @return the managed form
	 */
	IManagedForm getManagedForm();

	/**
	 * Returns the container that occupies the body of the page (the area below the title). Use this
	 * container as a parent for the controls that should be on the page. No layout manager has been
	 * set on the body.
	 *
	 * @return the body of the page
	 */
	Composite getBody();

	/**
	 * Returns the default configuration for the Console page.
	 *
	 * @return the default configuration
	 */
	public IMemento getDefaultConfig();

	/**
	 * Loads the persisted configuration for the Console page.
	 *
	 * @return the persisted configuration
	 */
	public IMemento loadConfig();

	/**
	 * Presents an error message together with an error icon a the top of the page.
	 *
	 * @param message
	 *            the message to show
	 */
	public void presentError(String message);
}
