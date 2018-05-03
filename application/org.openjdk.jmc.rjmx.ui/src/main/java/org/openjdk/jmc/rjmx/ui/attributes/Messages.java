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
package org.openjdk.jmc.rjmx.ui.attributes;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.rjmx.ui.attributes.messages"; //$NON-NLS-1$

	public static String AttributeInspector_DESCRIPTION_COLUMN_HEADER;
	public static String AttributeInspector_NAME_COLUMN_HEADER;
	public static String AttributeInspector_TYPE_COLUMN_HEADER;
	public static String AttributeInspector_VALUE_COLUMN_HEADER;
	public static String AttributeInspector_VALUE_IS_NULL;
	public static String ChangeValueAction_CHANGE_VALUE_ACTION_NAME;
	public static String EditDisplayName_ACTION_TEXT;
	public static String EditDisplayName_DIALOG_MESSAGE;
	public static String EditDisplayName_DIALOG_TITLE;
	public static String MRIAttributeInspector_ATTRIBUTE_NAME_MISSING;
	public static String MRIAttributeInspector_ATTRIBUTE_TYPE_MISSING;
	public static String MRIAttributeInspector_DESCRIPTION_COLUMN_HEADER;
	public static String MRIAttributeInspector_DESCRIPTOR;
	public static String MRIAttributeInspector_DISPLAY_NAME_COLUMN_HEADER;
	public static String MRIAttributeInspector_ERROR_GETTING_VALUE;
	public static String MRIAttributeInspector_ERROR_IN_ATTRIBUTE;
	public static String MRIAttributeInspector_LABEL_NOT_AVAILABLE;
	public static String MRIAttributeInspector_UPDATE_INTERVAL_COLUMN_HEADER;
	public static String MRIAttributeInspector_UPDATE_INTERVAL_DEFAULT;
	public static String MRIAttributeInspector_UPDATE_INTERVAL_ONCE;
	public static String MRIAttribute_ERROR_SETTING_ATTRIBUTE_MSG;
	public static String QuantityInputDialog_DIALOG_MESSAGE;
	public static String QuantityInputDialog_DIALOG_TITLE;
	public static String QuantityInputDialog_LABEL_TEXT;
	public static String ReadOnlyMRIAttribute_PROBLEM_ATTRIBUTE_NOT_FOUND;
	public static String ReadOnlyMRIAttribute_PROBLEM_CONNECTION_CLOSED;
	public static String ReadOnlyMRIAttribute_PROBLEM_EXCEPTION;
	public static String ReadOnlyMRIAttribute_PROBLEM_SERVER;
	public static String ReadOnlyMRIAttribute_PROBLEM_UNMARSHAL;
	public static String ReadOnlyMRIAttribute_STACK_TRACE_IN_LOG;
	public static String SetUnitMenuManager_CUSTOM_UNIT_MENU_ITEM;
	public static String SetUnitMenuManager_CUSTOM_UNIT_MENU_ITEM_MSG;
	public static String SetUnitMenuManager_KIND_OF_QUANTITY_BY_MULTIPLYING_WITH_MSG;
	public static String SetUnitMenuManager_RAW_VALUE_MENU_ITEM;
	public static String SetUnitMenuManager_SET_UNIT_MENU_ITEM;
	public static String UpdateIntervalDialog_DIALOG_MESSAGE;
	public static String UpdateIntervalDialog_DIALOG_TITLE;
	public static String UpdateIntervalManager_CHANGE_UPDATE_INTERVAL_MENU_ITEM;
	public static String UpdateIntervalManager_CUSTOM_UPDATE_INTERVAL_MENU_ITEM;
	public static String VisualizeAction_VISUALIZE_ATTRIBUTE_TEXT;
	public static String VisualizeAction_VISUALIZE_ATTRIBUTE_TRANSFORM_TEXT;
	public static String VisualizeWizardPage_CREATE_CHART_BUTTON_TEXT;
	public static String VisualizeWizardPage_CREATE_CHART_TITLE_TEXT;
	public static String VisualizeWizardPage_DEFAULT_CHART_NAME;
	public static String VisualizeWizardPage_DESCRIPTION_NEW_CHART;
	public static String VisualizeWizardPage_ERROR_TEXT_ONE_CHART_MUST_BE_SELECTED_TEXT;
	public static String VisualizeWizardPage_INVALID_CHART_NAME;
	public static String VisualizeWizardPage_SELECT_CHART_TEXT;
	public static String VisualizeWizardPage_TITLE_NEW_CHART;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
