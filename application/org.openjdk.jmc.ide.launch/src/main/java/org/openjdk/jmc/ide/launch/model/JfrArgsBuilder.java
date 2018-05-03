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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards.RecordingWizardModel;

/**
 * Options for starting JFR from commandline. Validates options and creates a commandline.
 * <p>
 * See https://docs.oracle.com/javase/7/docs/technotes/tools/windows/java.html for documentation of
 * options.
 * <p>
 * Supports two cases:
 * <ol>
 * <li>Continuous recording with dumponexit enabled.
 * <p>
 * For JRE pre 8u20:
 * <p>
 * {@code -XX:StartFlightRecording -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true,dumponexitpath=foo.jfr}
 * <p>
 * For JRE 8u20+ (see JDK-8034045):
 * <p>
 * {@code -XX:StartFlightRecording=settings=profile,dumponexit=true,filename=foo.jfr}
 * <li>Timebound recording.
 * <p>
 * {@code -XX:StartFlightRecording=settings=profile,delay=20s,duration=60s,name=MyRecording,filename=foo.jfr}
 * </ol>
 */
public class JfrArgsBuilder {

	private static final String UNLOCKCOMMERCIAL_ARGUMENT = "-XX:+UnlockCommercialFeatures"; //$NON-NLS-1$
	private static final String FLIGHTRECORDER_ARGUMENT = "-XX:+FlightRecorder"; //$NON-NLS-1$
	private static final String STARTFLIGHTRECORDING_ARGUMENT = "-XX:StartFlightRecording="; //$NON-NLS-1$
	private static final String FLIGHTRECORDEROPTIONS_ARGUMENT = "-XX:FlightRecorderOptions="; //$NON-NLS-1$

	private static final String DURATION_ARGUMENT = "duration="; //$NON-NLS-1$
	private static final String DELAY_ARGUMENT = "delay="; //$NON-NLS-1$
	private static final String FILENAME_ARGUMENT = "filename="; //$NON-NLS-1$
	private static final String NAME_ARGUMENT = "name="; //$NON-NLS-1$
	private static final String SETTINGS_ARGUMENT = "settings="; //$NON-NLS-1$
	private static final String DEFAULTRECORDING_ARGUMENT = "defaultrecording="; //$NON-NLS-1$
	private static final String DUMPONEXIT_ARGUMENT = "dumponexit="; //$NON-NLS-1$
	private static final String DUMPONEXITPATH_ARGUMENT = "dumponexitpath="; //$NON-NLS-1$

	private static final String EMPTY = ""; //$NON-NLS-1$
	private static final String COMMA = ","; //$NON-NLS-1$
	private static final String QUOT = "\""; //$NON-NLS-1$
//	private static final String FLAG_START = " -"; //$NON-NLS-1$
	private static final String SPACE = " "; //$NON-NLS-1$
	private static final String WHITESPACE_PATTERN = ".*\\s.*"; //$NON-NLS-1$
	private static final String TRUE = "true"; //$NON-NLS-1$
	private static final String ARG_DELIM_AND_START = " -"; //$NON-NLS-1$
	private static final String ARG_START = "-"; //$NON-NLS-1$

	private final boolean jfrEnabled;
	private final IQuantity duration;
	private final IQuantity delay;
	private final String settings;
	private final String jfrFilename;
	private final String name;
	private final boolean continuous;
	private boolean supportsDumpOnExitWithoutDefaultRecording;

	public JfrArgsBuilder(boolean jfrEnabled, boolean supportsDumpOnExitWithoutDefaultRecording, IQuantity duration,
			IQuantity delay, String settings, String jfrFilename, String name, boolean continuous) {
		this.jfrEnabled = jfrEnabled;
		this.supportsDumpOnExitWithoutDefaultRecording = supportsDumpOnExitWithoutDefaultRecording;
		this.duration = duration;
		this.delay = delay;
		this.settings = settings;
		this.jfrFilename = jfrFilename;
		this.name = name;
		this.continuous = continuous;
	}

