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
package org.openjdk.jmc.ui;

import java.awt.GraphicsEnvironment;
import java.util.logging.Level;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageRegistry;
import org.openjdk.jmc.ui.common.security.SecurityManagerFactory;
import org.openjdk.jmc.ui.common.util.Environment;
import org.openjdk.jmc.ui.common.util.Environment.OSType;
import org.openjdk.jmc.ui.misc.TrayManager;
import org.openjdk.jmc.ui.preferences.PreferenceConstants;
import org.openjdk.jmc.ui.security.DialogSecurityManager;
import org.osgi.framework.BundleContext;

/**
 * The Core Plug-in class for Mission Control
 */
public class UIPlugin extends MCAbstractUIPlugin {
	public static final String PLUGIN_ID = "org.openjdk.jmc.ui"; //$NON-NLS-1$

	// general
	public static final String ICON_AMPERSAND = "other.gif"; //$NON-NLS-1$
	public static final String ICON_HELP = "help.gif"; //$NON-NLS-1$
	public static final String ICON_ERROR = "error_obj.gif"; //$NON-NLS-1$
	public static final String ICON_ALERT = "alert_obj.png"; //$NON-NLS-1$
	public static final String ICON_TABLE_SETTINGS = "settings_obj.gif"; //$NON-NLS-1$
	public static final String ICON_REFRESH = "refresh_tab.gif"; //$NON-NLS-1$
	public static final String ICON_REFRESH_GRAY = "refresh_tab_grey.gif"; //$NON-NLS-1$
	public static final String ICON_STACKTRACE_ELEMENT = "stckframe_obj.gif"; //$NON-NLS-1$
	public static final String IMAGE_TABLE_SETTINGS = "tablesettings.gif"; //$NON-NLS-1$
	public static final String ICON_VERTICAL_LAYOUT = "th_vertical.gif"; //$NON-NLS-1$
	public static final String ICON_HORIZONTAL_LAYOUT = "th_horizontal.gif"; //$NON-NLS-1$
	public static final String ICON_EXPAND_ALL = "expandall.gif"; //$NON-NLS-1$
	public static final String ICON_COLLAPSE_ALL = "collapseall.gif"; //$NON-NLS-1$
	public static final String ICON_EXPAND_GRAYED = "collapsegrayed.gif"; //$NON-NLS-1$
	public static final String ICON_CLOCK = "clock16.gif"; //$NON-NLS-1$
	public static final String ICON_PROPERTY_OBJECT = "property_obj.gif"; //$NON-NLS-1$
	public static final String ICON_BINARY = "binary_co.gif"; //$NON-NLS-1$
	public static final String ICON_DATA = "data.gif"; //$NON-NLS-1$
	public static final String ICON_GARBAGE_BIN = "trash.png"; //$NON-NLS-1$
	public static final String ICON_ADRESS = "adress.gif"; //$NON-NLS-1$
	public static final String ICON_PASTE = "paste_edit.gif"; //$NON-NLS-1$
	public static final String ICON_CHANGE = "change.gif"; //$NON-NLS-1$
	public static final String ICON_SAVE = "save_edit.gif"; //$NON-NLS-1$
	public static final String ICON_ADD = "add.gif"; //$NON-NLS-1$;
	public static final String ICON_CUT = "cut_edit.gif"; //$NON-NLS-1$
	public static final String ICON_TIMESPAN = "time-span-16.png"; //$NON-NLS-1$
	public static final String ICON_TREE_MODE = "tree_mode.png"; //$NON-NLS-1$
	public static final String ICON_RESET_TO_DEFAULTS = "undo.gif"; //$NON-NLS-1$
	public static final String ICON_LOCK_TREE = "lock_tree.png"; //$NON-NLS-1$
	public static final String ICON_REGEX = "regex.png"; //$NON-NLS-1$;

	// control
	public static final String ICON_STOP = "stop.gif"; //$NON-NLS-1$
	public static final String ICON_PLAY = "play.gif"; //$NON-NLS-1$
	public static final String ICON_PAUSE = "pause-16.png"; //$NON-NLS-1$
	public static final String ICON_STEP_OVER = "stepover.gif"; //$NON-NLS-1$

