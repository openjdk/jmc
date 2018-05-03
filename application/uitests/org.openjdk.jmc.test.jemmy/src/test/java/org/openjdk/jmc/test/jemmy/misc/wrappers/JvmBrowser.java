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

import static org.openjdk.jmc.browser.wizards.Messages.ConnectionWizardPage_STORE_CAPTION;
import static org.openjdk.jmc.ui.security.Messages.MasterPasswordWizardPage_SET_MASTER_PASSWORD_TITLE;
import static org.openjdk.jmc.ui.security.Messages.MasterPasswordWizardPage_VERIFY_MASTER_PASSWORD_TITLE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jemmy.TimeoutExpiredException;
import org.junit.Assert;

import org.openjdk.jmc.browser.wizards.ConnectionWizardPage;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.helpers.ConnectionHelper;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCButton.Labels;
import org.openjdk.jmc.ui.misc.FileSelector;
import org.openjdk.jmc.ui.security.Constants;

/**
 * The Jemmy wrapper class for the JVM Browser
 */
public class JvmBrowser extends MCJemmyBase {
	private static final String ExportTreeToFileWizardPage_TREE_NAME = org.openjdk.jmc.ui.wizards.ExportTreeToFileWizardPage.TREE_NAME;
	private static final String ACTION_EDIT_TEXT = org.openjdk.jmc.browser.views.Messages.JVMBrowserView_ACTION_EDIT_TEXT;
	private static final String ACTION_DISCONNECT_TEXT = org.openjdk.jmc.browser.views.Messages.JVMBrowserView_ACTION_DISCONNECT_TEXT;
	private static final String ACTION_TREE_LAYOUT_TOOLTIP = org.openjdk.jmc.browser.views.Messages.JVMBrowserView_ACTION_TREE_LAYOUT_TOOLTIP;
	private static final String ACTION_NEW_CONNECTION_TEXT = org.openjdk.jmc.browser.views.Messages.JVMBrowserView_ACTION_NEW_CONNECTION_TEXT;
	private static final String ACTION_NEW_CONNECTION_TOOLTIP = org.openjdk.jmc.browser.views.Messages.JVMBrowserView_ACTION_NEW_CONNECTION_TOOLTIP;
	private static final String ACTION_NEW_FOLDER_TEXT = org.openjdk.jmc.browser.views.Messages.JVMBrowserView_ACTION_NEW_FOLDER_TEXT;
	private static final String ACTION_NEW_FOLDER_TOOLTIP = org.openjdk.jmc.browser.views.Messages.JVMBrowserView_ACTION_NEW_FOLDER_TOOLTIP;
	private static final String ACTION_REMOVE_TEXT = org.openjdk.jmc.browser.views.Messages.JVMBrowserView_ACTION_REMOVE_TEXT;
	private static final String CONNECTION_WIZARD_STORE_CAPTION = org.openjdk.jmc.browser.wizards.Messages.ConnectionWizardPage_STORE_CAPTION;
	private static final String DIALOG_FOLDER_PROPERTIES_TITLE = org.openjdk.jmc.browser.views.Messages.JVMBrowserView_FOLDER_PROPERTIES_TITLE_TEXT;
	private static final String DIALOG_NEW_FOLDER_DEFAULT_VALUE = org.openjdk.jmc.browser.views.Messages.JVMBrowserView_DIALOG_NEW_FOLDER_DEFAULT_VALUE;
	private static final String DIALOG_NEW_FOLDER_TITLE = org.openjdk.jmc.browser.views.Messages.JVMBrowserView_DIALOG_NEW_FOLDER_TITLE;
	private static final String DIALOG_REMOVE_TITLE = org.openjdk.jmc.browser.views.Messages.JVMBrowserView_DIALOG_REMOVE_TITLE;
	private static final String TOO_OLD_JVM_TITLE = org.openjdk.jmc.rjmx.messages.internal.Messages.JVMSupport_TITLE_TOO_OLD_JVM_CONSOLE;
	private static final String LOCAL_PROVIDER_NAME = org.openjdk.jmc.browser.attach.Messages.LocalDescriptorProvider_PROVIDER_NAME;
	private static final String COMMERCIAL_FEATURES_QUESTION_TITLE = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.COMMERCIAL_FEATURES_QUESTION_TITLE;
	private static final String DIALOG_NEW_CONNECTION_TITLE = org.openjdk.jmc.browser.wizards.Messages.ConnectionWizard_TITLE_NEW_CONNECTION;
	private static final String DIALOG_CONNECTION_PROPERTIES_TITLE = org.openjdk.jmc.browser.wizards.Messages.ConnectionWizard_TITLE_CONNECTION_PROPERTIES;
	private static final String ExportToFileWizardPage_WARN_IF_OVERWRITE_TEXT = org.openjdk.jmc.ui.wizards.Messages.ExportToFileWizardPage_WARN_IF_OVERWRITE_TEXT;
	private static final String JVM_BROWSER_TREE_NAME = org.openjdk.jmc.browser.views.JVMBrowserView.JVMBrowserView_TREE_NAME;
	private static final String ACTION_DUMP_LAST_PART_RECORDING_LABEL = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.ACTION_DUMP_LAST_PART_RECORDING_LABEL;
	private static final String ACTION_DUMP_RECORDING_LABEL = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.ACTION_DUMP_RECORDING_LABEL;
	private static final String ACTION_DUMP_ANY_RECORDING_LABEL = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.ACTION_DUMP_ANY_RECORDING_LABEL;
	private static final String ACTION_DUMP_WHOLE_RECORDING_LABEL = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.ACTION_DUMP_WHOLE_RECORDING_LABEL;
	private static final String ACTION_STOP_RECORDING_LABEL = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.ACTION_STOP_RECORDING_LABEL;
	private static final String ACTION_CLOSE_RECORDING_LABEL = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.ACTION_CLOSE_RECORDING_LABEL;
	private static final String ACTION_EDIT_RECORDING_LABEL = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.ACTION_EDIT_RECORDING_LABEL;
	private static final String DUMP_RECORDING_WIZARD_PAGE_TITLE = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.DUMP_RECORDING_WIZARD_PAGE_TITLE;

