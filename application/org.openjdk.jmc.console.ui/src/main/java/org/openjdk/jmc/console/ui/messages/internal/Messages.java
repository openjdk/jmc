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
package org.openjdk.jmc.console.ui.messages.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.console.ui.messages.internal.messages"; //$NON-NLS-1$

	public static String ALLOCATED_MEMORY_DESCRIPTION_TEXT;
	public static String ALLOCATED_MEMORY_NAME_TEXT;
	public static String APPLICATION_ARGUMENTS_LABEL;
	public static String AllThreadsContentProvider_CPU_COUNT_NOT_SUPPORTED_TEXT;
	public static String BLOCKED_COUNT_DESCRIPTION_TEXT;
	public static String BLOCKED_COUNT_NAME_TEXT;
	public static String BLOCKED_TIME_DESCRIPTION_TEXT;
	public static String BLOCKED_TIME_NAME_TEXT;
	public static String BOOT_CLASS_PATH_LABEL;
	public static String CLASS_PATH_LABEL;
	public static String COLUMN_KEY_TEXT;
	public static String COLUMN_VALUE_TEXT;
	public static String CONNECTION_INFORMATION_LABEL;
	public static String CONNECTION_INFORMATION_VALUE;
	public static String CPU_USAGE_DESCRIPTION_TEXT;
	public static String CPU_USAGE_NAME_TEXT;
	public static String CommunicationPage_CAPTION_DEFAULT_UPDATE_INTERVAL;
	public static String CommunicationPage_CAPTION_MAIL_SERVER;
	public static String CommunicationPage_CAPTION_MAIL_SERVER_PASSWORD;
	public static String CommunicationPage_CAPTION_MAIL_SERVER_PORT;
	public static String CommunicationPage_CAPTION_MAIL_SERVER_USER;
	public static String CommunicationPage_CAPTION_RETAINED_EVENT_VALUES;
	public static String CommunicationPage_CAPTION_SECURE_MAIL_SERVER;
	public static String CommunicationPage_DESCRIPTION;
	public static String CommunicationPage_UPDATE_INTERVAL_THREAD_STACK0;
	public static String ConsoleEditorInput_FAILED_TO_OPEN_EDITOR;
	public static String ConsoleEditor_CONNECTION_LOST;
	public static String ConsoleEditor_COULD_NOT_CONNECT;
	public static String ConsoleEditor_DIAGNOSTIC_COMMANDS_UNAVAILABLE;
	public static String ConsoleEditor_MANAGEMENT_CONSOLE;
	public static String ConsoleEditor_OPENING_MANAGEMENT_CONSOLE;
	public static String ConsoleEditor_PLATFORM_MBEANS_UNAVAILABLE;
	public static String ConsolePlugin_DIALOG_RESET_TO_DEFAULTS_MESSAGE;
	public static String ConsolePlugin_DIALOG_RESET_TO_DEFAULTS_TITLE;
	public static String ConsolePlugin_TOOLTIP_RESET_TO_DEFAULT_CONTROLS_TEXT;
	public static String GcTableSectionPart_GC_TABLE_SECTION_TITLE;
	public static String GeneralPage_DESCRIPTION;
	public static String GeneralPage_LIST_AGGREGATE_SIZE;
	public static String GeneralPage_SHOW_WARNING_BEFORE_UPDATING_HEAP_HISTOGRAM;
	public static String HeapHistogram_CLASS_COLUMN_TEXT;
	public static String HeapHistogram_DELTA_COLUMN_TEXT;
	public static String HeapHistogram_FAILED_TO_REFRESH;
	public static String HeapHistogram_INSTANCES_COLUMN_TEXT;
	public static String HeapHistogram_JVM_PERFORMANCE_WILL_BE_AFFECTED;
	public static String HeapHistogram_REFRESHING_HEAP_HISTOGRAM;
	public static String HeapHistogram_REFRESH_ACTION_TOOLTIP;
	public static String HeapHistogram_RESET_DELTA_ACTION_DESCRIPTION;
	public static String HeapHistogram_RESET_DELTA_ACTION_TEXT;
	public static String HeapHistogram_RESET_DELTA_ACTION_TOOLTOP;
	public static String HeapHistogram_SHOW_WARNING_BEFORE_UPDATING;
	public static String HeapHistogram_SIZE_COLUMN_TEXT;
	public static String HeapHistogram_TITLE;
	public static String HeapHistogram_WARNING_DIALOG_TITLE;
	public static String IS_DEADLOCKED_DESCRIPTION_TEXT;
	public static String IS_DEADLOCKED_NAME_TEXT;
	public static String IS_NATIVE_DESCRIPTION_TEXT;
	public static String IS_NATIVE_NAME_TEXT;
	public static String IS_SUSPENDED_DESCRIPTION_TEXT;
	public static String IS_SUSPENDED_NAME_TEXT;
	public static String LIBRARY_PATH_LABEL;
	public static String LOCK_NAME_DESCRIPTION_TEXT;
	public static String LOCK_NAME_NAME_TEXT;
	public static String LOCK_OWNER_ID_DESCRIPTION_TEXT;
	public static String LOCK_OWNER_ID_NAME_TEXT;
	public static String LOCK_OWNER_NAME_DESCRIPTION_TEXT;
	public static String LOCK_OWNER_NAME_NAME_TEXT;
	public static String MBeanAutomaticRefreshAction_MBEAN_STRUCTURA_REFRESH_ACTION_TEXT;
	public static String MBeanAutomaticRefreshAction_MBEAN_STRUCTURA_REFRESH_ACTION_TOOLTIP;
	public static String MBeanBrowserPage_LABEL_CASE_INSENSITIVE_KEY_COMPARISON_OVERRIDE_TEXT;
	public static String MBeanBrowserPage_LABEL_MBEAN_BROWSER_PREFERENCES_TEXT;
	public static String MBeanBrowserPage_LABEL_PROPERTIES_IN_ALPHABETIC_ORDER_OVERRIDE_TEXT;
	public static String MBeanBrowserPage_LABEL_PROPERTY_ASK_USER_BEFORE_MBEAN_UNREGISTER;
	public static String MBeanBrowserPage_LABEL_PROPERTY_KEY_ORDER_OVERRIDE_TEXT;
	public static String MBeanBrowserPage_LABEL_SHOW_COMPRESSED_PATHS_TEXT;
	public static String MBeanBrowserPage_LABEL_SUFFIX_PROPERTY_KEY_ORDER_OVERRIDE_TEXT;
	public static String MBeanBrowserPage_NOTE_PROPERTIES_TEXT;
	public static String MemoryTab_RUN_GC_ACTION_DESCRIPTION_TEXT;
	public static String MemoryTab_TITLE_COULD_NOT_RUN_GC;
	public static String NOT_ENABLED_TEXT;
	public static String NUMBER_OF_PROCESSORS_LABEL;
	public static String OPERATING_SYSTEM_ARCHITECTURE_LABEL;
	public static String OPERATING_SYSTEM_LABEL;
	public static String OverviewTab_SECTION_DASHBOARD_TEXT;
	public static String POOL_CUR_MAX_NAME_TEXT;
	public static String POOL_CUR_USAGE_NAME_TEXT;
	public static String POOL_CUR_USED_NAME_TEXT;
	public static String POOL_NAME_NAME_TEXT;
	public static String POOL_PEAK_MAX_NAME_TEXT;
	public static String POOL_PEAK_USED_NAME_TEXT;
	public static String POOL_TYPE_NAME_TEXT;
	public static String PROCESS_ID_LABEL;
	public static String PersistencePage_CAPTION_LOG_ROTATION_LIMIT_KB;
	public static String PersistencePage_CAPTION_PERSISTENCE_DIRECTORY;
	public static String PersistencePage_DESCRIPTION;
	public static String PersistencePage_ERROR_DIRECTORY_MUST_EXIST_OR_BE_CREATABLE;
	public static String PoolTableSectionPart_SECTION_TEXT;
	public static String SECTION_SERVER_INFORMATION_TITLE;
	public static String START_TIME_LABEL;
	public static String StackTraceLabelProvider_MESSAGE_PART_LINE_NUMBER_NOT_AVAILABLE;
	public static String StackTraceLabelProvider_MESSAGE_PART_NAME_UNKNOWN_THREAD_NAME;
	public static String StackTraceLabelProvider_MESSAGE_PART_NATIVE_METHOD;
	public static String StackTraceLabelProvider_STACK_TRACE_FORMAT_STRING;
	public static String StackTraceSectionPart_ACTION_REFRESH_STACK_TRACE_TEXT;
	public static String StackTraceSectionPart_SECTION_DESCRIPTION_DATE;
	public static String StackTraceSectionPart_SECTION_TEXT;
	public static String SystemTab_SECTION_SYSTEM_PROPERTIES_TEXT;
	public static String SystemTab_SECTION_SYSTEM_STATISTICS_TEXT;
	public static String TABLE_CATEGORY_DESC;
	public static String TABLE_CATEGORY_LABEL;
	public static String TABLE_VALUE_DESC;
	public static String TABLE_VALUE_LABEL;
	public static String THREAD_ID_DESCRIPTION_TEXT;
	public static String THREAD_ID_NAME_TEXT;
	public static String THREAD_NAME_DESCRIPTION_TEXT;
	public static String THREAD_NAME_NAME_TEXT;
	public static String THREAD_STATE_DESCRIPTION_TEXT;
	public static String THREAD_STATE_NAME_TEXT;
	public static String TOTAL_PHYSICAL_MEMORY_LABEL;
	public static String ThreadTableSectionPart_ENABLE_DEADLOCK_DETECTION_BUTTON_TEXT;
	public static String ThreadTableSectionPart_ENABLE_THREAD_ALLOCATION_BUTTON_TEXT;
	public static String ThreadTableSectionPart_ENABLE_THREAD_CPU_PROFILING_BUTTON_TEXT;
	public static String ThreadTableSectionPart_REFRESH_STACK_TRACE;
	public static String ThreadTableSectionPart_SECTION_DESCRIPTION_DATE;
	public static String ThreadTableSectionPart_SECTION_TEXT;
	public static String ThreadTableSectionPart_USING_FIND_MONITORED_DEADLOCKED_THREADS_HEADER;
	public static String ThreadTableSectionPart_USING_FIND_MONITORED_DEADLOCKED_THREADS_TEXT;
	public static String ThreadsModel_EXCEPTION_NO_DEADLOCK_DETECTION_AVAILABLE_MESSAGE;
	public static String ThreadsModel_EXCEPTION_NO_THREAD_INFO_MESSAGE;
	public static String VM_ARGUMENTS_LABEL;
	public static String VM_VENDOR_LABEL;
	public static String VM_VERSION_LABEL;
	public static String VM_VERSION_VALUE;
	public static String WAITED_COUNT_DESCRIPTION_TEXT;
	public static String WAITED_COUNT_NAME_TEXT;
	public static String WAITED_TIME_DESCRIPTION_TEXT;
	public static String WAITED_TIME_NAME_TEXT;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