	public String[] getJfrArgs(boolean quotWhitespace) throws Exception {
		List<String> jfrArgs = new ArrayList<>();

		if (jfrEnabled) {

			jfrArgs.add(UNLOCKCOMMERCIAL_ARGUMENT);
			jfrArgs.add(FLIGHTRECORDER_ARGUMENT);
			String adaptedFilename = getQuotedFilename(quotWhitespace);

			// ========= -XX:StartFlightRecording =========
			StringBuilder jfrStartArg = new StringBuilder();
			if (delay != null || duration != null || settings != null || name != null || jfrFilename != null
					|| continuous) {
				jfrStartArg.append(STARTFLIGHTRECORDING_ARGUMENT);
			}

			if (continuous) {
				if (supportsDumpOnExitWithoutDefaultRecording) {
					appendSettingsArg(quotWhitespace, jfrStartArg);
					jfrStartArg.append(DUMPONEXIT_ARGUMENT).append(TRUE).append(COMMA);
					appendNameArg(jfrStartArg);
					jfrStartArg.append(FILENAME_ARGUMENT).append(adaptedFilename).append(COMMA);
				} else {
					jfrStartArg.append(DEFAULTRECORDING_ARGUMENT).append(TRUE).append(COMMA);
				}

			} else { // Time bound
				appendSettingsArg(quotWhitespace, jfrStartArg);
				if (duration != null) {
					jfrStartArg.append(DURATION_ARGUMENT).append(getValidTimeRange(duration)).append(COMMA);
				}
				if (delay != null && delay.compareTo(RecordingWizardModel.MIN_USABLE_DELAY) >= 0) {
					jfrStartArg.append(DELAY_ARGUMENT).append(getValidTimeRange(delay)).append(COMMA);
				}
				appendNameArg(jfrStartArg);
				jfrStartArg.append(FILENAME_ARGUMENT).append(adaptedFilename).append(COMMA);
			}

			if (jfrStartArg.toString().endsWith(COMMA)) {
				jfrStartArg = jfrStartArg.deleteCharAt(jfrStartArg.length() - 1);
			}
			if (jfrStartArg.length() > 0) {
				jfrArgs.add(jfrStartArg.toString());
			}

			// ========= -XX:FlightRecorderOptions =========
			StringBuilder jfrOptionsArg = new StringBuilder();
			if (continuous && !supportsDumpOnExitWithoutDefaultRecording) {
				jfrOptionsArg.append(FLIGHTRECORDEROPTIONS_ARGUMENT);
				jfrOptionsArg.append(DUMPONEXIT_ARGUMENT).append(TRUE).append(COMMA);
				jfrOptionsArg.append(DUMPONEXITPATH_ARGUMENT).append(adaptedFilename).append(COMMA);
			}

			if (jfrOptionsArg.toString().endsWith(COMMA)) {
				jfrOptionsArg = jfrOptionsArg.deleteCharAt(jfrOptionsArg.length() - 1);
			}
			if (jfrOptionsArg.length() > 0) {
				jfrArgs.add(jfrOptionsArg.toString());
			}
		}

		return jfrArgs.toArray(new String[jfrArgs.size()]);
	}

	private void appendNameArg(StringBuilder jfrStartArg) {
		if (name != null && name.length() > 0) {
			String nameWithoutSpaces = name.replaceAll(SPACE, EMPTY);
			jfrStartArg.append(NAME_ARGUMENT).append(nameWithoutSpaces).append(COMMA);
		}
	}

	private void appendSettingsArg(boolean quotWhitespace, StringBuilder jfrStartArg) {
		if (settings != null && settings.length() > 0) {
			jfrStartArg.append(SETTINGS_ARGUMENT).append(getQuotedPath(settings, quotWhitespace)).append(COMMA);
		}
	}

	private String getQuotedFilename(boolean quotWhitespace) {
		// FIXME: Add timestamp here?
		return getQuotedPath(jfrFilename, quotWhitespace);
	}

	private String getQuotedPath(String path, boolean quotWhitespace) {
		return path.matches(WHITESPACE_PATTERN) && quotWhitespace ? QUOT + path + QUOT : path;
	}

	private String getValidTimeRange(IQuantity timerange) throws QuantityConversionException {
		// Convert to JFR-compatible time representation, avoiding broken units.
		return CommonConstraints.POSITIVE_TIMESPAN.persistableString(timerange).replace(" ", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static String cleanJfrArgsString(String origArgs) {
		return joinToCommandline(cleanJfrArgs(splitCommandline(origArgs)));
	}

	public static String[] splitCommandline(String origArgs) {
		// NOTE: Not entirely safe to split on ' -', but it's the best we can do.
		String[] origArray = origArgs.split(ARG_DELIM_AND_START);
		for (int i = 1; i < origArray.length; i++) {
			origArray[i] = ARG_START + origArray[i];
		}
		return origArray;
	}

	public static String joinToCommandline(String[] allArgs) {
		final String ARG_DELIM = " "; //$NON-NLS-1$
		return StringToolkit.join(Arrays.asList(allArgs), ARG_DELIM);
	}

	public static String[] cleanJfrArgs(String[] origArray) {
		String[] jfrArgPatterns = new String[] {"-XX:.?UnlockCommercialFeatures", "-XX:StartFlightRecording.*", //$NON-NLS-1$ //$NON-NLS-2$
				"-XX:FlightRecorderOptions.*", "-XX:.?FlightRecorder"}; //$NON-NLS-1$ //$NON-NLS-2$

		List<String> argsList = new ArrayList<>();
		argsList.addAll(Arrays.asList(origArray));
		for (Iterator<String> iterator = argsList.iterator(); iterator.hasNext();) {
			String arg = iterator.next();
			for (int i = 0; i < jfrArgPatterns.length; i++) {
				if (arg.matches(jfrArgPatterns[i])) {
					iterator.remove();
				}
			}
		}

		return argsList.toArray(new String[argsList.size()]);
	}
}
