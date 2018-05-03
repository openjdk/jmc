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
package org.openjdk.jmc.rcp.application;

import java.text.ParseException;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.statushandlers.AbstractStatusHandler;
import org.openjdk.jmc.commands.CommandsPlugin;
import org.openjdk.jmc.ui.idesupport.StandardPerspective;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {
	private final IApplicationContext m_context;
	private final OpenDocumentEventProcessor m_openDocProcessor;
	private AbstractStatusHandler workbenchErrorHandler;
	private boolean shouldExecuteCommands = true;

	ApplicationWorkbenchAdvisor(IApplicationContext context, OpenDocumentEventProcessor openDocProcessor) {
		m_context = context;
		m_openDocProcessor = openDocProcessor;
	}

	@Override
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(configurer);
	}

	@Override
	public void initialize(IWorkbenchConfigurer configurer) {
		configurer.setSaveAndRestore(true);
		initializeHighContrastModeListener();
	}

	private void initializeHighContrastModeListener() {
		Display current = Display.getCurrent();
		current.addListener(SWT.Settings, new HighContrastModeChangeListener(current.getHighContrast()));
	}

	@Override
	public String getInitialWindowPerspectiveId() {
		return StandardPerspective.ID;
	}

	@Override
	public void eventLoopIdle(Display display) {
		/*
		 * Due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=406670 , Eclipse 4.x may call
		 * WorkbenchWindowAdvisor.postWindowOpen before the window actually is shown. This is an
		 * attempt to delay commands until the window is shown.
		 */
		executeCommandsOnce();
		m_openDocProcessor.openFiles();
		super.eventLoopIdle(display);
	}

	private void executeCommandsOnce() {
		if (shouldExecuteCommands) {
			shouldExecuteCommands = false;
			Map<?, ?> arguments = m_context.getArguments();
			String[] appArguments = (String[]) arguments.get(IApplicationContext.APPLICATION_ARGS);
			if (appArguments.length > 0) {
				execute(buildCommandText(appArguments).split(";")); //$NON-NLS-1$
			}
		}
	}

	private void execute(String[] commands) {
		for (String command : commands) {
			try {
				CommandsPlugin.getDefault().execute(command, System.out);
			} catch (ParseException e) {
				ApplicationPlugin.getLogger().log(Level.WARNING, e.getMessage());
			}
		}
	}

	private String buildCommandText(String[] appArguments) {
		StringBuilder builder = new StringBuilder();
		for (int n = 0; n < appArguments.length; n++) {
			String arg = appArguments[n];
			if (arg.startsWith("-")) //$NON-NLS-1$
			{
				if (n != 0) {
					builder.append(';');
				}
				builder.append(arg.substring(1));
			} else {
				// Need to handle arguments with spaces, so enclose in quotes.
				builder.append(" \""); //$NON-NLS-1$
				builder.append(arg);
				builder.append('"');
			}
		}
		return builder.toString();
	}

	@Override
	public synchronized AbstractStatusHandler getWorkbenchErrorHandler() {
		if (workbenchErrorHandler == null) {
			workbenchErrorHandler = new BlockingWorkbenchErrorHandler();
		}
		return workbenchErrorHandler;
	}
}
