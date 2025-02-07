/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.rjmx.triggers.actions.internal;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.openjdk.jmc.rjmx.triggers.fields.internal.Field;
import org.openjdk.jmc.ui.common.CorePlugin;

public class TriggerActionValidator extends Job {

	private final String name;
	private final Field field;

	public TriggerActionValidator(String name, Field field) {
		super(name);
		this.name = name;
		this.field = field;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (this.name.equals("PathValidation")) {
			Path filePath = Paths.get(field.getValue());
			Path root = filePath.getRoot();
			if (root != null) {
				File driveFile = new File(root.toString());
				if (!driveFile.exists()) {
					setDefaultValue();
					return new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID,
							NLS.bind(Messages.TriggerActionValidator_INVALID_PATH, field.getValue()),
							new Exception(NLS.bind(Messages.TriggerActionValidator_INVALID_PATH, field.getValue())));
				}
			}
		} else if (this.name.equals("EmailValidation")) {
			String value = field.getValue();
			if (!value.isEmpty()) {
				String[] split = value.split(",|;|\\s");
				for (String sp : split) {
					String REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
					boolean matches = sp.trim().matches(REGEX);
					if (!matches) {
						setDefaultValue();
						return new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID,
								NLS.bind(Messages.TriggerActionValidator_INVALID_EMAIL, field.getValue()),
								new Exception(
										NLS.bind(Messages.TriggerActionValidator_INVALID_EMAIL, field.getValue())));
					}
				}
			}
		}
		return new Status(IStatus.OK, CorePlugin.PLUGIN_ID, "");
	}

	private void setDefaultValue() {

		switch (field.getId()) {
		case "dumpfilename":
			setDefaultValue("automaticallyTriggeredRecording.jfr");
			break;

		case "recordingfilename":
			setDefaultValue("automaticallyTriggeredRecording.jfr");
			break;

		case "filename":
			setDefaultValue("default.hprof");
			break;

		case "log_file":
			setDefaultValue("command.log");
			break;

		case "logfilename":
			setDefaultValue("log.txt");
			break;

		default:
			setDefaultValue("");
		}
	}

	private void setDefaultValue(String value) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				field.setValue(value);
			}
		});
	}

}
