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

import java.io.PrintStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.Hyperlink;

import org.openjdk.jmc.commands.IExecute;
import org.openjdk.jmc.commands.Statement;
import org.openjdk.jmc.ui.dial.DialViewer;

public class PrintUI implements IExecute {
	private static final int MAX_TABLE_ITEMS = 50;

	@Override
	public boolean execute(Statement statement, final PrintStream out) {
		Object o = ContextLookup.getContext(out);
		if (o != null) {
			Traverser.visit(o, new IVisitor() {
				@Override
				public void visit(Object node, String text, String name) {
					if (node instanceof Table) {
						printTable(out, (Table) node);
					}

					if (node instanceof Tree) {
						printTree(out, (Tree) node);
					}

					if (text != null) {
						if (node instanceof Hyperlink || node instanceof Label || node instanceof CLabel
								|| node instanceof FormText || node instanceof DialViewer) {
							printValue(out, "Label", text); //$NON-NLS-1$
						}
						if (node instanceof Text || node instanceof StyledText) {
							printValue(out, "Text", text); //$NON-NLS-1$
						}
						if (node instanceof Button) {
							printButton(out, (Button) node, text);
						}
						if (node instanceof Combo || node instanceof CCombo) {
							printValue(out, "Combo", text); //$NON-NLS-1$
						}
					}
				}
			});
		}
		return true;
	}

	private void printValue(PrintStream out, String widgetType, String value) {
		out.println(widgetType + ": " + value); //$NON-NLS-1$
	}

	private void printButton(PrintStream out, Button button, String text) {
		if (!isPushButton(button)) {
			text += button.getSelection() ? " (selected)" : " (not selected)"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		printValue(out, "Button", text); //$NON-NLS-1$
	}

	private boolean isPushButton(Button button) {
		return (button.getStyle() & (SWT.CHECK | SWT.RADIO | SWT.TOGGLE)) == 0;
	}

	private void printTree(PrintStream out, Tree tree) {
		for (TreeColumn tc : tree.getColumns()) {
			out.println("Tree Column: " + tc.getText()); //$NON-NLS-1$
		}

		int itemCount = Math.min(tree.getItemCount(), MAX_TABLE_ITEMS);
		int columnCount = tree.getColumnCount();
		for (int n = 0; n < itemCount; n++) {
			TreeItem ti = tree.getItem(n);
			out.print("Tree Item " + n + ": "); //$NON-NLS-1$ //$NON-NLS-2$
			for (int m = 0; m < columnCount; m++) {
				out.print(ti.getText(m) + ' ');
			}
			out.println();
		}
	}

	private void printTable(PrintStream out, Table t) {
		for (TableColumn tc : t.getColumns()) {
			out.println("Table Column: " + tc.getText()); //$NON-NLS-1$
		}

		int itemCount = Math.min(t.getItemCount(), MAX_TABLE_ITEMS);
		int columnCount = t.getColumnCount();
		for (int n = 0; n < itemCount; n++) {
			TableItem ti = t.getItem(n);
			out.print("Table Item " + n + ": "); //$NON-NLS-1$ //$NON-NLS-2$
			for (int m = 0; m < columnCount; m++) {
				out.print(ti.getText(m) + ' ');
			}
			out.println();
		}
		if (t.getItemCount() > itemCount) {
			out.println("..."); //$NON-NLS-1$
		}
	}
}
