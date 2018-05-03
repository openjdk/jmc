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
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.ICanonicalAccessorFactory;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;

@SuppressWarnings("nls")
public class MockNumberType implements IType<MockItem<Number, MockNumberType>> {
	private final Map<IAccessorKey<?>, AttributeDescription> keys = new HashMap<>();
	private final List<IAttribute<?>> attributes = new ArrayList<>();

	@SuppressWarnings({"rawtypes"})
	public MockNumberType() {

		IAccessorKey accessorKey = (IAccessorKey) Attribute.attr(MockAttributes.DOUBLE_VALUE_ID, UnitLookup.NUMBER);
		keys.put(accessorKey, new AttributeDescription(MockAttributes.DOUBLE_VALUE));
		accessorKey = (IAccessorKey) Attribute.attr(MockAttributes.LONG_INDEX_ID, UnitLookup.NUMBER);
		keys.put(accessorKey, new AttributeDescription(MockAttributes.INDEX_VALUE));
		attributes.add(MockAttributes.DOUBLE_VALUE);
		attributes.add(MockAttributes.INDEX_VALUE);
	}

	@Override
	public String getName() {
		return "Mock Double";
	}

	@Override
	public String getDescription() {
		return "Event type with a double value and a long index";
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
	public <M> IMemberAccessor<M, MockItem<Number, MockNumberType>> getAccessor(IAccessorKey<M> attribute) {
		// Hardcoded for now
		if (attribute.getIdentifier().equals(MockAttributes.LONG_INDEX_ID)) {
			return (IMemberAccessor<M, MockItem<Number, MockNumberType>>) new IMemberAccessor<IQuantity, MockItem<Number, MockNumberType>>() {
				@Override
				public IQuantity getMember(MockItem<Number, MockNumberType> inObject) {
					return UnitLookup.NUMBER_UNITY.quantity(inObject.getIndex());
				}
			};
		} else if (attribute.getIdentifier().equals(MockAttributes.DOUBLE_VALUE_ID)) {
			return (IMemberAccessor<M, MockItem<Number, MockNumberType>>) new IMemberAccessor<IQuantity, MockItem<Number, MockNumberType>>() {
				@Override
				public IQuantity getMember(MockItem<Number, MockNumberType> inObject) {
					return UnitLookup.NUMBER_UNITY.quantity(inObject.getValue());
				}
			};
		}
		throw new UnsupportedOperationException("Not supported by the testing framwork.");
	}

	@Override
	public String getIdentifier() {
		return MockTypeIDs.MOCK_NUMBER_TYPE;
	}

}
