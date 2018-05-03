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
package org.openjdk.jmc.common.util;

import javax.management.openmbean.CompositeData;

import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.QuantityRange;

/**
 * Toolkit for working with {@link IMemberAccessor member accessors}. The methods in this class can
 * create accessors that perform various types of calculations based on the values retrieved by
 * other accessors.
 */
public class MemberAccessorToolkit {

	/**
	 * Create an accessor that subtracts values of one accessor from the values of another accessor.
	 * 
	 * @param minuend
	 *            accessor retrieving the value to subtract from
	 * @param subtrahend
	 *            accessor retrieving the value to subtract
	 * @return an accessor returning the difference between the input accessor values
	 */
	public static <T> IMemberAccessor<IQuantity, T> difference(
		final IMemberAccessor<IQuantity, T> minuend, final IMemberAccessor<IQuantity, T> subtrahend) {
		return new IMemberAccessor<IQuantity, T>() {

			@Override
			public IQuantity getMember(T i) {
				IQuantity mv = minuend.getMember(i);
				IQuantity sv = subtrahend.getMember(i);
				return (mv != null) && (sv != null) ? mv.subtract(sv) : null;
			}
		};
	}

	/**
	 * Create an accessor that adds the values of two accessors.
	 * 
	 * @param term1
	 *            accessor retrieving the first value to add
	 * @param term2
	 *            accessor retrieving the second value to add
	 * @return an accessor returning the sum of the input accessor values
	 */
	public static <T> IMemberAccessor<IQuantity, T> sum(
		final IMemberAccessor<IQuantity, T> term1, final IMemberAccessor<IQuantity, T> term2) {
		return new IMemberAccessor<IQuantity, T>() {

			@Override
			public IQuantity getMember(T i) {
				IQuantity v1 = term1.getMember(i);
				IQuantity v2 = term2.getMember(i);
				return (v1 != null) && (v2 != null) ? v1.add(v2) : null;
			}
		};
	}

	/**
	 * Create an accessor that calculates the average of the values from two accessors.
	 * 
	 * @param data1
	 *            accessor retrieving the first value
	 * @param data2
	 *            accessor retrieving the second value
	 * @return an accessor returning the average of the input accessor values
	 */
	public static <T> IMemberAccessor<IQuantity, T> avg(
		final IMemberAccessor<IQuantity, T> data1, final IMemberAccessor<IQuantity, T> data2) {
		return new IMemberAccessor<IQuantity, T>() {

			@Override
			public IQuantity getMember(T i) {
				IQuantity v1 = data1.getMember(i);
				IQuantity v2 = data2.getMember(i);
				return (v1 != null) && (v2 != null) ? v1.add(v2.subtract(v1).multiply(0.5)) : null;
			}
		};
	}

	/**
	 * Create an accessor that adds half of a delta value to a bias value. Useful for example to
	 * find a center time if the bias is a start time and the delta is the duration of an event.
	 * 
	 * @param bias
	 *            accessor retrieving the bias value
	 * @param delta
	 *            accessor retrieving the delta value
	 * @return an accessor returning the the bias value plus half of the delta value
	 */
	public static <T> IMemberAccessor<IQuantity, T> addHalfDelta(
		final IMemberAccessor<IQuantity, T> bias, final IMemberAccessor<IQuantity, T> delta) {
		return new IMemberAccessor<IQuantity, T>() {

			@Override
			public IQuantity getMember(T i) {
				IQuantity v1 = bias.getMember(i);
				IQuantity v2 = delta.getMember(i);
				return (v1 != null) && (v2 != null) ? v1.add(v2.multiply(0.5)) : null;
			}
		};
	}

	/**
	 * Create an accessor that subtracts half of a delta value from a bias value. Useful for example
	 * to find a center time if the bias is an end time and the delta is the duration of an event.
	 * 
	 * @param bias
	 *            accessor retrieving the bias value
	 * @param delta
	 *            accessor retrieving the delta value
	 * @return an accessor returning the the bias value minus half of the delta value
	 */
	public static <T> IMemberAccessor<IQuantity, T> subtractHalfDelta(
		final IMemberAccessor<IQuantity, T> bias, final IMemberAccessor<IQuantity, T> delta) {
		return new IMemberAccessor<IQuantity, T>() {

			@Override
			public IQuantity getMember(T i) {
				IQuantity v1 = bias.getMember(i);
				IQuantity v2 = delta.getMember(i);
				return (v1 != null) && (v2 != null) ? v1.subtract(v2.multiply(0.5)) : null;
			}
		};
	}

