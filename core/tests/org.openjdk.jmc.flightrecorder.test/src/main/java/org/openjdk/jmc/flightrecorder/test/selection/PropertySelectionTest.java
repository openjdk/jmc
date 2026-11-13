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
import static org.junit.Assert.assertFalse;
import static org.openjdk.jmc.flightrecorder.test.selection.SelectionTestItems.count;
import static org.openjdk.jmc.flightrecorder.test.selection.SelectionTestItems.item;
import static org.openjdk.jmc.flightrecorder.test.selection.SelectionTestItems.items;
import static org.openjdk.jmc.flightrecorder.test.selection.SelectionTestItems.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.StructContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.selection.IItemStreamFlavor;
import org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor;
import org.openjdk.jmc.flightrecorder.selection.PropertySelection;
import org.openjdk.jmc.flightrecorder.selection.SelectionAspectToolkit;

public class PropertySelectionTest {
	private static final IAttribute<String> SELECTED = Attribute.attr("selected", "Selected", null,
			UnitLookup.PLAIN_TEXT);
	private static final IAttribute<String> FIRST = Attribute.attr("first", "First", null, UnitLookup.PLAIN_TEXT);
	private static final IAttribute<String> SECOND = Attribute.attr("second", "Second", null, UnitLookup.PLAIN_TEXT);

	@Test
	public void testSelectedPropertyKeepsRecordingWideEvaluation() {
		StructContentType<IItem> sourceType = type("source", SELECTED);
		StructContentType<IItem> destinationType = type("destination", SELECTED);
		IItemCollection universe = items(item(sourceType, SELECTED, "value"), item(destinationType, SELECTED, "value"),
				item(destinationType, SELECTED, "other"));
		PropertySelection selection = new PropertySelection(SELECTED, "value", "named property");
		List<IItemStreamFlavor> aspects = selection
				.getFlavors(ItemFilters.type("destination"), universe, Collections.singletonList(SELECTED))
				.collect(Collectors.toList());
		assertEquals("named property", selection.getName());
		assertFalse(selection.isEmpty());
		assertEquals(1, aspects.size());
		assertEquals(2, count(aspects.get(0).evaluate()));
	}

	@Test
	public void testSharedSelectedAttributeStaysFirstWithoutDestinationMatches() {
		StructContentType<IItem> sourceType = type("source", SELECTED);
		StructContentType<IItem> destinationType = type("destination", SELECTED);
		IItemCollection universe = items(item(sourceType, SELECTED, "selected"),
				item(destinationType, SELECTED, "other"));
		List<IItemStreamFlavor> aspects = new PropertySelection(SELECTED, "selected", "property")
				.getFlavors(ItemFilters.type("destination"), universe, null).collect(Collectors.toList());
		assertEquals(1, aspects.size());
		assertEquals(SELECTED, ((IPropertyFlavor) aspects.get(0)).getProperties().findFirst().get().getAttribute());
		assertEquals(1, count(aspects.get(0).evaluate()));
	}

	@Test
	public void testRelatedPropertyCombinationAndOrdering() {
		StructContentType<IItem> sourceType = type("source", SELECTED, FIRST, SECOND);
		StructContentType<IItem> destinationType = type("destination", FIRST, SECOND);
		IItemCollection universe = items(item(sourceType, SELECTED, "value", FIRST, "a", SECOND, "b"),
				item(destinationType, FIRST, "a", SECOND, "b"), item(destinationType, FIRST, "a", SECOND, "other"),
				item(destinationType, FIRST, "other", SECOND, "b"));
		List<IPropertyFlavor> aspects = new PropertySelection(SELECTED, "value", "property")
				.getFlavors(ItemFilters.type("destination"), universe, Arrays.asList(FIRST, SECOND))
				.map(IPropertyFlavor.class::cast).collect(Collectors.toList());
		assertEquals(6, aspects.size());
		assertEquals(0, count(aspects.get(0).evaluate()));
		assertEquals(0, count(aspects.get(1).evaluate()));
		assertEquals(2, aspects.get(2).getProperties().count());
		assertEquals(2, count(aspects.get(2).evaluate()));
		assertEquals(3, count(aspects.get(3).evaluate()));
		assertEquals(3, count(aspects.get(4).evaluate()));
		assertEquals(SELECTED, aspects.get(5).getProperties().findFirst().get().getAttribute());
		assertEquals(1, count(aspects.get(5).evaluate()));
	}