	private static final String TREE_ITEM_CONSOLE = "MBean Server";
	private static final String TREE_ITEM_FLIGHTRECORDER = "Flight Recorder";
	private static final String ACTION_START_CONSOLE_LABEL = "Start JMX Console";
	private static final String ACTION_START_FLIGHTRECORDER_LABEL = "Start Flight Recording...";
	private static final String ACTION_OPEN_PERSISTED_JMX_DATA = "Open Persisted JMX Data";

	private void ensureVisibleJvmBrowser() {
		MC.closeWelcome();
		MCMenu.ensureJvmBrowserVisible();
	}

	private MCTree getTree() {
		ensureVisibleJvmBrowser();
		return MCTree.getByName(getShell(), JVM_BROWSER_TREE_NAME);
	}

	private MCToolBar getToolBar() {
		ensureVisibleJvmBrowser();
		return MCToolBar.getByToolTip(getShell(), ACTION_TREE_LAYOUT_TOOLTIP);
	}

	/**
	 * Opens a JMX console to the Test VM.
	 */
	public void connect() {
		connect(MCJemmyTestBase.TEST_CONNECTION);
	}

	/**
	 * Opens a JMX console to the specified connection name. Will, depending on the layout of the
	 * JVM Browser, resolve the path to the connection
	 *
	 * @param name
	 *            the name of the process (local) to connect to
	 */
	public void connect(String name) {
		connect(true, createPathToLocalProcess(name));
	}

	/**
	 * Opens a JMX console to the specified connection path. This will, contrary to method
	 * {@code connect(String name)}, NOT resolve the path depending of the JVM Browser layout.
	 *
	 * @param path
	 *            the path of the connection
	 */
	public void connectRaw(String ... path) {
		connect(true, path);
	}

