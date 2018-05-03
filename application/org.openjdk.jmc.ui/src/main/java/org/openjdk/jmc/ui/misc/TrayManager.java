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
package org.openjdk.jmc.ui.misc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;

import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.idesupport.Messages;
import org.openjdk.jmc.ui.preferences.PreferenceConstants;

/**
 * Class that manages the tray icon for Mission Control. The class keeps track of the window state,
 * menu resources and preferences. Should be called from the ui-thread.
 */
public class TrayManager {
	final private IWorkbench m_workBench;
	private TrayItem m_trayItem;
	private Menu m_menu;

	/**
	 * Creates a {@link TrayManager}. Call dispose when you're finished using the manager
	 *
	 * @param windowConfigurer
	 *            the {@link IWorkbenchWindowConfigurer} to use.
	 */
	public TrayManager(IWorkbench workbench) {
		m_workBench = workbench;
	}

	/**
	 * Returns the shell for the {@link IWorkbench}
	 *
	 * @return the shell
	 */
	private Shell getWorkbenchShell() {
		return m_workBench.getActiveWorkbenchWindow().getShell();
	}

	/**
	 * Cleans up any resource required by this {@link TrayManager}
	 */
	public void dispose() {
		destroyTip();
		destroyTrayItem();
		destroyMenu();
	}

	/**
	 * Destroy the tray tooltip, if it exists
	 */
	private void destroyTip() {
		if (m_trayItem != null) {
			ToolTip tip = m_trayItem.getToolTip();
			if (tip != null && !tip.isDisposed()) {
				tip.dispose();
			}
		}
	}

	/**
	 * Destroys the tray item associated with this manager, If exist
	 */
	private void destroyTrayItem() {
		if (m_trayItem != null && !m_trayItem.isDisposed()) {
			m_trayItem.dispose();
			m_trayItem = null;
		}
	}

	/**
	 * Destroys the tray menu, if it exist.
	 */
	private void destroyMenu() {
		if (m_menu != null) {
			m_menu.dispose();
			m_menu = null;
		}
	}

	/**
	 * Shows a tooltip message at the tray icon iff the application has been minimized to tray icon.
	 *
	 * @param title
	 *            the title
	 * @param message
	 *            the message
	 * @param style
	 *            the style. See SWT.ICON_INFORMATION, SWT.ICON_ERROR, SWT.ICON_WARNING and
	 *            SWT.BALLOON
	 */
	public void showTooltip(final String title, final String message, final int style) {
		if (m_trayItem != null) {
			createTip(m_trayItem, title, message, style);
		}
	}

	/**
	 * Minimize the shell to a tray icon.
	 *
	 * @return true if successful
	 */
	public boolean minimizeToTray() {
		Tray tray = getWorkbenchShell().getDisplay().getSystemTray();
		if (tray != null) {
			m_trayItem = createTrayItem(tray);
			hideShell();
			return true;
		}
		return false;
	}

	/**
	 * Returns true if the OS supports a tray, false otherwise
	 *
	 * @return true, if the OS supports a tray
	 */
	public boolean isTraySupported() {
		return getWorkbenchShell().getDisplay().getSystemTray() != null;
	}

	/**
	 * Restores the window to the state it was before a call to {@link #minimizeToTray()}
	 */
	public void maximizeFromTray() {
		showShell();
		destroyTip();
		destroyMenu();
		destroyTrayItem();
	}

	/**
	 * Sets if the application should be minimized to a tray icon when the user closes the
	 * application.
	 *
	 * @param minimizeOnClose
	 *            true, if the shell should be minimized when the user closes the application
	 */
	public void setMinimizeToTrayOnClose(boolean minimizeOnClose) {
		UIPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.P_MINIMIZE_TO_TRAY_ON_CLOSE,
				minimizeOnClose);
	}

	/**
	 * Returns if the application should be minimized to tray icon when the user closes the window.
	 */
	public boolean getMinimizeToTrayOnClose() {
		return UIPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.P_MINIMIZE_TO_TRAY_ON_CLOSE);
	}

	/**
	 * Creates a tray item.
	 *
	 * @param tray
	 */
	private TrayItem createTrayItem(Tray tray) {
		destroyTrayItem();
		TrayItem trayItem = new TrayItem(getWorkbenchShell().getDisplay().getSystemTray(), SWT.NONE);
		trayItem.setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_MISSION_CONTROL));
		trayItem.setToolTipText(Messages.TrayManager_MISSION_CONTROL_TEXT);
		trayItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				maximizeFromTray();
			}

		});
		trayItem.addListener(SWT.MenuDetect, new Listener() {
			@Override
			public void handleEvent(Event event) {
				showPoupMenu();
			}
		});

		return trayItem;
	}

	/**
	 * Shows a popup menu for the tray. This allows the user to open or exit the application.
	 */
	private void showPoupMenu() {
		if (m_menu == null) {
			m_menu = createPopupMenu();
		}
		m_menu.setVisible(true);
	}

	/**
	 * Creates a tooltip.
	 *
	 * @param title
	 *            the title
	 * @param message
	 *            the message
	 * @param style
	 *            the style. See SWT.ICON_INFORMATION, SWT.ICON_ERROR, SWT.ICON_WARNING and
	 *            SWT.BALLOON
	 */
	private void createTip(TrayItem item, String title, String message, int style) {
		destroyTip();

		Shell s = new Shell();
		ToolTip tip = new ToolTip(s, style);
		tip.setMessage(message);
		tip.setText(title);
		tip.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				maximizeFromTray();
			}
		});
		item.setToolTip(tip);
		// need to set visibility after we set tooltip.
		tip.setVisible(true);
	}

	/**
	 * Creates a popup menu
	 *
	 * @return a menu
	 */
	private Menu createPopupMenu() {
		destroyMenu();

		Menu menu = new Menu(getWorkbenchShell(), SWT.POP_UP);

		// Create exit menu item
		MenuItem exit = new MenuItem(menu, SWT.PUSH);
		exit.setText(Messages.TrayManager_EXIT_MENU_ACTION_TEXT);
		exit.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				destroyTrayItem();
				destroyMenu();
				getWorkbench().close();
			}
		});

		// Creates the open menu item
		MenuItem open = new MenuItem(menu, SWT.PUSH);
		open.setText(Messages.TrayManager_OPEN_MENU_ITEM_TEXT);
		open.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				destroyMenu();
				maximizeFromTray();
			}
		});

		return menu;
	}

	/**
	 * Returns the Workbench associated with this {@link TrayManager}
	 *
	 * @return
	 */
	private IWorkbench getWorkbench() {
		return m_workBench;
	}

	/**
	 * Shows the shell
	 */
	private void showShell() {
		getWorkbenchShell().setVisible(true);
		getWorkbenchShell().setActive();
		getWorkbenchShell().setFocus();
		getWorkbenchShell().setMinimized(false);
	}

	/**
	 * Hides the shell
	 */
	private void hideShell() {
		getWorkbenchShell().setVisible(false);
	}
}
