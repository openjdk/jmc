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
package org.openjdk.jmc.alert;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.ui.MCAbstractUIPlugin;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class AlertPlugin extends MCAbstractUIPlugin {
	public final static String PLUGIN_ID = "org.openjdk.jmc.alert"; //$NON-NLS-1$

	public static final String IMAGE_ALERT_BANNER = "AlertBanner"; //$NON-NLS-1$
	public static final String PREF_KEY_POPUP = "POPUP"; //$NON-NLS-1$

	private static final int MAX_ALERT_SIZE = 1000;

	// The shared instance.
	private static AlertPlugin plugin;
	private AlertDialog dialog;
	private final ArrayList<AlertObject> alerts = new ArrayList<>();

	/**
	 * The constructor.
	 */
	public AlertPlugin() {
		super(PLUGIN_ID);
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		getPreferenceStore().setDefault(PREF_KEY_POPUP, true);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 */
	public static AlertPlugin getDefault() {
		return plugin;
	}

	public synchronized void addNotificationEvent(TriggerEvent e) {
		addAlertObject(new AlertObject(e.getCreationTime(), e.getSource().getServerDescriptor().getDisplayName(),
				e.getRule(), NotificationUIToolkit.prettyPrint(e), null));
	}

	public synchronized void addAlertObject(AlertObject ao) {
		if (alerts.size() >= MAX_ALERT_SIZE) {
			alerts.remove(0);
		}
		alerts.add(ao);
		showDialog(ao.getException() != null);
		showTrayPopup(ao);
	}

	private void showTrayPopup(AlertObject ao) {
		if (UIPlugin.getDefault().getTrayManager() != null) {
			final String message = createTrayMessage(ao);
			final String title = Messages.AlertPlugin_TRIGGER_ALERT_TEXT;
			final int style = SWT.BALLOON | SWT.ICON_WARNING;

			DisplayToolkit.safeAsyncExec(getDefault().getWorkbench().getDisplay(), new Runnable() {
				@Override
				public void run() {
					UIPlugin.getDefault().getTrayManager().showTooltip(title, message, style);
				}
			});
		}
	}

	// Special formatting for tray for non-exception messages.
	public String createTrayMessage(AlertObject ae) {
		if (ae.getException() == null) {
			return createRuleMessage(ae);
		} else {
			return ae.getMessage();
		}
	}

	private String createExceptionMessage(Date d, Throwable exception, TriggerRule rule) {
		StringBuilder builder = new StringBuilder();
		if (d != null) {
			DateFormat df1 = DateFormat.getDateInstance(DateFormat.SHORT);
			DateFormat df2 = DateFormat.getTimeInstance(DateFormat.MEDIUM);
			builder.append(MessageFormat.format(Messages.AlertPlugin_TIME_X_Y_TEXT,
					new Object[] {df1.format(d), df2.format(d)}));
		}
		builder.append(NLS.bind(Messages.AlertPlugin_MESSAGE_EXCEPTION_INVOKING_ACTION, rule.getName()));
		builder.append(NLS.bind(Messages.AlertPlugin_MESSAGE_EXCEPTION_INVOKING_ACTION_MESSAGE_CAPTION,
				exception.getLocalizedMessage()));
		builder.append(Messages.AlertPlugin_MESSAGE_EXCEPTION_INVOKING_ACTION_MESSAGE_MORE_INFORMATION);
		return builder.toString();
	}

	private String createRuleMessage(AlertObject ae) {
		Date d = ae.getCreationTime();

		String message = MessageFormat.format(Messages.AlertPlugin_RULE_X_Y_TEXT,
				new Object[] {ae.getRule().getRulePath(), ae.getRule().getName()});

		if (d != null) {
			DateFormat df1 = DateFormat.getDateInstance(DateFormat.SHORT);
			DateFormat df2 = DateFormat.getTimeInstance(DateFormat.MEDIUM);
			message += MessageFormat.format(Messages.AlertPlugin_TIME_X_Y_TEXT,
					new Object[] {df1.format(d), df2.format(d)});
		}
		message += MessageFormat.format(Messages.AlertPlugin_SOURCE_X_TEXT, new Object[] {ae.getSourceName()});
		return message;
	}

	public void setPopup(boolean popup) {
		getPreferenceStore().setValue(PREF_KEY_POPUP, popup);
	}

	public boolean getPopup() {
		return getPreferenceStore().getBoolean(PREF_KEY_POPUP);
	}

	public void showDialog(boolean alwaysShow) {
		if (getPopup() || alwaysShow || hasDialog()) {
			DisplayToolkit.safeAsyncExec(getDefault().getWorkbench().getDisplay(), new Runnable() {
				@Override
				public void run() {
					Display display = getDefault().getWorkbench().getDisplay();
					Shell shell = display.getActiveShell();
					if (shell != null && !shell.isDisposed()) {
						if (!hasDialog()) {
							dialog = createDialog(shell);
						}
						if (dialog != null) {
							dialog.open();
							dialog.refreshAlertDialog();
						}
					}
				}
			});
		}
	}

	public boolean hasDialog() {
		if (dialog == null) {
			return false;
		}
		if (dialog.getShell() == null) {
			return false;
		}
		if (dialog.getShell().isDisposed()) {
			return false;
		}
		return true;
	}

	public static AlertDialog createDialog(Shell shell) {
		Display display = getDefault().getWorkbench().getDisplay();
		if (display != null && !display.isDisposed() && display.getActiveShell() != null
				&& !display.getActiveShell().isDisposed()) {
			return new AlertDialog(display.getActiveShell());
		} else {
			return null;
		}
	}

	public synchronized void clearNotificationEventLog() {
		alerts.clear();
	}

	public synchronized AlertObject[] getAlerts() {
		return alerts.toArray(new AlertObject[alerts.size()]);
	}

	@Override
	public Image getImage(String image) {
		return getImageRegistry().get(image);
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		reg.put(IMAGE_ALERT_BANNER, getImageDescriptor("icons/trigger-alerts-wiz.gif").createImage()); //$NON-NLS-1$
	}

	public synchronized void addException(IConnectionHandle connectionHandle, TriggerRule rule, Throwable throwable) {
		// FIXME: JMC-4270 - Server time approximation is not reliable
//		IMBeanHelperService mhs = connectionHandle.getServiceOrNull(IMBeanHelperService.class);
//		long timestamp = 0;
//		if (mhs != null) {
//			timestamp = mhs.getApproximateServerTime(System.currentTimeMillis());
//		} else {
//			timestamp = System.currentTimeMillis();
//		}
//		Date creationDate = new Date(timestamp);
		Date creationDate = new Date();
		addAlertObject(new AlertObject(creationDate, connectionHandle.getServerDescriptor().getDisplayName(), rule,
				createExceptionMessage(creationDate, throwable, rule), throwable));
	}
}
