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
package org.openjdk.jmc.console.ui.notification;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.BundleContext;

import org.openjdk.jmc.console.ui.notification.tab.TriggerToolkit;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.ui.MCAbstractUIPlugin;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.misc.OverlayImageDescriptor;

/**
 * The main plugin class to be used in the desktop.
 */
public class NotificationPlugin extends MCAbstractUIPlugin {
	public final static String PLUGIN_ID = "org.openjdk.jmc.console.ui.notification"; //$NON-NLS-1$
	public static final String IMG_RULE_WIZRD = "new-trigger-wiz.gif"; //$NON-NLS-1$
	public static final String IMG_ALERT_OBJ = "alert_obj.png"; //$NON-NLS-1$
	public static final String DEFAULT_TRIGGER_FILE = "default_rules.xml"; //$NON-NLS-1$
	public static final String DEFAULT_TRIGGER_FILE_BUNDLE = "org.openjdk.jmc.console.ui.notification.default_rules"; //$NON-NLS-1$

	private static NotificationPlugin s_plugin;
	private LocalResourceManager localResourceManager;

	/**
	 * The constructor.
	 */
	public NotificationPlugin() {
		super(PLUGIN_ID);
		s_plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static NotificationPlugin getDefault() {
		return s_plugin;
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		if (localResourceManager != null) {
			localResourceManager.dispose();
		}
		s_plugin = null;
	}

	public NotificationRegistry getNotificationRepository() {
		return TriggerToolkit.getDefaultModel();
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		registerImage(registry, IMG_ALERT_OBJ, IMG_ALERT_OBJ);
	}

	public Image getImage(Object object, boolean enabled) {
		if (localResourceManager == null) {
			localResourceManager = new LocalResourceManager(JFaceResources.getResources());
		}
		ImageDescriptor image = AdapterUtil.getAdapter(object, ImageDescriptor.class);
		return image == null ? null : (Image) localResourceManager.get(new OverlayImageDescriptor(image, !enabled));
	}
}
