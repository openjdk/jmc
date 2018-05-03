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
package org.openjdk.jmc.browser.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.logging.Level;

import javax.management.remote.JMXServiceURL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.openjdk.jmc.browser.IJVMBrowserContextIDs;
import org.openjdk.jmc.browser.JVMBrowserPlugin;
import org.openjdk.jmc.rjmx.ConnectionDescriptorBuilder;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.IServerHandle.State;
import org.openjdk.jmc.rjmx.internal.ServerDescriptor;
import org.openjdk.jmc.rjmx.internal.ServerToolkit;
import org.openjdk.jmc.rjmx.servermodel.IServer;
import org.openjdk.jmc.rjmx.servermodel.internal.Server;
import org.openjdk.jmc.rjmx.servermodel.internal.ServerModelCredentials;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.common.security.CredentialsNotAvailableException;
import org.openjdk.jmc.ui.common.security.ICredentials;
import org.openjdk.jmc.ui.common.security.SecurityException;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.wizards.RelinkableWizardPage;

/**
 * This is the wizard page for editing connections.
 */
// FIXME: This class needs to be refactored, needs to handle {create, edit, view(discovered or connected)} x {custom, local, jdp}
public class ConnectionWizardPage extends RelinkableWizardPage {
	private static final int WIDTH_HINT = 400;
	private static final String LOCALHOST = "localhost"; //$NON-NLS-1$
	private static final String PROTOCOL_RMI = "rmi"; //$NON-NLS-1$
	final static String PAGE_NAME = Messages.ConnectionWizardPage_PAGE_NAME;
	private final String serverPath;
	private final Server server;
	private final ConnectionWizardModel model;
	private Text hostNameField;
	private Text usernameField;
	private Text portField;
	private Text javaCommandField;
	private Label javaCommandCaption;
	private Text jvmArgsField;
	private Label jvmArgsCaption;
	private Text passwordField;
	private Text connectionNameField;
	private Button storePasswordButton;
	private final InputVerifier verifier = new InputVerifier();
	private boolean hasEditedConnectionName = false;
	private JMXServiceURL currentUrl;
	private Button customUrlButton;
	private Text testConnectionStatusText;
	private boolean enableTextVerifier = true;
	private StackLayout fieldStackLayout;
	private Composite hostNamePortFieldComposite;
	private Composite serviceUrlComposite;
	private Composite fieldStack;
	private Text serviceUrlField;
	private Label pidCaption;
	private Text pidField;
	private boolean connectionFailed;

	public static final String HOSTNAME_FIELD_NAME = "wizards.connection.text.host"; //$NON-NLS-1$
	public static final String PORT_FIELD_NAME = "wizards.connection.text.port"; //$NON-NLS-1$
	public static final String USERNAME_FIELD_NAME = "wizards.connection.text.user"; //$NON-NLS-1$
	public static final String PASSWORD_FIELD_NAME = "wizards.connection.text.password"; //$NON-NLS-1$
	public static final String CONNECTIONNAME_FIELD_NAME = "wizards.connection.text.name"; //$NON-NLS-1$

	private class InputVerifier implements ModifyListener {

		@Override
		public void modifyText(ModifyEvent e) {
			if (!enableTextVerifier) {
				return;
			}
			if (e.getSource() == connectionNameField) {
				hasEditedConnectionName = true;
			} else {
				model.createdServer = null;
				connectionFailed = false;
				testConnectionStatusText.setText(Messages.ConnectionWizardPage_STATUS_IS_UNTESTED);
			}
			if (e.getSource() == serviceUrlField) {
				enableTextVerifier = false;
				updateHostAndPortFromServiceURL();
				enableTextVerifier = true;
			}

			if (!hasEditedConnectionName) {
				enableTextVerifier = false;
				connectionNameField.setText(generateConnectionName());
				enableTextVerifier = true;
			}
			checkIsPageComplete();
		}
	}

