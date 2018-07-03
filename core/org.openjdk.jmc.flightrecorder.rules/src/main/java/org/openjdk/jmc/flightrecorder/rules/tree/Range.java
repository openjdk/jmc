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
package org.openjdk.jmc.flightrecorder.rules.tree;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.IQuantity;

/**
 * A range of quantities. Typically used for time quantities.
 */
// FIXME: Not really related to item trees. Should be moved somewhere else.
public class Range {
	public final IQuantity startTime;
	public final IQuantity endTime;

	/**
	 * @param startTime
	 *            range start
	 * @param endTime
	 *            range end
	 */
	public Range(IQuantity startTime, IQuantity endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}

	/**
	 * @return {@code true} if the time is in the closed interval of this range
	 */
	public boolean isInside(IQuantity time) {
		return startTime.compareTo(time) <= 0 && endTime.compareTo(time) >= 0;
	}

	/**
	 * @return {@code true} if the time is before the closed interval of this range
	 */
	public boolean isBefore(IQuantity time) {
		return startTime.compareTo(time) > 0;
	}

	/**
	 * @return {@code true} if the time is after the closed interval of this range
	 */
	public boolean isAfter(IQuantity time) {
		return endTime.compareTo(time) < 0;
	}

	@Override
	public String toString() {
		return String.format("[%s, %s]", startTime.displayUsing(IDisplayable.AUTO), //$NON-NLS-1$
				endTime.displayUsing(IDisplayable.AUTO));
	}
}
