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
package org.openjdk.jmc.ui.formpage.commands.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.Widget;

import org.openjdk.jmc.commands.Statement;

/**
 * Command that can click button, table columns, tab item etc.
 */
public final class Click extends UICommand {
	@Override
	protected void execute(Object object, Statement s) {
		if (object instanceof Button) {
			clickButton((Button) object);
		}
		if (object instanceof TableColumn) {
			clickWidget((TableColumn) object);
		}
		if (object instanceof TreeColumn) {
			clickWidget((TreeColumn) object);
		}
		if (object instanceof TabItem) {
			clickTabItem((TabItem) object);
		}
		if (object instanceof CTabItem) {
			clickCTabItem((CTabItem) object);
		}
		if (object instanceof Table) {
			clickTable((Table) object);
		}
		if (object instanceof Tree) {
			clickTree((Tree) object);
		}
	}

	private void clickTree(Tree tree) {
		int count = tree.getItemCount();
		if (count > 0) {
			tree.select(tree.getItem(0));
		}
	}

	private void clickTable(Table table) {
		int count = table.getItemCount();
		if (count > 0) {
			table.setSelection(0);
		}
	}

	private void clickTabItem(TabItem tabItem) {
		tabItem.getParent().setSelection(tabItem);
	}

	private void clickCTabItem(CTabItem tabItem) {
		tabItem.getParent().setSelection(tabItem);
	}

	private void clickWidget(Widget widget) {
		Event event = new Event();
		event.button = SWT.BUTTON1;
		event.widget = widget;
		event.doit = true;
		event.type = SWT.Selection;
		event.display = widget.getDisplay();
		widget.notifyListeners(SWT.Selection, event);
	}

	private void clickButton(Button button) {
		int style = button.getStyle();
		if ((style & (SWT.CHECK | SWT.RADIO | SWT.TOGGLE)) == 0) {
			pushButton(button);
		} else {
			button.setSelection(!button.getSelection());
		}
	}

	private void pushButton(Button button) {
		Event event = new Event();
		event.button = SWT.BUTTON1;
		event.widget = button;
		event.doit = true;
		event.type = SWT.Selection;
		event.display = button.getDisplay();
		button.notifyListeners(SWT.Selection, event);
	}
}
