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
package org.openjdk.jmc.flightrecorder.pde.simplerule;

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

import org.openjdk.jmc.flightrecorder.pde.Activator;
import org.openjdk.jmc.pde.PluginReference;

public class SimpleRuleTemplateSection extends OptionTemplateSection {
	private static final String EXTENSION_POINT_ID_RULE_PROVIDER = "org.openjdk.jmc.flightrecorder.rules.ruleProvider"; //$NON-NLS-1$
	public static final String KEY_CLASS_NAME = "className"; //$NON-NLS-1$
	public static final String DEFAULT_CLASS_NAME = "SimpleJfrRule"; //$NON-NLS-1$
	public static final String KEY_RULE_NAME = "ruleName"; //$NON-NLS-1$
	public static final String DEFAULT_RULE_NAME = "Simple JFR Rule"; //$NON-NLS-1$

	public SimpleRuleTemplateSection() {
		setPageCount(1);
		createOptions();
	}

	@Override
	public String getSectionId() {
		return "simpleruletemplate"; //$NON-NLS-1$
	}

	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	@Override
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	private void createOptions() {
		addOption(KEY_PACKAGE_NAME, Messages.SimpleRuleTemplateSection_PACKAGE_LABEL, (String) null, 0);
		addOption(KEY_CLASS_NAME, Messages.SimpleRuleTemplateSection_CLASS_NAME_LABEL, DEFAULT_CLASS_NAME, 0);
		addOption(KEY_RULE_NAME, Messages.SimpleRuleTemplateSection_RULE_NAME_LABEL, DEFAULT_RULE_NAME, 0);
	}

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, "formpageContextID"); //$NON-NLS-1$
		page.setTitle(Messages.SimpleRuleTemplateSection_PAGE_TITLE);
		page.setDescription(Messages.SimpleRuleTemplateSection_PAGE_DESCRIPTION);
		wizard.addPage(page);
		markPagesAdded();
	}

	@Override
	public boolean isDependentOnParentWizard() {
		return true;
	}

	@Override
	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id));
	}

	@Override
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId));
	}

	@Override
	public String getUsedExtensionPoint() {
		return EXTENSION_POINT_ID_RULE_PROVIDER;
	}

	@Override
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension(EXTENSION_POINT_ID_RULE_PROVIDER, true);
		IPluginModelFactory factory = model.getPluginFactory();

		IPluginElement setElement = factory.createElement(extension);
		setElement.setName("ruleProvider"); //$NON-NLS-1$
		extension.add(setElement);
		if (!extension.isInTheModel()) {
			plugin.add(extension);
		}
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
			return packageName + ".rules"; //$NON-NLS-1$
		}
		return "rules"; //$NON-NLS-1$

	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		// Ensure schema version was defined
		if (schemaVersion == null) {
			return super.getDependencies(null);
		}
		// Create the dependencies
		IPluginReference[] dependencies = new IPluginReference[2];
		dependencies[0] = new PluginReference("org.openjdk.jmc.flightrecorder.rules", "6.0", //$NON-NLS-1$ //$NON-NLS-2$
				IMatchRules.GREATER_OR_EQUAL);
		dependencies[1] = new PluginReference("org.openjdk.jmc.flightrecorder", "6.0", IMatchRules.GREATER_OR_EQUAL); //$NON-NLS-1$ //$NON-NLS-2$
		return dependencies;
	}

	@Override
	protected ResourceBundle getPluginResourceBundle() {
		Bundle bundle = Platform.getBundle(org.openjdk.jmc.flightrecorder.pde.Activator.PLUGIN_ID);
		return Platform.getResourceBundle(bundle);
	}

	@Override
	protected URL getInstallURL() {
		return Activator.getDefault().getInstallURL();
	}

	@Override
	public String[] getNewFiles() {
		return new String[0];
	}

}