	/**
	 * Creates a new connection in the JVM Browser with the specified host and port, optionally with
	 * a specific name. This method doesn't validate the inputs, but it does attempt to validate
	 * that the connection is created, so if a test needs to verify the new connection dialog,
	 * specific code needs to be written for that with a {@link MCDialog}.
	 *
	 * @param host
	 *            the hostname for the connection
	 * @param port
	 *            the port for the connection
	 * @param user
	 *            the user name
	 * @param passwd
	 *            the password
	 * @param storeCredentials
	 *            {@code true} if credentials should be stored
	 * @param path
	 *            The path of the new connection, this can be either empty, in which case the
	 *            default naming scheme is used and the connection is created at the root level, or
	 *            it can be a list of strings representing the path of the new connection. If the
	 *            path is of length 1 and there is no item with that name, the new connection has
	 *            that string as the name, however, if that item exists then the new connection is
	 *            created beneath that item using the default name. This is basically the same
	 *            for strings of length n > 1.
	 */
	public void createConnection(
		String host, String port, String user, String passwd, Boolean storeCredentials, String ... path) {
		String connectionName = null;
		String[] finalPath = null;
		if (itemExists(path)) { // if the path specified already exists then it's a folder
			getTree().select(path);
			getTree().contextChoose(ACTION_NEW_CONNECTION_TEXT);
			finalPath = Arrays.copyOf(path, path.length + 1); // we need to save the name of the folder path
			finalPath[finalPath.length - 1] = getDefaultConnectionName(host, port); // with auto generated name
		} else if (path.length > 1) { // since the path doesn't exist, we have been specified a specific name
			String[] subPath = Arrays.copyOf(path, path.length - 1);
			getTree().select(subPath);
			getTree().contextChoose(ACTION_NEW_CONNECTION_TEXT);
			finalPath = path;
			connectionName = path[path.length - 1];
		} else {
			if (path.length == 1) {
				finalPath = Arrays.copyOf(path, path.length);
				connectionName = path[0];
			}
			getToolBar().clickToolItem(ACTION_NEW_CONNECTION_TOOLTIP);
		}
		MCDialog newConnection = new MCDialog(DIALOG_NEW_CONNECTION_TITLE);
		newConnection.enterText(ConnectionWizardPage.HOSTNAME_FIELD_NAME, host);
		newConnection.enterText(ConnectionWizardPage.PORT_FIELD_NAME, port);
		if (connectionName != null) {
			newConnection.enterText(ConnectionWizardPage.CONNECTIONNAME_FIELD_NAME, connectionName);
		}
		if (user != null) {
			newConnection.enterText(ConnectionWizardPage.USERNAME_FIELD_NAME, user);
		}
		if (passwd != null) {
			newConnection.enterText(ConnectionWizardPage.PASSWORD_FIELD_NAME, passwd);
		}
		if (storeCredentials != null) {
			MCButton.getByLabel(newConnection, ConnectionWizardPage_STORE_CAPTION, false).setState(storeCredentials);
		}
		newConnection.clickButton(MCButton.Labels.FINISH);
		waitForIdle();
		if (storeCredentials != null && storeCredentials == true) {
			handleSetMasterPassword(passwd);
		}
		Assert.assertTrue("Unable to create item " + Arrays.toString(finalPath) + " from " + Arrays.toString(path),
				itemExists(finalPath));
	}

	/**
	 * Creates a new connection in the JVM Browser with the specified host and port, optionally with
	 * a specific name. This method doesn't validate the inputs, but it does attempt to validate
	 * that the connection is created, so if a test needs to verify the new connection dialog,
	 * specific code needs to be written for that with a {@link MCDialog}.
	 *
	 * @param host
	 *            the hostname for the connection
	 * @param port
	 *            the port for the connection
	 * @param path
	 *            The path of the new connection, this can be either empty, in which case the
	 *            default naming scheme is used and the connection is created at the root level, or
	 *            it can be a list of strings representing the path of the new connection. If the
	 *            path is of length 1 and there is no item with that name, the new connection has
	 *            that string as the name, however, if that item exists then the new connection is
	 *            created beneath that item using the default name. This is basically the same
	 *            for strings of length n > 1.
	 */
	public void createConnection(String host, String port, String ... path) {
		createConnection(host, port, null, null, null, path);
	}

	private String getDefaultConnectionName(String host, String port) {
		String name = "";
		name += (host == null) ? "localhost" : host;
		name += ":";
		name += (port == null) ? "7091" : port;
		return name;
	}

	/**
	 * Creates a folder at the specified path
	 *
	 * @param path
	 *            the name/path of the folder, the new name will always be the last string entered
	 */
	public void createFolder(String ... path) {
		if (path.length > 1) {
			String[] subPath = Arrays.copyOf(path, path.length - 1);
			getTree().select(subPath);
			getTree().contextChoose(ACTION_NEW_FOLDER_TEXT);
		} else {
			getToolBar().clickToolItem(ACTION_NEW_FOLDER_TOOLTIP);
		}
		MCDialog newFolder = new MCDialog(DIALOG_NEW_FOLDER_TITLE);
		newFolder.replaceText(DIALOG_NEW_FOLDER_DEFAULT_VALUE, path[path.length - 1]);
		newFolder.clickButton(MCButton.Labels.OK);
		waitForIdle();
		Assert.assertTrue("Failed creating new folder", itemExists(path));
	}

