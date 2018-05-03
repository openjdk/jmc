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
package org.openjdk.jmc.flightrecorder.ui;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.osgi.framework.BundleContext;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.preferences.PreferenceKeys;
import org.openjdk.jmc.ui.MCAbstractUIPlugin;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

/**
 * The activator class controls the life cycle for the Flight Recording plug-in.
 */
public final class FlightRecorderUI extends MCAbstractUIPlugin {

	public static final String FLIGHT_RECORDING_FILE_EXTENSION = "jfr"; //$NON-NLS-1$
	public static final String TEMP_RECORDINGS_FOLDER = "tempRecordings"; //$NON-NLS-1$
	public static final String PLUGIN_ID = "org.openjdk.jmc.flightrecorder.ui"; //$NON-NLS-1$
	private static final String PAGE_MANAGER_ID = "pageManager"; //$NON-NLS-1$
	private static final String PAGE_STRUCTURE_LOCK_ID = "pageStructureLock"; //$NON-NLS-1$

	private static FlightRecorderUI plugin;

	private PageManager pageManager;

	/**
	 * The constructor
	 */
	public FlightRecorderUI() {
		super(PLUGIN_ID);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		if (pageManager != null) {
			getPreferences().put(PAGE_MANAGER_ID, pageManager.getState());
		}
		super.stop(context);
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		registerFromImageConstantClass(registry, ImageConstants.class);
	}

	public boolean removeFinishedRecordings() {
		return getPreferenceStore().getBoolean(PreferenceKeys.PROPERTY_REMOVE_FINISHED_RECORDING);
	}

	public boolean getConfirmRemoveTemplate() {
		return getPreferenceStore().getBoolean(PreferenceKeys.PROPERTY_CONFIRM_REMOVE_TEMPLATE);
	}

	public void setConfirmRemoveTemplate(boolean value) {
		getPreferenceStore().setValue(PreferenceKeys.PROPERTY_CONFIRM_REMOVE_TEMPLATE, value);
	}

	public IQuantity getLastPartToDumpTimespan() {
		return parseDumpTimespan(getPreferenceStore().getString(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TIMESPAN));
	}

	public static IQuantity parseDumpTimespan(String timespan) {
		try {
			return UnitLookup.TIMESPAN.parsePersisted(timespan);
		} catch (QuantityConversionException e) {
			return PreferenceKeys.DUMP_TIMESPAN_DEFAULT;
		}
	}

	public IQuantity getSelectionStoreSize() {
		return parseSelectionStoreSize(getPreferenceStore().getString(PreferenceKeys.PROPERTY_SELECTION_STORE_SIZE));
	}

	public static IQuantity parseSelectionStoreSize(String size) {
		try {
			return UnitLookup.NUMBER.parsePersisted(size);
		} catch (QuantityConversionException e) {
			return PreferenceKeys.DEFAULT_SELECTION_STORE_SIZE;
		}
	}

	public IQuantity getItemListSize() {
		return parseItemListSize(getPreferenceStore().getString(PreferenceKeys.PROPERTY_ITEM_LIST_SIZE));
	}

	public static IQuantity parseItemListSize(String size) {
		try {
			return UnitLookup.NUMBER.parsePersisted(size);
		} catch (QuantityConversionException e) {
			return PreferenceKeys.DEFAULT_ITEM_LIST_SIZE;
		}
	}

	public static String validateDumpTimespan(String text) {
		try {
			IQuantity timespan = UnitLookup.TIMESPAN.parseInteractive(text);
			if (timespan.doubleValue() <= 0.0) {
				return Messages.DUMP_RECORDING_TIMESPAN_LESS_THAN_ZERO;
			}
		} catch (QuantityConversionException qce) {
			return NLS.bind(Messages.DUMP_RECORDING_TIMESPAN_UNPARSABLE, qce.getLocalizedMessage());
		}
		return null;
	}

	public boolean isSetLastPartToDump() {
		return getPreferenceStore().getInt(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TYPE) == PreferenceKeys.DUMP_TIMESPAN;
	}

	public boolean isSetDumpWhole() {
		return getPreferenceStore().getInt(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TYPE) == PreferenceKeys.DUMP_WHOLE;
	}

	public boolean getShowMonitoringWarning() {
		return getPreferenceStore().getBoolean(PreferenceKeys.PROPERTY_SHOW_MONITORING_WARNING);
	}

	public void setShowMonitoringWarning(boolean showWarning) {
		getPreferenceStore().setValue(PreferenceKeys.PROPERTY_SHOW_MONITORING_WARNING, showWarning);
	}

	public boolean isAnalysisEnabled() {
		return getPreferenceStore().getBoolean(PreferenceKeys.PROPERTY_ENABLE_RECORDING_ANALYSIS);
	}

	public boolean includeExperimentalEventsAndFields() {
		return getPreferenceStore().getBoolean(PreferenceKeys.PROPERTY_INCLUDE_EXPERIMENTAL_EVENTS_AND_FIELDS);
	}

	public boolean allowIncompleteRecordingFile() {
		return getPreferenceStore().getBoolean(PreferenceKeys.PROPERTY_ALLOW_INCOMPLETE_RECORDING_FILE);
	}

	public void setPageStructureLocked(boolean lock) {
		getPreferences().putBoolean(PAGE_STRUCTURE_LOCK_ID, lock);
	}

	public boolean isPageStructureLocked() {
		return getPreferences().getBoolean(PAGE_STRUCTURE_LOCK_ID, false);
	}

	public File getTempRecordingsDir() throws IOException {
		// TODO: Make folder configurable in preferences
		File dir = getStateLocation().append(TEMP_RECORDINGS_FOLDER).toFile();
		if (!dir.isDirectory() && !dir.mkdirs()) {
			throw new IOException(NLS.bind(Messages.FOLDER_COULD_NOT_BE_CREATED, dir.getAbsolutePath()));
		}
		return dir;
	}

	public PageManager getPageManager() {
		if (pageManager == null) {
			IWorkbench workbench = getWorkbench();
			Runnable callback = () -> DisplayToolkit.safeAsyncExec(() -> refreshJfrEditors(workbench));
			pageManager = new PageManager(getPreferences().get(PAGE_MANAGER_ID, null), callback);
		}
		return pageManager;
	}

	private static void refreshJfrEditors(IWorkbench workbench) {
		for (IWorkbenchWindow ww : workbench.getWorkbenchWindows()) {
			for (IWorkbenchPage wp : ww.getPages()) {
				IEditorPart editor;
				// FIXME: Do this lazily for non active editors. Make sure that also editors that could not be fetched from the editor reference are refreshed.
				for (IEditorReference er : wp.getEditorReferences()) {
					if ((editor = er.getEditor(false)) instanceof JfrEditor) {
						((JfrEditor) editor).refreshPages();
					}
				}
			}
		}
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static FlightRecorderUI getDefault() {
		return plugin;
	}

}
