/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;

public interface IPropertyFlavor extends IFilterFlavor {

	interface IProperty {

		IAttribute<?> getAttribute();

		Object getValue();
	}

	Stream<IProperty> getProperties();

	static IPropertyFlavor build(IAttribute<?> attribute, Object value, IItemCollection items) {
		return (IPropertyFlavor) SelectionFlavorAdapters
				.toUI(org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor.build(attribute, value, items));
	}

	static IPropertyFlavor buildPointRange(
		IAttribute<IQuantity> pointAttribute, IRange<IQuantity> range, IItemCollection items) {
		return (IPropertyFlavor) SelectionFlavorAdapters.toUI(
				org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor.buildPointRange(pointAttribute, range, items));
	}

	static IPropertyFlavor buildPointRange(
		String description, IAttribute<IQuantity> pointAttribute, IRange<IQuantity> range, IItemCollection items) {
		return (IPropertyFlavor) SelectionFlavorAdapters.toUI(org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor
				.buildPointRange(description, pointAttribute, range, items));
	}

	static IPropertyFlavor buildRange(
		IAttribute<IRange<IQuantity>> rangeAttribute, IRange<IQuantity> limit, IItemCollection items) {
		return (IPropertyFlavor) SelectionFlavorAdapters.toUI(
				org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor.buildRange(rangeAttribute, limit, items));
	}

	static IPropertyFlavor buildRange(
		String description, IAttribute<IRange<IQuantity>> rangeAttribute, IRange<IQuantity> limit,
		IItemCollection items) {
		return (IPropertyFlavor) SelectionFlavorAdapters.toUI(org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor
				.buildRange(description, rangeAttribute, limit, items));
	}

	static String getIntervalDescription(IAttribute<?> rangeAttribute, IRange<?> limit) {
		return org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor.getIntervalDescription(rangeAttribute, limit);
	}

	static IPropertyFlavor build(
		IItemFilter filter, IItemCollection items, IAttribute<?> attribute, Object value, String valueDesc) {
		return (IPropertyFlavor) SelectionFlavorAdapters.toUI(org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor
				.build(filter, items, attribute, value, valueDesc));
	}

	static IPropertyFlavor combine(Supplier<Stream<IPropertyFlavor>> flavors, IItemCollection items) {
		return (IPropertyFlavor) SelectionFlavorAdapters
				.toUI(org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor.combine(() -> flavors.get()
						.map(f -> (org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor) SelectionFlavorAdapters
								.toCore(f)),
						items));
	}

	static IPropertyFlavor build(
		String name, IItemFilter filter, IItemCollection sourceItems, Supplier<Stream<IProperty>> properties) {
		return (IPropertyFlavor) SelectionFlavorAdapters.toUI(org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor
				.build(name, filter, sourceItems, () -> properties.get().map(SelectionFlavorAdapters::toCore)));
	}

	static IProperty buildProperty(IAttribute<?> attribute, Object value) {
		return SelectionFlavorAdapters
				.toUI(org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor.buildProperty(attribute, value));
	}
}
