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
package org.openjdk.jmc.flightrecorder.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;

/**
 * Class used to initialize default preference values for the flight recorder plugin.
 */
public class Initializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = FlightRecorderUI.getDefault().getPreferenceStore();
		store.setDefault(PreferenceKeys.PROPERTY_REMOVE_FINISHED_RECORDING, true);
		store.setDefault(PreferenceKeys.PROPERTY_CONFIRM_REMOVE_TEMPLATE, true);
		store.setDefault(PreferenceKeys.PROPERTY_SHOW_MONITORING_WARNING, true);
		store.setDefault(PreferenceKeys.PROPERTY_ENABLE_RECORDING_ANALYSIS, true);
		store.setDefault(PreferenceKeys.PROPERTY_INCLUDE_EXPERIMENTAL_EVENTS_AND_FIELDS, false);
		store.setDefault(PreferenceKeys.PROPERTY_ALLOW_INCOMPLETE_RECORDING_FILE, true);
		store.setDefault(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TIMESPAN,
				PreferenceKeys.DUMP_TIMESPAN_DEFAULT.persistableString());
		store.setDefault(PreferenceKeys.PROPERTY_DEFAULT_DUMP_TYPE, PreferenceKeys.NO_DEFAULT_DUMP);

		store.setDefault(PreferenceKeys.PROPERTY_SELECTION_STORE_SIZE,
				PreferenceKeys.DEFAULT_SELECTION_STORE_SIZE.persistableString());
		store.setDefault(PreferenceKeys.PROPERTY_ITEM_LIST_SIZE,
				PreferenceKeys.DEFAULT_ITEM_LIST_SIZE.persistableString());

		store.setDefault(PreferenceKeys.PROPERTY_MAXIMUM_PROPERTIES_ARRAY_STRING_SIZE,
				PreferenceKeys.DEFAULT_PROPERTIES_ARRAY_STRING_SIZE.persistableString());
		store.setDefault(PreferenceKeys.PROPERTY_NUM_EDITOR_RULE_EVALUATION_THREADS,
				PreferenceKeys.DEFAULT_NUM_EDITOR_RULE_EVALUATION_THREADS.persistableString());

		store.setDefault(PreferenceKeys.PROPERTY_OVERVIEW_SHOWOK, PreferenceKeys.PROPERTY_DEFAULT_OVERVIEW_SHOWOK);
		store.setDefault(PreferenceKeys.PROPERTY_OVERVIEW_SHOWRESULTDETAILS,
				PreferenceKeys.PROPERTY_DEFAULT_OVERVIEW_SHOWRESULTDETAILS);
	}
}
