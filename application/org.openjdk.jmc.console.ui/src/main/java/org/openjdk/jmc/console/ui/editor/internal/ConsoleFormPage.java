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
package org.openjdk.jmc.console.ui.editor.internal;

import java.io.StringReader;

import javax.inject.Inject;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.service.prefs.Preferences;

import org.openjdk.jmc.console.ui.editor.IConsolePageContainer;
import org.openjdk.jmc.console.ui.editor.IConsolePageStateHandler;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.ui.common.util.Environment;
import org.openjdk.jmc.ui.misc.MementoToolkit;

/**
 * Extension point class that console tabs can subclass. The ConsoleTab uses the FormPage .
 */
public class ConsoleFormPage extends FormPage implements IConsolePageContainer {

	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ICON = "icon"; //$NON-NLS-1$
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	private static final String HELP_CONTEXT_ID = "helpContextID"; //$NON-NLS-1$

	private IMemento defaultConfig;
	private String id;
	private Image icon;
	private String helpContextID;

	@Inject
	private IConnectionHandle connectionHandle;
	private IConsolePageStateHandler stateHandler;

	/**
	 * The Console tab will be initialized by the ConsoleEditor but the extension point mechanism
	 * needs a 0-argument constructor.
	 */
	public ConsoleFormPage() {
		super(null, "", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public ConsoleEditor getEditor() {
		return (ConsoleEditor) super.getEditor();
	}

	@Override
	public Composite getBody() {
		return getManagedForm().getForm().getBody();
	}

	/**
	 * @return the connection handle associated with this tab.
	 */
	public IConnectionHandle getConnectionHandle() {
		return connectionHandle;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		Form form = managedForm.getForm().getForm();
		managedForm.getToolkit().decorateFormHeading(form);
		form.setText(getTitle());
		form.setImage(getTitleImage());

		validateDependencies();

		IToolBarManager toolBar = managedForm.getForm().getToolBarManager();
		toolBar.add(new GroupMarker(TB_FIRST_GROUP));
		toolBar.add(new GroupMarker(TB_HELP_GROUP));

		PlatformUI.getWorkbench().getHelpSystem().setHelp(managedForm.getForm(), helpContextID);

		if (Environment.isDebug()) {
			// Add button for displaying tab state
			toolBar.add(new ShowTabStateAction(this));
		}
		toolBar.update(true);
	}

	private void setHelpContextID(String contextID) {
		helpContextID = contextID;
	}

	protected void validateDependencies() {
	}

	@Override
	public IMemento loadConfig() {
		try {
			return XMLMemento.createReadRoot(new StringReader(getServerConfiguration().get(getId(), ""))); //$NON-NLS-1$
		} catch (Exception e) {
			return defaultConfig;
		}
	}

	@Override
	public IMemento getDefaultConfig() {
		return defaultConfig;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Image getTitleImage() {
		return icon;
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
		super.setInitializationData(config, propertyName, data);
		id = config.getAttribute(ATTRIBUTE_ID);
		setHelpContextID(config.getAttribute(HELP_CONTEXT_ID));

		defaultConfig = XMLMemento.createWriteRoot(id);
		IConfigurationElement[] ce = config.getChildren(ATTRIBUTE_CLASS);
		if (ce.length == 1) {
			MementoToolkit.copy(ce[0], defaultConfig);
		}
		String iconName = config.getAttribute(ATTRIBUTE_ICON);
		if (iconName != null) {
			String pluginId = config.getDeclaringExtension().getContributor().getName();
			ImageDescriptor iconDesc = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, iconName);
			icon = (Image) JFaceResources.getResources().get(iconDesc);
		}
	}

	@Override
	public void dispose() {
		if (stateHandler != null) {
			XMLMemento state = XMLMemento.createWriteRoot(getId());
			if (stateHandler.saveState(state)) {
				getServerConfiguration().put(getId(), MementoToolkit.asString(state));
			}
			stateHandler.dispose();
		}
		super.dispose();
	}

	protected boolean saveState(IMemento state) {
		if (stateHandler != null) {
			return stateHandler.saveState(state);
		}
		return false;
	}

	public void setContentStateHandler(IConsolePageStateHandler handler) {
		stateHandler = handler;
	}

	private Preferences getServerConfiguration() {
		return RJMXPlugin.getDefault().getServerPreferences(getConnectionHandle().getServerDescriptor().getGUID());
	}

	@Override
	public void presentError(final String message) {
		final IMessageManager manager = getManagedForm().getMessageManager();
		Display display = getSite().getShell().getDisplay();
		if (!display.isDisposed()) {
			display.syncExec(new Runnable() {
				@Override
				public void run() {
					manager.addMessage(message, message, null, IMessageProvider.ERROR);
				}
			});
		}
	}
}
