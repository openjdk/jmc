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
package org.openjdk.jmc.ui.handlers;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Control;

import org.openjdk.jmc.ui.common.util.Environment;

/**
 * Context menu filled with standard group ids.
 */
public class MCContextMenuManager extends MenuManager {
	/*
	 * As defined in org.eclipse.ui.navigator.ICommonMenuConstants and extension point
	 * org.eclipse.ui.navigator.viewer. We have no dependency to bundle org.eclipse.ui.navigator, so
	 * we need to define them again here.
	 */
	public static final String GROUP_TOP = "group.top"; //$NON-NLS-1$
	public static final String GROUP_VIEWER_SETUP = "group.viewerSetup"; //$NON-NLS-1$
	public static final String GROUP_NEW = "group.new"; //$NON-NLS-1$
	public static final String GROUP_OPEN = "group.open"; //$NON-NLS-1$
	public static final String GROUP_OPEN_WITH = "group.openWith"; //$NON-NLS-1$
	public static final String GROUP_EDIT = "group.edit"; //$NON-NLS-1$
	public static final String GROUP_ADDITIONS = "additions"; //$NON-NLS-1$
	public static final String GROUP_PROPETIES = "group.properties"; //$NON-NLS-1$

	public static MCContextMenuManager create(Control control) {
		return create(control, "MCContextMenuManager"); //$NON-NLS-1$
	}

	public static MCContextMenuManager create(Control control, String id) {
		final MCContextMenuManager mm = new MCContextMenuManager(id);
		control.setMenu(mm.createContextMenu(control));
		control.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				mm.dispose();
			}
		});
		return mm;
	}

	public MCContextMenuManager(String id) {
		this(id, id);
	}

	public MCContextMenuManager(String name, String id) {
		super(name, id);
		initialize(id);
	}

	public void addAll(IContributionItem ... items) {
		for (IContributionItem i : items) {
			add(i);
		}
	}

	protected void initialize(final String id) {
		class ExtensionURIAction extends Action {
			public ExtensionURIAction() {
				super("[popup:" + id + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		if (Environment.isDebug()) {
			add(new ExtensionURIAction());
		}
		addSeparator(GROUP_TOP);
		addSeparator(GROUP_VIEWER_SETUP);
		addSeparator(GROUP_NEW);
		addSeparator(GROUP_OPEN);
		addSeparator(GROUP_EDIT);
		addSeparator(GROUP_ADDITIONS);
		addSeparator(GROUP_PROPETIES);
	}

	static class DebugSeparatorAction extends Action {
		DebugSeparatorAction(String text) {
			super("[" + text + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void addSeparator(String separatorId) {
		if (Environment.isDebug()) {
			add(new Separator("dummy")); //$NON-NLS-1$
			add(new DebugSeparatorAction(separatorId));
			add(new GroupMarker(separatorId));
		} else {
			add(new Separator(separatorId));
		}
	}
}
