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
package org.openjdk.jmc.ide.launch.model;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.JavaRuntime;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.ConfigurationRepositoryFactory;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationRepository;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards.RecordingWizardModel;
import org.openjdk.jmc.ide.launch.LaunchPlugin;
import org.openjdk.jmc.ide.launch.Messages;
import org.openjdk.jmc.ui.common.idesupport.IDESupportToolkit;

public class JfrLaunchModel extends RecordingWizardModel {

	public static final String JRE_SUPPORTS_DUMPONEXIT_CHANGED = "jreSupportsDumpOnExitChanged"; //$NON-NLS-1$

	private static final String TEMPLATE_FILEEXTENSION = ".jfc"; //$NON-NLS-1$
	private static final String LIB_JFR = "lib/jfr"; //$NON-NLS-1$
	private static final String JRE_LIB_JFR = "jre/lib/jfr"; //$NON-NLS-1$

	private static final String EMPTY = ""; //$NON-NLS-1$

	private static final boolean DEFAULT_ENABLED = false;
	private static final boolean DEFAULT_AUTO_OPEN = false;

	// TODO: Should this be changed to "profile"?
	private static final String DEFAULT_SETTINGS = "default"; //$NON-NLS-1$
	private static final String DEFAULT_NAME = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.RECORDING_DESCRIPTOR_DEFAULT_NAME;
	private static final boolean DEFAULT_CONTINUOUS = true;

	private static final String ENABLED_ATTRIBUTE = "jmc.jfr.launching.JFR.ENABLED"; //$NON-NLS-1$
	private static final String AUTO_OPEN_ATTRIBUTE = "jmc.jfr.launching.JFR.AUTO_OPEN"; //$NON-NLS-1$
	private static final String DURATION_ATTRIBUTE = "jmc.jfr.launching.JFR.DURATION"; //$NON-NLS-1$
	private static final String DELAY_ATTRIBUTE = "jmc.jfr.launching.JFR.DELAY"; //$NON-NLS-1$
	private static final String SETTINGS_ATTRIBUTE = "jmc.jfr.launching.JFR.SETTINGS"; //$NON-NLS-1$
	private static final String FILENAME_ATTRIBUTE = "jmc.jfr.launching.JFR.FILENAME"; //$NON-NLS-1$
	private static final String NAME_ATTRIBUTE = "jmc.jfr.launching.JFR.NAME"; //$NON-NLS-1$
	private static final String CONTINUOUS_ATTRIBUTE = "jmc.jfr.launching.JFR.CONTINUOUS"; //$NON-NLS-1$

	private File jreRoot;
	private String jreVersion;
	private String jreName;

	private boolean m_autoOpen;
	private boolean m_jfrEnabled = true;
	private boolean jreSupportsDumpOnExitWithoutDefaultRecording;

	public JfrLaunchModel(boolean jfrEnabled, boolean autoOpen) {
		super(new NullFlightRecorderService(JavaVersionSupport.JDK_7_U_4), ControlPanel.getDefaultRecordingFile("")); //$NON-NLS-1$

		setFixedRecording(!DEFAULT_CONTINUOUS);

		setJfrEnabled(jfrEnabled);
		setAutoOpen(autoOpen);
	}

	public JfrLaunchModel() {
		this(DEFAULT_ENABLED, DEFAULT_AUTO_OPEN);
	}

	@Override
	protected EventConfigurationRepository createRepository(SchemaVersion version) {
		return ConfigurationRepositoryFactory.create();
	}

	@Override
	public String checkForErrors(boolean hasExtraTemplate) {
		// FIXME: Anything extra needed here?
		return super.checkForErrors(hasExtraTemplate);
	}

	public String getTemplatePath() {
		return getTemplatePath(getActiveConfiguration());
	}

	public String getTemplatePath(IEventConfiguration config) {
		if (config instanceof EventConfiguration) {
			String locationPath = ((EventConfiguration) config).getLocationPath();
			if (locationPath != null) {
				return locationPath;
			}
		}
		return EMPTY;
	}