	public static final String ICON_STOP_GREY = "stopgrey.gif"; //$NON-NLS-1$
	public static final String ICON_PLAY_GREY = "playgrey.gif"; //$NON-NLS-1$
	public static final String ICON_PAUSE_GREY = "pause_grey.gif"; //$NON-NLS-1$
	public static final String ICON_STEP_OVER_GREY = "stepover_grey.gif"; //$NON-NLS-1$

	public static final String ICON_EXPORT = "export.gif"; //$NON-NLS-1$
	public static final String ICON_IMPORT = "import.gif"; //$NON-NLS-1$

	// overlay
	public static final String ICON_OVERLAY_ERROR = "error_co.gif"; //$NON-NLS-1$
	public static final String ICON_OVERLAY_RECURSIVE = "recursive_co.gif"; //$NON-NLS-1$
	public static final String ICON_OVERLAY_STATIC = "static_co.gif"; //$NON-NLS-1$

	// class
	public static final String ICON_CLASS_PUBLIC = "class_obj.gif"; //$NON-NLS-1$

	// exception
	public static final String ICON_EXCEPTION = "jexception_obj.png"; //$NON-NLS-1$
	public static final String ICON_RUNTIME_EXCEPTION = "jexceptiond_obj.png"; //$NON-NLS-1$

	// method
	public static final String ICON_METHOD_DEFAULT = "methdef_obj.gif"; //$NON-NLS-1$
	public static final String ICON_METHOD_PRIVATE = "methpri_obj.gif"; //$NON-NLS-1$
	public static final String ICON_METHOD_PUBLIC = "methpub_obj.gif"; //$NON-NLS-1$
	public static final String ICON_METHOD_PROTECTED = "methpro_obj.gif"; //$NON-NLS-1$
	public static final String ICON_METHOD_NON_OPTIMIZED = "non-optimized-method-16.png"; //$NON-NLS-1$
	public static final String ICON_METHOD_OPTIMZED = "optimized-method-16.png"; //$NON-NLS-1$
	public static final String ICON_METHOD_CALLER = "caller.png"; //$NON-NLS-1$
	public static final String ICON_METHOD_CALLEE = "callee.png"; //$NON-NLS-1$

	// package
	public static final String ICON_PACKAGE = "package_obj.gif"; //$NON-NLS-1$
	public static final String ICON_LOGICAL_PACKAGE = "logical_package_obj.gif"; //$NON-NLS-1$

	// module
	public static final String ICON_MODULE = "jigsaw-32.png"; //$NON-NLS-1$

	// thread
	public final static String ICON_THREAD_RUNNING = "thread_obj.gif"; //$NON-NLS-1$
	public final static String ICON_THREAD_SUSPENDED = "threads_obj.gif"; //$NON-NLS-1$
	public final static String ICON_THREAD_TERMINATED = "threadt_obj.gif"; //$NON-NLS-1$
	public static final String ICON_THREAD_DEADLOCKED = "live-thread-deadlocked-16.PNG"; //$NON-NLS-1$
	public static final String ICON_THREAD_DEADLOCKED_GREY = "live-thread-deadlocked-grey-16.PNG"; //$NON-NLS-1$
	public static final String ICON_THREAD_NEW = "live-thread-new-16.png"; //$NON-NLS-1$
	public static final String ICON_THREAD_TIMEWAITING = "live-thread-timewaiting-16.png"; //$NON-NLS-1$
	public static final String ICON_THREAD_WAITING = "live-thread-waiting-16.png"; //$NON-NLS-1$
	public static final String ICON_THREAD_BLOCKED = "live-thread-locked-16.png"; //$NON-NLS-1$
	public static final String ICON_THREAD_GROUP = "threadgroup.gif"; //$NON-NLS-1$
	public static final String ICON_THREAD_LOCK = ICON_THREAD_BLOCKED;

	public static final String ICON_ZOOM_IN = "zoom-in-16.png"; //$NON-NLS-1$
	public static final String ICON_ZOOM_OUT = "zoom-out-16.png"; //$NON-NLS-1$
	public static final String ICON_SELECT_ALL = "select-all-16.png"; //$NON-NLS-1$
	public static final String ICON_NAV_FORWARD = "forward_nav.gif"; //$NON-NLS-1$
	public static final String ICON_NAV_BACKWARD = "backward_nav.gif"; //$NON-NLS-1$
	public static final String ICON_NAV_DOWN = "down_nav.gif"; //$NON-NLS-1$
	public static final String ICON_NAV_UP = "up_nav.gif"; //$NON-NLS-1$
	public static final String ICON_ZOOM_ON = "zoom-tool-on-16.png"; //$NON-NLS-1$
	public static final String ICON_ZOOM_OFF = "zoom-tool-off-16.png"; //$NON-NLS-1$
	public static final String ICON_SELECT_ON = "selection-tool-on-16.png"; //$NON-NLS-1$
	public static final String ICON_SELECT_OFF = "selection-tool-off-16.png"; //$NON-NLS-1$

