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

import java.util.MissingResourceException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Storage for common ImageDescriptors.
 */
public class CoreImages {

	/*
	 * NOTE: To copy stuff here from CorePlugin, the following regexps may be used. Find:
	 * String\s+ICON_(\S*)([^;]*);\s+//.* Replace with: ImageDescriptor $1 =
	 * icon(CorePlugin.ICON_$1); That is, until images are removed completely from CorePlugin.
	 */

	// general
	public static final ImageDescriptor HELP = icon(UIPlugin.ICON_HELP);
	public static final ImageDescriptor ERROR = icon(UIPlugin.ICON_ERROR);
	public static final ImageDescriptor ALERT = icon(UIPlugin.ICON_ALERT);
	public static final ImageDescriptor TABLE_SETTINGS = icon(UIPlugin.ICON_TABLE_SETTINGS);
	public static final ImageDescriptor REFRESH = icon(UIPlugin.ICON_REFRESH);
	public static final ImageDescriptor REFRESH_GRAY = icon(UIPlugin.ICON_REFRESH_GRAY);
	public static final ImageDescriptor STACKTRACE_ELEMENT = icon(UIPlugin.ICON_STACKTRACE_ELEMENT);
	public static final ImageDescriptor IMAGE_TABLE_SETTINGS = icon(UIPlugin.IMAGE_TABLE_SETTINGS);
	public static final ImageDescriptor VERTICAL_LAYOUT = icon(UIPlugin.ICON_VERTICAL_LAYOUT);
	public static final ImageDescriptor HORIZONTAL_LAYOUT = icon(UIPlugin.ICON_HORIZONTAL_LAYOUT);
	public static final ImageDescriptor EXPAND_ALL = icon(UIPlugin.ICON_EXPAND_ALL);
	public static final ImageDescriptor COLLAPSE_ALL = icon(UIPlugin.ICON_COLLAPSE_ALL);
	public static final ImageDescriptor EXPAND_GRAYED = icon(UIPlugin.ICON_EXPAND_GRAYED);
	public static final ImageDescriptor CLOCK = icon(UIPlugin.ICON_CLOCK);
	public static final ImageDescriptor PROPERTY_OBJECT = icon(UIPlugin.ICON_PROPERTY_OBJECT);
	public static final ImageDescriptor BINARY = icon(UIPlugin.ICON_BINARY);
	public static final ImageDescriptor DATA = icon(UIPlugin.ICON_DATA);
	public static final ImageDescriptor GARBAGE_BIN = icon(UIPlugin.ICON_GARBAGE_BIN);
	public static final ImageDescriptor ADDRESS = icon(UIPlugin.ICON_ADRESS);
	public static final ImageDescriptor TIMESPAN = icon(UIPlugin.ICON_TIMESPAN);
	public static final ImageDescriptor TREE_MODE = icon(UIPlugin.ICON_TREE_MODE);

	// control
	public static final ImageDescriptor STOP = icon(UIPlugin.ICON_STOP);
	public static final ImageDescriptor PLAY = icon(UIPlugin.ICON_PLAY);
	public static final ImageDescriptor PAUSE = icon(UIPlugin.ICON_PAUSE);

	public static final ImageDescriptor STOP_GREY = icon(UIPlugin.ICON_STOP_GREY);
	public static final ImageDescriptor PLAY_GREY = icon(UIPlugin.ICON_PLAY_GREY);

	// overlay
	public static final ImageDescriptor OVERLAY_ERROR = icon(UIPlugin.ICON_OVERLAY_ERROR);
	public static final ImageDescriptor OVERLAY_RECURSIVE = icon(UIPlugin.ICON_OVERLAY_RECURSIVE);
	public static final ImageDescriptor OVERLAY_STATIC = icon(UIPlugin.ICON_OVERLAY_STATIC);

