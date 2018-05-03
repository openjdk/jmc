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
package org.openjdk.jmc.common.test.item;

import static org.openjdk.jmc.common.item.Attribute.attr;
import static org.openjdk.jmc.common.unit.UnitLookup.BYTE;
import static org.openjdk.jmc.common.unit.UnitLookup.MEMORY;
import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.PersistableItemFilter;
import org.openjdk.jmc.common.test.MCTestCase;
import org.openjdk.jmc.common.unit.BinaryPrefix;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.util.StateToolkit;

@SuppressWarnings("nls")
public class ItemFilterTest extends MCTestCase {
	protected static final LinearUnit B = BYTE;
	protected static final LinearUnit MB = MEMORY.getUnit(BinaryPrefix.MEBI);

	protected static final boolean VERBOSE = Boolean.getBoolean("org.openjdk.jmc.common.test.item.verbose");

	protected static final IAttribute<IQuantity> LOWFIELD_ATTRIBUTE = attr("lowfield", null, MEMORY);
	protected static final IAttribute<IQuantity> HIGHFIELD_ATTRIBUTE = attr("highfield", null, MEMORY);
	protected static final IAttribute<IQuantity> RANGEFIELD_ATTRIBUTE = attr("rangefield", null, MEMORY);
	protected static final IAttribute<IQuantity> MAGICFIELD_ATTRIBUTE = attr("magicfield", null, MEMORY);
	protected static final IAttribute<IQuantity> SIZE_ATTRIBUTE = attr("size", null, MEMORY);
	protected static final IAttribute<String> NAME_ATTRIBUTE = attr("name", null, PLAIN_TEXT);

	@SafeVarargs
	protected static <T> Set<T> createSet(T ... values) {
		return new HashSet<>(Arrays.asList(values));
	}

	protected PersistableItemFilter createComparisionFilter() {
		IItemFilter lessFilter = ItemFilters.less(LOWFIELD_ATTRIBUTE, B.quantity(16));
		IItemFilter moreFilter = ItemFilters.more(HIGHFIELD_ATTRIBUTE, MB.quantity(4));
		IItemFilter xorFilter = ItemFilters.or(ItemFilters.and(lessFilter, ItemFilters.not(moreFilter)),
				ItemFilters.and(ItemFilters.not(lessFilter), moreFilter));
		IItemFilter rangeFilter = ItemFilters.interval(RANGEFIELD_ATTRIBUTE, B.quantity(32), true, B.quantity(64),
				false);
		return (PersistableItemFilter) ItemFilters.and(xorFilter, rangeFilter);
	}

	protected PersistableItemFilter createValueFilter() {
		IItemFilter exactFilter = ItemFilters.equals(SIZE_ATTRIBUTE, B.quantity(4711));
		Set<IQuantity> powerSet = new HashSet<>();
		for (int i = 128; i < 4096; i <<= 1) {
			powerSet.add(B.quantity(i));
		}
		IItemFilter powerFilter = ItemFilters.memberOf(SIZE_ATTRIBUTE, powerSet);
		IItemFilter sizeFilter = ItemFilters.or(exactFilter, powerFilter);
		IItemFilter trustedFilter = ItemFilters.memberOf(NAME_ATTRIBUTE, createSet("Alice", "Bob", "Carol", "Dave"));
		IItemFilter untrustedFilter = ItemFilters.equals(NAME_ATTRIBUTE, "Eve");
		IItemFilter trustFilter = ItemFilters.and(trustedFilter, ItemFilters.not(untrustedFilter));
		return (PersistableItemFilter) ItemFilters.or(sizeFilter, trustFilter);
	}

	protected PersistableItemFilter createTypeFilter() {
		IItemFilter fieldFilter = ItemFilters.hasAttribute(MAGICFIELD_ATTRIBUTE);
		IItemFilter oneTypeFilter = ItemFilters.type("onetype");
		IItemFilter someTypeFilter = ItemFilters.type(createSet("the-A-type", "the-B-type"));
		return (PersistableItemFilter) ItemFilters.and(fieldFilter, oneTypeFilter, someTypeFilter);
	}

	protected PersistableItemFilter createFilter() {
		return (PersistableItemFilter) ItemFilters.and(createComparisionFilter(), createValueFilter(),
				createTypeFilter());
	}

	protected String toString(IItemFilter filter) throws Exception {
		IWritableState writableState = StateToolkit.createWriter("filter");
		((PersistableItemFilter) filter).saveTo(writableState);
		return writableState.toString();
	}

	protected void assertPersists(IItemFilter filter) throws Exception {
		String document = StateToolkit.toXMLString((PersistableItemFilter) filter);
		if (VERBOSE) {
			System.out.println(document);
		}
		IItemFilter restoredFilter = PersistableItemFilter.readFrom(StateToolkit.fromXMLString(document));
		if (VERBOSE) {
			System.out.println("---");
			System.out.println(toString(restoredFilter));
		}
		assertEquals(toString(filter), toString(restoredFilter));
	}

	@Test
	public void testPersistingComparisions() throws Exception {
		assertPersists(createComparisionFilter());
	}

	@Test
	public void testPersistingValues() throws Exception {
		assertPersists(createValueFilter());
	}

	@Test
	public void testPersistingTypes() throws Exception {
		assertPersists(createTypeFilter());
	}

	@Test
	public void testPersistingValid() throws Exception {
		assertPersists(createFilter());
	}

}
