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

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;

public enum CollectorType {

	CMS("ConcurrentMarkSweep"), //$NON-NLS-1$
	DEF_NEW("DefNew"), //$NON-NLS-1$
	G1_FULL("G1Full"), //$NON-NLS-1$
	G1_NEW("G1New"), //$NON-NLS-1$
	G1_OLD("G1Old"), //$NON-NLS-1$
	PAR_NEW("ParNew"), //$NON-NLS-1$
	PARALLEL_OLD("ParallelOld"), //$NON-NLS-1$
	PARALLEL_SCAVENGE("ParallelScavenge"), //$NON-NLS-1$
	PS_MARK_SWEEP("PSMarkSweep"), //$NON-NLS-1$
	SERIAL_OLD("SerialOld"), //$NON-NLS-1$,
	Z("Z"), //$NON-NLS-1$
	NA("N/A"), //$NON-NLS-1$
	UNKNOWN(""), //$NON-NLS-1$
	;

	private final String collectorName;

	private CollectorType(final String collectorName) {
		this.collectorName = collectorName;
	}

	public String getCollectorName() {
		return this.collectorName;
	}

	public static CollectorType getOldCollectorType(final IItemCollection items) {
		final String oc = items.getAggregate(JdkAggregators.OLD_COLLECTOR);
		if (oc != null) {
			for (final CollectorType collectorType : CollectorType.values()) {
				if (collectorType.getCollectorName().equals(oc)) {
					return collectorType;
				}
			}
		}
		return CollectorType.UNKNOWN;
	}
}
