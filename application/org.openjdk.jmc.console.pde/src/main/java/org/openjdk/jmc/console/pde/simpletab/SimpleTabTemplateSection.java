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
package org.openjdk.jmc.console.pde.simpletab;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.OptionTemplateSection;
import org.osgi.framework.Bundle;

import org.openjdk.jmc.console.pde.Activator;
import org.openjdk.jmc.pde.PluginReference;

/**
 */
public class SimpleTabTemplateSection extends OptionTemplateSection {
	public static final String KEY_CLASS_NAME = "className"; //$NON-NLS-1$
	public static final String CLASS_NAME = "SimpleSampleTab"; //$NON-NLS-1$
	public static final String KEY_NAME = "name"; //$NON-NLS-1$
	public static final String NAME = "Simple Sample Tab"; //$NON-NLS-1$

	/**
	 * Constructor for HelloWorldTemplate.
	 */
	public SimpleTabTemplateSection() {
		setPageCount(1);
		createOptions();
	}

	@Override
	public String getSectionId() {
		return "simpletabtemplate"; //$NON-NLS-1$
	}

	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	@Override
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	private void createOptions() {
		addOption(KEY_PACKAGE_NAME, "Package:", (String) null, 0);
		addOption(KEY_CLASS_NAME, "Class name:", CLASS_NAME, 0);
		addOption(KEY_NAME, "Tab name:", NAME, 0);
	}

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, "formpageContextID");
		page.setTitle("Simple Console Tab");
		page.setDescription("This template will generate a sample console tab that displays the CPU load.");
		wizard.addPage(page);
		markPagesAdded();
	}

	@Override
	public boolean isDependentOnParentWizard() {
		return true;
	}

	@Override
	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id));
	}

	@Override
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId));
	}

	@Override
	public String getUsedExtensionPoint() {
		return "org.openjdk.jmc.ui.common.formpage"; //$NON-NLS-1$
	}

	@Override
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.openjdk.jmc.console.ui.consolepage", true); //$NON-NLS-1$
		IPluginModelFactory factory = model.getPluginFactory();

		IPluginElement setElement = factory.createElement(extension);
		setElement.setName("consolePage"); //$NON-NLS-1$
		setElement.setAttribute("id", plugin.getId() + ".formPage"); //$NON-NLS-1$ //$NON-NLS-2$
		String fullClassName = getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(KEY_CLASS_NAME); //$NON-NLS-1$

		setElement.setAttribute("class", fullClassName); //$NON-NLS-1$
		setElement.setAttribute("hostEditorId", "org.openjdk.jmc.console.ui.editor"); //$NON-NLS-1$ //$NON-NLS-2$
		setElement.setAttribute("icon", "icons/tab.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		setElement.setAttribute("name", getStringOption(KEY_NAME)); //$NON-NLS-1$
		extension.add(setElement);
		if (!extension.isInTheModel()) {
			plugin.add(extension);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.pde.ui.templates.ITemplateSection#getFoldersToInclude()
	 */
	@Override
	public String[] getNewFiles() {
		return new String[] {"icons/"}; //$NON-NLS-1$
	}

	protected String getFormattedPackageName(String id) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < id.length(); i++) {
			char ch = id.charAt(i);
			if (buffer.length() == 0) {
				if (Character.isJavaIdentifierStart(ch)) {
					buffer.append(ch);
				}
			} else {
				if (Character.isJavaIdentifierPart(ch) || ch == '.') {
					buffer.append(ch);
				}
			}
		}
		String packageName = buffer.toString().toLowerCase(Locale.ENGLISH);

		if (packageName.length() != 0) {
			return packageName + ".tabs"; //$NON-NLS-1$
		}
		return "tabs"; //$NON-NLS-1$

	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		// Ensure schema version was defined
		if (schemaVersion == null) {
			return super.getDependencies(null);
		}
		// Create the dependencies for the splash handler extension template addition
		IPluginReference[] dependencies = new IPluginReference[6];
		dependencies[0] = new PluginReference("org.eclipse.swt", "3.3", IMatchRules.GREATER_OR_EQUAL); //$NON-NLS-1$
		dependencies[1] = new PluginReference("org.eclipse.core.runtime", "3.3", IMatchRules.GREATER_OR_EQUAL); //$NON-NLS-1$
		dependencies[2] = new PluginReference("org.eclipse.ui", "3.3", IMatchRules.GREATER_OR_EQUAL); //$NON-NLS-1$
		dependencies[3] = new PluginReference("org.eclipse.ui.forms", "3.3", IMatchRules.GREATER_OR_EQUAL); //$NON-NLS-1$
		dependencies[4] = new PluginReference("org.openjdk.jmc.ui", "5.2", IMatchRules.GREATER_OR_EQUAL); //$NON-NLS-1$
		dependencies[5] = new PluginReference("org.openjdk.jmc.rjmx", "5.2", IMatchRules.GREATER_OR_EQUAL); //$NON-NLS-1$
		return dependencies;
	}

	@Override
	protected ResourceBundle getPluginResourceBundle() {
		Bundle bundle = Platform.getBundle(org.openjdk.jmc.console.pde.Activator.PLUGIN_ID);
		return Platform.getResourceBundle(bundle);
	}

	@Override
	protected URL getInstallURL() {
		return Activator.getDefault().getInstallURL();
	}

}
