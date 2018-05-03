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
package org.openjdk.jmc.rjmx.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.management.Descriptor;
import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.RequiredModelMBean;

import org.openjdk.jmc.rjmx.RJMXPlugin;

/**
 * This MBean is used to forward notifications from the notification MBean repository. It is also
 * responsible to return proper metadata regarding the available notifications.
 */
public class SyntheticNotificationMBean extends RequiredModelMBean {

	public SyntheticNotificationMBean(SyntheticNotificationEntry[] entries)
			throws RuntimeOperationsException, MBeanException {
		super();
		initializeModelMBeanInfo(entries);
	}

	private void initializeModelMBeanInfo(SyntheticNotificationEntry[] entries) {
		try {
			setModelMBeanInfo(new ModelMBeanInfoSupport((this.getClass().getName()), "Synthetic notification.", null, //$NON-NLS-1$
					null, null, createNotificationInfo(entries)));
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.SEVERE, "Could not setup synthetic notification MBean!", e); //$NON-NLS-1$
		}
	}

	private ModelMBeanNotificationInfo[] createNotificationInfo(SyntheticNotificationEntry[] entries) {
		List<MBeanNotificationInfo> list = new ArrayList<>();
		for (SyntheticNotificationEntry entry : entries) {
			ModelMBeanNotificationInfo notificationInfo = createNotificationInfo(entry);
			Descriptor descriptorCopy = notificationInfo.getDescriptor();
			descriptorCopy.setField(JMX.OPEN_TYPE_FIELD, entry.getNotification().getValueType());
			notificationInfo.setDescriptor(descriptorCopy);
			list.add(notificationInfo);
		}
		return list.toArray(new ModelMBeanNotificationInfo[list.size()]);
	}

	private ModelMBeanNotificationInfo createNotificationInfo(SyntheticNotificationEntry entry) {
		return new ModelMBeanNotificationInfo(new String[] {entry.getNotificationDescriptor().getDataPath()},
				Notification.class.getName(), entry.getDescription());
	}
}
