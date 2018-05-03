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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.openjdk.jmc.common.collection.BoundedList;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.preferences.PreferencesKeys;
import org.openjdk.jmc.rjmx.services.IAttributeStorage;
import org.openjdk.jmc.rjmx.services.MRIDataSeries;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.ui.common.xydata.DefaultTimestampedData;
import org.openjdk.jmc.ui.common.xydata.ITimestampedData;

/**
 * Simple attribute storage that contains attribute events.
 * <p>
 * NOTE: You must synchronize on the storage if you use the dataseries iterator!
 */
public class BufferingAttributeStorage extends Observable implements IAttributeStorage {

	private static int preferenceLookupCounter = 0;
	private static int currentRetainedEventValues;

	private static int lookupRetainedEventValues() {
		if (preferenceLookupCounter++ % 1000 == 0) {
			currentRetainedEventValues = RJMXPlugin.getDefault().getRJMXPreferences().getInt(
					PreferencesKeys.PROPERTY_RETAINED_EVENT_VALUES, PreferencesKeys.DEFAULT_RETAINED_EVENT_VALUES);
		}
		return currentRetainedEventValues;
	}

	private final BoundedList<ITimestampedData> cache = new BoundedList<>(lookupRetainedEventValues());
	private final MRI mri;
	private final ISubscriptionService subscriptionService;
	private final IMRIValueListener valueListener = new IMRIValueListener() {
		@Override
		public void valueChanged(MRIValueEvent event) {
			Object value = event.getValue();
			if (value instanceof Number) {
				if (lookupRetainedEventValues() != cache.getMaxSize()) {
					cache.setMaxSize(Math.max(lookupRetainedEventValues(), Math.max(cache.getSize() - 1000, 1)));
				}
				DefaultTimestampedData data = new DefaultTimestampedData(event.getTimestamp() * 1000 * 1000L,
						((Number) value).doubleValue());
				cache.add(data);
				setChanged();
				notifyObservers(data);
			}
		}
	};
	private final List<MRIDataSeries> dataSeries = new ArrayList<>(1);

	public BufferingAttributeStorage(MRI attribute, ISubscriptionService subscriptionService) {
		mri = attribute;
		this.subscriptionService = subscriptionService;
		dataSeries.add(new MRIDataSeries() {

			@Override
			public Iterator<ITimestampedData> createIterator(long min, long max) {
				return cache.iterator();
			}

			@Override
			public MRI getAttribute() {
				return mri;
			}
		});

	}

	@Override
	public long getDataStart() {
		ITimestampedData first = cache.getFirst();
		return first != null ? first.getX() : Long.MAX_VALUE;
	}

	@Override
	public long getDataEnd() {
		ITimestampedData last = cache.getLast();
		return last != null ? last.getX() : Long.MIN_VALUE;
	}

	@Override
	public synchronized void addObserver(Observer o) {
		if (countObservers() == 0) {
			subscriptionService.addMRIValueListener(mri, valueListener);
		}
		super.addObserver(o);
	}

	@Override
	public synchronized void deleteObserver(Observer o) {
		super.deleteObserver(o);
		if (countObservers() == 0) {
			subscriptionService.removeMRIValueListener(mri, valueListener);
		}
	}

	void dispose() {
		subscriptionService.removeMRIValueListener(mri, valueListener);
	}

	@Override
	public List<MRIDataSeries> getDataSeries() {
		return dataSeries;
	}

	public int getRetainedLength() {
		return cache.getSize();
	}
}
