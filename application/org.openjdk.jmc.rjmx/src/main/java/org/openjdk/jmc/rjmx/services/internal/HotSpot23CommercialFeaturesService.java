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
package org.openjdk.jmc.rjmx.services.internal;

import javax.management.MBeanServerConnection;

import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.ICommercialFeaturesService;
import org.openjdk.jmc.rjmx.services.IDiagnosticCommandService;

public class HotSpot23CommercialFeaturesService implements ICommercialFeaturesService {
	private final static String VM_FLAG = "UnlockCommercialFeatures"; //$NON-NLS-1$
	private final static String UNLOCK_COMMAND = "VM.unlock_commercial_features"; //$NON-NLS-1$
	private final MBeanServerConnection server;
	private final IDiagnosticCommandService dcs;

	public HotSpot23CommercialFeaturesService(IConnectionHandle handle)
			throws ConnectionException, ServiceNotAvailableException {
		server = handle.getServiceOrThrow(MBeanServerConnection.class);
		dcs = handle.getServiceOrNull(IDiagnosticCommandService.class);
		try {
			HotspotManagementToolkit.getVMOption(server, VM_FLAG); // Will fail if option is not available
		} catch (Exception e) {
			throw new ServiceNotAvailableException(""); //$NON-NLS-1$
		}
	}

	@Override
	public boolean isCommercialFeaturesEnabled() {
		try {
			return ((String) HotspotManagementToolkit.getVMOption(server, VM_FLAG)).contains("true"); //$NON-NLS-1$
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void enableCommercialFeatures() throws Exception {
		if (dcs != null) {
			dcs.runCtrlBreakHandlerWithResult(UNLOCK_COMMAND);
		}
		if (!isCommercialFeaturesEnabled()) {
			HotspotManagementToolkit.setVMOption(server, VM_FLAG, "true"); //$NON-NLS-1$
		}
	}
}
