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
package org.openjdk.jmc.alert;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.TypedLabelProvider;

public class AlertDialog extends TitleAreaDialog {
	private Button m_clearButton;
	private Text m_text;
	private TableViewer m_viewer;

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createPopupButton(parent);
		createClearButton(parent);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
	}

	private void createPopupButton(Composite parent) {
		((GridLayout) parent.getLayout()).numColumns++;
		final Button button = new Button(parent, SWT.CHECK);
		button.setText(Messages.AlertDialog_POP_UP_ON_ALERTS_TEXT);
		button.setSelection(AlertPlugin.getDefault().getPopup());
		button.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// not used
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (button != null && !button.isDisposed()) {
					AlertPlugin.getDefault().setPopup(button.getSelection());
				}

			}
		});

	}

	protected void createClearButton(Composite parent) {
		m_clearButton = createButton(parent, IDialogConstants.CLIENT_ID, Messages.AlertDialog_CLEAR_ALERTS_TEXT0, true);
		m_clearButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// not used
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				AlertPlugin.getDefault().clearNotificationEventLog();
				m_text.setText(""); //$NON-NLS-1$
				m_viewer.setInput(null);
			}
		});
	}

	private static class AlertLabelProvider extends TypedLabelProvider<AlertObject> {

		public AlertLabelProvider() {
			super(AlertObject.class);
		}

		@Override
		protected Color getForegroundTyped(AlertObject ao) {
			return ao.getException() == null ? null : Display.getCurrent().getSystemColor(SWT.COLOR_RED);
		}

		@Override
		protected Color getBackgroundTyped(AlertObject ao) {
			return ao.getException() == null ? null : Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);
		}
	}

	private static class DateLabelProvider extends AlertLabelProvider {
		DateFormat df = DateFormat.getDateTimeInstance();

		@Override
		protected String getTextTyped(AlertObject element) {
			return df.format(element.getCreationTime());
		}

		@Override
		protected Image getImageTyped(AlertObject ao) {
			boolean ex = ao.getException() != null;
			return UIPlugin.getDefault().getImage(ex ? UIPlugin.ICON_ALERT : UIPlugin.ICON_EXCEPTION);
		}
	}

	private static class RuleLabelProvider extends AlertLabelProvider {
		@Override
		protected String getTextTyped(AlertObject ao) {
			return ao.getRule().getRulePath() + "\\" + ao.getRule().getName(); //$NON-NLS-1$
		}
	}

	private static class SourceLabelProvider extends AlertLabelProvider {

		@Override
		protected String getTextTyped(AlertObject ao) {
			return ao.getSourceName();
		}
	}

	private static class DateComparator implements Comparator<Object> {

		@Override
		public int compare(Object o1, Object o2) {
			return ((AlertObject) o1).getCreationTime().compareTo(((AlertObject) o2).getCreationTime());
		}

	}

	public AlertDialog(Shell parentShell) {
		super(parentShell);
		setTitleImage(AlertPlugin.getDefault().getImage(AlertPlugin.IMAGE_ALERT_BANNER));
		setShellStyle(SWT.DIALOG_TRIM | getDefaultOrientation() | SWT.RESIZE | SWT.MAX);
		setBlockOnOpen(false);
	}

	@Override
	protected Control createContents(Composite parent) {
		getShell().setText(Messages.AlertDialog_DIALOG_TITLE);
		getShell().setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_ALERT));
		Control contents = super.createContents(parent);
		setTitle(Messages.AlertDialog_DIALOG_TITLE);
		setMessage(Messages.AlertDialog_DIALOG_MESSAGE);
		DisplayToolkit.placeDialogInCenter(getParentShell(), getShell());

		return contents;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control dialogArea = super.createDialogArea(parent);

		GridData gd = null;

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		Composite composite = new Composite((Composite) dialogArea, SWT.NONE);
		composite.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 100;
		TableViewer viewer = createViewer(composite);
		viewer.getTable().setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = 100;
		m_text = createMessage(composite);
		m_text.setLayoutData(gd);

		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		return dialogArea;
	}

	private Text createMessage(Composite parent) {
		Text text = new Text(parent, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP);
		return text;
	}

	public void select(AlertObject alertObject) {
		m_text.setText(alertObject.getMessage());
	}

	public TableViewer createViewer(Composite parent) {
		Table table = new Table(parent, SWT.FLAT | SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
		m_viewer = new TableViewer(table);

		List<IColumn> columns = new ArrayList<>();
		columns.add(new ColumnBuilder(Messages.AlertDialog_COLUMN_HEADER_DATE, "date", new DateLabelProvider()) //$NON-NLS-1$
				.comparator(new DateComparator()).build());
		columns.add(
				new ColumnBuilder(Messages.AlertDialog_COLUMN_HEADER_RULE, "rule", new RuleLabelProvider()).build()); //$NON-NLS-1$
		columns.add(new ColumnBuilder(Messages.AlertDialog_COLUMN_HEADER_SOURCE, "source", new SourceLabelProvider()) //$NON-NLS-1$
				.build());
		ColumnManager.build(m_viewer, columns, null);

		m_viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ssel = (IStructuredSelection) event.getSelection();
				if (ssel.size() >= 1) {
					select((AlertObject) ssel.getFirstElement());
				}
			}
		});

		m_viewer.setContentProvider(ArrayContentProvider.getInstance());
		m_viewer.setInput(AlertPlugin.getDefault().getAlerts());
		return m_viewer;
	}

	public void refreshAlertDialog() {
		if (m_viewer != null && !m_viewer.getTable().isDisposed()) {
			m_viewer.setInput(AlertPlugin.getDefault().getAlerts());
		}
	}
}
