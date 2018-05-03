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
package org.openjdk.jmc.console.ui.notification.wizard;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.console.ui.notification.wizard.messages"; //$NON-NLS-1$

	public static String ActionWizardPage_DESCRIPTION;
	public static String ActionWizardPage_TITLE;
	public static String AttributeConfigurationWizardPage_DESCRIPTION;
	public static String AttributeConfigurationWizardPage_TITLE;
	public static String AttributeSelectionWizardPage_DESCRIPTION;
	public static String AttributeSelectionWizardPage_TITLE;
	public static String ConstraintWizardPage_DESCRIPTION;
	public static String ConstraintWizardPage_TITLE;
	public static String NameWizardPage_CAPTION_RULE_NAME_TEXT;
	public static String NameWizardPage_DEFAULT_TRIGGER_NAME;
	public static String NameWizardPage_DESCRIPTION;
	public static String NameWizardPage_RULE_GROUP_NAME_TEXT;
	public static String NameWizardPage_TITLE;
	public static String NameWizardPage_TRIGGER_DESCRIPTION_TEXT;
	public static String RuleExportWizard_DEFAULT_WIZARD_EXPORT_MESSAGE;
	public static String RuleExportWizard_ERROR_EXPORTING_RULES_TITLE;
	public static String RuleExportWizard_ERROR_EXPORTING_RULES_TO_FILE_X_TEXT;
	public static String RuleExportWizard_RULE_EXPORT_WIZARD_NAME;
	public static String RuleExportWizard_TITLE_EXPORT_TRIGGER_RULES;
	public static String RuleExportWizard_WINDOW_TITLE_EXPORT_TRIGGER_RULES;
	public static String RuleImportWizard_ERROR_IMPORTING_FROM_FILE_X;
	public static String RuleImportWizard_ERROR_IMPORT_DIALOG_TITLE;
	public static String RuleImportWizard_IMPORT_TIGGER_RULES_TITLE;
	public static String RuleImportWizard_RULES_WITH_THESE_NAMES_COULD_NOT_IMPORT;
	public static String RuleImportWizard_WARNING_DIALOG_TITLE;
	public static String RuleImportWizard_WINDOW_TITLE_IMPORT_TRIGGER_RULES;
	public static String RuleImportWizard_WIZARD_MESSAGE_IMPORT_RULES;
	public static String RuleImportWizard_WIZARD_NAME_IMPORT_RULES;
	public static String RuleWizard_DEFAULT_RULE_NAME;
	public static String RuleWizard_WINDOW_TITLE;
	public static String RuleWizard_WINDOW_TITLE_EDIT;
	public static String TriggerConditionWizardPage_DESCRIPTION;
	public static String TriggerConditionWizardPage_TITLE;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
