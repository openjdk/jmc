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
package org.openjdk.jmc.rjmx.services.jfr;

import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_MS;
import static org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND;

import java.util.List;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor.RecordingState;

/**
 * Helper methods to facilitate the use of the {@link IFlightRecorderService}.
 */
public final class FlightRecorderToolkit {
	private FlightRecorderToolkit() {
		throw new AssertionError("Not to be instantiated!"); //$NON-NLS-1$
	}

	/**
	 * Will return the first descriptor matching the name. Since there is usually very few
	 * recordings available, we just do a linear search.
	 *
	 * @param name
	 *            the name to look for.
	 * @param descriptors
	 *            the descriptors to search.
	 * @return the matching descriptor, or null if none was found.
	 */
	public static IRecordingDescriptor getDescriptorByName(List<IRecordingDescriptor> descriptors, String name) {
		if (name == null) {
			return null;
		}
		for (IRecordingDescriptor descriptor : descriptors) {
			if (name.equals(descriptor.getName())) {
				return descriptor;
			}
		}
		return null;
	}

	/**
	 * Will return the first descriptor matching the id. Since there is usually very few recordings
	 * available, we just do a linear search.
	 *
	 * @param id
	 *            the id to look for.
	 * @param descriptors
	 *            the descriptors to search.
	 * @return the matching descriptor, or null if none was found.
	 */
	public static IRecordingDescriptor getDescriptorByID(IRecordingDescriptor[] descriptors, Integer id) {
		if (id == null) {
			return null;
		}
		for (IRecordingDescriptor descriptor : descriptors) {
			if (id.intValue() == descriptor.getId().intValue()) {
				return descriptor;
			}
		}
		return null;
	}

	/**
	 * Will return the descriptor that covers the largest part of the desired time range, prefers
	 * running recordings over stopped recordings.
	 *
	 * @param descriptors
	 *            The descriptors to search
	 * @param timerange
	 *            The desired time range in milliseconds
	 * @return The best matching descriptor or null if none was found.
	 */
	public static IRecordingDescriptor getDescriptorByTimerange(
		List<IRecordingDescriptor> descriptors, IQuantity timerange) {
		IRecordingDescriptor descriptor = null;
		descriptor = getDescriptorByTimerange(descriptors, timerange, IRecordingDescriptor.RecordingState.RUNNING);
		if (descriptor == null) {
			descriptor = getDescriptorByTimerange(descriptors, timerange, IRecordingDescriptor.RecordingState.STOPPED);
		}
		return descriptor;
	}

	private static IRecordingDescriptor getDescriptorByTimerange(
		List<IRecordingDescriptor> descriptors, IQuantity timerange, RecordingState recordingState) {
		if (timerange == null) {
			return null;
		}
		long now = System.currentTimeMillis();
		long desiredStartTime = now - timerange.clampedLongValueIn(MILLISECOND);
		long bestStartTime = Long.MAX_VALUE;
		IRecordingDescriptor bestMatchingDescriptor = null;
		for (IRecordingDescriptor descriptor : descriptors) {
			if (descriptor.getState() == recordingState) {
				long dataStartTime = Long.MAX_VALUE;
				IQuantity dataStart = descriptor.getDataStartTime();
				if (dataStart != null) {
					dataStartTime = dataStart.clampedLongValueIn(EPOCH_MS);
					if (dataStartTime <= desiredStartTime) {
						return descriptor;
					}
				} else {
					// Generate a synthetic data start time
					IQuantity recordingStart = descriptor.getStartTime();
					if (recordingStart != null) {
						dataStartTime = recordingStart.clampedLongValueIn(EPOCH_MS);
						// FIXME: Also take max size into account?
						long maxAge = descriptor.getMaxAge().clampedLongValueIn(MILLISECOND);
						if (maxAge > 0) {
							dataStartTime = Math.max(dataStartTime, now - maxAge);
						}
					} else if (bestMatchingDescriptor == null) {
						// Last resort choice
						bestMatchingDescriptor = descriptor;
					}
				}
				if (dataStartTime < bestStartTime) {
					bestMatchingDescriptor = descriptor;
					bestStartTime = dataStartTime;
				}
			}
		}
		return bestMatchingDescriptor;
	}
}
