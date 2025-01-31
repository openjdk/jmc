/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2025, Red Hat Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.console.agent.wizards;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openjdk.jmc.console.agent.AgentJmxHelper;
import org.openjdk.jmc.console.agent.messages.internal.Messages;
import org.openjdk.jmc.common.jvm.JVMDescriptor;

public class StartAgentWizardPage extends BaseWizardPage {

	private static final String FILE_OPEN_JAR_EXTENSION = "*.jar"; // $NON-NLS-1$
	private static final String FILE_OPEN_XML_EXTENSION = "*.xml"; // $NON-NLS-1$

	private final AgentJmxHelper helper;

	private Text targetJvmText;
	private Text agentJarText;
	private Button agentJarBrowseButton;
	private Text agentXmlText;
	private Button agentXmlBrowseButton;

	protected StartAgentWizardPage(AgentJmxHelper helper) {
		super(Messages.StartAgentWizardPage_PAGE_NAME);
		this.helper = helper;
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		setTitle(Messages.StartAgentWizardPage_MESSAGE_START_AGENT_WIZARD_PAGE_TITLE);
		setDescription(Messages.StartAgentWizardPage_MESSAGE_START_AGENT_WIZARD_PAGE_DESCRIPTION);

		ScrolledComposite sc = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		Composite container = new Composite(sc, SWT.NONE);
		sc.setContent(container);

		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		createTargetJvmContainer(container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createSeparator(container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createAgentBrowserContainer(container).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		bindListeners();
		populateUi();

		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		setControl(sc);
	}

	public JVMDescriptor getTargetJvm() {
		return helper.getConnectionHandle().getServerDescriptor().getJvmInfo();
	}

	public String getAgentJarPath() {
		return agentJarText.getText();
	}

	public String getAgentXmlPath() {
		return agentXmlText.getText();
	}

	private Composite createTargetJvmContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		int cols = 5;
		GridLayout layout = new GridLayout(cols, false);
		layout.horizontalSpacing = 8;
		container.setLayout(layout);

		targetJvmText = createTextInput(container, cols, Messages.StartAgentWizardPage_LABEL_TARGET_JVM, ""); // $NON-NLS-1$
		targetJvmText.setEnabled(false);

		return container;
	}

	private Composite createAgentBrowserContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		int cols = 8;
		GridLayout layout = new GridLayout(cols, false);
		layout.horizontalSpacing = 8;
		container.setLayout(layout);
		TrayDialog.setDialogHelpAvailable(false);

		agentJarText = createTextInput(container, cols - 2, Messages.StartAgentWizardPage_LABEL_AGENT_JAR,
				Messages.StartAgentWizardPage_MESSAGE_PATH_TO_AN_AGENT_JAR);
		agentJarText.setEditable(false);
		agentJarBrowseButton = createButton(container, Messages.StartAgentWizardPage_LABEL_BROWSE);
		agentJarBrowseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 0));

		agentXmlText = createTextInput(container, cols - 2, Messages.StartAgentWizardPage_LABEL_AGENT_XML,
				Messages.StartAgentWizardPage_MESSAGE_PATH_TO_AN_AGENT_CONFIG);
		agentXmlText.setEditable(false);
		agentXmlBrowseButton = createButton(container, Messages.StartAgentWizardPage_LABEL_BROWSE);
		agentXmlBrowseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 0));
		return container;
	}

	private void bindListeners() {
		agentJarBrowseButton.addListener(SWT.Selection, e -> {
			String[] path = openFileDialog(Messages.StartAgentWizardPage_DIALOG_BROWSER_FOR_AGENT_JAR,
					new String[] {FILE_OPEN_JAR_EXTENSION}, SWT.OPEN | SWT.SINGLE);
			if (path.length != 0) {
				setText(agentJarText, path[0]);
			}
		});
		agentXmlBrowseButton.addListener(SWT.Selection, e -> {
			String[] path = openFileDialog(Messages.StartAgentWizardPage_DIALOG_BROWSER_FOR_AGENT_CONFIG,
					new String[] {FILE_OPEN_XML_EXTENSION}, SWT.OPEN | SWT.SINGLE);
			if (path.length != 0) {
				setText(agentXmlText, path[0]);
			}
		});
		agentJarText.addModifyListener(e -> setPageComplete(!agentJarText.getText().isEmpty()));
		getWizard().getContainer().updateButtons();
	}

	private void populateUi() {
		setText(targetJvmText, helper.getConnectionHandle().getServerDescriptor().getDisplayName());
	}
}