	/**
	 * Deletes an item at the specified path.
	 *
	 * @param path
	 *            the path of the item to delete
	 */
	public void deleteItem(String ... path) {
		selectContextOption(ACTION_REMOVE_TEXT, path);
		MCDialog delete = new MCDialog(DIALOG_REMOVE_TITLE);
		delete.clickButton(MCButton.Labels.YES);
		waitForIdle();
		Assert.assertFalse("Failed deleting", itemExists(path));
	}

	/**
	 * Makes sure that the JVM Browser is in non-tree (flat) mode
	 */
	public void disableTreeLayout() {
		setLayout(false);
	}

	/**
	 * Finds out if the JVM Browser in tree layout mode
	 * 
	 * @return {@code true} if in tree mode, otherwise {@code false}
	 */
	public boolean isTreeLayout() {
		return getTree().hasItem(LOCAL_PROVIDER_NAME);
	}

	private String[] createPathToLocalProcess(String processName) {
		if (isTreeLayout()) {
			return new String[] {LOCAL_PROVIDER_NAME, processName};
		} else {
			return new String[] {processName};
		}
	}

	/**
	 * Closes the JMX console for the default test connection
	 */
	public void disconnect() {
		disconnect(createPathToLocalProcess(MCJemmyTestBase.TEST_CONNECTION));
	}

	/**
	 * Closes the JMX console of the specified connection name
	 *
	 * @param path
	 *            the name of the connection
	 */
	public void disconnect(String ... path) {
		selectContextOption(ACTION_DISCONNECT_TEXT, path);
		MCDialog disconnectDialog = new MCDialog(ACTION_DISCONNECT_TEXT);
		disconnectDialog.clickButton(MCButton.Labels.OK);
	}

	/**
	 * Stops the named recording on the default test connection
	 *
	 * @param name
	 *            the name of the running recording
	 */
	public void stopRecording(String name) {
		stopRecording(name, createPathToLocalProcess(MCJemmyTestBase.TEST_CONNECTION));
	}

	/**
	 * Stops the named recording on the specified connection path
	 *
	 * @param name
	 *            the name of the running recording
	 * @param path
	 *            the path to the connection for the running recording
	 */
	public void stopRecording(String name, String ... path) {
		selectContextOption(ACTION_STOP_RECORDING_LABEL, createRecordingPath(name, path));
	}

	/**
	 * Closes the named recording on the default test connection
	 *
	 * @param name
	 *            the name of the running recording
	 */
	public void closeRecording(String name) {
		closeRecording(name, createPathToLocalProcess(MCJemmyTestBase.TEST_CONNECTION));
	}

	/**
	 * Closes the named recording on the specified connection path
	 *
	 * @param name
	 *            the name of the running recording
	 * @param path
	 *            the path to the connection for the running recording
	 */
	public void closeRecording(String name, String ... path) {
		selectContextOption(ACTION_CLOSE_RECORDING_LABEL, createRecordingPath(name, path));
	}

	/**
	 * Starts the dump default recording wizard on the default test connection
	 *
	 * @return a {@link MCDialog}
	 */
	public MCDialog dumpDefaultRecording() {
		return dumpDefaultRecording(createPathToLocalProcess(MCJemmyTestBase.TEST_CONNECTION));
	}

	/**
	 * Starts the dump default recording wizard on the specified connection path
	 *
	 * @param path
	 *            the path to the connection for the running recording
	 * @return a {@link MCDialog}
	 */
	public MCDialog dumpDefaultRecording(String ... path) {
		return doDumpRecording(ACTION_DUMP_ANY_RECORDING_LABEL, path);
	}

	/**
	 * Double clicks a recording for the default test connection and returns immediately
	 *
	 * @param name
	 *            the name of the recording
	 * @return a {@link MCDialog}
	 */
	public MCDialog doubleClickRecording(String name) {
		return doubleClickRecording(name, createPathToLocalProcess(MCJemmyTestBase.TEST_CONNECTION));
	}

	/**
	 * Double-clicks a recording for the specified connection and returns immediately
	 *
	 * @param name
	 *            the name of the recording
	 * @param path
	 *            the path of the connection
	 * @return a {@link MCDialog}
	 */
	public MCDialog doubleClickRecording(String name, String ... path) {
		getTree().selectAndClick(2, createRecordingPath(name, path));
		return MCDialog.getByAnyDialogTitle(false, DUMP_RECORDING_WIZARD_PAGE_TITLE);
	}

