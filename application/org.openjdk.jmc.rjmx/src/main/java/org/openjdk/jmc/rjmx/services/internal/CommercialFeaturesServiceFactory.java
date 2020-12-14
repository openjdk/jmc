/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.ICommercialFeaturesService;
import org.openjdk.jmc.rjmx.services.IServiceFactory;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;

public class CommercialFeaturesServiceFactory implements IServiceFactory<ICommercialFeaturesService> {

	@Override
	public ICommercialFeaturesService getServiceInstance(IConnectionHandle handle)
			throws ConnectionException, ServiceNotAvailableException {
		// Optimization - using already available information instead of doing more round trips.
		// It's always a bit precarious to look at version instead of capability, but in this case
		// it should be safe - the commercial features flag is not coming back
		JVMDescriptor descriptor = handle.getServerDescriptor().getJvmInfo();
		if (descriptor != null) {
			JavaVersion version = new JavaVersion(descriptor.getJavaVersion());
			if (version.getMajorVersion() >= 11) {
				return new NoCommercialFeaturesService();
			}
		} else if (handle.isConnected() && ConnectionToolkit.isOracle(handle)) {
			if (ConnectionToolkit.isJavaVersionAboveOrEqual(handle, JavaVersionSupport.JDK_11)) {
				return new NoCommercialFeaturesService();
			}
		}

		// Funnily enough, OpenJDK built JVMs for unknown reasons also have the unlock commercial features flag,
		// so we'll just check if Oracle is the JVM vendor. Any other vendor will not have JFR protected by commercial flags.
		if (ConnectionToolkit.isOracle(handle)) {
			return new HotSpot23CommercialFeaturesService(handle);
		}
		return new NoCommercialFeaturesService();
	}

	@Override
	public Class<ICommercialFeaturesService> getServiceType() {
		return ICommercialFeaturesService.class;
	}
}
