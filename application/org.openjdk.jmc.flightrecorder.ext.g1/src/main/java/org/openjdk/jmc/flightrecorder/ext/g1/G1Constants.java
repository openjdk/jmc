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
package org.openjdk.jmc.flightrecorder.ext.g1;

import static org.openjdk.jmc.common.item.Attribute.attr;
import static org.openjdk.jmc.common.unit.UnitLookup.MEMORY;
import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;
import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;

import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;

@SuppressWarnings("nls")
public final class G1Constants {
	public static final IAttribute<String> WHEN = attr("when", "When", PLAIN_TEXT);
	public static final IAttribute<String> REGION_TYPE = attr("type", "Type", PLAIN_TEXT);
	public static final IAttribute<String> REGION_TO_TYPE = attr("to", "To Type", PLAIN_TEXT);
	public static final IAttribute<String> TYPE = Attribute
			.canonicalize(new Attribute<String>("(type)", "Type", "The type of the G1 Heap region", PLAIN_TEXT) {
				@Override
				public <U> IMemberAccessor<String, U> customAccessor(IType<U> type) {
					if (type.getIdentifier().equals(HEAP_REGION_INFORMATION)) {
						return REGION_TYPE.getAccessor(type);
					} else if (type.getIdentifier().equals(HEAP_REGION_TYPE_CHANGE)) {
						return REGION_TO_TYPE.getAccessor(type);
					}
					return null;
				}
			});
	public static final IAttribute<IQuantity> REGION_CAPACITY = attr("capacity", "Capacity", MEMORY);
	public static final IAttribute<IQuantity> REGION_USED = attr("used", "Used", MEMORY);
	public static final IAttribute<IQuantity> REGION_INDEX = attr("index", "Index", NUMBER);
	public static final IAttribute<IQuantity> REGION_ALLOC_CONTEXT = attr("allocContext", "Allocation Context", NUMBER);

	private static final String HEAP_REGION_TYPE_CHANGE = JdkTypeIDs.GC_G1_HEAP_REGION_TYPE_CHANGE;
	private static final String HEAP_REGION_INFORMATION = JdkTypeIDs.GC_G1_HEAP_REGION_INFORMATION;

	public static final IItemFilter HEAP_REGION_TYPE_CHANGES = ItemFilters.type(HEAP_REGION_TYPE_CHANGE);
	public static final IItemFilter HEAP_REGION_DUMPS = ItemFilters.type(HEAP_REGION_INFORMATION);
	public static final IItemFilter ALL_REGION_EVENTS = ItemFilters.or(HEAP_REGION_DUMPS, HEAP_REGION_TYPE_CHANGES);
}
