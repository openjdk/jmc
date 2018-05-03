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
package org.openjdk.jmc.flightrecorder.ui.selection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.RangeMatchPolicy;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.RangeContentType;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;

public interface IPropertyFlavor extends IFilterFlavor {

	interface IProperty {

		IAttribute<?> getAttribute();

		Object getValue();
	}

	Stream<IProperty> getProperties();

	@SuppressWarnings({"unchecked", "rawtypes"})
	static IPropertyFlavor build(IAttribute<?> attribute, Object value, IItemCollection items) {
		/*
		 * FIXME: We should really handle describing multiple values (to some max limit).
		 * 
		 * Event type is Lost Events or Flight Recording. See also
		 * ItemCollectionToolkit.getDescription. Maybe we should display it the same way as it's
		 * done in the Properties view.
		 */

		if (value instanceof Collection && ((Collection) value).size() == 1) {
			value = ((Collection) value).iterator().next();
		}
		if (value instanceof IRange) {
			ContentType<?> type = attribute.getContentType();
			if (type instanceof KindOfQuantity) {
				return buildPointRange((IAttribute<IQuantity>) attribute, (IRange<IQuantity>) value, items);
			} else if (type instanceof RangeContentType) {
				return buildRange((IAttribute<IRange<IQuantity>>) attribute, (IRange<IQuantity>) value, items);
			}
		}
		if (value instanceof Set) {
			if (attribute.equals(JfrAttributes.EVENT_TYPE)) {
				return build(
						ItemFilters.type((String[]) ((Set) value).stream().map(t -> ((IType<?>) t).getIdentifier())
								.toArray(String[]::new)),
						items, attribute, value,
						NLS.bind(Messages.FLAVOR_IS_IN_SET, attribute.getName(), ((Set<?>) value).size()));
			}
			return build(ItemFilters.memberOf(attribute, ((Set) value)), items, attribute, value,
					NLS.bind(Messages.FLAVOR_IS_IN_SET, attribute.getName(), ((Set<?>) value).size()));
		} else if (attribute.equals(JfrAttributes.EVENT_TYPE)) {
			return build(ItemFilters.type(((IType<?>) value).getIdentifier()), items, attribute, value,
					NLS.bind(Messages.FLAVOR_IS, attribute.getName(), TypeHandling.getValueString(value)));
		} else {
			return build(ItemFilters.equals((IAttribute) attribute, value), items, attribute, value,
					NLS.bind(Messages.FLAVOR_IS, attribute.getName(), TypeHandling.getValueString(value)));
		}
	}

	static IPropertyFlavor buildPointRange(
		IAttribute<IQuantity> pointAttribute, IRange<IQuantity> range, IItemCollection items) {
		return buildPointRange(getIntervalDescription(pointAttribute, range), pointAttribute, range, items);
	}

	static IPropertyFlavor buildPointRange(
		String description, IAttribute<IQuantity> pointAttribute, IRange<IQuantity> range, IItemCollection items) {
		IItemFilter interval = ItemFilters.interval(pointAttribute, range.getStart(), true, range.getEnd(), true);
		return build(interval, items, pointAttribute, range, description);
	}

	static IPropertyFlavor buildRange(
		IAttribute<IRange<IQuantity>> rangeAttribute, IRange<IQuantity> limit, IItemCollection items) {
		return buildRange(getIntervalDescription(rangeAttribute, limit), rangeAttribute, limit, items);
	}

	static IPropertyFlavor buildRange(
		String description, IAttribute<IRange<IQuantity>> rangeAttribute, IRange<IQuantity> limit,
		IItemCollection items) {
		IItemFilter interval = ItemFilters.matchRange(RangeMatchPolicy.CONTAINED_IN_CLOSED, rangeAttribute, limit);
		return build(interval, items, rangeAttribute, limit, description);
	}

	static String getIntervalDescription(IAttribute<?> rangeAttribute, IRange<?> limit) {
		return NLS.bind(Messages.FLAVOR_IS_IN_INTERVAL, rangeAttribute.getName(),
				limit.displayUsing(IDisplayable.AUTO));
	}

	static IPropertyFlavor build(
		IItemFilter filter, IItemCollection items, IAttribute<?> attribute, Object value, String valueDesc) {
		return build(valueDesc, filter, items, Arrays.asList(buildProperty(attribute, value))::stream);
	}

	static IPropertyFlavor combine(Supplier<Stream<IPropertyFlavor>> flavors, IItemCollection items) {
		IItemFilter filter = ItemFilters.and(flavors.get().map(IFilterFlavor::getFilter).toArray(IItemFilter[]::new));
		String name = null;
		for (String fName : flavors.get().map(IFilterFlavor::getName).collect(Collectors.toList())) {
			name = (name == null) ? fName : NLS.bind(Messages.FLAVOR_FILTER_AND_FILTER, name, fName);
		}
		return build(name, filter, items, () -> flavors.get().flatMap(IPropertyFlavor::getProperties));
	}

	static IPropertyFlavor build(
		String name, IItemFilter filter, IItemCollection sourceItems, Supplier<Stream<IProperty>> properties) {
		return new IPropertyFlavor() {

			@Override
			public String getName() {
				return name;
			}

			@Override
			public IItemCollection evaluate() {
				return sourceItems.apply(filter);
			}

			@Override
			public IItemFilter getFilter() {
				return filter;
			}

			@Override
			public Stream<IProperty> getProperties() {
				return properties.get();
			}
		};
	}

	static IProperty buildProperty(IAttribute<?> attribute, Object value) {
		return new IProperty() {

			@Override
			public IAttribute<?> getAttribute() {
				return attribute;
			}

			@Override
			public Object getValue() {
				return value;
			}

		};
	}

}
