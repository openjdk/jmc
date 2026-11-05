/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.test.selection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.openjdk.jmc.flightrecorder.test.selection.SelectionTestItems.count;
import static org.openjdk.jmc.flightrecorder.test.selection.SelectionTestItems.item;
import static org.openjdk.jmc.flightrecorder.test.selection.SelectionTestItems.items;
import static org.openjdk.jmc.flightrecorder.test.selection.SelectionTestItems.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.StructContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.selection.IItemStreamFlavor;
import org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor;
import org.openjdk.jmc.flightrecorder.selection.SelectionAspectToolkit;
import org.openjdk.jmc.flightrecorder.selection.StacktraceFrameSelection;
import org.openjdk.jmc.flightrecorder.selection.TypeSelection;

@SuppressWarnings("nls")
public class SelectionContractTest {
	private static final IAttribute<String> KEY = Attribute.attr("contractKey", "Key", null, UnitLookup.PLAIN_TEXT);

	@Test
	public void propertyMembershipIsIndependentOfCallerSetMutation() {
		StructContentType<IItem> eventType = type("contract", KEY);
		IItemCollection universe = items(item(eventType, KEY, "a"), item(eventType, KEY, "b"),
				item(eventType, KEY, "c"));
		Set<String> values = new LinkedHashSet<>(Arrays.asList("a", "b"));
		IPropertyFlavor aspect = IPropertyFlavor.build(KEY, values, universe);
		String identifier = aspect.getIdentifier();
		values.clear();
		values.add("c");
		assertEquals(2, count(aspect.evaluate()));
		assertEquals(identifier, aspect.getIdentifier());
		Set<?> properties = (Set<?>) aspect.getProperties().findFirst().get().getValue();
		assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b")), properties);
		assertThrows(UnsupportedOperationException.class, properties::clear);
	}

	@Test
	public void propertySupplierIsSnapshottedOnce() {
		StructContentType<IItem> eventType = type("contract", KEY);
		IItemCollection universe = items(item(eventType, KEY, "a"));
		List<IPropertyFlavor.IProperty> properties = new ArrayList<>();
		properties.add(IPropertyFlavor.buildProperty(KEY, "a"));
		AtomicInteger supplied = new AtomicInteger();
		IPropertyFlavor aspect = IPropertyFlavor.build("custom", ItemFilters.equals(KEY, "a"), universe, () -> {
			supplied.incrementAndGet();
			return properties.stream();
		});
		properties.clear();
		assertEquals("a", aspect.getProperties().findFirst().get().getValue());
		assertEquals(aspect.getIdentifier(), aspect.getIdentifier());
		assertEquals(1, supplied.get());
		assertEquals(1, count(aspect.evaluate()));
	}

	@Test
	public void combinedPropertySupplierIsEvaluatedOnce() {
		StructContentType<IItem> eventType = type("contract", KEY);
		IItemCollection universe = items(item(eventType, KEY, "a"), item(eventType, KEY, "b"));
		List<IPropertyFlavor> aspects = new ArrayList<>();
		aspects.add(IPropertyFlavor.build(KEY, "a", universe));
		AtomicInteger supplied = new AtomicInteger();
		IPropertyFlavor combined = IPropertyFlavor.combine(() -> {
			supplied.incrementAndGet();
			return aspects.stream();
		}, universe);
		aspects.clear();
		assertEquals(1, count(combined.evaluate()));
		assertEquals("a", combined.getProperties().findFirst().get().getValue());
		assertEquals(1, supplied.get());
	}

	@Test
	public void typeSelectionSnapshotsCallerTypes() {
		StructContentType<IItem> first = type("first", KEY);
		StructContentType<IItem> second = type("second", KEY);
		IItem original = item(first, KEY, "a");
		IItemCollection universe = items(original, item(second, KEY, "b"));
		Set<IType<?>> selectedTypes = new LinkedHashSet<>(Collections.singleton(first));
		TypeSelection selection = new TypeSelection(selectedTypes, "first type");
		selectedTypes.clear();
		selectedTypes.add(second);
		IItemCollection selected = selection.getFlavors(null, universe, null).findFirst().get().evaluate();
		assertEquals(1, count(selected));
		assertSame(original, selected.iterator().next().iterator().next());
	}

	@Test
	public void missingStackFrameStillExposesSelectedEvents() {
		IItemCollection selected = items(item(type("contract", KEY), KEY, "a"));
		List<IItemStreamFlavor> aspects = SelectionAspectToolkit
				.listAspects(new StacktraceFrameSelection(null, selected, "events only"), null, selected, null);
		assertEquals(1, aspects.size());
		assertSame(selected, aspects.get(0).evaluate());
	}
}
