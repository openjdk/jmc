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
package org.openjdk.jmc.console.ui.mbeanbrowser.tree;

import java.io.IOException;
import java.util.logging.Level;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.SelectionProviderAction;

import org.openjdk.jmc.console.ui.mbeanbrowser.MBeanBrowserPlugin;
import org.openjdk.jmc.console.ui.mbeanbrowser.messages.internal.Messages;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;
import org.openjdk.jmc.rjmx.ui.internal.RJMXUIConstants;
import org.openjdk.jmc.ui.common.tree.DefaultTreeNode;

/**
 * An action to unregister MBeans from a server. Possible asks the user for confirmation.
 */
public class UnregisterMBeanAction extends SelectionProviderAction {

	private final static String[] SYSTEM_MBEAN_DOMAINS = new String[] {"java.lang", "java.nio", "JMImplementaion", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"java.util.logging", "java.nio", "com.sun.management"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private final MBeanServerConnection mbeanServer;
	private final TreeViewer viewer;

	public UnregisterMBeanAction(MBeanServerConnection server, TreeViewer viewer) {
		super(viewer, Messages.UNREGISTER_MBEAN_ACTION_LABEL);
		mbeanServer = server;
		this.viewer = viewer;
		setDescription(Messages.UNREGISTER_MBEAN_ACTION_DESCRIPTION);
		setEnabled(false);
	}

	@Override
	public void selectionChanged(IStructuredSelection selection) {
		boolean enabled = false;
		for (Object o : getStructuredSelection().toList()) {
			if (o instanceof DefaultTreeNode) {
				if (getObjectName((DefaultTreeNode) o) != null) {
					enabled = true;
					break;
				}
			}
		}
		setEnabled(enabled);
	}

	private ObjectName getObjectName(DefaultTreeNode node) {
		return (ObjectName) node.getAdapter(ObjectName.class);
	}

	@Override
	public void run() {
		for (Object o : getStructuredSelection().toList()) {
			if (o instanceof DefaultTreeNode) {
				ObjectName mbean = getObjectName((DefaultTreeNode) o);
				if (mbean != null && checkUnregisterMBean(mbean)) {
					unregisterMBean(mbean);
					viewer.refresh();
				}
			}
		}
	}

	private void unregisterMBean(ObjectName mbean) {
		try {
			mbeanServer.unregisterMBean(mbean);
		} catch (InstanceNotFoundException e) {
			MBeanBrowserPlugin.getDefault().getLogger().log(Level.WARNING, "Instance to delete not found: " + mbean, e); //$NON-NLS-1$
		} catch (MBeanRegistrationException e) {
			MBeanBrowserPlugin.getDefault().getLogger().log(Level.WARNING,
					"Exception during preDeregister of MBean: " + mbean, e); //$NON-NLS-1$
		} catch (IOException e) {
			MBeanBrowserPlugin.getDefault().getLogger().log(Level.WARNING,
					"Communication problem talking to MBean server: " + mbean, e); //$NON-NLS-1$
		}
	}

	private boolean checkUnregisterMBean(ObjectName mbean) {
		if (isSystemMBean(mbean)) {
			return checkUnregisterSystemMBean(mbean);
		} else {
			return checkUnregisterOtherMBean(mbean);
		}
	}

	private boolean isSystemMBean(ObjectName mbean) {
		String domain = mbean.getDomain();
		if (domain != null) {
			for (String systemDomain : SYSTEM_MBEAN_DOMAINS) {
				if (domain.equals(systemDomain)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkUnregisterSystemMBean(ObjectName mbean) {
		return MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), Messages.UNREGISTER_SYSTEM_MBEAN_TITLE,
				NLS.bind(Messages.UNREGISTER_SYSTEM_MBEAN_LABEL, mbean));
	}

	private boolean checkUnregisterOtherMBean(ObjectName mbean) {
		if (!readUnregisterMBeanSetting()) {
			return true;
		}
		MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(
				Display.getCurrent().getActiveShell(), Messages.UNREGISTER_MBEAN_TITLE,
				NLS.bind(Messages.UNREGISTER_MBEAN_LABEL, mbean), Messages.UNREGISTER_MBEAN_TOGGLE_LABEL, true, null,
				null);
		if (dialog.getReturnCode() == IDialogConstants.OK_ID) {
			storeUnregisterMBeanSetting(dialog.getToggleState());
			return true;
		}
		return false;
	}

	private boolean readUnregisterMBeanSetting() {
		return RJMXUIPlugin.getDefault().getPreferenceStore()
				.getBoolean(RJMXUIConstants.PROPERTY_ASK_USER_BEFORE_MBEAN_UNREGISTER);
	}

	private void storeUnregisterMBeanSetting(boolean removeMBean) {
		RJMXUIPlugin.getDefault().getPreferenceStore()
				.setValue(RJMXUIConstants.PROPERTY_ASK_USER_BEFORE_MBEAN_UNREGISTER, removeMBean);
	}

}
