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
package org.openjdk.jmc.greychart.ui.messages.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.greychart.ui.messages.internal.messages"; //$NON-NLS-1$

	public static String ChartComposite_DIALOG_RANGE_INPUT_MAXIMUM_TEXT;
	public static String ChartComposite_DIALOG_RANGE_INPUT_MESSAGE_FROM_SMALLER_THAN_TO;
	public static String ChartComposite_DIALOG_RANGE_INPUT_MINIMUM_TEXT;
	public static String ChartComposite_DIALOG_RANGE_INPUT_TEXT;
	public static String ChartComposite_DIALOG_RANGE_INPUT_TITLE;
	public static String ChartComposite_INPUT_GRAPH_TITLE_MESSAGE;
	public static String ChartComposite_INPUT_GRAPH_TITLE_TITLE;
	public static String ChartComposite_INPUT_X_AXIS_TITLE_MESSAGE;
	public static String ChartComposite_INPUT_X_AXIS_TITLE_TITLE;
	public static String ChartComposite_INPUT_Y_AXIS_TITLE_MESSAGE;
	public static String ChartComposite_INPUT_Y_AXIS_TITLE_TITLE;
	public static String ChartComposite_MENU_AUTO_RANGE_TEXT;
	public static String ChartComposite_MENU_AUTO_RANGE_ZERO_TEXT;
	public static String ChartComposite_MENU_CUSTOM_RANGE_TEXT;
	public static String ChartComposite_MENU_EDIT_TITLES_TEXT;
	public static String ChartComposite_MENU_GRAPH_TITLE_TEXT;
	public static String ChartComposite_MENU_LABEL_DENSITY_TEXT;
	public static String ChartComposite_MENU_RENDERING_MODE_AVERAGING_TEXT;
	public static String ChartComposite_MENU_RENDERING_MODE_SUBSAMPLING_TEXT;
	public static String ChartComposite_MENU_RENDERING_MODE_TEXT;
	public static String ChartComposite_MENU_X_AXIS_TITLE_TEXT;
	public static String ChartComposite_MENU_Y_AXIS_RANGE_TEXT;
	public static String ChartComposite_MENU_Y_AXIS_TITLE_TEXT;
	public static String ChartComposite_MENU_ZOOM_IN_TEXT;
	public static String ChartComposite_MENU_ZOOM_OUT_TEXT;
	public static String ChartComposite_SHOW_LAST_10_MINUTES;
	public static String ChartComposite_SHOW_LAST_15_SECONDS;
	public static String ChartComposite_SHOW_LAST_DAY;
	public static String ChartComposite_SHOW_LAST_HOUR;
	public static String ChartComposite_SHOW_LAST_MINUTE;
	public static String ChartComposite_SHOW_LAST_WEEK;
	public static String ChartComposite_SHOW_MENU_TEXT;
	public static String ChartComposite_X_AXIS_TITLE;
	public static String TICK_DENSITY_NAME_DENSE;
	public static String TICK_DENSITY_NAME_NORMAL;
	public static String TICK_DENSITY_NAME_SPARSE;
	public static String TICK_DENSITY_NAME_VARIABLE;
	public static String TICK_DENSITY_NAME_VERY_DENSE;
	public static String TICK_DENSITY_NAME_VERY_SPARSE;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