	// class
	public static final ImageDescriptor CLASS_PRIVATE = icon("innerclass_private_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor CLASS_PROTECTED = icon("innerclass_protected_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor CLASS_PACKAGE = icon("innerclass_default_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor CLASS_PUBLIC = icon(UIPlugin.ICON_CLASS_PUBLIC);
	public static final ImageDescriptor ENUM_PRIVATE = icon("enum_private_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor ENUM_PROTECTED = icon("enum_protected_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor ENUM_PACKAGE = icon("enum_default_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor ENUM_PUBLIC = icon("enum_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor INTERFACE_PRIVATE = icon("innerinterface_private_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor INTERFACE_PROTECTED = icon("innerinterface_protected_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor INTERFACE_PACKAGE = icon("innerinterface_default_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor INTERFACE_PUBLIC = icon("innerinterface_public_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor ANNOTATION_PRIVATE = icon("annotation_private_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor ANNOTATION_PROTECTED = icon("annotation_protected_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor ANNOTATION_PACKAGE = icon("annotation_default_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor ANNOTATION_PUBLIC = icon("annotation_obj.gif"); //$NON-NLS-1$

	// exception
	public static final ImageDescriptor EXCEPTION = icon(UIPlugin.ICON_EXCEPTION);
	public static final ImageDescriptor RUNTIME_EXCEPTION = icon(UIPlugin.ICON_RUNTIME_EXCEPTION);

	// method
	public static final ImageDescriptor METHOD_DEFAULT = icon(UIPlugin.ICON_METHOD_DEFAULT);
	public static final ImageDescriptor METHOD_PRIVATE = icon(UIPlugin.ICON_METHOD_PRIVATE);
	public static final ImageDescriptor METHOD_PUBLIC = icon(UIPlugin.ICON_METHOD_PUBLIC);
	public static final ImageDescriptor METHOD_PROTECTED = icon(UIPlugin.ICON_METHOD_PROTECTED);
	public static final ImageDescriptor METHOD_NON_OPTIMIZED = icon(UIPlugin.ICON_METHOD_NON_OPTIMIZED);
	public static final ImageDescriptor METHOD_OPTIMZED = icon(UIPlugin.ICON_METHOD_OPTIMZED);

	// package
	public static final ImageDescriptor PACKAGE = icon(UIPlugin.ICON_PACKAGE);
	public static final ImageDescriptor LOGICAL_PACKAGE = icon(UIPlugin.ICON_LOGICAL_PACKAGE);

	// module
	public static final ImageDescriptor MODULE = icon(UIPlugin.ICON_MODULE);

	// thread
	public final static ImageDescriptor THREAD = icon(UIPlugin.ICON_THREAD_RUNNING);
	public final static ImageDescriptor THREAD_RUNNING = icon(UIPlugin.ICON_THREAD_RUNNING);
	public final static ImageDescriptor THREAD_SUSPENDED = icon(UIPlugin.ICON_THREAD_SUSPENDED);
	public final static ImageDescriptor THREAD_TERMINATED = icon(UIPlugin.ICON_THREAD_TERMINATED);
	public static final ImageDescriptor THREAD_DEADLOCKED = icon(UIPlugin.ICON_THREAD_DEADLOCKED);
	public static final ImageDescriptor THREAD_DEADLOCKED_GREY = icon(UIPlugin.ICON_THREAD_DEADLOCKED_GREY);
	public static final ImageDescriptor THREAD_NEW = icon(UIPlugin.ICON_THREAD_NEW);
	public static final ImageDescriptor THREAD_TIMEWAITING = icon(UIPlugin.ICON_THREAD_TIMEWAITING);
	public static final ImageDescriptor THREAD_WAITING = icon(UIPlugin.ICON_THREAD_WAITING);
	public static final ImageDescriptor THREAD_BLOCKED = icon(UIPlugin.ICON_THREAD_BLOCKED);
	public static final ImageDescriptor THREAD_GROUP = icon(UIPlugin.ICON_THREAD_GROUP);
	public static final ImageDescriptor THREAD_LOCK = THREAD_BLOCKED;

	public static final ImageDescriptor ZOOM_IN = icon(UIPlugin.ICON_ZOOM_IN);
	public static final ImageDescriptor ZOOM_OUT = icon(UIPlugin.ICON_ZOOM_OUT);
	public static final ImageDescriptor NAV_FORWARD = icon(UIPlugin.ICON_NAV_FORWARD);
	public static final ImageDescriptor NAV_BACKWARD = icon(UIPlugin.ICON_NAV_BACKWARD);
	public static final ImageDescriptor NAV_UP = icon(UIPlugin.ICON_NAV_UP);
	public static final ImageDescriptor NAV_DOWN = icon(UIPlugin.ICON_NAV_DOWN);
	public static final ImageDescriptor ZOOM_ON = icon(UIPlugin.ICON_ZOOM_ON);
	public static final ImageDescriptor ZOOM_OFF = icon(UIPlugin.ICON_ZOOM_OFF);
	public static final ImageDescriptor SELECT_ON = icon(UIPlugin.ICON_SELECT_ON);
	public static final ImageDescriptor SELECT_OFF = icon(UIPlugin.ICON_SELECT_OFF);

	public static final ImageDescriptor FOLDER = icon(UIPlugin.ICON_FOLDER);
	public static final ImageDescriptor FOLDER_CLOSED = icon(UIPlugin.ICON_FOLDER_CLOSED);

	public static final ImageDescriptor COLOR_PALETTE = icon(UIPlugin.ICON_COLOR_PALETTE);

	public static final ImageDescriptor DIAL_PANEL = icon(UIPlugin.ICON_DIAL_PANEL);
	public static final ImageDescriptor DIAL_BACKGROUND = icon(UIPlugin.ICON_DIAL_BACKGROUND);
	// public static final ImageDescriptor DIAL_PANEL2 = icon(CorePlugin.ICON_DIAL_PANEL2);

	public static final ImageDescriptor DIAL_PANEL_1_10 = icon(UIPlugin.ICON_DIAL_PANEL_1_10);
	public static final ImageDescriptor DIAL_PANEL_10_100 = icon(UIPlugin.ICON_DIAL_PANEL_10_100);
	public static final ImageDescriptor DIAL_PANEL_100_1000 = icon(UIPlugin.ICON_DIAL_PANEL_100_1000);
	public final static ImageDescriptor DELETE = icon(UIPlugin.ICON_DELETE);

	public final static ImageDescriptor MISSION_CONTROL = icon(UIPlugin.ICON_MISSION_CONTROL);

	public final static ImageDescriptor AMPERSAND = icon(UIPlugin.ICON_AMPERSAND);

	public final static ImageDescriptor FIND = icon(UIPlugin.ICON_FIND);

	/**
	 * Not to be instantiated.
	 */
	private CoreImages() {
	}

	private static ImageDescriptor icon(String filename) {
		return createDescriptor("$nl$/icons/" + filename); //$NON-NLS-1$
	}

	private static ImageDescriptor createDescriptor(String relPath) {
		ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(UIPlugin.PLUGIN_ID, relPath);
		if (desc == null) {
			// FIXME: Throwing an exception has the development time advantage of being very intrusive. For release time, logging might be better.
			throw new MissingResourceException("Missing image '" + relPath + '\'', ImageDescriptor.class.getName(), //$NON-NLS-1$
					UIPlugin.PLUGIN_ID + '/' + relPath);
		}
		return desc;
	}
}
