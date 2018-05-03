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
package org.openjdk.jmc.console.ui.tabs.memory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Observable;

import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;

public class MemoryPoolModel extends Observable {

	private final MemoryPoolInformation[] m_pools;

	public MemoryPoolModel(IConnectionHandle connectionHandle) {
		IMBeanHelperService mbeanHelperService = connectionHandle.getServiceOrDummy(IMBeanHelperService.class);
		ISubscriptionService subscriptionService = connectionHandle.getServiceOrDummy(ISubscriptionService.class);
		List<ObjectName> memoryPools = findPools(mbeanHelperService);
		m_pools = setupSubscriptions(memoryPools, mbeanHelperService, subscriptionService);
	}

	public MemoryPoolInformation[] getAllPools() {
		return m_pools;
	}

	private MemoryPoolInformation[] setupSubscriptions(
		List<ObjectName> memoryPools, IMBeanHelperService mbeanHelperService,
		ISubscriptionService subscriptionService) {
		List<MemoryPoolInformation> pools = new ArrayList<>();
		for (ObjectName poolName : memoryPools) {
			pools.add(setupSubscription(poolName, mbeanHelperService, subscriptionService));
		}
		return pools.toArray(new MemoryPoolInformation[pools.size()]);
	}

	private MemoryPoolInformation setupSubscription(
		ObjectName poolName, IMBeanHelperService mbeanHelperService, ISubscriptionService subscriptionService) {
		final MemoryPoolInformation info = new MemoryPoolInformation();
		try {
			info.setPoolName(
					mbeanHelperService.getAttributeValue(new MRI(Type.ATTRIBUTE, poolName, "Name")).toString()); //$NON-NLS-1$
			info.setPoolType(
					mbeanHelperService.getAttributeValue(new MRI(Type.ATTRIBUTE, poolName, "Type")).toString()); //$NON-NLS-1$

			MRI usageMRI = new MRI(Type.ATTRIBUTE, poolName, "Usage"); //$NON-NLS-1$
			subscriptionService.addMRIValueListener(usageMRI, new IMRIValueListener() {
				@Override
				public void valueChanged(MRIValueEvent event) {
					// might receive MRIValue.UnavailableValue during shutdown
					if (event.getValue() instanceof CompositeData) {
						updateUsage(info, (CompositeData) event.getValue());
					}
				}
			});
			updateUsage(info, (CompositeData) mbeanHelperService.getAttributeValue(usageMRI));

			MRI peakMRI = new MRI(Type.ATTRIBUTE, poolName, "PeakUsage"); //$NON-NLS-1$
			subscriptionService.addMRIValueListener(peakMRI, new IMRIValueListener() {
				@Override
				public void valueChanged(MRIValueEvent event) {
					// might receive MRIValue.UnavailableValue during shutdown
					if (event.getValue() instanceof CompositeData) {
						updatePeak(info, (CompositeData) event.getValue());
					}
				}
			});
			updatePeak(info, (CompositeData) mbeanHelperService.getAttributeValue(peakMRI));
		} catch (Exception e) {
			return null;
		}
		return info;
	}

	private void updateUsage(MemoryPoolInformation info, CompositeData value) {
		Long used = ((Long) value.get("used")); //$NON-NLS-1$
		Long max = ((Long) value.get("max")); //$NON-NLS-1$
		if (used != null && max != null) {
			info.setCurUsed(UnitLookup.BYTE.quantity(used));
			info.setCurMax(max == -1 ? null : UnitLookup.BYTE.quantity(max));
			info.setCurUsage(
					max == -1 ? null : UnitLookup.PERCENT_UNITY.quantity(used.doubleValue() / max.doubleValue()));
			setChanged();
			notifyObservers();
		}
	}

	private void updatePeak(MemoryPoolInformation info, CompositeData value) {
		Long used = ((Long) value.get("used")); //$NON-NLS-1$
		Long max = ((Long) value.get("max")); //$NON-NLS-1$
		if (used != null && max != null) {
			info.setPeakUsed(UnitLookup.BYTE.quantity(used));
			info.setPeakMax(max == -1 ? null : UnitLookup.BYTE.quantity(max));
			setChanged();
			notifyObservers();
		}
	}

	private List<ObjectName> findPools(IMBeanHelperService mbeanHelperService) {
		List<ObjectName> poolNames = new ArrayList<>();
		try {
			for (Entry<ObjectName, MBeanInfo> entry : mbeanHelperService.getMBeanInfos().entrySet()) {
				ObjectName o = entry.getKey();
				if (o.getDomain().equals("java.lang")) { //$NON-NLS-1$
					if (o.getKeyProperty("type").equals("MemoryPool")) { //$NON-NLS-1$ //$NON-NLS-2$
						poolNames.add(o);
					}
				}
			}
		} catch (IOException e1) {
			poolNames.clear();
		}
		return poolNames;
	}
}
