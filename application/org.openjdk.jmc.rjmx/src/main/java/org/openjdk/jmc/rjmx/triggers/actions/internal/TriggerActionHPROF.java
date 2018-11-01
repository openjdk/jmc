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
package org.openjdk.jmc.rjmx.triggers.actions.internal;

import java.util.logging.Level;

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.triggers.TriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;
import org.openjdk.jmc.ui.common.util.Filename;

/**
 * This notification action triggers a hprof dump.
 */
public class TriggerActionHPROF extends TriggerAction {
	private final static ObjectName HOTSPOT;
	private final static String HPROF_OPERATION_NAME = "dumpHeap"; //$NON-NLS-1$

	static {
		ObjectName hs = null;
		try {
			hs = new ObjectName("com.sun.management:type=HotSpotDiagnostic"); //$NON-NLS-1$
		} catch (Exception e) {
			// This should really never ever happen!
			System.out.println("Could not create the HotSpotDiagnostic MBean ObjectName!"); //$NON-NLS-1$
			e.printStackTrace();
		}
		HOTSPOT = hs;
	}

	/**
	 * @throws Exception
	 * @see TriggerAction#handleNotificationEvent(TriggerEvent)
	 */
	@Override
	public void handleNotificationEvent(TriggerEvent e) throws Exception {
		String fileName = Filename.splitFilename(getSetting("filename").getString()).asRandomFilename().toString(); //$NON-NLS-1$
		Boolean onlyLive = getSetting("only_live").getBoolean(); //$NON-NLS-1$
		if (onlyLive == null) {
			onlyLive = Boolean.TRUE;
		}
		dumpHeap(e.getSource(), fileName, onlyLive);
	}

	private void dumpHeap(IConnectionHandle connectionHandle, String fileName, Boolean onlyLive) throws Exception {
		MBeanServerConnection service = connectionHandle.getServiceOrDummy(MBeanServerConnection.class);
		try {
			// Then we send it off to the service for invocation.
			service.invoke(HOTSPOT, HPROF_OPERATION_NAME, new Object[] {fileName, onlyLive},
					new String[] {String.class.getName(), Boolean.TYPE.getName()});
			RJMXPlugin.getDefault().getLogger().log(Level.INFO, "HPROF heap dump triggered to file " + fileName); //$NON-NLS-1$
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.SEVERE, "Could not invoke the hprof action!", e); //$NON-NLS-1$
			throw e;
		}
	}

	@Override
	public boolean supportsAction(IConnectionHandle handle) {
		MBeanServerConnection service = handle.getServiceOrNull(MBeanServerConnection.class);
		if (service == null) {
			return false;
		}
		return hasHProfDumpOperation(service);
	}

	private boolean hasHProfDumpOperation(MBeanServerConnection service) {
		try {
			MBeanInfo info = service.getMBeanInfo(HOTSPOT);
			for (MBeanOperationInfo operation : info.getOperations()) {
				if (operation.getName().equals(HPROF_OPERATION_NAME)
						&& hasHProfDumpSignature(operation.getSignature())) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean hasHProfDumpSignature(MBeanParameterInfo[] parameters) {
		return parameters.length == 2 && parameters[0].getType().equals(String.class.getName())
				&& parameters[1].getType().equals(Boolean.TYPE.getName());
	}
}
