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

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.jemmy.control.Wrap;
import org.jemmy.interfaces.Parent;
import org.jemmy.interfaces.Selectable;
import org.jemmy.swt.lookup.ByItemToolTipLookup;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.fetchers.Fetcher;

/**
 * The Jemmy wrapper for ToolBars
 */
public class MCToolBar extends MCJemmyBase {

	private MCToolBar(Wrap<? extends ToolBar> toolBar) {
		this.control = toolBar;
	}

	/**
	 * Finds and returns a {@link MCToolBar} (in the main Mission Control Window) by means of one of the
	 * toolitem tooltips
	 *
	 * @param toolTip
	 *            the tooltip of any of the toolitems
	 * @return a {@link MCToolBar}
	 */
	public static MCToolBar getByToolTip(String toolTip) {
		return getByToolTip(getShell(), toolTip);
	}

	/**
	 * Finds and returns a {@link MCToolBar} by means of one of the toolitem tooltips
	 *
	 * @param shell
	 *            the shell from where to start searching for the widget
	 * @param toolTip
	 *            the tooltip of any of the toolitems
	 * @return a {@link MCToolBar}
	 */
	@SuppressWarnings("unchecked")
	static MCToolBar getByToolTip(Wrap<? extends Shell> shell, String toolTip) {
		return new MCToolBar(shell.as(Parent.class, ToolBar.class)
				.lookup(ToolBar.class, new ByItemToolTipLookup<ToolBar>(toolTip)).wrap());
	}

	/**
	 * Selects a toolitem in the toolbar
	 *
	 * @param toolTip
	 *            the tooltip of the toolitem
	 */
	public void selectToolItem(String toolTip) {
		if (!getToolItemSelected(toolTip)) {
			clickToolItem(toolTip);
		}
	}

	/**
	 * Un-selects a toolitem in the toolbar
	 *
	 * @param toolTip
	 *            the tooltip of the toolitem
	 */
	public void unselectToolItem(String toolTip) {
		if (getToolItemSelected(toolTip)) {
			clickToolItem(toolTip);
		}
	}

	/**
	 * Find out if the toolitem is selected
	 *
	 * @param toolTip
	 *            the tooltip of the toolitem to inspect
	 * @return {@code true} if selected, otherwise {@code false}
	 */
	public boolean getToolItemSelected(String toolTip) {
		return control.getProperty(Selectable.STATE_PROP_NAME).equals(toolTip);
	}

	/**
	 * Checks if toolitem in the toolbar with the tooltip provided is enabled or not.
	 *
	 * @param toolTip
	 *            the tooltip of the toolitem
	 * @return {@code true} if the toolItem is enabled, otherwise {@code false}
	 */
	public Boolean toolItemEnabled(String toolTip) {
		final ToolBar toolbar = (ToolBar) this.control.getControl();
		Fetcher<Boolean> fetcher = new Fetcher<Boolean>() {
			@Override
			public void run() {
				int count = toolbar.getItemCount();
				ToolItem item;
				Boolean enabled = null;
				for (int i = 0; i < count; i++) {
					item = toolbar.getItem(i);
					if (item.getToolTipText().equals(toolTip)) {
						enabled = item.isEnabled();
						break;
					}
				}
				if (enabled == null) {
					System.out.println("Not able to find the toolItem with toolTip " + toolTip);
					enabled = false;
				}
				setOutput(enabled);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Clicks a toolitem in the toolbar
	 *
	 * @param toolTip
	 *            the tooltip of the toolitem
	 */
	@SuppressWarnings("unchecked")
	public void clickToolItem(String toolTip) {
		control.as(Selectable.class).selector().select(toolTip);
	}

}
