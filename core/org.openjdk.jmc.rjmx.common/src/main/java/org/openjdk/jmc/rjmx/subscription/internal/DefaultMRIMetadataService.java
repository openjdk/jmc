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
package org.openjdk.jmc.rjmx.subscription.internal;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.management.ObjectName;

import org.osgi.framework.ServiceException;

import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.internal.IDisposableService;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.subscription.IMBeanServerChangeListener;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.MRI;

/**
 * This is the default implementation of the metadata service.
 */
public final class DefaultMRIMetadataService extends Observable implements IMRIMetadataService, IDisposableService {

	private final Observer metadataObserver;
	private final IMBeanHelperService mbeanService;
	private final IMBeanServerChangeListener mbeanListener = new IMBeanServerChangeListener() {

		@Override
		public void mbeanUnregistered(ObjectName mbean) {
			metadataObserver.update(null, mbean);
		}

		@Override
		public void mbeanRegistered(ObjectName mbean) {
			metadataObserver.update(null, mbean);
		}
	};

	/**
	 * Constructor.
	 *
	 * @throws ServiceException
	 */
	public DefaultMRIMetadataService(IConnectionHandle handle)
			throws ConnectionException, ServiceNotAvailableException {
		metadataObserver = createMetadataObserver();
		getGlobalService().addObserver(metadataObserver);
		mbeanService = handle.getServiceOrThrow(IMBeanHelperService.class);
		mbeanService.addMBeanServerChangeListener(mbeanListener);
	}

	@Override
	public IMRIMetadata getMetadata(MRI mri) {
		return new MRIMetadataWrapper(mri, this);
	}

	@Override
	public Object getMetadata(MRI mri, String dataKey) {
		Object md = getGlobalService().getMetadata(mri, dataKey);
		if (md != null) {
			return md;
		}
		Map<String, Object> mriMetadata = mbeanService.getMBeanMetadata(mri.getObjectName()).get(mri);
		return mriMetadata != null ? mriMetadata.get(dataKey) : null;
	}

	@Override
	public void setMetadata(MRI mri, String dataKey, String data) {
		getGlobalService().setMetadata(mri, dataKey, data);
	}

	@Override
	public void dispose() {
		mbeanService.removeMBeanServerChangeListener(mbeanListener);
		getGlobalService().deleteObserver(metadataObserver);
	}

	private static IMRIMetadataService getGlobalService() {
		return RJMXPlugin.getDefault().getService(IMRIMetadataService.class);
	}

	private Observer createMetadataObserver() {
		return new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				setChanged();
				notifyObservers(arg);
			}
		};
	}
}
