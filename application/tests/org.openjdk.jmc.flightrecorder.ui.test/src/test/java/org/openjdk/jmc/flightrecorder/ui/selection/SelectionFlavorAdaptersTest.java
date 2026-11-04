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
package org.openjdk.jmc.flightrecorder.ui.selection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;

import org.junit.Test;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

public class SelectionFlavorAdaptersTest {

	@Test
	public void corePropertyFlavorRetainsCapabilitiesAndIdentity() {
		IRange<IQuantity> range = QuantityRange.createWithEnd(UnitLookup.EPOCH_S.quantity(1),
				UnitLookup.EPOCH_S.quantity(2));
		org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor core = org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor
				.buildPointRange(JfrAttributes.START_TIME, range, ItemCollectionToolkit.EMPTY);
		IItemStreamFlavor ui = SelectionFlavorAdapters.toUI(core);

		assertTrue(ui instanceof IPropertyFlavor);
		assertTrue(ui instanceof IFilterFlavor);
		assertSame(core, SelectionFlavorAdapters.toCore(ui));
		assertEquals(core.getIdentifier(), SelectionFlavorAdapters.toCore(ui).getIdentifier());
		assertSame(core.getFilter(), ((IFilterFlavor) ui).getFilter());
		assertSame(range, ((IPropertyFlavor) ui).getProperties().findFirst().get().getValue());
		assertSame(range, FlavorToolkit.getRange(ui).get());
	}

	@Test
	public void legacyPropertyFlavorStillSupportsRangeEvaluation() {
		IPropertyFlavor legacy = legacyPropertyFlavor();
		org.openjdk.jmc.flightrecorder.selection.IItemStreamFlavor core = SelectionFlavorAdapters.toCore(legacy);

		assertTrue(core instanceof org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor);
		assertSame(legacy, SelectionFlavorAdapters.toUI(core));
		assertSame(ItemCollectionToolkit.EMPTY, core.evaluate());
		assertSame(legacy.getProperties().findFirst().get().getValue(), FlavorToolkit.getRange(legacy).get());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void arbitraryLegacyFlavorDoesNotInventStableIdentity() {
		SelectionFlavorAdapters.toCore(legacyPropertyFlavor()).getIdentifier();
	}

	@Test
	public void noSelectionFlavorRetainsEmptySignal() {
		IItemStreamFlavor ui = IItemStreamFlavor.build("No selection", true, ItemCollectionToolkit.EMPTY);

		assertTrue(ui.isEmpty());
		assertTrue(SelectionFlavorAdapters.toCore(ui).isEmpty());
		assertSame(ItemCollectionToolkit.EMPTY, ui.evaluate());
	}

	private static IPropertyFlavor legacyPropertyFlavor() {
		IRange<IQuantity> range = QuantityRange.createWithEnd(UnitLookup.EPOCH_S.quantity(1),
				UnitLookup.EPOCH_S.quantity(2));
		return new IPropertyFlavor() {
			@Override
			public String getName() {
				return "Legacy property";
			}

			@Override
			public IItemCollection evaluate() {
				return ItemCollectionToolkit.EMPTY;
			}

			@Override
			public IItemFilter getFilter() {
				return ItemFilters.all();
			}

			@Override
			public Stream<IProperty> getProperties() {
				return Stream.of(IPropertyFlavor.buildProperty(JfrAttributes.START_TIME, range));
			}
		};
	}
}