	public static final String ICON_FOLDER = "fldr_obj.gif"; //$NON-NLS-1$
	public static final String ICON_FOLDER_CLOSED = "closedFolder.gif"; //$NON-NLS-1$

	public static final String ICON_COLOR_PALETTE = "color-palette.gif"; //$NON-NLS-1$

	public static final String ICON_DIAL_PANEL = "single-dial.png"; //$NON-NLS-1$
	public static final String ICON_DIAL_BACKGROUND = "dial-bkgnd.png"; //$NON-NLS-1$
	public static final String ICON_DIAL_PANEL2 = "dial2.png"; //$NON-NLS-1$TDial

	public static final String ICON_DIAL_PANEL_1_10 = "dial_1_to_10.png"; //$NON-NLS-1$
	public static final String ICON_DIAL_PANEL_10_100 = "dial_10_to_100.png"; //$NON-NLS-1$
	public static final String ICON_DIAL_PANEL_100_1000 = "dial_100_to_1000.png"; //$NON-NLS-1$
	public final static String ICON_DELETE = "delete-16.png"; //$NON-NLS-1$

	public final static String ICON_MISSION_CONTROL = "mission_control.gif"; //$NON-NLS-1$
	public static final String ICON_TOOLS = "external_tools.gif"; //$NON-NLS-1$
	public static final String ICON_TOOLBAR = "toolbar.gif"; //$NON-NLS-1$

	public static final String ICON_LAYOUT = "layout.gif"; //$NON-NLS-1$

	public static final String ICON_SERVICES = "service.gif"; //$NON-NLS-1$

	public static final String ICON_TABGROUP_OTHER = "tabgroup_other.png"; //$NON-NLS-1$

	public static final String ICON_BANNER_PASSWORD_WIZARD = "bannerpasswordwiz.gif"; //$NON-NLS-1$

	public static final String ICON_OVERLAY_WARNING = "overlay_warning.gif"; //$NON-NLS-1$

	public static final String ICON_FIND = "search-glass.png"; //$NON-NLS-1$

	public static enum ImageRegistryPrefixes {
		COLORED_SQUARE, TYPE_IMAGES, NONE // Use NONE prefix to avoid conflict with other prefixes
	}

	// The shared instance.
	private static UIPlugin plugin;
	private TrayManager m_trayManager;