	public void setTemplate(String templateLocation) {
		if (templateLocation != null) {
			IEventConfiguration selectConfig = null;
			for (IEventConfiguration config : getTemplateRepository().getTemplates(getVersion())) {
				if (selectConfig == null) {
					selectConfig = config;
				}
				// FIXME: Is this really a good check?
				if (templateLocation.equals(getTemplatePath(config))) {
					selectConfig = config;
					break;
				}
			}
			setActiveConfigurationTemplate(selectConfig);
		}
	}

	/**
	 * Updates the model based in the JRE set in configuration.
	 *
	 * @param configuration
	 * @return true if the model was updated
	 * @throws CoreException
	 *             if configuration can't be accessed.
	 * @throws ParseException
	 *             if configuration can't be parsed.
	 * @throws IOException
	 *             if configuration can't be read.
	 * @throws FileNotFoundException
	 *             if configuration can't be read.
	 */
	public boolean updateFromJREConfiguration(ILaunchConfiguration configuration) throws CoreException {
		File newJreRoot = getJRERoot(configuration);
		String newJreVersion = getJREVersion(configuration, newJreRoot);
		String newJreName = getJREName(configuration);
		String oldJreVersion = jreVersion;

		if (!newJreVersion.equals(jreVersion) || !newJreName.equals(jreName) || !newJreRoot.equals(jreRoot)) {
			IEventConfiguration oldSettings = getActiveConfiguration();
			IEventConfiguration newSettings = oldSettings;

			jreRoot = newJreRoot;
			jreVersion = newJreVersion;
			jreName = newJreName;

			if (removeOldConfigs(oldSettings)) {
				newSettings = null;
			}

			for (File jfcFile : findJFCFiles(jreRoot)) {
				String name = jfcFile.getName();
				name = name.substring(0, name.length() - TEMPLATE_FILEEXTENSION.length());
				EventConfiguration jreConfig = null;
				try {
					jreConfig = new EventConfiguration(new JreFileStorageDelegate(name, jfcFile));
				} catch (IOException | ParseException e) {
					// FIXME: Should we display the error in the GUI?
					LaunchPlugin.getDefault().getLogger().log(Level.WARNING,
							MessageFormat.format("Problem reading recording configuration file: {0}", jfcFile), e); //$NON-NLS-1$
					continue;
				}

				jreConfig.setName(jfcFile.getName().replaceAll(TEMPLATE_FILEEXTENSION, "")); //$NON-NLS-1$
				getTemplateRepository().add(jreConfig);
				if (newSettings == null && (configuration.getAttribute(SETTINGS_ATTRIBUTE, DEFAULT_SETTINGS)
						.equals(jreConfig.getName()))) {
					newSettings = jreConfig;
				}
			}

			JavaVersion javaVersion = new JavaVersion(jreVersion);
			SchemaVersion newVersion = SchemaVersion.fromJavaVersion(javaVersion);
			if (newSettings != null && newSettings.getVersion() != null
					&& !newSettings.getVersion().equals(newVersion)) {
				newSettings = null;
			}
			setActiveConfigurationTemplate(newSettings);
			boolean oldSupportsDumpOnExit = jreSupportsDumpOnExitWithoutDefaultRecording;
			jreSupportsDumpOnExitWithoutDefaultRecording = javaVersion
					.isGreaterOrEqualThan(JavaVersionSupport.DUMP_ON_EXIT_WITHOUT_DEFAULTRECORDING_SUPPORTED);
			if (oldSupportsDumpOnExit != jreSupportsDumpOnExitWithoutDefaultRecording) {
				setChanged();
				notifyObservers(JRE_SUPPORTS_DUMPONEXIT_CHANGED);
			}
			if (Objects.equals(oldJreVersion, jreVersion)) {
				setVersion(newVersion);
				setChanged();
				notifyObservers(JRE_VERSION_CHANGED);

			}
			return true;
		}
		return false;
	}