	/**
	 * Starts the dump recording wizard on the named recording on the default test connection
	 *
	 * @param name
	 *            the name of the running recording
	 * @return a {@link MCDialog}
	 */
	public MCDialog dumpRecording(String name) {
		return dumpRecording(name, createPathToLocalProcess(MCJemmyTestBase.TEST_CONNECTION));
	}

	/**
	 * Starts the dump recording wizard on the named recording on the specified connection path
	 *
	 * @param name
	 *            the name of the running recording
	 * @param path
	 *            the path to the connection or recording
	 * @return a {@link MCDialog}
	 */
	public MCDialog dumpRecording(String name, String ... path) {
		return doDumpRecording(ACTION_DUMP_RECORDING_LABEL, createRecordingPath(name, path));
	}

	private MCDialog doDumpRecording(String actionName, String ... path) {
		selectContextOption(actionName, path);
		return MCDialog.getByAnyDialogTitle(false, DUMP_RECORDING_WIZARD_PAGE_TITLE);
	}

	/**
	 * Starts the edit recording wizard on the named recording on the default test connection
	 *
	 * @param name
	 *            the name of the running recording
	 * @return a {@link JfrWizard}
	 */
	public JfrWizard editRecording(String name) {
		return editRecording(name, createPathToLocalProcess(MCJemmyTestBase.TEST_CONNECTION));
	}

	/**
	 * Starts the edit recording wizard on the named recording on the specified connection path
	 *
	 * @param name
	 *            the name of the running recording
	 * @param path
	 *            the path to the connection for the running recording
	 * @return a {@link JfrWizard}
	 */
	public JfrWizard editRecording(String name, String ... path) {
		selectContextOption(ACTION_EDIT_RECORDING_LABEL, createRecordingPath(name, path));
		return new JfrWizard(JfrWizard.EDIT_RECORDING_WIZARD_PAGE_TITLE);
	}

	/**
	 * Dumps all of the named recording on the default test connection
	 *
	 * @param name
	 *            the name of the running recording
	 */
	public void dumpWholeRecording(String name) {
		dumpWholeRecording(name, MCJemmyTestBase.TEST_CONNECTION);
	}

	/**
	 * Dumps all of the named recording on the specified connection path
	 *
	 * @param name
	 *            the name of the running recording
	 * @param connection
	 *            the path to the connection for the running recording
	 */
	public void dumpWholeRecording(String name, String connection) {
		selectContextOption(ACTION_DUMP_WHOLE_RECORDING_LABEL,
				createRecordingPath(name, createPathToLocalProcess(connection)));
		waitForSubstringMatchedEditor(cleanConnectionName(connection));
	}

	/**
	 * Dumps the last part of the named recording on the default test connection
	 *
	 * @param name
	 *            the name of the running recording
	 */
	public void dumpLastPartOfRecording(String name) {
		dumpLastPartOfRecording(name, MCJemmyTestBase.TEST_CONNECTION);
	}

	/**
	 * Dumps the last part of the named recording on the specified connection path
	 *
	 * @param name
	 *            the name of the running recording
	 * @param connection
	 *            the path to the connection for the running recording
	 */
	public void dumpLastPartOfRecording(String name, String connection) {
		selectContextOption(ACTION_DUMP_LAST_PART_RECORDING_LABEL,
				createRecordingPath(name, createPathToLocalProcess(connection)));
		waitForSubstringMatchedEditor(cleanConnectionName(connection));
	}

	private String cleanConnectionName(String connection) {
		return connection.replaceAll("[^A-Za-z0-9]", "");
	}

	/**
	 * Returns the filename of a currently running recording (on the default test connection)
	 *
	 * @param name
	 *            the name of the recording
	 * @return the file name
	 */
	public String getRunningRecordingFileName(String name) {
		return getRunningRecordingFileName(name, createPathToLocalProcess(MCJemmyTestBase.TEST_CONNECTION));
	}

	/**
	 * Returns the filename of a currently running recording
	 *
	 * @param name
	 *            the name of the recording
	 * @param path
	 *            the connection path
	 * @return the file name
	 */
	public String getRunningRecordingFileName(String name, String ... path) {
		// Open the editor on the recording to get the file name
		JfrWizard recordingwizard = editRecording(name, path);
		String fileName = recordingwizard.getFileName();
		recordingwizard.cancelWizard();
		return fileName;
	}

