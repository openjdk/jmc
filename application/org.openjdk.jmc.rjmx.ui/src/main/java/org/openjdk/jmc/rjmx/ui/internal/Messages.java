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
package org.openjdk.jmc.rjmx.ui.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.rjmx.ui.internal.messages"; //$NON-NLS-1$

	public static String ADD_ATTIBUTES_ACTION_TEXT;
	public static String ADD_ATTIBUTES_ACTION_TOOLTIP;
	public static String ArrayLengthCellEditor_ENTER_THE_LENGTH_OF_THE_ARRAY;
	public static String AttributeDialSectionPart_CLEAR_STATISTICS_MENU_TEXT;
	public static String AttributeSectionPart_OBJECT_NAME_COLUMN_HEADER;
	public static String AttributeSelectorDialog_LABEL_FILTER_TEXT;
	public static String ChartSectionPart_EDIT_COLOR_TEXT;
	public static String ConfigurePersistenceAction_ENABLE;
	public static String ConfigurePersistenceAction_TEXT;
	public static String ConfigurePersistenceAction_TOOLTIP_TEXT;
	public static String DELETE_COMMAND_TEXT;
	public static String NewVisualizerAction_ADD_CHART_TEXT;
	public static String NewVisualizerAction_MY_CHART_X_TEXT;
	public static String RemoveAction_REMOVE_CONTROL_TEXT;
	public static String StatisticsTable_ATTRIBUTE_DESCRIPTION;
	public static String StatisticsTable_ATTRIBUTE_NAME;
	public static String StatisticsTable_AVERAGE_DESCRIPTION;
	public static String StatisticsTable_AVERAGE_NAME;
	public static String StatisticsTable_MAXIMUM_DESCRIPTION;
	public static String StatisticsTable_MAXIMUM_NAME;
	public static String StatisticsTable_MINIMUM_DESCRIPTION;
	public static String StatisticsTable_MINIMUM_NAME;
	public static String StatisticsTable_SIGMA_DESCRIPTION;
	public static String StatisticsTable_VALUE_DESCRIPTION;
	public static String StatisticsTable_VALUE_NAME;
	public static String ToggleAccessibleControlAction_ACTION_NAME;
	public static String ToggleAccessibleControlAction_TOOLTIP_TEXT;
	public static String UpdatesAction_ACTION_NAME;
	public static String UpdatesAction_TOOLTIP_TEXT;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
