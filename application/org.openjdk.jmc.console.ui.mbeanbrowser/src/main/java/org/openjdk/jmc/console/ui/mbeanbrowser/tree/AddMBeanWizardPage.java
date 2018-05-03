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
package org.openjdk.jmc.console.ui.mbeanbrowser.tree;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.osgi.service.prefs.Preferences;

import org.openjdk.jmc.console.ui.mbeanbrowser.MBeanBrowserPlugin;
import org.openjdk.jmc.console.ui.mbeanbrowser.messages.internal.Messages;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.ui.wizards.IPerformFinishable;

/**
 * A wizard dialog to query the user for domain and class name of MBean to create and register.
 */
public class AddMBeanWizardPage extends WizardPage implements IPerformFinishable {

	private static final String ADD_MBEAN_WIZARD_SETTINGS = "AddMBeanWizardPage"; //$NON-NLS-1$
	private static final String MBEAN_CLASS_NAME_KEY = "MBeanClassName"; //$NON-NLS-1$
	private static final String DEFAULT_MBEAN_CLASS_NAME = "com.sun.management.MissionControl"; //$NON-NLS-1$
	private static final String MBEAN_OBJECT_NAME_KEY = "MBeanObjectName"; //$NON-NLS-1$
	private static final String DEFAULT_MBEAN_OBJECT_NAME = "com.sun.management:type=MissionControl"; //$NON-NLS-1$

	private final MBeanServerConnection mbeanServerConnection;
	private final String connectionGUID;
	private Text objectNameField;
	private Text classNameField;

	public AddMBeanWizardPage(MBeanServerConnection connection, String guid) {
		super(Messages.ADD_MBEAN_LABEL);
		mbeanServerConnection = connection;
		this.connectionGUID = guid;
		setImageDescriptor(MBeanBrowserPlugin.getDefault().getImageRegistry()
				.getDescriptor(MBeanBrowserPlugin.ICON_CREATE_MBEAN_WIZARD));
		setDescription(Messages.ADD_MBEAN_LONG_DESCRIPTION);
		setTitle(Messages.ADD_MBEAN_TITLE);
	}

	@Override
	public boolean performFinish() {
		try {
			mbeanServerConnection.createMBean(getMBeanClassName(), getValidObjectName());
			storeWizardSettings();
			return true;
		} catch (Exception e) {
			setErrorMessage(
					NLS.bind(Messages.UNABLE_TO_CREATE_MBEAN_MESSAGE, e.getClass().getName(), e.getLocalizedMessage()));
			return false;
		}
	}

	private void checkPageComplete() {
		if (getValidObjectName() == null) {
			setErrorMessage(NLS.bind(Messages.INVALID_MBEAN_NAME_MESSAGE, objectNameField.getText()));
			setPageComplete(false);
			return;
		}
		if (getMBeanClassName().isEmpty()) {
			setErrorMessage(Messages.INVALID_EMPTY_CLASS_NAME);
			setPageComplete(false);
			return;
		}
		setErrorMessage(null);
		setPageComplete(true);
	}

	private ObjectName getValidObjectName() {
		String name = objectNameField.getText();
		if (name.isEmpty()) {
			return null;
		}
		try {
			return new ObjectName(name);
		} catch (MalformedObjectNameException e) {
			return null;
		}
	}

	private String getMBeanClassName() {
		return classNameField.getText().trim();
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		setControl(container);

		addLabel(container, Messages.MBEAN_OBJECT_NAME_LABEL);
		objectNameField = createField(container, DEFAULT_MBEAN_OBJECT_NAME);
		ModifyListener listener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPageComplete();
			}
		};
		objectNameField.addModifyListener(listener);
		addLabel(container, Messages.MBEAN_CLASS_NAME_LABEL);
		classNameField = createField(container, DEFAULT_MBEAN_CLASS_NAME);
		classNameField.addModifyListener(listener);

		readWizardSettings();
		checkPageComplete();
	}

	private Text createField(Composite container, String defaultText) {
		Text field = new Text(container, SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.BORDER);
		field.setText(defaultText);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd.widthHint = 400;
		field.setLayoutData(gd);
		return field;
	}

	private void addLabel(Composite container, String label) {
		Label objectNameLabel = new Label(container, SWT.NONE);
		objectNameLabel.setText(label);
		objectNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
	}

	private void readWizardSettings() {
		try {
			IMemento state = XMLMemento.createReadRoot(
					new StringReader(getServerPreferencesForConnection().get(ADD_MBEAN_WIZARD_SETTINGS, ""))); //$NON-NLS-1$
			String on = state.getString(MBEAN_OBJECT_NAME_KEY);
			if (on != null) {
				objectNameField.setText(on);
			}
			String cn = state.getString(MBEAN_CLASS_NAME_KEY);
			if (cn != null) {
				classNameField.setText(cn);
			}
		} catch (WorkbenchException e) {
		}
	}

	private Preferences getServerPreferencesForConnection() {
		return RJMXPlugin.getDefault().getServerPreferences(connectionGUID);
	}

	private void storeWizardSettings() {
		XMLMemento state = XMLMemento.createWriteRoot(ADD_MBEAN_WIZARD_SETTINGS);
		state.putString(MBEAN_OBJECT_NAME_KEY, objectNameField.getText());
		state.putString(MBEAN_CLASS_NAME_KEY, classNameField.getText());
		StringWriter sw = new StringWriter();
		try {
			state.save(sw);
		} catch (IOException e) {
			// will not happen
		}
		getServerPreferencesForConnection().put(ADD_MBEAN_WIZARD_SETTINGS, sw.toString());
	}
}
