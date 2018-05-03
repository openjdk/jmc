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
package org.openjdk.jmc.common.test.mock.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.ICanonicalAccessorFactory;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.UnitLookup;

// FIXME: Should perhaps be moved to flightrecorder.test where it is used
@SuppressWarnings("nls")
public class MockStacktraceType implements IType<MockItem<IMCStackTrace, MockStacktraceType>> {
	private final Map<IAccessorKey<?>, AttributeDescription> keys = new HashMap<>();
	private final List<IAttribute<?>> attributes = new ArrayList<>();

	@SuppressWarnings({"rawtypes"})
	public MockStacktraceType() {
		IAccessorKey accessorKey = (IAccessorKey) Attribute.attr(MockAttributes.STACKTRACE_ID, UnitLookup.STACKTRACE);
		keys.put(accessorKey, new AttributeDescription(MockAttributes.STACKTRACE_VALUE));
		attributes.add(MockAttributes.STACKTRACE_VALUE);
	}

	@Override
	public String getName() {
		return "Mock stack trace";
	}

	@Override
	public String getDescription() {
		return "Mock stack trace";
	}

	@Override
	public List<IAttribute<?>> getAttributes() {
		return attributes;
	}

	@Override
	public Map<IAccessorKey<?>, ? extends IDescribable> getAccessorKeys() {
		return keys;
	}

	@Override
	public boolean hasAttribute(ICanonicalAccessorFactory<?> attribute) {
		return attribute.getAccessor(this) != null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <M> IMemberAccessor<M, MockItem<IMCStackTrace, MockStacktraceType>> getAccessor(IAccessorKey<M> attribute) {
		if (attribute.getIdentifier().equals(MockAttributes.STACKTRACE_ID)) {
			return (IMemberAccessor<M, MockItem<IMCStackTrace, MockStacktraceType>>) new IMemberAccessor<IMCStackTrace, MockItem<IMCStackTrace, MockStacktraceType>>() {
				@Override
				public IMCStackTrace getMember(MockItem<IMCStackTrace, MockStacktraceType> inObject) {
					return inObject.getValue();
				}
			};
		}
		throw new UnsupportedOperationException(
				"Attribute " + attribute.getIdentifier() + " is not supported by the testing framwork.");
	}

	@Override
	public String getIdentifier() {
		return MockTypeIDs.MOCK_STACKTRACE_TYPE;
	}

}