	/**
	 * Edits a connection with the specified parameters. If a parameter is not null then that field
	 * is set to the parameter value
	 *
	 * @param name
	 *            the new name to give the connection
	 * @param host
	 *            the new host
	 * @param port
	 *            the new port
	 * @param user
	 *            the username to use in the jmx connection
	 * @param serverPasswd
	 *            the server password for the specified username
	 * @param mcPasswd
	 *            the password Mission Control uses to save the credentials locally
	 * @param save
	 *            whether or not to save the credentials locally
	 * @param path
	 *            the path of the connection to edit
	 */
	public void editConnection(
		String name, String host, String port, String user, String serverPasswd, String mcPasswd, Boolean save,
		String ... path) {
		MCMenu.ensureJvmBrowserVisible();
		getTree().select(path);
		getTree().contextChoose(ACTION_EDIT_TEXT);
		MCDialog properties = new MCDialog(DIALOG_CONNECTION_PROPERTIES_TITLE);
		if (host != null) {
			properties.enterText(ConnectionWizardPage.HOSTNAME_FIELD_NAME, host);
		}
		if (port != null) {
			properties.enterText(ConnectionWizardPage.PORT_FIELD_NAME, port);
		}
		if (user != null) {
			properties.enterText(ConnectionWizardPage.USERNAME_FIELD_NAME, user);
		}
		if (serverPasswd != null) {
			properties.enterText(ConnectionWizardPage.PASSWORD_FIELD_NAME, serverPasswd);
		}
		if (name != null) {
			properties.enterText(ConnectionWizardPage.CONNECTIONNAME_FIELD_NAME, name);
		}
		if (save != null) {
			properties.setButtonState(CONNECTION_WIZARD_STORE_CAPTION, save);
		}
		properties.clickButton(MCButton.Labels.FINISH);
	}

	/**
	 * Makes sure that the JVM Browser is in tree mode
	 */
	public void enableTreeLayout() {
		setLayout(true);
	}

	/**
	 * Finds out whether or not a connection with the specified path exists 
	 *
	 * @param path
	 *            the path to find
	 * @return {@code true} if a connection is found, {@code false} if not.
	 */
	public boolean itemExists(String ... path) {
		return getTree().hasItem(path);
	}

	/**
	 * Opens the persisted JMX data editor for the JVM running Mission Control.
	 */
	public void openPersistedJMXData() {
		openPersistedJMXData(createPathToLocalProcess(MCJemmyTestBase.TEST_CONNECTION));
	}

	/**
	 * Opens the persisted JMX data editor for the named connection.
	 *
	 * @param path
	 *            the path to the connection
	 */
	public void openPersistedJMXData(String ... path) {
		MCMenu.ensureJvmBrowserVisible();
		selectAction(TREE_ITEM_CONSOLE, path);
		getTree().contextChoose(ACTION_OPEN_PERSISTED_JMX_DATA);
		Assert.assertTrue("Unable to find console editor \"Persisted JMX Data\"",
				MCJemmyBase.waitForSubstringMatchedEditor("Persisted JMX Data"));
	}

	/**
	 * Renames a folder at the specified path
	 *
	 * @param newName
	 *            the new name for the folder
	 * @param path
	 *            the path of the folder to rename
	 */
	public void renameFolder(String newName, String ... path) {
		String[] finalPath = Arrays.copyOf(path, path.length);
		finalPath[path.length - 1] = newName;
		getTree().select(path);
		getTree().contextChoose(ACTION_EDIT_TEXT);
		MCDialog rename = new MCDialog(DIALOG_FOLDER_PROPERTIES_TITLE);
		rename.replaceText(path[path.length - 1], newName);
		rename.closeWithButton(MCButton.Labels.OK);
		waitForIdle();
		Assert.assertTrue("Failed to properly rename folder", itemExists(finalPath));
	}

	/**
	 * Method used to start non-standard features. Also used to start standard-features with
	 * non-standard expected behavior, i.e. dynamic enablement dialog.
	 *
	 * @param path
	 *            the name of the connection to use
	 * @param option
	 *            the name of the feature to start
	 */
	public void selectContextOption(String option, String ... path) {
		MCMenu.ensureJvmBrowserVisible();
		getTree().select(path);
		getTree().contextChoose(option);
	}

	/**
	 * Attempts to connect to an MBean Server without verifying the connection. If connecting to a
	 * pre-7u4 JVM the calling code may need to handle the resulting dialog
	 *
	 * @param path
	 *            the path of the connection
	 */
	public void unverifiedConnect(String ... path) {
		connect(false, path);
	}

	/**
	 * Starts the Flight Recording wizard for the test connection
	 *
	 * @return the Flight Recording wizard dialog ({@link JfrWizard})
	 */
	public JfrWizard startFlightRecordingWizard() {
		return startFlightRecordingWizard(createPathToLocalProcess(MCJemmyTestBase.TEST_CONNECTION));
	}