	private boolean removeOldConfigs(IEventConfiguration oldSettings) {
		List<IEventConfiguration> configsToRemove = new ArrayList<>();
		for (IEventConfiguration config : getTemplateRepository().getTemplates(getVersion())) {
			// NOTE: Remove all the old jre file configs, keep the others (imported)
			// FIXME: Can we make this check nicer?
			// FIXME: Should this really be translated?
			if (Messages.VOLATILE_CONFIGURATION_IN_JRE.equals(config.getLocationInfo())) {
				configsToRemove.add(config);
			}
		}
		for (IEventConfiguration config : configsToRemove) {
			getTemplateRepository().remove(config);
		}
		return configsToRemove.contains(oldSettings);
	}

	private static Collection<File> findJFCFiles(File jreRoot) {
		Collection<File> jfcFiles = Collections.emptyList();

		File jfcDir = new File(jreRoot, JRE_LIB_JFR);
		if (jfcDir.exists()) {
			jfcFiles = findJFCFilesInDir(jfcDir);
		} else {
			jfcDir = new File(jreRoot, LIB_JFR);
			if (jfcDir.exists()) {
				jfcFiles = findJFCFilesInDir(jfcDir);
			}
		}
		return jfcFiles;
	}

	private static Collection<File> findJFCFilesInDir(File jfcDir) {
		File[] listFiles = jfcDir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.isFile() && pathname.getAbsolutePath().endsWith(TEMPLATE_FILEEXTENSION);
			}
		});
		if (listFiles == null) {
			return Collections.emptyList();
		}
		return Arrays.asList();
	}

	public void updateFromConfiguration(ILaunchConfiguration configuration)
			throws CoreException, QuantityConversionException, FileNotFoundException, IOException, ParseException {
		updateFromJREConfiguration(configuration);
		updateJfrLaunchFromConfiguration(configuration);
	}

	private void updateJfrLaunchFromConfiguration(ILaunchConfiguration configuration)
			throws CoreException, QuantityConversionException {
		setJfrEnabled(configuration.getAttribute(ENABLED_ATTRIBUTE, DEFAULT_ENABLED));
		setAutoOpen(configuration.getAttribute(AUTO_OPEN_ATTRIBUTE, DEFAULT_AUTO_OPEN));
		setName(configuration.getAttribute(NAME_ATTRIBUTE, DEFAULT_NAME));
		String filePath = configuration.getAttribute(FILENAME_ATTRIBUTE, (String) null);
		if (filePath != null) {
			setPath(IDESupportToolkit.createFileResource(filePath));
		}
		setFixedRecording(!configuration.getAttribute(CONTINUOUS_ATTRIBUTE, DEFAULT_CONTINUOUS));
		setDelay(configuration.getAttribute(DELAY_ATTRIBUTE, DEFAULT_DELAY.persistableString()));
		setDuration(configuration.getAttribute(DURATION_ATTRIBUTE, DEFAULT_DURATION.persistableString()));
		setTemplate(configuration.getAttribute(SETTINGS_ATTRIBUTE, DEFAULT_SETTINGS));
	}

	private static File getJRERoot(ILaunchConfiguration configuration) throws CoreException {
		IVMInstall vmInstall = JavaRuntime.computeVMInstall(configuration);
		return vmInstall == null ? null : vmInstall.getInstallLocation();
	}

	private String getJREVersion(ILaunchConfiguration configuration, File theJreRoot) throws CoreException {
		String jreVersion = parseJavaVersionFromJre(theJreRoot);
		// FIXME: Might need a new way to parse the update version for JDK 9 and forward, will deal with that when needed.
		if (jreVersion == null) {
			IVMInstall vmInstall = JavaRuntime.computeVMInstall(configuration);
			jreVersion = (vmInstall instanceof IVMInstall2) ? ((IVMInstall2) vmInstall).getJavaVersion()
					: Messages.JfrLaunch_UNKNOWN_JRE_VERSION;
		}
		return jreVersion;
	}

	private static String parseJavaVersionFromJre(File theJreRoot) {
		FileInputStream fis = null;
		JarInputStream jis = null;
		try {
			File rtJar = new File(theJreRoot, "jre/lib/rt.jar"); //$NON-NLS-1$
			if (!rtJar.exists()) {
				rtJar = new File(theJreRoot, "lib/rt.jar"); //$NON-NLS-1$
			}
			if (rtJar.exists()) {
				fis = new FileInputStream(rtJar);
				jis = new JarInputStream(fis);
				Manifest mf = jis.getManifest();
				jis.close();
				Attributes as = mf.getMainAttributes();
				String impVer = as.getValue("Implementation-Version"); //$NON-NLS-1$
				if (impVer != null) {
					return new JavaVersion(impVer).toString();
				}
			}
		} catch (IOException e) {
		} finally {
			IOToolkit.closeSilently(jis);
			IOToolkit.closeSilently(fis);
		}
		return null;
	}

	private static String getJREName(ILaunchConfiguration configuration) throws CoreException {
		IVMInstall vmInstall = JavaRuntime.computeVMInstall(configuration);
		return vmInstall != null ? vmInstall.getName() : Messages.JfrLaunch_UNKNOWN_JRE_NAME;
	}

	public void updateToConfiguration(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ENABLED_ATTRIBUTE, isJfrEnabled());
		configuration.setAttribute(AUTO_OPEN_ATTRIBUTE, getAutoOpen());
		configuration.setAttribute(DURATION_ATTRIBUTE, getDurationString());
		configuration.setAttribute(DELAY_ATTRIBUTE, getDelayString());
		configuration.setAttribute(SETTINGS_ATTRIBUTE, getTemplatePath());
		configuration.setAttribute(NAME_ATTRIBUTE, getName());
		configuration.setAttribute(FILENAME_ATTRIBUTE, getPath().getPath());
		configuration.setAttribute(CONTINUOUS_ATTRIBUTE, isContinuous());
	}

	public void setAutoOpen(boolean autoOpen) {
		m_autoOpen = autoOpen;
		onChange();
	}

	public boolean getAutoOpen() {
		return m_autoOpen;
	}

	public boolean isJfrEnabled() {
		return m_jfrEnabled;
	}

	public void setJfrEnabled(boolean enabled) {
		m_jfrEnabled = enabled;
		onChange();
	}

	public JfrArgsBuilder createArgsBuilder() throws Exception {
		return new JfrArgsBuilder(isJfrEnabled(), jreSupportsDumpOnExitWithoutDefaultRecording, getDuration(),
				getDelay(), getTemplatePath(), getRecordingFile().getAbsolutePath(), getName(), isContinuous());
	}

	public File getRecordingFile() throws FileNotFoundException {
		IPath path = new Path(getPath().getPath());

		/*
		 * IDESupportToolkit.resolveFileSystemPath does not work if the path does not exist, so we
		 * need to back up to find an existing path. We do not create the path, that responsibility
		 * lies elsewhere.
		 */
		File base = null;
		Stack<String> segments = new Stack<>();
		do {
			if (path.segmentCount() == 0) {
				break;
			}
			try {
				segments.push(path.lastSegment());
				path = path.removeLastSegments(1);
				base = IDESupportToolkit.resolveFileSystemPath(path.toOSString());

			} catch (FileNotFoundException fnfe) {
				// path did not exist
			}
		} while (base == null);

		if (base == null) {
			throw new FileNotFoundException("Can't resolve file " + getPath().getPath()); //$NON-NLS-1$
		}

		File f = base;
		while (!segments.empty()) {
			f = new File(f, segments.pop());
		}
		return f;
	}

	public boolean isJreSupportsDumpOnExitWithoutDefaultRecording() {
		return jreSupportsDumpOnExitWithoutDefaultRecording;
	}

	public String getShortConfigurationDescription() {
		return "JFR enabled=" + isJfrEnabled() + ", Open automatically=" + getAutoOpen() + ", Continuous=" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ isContinuous();
	}
}