	/**
	 * The constructor.
	 */
	public UIPlugin() {
		super(PLUGIN_ID);
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		initSwingLookAndFeel();
		// FIXME: Move to extension point
		SecurityManagerFactory.setDefaultSecurityManager(new DialogSecurityManager());
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
		Platform.getInstanceLocation().release();
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		registerImage(registry, ICON_CLASS_PUBLIC, ICON_CLASS_PUBLIC);
		registerImage(registry, ICON_AMPERSAND, ICON_AMPERSAND);
		registerImage(registry, ICON_HELP, ICON_HELP);
		registerImage(registry, ICON_EXCEPTION, ICON_EXCEPTION);
		registerImage(registry, ICON_RUNTIME_EXCEPTION, ICON_RUNTIME_EXCEPTION);
		registerImage(registry, ICON_PACKAGE, ICON_PACKAGE);
		registerImage(registry, ICON_LOGICAL_PACKAGE, ICON_LOGICAL_PACKAGE);
		registerImage(registry, ICON_METHOD_PROTECTED, ICON_METHOD_PROTECTED);
		registerImage(registry, ICON_METHOD_PRIVATE, ICON_METHOD_PRIVATE);
		registerImage(registry, ICON_METHOD_PUBLIC, ICON_METHOD_PUBLIC);
		registerImage(registry, ICON_METHOD_DEFAULT, ICON_METHOD_DEFAULT);
		registerImage(registry, ICON_METHOD_CALLEE, ICON_METHOD_CALLEE);
		registerImage(registry, ICON_METHOD_CALLER, ICON_METHOD_CALLER);
		registerImage(registry, ICON_ALERT, ICON_ALERT);
		registerImage(registry, ICON_VERTICAL_LAYOUT, ICON_VERTICAL_LAYOUT);
		registerImage(registry, ICON_HORIZONTAL_LAYOUT, ICON_HORIZONTAL_LAYOUT);
		registerImage(registry, ICON_TABLE_SETTINGS, ICON_TABLE_SETTINGS);
		registerImage(registry, ICON_STACKTRACE_ELEMENT, ICON_STACKTRACE_ELEMENT);
		registerImage(registry, ICON_REFRESH, ICON_REFRESH);
		registerImage(registry, ICON_REFRESH_GRAY, ICON_REFRESH_GRAY);
		registerImage(registry, ICON_METHOD_NON_OPTIMIZED, ICON_METHOD_NON_OPTIMIZED);
		registerImage(registry, ICON_METHOD_OPTIMZED, ICON_METHOD_OPTIMZED);
		registerImage(registry, ICON_DATA, ICON_DATA);
		registerImage(registry, ICON_BINARY, ICON_BINARY);
		registerImage(registry, ICON_GARBAGE_BIN, ICON_GARBAGE_BIN);
		registerImage(registry, ICON_ADRESS, ICON_ADRESS);
		registerImage(registry, ICON_STOP, ICON_STOP);
		registerImage(registry, ICON_PLAY, ICON_PLAY);
		registerImage(registry, ICON_STEP_OVER, ICON_STEP_OVER);

		registerImage(registry, ICON_STOP_GREY, ICON_STOP_GREY);
		registerImage(registry, ICON_PLAY_GREY, ICON_PLAY_GREY);
		registerImage(registry, ICON_STEP_OVER_GREY, ICON_STEP_OVER_GREY);
		registerImage(registry, ICON_PAUSE_GREY, ICON_PAUSE_GREY);

		registerImage(registry, ICON_IMPORT, ICON_IMPORT);
		registerImage(registry, ICON_EXPORT, ICON_EXPORT);

		registerImage(registry, ICON_PASTE, ICON_PASTE);
		registerImage(registry, ICON_SAVE, ICON_SAVE);
		registerImage(registry, ICON_CHANGE, ICON_CHANGE);
		registerImage(registry, ICON_ADD, ICON_ADD);
		registerImage(registry, ICON_CUT, ICON_CUT);
		registerImage(registry, ICON_TIMESPAN, ICON_TIMESPAN);
		registerImage(registry, ICON_REGEX, ICON_REGEX);

		registerImage(registry, ICON_CLOCK, ICON_CLOCK);
		registerImage(registry, ICON_PROPERTY_OBJECT, ICON_PROPERTY_OBJECT);

		registerImage(registry, ICON_EXPAND_ALL, ICON_EXPAND_ALL);
		registerImage(registry, ICON_COLLAPSE_ALL, ICON_COLLAPSE_ALL);

		// Threads
		registerImage(registry, ICON_THREAD_RUNNING, ICON_THREAD_RUNNING);
		registerImage(registry, ICON_THREAD_SUSPENDED, ICON_THREAD_SUSPENDED);
		registerImage(registry, ICON_THREAD_TERMINATED, ICON_THREAD_TERMINATED);
		registerImage(registry, ICON_THREAD_DEADLOCKED, ICON_THREAD_DEADLOCKED);
		registerImage(registry, ICON_THREAD_DEADLOCKED_GREY, ICON_THREAD_DEADLOCKED_GREY);
		registerImage(registry, ICON_THREAD_TIMEWAITING, ICON_THREAD_TIMEWAITING);
		registerImage(registry, ICON_THREAD_WAITING, ICON_THREAD_WAITING);
		registerImage(registry, ICON_THREAD_BLOCKED, ICON_THREAD_BLOCKED);
		registerImage(registry, ICON_THREAD_NEW, ICON_THREAD_NEW);
		registerImage(registry, ICON_THREAD_GROUP, ICON_THREAD_GROUP);

		// overlay
		registerImage(registry, ICON_ERROR, ICON_ERROR);
		registerImage(registry, ICON_OVERLAY_ERROR, ICON_OVERLAY_ERROR);
		registerImage(registry, ICON_OVERLAY_RECURSIVE, ICON_OVERLAY_RECURSIVE);

		registerImage(registry, IMAGE_TABLE_SETTINGS, IMAGE_TABLE_SETTINGS);

		// Panning and zooming
		registerImage(registry, ICON_ZOOM_IN, ICON_ZOOM_IN);
		registerImage(registry, ICON_ZOOM_OUT, ICON_ZOOM_OUT);
		registerImage(registry, ICON_SELECT_ALL, ICON_SELECT_ALL);
		registerImage(registry, ICON_ZOOM_OFF, ICON_ZOOM_OFF);
		registerImage(registry, ICON_ZOOM_ON, ICON_ZOOM_ON);
		registerImage(registry, ICON_SELECT_ON, ICON_SELECT_ON);
		registerImage(registry, ICON_SELECT_OFF, ICON_SELECT_OFF);

		registerImage(registry, ICON_NAV_FORWARD, ICON_NAV_FORWARD);
		registerImage(registry, ICON_NAV_BACKWARD, ICON_NAV_BACKWARD);
		registerImage(registry, ICON_NAV_UP, ICON_NAV_UP);
		registerImage(registry, ICON_NAV_DOWN, ICON_NAV_DOWN);

		registerImage(registry, ICON_DIAL_PANEL, ICON_DIAL_PANEL);
		registerImage(registry, ICON_DIAL_PANEL_1_10, ICON_DIAL_PANEL_1_10);
		registerImage(registry, ICON_DIAL_PANEL_10_100, ICON_DIAL_PANEL_10_100);
		registerImage(registry, ICON_DIAL_PANEL_100_1000, ICON_DIAL_PANEL_100_1000);

		registerImage(registry, ICON_DIAL_BACKGROUND, ICON_DIAL_BACKGROUND);
		registerImage(registry, ICON_FOLDER, ICON_FOLDER);
		registerImage(registry, ICON_FOLDER_CLOSED, ICON_FOLDER_CLOSED);

		registerImage(registry, ICON_COLOR_PALETTE, ICON_COLOR_PALETTE);

		registerImage(registry, ICON_DELETE, ICON_DELETE);
		registerImage(registry, ICON_MISSION_CONTROL, ICON_MISSION_CONTROL);

		registerImage(registry, ICON_TOOLS, ICON_TOOLS);
		registerImage(registry, ICON_TOOLBAR, ICON_TOOLBAR);
		registerImage(registry, ICON_LAYOUT, ICON_LAYOUT);

		registerImage(registry, ICON_SERVICES, ICON_SERVICES);

		registerImage(registry, ICON_TABGROUP_OTHER, ICON_TABGROUP_OTHER);

		registerImage(registry, ICON_BANNER_PASSWORD_WIZARD, ICON_BANNER_PASSWORD_WIZARD);

		registerImage(registry, ICON_RESET_TO_DEFAULTS, ICON_RESET_TO_DEFAULTS);

		registerImage(registry, ICON_LOCK_TREE, ICON_LOCK_TREE);

	}

