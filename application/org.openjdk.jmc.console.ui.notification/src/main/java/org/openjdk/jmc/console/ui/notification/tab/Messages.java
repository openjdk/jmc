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
package org.openjdk.jmc.console.ui.notification.tab;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.console.ui.notification.tab.messages"; //$NON-NLS-1$

	public static String RuleDetailsTabSectionPart_ACTION_TAB_TEXT;
	public static String RuleDetailsTabSectionPart_CONDITION_TAB_TEXT;
	public static String RuleDetailsTabSectionPart_CONSTRAINTS_TAB_TEXT;
	public static String RuleDetailsTabSectionPart_SECTION_TEXT;
	public static String TriggerConditionSectionPart_DESCRIPTION_TITLE;
	public static String TriggerConditionSectionPart_ERROR_MESSAGE_COULD_NOT_PARSE_DESCRIPTION;
	public static String TriggerSectionPart_BUTTON_ADD_TEXT;
	public static String TriggerSectionPart_BUTTON_ADD_TOOLTIP;
	public static String TriggerSectionPart_BUTTON_DELETE_TEXT;
	public static String TriggerSectionPart_BUTTON_DELETE_TOOLTIP;
	public static String TriggerSectionPart_BUTTON_RENAME_TEXT;
	public static String TriggerSectionPart_BUTTON_SHOW_ALERTS_TEXT0;
	public static String TriggerSectionPart_CONFIRM_REMOVE_TITLE;
	public static String TriggerSectionPart_CONFIRM_REMOVE_TRIGGER_PLURAL;
	public static String TriggerSectionPart_CONFIRM_REMOVE_TRIGGER_SINGULAR;
	public static String TriggerSectionPart_DEFAULT_RULES_GROUP_NAME_TEXT;
	public static String TriggerSectionPart_DIALOG_RENAME_RULE_MESSAGE_TEXT;
	public static String TriggerSectionPart_DIALOG_RENAME_RULE_TITLE;
	public static String TriggerSectionPart_DIALOG_RULE_EXISTS_MESSAGE_TEXT;
	public static String TriggerSectionPart_ENTER_NEW_GROUP_NAME_TEXT;
	public static String TriggerSectionPart_ERROR_MESSAGE_RESETTING_TRIGGERS;
	public static String TriggerSectionPart_EXPORT_TRIGGER_BUTTON_TEXT_NAME;
	public static String TriggerSectionPart_IMPORT_TRIGGERS_BUTTON_TEXT;
	public static String TriggerSectionPart_RENAME_RULE_GROUP_TITLE;
	public static String TriggerSectionPart_RESET_TITLE_TEXT;
	public static String TriggerSectionPart_RESET_TO_DEFAULT_QUESTION_TEXT;
	public static String TriggerSectionPart_SECTION_DESCRIPTION;
	public static String TriggerSectionPart_SECTION_TEXT;
	public static String TriggerSectionPart_TOOLTIP_RENAME_RULE_TEXT;
	public static String TriggerSectionPart_TRIGGERS_RESET_BUTTON_TEXT;
	public static String TriggerToolkit_ERROR_COULD_NOT_READ_DEFAULT_TEMPLATE_FILE;
	public static String TriggerToolkit_MESSAGE_DEFAULT_TRIGGERS_LOADED;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
