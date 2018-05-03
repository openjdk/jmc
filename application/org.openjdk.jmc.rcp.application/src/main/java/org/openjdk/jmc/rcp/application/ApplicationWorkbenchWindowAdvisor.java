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

import java.lang.reflect.Method;
import java.util.logging.Level;

import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.part.EditorInputTransfer;

import org.openjdk.jmc.rcp.logging.LoggingToolkit;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.misc.TrayManager;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {
	private static final int DEFAULT_WIDTH = 1400;
	private static final int DEFAULT_HEIGHT = 850;
	private static final String SANE_SCREEN_POS_SYSPROP = "mc.test.saneinitialscreenposition"; //$NON-NLS-1$

	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);

		try {
			UIPlugin.getDefault().setTrayManager(new TrayManager(configurer.getWorkbenchConfigurer().getWorkbench()));
		} catch (UnsupportedOperationException e) {
			ApplicationPlugin.getLogger().log(Level.WARNING, "Could not set TrayManager.", e); //$NON-NLS-1$
		}
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	@Override
	public boolean preWindowShellClose() {
		TrayManager manager = UIPlugin.getDefault().getTrayManager();
		if (manager != null) {
			if (manager.getMinimizeToTrayOnClose()) {
				if (manager.minimizeToTray()) {
					return false;
				}
			}
			manager.dispose();
		}
		return true;
	}

	@Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setShowCoolBar(false);
		configurer.setShowStatusLine(true);
		configurer.setShowProgressIndicator(true);
		configurer.setShowPerspectiveBar(false);
		configurer.addEditorAreaTransfer(LocalSelectionTransfer.getTransfer());
		configurer.addEditorAreaTransfer(EditorInputTransfer.getInstance());
		configurer.addEditorAreaTransfer(FileTransfer.getInstance());
		configurer.configureEditorAreaDropListener(new MissionControlEditorDropAdapter(configurer.getWindow()));
		configurer.setInitialSize(new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT));
	}

	@Override
	public void postWindowCreate() {
		super.postWindowCreate();
		try {
			if ("true".equalsIgnoreCase(System.getProperty(SANE_SCREEN_POS_SYSPROP))) { //$NON-NLS-1$
				IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
				Display display = configurer.getWindow().getWorkbench().getDisplay();
				Monitor[] monitors = display.getMonitors();
				Shell jmcShell = configurer.getWindow().getShell();
				Rectangle jmcBounds = jmcShell.getBounds();

				boolean withinSingleMonitorBounds = false;
				for (Monitor monitor : monitors) {
					if (monitor.getClientArea().intersection(jmcBounds).equals(jmcBounds)) {
						withinSingleMonitorBounds = true;
						break;
					}
				}

				// only reposition (and resize) if JMC is out of bounds on all of the available monitors
				if (!withinSingleMonitorBounds) {
					Rectangle primaryDisplayClientArea = display.getPrimaryMonitor().getClientArea();
					if (primaryDisplayClientArea.width <= DEFAULT_WIDTH
							|| primaryDisplayClientArea.height <= DEFAULT_HEIGHT) {
						jmcShell.setBounds(0, 0, primaryDisplayClientArea.width, primaryDisplayClientArea.height);
					} else {
						int x = (primaryDisplayClientArea.width - DEFAULT_WIDTH) / 2;
						int y = (primaryDisplayClientArea.height - DEFAULT_HEIGHT) / 2;
						jmcShell.setBounds(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
					}
				}
			}
		} catch (SecurityException e) {
			// not allowed to access the system property so no action
		}
	}

	@Override
	public void postWindowOpen() {
		removeUnwantedPreferencesPages();
	}

	private void removeUnwantedPreferencesPages() {
		PreferenceManager pm = PlatformUI.getWorkbench().getPreferenceManager();
		IPreferenceNode root = getRoot(pm);
		removePrefsPage(root, "org.eclipse.equinox.security.ui.category"); //$NON-NLS-1$
	}

	// Someone at Eclipse deserves a considerable spanking for not exposing the root...
	private IPreferenceNode getRoot(PreferenceManager pm) {
		try {
			Method m = PreferenceManager.class.getDeclaredMethod("getRoot", (Class[]) null); //$NON-NLS-1$
			m.setAccessible(true);
			return (IPreferenceNode) m.invoke(pm);
		} catch (Exception e) {
			LoggingToolkit.getLogger().log(Level.WARNING,
					"Could not get the root node for the preferences, and will not be able to prune unwanted prefs pages.", //$NON-NLS-1$
					e);
		}
		return null;
	}

	private void removePrefsPage(IPreferenceNode root, String id) {
		for (IPreferenceNode node : root.getSubNodes()) {
			logPrefsNode(node);
			if (node.getId().equals(id)) {
				root.remove(node);
				LoggingToolkit.getLogger().log(Level.INFO,
						String.format("Removed preference page %s (ID:%s)", node.getLabelText(), node.getId())); //$NON-NLS-1$
			} else {
				removePrefsPage(node, id);
			}
		}
	}

	private void logPrefsNode(IPreferenceNode node) {
		LoggingToolkit.getLogger().log(Level.FINE,
				String.format("Prefs node: %s %s", node.getLabelText(), node.getId())); //$NON-NLS-1$
	}
}
