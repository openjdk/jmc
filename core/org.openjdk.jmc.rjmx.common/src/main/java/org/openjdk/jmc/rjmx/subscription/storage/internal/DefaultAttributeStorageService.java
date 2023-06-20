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
package org.openjdk.jmc.rjmx.subscription.storage.internal;

import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmc.rjmx.internal.IDisposableService;
import org.openjdk.jmc.rjmx.services.IAttributeStorage;
import org.openjdk.jmc.rjmx.services.IAttributeStorageService;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;

/**
 * Default implementation of {@link IAttributeStorageService}. Keeps a pretty rigid synchronization
 * on the methods.
 */
public class DefaultAttributeStorageService implements IAttributeStorageService, IDisposableService {

	private final ISubscriptionService attributeSubscriptionService;
	private Map<MRI, BufferingAttributeStorage> activeStorages = new HashMap<>();

	/**
	 * Creates a new {@link IAttributeStorageService}.
	 *
	 * @param subscriptionService
	 */
	public DefaultAttributeStorageService(ISubscriptionService subscriptionService) {
		attributeSubscriptionService = subscriptionService;
	}

	@Override
	public synchronized IAttributeStorage getAttributeStorage(MRI mri) {
		if (activeStorages == null) {
			throw new IllegalStateException("Attribute storage service is disposed!"); //$NON-NLS-1$
		}
		BufferingAttributeStorage storage = activeStorages.get(mri);
		if (storage == null) {
			storage = new BufferingAttributeStorage(mri, attributeSubscriptionService);
			activeStorages.put(mri, storage);
		}
		return storage;
	}

	@Override
	public int getRetainedLength(MRI mri) {
		if (activeStorages == null) {
			throw new IllegalStateException("Attribute storage service is disposed!"); //$NON-NLS-1$
		}
		BufferingAttributeStorage storage = activeStorages.get(mri);
		if (storage != null) {
			return storage.getRetainedLength();
		}
		return 0;
	}

	@Override
	public synchronized void dispose() {
		for (BufferingAttributeStorage s : activeStorages.values()) {
			s.dispose();
		}
		activeStorages = null;
	}
}