	private void showServiceUrlField() {
		enableTextVerifier = false;
		serviceUrlField.setText(currentUrl.toString());
		serviceUrlField.setEditable(isEditable());
		enableTextVerifier = true;
		fieldStackLayout.topControl = serviceUrlComposite;
		fieldStack.layout();
	}

	private void setInputFieldsShowing(boolean showServiceUrlField) {
		if (showServiceUrlField) {
			// Show field for entry of raw service URL
			showServiceUrlField();
		} else {
			// Show host and port fields instead of service URL
			fieldStackLayout.topControl = hostNamePortFieldComposite;
			fieldStack.layout();
		}
		checkIsPageComplete();
	}

	/**
	 * Tries to update the hostname and port from the service URL, given that the service URL is a
	 * valid one.
	 *
	 * @return true if successful.
	 */
	private boolean updateHostAndPortFromServiceURL() {
		try {
			JMXServiceURL tmpUrl = new JMXServiceURL(serviceUrlField.getText());
			hostNameField.setText(ConnectionToolkit.getHostName(tmpUrl));
			portField.setText(String.valueOf(ConnectionToolkit.getPort(tmpUrl)));
			setErrorMessage(null);
			return true;
		} catch (MalformedURLException e) {
			setMessage(null);
			setErrorMessage(NLS.bind(Messages.ConnectionWizardPage_ERROR_MESSAGE_NOT_VALID_SERVICE_URL,
					e.getLocalizedMessage()));
			setPageComplete(false);
			return false;
		}
	}

