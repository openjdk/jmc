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
package org.openjdk.jmc.console.jconsole.tabs;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Panel;

import javax.inject.Inject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.IManagedForm;
import org.openjdk.jmc.console.ui.editor.IConsolePageStateHandler;
import org.openjdk.jmc.rjmx.IConnectionHandle;

/**
 * JConsole plug-in plug-in. :)
 * <p>
 * Allows for JConsole plug-ins to be run in the management console.
 */
public class JConsoleTab implements IConsolePageStateHandler {
	private Frame embeddedFrame;
	private JConsolePluginTabbedPane consolePluginTabbedPane;

	@Inject
	protected void createPageContent(IManagedForm managedForm, IConnectionHandle connection) {
		managedForm.getForm().getBody().setLayout(new FillLayout());

		Composite embed = new Composite(managedForm.getForm().getBody(), SWT.EMBEDDED | SWT.NO_BACKGROUND);
		embed.setLayout(new FillLayout());
		embed.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				// NOTE: Workaround to avoid memory leak caused by SWT_AWT.new_Frame which adds the frame to java.awt.Window.allWindows twice, so we remove it once more here
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						try {
							embeddedFrame.removeNotify();
						} catch (Throwable e) {
						}
					}
				});
			}
		});
		embeddedFrame = SWT_AWT.new_Frame(embed);
		embeddedFrame.setLayout(new BorderLayout());
		Panel awtPanel = new Panel(new BorderLayout());
		embeddedFrame.add(awtPanel, BorderLayout.CENTER);
		consolePluginTabbedPane = new JConsolePluginTabbedPane(connection);
		awtPanel.add(consolePluginTabbedPane, BorderLayout.CENTER);
	}

	@Override
	public void dispose() {
		if (consolePluginTabbedPane != null) {
			consolePluginTabbedPane.dispose();
		}
	}

	@Override
	public boolean saveState(IMemento state) {
		return false;
	}
}
