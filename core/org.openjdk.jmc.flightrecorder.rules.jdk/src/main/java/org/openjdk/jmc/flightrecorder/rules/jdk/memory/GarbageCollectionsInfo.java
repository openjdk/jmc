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
package org.openjdk.jmc.flightrecorder.rules.jdk.memory;

import org.openjdk.jmc.common.item.Aggregators.MergingAggregator;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemConsumer;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;

class GarbageCollectionsInfo implements IItemConsumer<GarbageCollectionsInfo> {
	private final IMemberAccessor<String, IItem> causeAccessor;
	private final IMemberAccessor<String, IItem> nameAccessor;

	private int gcCount;
	private int systemGcCount;
	private int gcLockers;
	private int objectCountGCs;
	private boolean nonRequestedSerialOldGc;

	GarbageCollectionsInfo(IMemberAccessor<String, IItem> causeAccessor, IMemberAccessor<String, IItem> nameAccessor) {
		this.causeAccessor = causeAccessor;
		this.nameAccessor = nameAccessor;
	}

	public int getGcCount() {
		return gcCount;
	}

	public int getGcLockers() {
		return gcLockers;
	}

	public int getObjectCountGCs() {
		return objectCountGCs;
	}

	public int getSystemGcCount() {
		return systemGcCount;
	}

	public boolean foundNonRequestedSerialOldGc() {
		return nonRequestedSerialOldGc;
	}

	@Override
	public void consume(IItem item) {
		String cause = causeAccessor.getMember(item);
		cause = cause != null ? cause.toLowerCase() : ""; //$NON-NLS-1$
		if ("heap inspection initiated gc".equals(cause)) { //$NON-NLS-1$
			objectCountGCs++;
		} else if ("system.gc()".equals(cause)) { //$NON-NLS-1$
			systemGcCount++;
		} else {
			if (cause.contains("gclocker")) { //$NON-NLS-1$
				gcLockers++;
			}
			if (!nonRequestedSerialOldGc && CollectorType.SERIAL_OLD.getCollectorName().equals(nameAccessor.getMember(item))) {
				nonRequestedSerialOldGc = true;
			}
		}
		gcCount++;

	}

	@Override
	public GarbageCollectionsInfo merge(GarbageCollectionsInfo other) {
		gcCount += other.gcCount;
		systemGcCount += other.systemGcCount;
		gcLockers += other.gcLockers;
		objectCountGCs += other.objectCountGCs;
		nonRequestedSerialOldGc |= other.nonRequestedSerialOldGc;
		return this;
	}

	public static final IAggregator<GarbageCollectionsInfo, ?> GC_INFO_AGGREGATOR = new MergingAggregator<GarbageCollectionsInfo, GarbageCollectionsInfo>(
			null, null, UnitLookup.UNKNOWN) {

		@Override
		public boolean acceptType(IType<IItem> type) {
			return type.getIdentifier().equals(JdkTypeIDs.GARBAGE_COLLECTION);
		}

		@Override
		public GarbageCollectionsInfo newItemConsumer(IType<IItem> type) {
			IMemberAccessor<String, IItem> causeAccessor = JdkAttributes.GC_CAUSE.getAccessor(type);
			IMemberAccessor<String, IItem> nameAccessor = JdkAttributes.GC_NAME.getAccessor(type);
			return new GarbageCollectionsInfo(causeAccessor, nameAccessor);
		}

		@Override
		public GarbageCollectionsInfo getValue(GarbageCollectionsInfo consumer) {
			return consumer == null ? new GarbageCollectionsInfo(null, null) : consumer;
		}

	};
}
