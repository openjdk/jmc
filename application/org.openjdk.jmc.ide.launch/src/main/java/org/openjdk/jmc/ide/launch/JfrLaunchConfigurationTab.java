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
package org.openjdk.jmc.ide.launch;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.ide.launch.model.JfrLaunchModel;

/**
 * Launch configuration tab for starting the local java application with Flight Recorder enabled.
 */
public class JfrLaunchConfigurationTab extends AbstractLaunchConfigurationTab implements Observer {
	// TODO: Now that we target Eclipse 4, we could also use ILaunchConfigurationTab2, which might have some useful features.

	private final JfrLaunchModel model;
	private JfrLaunchPage jfrLaunchPage;

	public JfrLaunchConfigurationTab() throws Exception {
		model = new JfrLaunchModel();
		model.addObserver(this);
		jfrLaunchPage = new JfrLaunchPage(model);
	}

	@Override
	public void createControl(Composite parent) {
		// FIXME: Make sure to disable this for JDK 6
		// FIXME: Add timestamp to file?
		// TODO: Show the full commandline (at least in debug mode)
		// TODO: Change the help
		// TODO: Better tooltips?
		// TODO: Status bar plug-in that shows the last 2-3 recordings
		jfrLaunchPage.createControl(parent);
		setControl(jfrLaunchPage.getControl());
	}

	/**
	 * Initializes the given launch configuration with default values for this tab. This method is
	 * called when a new launch configuration is created such that the configuration can be
	 * initialized with meaningful values. This method may be called before this tab's control is
	 * created.
	 */
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// NOTE: Apparently this is called with one tab instance, but then another tab instance is the one used.
		try {
			JfrLaunchModel tempModel = new JfrLaunchModel();
			tempModel.updateFromConfiguration(configuration);
			tempModel.updateToConfiguration(configuration);
		} catch (Exception e) {
			setErrorMessage(NLS.bind(Messages.JfrLaunch_CONFIG_EXCEPTION, e.getLocalizedMessage()));
			LaunchPlugin.getDefault().getLogger().log(Level.INFO, "Exception occurred reading configuration", e); //$NON-NLS-1$
		}
	}

	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		// NOTE: Update the JRE in the model
		try {
			updateTemplate(workingCopy);
		} catch (Exception e) {
			setErrorMessage(NLS.bind(Messages.JfrLaunch_CONFIG_EXCEPTION, e.getLocalizedMessage()));
			LaunchPlugin.getDefault().getLogger().log(Level.INFO, "Exception occurred reading configuration", e); //$NON-NLS-1$
		}
	}

	private void updateTemplate(ILaunchConfiguration configuration)
			throws CoreException, QuantityConversionException, FileNotFoundException, IOException, ParseException {
		if (model.updateFromJREConfiguration(configuration)) {
			jfrLaunchPage.refreshTemplateCombo();
		}
	}

	/**
	 * Initializes this tab's controls with values from the given launch configuration. This method
	 * is called when a configuration is selected to view or edit, after this tab's control has been
	 * created.
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		// TODO: Improve this, would be nice to: 1. Not have to set in both gui and model. 2. Not have to deal with all settings separately.

		try {
			updateTemplate(configuration);

			JfrLaunchModel tempModel = new JfrLaunchModel();
			tempModel.updateFromConfiguration(configuration);

			if (model.isJfrEnabled() != tempModel.isJfrEnabled()) {
				jfrLaunchPage.setJfrEnabled(tempModel.isJfrEnabled());
				model.setJfrEnabled(tempModel.isJfrEnabled());
			}
			if (model.getAutoOpen() != tempModel.getAutoOpen()) {
				jfrLaunchPage.setAutoOpen(tempModel.getAutoOpen());
				model.setAutoOpen(tempModel.getAutoOpen());
			}
			if (model.isContinuous() != tempModel.isContinuous()) {
				jfrLaunchPage.setContinuous(tempModel.isContinuous());
				model.setFixedRecording(!tempModel.isContinuous());
			}
			if (!model.getDurationString().equals(tempModel.getDurationString())) {
				jfrLaunchPage.setDuration(tempModel.getDurationString());
				model.setDuration(tempModel.getDurationString());
			}
			if (!model.getDelayString().equals(tempModel.getDelayString())) {
				jfrLaunchPage.setDelay(tempModel.getDelayString());
				model.setDelay(tempModel.getDelayString());
			}
			if (!model.getName().equals(tempModel.getName())) {
				jfrLaunchPage.setName(tempModel.getName());
				model.setName(tempModel.getName());
			}
			if (!model.getPath().equals(tempModel.getPath())) {
				jfrLaunchPage.setFileName(tempModel.getPath());
				model.setPath(tempModel.getPath());
			}
		} catch (Exception e) {
			setErrorMessage(NLS.bind(Messages.JfrLaunch_CONFIG_EXCEPTION, e.getLocalizedMessage()));
			LaunchPlugin.getDefault().getLogger().log(Level.INFO, "Exception occurred reading configuration", e); //$NON-NLS-1$
		}
		setDirty(false);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		if (isDirty()) {
			model.updateToConfiguration(configuration);
			setDirty(false);
		}
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		return !jfrLaunchPage.isJfrEnabled() || getErrorMessage() == null;
	}

	@Override
	public Image getImage() {
		return LaunchPlugin.getDefault().getImage(LaunchPlugin.JFR_ICON);
	};

	@Override
	public String getName() {
		return Messages.JfrLaunch_JFR;
	}

	@Override
	public void update(Observable o, Object arg) {
		setDirty(true);
		setWarningMessage(model.getWarningMessage());
		// FIXME: Fake extra template to suppress message about using Template Manager?
		setErrorMessage(model.checkForErrors(false));
		updateLaunchConfigurationDialog();
	}
}
