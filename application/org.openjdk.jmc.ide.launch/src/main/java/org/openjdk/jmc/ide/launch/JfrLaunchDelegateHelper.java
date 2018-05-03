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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.ide.launch.model.JfrArgsBuilder;
import org.openjdk.jmc.ide.launch.model.JfrLaunchModel;
import org.openjdk.jmc.ui.MCPathEditorInput;
import org.openjdk.jmc.ui.WorkbenchToolkit;
import org.openjdk.jmc.ui.common.resource.MCFile;
import org.openjdk.jmc.ui.common.util.StatusFactory;
import org.openjdk.jmc.ui.misc.DialogToolkit;

/**
 * Helper for all JFR launch delegate.
 */
public class JfrLaunchDelegateHelper {
	private static final int EXTRA_TIME_IN_MS_BEFORE_OPENING_JFR = 2000;

	private Thread recordingWaiter;
	private IDebugEventSetListener terminationListener;
	private MCFile jfrPathToOpen;
	private File recordingFile;

	public void preLaunch(final ILaunchConfiguration configuration) throws CoreException {
		terminationListener = null;
		jfrPathToOpen = null;
		recordingWaiter = null;

		JfrLaunchModel model = getModel(configuration);
		if (model.isJfrEnabled()) {
			boolean autoOpen = model.getAutoOpen();
			boolean continuous = model.isContinuous();
			long delayPlusDuration = getDelayPlusDuration(model);

			logDebugMessage(
					String.format("Preparing for launch with JFR. Auto open=%s, continuous=%s, delayPlusDuration=%s", //$NON-NLS-1$
							autoOpen, configuration, delayPlusDuration));

			jfrPathToOpen = model.getPath();
			recordingFile = getRecordingFileInEnsuredDirectory(model);

			// FIXME: Check if the user wants a unique filename, and in that case create one.

			// FIXME: The default project Mission Control only appears if autoOpen is enabled, need to find out why.

			if (autoOpen) {
				if (continuous) {
					terminationListener = createDumpOnExitListener();
				} else if (delayPlusDuration > 0) {
					recordingWaiter = createDurationWaiter(delayPlusDuration);
					terminationListener = createInterruptWaiterListener();
				}
			}
			if (terminationListener != null) {
				DebugPlugin.getDefault().addDebugEventListener(terminationListener);
			}
		}
	}

	private static long getDelayPlusDuration(JfrLaunchModel model) throws CoreException {
		try {
			IQuantity delayPlusDuration = model.getDelay();
			IQuantity duration = model.getDuration();
			if (delayPlusDuration == null) {
				if (duration == null) {
					return 0;
				}
				delayPlusDuration = duration;
			} else if (duration != null) {
				delayPlusDuration = delayPlusDuration.add(duration);
			}
			return delayPlusDuration.longValueIn(UnitLookup.MILLISECOND);
		} catch (QuantityConversionException e) {
			throw new CoreException(StatusFactory.createErr(e.getLocalizedMessage()));
		}
	}

