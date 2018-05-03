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
package org.openjdk.jmc.flightrecorder.controlpanel.ui;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageRegistry;
import org.osgi.framework.BundleContext;

import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.ConfigurationRepositoryFactory;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationRepository;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.ui.MCAbstractUIPlugin;
import org.openjdk.jmc.ui.common.CorePlugin;
import org.openjdk.jmc.ui.common.idesupport.IDESupportToolkit;
import org.openjdk.jmc.ui.common.idesupport.IIDESupport;
import org.openjdk.jmc.ui.common.resource.MCFile;
import org.openjdk.jmc.ui.misc.DialogToolkit;

/**
 * The activator class that controls the plug-in life cycle of the Flight Recorder Control Panel
 * plug-in.
 */
public final class ControlPanel extends MCAbstractUIPlugin {
	private static final String LAST_PATH = "last.jfr.path"; //$NON-NLS-1$
	public static final String PLUGIN_ID = "org.openjdk.jmc.flightrecorder.controlpanel.ui"; //$NON-NLS-1$
	/*
	 * Link to the commercial license, for when connecting to JDK versions prior to 11, and
	 * dynamically enabling JFR.
	 */
	private static final String COMMERCIAL_LICENSE_URL = "http://www.oracle.com/technetwork/java/javase/terms/products/index.html"; //$NON-NLS-1$
	private static final String DEFAULT_FILENAME = "flight_recording"; //$NON-NLS-1$
	private static final int MAX_FILENAME_SUFFIX_LENGTH = 100;
	private static ControlPanel s_plugin;

	private EventConfigurationRepository m_repository;

	public ControlPanel() {
		super(PLUGIN_ID);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		s_plugin = this;
		m_repository = ConfigurationRepositoryFactory.create();
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		registerFromImageConstantClass(registry, ImageConstants.class);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		s_plugin = null;
		super.stop(context);
	}

	public static ControlPanel getDefault() {
		return s_plugin;
	}

	public EventConfigurationRepository getRecordingTemplateRepository() {
		return m_repository;
	}

	public static boolean askUserForEnable(IFlightRecorderService flrService, String question)
			throws FlightRecorderException {
		if (DialogToolkit.openQuestionWithLinkOnUiThread(Messages.COMMERCIAL_FEATURES_QUESTION_TITLE,
				question + "\n\n" + Messages.COMMERCIAL_FEATURES_QUESTION_TIP + //$NON-NLS-1$
						"\n\n" + Messages.COMMERCIAL_FEATURES_QUESTION_LICENSE, //$NON-NLS-1$
				Messages.COMMERCIAL_FEATURES_QUESTION_LINKTEXT, COMMERCIAL_LICENSE_URL)) {
			flrService.enable();
			return true;
		}
		return false;
	}

	public static MCFile getDefaultRecordingFile(IServerHandle server) {
		return getDefaultRecordingFile(server.getServerDescriptor().getDisplayName());
	}

	public static MCFile getDefaultRecordingFile(String suffixPart) {
		suffixPart = suffixPart.replaceAll("[^A-Za-z0-9]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		suffixPart = suffixPart.length() > 0 ? "_" + suffixPart : suffixPart; //$NON-NLS-1$
		suffixPart = suffixPart.length() > MAX_FILENAME_SUFFIX_LENGTH
				? suffixPart.substring(0, MAX_FILENAME_SUFFIX_LENGTH) : suffixPart;

		String lastPathStr = getDefault().getPreferenceStore().getString(LAST_PATH);
		if (lastPathStr.isEmpty()) {
			lastPathStr = getDefaultRecordingFolder() + File.separator + "dummy.jfr"; //$NON-NLS-1$
		}
		IPath lastPath = Path.fromOSString(lastPathStr);

		IPath lastFolder = lastPath.removeLastSegments(1);
		MCFile f = getDefaultRecordingFile(lastFolder, suffixPart);
		int i = 1;
		while (IDESupportToolkit.validateFileResourcePath(f.getPath()) == IIDESupport.FILE_EXISTS_STATUS
				&& i < 100000) {
			f = getDefaultRecordingFile(lastFolder, suffixPart + "_" + (i++)); //$NON-NLS-1$
		}
		return f;
	}

	private static String getDefaultRecordingFolder() {
		return System.getProperty("user.home"); //$NON-NLS-1$
	}

	private static MCFile getDefaultRecordingFile(IPath folder, String suffixPart) {
		String filename = DEFAULT_FILENAME + suffixPart + "." + FlightRecorderUI.FLIGHT_RECORDING_FILE_EXTENSION; //$NON-NLS-1$
		return IDESupportToolkit.createDefaultFileResource(folder.append(filename).toOSString());
	}

	public static MCFile openRecordingFileBrowser(MCFile currentFile) {
		MCFile selected = CorePlugin.getDefault().getIDESupport().browseForSaveAsFile(
				Messages.RECORDING_FILE_BROWSER_TITLE, currentFile.getPath(),
				FlightRecorderUI.FLIGHT_RECORDING_FILE_EXTENSION, Messages.RECORDING_FILE_BROWSER_DESCRIPTION);
		if (selected != null) {
			getDefault().getPreferenceStore().putValue(LAST_PATH, selected.getPath());
		}
		return selected;
	}

	public static String getRecordingFileValidationMessage(IStatus pathValidation) {
		if (pathValidation == IIDESupport.FILE_EXISTS_STATUS) {
			return Messages.RECORDING_FILE_EXISTS;
		} else if (!pathValidation.isOK()) {
			return pathValidation.getMessage();
		}
		return null;
	}
}
