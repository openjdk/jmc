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
package org.openjdk.jmc.rcp.application.scripting.model;

import java.text.ParseException;

import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import org.openjdk.jmc.commands.CommandsPlugin;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

/**
 * Schedules a processes, currently only one process is supported ;)
 * <p>
 * The class makes sure there are no Eclipse jobs running before executing an instruction.
 */
public final class OperatingSystem {

	private static final int TIME_SLICE = 100;

	private final Process m_process;
	private final Display m_display;

	public OperatingSystem(Display display, Process process) {
		m_process = process;
		m_display = display;
	}

	public void scheduleProcess() {
		DisplayToolkit.safeTimerExec(m_display, TIME_SLICE, new Runnable() {
			@Override
			public void run() {
				tryToExecute();
			}
		});
	}

	public boolean execute(Process p) {
		if (p.getProgram().getLineCount() > 0) {
			try {
				CommandsPlugin.getDefault().execute(p.getInstruction().getSource(), p.getStandardOut());
			} catch (ParseException e) {
				p.getErrorOut().println(e.getMessage());
				p.stop();
				return false;
			}

			p.nextInstruction();
			if (!p.hasMoreinstuctions()) {
				p.terminate();
			}
			return true;
		}
		return false;

	}

	public Process getProcessInFocus() {
		return m_process;
	}

	void tryToExecute() {
		if (m_process.isRunning()) {
			IJobManager manager = Job.getJobManager();
			if (manager.isIdle()) {
				execute(m_process);
				if (m_process.isSingleStep()) {
					return;
				}
			}
			scheduleProcess();
		}
	}
}
