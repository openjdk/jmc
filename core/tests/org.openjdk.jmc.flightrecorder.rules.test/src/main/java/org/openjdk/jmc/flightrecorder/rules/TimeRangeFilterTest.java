/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2025, Datadog, Inc. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.rules;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.ICanonicalAccessorFactory;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.MemberAccessorToolkit;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

public class TimeRangeFilterTest {

	@Test
	public void intersects() {
		long start1 = System.currentTimeMillis();
		long end1 = start1 + 1000;
		MockEventItem item1 = new MockEventItem(UnitLookup.EPOCH_MS.quantity(start1),
				UnitLookup.EPOCH_MS.quantity(end1));
		IItemFilter timeRange = RulesToolkit.createRangeFilter(item1);
		long start2 = start1 - 100;
		long end2 = start2 + 1000;
		MockEventItem item2 = new MockEventItem(UnitLookup.EPOCH_MS.quantity(start2),
				UnitLookup.EPOCH_MS.quantity(end2));
		Assert.assertTrue(timeRange.getPredicate(item2).test(item2));

		start2 = start1 + 100;
		end2 = start2 + 1000;
		item2 = new MockEventItem(UnitLookup.EPOCH_MS.quantity(start2), UnitLookup.EPOCH_MS.quantity(end2));
		Assert.assertTrue(timeRange.getPredicate(item2).test(item2));
	}

	private static class MockEventItem implements IItem, IType<IItem> {
		private IQuantity startTime;
		private IQuantity endTime;

		public MockEventItem(IQuantity startTime, IQuantity endTime) {
			this.startTime = startTime;
			this.endTime = endTime;
		}

		@Override
		public IType<IItem> getType() {
			return this;
		}

		@Override
		public String getName() {
			return "MockEventItem";
		}

		@Override
		public String getDescription() {
			return "MockEventItem";
		}

		@Override
		public List<IAttribute<?>> getAttributes() {
			return Collections.emptyList();
		}

		@Override
		public Map<IAccessorKey<?>, ? extends IDescribable> getAccessorKeys() {
			return Collections.emptyMap();
		}

		@Override
		public boolean hasAttribute(ICanonicalAccessorFactory<?> attribute) {
			return false;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <M> IMemberAccessor<M, IItem> getAccessor(IAccessorKey<M> attribute) {
			if (attribute.getIdentifier().equals(JfrAttributes.START_TIME.getIdentifier())) {
				return (IMemberAccessor<M, IItem>) MemberAccessorToolkit
						.<IItem, IQuantity, IQuantity> constant(startTime);
			}
			if (attribute.getIdentifier().equals(JfrAttributes.END_TIME.getIdentifier())) {
				return (IMemberAccessor<M, IItem>) MemberAccessorToolkit
						.<IItem, IQuantity, IQuantity> constant(endTime);
			}
			return null;
		}

		@Override
		public String getIdentifier() {
			return null;
		}

	}
}