	/**
	 * Starts the Flight Recording wizard for the connection with the given path
	 *
	 * @param path
	 *            the path to the connection for which to start the flight recording
	 * @return the Flight Recording wizard dialog ({@link JfrWizard})
	 */
	public JfrWizard startFlightRecordingWizard(String ... path) {
		return startFlightRecordingWizard(false, path);
	}

	/**
	 * Starts the Flight Recording wizard for the connection with the given path
	 *
	 * @param enableCommercialFeatures
	 *            {@code true} if a dialog for dynamically enabling commercial features is expected.
	 *            Otherwise {@code false}
	 * @param path
	 *            the path to the connection for which to start the flight recording
	 * @return the Flight Recording wizard dialog ({@link JfrWizard})
	 */
	public JfrWizard startFlightRecordingWizard(boolean enableCommercialFeatures, String ... path) {
		MCMenu.ensureJvmBrowserVisible();
		selectAction(TREE_ITEM_FLIGHTRECORDER, path);
		getTree().contextChoose(ACTION_START_FLIGHTRECORDER_LABEL);
		if (enableCommercialFeatures) {
			MCDialog dialog = new MCDialog(COMMERCIAL_FEATURES_QUESTION_TITLE);
			dialog.closeWithButton(Labels.YES);
		}
		return new JfrWizard(JfrWizard.START_RECORDING_WIZARD_PAGE_TITLE);
	}

	/**
	 * Exports connections to file, it does not assert that the file exists.
	 *
	 * @param fileName
	 *            the name of the file to export the connection(s) to
	 * @param names
	 *            the connection(s) to export.
	 */
	public void exportConnections(String fileName, String ... names) {
		MCDialog dialog = MCMenu.openExportDialog();
		MCTree tree = MCTree.getFirstVisible(dialog);
		tree.select("Mission Control", "Connections");
		MCButton.getByLabel(dialog, MCButton.Labels.NEXT, false).click();
		tree = MCTree.getByName(dialog.getDialogShell(), ExportTreeToFileWizardPage_TREE_NAME);
		MCButton.getByLabel(dialog, ExportToFileWizardPage_WARN_IF_OVERWRITE_TEXT, false).setState(false);
		for (String name : names) {
			tree.select(name);
			tree.setSelectedItemState(true);
		}
		MCText.getByName(dialog, FileSelector.FILENAME_FIELD_NAME).setText(fileName);
		MCButton.getByLabel(dialog, MCButton.Labels.FINISH, false).click();
		sleep(1000);
	}

	/**
	 * Attempts to import connections from the given filename.
	 *
	 * @param fileName
	 *            absolute URI for the file to import.
	 * @param fileExists
	 *            specifies if the file is expected to be found
	 */
	public void importConnections(String fileName, Boolean fileExists) {
		MCDialog dialog = MCMenu.openImportDialog();
		MCTree tree = MCTree.getFirstVisible(dialog);
		tree.select("Mission Control", "Connections");
		MCButton.getByLabel(dialog, MCButton.Labels.NEXT, false).click();
		MCText.getByName(dialog, FileSelector.FILENAME_FIELD_NAME).setText(fileName);

		if (fileExists) {
			MCButton.getByLabel(dialog, MCButton.Labels.FINISH, false).click();
		} else {
			Assert.assertFalse("Finish button not disabled",
					MCButton.getByLabel(dialog, MCButton.Labels.FINISH, false).isEnabled());
			Assert.assertFalse("Next button not disabled",
					MCButton.getByLabel(dialog, MCButton.Labels.NEXT, false).isEnabled());
			MCButton.getByLabel(dialog, MCButton.Labels.CANCEL, false).click();
		}
		sleep(1000);
	}

	/**
	 * Handles the Set Master Password dialog.
	 *
	 * @param password
	 *            the password used as a new master password. Must be longer than five characters.
	 */
	public void handleSetMasterPassword(String password) {
		MCDialog masterPasswordShell = MCDialog.getByAnyDialogTitle(
				MasterPasswordWizardPage_SET_MASTER_PASSWORD_TITLE,
				MasterPasswordWizardPage_VERIFY_MASTER_PASSWORD_TITLE);
		if (masterPasswordShell.getText().equals(MasterPasswordWizardPage_SET_MASTER_PASSWORD_TITLE)) {
			masterPasswordShell.enterText(Constants.PASSWORD1_FIELD_NAME, password);
			masterPasswordShell.enterText(Constants.PASSWORD2_FIELD_NAME, password);
		} else {
			masterPasswordShell.enterText(Constants.PASSWORD1_FIELD_NAME, password);
		}
		masterPasswordShell.clickButton(MCButton.Labels.OK);
		sleep(1000);
	}