	@Test
	public void testProjectionAndRelatedPropertyHaveDistinctStableIdentifiers() {
		IAttribute<String> requestId = Attribute.attr("requestId", "Request ID", null, UnitLookup.PLAIN_TEXT);
		IAttribute<String> otherString = Attribute.attr("otherString", "Other string", null, UnitLookup.PLAIN_TEXT);
		StructContentType<IItem> sourceType = type("request", requestId, otherString);
		StructContentType<IItem> destinationType = type("destination", otherString);
		IItemCollection universe = items(item(sourceType, requestId, "A", otherString, "B"),
				item(destinationType, otherString, "A"), item(destinationType, otherString, "B"));
		PropertySelection selection = new PropertySelection(requestId, "A", "request selection");
		List<IItemStreamFlavor> aspects = SelectionAspectToolkit.listAspects(selection, ItemFilters.type("destination"),
				universe, Collections.singletonList(otherString));
		assertEquals(3, aspects.size());
		assertEquals(3, aspects.stream().map(IItemStreamFlavor::getIdentifier).distinct().count());
		assertEquals("content-type:property:" + otherString.getIdentifier() + "|"
				+ otherString.getContentType().getIdentifier(), aspects.get(0).getIdentifier());
		assertEquals(
				"related:property:" + otherString.getIdentifier() + "|" + otherString.getContentType().getIdentifier(),
				aspects.get(1).getIdentifier());
		assertEquals("property:" + requestId.getIdentifier() + "|" + requestId.getContentType().getIdentifier(),
				aspects.get(2).getIdentifier());
		assertEquals(1, count(aspects.get(0).evaluate()));
		assertEquals(2, count(aspects.get(1).evaluate()));
		assertEquals(1, count(aspects.get(2).evaluate()));
		List<IItemStreamFlavor> renamed = SelectionAspectToolkit.listAspects(
				new PropertySelection(requestId, "A", "another display name"), ItemFilters.type("destination"),
				universe, Collections.singletonList(otherString));
		assertEquals(aspects.stream().map(IItemStreamFlavor::getIdentifier).collect(Collectors.toList()),
				renamed.stream().map(IItemStreamFlavor::getIdentifier).collect(Collectors.toList()));
	}

	@Test
	public void testDuplicateDestinationAttributesDoNotDuplicateAspectIdentifiers() {
		StructContentType<IItem> sourceType = type("source", SELECTED, FIRST);
		StructContentType<IItem> destinationType = type("destination", FIRST);
		IItemCollection universe = items(item(sourceType, SELECTED, "A", FIRST, "B"), item(destinationType, FIRST, "A"),
				item(destinationType, FIRST, "B"));
		PropertySelection selection = new PropertySelection(SELECTED, "A", "property");
		List<IItemStreamFlavor> unique = SelectionAspectToolkit.listAspects(selection, ItemFilters.type("destination"),
				universe, Collections.singletonList(FIRST));
		List<IItemStreamFlavor> repeated = SelectionAspectToolkit.listAspects(selection,
				ItemFilters.type("destination"), universe, Arrays.asList(FIRST, FIRST));
		assertEquals(unique.stream().map(IItemStreamFlavor::getIdentifier).collect(Collectors.toList()),
				repeated.stream().map(IItemStreamFlavor::getIdentifier).collect(Collectors.toList()));
		assertEquals(repeated.size(), repeated.stream().map(IItemStreamFlavor::getIdentifier).distinct().count());
	}

	@Test
	public void testPropertySelectionSnapshotsValueSet() {
		Set<String> values = new HashSet<>(Collections.singleton("first"));
		PropertySelection selection = new PropertySelection(SELECTED, values, "property");
		values.add("second");
		StructContentType<IItem> eventType = type("source", SELECTED);
		IItemCollection universe = items(item(eventType, SELECTED, "first"), item(eventType, SELECTED, "second"));
		assertEquals(Collections.singleton("first"), selection.getValue());
		assertEquals(1, count(selection.getFlavors(null, universe, Collections.singletonList(SELECTED)).findFirst()
				.get().evaluate()));
	}
}
