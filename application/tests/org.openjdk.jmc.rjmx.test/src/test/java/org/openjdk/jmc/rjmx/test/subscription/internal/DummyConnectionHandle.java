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
package org.openjdk.jmc.rjmx.test.subscription.internal;

import java.util.Observer;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.internal.ServerDescriptor;
import org.openjdk.jmc.rjmx.services.internal.ServiceFactoryManager;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.internal.MRIMetadataWrapper;

@SuppressWarnings("nls")
public class DummyConnectionHandle implements IConnectionHandle {
	private final IServerDescriptor serverDescriptor = new ServerDescriptor();

	public DummyConnectionHandle() {
	}

	@Override
	public <T> T getServiceOrDummy(Class<T> serviceInterface) {
		if (!serviceInterface.isInterface()) {
			/*
			 * If you get this, you need to call createServiceOrNull() instead and handle the null
			 * case. (And yes, we want to check this before attempting to create a service. Ideally,
			 * we would like to check this at compile time, but there was no way to do that yet. JSR
			 * 308 and the Checker Framework should change that.)
			 */
			throw new IllegalArgumentException("Will not be able to create dummy implementations of " //$NON-NLS-1$
					+ serviceInterface.getName() + " since the service is not an interface"); //$NON-NLS-1$
		}
		T service = getServiceOrNull(serviceInterface);
		if (service == null) {
			service = ServiceFactoryManager.createDummyService(serviceInterface, null);
		}
		return service;
	}

	@Override
	public <T> T getServiceOrNull(Class<T> serviceClass) {
		if (serviceClass == IMRIMetadataService.class) {
			@SuppressWarnings("unchecked")
			T attributeInfoService = (T) new IMRIMetadataService() {

				@Override
				public IMRIMetadata getMetadata(MRI descriptor) {
					return new MRIMetadataWrapper(descriptor, this);
				}

				@Override
				public Object getMetadata(MRI descriptor, String dataKey) {
					return null;
				}

				@Override
				public void setMetadata(MRI descriptor, String dataKey, String data) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void addObserver(Observer o) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void deleteObserver(Observer o) {
					throw new UnsupportedOperationException();
				}

			};
			return attributeInfoService;
		}
		return null;
	}

	@Override
	public boolean hasService(Class<?> serviceClass) {
		return (serviceClass == IMRIMetadataService.class);
	}

	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public String getDescription() {
		return "Dummy"; //$NON-NLS-1$
	}

	@Override
	public void close() {

	}

	@Override
	public IServerDescriptor getServerDescriptor() {
		return serverDescriptor;
	}

	@Override
	public <T> T getServiceOrThrow(Class<T> serviceClass) throws ServiceNotAvailableException {
		T service = getServiceOrNull(serviceClass);
		if (service == null) {
			throw new ServiceNotAvailableException("Service '" + serviceClass.getName() + "' is not available!");
		}
		return service;
	}
}