	/**
	 * Returns the shared instance.
	 */
	public static UIPlugin getDefault() {
		return plugin;
	}

	public boolean getAccessibilityMode() {
		return getPreferenceStore().getBoolean(PreferenceConstants.P_ACCESSIBILITY_MODE);
	}

	/**
	 * @return the tray manager
	 */
	public TrayManager getTrayManager() {
		return m_trayManager;
	}

	/**
	 * @param trayManager
	 */
	public void setTrayManager(TrayManager trayManager) {
		m_trayManager = trayManager;
	}

	/**
	 * Sets the Swing look and feel if needed.
	 */
	private static void initSwingLookAndFeel() {
		// Avoid the possibly broken GTK look and feel on Linux
		String laf = System.getProperty("swing.defaultlaf"); //$NON-NLS-1$
		if (Environment.getOSType() == OSType.LINUX && laf == null && !GraphicsEnvironment.isHeadless()) {
			laf = "javax.swing.plaf.metal.MetalLookAndFeel"; //$NON-NLS-1$
			System.setProperty("swing.defaultlaf", laf); //$NON-NLS-1$
			System.setProperty("swing.systemlaf", laf); //$NON-NLS-1$
			UIPlugin.getDefault().getLogger().log(Level.INFO,
					"On Linux, setting look and feel system properties to " + laf); //$NON-NLS-1$
		}
	}
}
