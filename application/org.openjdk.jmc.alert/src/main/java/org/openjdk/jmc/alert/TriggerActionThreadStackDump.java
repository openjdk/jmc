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
package org.openjdk.jmc.alert;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.IDiagnosticCommandService;
import org.openjdk.jmc.rjmx.triggers.TriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationToolkit;
import org.openjdk.jmc.ui.common.idesupport.IDESupportToolkit;
import org.openjdk.jmc.ui.common.resource.MCFile;

/**
 * This class implements a notification action that extracts a thread dump when it receives a
 * notification and shows the thread dump as an application alert, logs the thread dump, or both.
 */
public class TriggerActionThreadStackDump extends TriggerAction {
	private static final String XML_ELEMENT_LOG_TO_FILE = "log_to_file"; //$NON-NLS-1$
	private static final String XML_ELEMENT_SHOW_APPLICATION_ALERT = "show_application_alert"; //$NON-NLS-1$
	private static final String XML_ELEMENT_APPEND = "append"; //$NON-NLS-1$
	public static final String XML_ELEMENT_LOG_FILE_NAME = "log_filename"; //$NON-NLS-1$

	private TriggerApplicationAlert m_applicationAlertAction;

	/**
	 * @throws Exception
	 * @see org.openjdk.jmc.rjmx.triggers.ITriggerAction#handleNotificationEvent(org.openjdk.jmc.rjmx.triggers.TriggerEvent)
	 */
	@Override
	public void handleNotificationEvent(TriggerEvent e) throws Exception {
		String stackDump = (e.getSource().getServiceOrThrow(IDiagnosticCommandService.class))
				.runCtrlBreakHandlerWithResult("Thread.print"); //$NON-NLS-1$
		TriggerEvent newEvent = new TriggerEvent(e.getSource(), e.getRule(), stackDump, e.wasTriggered());
		if (getShowApplicationAlert()) {
			getApplicationAlertAction().handleNotificationEvent(newEvent);
		}
		String data = NotificationToolkit.prettyPrint(e);
		data += stackDump;
		if (getLogToFile()) {
			MCFile file = getMCFile();
			String jobName = NLS.bind(Messages.TriggerActionThreadStackDump_JOB_TITLE_WRITING_STACK_DUMP,
					file.getPath());
			if (isAppend()) {
				data += "\n\n"; //$NON-NLS-1$
			}
			InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			IDESupportToolkit.writeAsJob(jobName, file, stream, isAppend());
		} else {
			System.out.println(data);
		}
	}

	private boolean isAppend() {
		return getSetting(XML_ELEMENT_APPEND).getBoolean().booleanValue();
	}

	/**
	 * Returns the application alert action that this action uses to show thread dumps in alerts.
	 */
	private TriggerApplicationAlert getApplicationAlertAction() {
		if (m_applicationAlertAction == null) {
			m_applicationAlertAction = new TriggerApplicationAlert();
		}
		return m_applicationAlertAction;
	}

	/**
	 * Returns whether this action should show thread dumps as application alerts.
	 *
	 * @return Whether to show application alerts, as a boolean.
	 */
	public boolean getShowApplicationAlert() {
		return getSetting(XML_ELEMENT_SHOW_APPLICATION_ALERT).getBoolean().booleanValue();
	}

	/**
	 * Returns whether this action will log thread dumps to a file.
	 *
	 * @return true if thread dumps are logged to file.
	 */
	public boolean getLogToFile() {
		return getSetting(XML_ELEMENT_LOG_TO_FILE).getBoolean().booleanValue();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}

	/**
	 * Gets the logFileName.
	 *
	 * @return Returns a String
	 */
	public String getLogFileName() {
		return getSetting(XML_ELEMENT_LOG_FILE_NAME).getFileName();
	}

	private MCFile getMCFile() {
		return IDESupportToolkit.createFileResource(getLogFileName());
	}

	@Override
	public boolean supportsAction(IConnectionHandle handle) {
		return handle.getServiceOrNull(IDiagnosticCommandService.class) != null;
	}
}
