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
package org.openjdk.jmc.console.ui.actions;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.ObjectName;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;

import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.subscription.IMBeanServerChangeListener;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.misc.IRefreshable;

/**
 * Action for turning view updates on/off on MBean creation/destruction events.
 */
public final class MBeanAutomaticRefreshAction extends Action implements IMBeanServerChangeListener {
	final private IRefreshable m_refreshable;
	final private AtomicBoolean m_enabled = new AtomicBoolean(true);

	public MBeanAutomaticRefreshAction(IRefreshable refreshable) {
		super("", IAction.AS_CHECK_BOX); //$NON-NLS-1$
		setText(Messages.MBeanAutomaticRefreshAction_MBEAN_STRUCTURA_REFRESH_ACTION_TEXT);
		setToolTipText(Messages.MBeanAutomaticRefreshAction_MBEAN_STRUCTURA_REFRESH_ACTION_TOOLTIP);
		setChecked(true);
		setImageDescriptor(UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_REFRESH));
		setDisabledImageDescriptor(UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_REFRESH_GRAY));
		setId("refresh"); //$NON-NLS-1$

		m_refreshable = refreshable;
		setChecked(m_enabled.get());
	}

	@Override
	public void run() {
		m_enabled.set(isChecked());
		refresh();
	}

	private void refresh() {
		if (m_enabled.get()) {
			m_refreshable.refresh();
		}
	}

	@Override
	public void mbeanRegistered(ObjectName mbean) {
		refresh();
	}

	@Override
	public void mbeanUnregistered(ObjectName mbean) {
		refresh();
	}

}
