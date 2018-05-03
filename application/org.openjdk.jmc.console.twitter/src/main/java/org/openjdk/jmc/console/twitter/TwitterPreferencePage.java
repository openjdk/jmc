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
package org.openjdk.jmc.console.twitter;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import twitter4j.Twitter;
import twitter4j.auth.RequestToken;

/**
 * Preference page that configures {@link TwitterPlugin}
 */
public class TwitterPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private Text m_pin;
	private Text m_consumerKey;
	private Text m_consumerSecret;
	private Button finishButton;
	private RequestToken m_requestToken;
	private Control m_authorizationButton;
	private Twitter m_twitter;
	private TableViewer m_viewer;
	private Control m_removeButton;

	public TwitterPreferencePage() {

	}

	public TwitterPreferencePage(String title) {
		super(title);
	}

	public TwitterPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {

	}

	@Override
	protected Control createContents(Composite parent) {
		resetTwitter();

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		writeText(container,
				"Before an application, like JDK Mission Control, is allowed to tweet, the application needs to be authorized.");
		writeText(container,
				"You must first set up your own new 'Application' in Twitter (apps.twitter.com) which Mission Control will use to post from. Enter the Consumer Key and Secret for your application below.");
		writeText(container,
				"Finally click Authorize to start authorization. A web browser will appear where you will grant Mission Control permission. The Twitter user you log in as in the web browser will be the one you can use from within Mission Control.");

		m_consumerKey = createMandatoryTextField(container, "Consumer Key:", 2);
		m_consumerSecret = createMandatoryTextField(container, "Consumer Secret:", 2);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, false, false);
		m_authorizationButton = createAuthorizeButton(container);
		m_authorizationButton.setLayoutData(gd2);

		writeText(container, "Then enter the PIN that is shown in the web browser and click Add.");

		m_pin = createMandatoryTextField(container, "PIN:", 1);
		m_pin.setEnabled(false);
		GridData gd4 = new GridData(SWT.FILL, SWT.FILL, false, false);
		finishButton = createAddButton(container);
		finishButton.setLayoutData(gd4);
		finishButton.setEnabled(false);

		writeText(container, "Authorized Users:");
		GridData gd10 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd10.horizontalSpan = 2;
		gd10.verticalSpan = 2;
		m_viewer = createTableViewer(container);
		m_viewer.getControl().setLayoutData(gd10);

		GridData gd11 = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		gd11.verticalSpan = 2;
		m_removeButton = createRemoveButton(container);
		m_removeButton.setLayoutData(gd11);
		m_removeButton.setEnabled(false);
		hookViewerRefresh();

		return container;
	}

	private Text createMandatoryTextField(Composite container, String caption, int textSpan) {
		GridData gdLabel = new GridData(SWT.FILL, SWT.CENTER, false, false);
		Label label = new Label(container, SWT.NONE);
		label.setText(caption);
		label.setLayoutData(gdLabel);
		GridData gdField = new GridData(SWT.FILL, SWT.FILL, true, false);
		gdField.horizontalSpan = textSpan;
		Text field = createTextField(container);
		field.setLayoutData(gdField);
		return field;
	}

	private void resetTwitter() {
		m_twitter = TwitterPlugin.getDefault().getTwitter();
		m_requestToken = null;
	}

	void updateEnablement() {
		m_removeButton.setEnabled(TwitterPlugin.getDefault().getTweeters().size() > 0);
	}

	private void hookViewerRefresh() {
		m_viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateEnablement();
			}
		});
	}

	private Button createRemoveButton(Composite container) {
		Button b = new Button(container, SWT.NONE);
		b.setText("Remove");
		b.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection ss = (IStructuredSelection) m_viewer.getSelection();
				if (!ss.isEmpty()) {
					TwitterPlugin.getDefault().removeTweeter((Tweeter) ss.getFirstElement());
					m_viewer.refresh();
				}
			}
		});
		return b;
	}

	private TableViewer createTableViewer(Composite parent) {
		Table table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		table.setLinesVisible(true);

		final TableViewer viewer = new TableViewer(table);
		new TableColumn(table, SWT.NONE);
		TableLayout tl = new TableLayout();
		tl.addColumnData(new ColumnWeightData(1));
		table.setLayout(tl);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setInput(TwitterPlugin.getDefault().getTweeters());
		return viewer;
	}

	@Override
	public boolean performOk() {
		return true;
	}

	private void writeText(Composite parent, String text) {
		GridData gd0 = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd0.horizontalSpan = 3;
		gd0.widthHint = 200;
		Label desc = new Label(parent, SWT.WRAP);
		desc.setText(text);
		desc.setLayoutData(gd0);
	}

	private Text createTextField(Composite container) {
		final Text t = new Text(container, SWT.BORDER);
		t.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				finishButton.setEnabled(t.getText().length() > 0);
			}
		});
		return t;
	}

	private Button createAddButton(final Composite container) {
		Button b = new Button(container, SWT.NONE);
		b.setText("Add");
		b.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TwitterPlugin.getDefault().addTweeter(m_twitter, m_requestToken, m_pin.getText().trim());
				m_pin.setText("");
				m_viewer.refresh();
			}
		});
		return b;
	}

	private Button createAuthorizeButton(Composite container) {
		Button b = new Button(container, SWT.NONE);
		b.setText("Authorize");
		b.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				resetTwitter();
				TwitterPlugin.getDefault().setConsumerKeyAndSecret(m_consumerKey.getText().trim(),
						m_consumerSecret.getText().trim());
				m_requestToken = TwitterPlugin.getDefault().authorize(m_twitter);
				boolean enabled = m_requestToken != null;
				finishButton.setEnabled(enabled);
				m_pin.setEnabled(enabled);
			}
		});
		return b;
	}
}
