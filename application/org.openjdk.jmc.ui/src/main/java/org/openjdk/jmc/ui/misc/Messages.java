/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.ui.misc;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.ui.misc.messages"; //$NON-NLS-1$

	public static String AbstractWarningItem_WARNING;
	public static String ChartDisplayControlBar_SCALE_TO_FIT_TOOLTIP;
	public static String ChartDisplayControlBar_SELECTION_TOOL_TOOLTIP;
	public static String ChartDisplayControlBar_ZOOM_IN_CLICK_TOOLTIP;
	public static String ChartDisplayControlBar_ZOOM_IN_HOLD_TOOLTIP;
	public static String ChartDisplayControlBar_ZOOM_PAN_TOOLTIP;
	public static String ChartDisplayControlBar_ZOOM_OUT_CLICK_TOOLTIP;
	public static String ChartDisplayControlBar_ZOOM_OUT_HOLD_TOOLTIP;
	public static String ChartLaneHeightControls_LANE_HEIGHT_DECREASE_TOOLTIP;
	public static String ChartLaneHeightControls_LANE_HEIGHT_INCREASE_TOOLTIP;
	public static String ChartLaneHeightControls_OVERVIEW_BUTTON_TOOLTIP;
	public static String DIALOG_FILE_EXISTS_TITLE;
	public static String DIALOG_OVERWRITE_QUESTION_TEXT;
	public static String EXPORT_AS_IMAGE_ACTION_TEXT;
	public static String ExceptionDialog_NO_DETAILS_AVAILABLE;
	public static String FAILED_TO_SAVE_IMAGE;
	public static String FILE_DOES_NOT_EXIST;
	public static String FileSelectorComposite_DIR_DOES_NOT_EXIST;
	public static String FileSelectorComposite_FILE_SELECTOR_BROWSE_BUTTON_TEXT;
	public static String FileSelectorComposite_PLEASE_SPECIFY_FILE;
	public static String IntFieldEditor_ERROR_MESSAGE_PART_INTEGER;
	public static String IntFieldEditor_ERROR_MESSAGE_PART_NATURAL_NUMBER;
	public static String IntFieldEditor_ERROR_MESSAGE_PART_NEGATIVE_INTEGER;
	public static String IntFieldEditor_ERROR_MESSAGE_PART_POSITIVE_INTEGER;
	public static String MOVE_LEFT;
	public static String MOVE_RIGHT;
	public static String NumberFieldEditor_ERROR_MESSAGE_INTERVAL;
	public static String NumberFieldEditor_ERROR_MESSAGE_MUST_BE;
	public static String NumberFieldEditor_ERROR_MESSAGE_NO_GREATER;
	public static String NumberFieldEditor_ERROR_MESSAGE_NO_SMALLER;
	public static String ProgressComposite_PLEASE_WAIT;
	public static String QuestionLinkDialog_FAILED_OPEN_BROWSER;
	public static String TimeFilter_END_TIME_EXCEEDS_ERROR;
	public static String TimeFilter_ERROR;
	public static String TimeFilter_FILTER;
	public static String TimeFilter_FILTER_EVENTS;
	public static String TimeFilter_FROM;
	public static String TimeFilter_INVALID_FORMAT_ERROR;
	public static String TimeFilter_RESET;
	public static String TimeFilter_START_TIME_LONGER_THAN_END_ERROR;
	public static String TimeFilter_START_TIME_PRECEEDS_ERROR;
	public static String TimeFilter_TO;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