	/**
	 * Opens a JMX console to the specified connection name. Will, depending on the layout of the
	 * JVM Browser, resolve the path to the connection
	 * 
	 * @param valid
	 *            will, if {@code true}, validate that an appropriate dialog or console editor is
	 *            opened
	 * @param path
	 *            the path of the connection
	 */
	public void connect(boolean valid, String ... path) {
		MCMenu.ensureJvmBrowserVisible();
		String connectionName = path[path.length - 1];
		selectAction(TREE_ITEM_CONSOLE, path);
		getTree().contextChoose(ACTION_START_CONSOLE_LABEL);
		if (valid) {
			if (!ConnectionHelper.is7u40orLater(connectionName)) {
				try {
					MCDialog dialog = new MCDialog(TOO_OLD_JVM_TITLE);
					dialog.closeWithButton(MCButton.Labels.OK);
				} catch (TimeoutExpiredException tee) {
					Assert.fail("JVM Too Old warning did not show.");
				}
			}
			Assert.assertTrue("Could not find JMX Console for connection \"" + connectionName + "\"",
					waitForSubstringMatchedEditor(connectionName));
		}
	}

	/**
	 * Does substring matching of the specified recording name against the currently running
	 * recordings for the specified connection
	 *
	 * @param name
	 *            the name of the recording to search for
	 * @param path
	 *            the path to the connection
	 * @return {@code true} if there is a matching recording. Otherwise {@code false}
	 */
	public boolean hasRecording(String name, String ... path) {
		return checkHasRecording(name, getCurrentRecordings(path));
	}

	/**
	 * Does substring matching of the specified recording name against the currently running
	 * recordings
	 *
	 * @param name
	 *            the name of the recording to search for
	 * @return {@code true} if there is a matching recording. Otherwise {@code false}
	 */
	public boolean hasRecording(String name) {
		return checkHasRecording(name, getCurrentRecordings());
	}

	private boolean checkHasRecording(String name, List<String> recordings) {
		boolean result = false;
		for (String recording : recordings) {
			if (recording.contains(name)) {
				result = true;
				break;
			}
		}
		return result;
	}

	/**
	 * Returns a list of currently running recordings for the default test connection
	 *
	 * @return a {@link List} of {@link String} of the currently running recordings. {@code null} if
	 *         no recordings could be found
	 */
	public List<String> getCurrentRecordings() {
		return getCurrentRecordings(MCJemmyTestBase.TEST_CONNECTION);
	}

	/**
	 * Returns a list of strings containing the currently running recordings on this JVM
	 *
	 * @param path
	 *            the path to the connection
	 * @return a {@link List} of {@link String} of the currently running recordings. {@code null} if
	 *         no recordings could be found
	 */
	public List<String> getCurrentRecordings(String ... path) {
		MCMenu.ensureJvmBrowserVisible();
		getTree().select(createRecordingPath(null, path));
		getTree().expand();
		// wait for the node to expand and be populated (info retrieved from the JVM) before looking for an error dialog
		sleep(1000);
		MCDialog error = MCDialog.getByAnyDialogTitle(false, "Problem retrieving information for");
		List<String> result = null;
		if (error != null) {
			error.closeWithButton(Labels.OK);
			// create and return an empty list
			result = new ArrayList<>();
		} else {
			result = getTree().getSelectedItemChildrenTexts();
		}
		return result;
	}

	private String[] createRecordingPath(String recordingName, String ... path) {
		List<String> completePath = Arrays.asList(path).stream().collect(Collectors.toList());
		completePath.add("Flight Recorder");
		if (recordingName != null) {
			completePath.add(recordingName);
		}
		return completePath.toArray(new String[completePath.size()]);
	}

	private void selectAction(String action, String ... path) {
		String[] actionPath = Arrays.copyOf(path, path.length + 1);
		actionPath[path.length] = action;
		getTree().select(actionPath);
	}

	private void setLayout(boolean tree) {
		if (tree) {
			getToolBar().selectToolItem(ACTION_TREE_LAYOUT_TOOLTIP);
		} else {
			getToolBar().unselectToolItem(ACTION_TREE_LAYOUT_TOOLTIP);
		}
	}

}
