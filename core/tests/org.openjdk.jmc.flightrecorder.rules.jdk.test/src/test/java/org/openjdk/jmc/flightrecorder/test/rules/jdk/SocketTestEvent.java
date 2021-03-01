/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.test.rules.jdk;

import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.ICanonicalAccessorFactory;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.MemberAccessorToolkit;

public class SocketTestEvent extends TestEvent {
	private final String address;
	private final long duration;
	private final long bytesProcessed;

	public SocketTestEvent(String eventType, String address, long duration, long bytesProcessed) {
		super(eventType);
		this.address = address;
		this.duration = duration;
		this.bytesProcessed = bytesProcessed;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <M> IMemberAccessor<M, IItem> getAccessor(IAccessorKey<M> attribute) {
		if ("duration".equals(attribute.getIdentifier())) {
			return (IMemberAccessor<M, IItem>) MemberAccessorToolkit
					.<IItem, IQuantity, IQuantity> constant(UnitLookup.MILLISECOND.quantity(duration));
		}
		if ("address".equals(attribute.getIdentifier())) {
			return (IMemberAccessor<M, IItem>) MemberAccessorToolkit.<IItem, String, String> constant(address);
		}
		if ("bytesRead".equals(attribute.getIdentifier())) {
			return (IMemberAccessor<M, IItem>) MemberAccessorToolkit
					.<IItem, IQuantity, IQuantity> constant(UnitLookup.BYTE.quantity(bytesProcessed));
		}
		if ("bytesWritten".equals(attribute.getIdentifier())) {
			return (IMemberAccessor<M, IItem>) MemberAccessorToolkit
					.<IItem, IQuantity, IQuantity> constant(UnitLookup.BYTE.quantity(bytesProcessed));
		}
		return null;
	}

	@Override
	public boolean hasAttribute(ICanonicalAccessorFactory<?> attribute) {
		if ("duration".equals(attribute.getIdentifier())) {
			return true;
		}
		if ("address".equals(attribute.getIdentifier())) {
			return true;
		}
		if ("bytesRead".equals(attribute.getIdentifier())) {
			return true;
		}
		if ("bytesWritten".equals(attribute.getIdentifier())) {
			return true;
		}
		return false;
	}

}
