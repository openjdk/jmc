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
package org.openjdk.jmc.ui.idesupport;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import org.openjdk.jmc.ui.UIPlugin;

/**
 * The standard Mission Control perspective, used in the Eclipse IDE and in the stand alone RCP
 * application.
 */
public class StandardPerspective implements IPerspectiveFactory {
	private static final String PREF_KEY_APPLICATION_CLOSED_WITH_EDITOR = "closedWithEditor"; //$NON-NLS-1$

	public static final String ID = "org.openjdk.jmc.ui.idesupport.StandardPerspective"; //$NON-NLS-1$
	public static final String PROP_SHEET_ID = "org.eclipse.ui.views.PropertySheet"; //$NON-NLS-1$
	public static final String PROGRESS_ID = "org.eclipse.ui.views.ProgressView"; //$NON-NLS-1$
	public static final String MC_STANDARD_PERSPECTIVE_ID = "org.openjdk.jmc.rcp.application.perspective"; //$NON-NLS-1$

	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);

		layout.createPlaceholderFolder("org.openjdk.jmc.ui.idesupport.tallFolder", IPageLayout.LEFT, 0.22f, //$NON-NLS-1$
				layout.getEditorArea());
	}

	/*
	 * setClosedWithEditor and wasClosedWithEditorOpened is a workaround for an Eclipse bug. The bug
	 * happens when you close the application with and an editor open and a view(with a toolbar)
	 * overlaps another view, then the toolbar of the first view is drawn over the second view. The
	 * workaround consists of storing in the preference store that the editor were closed with an
	 * editor open and when the first view is created show it. /Erik
	 */
	public static void setClosedWithEditor(boolean closed) {
		UIPlugin.getDefault().getPreferenceStore().setValue(PREF_KEY_APPLICATION_CLOSED_WITH_EDITOR, closed);
	}

	/**
	 * Flag that indicates that the workbench was closed with an editor opened.
	 *
	 * @return true if the workbench was closed with an opened editor.
	 */
	public static boolean isClosedWithEditorOpened() {
		return UIPlugin.getDefault().getPreferenceStore().getBoolean(PREF_KEY_APPLICATION_CLOSED_WITH_EDITOR);
	}
}
