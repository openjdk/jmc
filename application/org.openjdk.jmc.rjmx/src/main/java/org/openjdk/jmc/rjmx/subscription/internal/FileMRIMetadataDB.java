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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IStateful;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataProviderService;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.MRI;

/**
 * This class handles metadata for MRIs. Metadata currently comes from either the user, such as when
 * the user specifies the update times for attribute subscriptions, or from the mrimetadata.xml file
 * that stores default values. These two sources of metadata means that metadata lookups are usually
 * performed in two steps, first this manager looks at the user-specified metadata, otherwise it
 * returns the default values.
 */
public final class FileMRIMetadataDB extends Observable implements IStateful, IMRIMetadataService {

	private static final String ELEMENT_MAP_ENTRY = "mapEntry"; //$NON-NLS-1$
	private static final String ATTRIBUTE_VALUE = "value"; //$NON-NLS-1$
	private static final String ATTRIBUTE_KEY = "key"; //$NON-NLS-1$

	// Maps attribute names to AttributeInfo objects that represent metadata
	private final Map<MRI, Map<String, Object>> metadataMap;
	private final Map<MRI, Map<String, String>> changedMetadataStore;
	private final IMRIMetadataProviderService subService;

	private FileMRIMetadataDB(Map<MRI, Map<String, String>> changedMetadataStore,
			IMRIMetadataProviderService subService) {
		this.changedMetadataStore = changedMetadataStore;
		metadataMap = FileMRIMetadata.readDefaultsFromFile();
		this.subService = subService;
	}

	/**
	 * Changes the value of a metadata item with a certain key.
	 *
	 * @param mri
	 * @param key
	 * @param value
	 */
	@Override
	public void setMetadata(MRI mri, String key, String value) {
		synchronized (changedMetadataStore) {
			changedMetadataStore.computeIfAbsent(mri, k -> new HashMap<>()).put(key, value);
		}
		setChanged();
		notifyObservers(mri);
	}

	/**
	 * Will attempt to look up the metadata value for the certain key.
	 *
	 * @param mri
	 * @param dataKey
	 * @return the metadata.
	 */
	@Override
	public Object getMetadata(MRI mri, String dataKey) {
		synchronized (changedMetadataStore) {
			Map<String, String> mriMetadataMap = changedMetadataStore.get(mri);
			if (mriMetadataMap != null && mriMetadataMap.containsKey(dataKey)) {
				return mriMetadataMap.get(dataKey);
			}
		}
		Map<String, Object> metadataForMri;
		synchronized (metadataMap) {
			metadataForMri = metadataMap.computeIfAbsent(mri, k -> new HashMap<>(2));
			Object metadata = metadataForMri.get(dataKey);
			if (metadata != null) {
				return metadata;
			}
		}
		Object metadata = subService.getMetadata(this, mri, dataKey);
		synchronized (metadataMap) {
			// FIXME: JMC-4672 - We cache the value to avoid multiple queries to the server. This may or may not be desirable.
			metadataForMri.put(dataKey, metadata);
		}
		return metadata;
	}

	@Override
	public IMRIMetadata getMetadata(MRI mri) {
		return new MRIMetadataWrapper(mri, this);
	}

	@Override
	public void saveTo(IWritableState state) {
		synchronized (changedMetadataStore) {
			for (Entry<MRI, Map<String, String>> mriEntry : changedMetadataStore.entrySet()) {
				IWritableState mriChild = state.createChild(ELEMENT_MAP_ENTRY);
				mriChild.putString(ATTRIBUTE_KEY, mriEntry.getKey().getQualifiedName());
				for (Entry<String, String> dataEntry : mriEntry.getValue().entrySet()) {
					IWritableState dataChild = mriChild.createChild(ELEMENT_MAP_ENTRY);
					dataChild.putString(ATTRIBUTE_KEY, dataEntry.getKey());
					dataChild.putString(ATTRIBUTE_VALUE, dataEntry.getValue());
				}
			}
		}
	}

	public static FileMRIMetadataDB buildFromState(IState state, IMRIMetadataProviderService subService)
			throws RuntimeException {
		HashMap<MRI, Map<String, String>> metadata = new HashMap<>();
		for (IState mriElement : state.getChildren()) {
			MRI mri = MRI.createFromQualifiedName(mriElement.getAttribute(ATTRIBUTE_KEY));
			Map<String, String> data = new HashMap<>();
			for (IState dataElement : mriElement.getChildren()) {
				data.put(dataElement.getAttribute(ATTRIBUTE_KEY), dataElement.getAttribute(ATTRIBUTE_VALUE));
			}
			metadata.put(mri, data);
		}
		return new FileMRIMetadataDB(metadata, subService);
	}

	public static FileMRIMetadataDB buildDefault(IMRIMetadataProviderService subService) {
		return new FileMRIMetadataDB(new HashMap<MRI, Map<String, String>>(), subService);
	}

}
