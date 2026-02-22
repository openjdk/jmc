/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.console.jconsole.tabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.openjdk.jmc.console.jconsole.Activator;
import org.openjdk.jmc.console.jconsole.JConsolePluginLoader;
import org.openjdk.jmc.console.jconsole.MissionControlContext;
import org.openjdk.jmc.rjmx.common.IConnectionHandle;

import com.sun.tools.jconsole.JConsolePlugin;

/**
 * Will contain all the JConsole tabs.
 * <p>
 * Note that this class contains non-serializable fields. Even though its superclasses implement
 * Serializable, this class is not intended to be serialized.
 */
public class JConsolePluginTabbedPane extends JTabbedPane {
	private static final long serialVersionUID = 1L;
	private final List<JConsolePlugin> plugins = new ArrayList<>();
	private final Map<JConsolePlugin, SwingWorker<?, ?>> swingWorkers = new HashMap<>();
	private volatile boolean disposeTimerTask;
	private final MissionControlContext ctx;
	private static final String STACKTRACE_VIEW_ID = "org.openjdk.jmc.flightrecorder.ui.StacktraceView";

	public JConsolePluginTabbedPane(IConnectionHandle connectionHandle) {
		// FIXME: Make placement configurable in settings
		super(SwingConstants.TOP);
		ctx = new MissionControlContext(connectionHandle);
		setBackground(Color.WHITE);
		setForeground(Color.BLACK);
		try {
			File pluginDir = Activator.getJConsolePluginDir();

			addFromExtensionPoints();
			addFromPluginsFolder(pluginDir);

			if (plugins.isEmpty()) {
				// Only add error tab if NO plug-ins can be found.
				addAppropriateErrorTab(pluginDir);
				return;
			}
			for (JConsolePlugin plugin : plugins) {
				plugin.setContext(ctx);
				for (Entry<String, JPanel> e : plugin.getTabs().entrySet()) {
					this.add(e.getKey(), e.getValue());
				}
			}
		} catch (Exception e) {
			addErrorTab(Messages.JConsolePluginTabbedPane_TAB_TITLE_COULD_NOT_CREATE_PLUGINS, e);
		}
		startUpdateThread();
	}

	private void addAppropriateErrorTab(File pluginDir) {
		if (pluginDir.toString().equals("")) { //$NON-NLS-1$
			addErrorTab(Messages.JConsolePluginTabbedPane_TAB_TITLE_COULD_NOT_CREATE_PLUGINS,
					Messages.JConsolePluginTabbedPane_ERROR_MESSAGE_MESSAGE_PREFERENCE_NOT_SET);
			return;
		}
		if (!pluginDir.exists() || !pluginDir.isDirectory()) {
			addErrorTab(Messages.JConsolePluginTabbedPane_TAB_TITLE_COULD_NOT_CREATE_PLUGINS,
					NLS.bind(Messages.JConsolePluginTabbedPane_ERROR_MESSAGE_DIRECTORY_DOES_NOT_EXIST, pluginDir));
			return;
		}
		addErrorTab(Messages.JConsolePluginTabbedPane_TAB_TITLE_NO_PLUGINS,
				NLS.bind(Messages.JConsolePluginTabbedPane_ERROR_MESSAGE_COULD_NOT_FIND_ANY_PLUGINS, pluginDir));
	}

	private void addFromExtensionPoints() {
		plugins.addAll(JConsolePluginLoader.getExtensionPlugins());
	}

	public void addFromPluginsFolder(File pluginDir) throws IOException {
		if (pluginDir.exists() && pluginDir.isDirectory()) {
			plugins.addAll(JConsolePluginLoader.getPlugins(Activator.getJConsolePluginDir()));
		} else {
			Activator.getLogger().warning(String.format(
					"Could not find a properly configured JConsole plug-in folder (%s). Please set up the plug-in folder in the preferences.", //$NON-NLS-1$
					pluginDir.toString()));
		}
	}

	private void addErrorTab(String title, String message) {
		JPanel p = new JPanel(new BorderLayout());
		String labelMessage = NLS.bind(Messages.JConsolePluginTabbedPane_ERROR_MESSAGE_COULD_NOT_CREATE_PLUGIN_TAB_HTML,
				message);
		JLabel l = new JLabel(labelMessage);
		l.setBackground(Color.WHITE);
		l.setForeground(Color.BLACK);
		l.setOpaque(true);
		p.add(l, BorderLayout.CENTER);
		Activator.getLogger().log(Level.WARNING,
				NLS.bind(Messages.JConsolePluginTabbedPane_ERROR_MESSAGE_COULD_NOT_CREATE_PLUGIN_TAB, message));
		this.add(title, p);
		setFocusTraversalProperties(p);
		addKeyListener(p);
		addKeyListenerForFwdFocusToSWT(p);
	}

	/**
	 * Adding key listener to transfer focus forward or backward based on 'TAB' or 'Shift + TAB'
	 */
	private void addKeyListener(JComponent comp) {
		comp.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
						e.getComponent().transferFocusBackward();
					} else {
						e.getComponent().transferFocus();
					}
					e.consume();
				}
			}
		});
	}

	/**
	 * Setting the focus traversal properties.
	 */
	private void setFocusTraversalProperties(JComponent comp) {
		comp.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.emptySet());
		comp.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Collections.emptySet());
		comp.setFocusable(true);
		comp.setFocusTraversalKeysEnabled(true);
	}

	/**
	 * Adding key listener and checking if all the swing components are already cycled (Fwd) once.
	 * On completion of swing component cycle transferring focus back to SWT.
	 */
	private void addKeyListenerForFwdFocusToSWT(JComponent comp) {
		comp.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
						e.getComponent().transferFocusBackward();
					} else {
						setFocusBackToSWT();
					}
					e.consume();
				}
			}
		});
	}

	/**
	 * This method sets the focus from swing to SWT. If outline page is active focus will be set to
	 * outline view else to JVM Browser
	 */
	private void setFocusBackToSWT() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IViewPart outlineView = activePage.showView(STACKTRACE_VIEW_ID);
					if (activePage.getActiveEditor() != null) {
						outlineView.setFocus();
					}
				} catch (PartInitException e) {
					Activator.getLogger().log(Level.INFO, "Failed to set focus", e); //$NON-NLS-1$
				}
			}
		});
	}

	private void startUpdateThread() {
		final Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				if (disposeTimerTask) {
					t.cancel();
					ctx.dispose();
					return;
				}
				for (JConsolePlugin p : plugins) {
					SwingWorker<?, ?> sw = p.newSwingWorker();
					SwingWorker<?, ?> prevSW = swingWorkers.get(p);
					// Schedule SwingWorker to run only if the previous SwingWorker has finished its task and it hasn't started.
					if (prevSW == null || prevSW.isDone()) {
						if (sw == null || sw.getState() == SwingWorker.StateValue.PENDING) {
							swingWorkers.put(p, sw);
							if (sw != null) {
								sw.execute();
							}
						}
					}
				}
			}

		}, 1000, 1000);
	}

	private void addErrorTab(String title, Exception e) {
		addErrorTab(title, e.getMessage());
	}

	void dispose() {
		disposeTimerTask = true;
	}
}