	private static File getRecordingFileInEnsuredDirectory(JfrLaunchModel model) throws CoreException {
		try {
			File rf = model.getRecordingFile();
			File parent = rf.getParentFile();
			if (!(parent.isDirectory() || parent.mkdirs())) {
				throw new CoreException(new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID,
						"Could not create the folder for the flight recording file: " + parent.getAbsolutePath())); //$NON-NLS-1$
			}
			return rf;
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID, e.getLocalizedMessage()));
		}
	}

	private void logDebugMessage(String message) {
		if (Boolean.getBoolean("org.openjdk.jmc.debug")) { //$NON-NLS-1$
			LaunchPlugin.getDefault().getLogger().log(Level.INFO, message);
		}
	}

	public void postLaunch(final ILaunchConfiguration configuration) throws CoreException {
		if (recordingWaiter != null) {
			recordingWaiter.start();
		}
	}

	public static String getVMArgsFromString(
		String origArgs, ILaunchConfiguration configuration, boolean quotWhitespace) {
		return JfrArgsBuilder
				.joinToCommandline(getVMArgs(JfrArgsBuilder.splitCommandline(origArgs), configuration, quotWhitespace));
	}

	protected static String[] getVMArgs(String[] origArgs, ILaunchConfiguration configuration, boolean quotWhitespace) {
		String[] jfrArgs = new String[0];
		try {
			JfrLaunchModel model = getModel(configuration);
			JfrArgsBuilder argsBuilder = model.createArgsBuilder();
			LaunchPlugin.getDefault().getLogger().log(Level.INFO,
					"JFR Launch configuration: " + model.getShortConfigurationDescription()); //$NON-NLS-1$

			jfrArgs = argsBuilder.getJfrArgs(quotWhitespace);
		} catch (Exception e) {
			LaunchPlugin.getDefault().getLogger().log(Level.SEVERE, "Could not create jfr Args", e); //$NON-NLS-1$
		}

		origArgs = JfrArgsBuilder.cleanJfrArgs(origArgs);

		String[] allArgs = new String[origArgs.length + jfrArgs.length];
		for (int i = 0; i < origArgs.length; i++) {
			allArgs[i] = origArgs[i];
		}
		for (int i = 0; i < jfrArgs.length; i++) {
			allArgs[i + origArgs.length] = jfrArgs[i];
		}

		LaunchPlugin.getDefault().getLogger().log(Level.INFO,
				"VM arguments from JFR Launch : " + JfrArgsBuilder.joinToCommandline(allArgs)); //$NON-NLS-1$

		return allArgs;
	}

	protected Thread createDurationWaiter(final long delayPlusDuration) {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(delayPlusDuration + EXTRA_TIME_IN_MS_BEFORE_OPENING_JFR);
					scheduleOpenJfrJob();
				} catch (InterruptedException e) {
					LaunchPlugin.getDefault().getLogger().log(Level.WARNING,
							Messages.JfrLaunch_RECORDING_WAITER_INTERRUPTED);
				}
			}
		});
	}

	protected void removeTerminationListener() {
		DebugPlugin.getDefault().removeDebugEventListener(terminationListener);
	}

	protected IDebugEventSetListener createInterruptWaiterListener() {
		return new IDebugEventSetListener() {
			@Override
			public void handleDebugEvents(DebugEvent[] events) {
				for (int i = 0; i < events.length; i++) {
					if (events[i].getKind() == DebugEvent.TERMINATE) {
						// FIXME: Verify that we get rid of all these threads
						if (recordingWaiter != null && recordingWaiter.isAlive()) {
							recordingWaiter.interrupt();
						}
					}
				}
				removeTerminationListener();
			}

		};
	}

	private IDebugEventSetListener createDumpOnExitListener() {
		return new IDebugEventSetListener() {
			@Override
			public void handleDebugEvents(DebugEvent[] events) {
				// FIXME: Check that this only gets event for a specific launch
				for (int i = 0; i < events.length; i++) {
					if (events[i].getKind() == DebugEvent.TERMINATE) {
						scheduleOpenJfrJob();
						removeTerminationListener();
					}
				}
			}
		};
	}

	protected void scheduleOpenJfrJob() {
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(recordingFile);
			boolean wrote = jfrPathToOpen.tryWriteStream(stream, null);
			if (wrote) {
				String info = recordingFile.getAbsolutePath() + " was written to " + jfrPathToOpen.getPath() //$NON-NLS-1$
						+ " which is not expected. Should have existed after refresh."; //$NON-NLS-1$
				LaunchPlugin.getDefault().getLogger().info(info);
			}
			WorkbenchToolkit.asyncOpenEditor(new MCPathEditorInput(recordingFile));
			return;
		} catch (IOException e) {
		} finally {
			IOToolkit.closeSilently(stream);
		}
		displayErrorMessage(NLS.bind(Messages.JfrLaunch_JFR_FILE_DID_NOT_EXIST, jfrPathToOpen));
	}

	protected void displayErrorMessage(String message) {
		DialogToolkit.showErrorDialogAsync(Display.getDefault(), Messages.JfrLaunch_JFR_LAUNCH_PROBLEM_TITLE, message);
	}

	protected static JfrLaunchModel getModel(ILaunchConfiguration configuration) {
		try {
			JfrLaunchModel model = new JfrLaunchModel();
			model.updateFromConfiguration(configuration);
			return model;
		} catch (CoreException e) {
			LaunchPlugin.getDefault().getLogger().log(Level.WARNING, "Exception occurred reading configuration", e); //$NON-NLS-1$
		} catch (QuantityConversionException e) {
			LaunchPlugin.getDefault().getLogger().log(Level.WARNING, "Invalid JFR options", e); //$NON-NLS-1$
		} catch (IOException | ParseException e) {
			LaunchPlugin.getDefault().getLogger().log(Level.WARNING, "Problem reading recording configuration file", e); //$NON-NLS-1$
		}
		return null;

	}
}
