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

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;

/**
 * Constant key definitions for Latency preferences.
 */
public class PreferenceKeys {
	public static final String PROPERTY_REMOVE_FINISHED_RECORDING = "flightRecorder.removeFinishedRecording"; //$NON-NLS-1$
	public static final String PROPERTY_CONFIRM_REMOVE_TEMPLATE = "flightRecorder.confirmRemoveTemplate"; //$NON-NLS-1$
	public static final String PROPERTY_SHOW_MONITORING_WARNING = "flightrecorder.controlpanel.show.monitoring.warning"; //$NON-NLS-1$
	public static final String PROPERTY_DEFAULT_DUMP_TIMESPAN = "flightrecorder.controlpanel.default.dump.timespan"; //$NON-NLS-1$
	public static final String PROPERTY_DEFAULT_DUMP_TYPE = "flightrecorder.controlpanel.default.dump.type"; //$NON-NLS-1$
	public static final String PROPERTY_ENABLE_RECORDING_ANALYSIS = "flightrecorder.controlpanel.enable.recording.analysis"; //$NON-NLS-1$
	public static final String PROPERTY_INCLUDE_EXPERIMENTAL_EVENTS_AND_FIELDS = "flightrecorder.ui.includeExperimentalEventsAndFields"; //$NON-NLS-1$
	public static final String PROPERTY_ALLOW_INCOMPLETE_RECORDING_FILE = "flightrecorder.ui.allowIncompleteRecordingFile"; //$NON-NLS-1$
	public static final IQuantity DUMP_TIMESPAN_DEFAULT = UnitLookup.MINUTE.quantity(5);
	public static final int NO_DEFAULT_DUMP = 0;
	public static final int DUMP_TIMESPAN = 1;
	public static final int DUMP_WHOLE = 2;
	public static final String PROPERTY_RULES_CONFIGURATION = "flightrecorder.rules.config"; //$NON-NLS-1$
	public static final String PROPERTY_RULES_FILTER = "flightrecorder.rules.filter"; //$NON-NLS-1$

	public static final String PROPERTY_SELECTION_STORE_SIZE = "flightrecorder.selectionstore.size"; //$NON-NLS-1$
	public static final IQuantity DEFAULT_SELECTION_STORE_SIZE = UnitLookup.NUMBER_UNITY.quantity(10);

	public static final String PROPERTY_ITEM_LIST_SIZE = "flightrecorder.itemlist.size"; //$NON-NLS-1$
	public static final IQuantity DEFAULT_ITEM_LIST_SIZE = UnitLookup.NUMBER_UNITY.quantity(10_000);

	public static final String PROPERTY_MAXIMUM_PROPERTIES_ARRAY_STRING_SIZE = "flightrecorder.properties.array.string.size"; //$NON-NLS-1$
	public static final IQuantity DEFAULT_PROPERTIES_ARRAY_STRING_SIZE = UnitLookup.NUMBER_UNITY.quantity(200);

	public static final String PROPERTY_NUM_EDITOR_RULE_EVALUATION_THREADS = "flightrecorder.rule.evaluation.threads"; //$NON-NLS-1$
	public static final IQuantity DEFAULT_NUM_EDITOR_RULE_EVALUATION_THREADS = getDefaultNumberOfEvaluationThreads();

	// FIXME: Used for testing. Perhaps remove in final version.
	public static final String PROPERTY_OVERVIEW_SHOWOK = "flightrecorder.overview.showok"; //$NON-NLS-1$
	public static final boolean PROPERTY_DEFAULT_OVERVIEW_SHOWOK = false;
	public static final String PROPERTY_OVERVIEW_SHOWRESULTDETAILS = "flightrecorder.overview.showresultdetails"; //$NON-NLS-1$
	public static final boolean PROPERTY_DEFAULT_OVERVIEW_SHOWRESULTDETAILS = true;

	private static IQuantity getDefaultNumberOfEvaluationThreads() {
		int threadsHint = Runtime.getRuntime().availableProcessors() / 2;
		return UnitLookup.NUMBER_UNITY.quantity(Math.max(threadsHint, 1));
	}
}
