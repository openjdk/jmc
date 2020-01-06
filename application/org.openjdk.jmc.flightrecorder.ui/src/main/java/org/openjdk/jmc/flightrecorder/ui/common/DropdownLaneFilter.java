/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.flightrecorder.ui.common.LaneEditor.LaneDefinition;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.PatternFly.Palette;

public class DropdownLaneFilter extends Composite {
	private static final int EXTRA_SHELL_WIDTH = 100;
	private static final int SHELL_HEIGHT = 400;
	private Button dropdownButton;
	private GridLayout layout;
	private MCContextMenuManager[] mms;
	private Shell shell;
	private ThreadGraphLanes lanes;
	private TypeFilterBuilder filterEditor;

	public DropdownLaneFilter(Composite parent, ThreadGraphLanes lanes, MCContextMenuManager[] mms) {
		super(parent, SWT.NONE);
		this.lanes = lanes;
		this.mms = mms;
		this.setBackground(Palette.getThreadsPageBackgroundColor());
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		this.layout = layout;
		setLayout(layout);
		dropdownButton = new Button(this, SWT.TOGGLE);
		dropdownButton.setLayoutData(new GridData(GridData.FILL_BOTH));
		dropdownButton.setText(Messages.DropdownLaneFilter_THREAD_STATE_SELECTION);
		dropdownButton.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event e) {
				if (dropdownButton.getSelection()) {
					displayDropdown();
				}
			}
		});
	}

	/**
	 * Creates a new shell which is positioned below the dropdown button.
	 * This new shell creates the appearance of a dropdown component, and it's
	 * contents will be the TypeFilterBuilder as found in the Edit Thread Lanes
	 * dialog.
	 */
	private synchronized void displayDropdown() {
		Point p = dropdownButton.getParent().toDisplay(dropdownButton.getLocation());
		Point size = dropdownButton.getSize();
		Rectangle shellRect = new Rectangle(p.x, p.y + size.y, size.x, 0);

		shell = new Shell(DropdownLaneFilter.this.getShell(), SWT.BORDER);
		shell.addShellListener(new ShellAdapter() {

			public void shellDeactivated(ShellEvent e) {
				if (!isCursorOnTopOfButton()) {
					// If the shell is closed without clicking the button (i.e., not forcing
					// a toggle), then the button must be toggled programmatically.
					dropdownButton.setSelection(false);
				}
				disposeDropdown();
			}
		});

		shell.setLayout(this.layout);
		shell.setSize(shellRect.width + EXTRA_SHELL_WIDTH, SHELL_HEIGHT);
		shell.setLocation(shellRect.x, shellRect.y);

		filterEditor = new TypeFilterBuilder(shell, this::onTypeFilterChange);
		filterEditor.setInput(lanes.getTypeTree());
		filterEditor.selectTypes(lanes.getEnabledLanes());
		filterEditor.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		shell.open();
	}

	private synchronized void disposeDropdown() {
		if (shell != null && !shell.isDisposed()) {
			shell.close();
		}
	}

	/**
	 * Determine whether or not the mouse cursor is overlapping the dropdown button.
	 *
	 * An open dropdown shell should close when the user clicks the button. In Linux, the MouseListener
	 * on the button will fire. In Windows, the shell has priority and the MouseListener doesn't get activated.
	 *
	 * This function is to be used in the ShellAdapter to determine if the user closed the shell by
	 * trying to click the button, or by clicking away from the dropdown shell.
	 *
	 * @return true if the mouse cursor is on top of the button
	 */
	private boolean isCursorOnTopOfButton() {
		Point cursor = Display.getCurrent().getCursorLocation();
		Point buttonLoc = dropdownButton.toDisplay(1, 1);
		Rectangle buttonRect = new Rectangle(buttonLoc.x, buttonLoc.y, dropdownButton.getSize().x, dropdownButton.getSize().y);
		return buttonRect.contains(cursor);
	}

	/**
	 * Creates a new filter, Quick Filter, to be used by the dropdown lane filter.
	 * Sets the quick filter to be active, and update the context menu.
	 */
	private void onTypeFilterChange() {
		LaneDefinition quickLaneDef = new LaneDefinition(Messages.DropdownLaneFilter_QUICK_FILTER, true,
					ItemFilters.type(filterEditor.getCheckedTypeIds().collect(Collectors.toSet())), false);
		lanes.useDropdownFilter(quickLaneDef);
		lanes.updateContextMenus(mms, false);
	}
}