	/**
	 * Create an accessor that constructs ranges based on start and end values.
	 * 
	 * @param start
	 *            accessor retrieving the start value
	 * @param end
	 *            accessor retrieving the end value
	 * @return an accessor returning ranges based on the start and end values
	 */
	public static <T> IMemberAccessor<IRange<IQuantity>, T> rangeWithEnd(
		final IMemberAccessor<IQuantity, T> start, final IMemberAccessor<IQuantity, T> end) {
		return new IMemberAccessor<IRange<IQuantity>, T>() {

			@Override
			public IRange<IQuantity> getMember(T i) {
				IQuantity vStart = start.getMember(i);
				IQuantity vEnd = end.getMember(i);
				return (vStart != null) && (vEnd != null) ? QuantityRange.createWithEnd(vStart, vEnd) : null;
			}
		};
	}

	/**
	 * Create an accessor that constructs ranges based on start and extent values.
	 * 
	 * @param start
	 *            accessor retrieving the start value
	 * @param extent
	 *            accessor retrieving the range extent
	 * @return an accessor returning ranges based on the start and extent values
	 */
	public static <T> IMemberAccessor<IRange<IQuantity>, T> rangeWithExtent(
		final IMemberAccessor<IQuantity, T> start, final IMemberAccessor<IQuantity, T> extent) {
		return new IMemberAccessor<IRange<IQuantity>, T>() {

			@Override
			public IRange<IQuantity> getMember(T i) {
				IQuantity vStart = start.getMember(i);
				@SuppressWarnings("unchecked")
				ITypedQuantity<LinearUnit> vExtent = (ITypedQuantity<LinearUnit>) extent.getMember(i);
				return (vStart != null) && (vExtent != null) ? QuantityRange.createWithExtent(vStart, vExtent) : null;
			}
		};
	}

	/**
	 * Create an accessor that constructs ranges based on end and extent values.
	 * 
	 * @param extent
	 *            accessor retrieving the range extent
	 * @param end
	 *            accessor retrieving the end value
	 * @return an accessor returning ranges based on the end and extent values
	 */
	public static <T> IMemberAccessor<IRange<IQuantity>, T> rangeWithExtentEnd(
		final IMemberAccessor<IQuantity, T> extent, final IMemberAccessor<IQuantity, T> end) {
		return new IMemberAccessor<IRange<IQuantity>, T>() {

			@Override
			public IRange<IQuantity> getMember(T i) {
				@SuppressWarnings("unchecked")
				ITypedQuantity<LinearUnit> vExtent = (ITypedQuantity<LinearUnit>) extent.getMember(i);
				IQuantity vEnd = end.getMember(i);
				return (vEnd != null) && (vExtent != null)
						? QuantityRange.createWithExtent(vEnd.subtract(vExtent), vExtent) : null;
			}
		};
	}

	/**
	 * Create an accessor that constructs point ranges (ranges with zero extent) based on point
	 * values.
	 * 
	 * @param point
	 *            accessor retrieving the point position values
	 * @return an accessor returning point ranges
	 */
	public static <T> IMemberAccessor<IRange<IQuantity>, T> pointRange(final IMemberAccessor<IQuantity, T> point) {
		return new IMemberAccessor<IRange<IQuantity>, T>() {

			@Override
			public IRange<IQuantity> getMember(T i) {
				IQuantity value = point.getMember(i);
				return (value != null) ? QuantityRange.createPoint(value) : null;
			}
		};
	}

	/**
	 * Create an accessor that always return the same value.
	 * 
	 * @param value
	 *            constant value to return
	 * @return an accessor returning the constant value
	 */
	public static <T, M, V extends M> IMemberAccessor<M, T> constant(final V value) {
		return new IMemberAccessor<M, T>() {

			@Override
			public M getMember(T inObject) {
				return value;
			}
		};
	}

	/**
	 * Create an accessor that returns a fixed element index from arrays.
	 * 
	 * @param index
	 *            array index to return
	 * @return an accessor returning the object at the specified index in an array
	 */
	public static IMemberAccessor<?, Object[]> arrayElement(final int index) {
		return new IMemberAccessor<Object, Object[]>() {

			@Override
			public Object getMember(Object[] object) {
				return object[index];
			}
		};
	}

	/**
	 * Create an accessor that returns a specified value from {@link CompositeData} instances.
	 * 
	 * @param key
	 *            key of the value to get
	 * @return an accessor returning the value associated with the specified key
	 */
	public static IMemberAccessor<?, CompositeData> compositeElement(final String key) {
		return new IMemberAccessor<Object, CompositeData>() {

			@Override
			public Object getMember(CompositeData inObject) {
				return inObject.get(key);
			}
		};
	}

}
