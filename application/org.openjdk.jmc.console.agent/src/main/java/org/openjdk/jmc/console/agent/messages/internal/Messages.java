/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2025, Red Hat Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.console.agent.messages.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.console.agent.messages.internal.messages"; //$NON-NLS-1$
	public static String AgentEditorOpener_JOB_NAME;
	public static String AgentEditorOpener_MESSAGE_COULD_NOT_CONNECT;
	public static String AgentEditorOpener_MESSAGE_STARTING_AGENT_ON_REMOTE_JVM_NOT_SUPPORTED;
	public static String AgentEditorOpener_MESSAGE_START_AGENT_MANUALLY;
	public static String AgentEditorOpener_MESSAGE_FAILED_TO_OPEN_AGENT_EDITOR;
	public static String AgentEditor_CONNECTION_LOST;
	public static String AgentEditor_AGENT_EDITOR_TITLE;
	public static String AgentEditorAction_MESSAGE_REFRESH;
	public static String AgentEditorAction_MESSAGE_LOAD_PRESET;
	public static String AgentEditorAction_MESSAGE_SAVE_AS_PRESET;
	public static String AgentEditorUI_MESSAGE_FAILED_TO_LOAD_PRESET;
	public static String AgentEditorUI_MESSAGE_EMPTY_PRESET;
	public static String AgentEditorUI_MESSAGE_EMPTY_PRESET_TITLE;
	public static String CapturedValue_ERROR_RELATION_KEY_HAS_INCORRECT_SYNTAX;
	public static String CapturedValue_ERROR_CONVERTER_HAS_INCORRECT_SYNTAX;
	public static String Event_ERROR_ID_CANNOT_BE_EMPTY_OR_NULL;
	public static String Event_ERROR_NAME_CANNOT_BE_EMPTY_OR_NULL;
	public static String Event_ERROR_CLASS_CANNOT_BE_EMPTY_OR_NULL;
	public static String Event_ERROR_METHOD_NAME_CANNOT_BE_EMPTY_OR_NULL;
	public static String Event_ERROR_METHOD_DESCRIPTOR_CANNOT_BE_EMPTY_OR_NULL;
	public static String Event_ERROR_METHOD_PARAMETER_CANNOT_BE_NULL;
	public static String Event_ERROR_FIELD_CANNOT_BE_NULL;
	public static String Event_ERROR_CLASS_HAS_INCORRECT_SYNTAX;
	public static String Event_ERROR_PATH_HAS_INCORRECT_SYNTAX;
	public static String Event_ERROR_METHOD_NAME_HAS_INCORRECT_SYNTAX;
	public static String Event_ERROR_METHOD_DESCRIPTOR_HAS_INCORRECT_SYNTAX;
	public static String Event_ERROR_INDEX_MUST_BE_UNIQUE;
	public static String Field_ERROR_NAME_CANNOT_BE_EMPTY_OR_NULL;
	public static String Field_ERROR_EXPRESSION_CANNOT_BE_EMPTY_OR_NULL;
	public static String Field_ERROR_EXPRESSION_HAS_INCORRECT_SYNTAX;
	public static String MethodParameter_ERROR_INDEX_CANNOT_BE_LESS_THAN_ZERO;
	public static String MethodParameter_ERROR_NAME_CANNOT_BE_EMPTY_OR_NULL;
	public static String Preset_ERROR_FILE_NAME_CANNOT_BE_EMPTY_OR_NULL;
	public static String Preset_ERROR_MUST_HAVE_UNIQUE_ID;
	public static String Preset_ERROR_MUST_HAVE_UNIQUE_EVENT_CLASS_NAME;
	public static String CapturedValueEditingPage_PAGE_NAME;
	public static String CapturedValueEditingPage_MESSAGE_PARAMETER_OR_RETURN_VALUE_EDITING_PAGE_TITLE;
	public static String CapturedValueEditingPage_MESSAGE_PARAMETER_OR_RETURN_VALUE_EDITING_PAGE_DESCRIPTION;
	public static String CapturedValueEditingPage_MESSAGE_FIELD_EDITING_PAGE_TITLE;
	public static String CapturedValueEditingPage_MESSAGE_FIELD_EDITING_PAGE_DESCRIPTION;
	public static String CapturedValueEditingPage_LABEL_NAME;
	public static String CapturedValueEditingPage_LABEL_INDEX;
	public static String CapturedValueEditingPage_LABEL_IS_RETURN_VALUE;
	public static String CapturedValueEditingPage_LABEL_EXPRESSION;
	public static String CapturedValueEditingPage_LABEL_DESCRIPTION;
	public static String CapturedValueEditingPage_LABEL_CONTENT_TYPE;
	public static String CapturedValueEditingPage_LABEL_CLEAR;
	public static String CapturedValueEditingPage_LABEL_RELATIONAL_KEY;
	public static String CapturedValueEditingPage_LABEL_CONVERTER;
	public static String CapturedValueEditingPage_MESSAGE_NAME_OF_THE_CAPTURING;
	public static String CapturedValueEditingPage_MESSAGE_JAVA_PRIMARY_EXPRESSION_TO_BE_EVALUATED;
	public static String CapturedValueEditingPage_MESSAGE_OPTIONAL_DESCRIPTION_OF_THIS_CAPTURING;
	public static String CapturedValueEditingPage_MESSAGE_RELATIONAL_KEY_DESCRIPTION;
	public static String CapturedValueEditingPage_MESSAGE_CONVERTER_DESCRIPTION;
	public static String EventEditingWizardConfigPage_PAGE_NAME;
	public static String EventEditingWizardConfigPage_MESSAGE_EVENT_EDITING_WIZARD_CONFIG_PAGE_TITLE;
	public static String EventEditingWizardConfigPage_MESSAGE_EVENT_EDITING_WIZARD_CONFIG_PAGE_DESCRIPTION;
	public static String EventEditingWizardConfigPage_LABEL_ID;
	public static String EventEditingWizardConfigPage_LABEL_NAME;
	public static String EventEditingWizardConfigPage_LABEL_DESCRIPTION;
	public static String EventEditingWizardConfigPage_LABEL_CLASS;
	public static String EventEditingWizardConfigPage_LABEL_METHOD;
	public static String EventEditingWizardConfigPage_LABEL_PATH;
	public static String EventEditingWizardConfigPage_LABEL_LOCATION;
	public static String EventEditingWizardConfigPage_LABEL_CLEAR;
	public static String EventEditingWizardConfigPage_LABEL_RECORD_EXCEPTIONS;
	public static String EventEditingWizardConfigPage_LABEL_RECORD_STACK_TRACE;
	public static String EventEditingWizardConfigPage_MESSAGE_EVENT_ID;
	public static String EventEditingWizardConfigPage_MESSAGE_NAME_OF_THE_EVENT;
	public static String EventEditingWizardConfigPage_MESSAGE_FULLY_QUALIFIED_CLASS_NAME;
	public static String EventEditingWizardConfigPage_MESSAGE_METHOD_NAME;
	public static String EventEditingWizardConfigPage_MESSAGE_METHOD_DESCRIPTOR;
	public static String EventEditingWizardConfigPage_MESSAGE_OPTIONAL_DESCRIPTION_OF_THIS_EVENT;
	public static String EventEditingWizardConfigPage_MESSAGE_PATH_TO_EVENT;
	public static String EventEditingWizardFieldPage_PAGE_NAME;
	public static String EventEditingWizardFieldPage_MESSAGE_EVENT_EDITING_WIZARD_FIELD_PAGE_TITLE;
	public static String EventEditingWizardFieldPage_MESSAGE_EVENT_EDITING_WIZARD_FIELD_PAGE_DESCRIPTION;
	public static String EventEditingWizardFieldPage_MESSAGE_UNABLE_TO_SAVE_THE_FIELD;
	public static String EventEditingWizardFieldPage_LABEL_NAME;
	public static String EventEditingWizardFieldPage_LABEL_EXPRESSION;
	public static String EventEditingWizardFieldPage_LABEL_DESCRIPTION;
	public static String EventEditingWizardFieldPage_ID_NAME;
	public static String EventEditingWizardFieldPage_ID_EXPRESSION;
	public static String EventEditingWizardFieldPage_ID_DESCRIPTION;
	public static String EventEditingWizardParameterPage_PAGE_NAME;
	public static String EventEditingWizardParameterPage_MESSAGE_EVENT_EDITING_WIZARD_PARAMETER_PAGE_TITLE;
	public static String EventEditingWizardParameterPage_MESSAGE_EVENT_EDITING_WIZARD_PARAMETER_PAGE_DESCRIPTION;
	public static String EventEditingWizardParameterPage_MESSAGE_RETURN_VALUE;
	public static String EventEditingWizardParameterPage_MESSAGE_UNABLE_TO_SAVE_THE_PARAMETER_OR_RETURN_VALUE;
	public static String EventEditingWizardParameterPage_LABEL_INDEX;
	public static String EventEditingWizardParameterPage_LABEL_NAME;
	public static String EventEditingWizardParameterPage_LABEL_DESCRIPTION;
	public static String EventEditingWizardParameterPage_ID_INDEX;
	public static String EventEditingWizardParameterPage_ID_NAME;
	public static String EventEditingWizardParameterPage_ID_DESCRIPTION;
	public static String PresetSelectorWizardPage_PAGE_NAME;
	public static String PresetSelectorWizardPage_MESSAGE_FAILED_TO_SAVE_PRESET;
	public static String PresetSelectorWizardPage_MESSAGE_PAGE_TITLE;
	public static String PresetSelectorWizardPage_MESSAGE_PAGE_DESCRIPTION;
	public static String PresetSelectorWizardPage_ID_PRESET;
	public static String PresetSelectorWizardPage_MESSAGE_EVENTS;
	public static String PresetSelectorWizardPage_ERROR_PAGE_TITLE;
	public static String PresetSelectorWizardPage_SAVE_PRESET_TITLE;
	public static String PresetSelectorWizardPage_SAVE_PRESET_MESSAGE;
	public static String PresetEditingWizardConfigPage_PAGE_NAME;
	public static String PresetEditingWizardConfigPage_MESSAGE_PRESET_EDITING_WIZARD_CONFIG_PAGE_TITLE;
	public static String PresetEditingWizardConfigPage_MESSAGE_PRESET_EDITING_WIZARD_CONFIG_PAGE_DESCRIPTION;
	public static String PresetEditingWizardConfigPage_LABEL_FILE_NAME;
	public static String PresetEditingWizardConfigPage_LABEL_CLASS_PREFIX;
	public static String PresetEditingWizardConfigPage_LABEL_ALLOW_TO_STRING;
	public static String PresetEditingWizardConfigPage_LABEL_ALLOW_CONVERTER;
	public static String PresetEditingWizardConfigPage_MESSAGE_NAME_OF_THE_SAVED_XML;
	public static String PresetEditingWizardConfigPage_MESSAGE_PREFIX_ADDED_TO_GENERATED_EVENT_CLASSES;
	public static String PresetEditingWizardEventPage_PAGE_NAME;
	public static String PresetEditingWizardEventPage_MESSAGE_PRESET_EDITING_WIZARD_EVENT_PAGE_TITLE;
	public static String PresetEditingWizardEventPage_MESSAGE_PRESET_EDITING_WIZARD_EVENT_PAGE_DESCRIPTION;
	public static String PresetEditingWizardEventPage_MESSAGE_UNABLE_TO_SAVE_THE_PRESET;
	public static String PresetEditingWizardEventPage_LABEL_ID_COLUMN;
	public static String PresetEditingWizardEventPage_LABEL_NAME_COLUMN;
	public static String PresetEditingWizardEventPage_ID_ID_COLUMN;
	public static String PresetEditingWizardEventPage_ID_NAME_COLUMN;
	public static String PresetEditingWizardPreviewPage_PAGE_NAME;
	public static String PresetEditingWizardPreviewPage_MESSAGE_PRESET_EDITING_WIZARD_PREVIEW_PAGE_TITLE;
	public static String PresetEditingWizardPreviewPage_MESSAGE_PRESET_EDITING_WIZARD_PREVIEW_PAGE_DESCRIPTION;
	public static String PresetManagerPage_PAGE_NAME;
	public static String PresetManagerPage_MESSAGE_PRESET_MANAGER_PAGE_TITLE;
	public static String PresetManagerPage_MESSAGE_PRESET_MANAGER_PAGE_DESCRIPTION;
	public static String PresetManagerPage_MESSAGE_PRESET_MANAGER_UNABLE_TO_SAVE_THE_PRESET;
	public static String PresetManagerPage_MESSAGE_PRESET_MANAGER_UNABLE_TO_IMPORT_THE_PRESET;
	public static String PresetManagerPage_MESSAGE_PRESET_MANAGER_UNABLE_TO_EXPORT_THE_PRESET;
	public static String PresetManagerPage_MESSAGE_IMPORT_EXTERNAL_PRESET_FILES;
	public static String PresetManagerPage_MESSAGE_EXPORT_PRESET_TO_A_FILE;
	public static String PresetManagerPage_MESSAGE_EVENTS;
	public static String ProbeCreationPage_MESSAGE_PROBE_CREATION_PAGE_TITLE;
	public static String ProbeCreationPage_MESSAGE_PROBE_CREATION_PAGE_DESCRIPTION;
	public static String ProbeCreationPage_MESSAGE_PROBE_CREATION_SET_NAME_MESSAGE;
	public static String ProbeCreationPage_MESSAGE_PROBE_CREATION_SET_NAME_DESCRIPTION;
	public static String EditorTab_TITLE;
	public static String ActionButtons_LABEL_SAVE_TO_PRESET_BUTTON;
	public static String ActionButtons_LABEL_SAVE_TO_FILE_BUTTON;
	public static String ActionButtons_LABEL_APPLY_PRESET_BUTTON;
	public static String ActionButtons_LABEL_APPLY_LOCAL_CONFIG_BUTTON;
	public static String ActionButtons_ERROR_PAGE_TITLE;
	public static String ActionButtons_MESSAGE_APPLY_LOCAL_CONFIG;
	public static String LiveConfigTab_TITLE;
	public static String OverviewTab_TITLE;
	public static String OverviewTab_MESSAGE_AGENT_LOADED;
	public static String EditAgentSection_MESSAGE_ENTER_PATH;
	public static String EditAgentSection_MESSAGE_AGENT_XML_PATH;
	public static String EditAgentSection_MESSAGE_BROWSE;
	public static String EditAgentSection_MESSAGE_EDIT;
	public static String EditAgentSection_MESSAGE_VALIDATE;
	public static String EditAgentSection_MESSAGE_APPLY;
	public static String EditAgentSection_MESSAGE_NO_WARNINGS_OR_ERRORS_FOUND;
	public static String PresetsTab_TITLE;
	public static String BaseWizardPage_MESSAGE_UNEXPECTED_ERROR_HAS_OCCURRED;
	public static String StartAgentWizard_MESSAGE_FAILED_TO_START_AGENT;
	public static String StartAgentWizard_MESSAGE_FAILED_TO_OPEN_AGENT_EDITOR;
	public static String StartAgentWizard_MESSAGE_UNEXPECTED_ERROR_HAS_OCCURRED;
	public static String StartAgentWizard_MESSAGE_INVALID_AGENT_CONFIG;
	public static String StartAgentWizard_MESSAGE_ACCESS_TO_UNSAFE_REQUIRED;
	public static String StartAgentWizard_WIZARD_FINISH_BUTTON_TEXT;
	public static String StartAgentWizard_MESSAGE_FAILED_TO_LOAD_AGENT;
	public static String StartAgentWizardPage_PAGE_NAME;
	public static String StartAgentWizardPage_MESSAGE_START_AGENT_WIZARD_PAGE_TITLE;
	public static String StartAgentWizardPage_MESSAGE_START_AGENT_WIZARD_PAGE_DESCRIPTION;
	public static String StartAgentWizardPage_MESSAGE_PATH_TO_AN_AGENT_JAR;
	public static String StartAgentWizardPage_MESSAGE_PATH_TO_AN_AGENT_CONFIG;
	public static String StartAgentWizardPage_LABEL_TARGET_JVM;
	public static String StartAgentWizardPage_LABEL_AGENT_JAR;
	public static String StartAgentWizardPage_LABEL_AGENT_XML;
	public static String StartAgentWizardPage_LABEL_BROWSE;
	public static String StartAgentWizardPage_DIALOG_BROWSER_FOR_AGENT_JAR;
	public static String StartAgentWizardPage_DIALOG_BROWSER_FOR_AGENT_CONFIG;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