	private class CustomURLSelector extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			setInputFieldsShowing(customUrlButton.getSelection());
		}
	}

	/**
	 * Creates a page for editing the supplied model.
	 *
	 * @param descriptor
	 *            the descriptor to edit.
	 */
	public ConnectionWizardPage(Server server, String serverPath, ConnectionWizardModel serverConnectModel) {
		super(PAGE_NAME, Messages.ConnectionWizardPage_PAGE_TITLE, null);
		this.server = server;
		this.serverPath = serverPath;
		model = serverConnectModel;
	}

	@Override
	public void createControl(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		// Limit x size so dialog doesn't get too big if the descriptor is very large
		gd.widthHint = WIDTH_HINT;
		c.setLayoutData(gd);
		c.setLayout(new GridLayout(2, false));

		if (isDiscovered()) {
			addCommandLineInfo(c);
			addJVMArgsInfo(c);
			addPidInfo(c);
		}
		if (!isDiscovered() || hasServiceUrl()) {
			createHostPortServiceURLComposite(c);
		}

		createLine(c, 2);

		if (!isDiscovered()) {
			addUsernameAndPassword(c);
			addEncryptionOption(c);
		}

		createLine(c, 2);

		addConnectionName(c);
		if (isEditable()) {
			addConnectionTester(c);
		}

		setControl(c);
		initializeFields();
		if (isEditable()) {
			hookListeners();
		}
		checkIsPageComplete();

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJVMBrowserContextIDs.CREATE_CONNECTOR);
	}

	@Override
	public void performHelp() {
		PlatformUI.getWorkbench().getHelpSystem().displayHelp(IJVMBrowserContextIDs.CREATE_CONNECTOR);
	}

	private void hookListeners() {
		hostNameField.addModifyListener(verifier);
		connectionNameField.addModifyListener(verifier);
		serviceUrlField.addModifyListener(verifier);
		portField.addModifyListener(verifier);
		usernameField.addModifyListener(verifier);
		passwordField.addModifyListener(verifier);
	}

	static void selectAllOnFocus(final Text text) {
		text.addFocusListener(new FocusAdapter() {

			@Override
			public void focusGained(FocusEvent e) {
				text.selectAll();
			}
		});
	}

	private void addConnectionTester(Composite c) {
		Label connectionStatusLabel = new Label(c, SWT.LEFT);
		connectionStatusLabel.setText(Messages.ConnectionWizardPage_STATUS_CAPTION);
		connectionStatusLabel.setToolTipText(Messages.ConnectionWizardPage_STATUS_TOOLTIP);
		Composite testConnectionComposite = new Composite(c, SWT.LEFT);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		testConnectionComposite.setLayoutData(data);
		GridLayout l = new GridLayout(2, false);
		l.marginWidth = 0;
		l.marginHeight = 0;
		testConnectionComposite.setLayout(l);
		testConnectionStatusText = new Text(testConnectionComposite, SWT.LEFT | SWT.READ_ONLY | SWT.BORDER);
		testConnectionStatusText.setText(Messages.ConnectionWizardPage_STATUS_IS_UNTESTED);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		testConnectionStatusText.setLayoutData(data);
		Button testConnectionButton = new Button(testConnectionComposite, SWT.CENTER);
		testConnectionButton.setText(Messages.ConnectionWizardPage_TEST_CONNECTION);
		testConnectionButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (testConnection() == null) {
					testConnectionStatusText.setText(Messages.ConnectionWizardPage_STATUS_IS_CONNECTED);
				} else {
					setConnectionFailed();
					testConnectionStatusText.setText(Messages.ConnectionWizardPage_STATUS_IS_NOT_CONNECTED);
				}
			}
		});
	}

	private void addConnectionName(Composite parent) {
		Label connectionNameLabel = new Label(parent, SWT.LEFT);
		connectionNameLabel.setText(Messages.ConnectionWizardPage_CONNECTION_NAME_CAPTION);
		connectionNameLabel.setToolTipText(Messages.ConnectionWizardPage_CONNECTION_NAME_TOOLTIP);
		connectionNameField = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = WIDTH_HINT;
		connectionNameField.setLayoutData(data);
		connectionNameField.setData("name", CONNECTIONNAME_FIELD_NAME); //$NON-NLS-1$
		selectAllOnFocus(connectionNameField);
	}

	private void addEncryptionOption(Composite parent) {
		Label dummy = new Label(parent, SWT.LEFT);
		dummy.setText(""); //$NON-NLS-1$
		Composite encryptionComposite = new Composite(parent, SWT.NONE);
		encryptionComposite.setLayout(new GridLayout(2, false));
		storePasswordButton = new Button(encryptionComposite, SWT.CHECK);
		storePasswordButton.setText(Messages.ConnectionWizardPage_STORE_CAPTION);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		storePasswordButton.setLayoutData(data);
		new Label(encryptionComposite, SWT.NONE);
	}

	private void addUsernameAndPassword(Composite parent) {
		Label usernameLabel = new Label(parent, SWT.LEFT);
		usernameLabel.setText(Messages.ConnectionWizardPage_USER_CAPTION);
		usernameLabel.setToolTipText(Messages.ConnectionWizardPage_USER_TOOLTIP);
		usernameField = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		usernameField.setLayoutData(data);
		usernameField.setData("name", USERNAME_FIELD_NAME); //$NON-NLS-1$
		selectAllOnFocus(usernameField);

		Label passwordLabel = new Label(parent, SWT.LEFT);
		passwordLabel.setText(Messages.ConnectionWizardPage_PASSWORD_CAPTION);
		passwordLabel.setToolTipText(Messages.ConnectionWizardPage_PASSWORD_TOOLTIP);
		passwordField = new Text(parent, SWT.SINGLE | SWT.PASSWORD | SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		passwordField.setLayoutData(data);
		passwordField.setData("name", PASSWORD_FIELD_NAME); //$NON-NLS-1$
		selectAllOnFocus(passwordField);
	}

	private void addCommandLineInfo(Composite parent) {
		GridData data;
		javaCommandCaption = new Label(parent, SWT.LEFT);
		javaCommandCaption.setText(Messages.ConnectionWizardPage_CAPTION_JAVA_COMMAND);
		javaCommandCaption.setToolTipText(Messages.ConnectionWizardPage_TOOLTIP_DISCOVERED_INFO);
		javaCommandField = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		javaCommandField.setToolTipText(Messages.ConnectionWizardPage_TOOLTIP_DISCOVERED_INFO);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = WIDTH_HINT;
		javaCommandField.setLayoutData(data);
	}

	private void addJVMArgsInfo(Composite parent) {
		GridData data;
		jvmArgsCaption = new Label(parent, SWT.LEFT);
		jvmArgsCaption.setText(Messages.ConnectionWizardPage_CAPTION_JVMARGS);
		jvmArgsCaption.setToolTipText(Messages.ConnectionWizardPage_TOOLTIP_DISCOVERED_INFO);
		jvmArgsField = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		jvmArgsField.setToolTipText(Messages.ConnectionWizardPage_TOOLTIP_DISCOVERED_INFO);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = WIDTH_HINT;
		jvmArgsField.setLayoutData(data);
	}

	private void addPidInfo(Composite parent) {
		GridData data;
		pidCaption = new Label(parent, SWT.LEFT);
		pidCaption.setText(Messages.ConnectionWizardPage_CAPTION_PID);
		pidCaption.setToolTipText(Messages.ConnectionWizardPage_TOOLTIP_DISCOVERED_INFO);
		pidField = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		pidField.setToolTipText(Messages.ConnectionWizardPage_TOOLTIP_DISCOVERED_INFO);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		pidField.setLayoutData(data);
	}

	private boolean isDiscovered() {
		return server != null && server.getDiscoveryInfo() != null;
	}

	private boolean isEditable() {
		return !isDiscovered() && !isConnected();
	}

	private boolean isConnected() {
		return server != null && server.getServerHandle().getState() == State.CONNECTED;
	}

	private boolean hasServiceUrl() {
		return server != null ? server.getConnectionUrl() != null : false;
	}

	private void createHostPortServiceURLComposite(Composite outer) {
		// FIXME: Make sure to fix the layout to align for all components, for example commandline, pid and service url (for JDP)
		Composite inner = new Composite(outer, SWT.NONE);
		GridLayout l = new GridLayout(1, false);
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 2));

		l.marginWidth = 0;
		l.marginHeight = 0;
		inner.setLayout(l);

		fieldStack = new Composite(inner, SWT.NONE);
		fieldStack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		fieldStackLayout = new StackLayout();
		fieldStack.setLayout(fieldStackLayout);
		createHostPortComposite(fieldStack);
		createServiceURLComposite(fieldStack);
		createCustomURLButtonComposite(inner);
	}

	private void createCustomURLButtonComposite(Composite inner) {
		CustomURLSelector customURLSelector = new CustomURLSelector();
		customUrlButton = new Button(inner, SWT.TOGGLE);
		customUrlButton.setText(Messages.ConnectionWizardPage_BUTTON_CUSTOM_JMX_SERVICE_URL_TEXT);
		customUrlButton.addSelectionListener(customURLSelector);
		customUrlButton.setLayoutData(new GridData(SWT.RIGHT, GridData.VERTICAL_ALIGN_BEGINNING, false, false));
		if (isDiscovered()) {
			customUrlButton.setVisible(false);
		}
	}

	private void createServiceURLComposite(Composite parent) {
		serviceUrlComposite = new Composite(parent, SWT.NONE);
		GridLayout l = new GridLayout(2, false);
		l.marginWidth = 0;
		l.marginHeight = 0;
		serviceUrlComposite.setLayout(l);
		GridData data = new GridData(GridData.FILL_HORIZONTAL, SWT.CENTER, true, true);
		serviceUrlComposite.setLayoutData(data);

		Label serviceLabel = new Label(serviceUrlComposite, SWT.LEFT);
		serviceLabel.setText(Messages.ConnectionWizardPage_SERVICE_URL_CAPTION);
		serviceLabel.setToolTipText(Messages.ConnectionWizardPage_SERVICE_URL_TOOLTIP);
		serviceUrlField = new Text(serviceUrlComposite, SWT.SINGLE | SWT.BORDER);
		serviceLabel.setToolTipText(Messages.ConnectionWizardPage_SERVICE_URL_TOOLTIP);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		serviceUrlField.setLayoutData(data);
		selectAllOnFocus(serviceUrlField);
	}

	private void createHostPortComposite(Composite parent) {
		GridLayout l;
		hostNamePortFieldComposite = new Composite(parent, SWT.NONE);
		l = new GridLayout(2, false);
		l.marginWidth = 0;
		l.marginHeight = 0;
		hostNamePortFieldComposite.setLayout(l);
		Label hostNameLabel = new Label(hostNamePortFieldComposite, SWT.LEFT);
		hostNameLabel.setText(Messages.ConnectionWizardPage_HOST_CAPTION);
		hostNameLabel.setToolTipText(Messages.ConnectionWizardPage_HOST_TOOLTIP);
		hostNameField = new Text(hostNamePortFieldComposite, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		hostNameField.setLayoutData(data);
		hostNameField.setData("name", HOSTNAME_FIELD_NAME); //$NON-NLS-1$
		selectAllOnFocus(hostNameField);

		Label portLabel = new Label(hostNamePortFieldComposite, SWT.LEFT);
		portLabel.setText(Messages.ConnectionWizardPage_PORT_CAPTION);
		portLabel.setToolTipText(Messages.ConnectionWizardPage_PORT_TOOLTIP);
		portField = new Text(hostNamePortFieldComposite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		portField.setLayoutData(data);
		portField.setData("name", PORT_FIELD_NAME); //$NON-NLS-1$
		selectAllOnFocus(portField);
	}

	private void initializeFields() {
		String host = LOCALHOST;
		int port = ConnectionToolkit.getDefaultPort();
		String name = LOCALHOST;
		if (server != null) {
			currentUrl = server.getConnectionUrl();
			name = server.getServerHandle().getServerDescriptor().getDisplayName();
			ICredentials credential = server.getCredentials();
			if (credential != null) {
				try {
					String username = credential.getUsername();
					if (username != null) {
						usernameField.setText(username);
					}
					String password = credential.getPassword();
					if (password != null) {
						passwordField.setText(password);
					}
					if (storePasswordButton != null) {
						storePasswordButton.setSelection(credential.getExportedId() != null);
					}
				} catch (CredentialsNotAvailableException e) {
					// Open dialog and let credentials be overwritten
				} catch (SecurityException e) {
					throw new RuntimeException(e);
				}
			}
		}

		if (currentUrl != null) {
			host = ConnectionToolkit.getHostName(currentUrl);
			port = ConnectionToolkit.getPort(currentUrl);
			try {
				// Used to see what the URL path should look like by default
				String defaultUrlPath = ConnectionToolkit.createServiceURL(host, port).getURLPath();
				if (!(currentUrl.getProtocol().equals(PROTOCOL_RMI)
						&& currentUrl.getURLPath().equals(defaultUrlPath))) {
					// The protocol or URL path does not match defaults,
					// must be custom URL
					customUrlButton.setSelection(true);
				}
			} catch (IOException e) {
				customUrlButton.setSelection(true);
				JVMBrowserPlugin.getDefault().getLogger().log(Level.FINE, e.getMessage(), e);
			}
		}

		connectionNameField.setText(name);
		if (isDiscovered()) {
			String commandLine = ServerToolkit.getJavaCommand(server);
			String jvmArgs = ServerToolkit.getJVMArguments(server);
			javaCommandField.setText(commandLine == null ? "" : commandLine); //$NON-NLS-1$
			jvmArgsField.setText(jvmArgs == null ? "" : jvmArgs); //$NON-NLS-1$
			Integer pid = ServerToolkit.getPid(server.getServerHandle());
			pidField.setText(pid != null ? "" + pid : Messages.ConnectionWizardPage_TEXT_UNKNOWN); //$NON-NLS-1$
			if (hasServiceUrl()) {
				showServiceUrlField();
			}
		} else {
			hostNameField.setText(host);
			portField.setText("" + port); //$NON-NLS-1$
			setInputFieldsShowing(customUrlButton.getSelection());
			hasEditedConnectionName = !name.equals(generateConnectionName());
		}
		if (!isEditable()) {
			connectionNameField.setEditable(false);
			if (!isDiscovered()) {
				hostNameField.setEditable(false);
				portField.setEditable(false);
				passwordField.setEditable(false);
				usernameField.setEditable(false);
				serviceUrlField.setEditable(false);
				storePasswordButton.setEnabled(false);
			}
		}
	}

	private void checkIsPageComplete() {
		String warningMessage = null;
		if (!isEditable()) {
			setErrorMessage(null);
			setMessage(isDiscovered() ? Messages.ConnectionWizardPage_MESSAGE_LOCAL_CONNECTION
					: Messages.ConnectionWizardPage_MESSAGE_CONNECTED);
			setPageComplete(false);
			return;
		}

		if (customUrlButton.getSelection()) {
			try {
				currentUrl = new JMXServiceURL(serviceUrlField.getText());
				if (checkServerWithSameUrl(currentUrl)) {
					warningMessage = Messages.ConnectionWizardPage_INFO_MESSAGE_SAME_URL;
				}
				setErrorMessage(null);
			} catch (MalformedURLException ex) {
				setError(NLS.bind(Messages.ConnectionWizardPage_ERROR_MESSAGE_NOT_VALID_SERVICE_URL,
						ex.getLocalizedMessage()));
				return;
			}
		} else {
			int port;
			try {
				port = Integer.parseInt(portField.getText());
				if (port < 0 || port > 65535) {
					setError(NLS.bind(Messages.ConnectionWizardPage_ERROR_INVALID_PORT, String.valueOf(port)));
					return;
				}
			} catch (NumberFormatException e) {
				setError(Messages.ConnectionWizardPage_ERROR_MESSAGE_PORT_MUST_BE_INTEGER);
				return;
			}
			if (checkServerWithSameHostPort(getPort(), getHostName())) {
				warningMessage = Messages.ConnectionWizardPage_INFO_MESSAGE_SAME_HOST_PORT;
			}
			try {
				IConnectionDescriptor props = new ConnectionDescriptorBuilder().hostName(getHostName()).port(getPort())
						.build();
				JMXServiceURL tmpUrl = props.createJMXServiceURL();
				currentUrl = new JMXServiceURL(tmpUrl.toString());
			} catch (IOException ex) {
				setError(NLS.bind(Messages.ConnectionWizardPage_ERROR_MESSAGE_NOT_VALID_SERVICE_URL,
						ex.getLocalizedMessage()));
				return;
			}
		}
		if (warningMessage == null && checkServerWithSameName(getConnectionName())) {
			warningMessage = NLS.bind(Messages.ConnectionWizardPage_INFO_MESSAGE_NAME_ALREADY_EXIST,
					getConnectionName());
		}
		setErrorMessage(null);
		setMessage(
				warningMessage != null ? warningMessage
						: Messages.ConnectionWizardPage_MESSAGE_ENTER_CONNECTION_DETAILS,
				warningMessage != null ? INFORMATION : NONE);
		setPageComplete(true);
	}

	private boolean checkServerWithSameName(String connectionName) {
		if (server == null) {
			for (IServer s : model.serverModel.elements()) {
				if (connectionName.equals(s.getServerHandle().getServerDescriptor().getDisplayName())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkServerWithSameHostPort(int port, String host) {
		if (server == null) {
			for (Server s : model.serverModel.elements()) {
				JMXServiceURL url = s.getConnectionUrl();
				if (url != null && host.equals(ConnectionToolkit.getHostName(url))
						&& port == ConnectionToolkit.getPort(url)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkServerWithSameUrl(JMXServiceURL currentUrl) {
		if (server == null) {
			for (Server s : model.serverModel.elements()) {
				JMXServiceURL url = s.getConnectionUrl();
				if (url != null && currentUrl.equals(url)) {
					return true;
				}
			}
		}

		return false;
	}

	private void setError(String errorMessage) {
		setMessage(null);
		setErrorMessage(errorMessage);
		setPageComplete(false);
	}

	void updateModel() {
		if (isEditable()) {
			try {
				String username = getUsername();
				String password = getPassword();
				boolean storePassword = getStorePassword();
				String connectionName = getConnectionName();

				ServerModelCredentials credentials = new ServerModelCredentials(username, password, storePassword);
				IConnectionDescriptor cd = new ConnectionDescriptorBuilder().url(currentUrl).credentials(credentials)
						.build();

				Server newServer;
				if (server != null) {
					IServerDescriptor sd = new ServerDescriptor(server.getServerHandle().getServerDescriptor(),
							connectionName);
					newServer = new Server(server.getPath(), currentUrl, credentials, server.getDiscoveryInfo(), sd,
							cd);

				} else {
					IServerDescriptor sd = new ServerDescriptor(null, connectionName, null);
					newServer = new Server(serverPath, currentUrl, credentials, null, sd, cd);
				}
				model.createdServer = newServer;
				model.connectToServer = newServer;
			} catch (SecurityException e) {
				UIPlugin.getDefault().getLogger().log(Level.FINE, "Could not build a IConnectionDescriptor", e); //$NON-NLS-1$
				MessageDialog.openWarning(getShell(),
						org.openjdk.jmc.ui.security.Messages.SecurityDialogs_UNABLE_TO_CONTINUE,
						Messages.ConnectionWizard_EXCEPTION_COULD_NOT_STORE_CONNECTION);
			}
		}
	}

	private Exception testConnection() {
		if (model.createdServer == null) {
			updateModel();
			ConnectionTester ct = new ConnectionTester(model.createdServer.getServerHandle());
			JVMBrowserPlugin.getDefault().runProgressTask(false, true, false, ct);
			if (ct.exception != null) {
				model.createdServer = null;
			}
			return ct.exception;
		}
		return null;
	}

	private void setConnectionFailed() {
		connectionFailed = true;
		setPageComplete(true);
	}

	private String generateConnectionName() {
		if (Integer.toString(ConnectionToolkit.getDefaultPort()).equals(portField.getText().trim())) {
			return hostNameField.getText().trim();
		}
		return hostNameField.getText().trim() + ":" + portField.getText().trim(); //$NON-NLS-1$
	}

	private static void createLine(Composite parent, int ncol) {
		Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData.horizontalSpan = ncol;
		line.setLayoutData(gridData);
	}

	// Getters for the text fields
	private int getPort() {
		try {
			return Integer.parseInt(portField.getText());
		} catch (NumberFormatException nfe) {
			return -1;
		}
	}

	private String getConnectionName() {
		return connectionNameField.getText().trim();
	}

	private String getUsername() {
		return usernameField.getText().trim();
	}

	private String getPassword() {
		if (passwordField.getText().trim() != null && !passwordField.getText().trim().equals("")) { //$NON-NLS-1$
			return passwordField.getText().trim();
		}
		return null;
	}

	private boolean getStorePassword() {
		return storePasswordButton.getSelection();
	}

	private String getHostName() {
		return hostNameField.getText().trim();
	}

	@Override
	public IWizardPage getNextPage() {
		Exception e = testConnection();
		if (e != null) {
			DialogToolkit.showException(getControl().getShell(), Messages.ConnectionWizardPage_STATUS_IS_NOT_CONNECTED,
					NLS.bind(Messages.ConnectionWizardPage_COULD_NOT_CONNECT_DISABLE_NEXT, getConnectionName()), e);
			return null;
		} else {
			return super.getNextPage();
		}

	}

	@Override
	public boolean canFlipToNextPage() {
		return !connectionFailed && super.canFlipToNextPage();
	}

	private static class ConnectionTester implements IRunnableWithProgress {

		IServerHandle sh;
		Exception exception;

		ConnectionTester(IServerHandle sh) {
			this.sh = sh;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.ConnectionWizardPage_TESTING_CONNECTION_CAPTION, 1);
			try {
				sh.connect(Messages.ConnectionWizardPage_TESTING_CONNECTION_CAPTION).close();
				monitor.worked(1);
			} catch (Exception e) {
				exception = e;
			} finally {
				monitor.done();
			}
		}

	}
}
