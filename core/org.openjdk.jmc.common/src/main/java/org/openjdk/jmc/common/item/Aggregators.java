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
package org.openjdk.jmc.common.item;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openjdk.jmc.common.IPredicate;
import org.openjdk.jmc.common.messages.internal.Messages;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.PredicateToolkit;
import org.openjdk.jmc.common.util.StringToolkit;

public class Aggregators {

	public static abstract class AggregatorBase<V, C extends IItemConsumer<C>> implements IAggregator<V, C> {
		private final String name;
		private final String description;
		private final IType<? super V> ct;

		public AggregatorBase(String name, String description, IType<? super V> ct) {
			this.name = name;
			this.description = description;
			this.ct = ct;
		}

		@Override
		public IType<? super V> getValueType() {
			return ct;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getDescription() {
			return description;
		}

	}

	public static abstract class MergingAggregator<V, C extends IItemConsumer<C>> extends AggregatorBase<V, C> {

		public MergingAggregator(String name, String description, IType<? super V> ct) {
			super(name, description, ct);
		}

		// FIXME: consumers can have different types, they should maybe not be merged?
		// FIXME: Who should be responsible for this merge?
		@Override
		public V getValue(Iterator<C> consumers) {
			if (consumers.hasNext()) {
				C consumer = consumers.next();
				while (consumers.hasNext()) {
					C next = consumers.next();
					consumer = consumer.merge(next);
				}
				return getValue(consumer);
			}
			return null;
		}

		public abstract V getValue(C consumer);

	}

	public static abstract class FieldAggregatorBase<V, C extends IItemConsumer<C>> extends MergingAggregator<V, C> {

		FieldAggregatorBase(String name, String description, IType<V> ct) {
			super(name, description, ct);
		}

		@Override
		public boolean acceptType(IType<IItem> type) {
			IMemberAccessor<? extends V, IItem> a = doGetAccessor(type);
			return a != null;
		}

		protected abstract IMemberAccessor<? extends V, IItem> doGetAccessor(IType<IItem> type);

		IMemberAccessor<? extends V, IItem> getAccessor(IType<IItem> type) {
			IMemberAccessor<? extends V, IItem> ma = doGetAccessor(type);
			if (ma != null) {
				return ma;
			}
			throw new IllegalArgumentException("IAggregator.acceptType must be called before newValueConsumer"); //$NON-NLS-1$
		}
	}

	private static abstract class QuantityConsumer<C> implements IItemConsumer<C> {

		IMemberAccessor<? extends IQuantity, IItem> accessor;

		QuantityConsumer(IMemberAccessor<? extends IQuantity, IItem> accessor) {
			this.accessor = accessor;
		}
	}

	private static class SumConsumer extends QuantityConsumer<SumConsumer> {

		double sum = 0.0;
		IUnit unit = null;

		SumConsumer(IMemberAccessor<? extends IQuantity, IItem> accessor) {
			super(accessor);
		}

		@Override
		public void consume(IItem item) {
			IQuantity fieldValue = accessor.getMember(item);
			if (unit == null) {
				unit = fieldValue.getUnit();
			}
			sum += fieldValue.doubleValueIn(unit);
		}

		@Override
		public SumConsumer merge(SumConsumer other) {
			// FIXME: Should we create a new instance, or is it OK to modify consumers like this?
			if (unit != null) {
				if (other.unit != null) {
					sum += other.unit.valueTransformTo(unit).targetValue(other.sum);
				}
				return this;
			}
			return other;
		}

	}

	public static abstract class Sum extends FieldAggregatorBase<IQuantity, SumConsumer> {

		public Sum(String name, String description, LinearKindOfQuantity ct) {
			super(name, description, ct);
		}

		@Override
		public SumConsumer newItemConsumer(IType<IItem> type) {
			return new SumConsumer(getAccessor(type));
		}

		@Override
		public IQuantity getValue(SumConsumer consumer) {
			return consumer.unit != null ? consumer.unit.quantity(consumer.sum) : null;
		}

	}

	public static abstract class Variance extends FieldAggregatorBase<IQuantity, VarianceConsumer> {
		private final boolean besselCorrection;

		public Variance(String name, String description, LinearKindOfQuantity ct, boolean besselCorrection) {
			super(name, description, ct);
			this.besselCorrection = besselCorrection;
		}

		@Override
		public VarianceConsumer newItemConsumer(IType<IItem> type) {
			return new VarianceConsumer(getAccessor(type));
		}

		@Override
		public IQuantity getValue(VarianceConsumer consumer) {
			if (consumer.unit == null) {
				return null;
			}
			Number variance = consumer.getVariance(besselCorrection);
			return variance == null ? null : consumer.unit.quantity(variance);
		}
	}

	public static abstract class Stddev extends FieldAggregatorBase<IQuantity, VarianceConsumer> {
		private final boolean besselCorrection;

		public Stddev(String name, String description, LinearKindOfQuantity ct, boolean besselCorrection) {
			super(name, description, ct);
			this.besselCorrection = besselCorrection;
		}

		@Override
		public VarianceConsumer newItemConsumer(IType<IItem> type) {
			return new VarianceConsumer(getAccessor(type));
		}

		@Override
		public IQuantity getValue(VarianceConsumer consumer) {
			if (consumer.unit == null) {
				return null;
			}
			Number stddev = consumer.getStddev(besselCorrection);
			return stddev == null ? null : consumer.unit.quantity(stddev);
		}
	}

	/**
	 * Consumer for calculating stddev and variance in a one pass, numerically stable way.
	 */
	public static class VarianceConsumer extends QuantityConsumer<VarianceConsumer> {
		public long n = 0;
		public double mean = 0.0;
		public double M2 = 0.0;
		public IUnit unit = null;

		public VarianceConsumer(IMemberAccessor<? extends IQuantity, IItem> accessor) {
			super(accessor);
		}

		public Number getVariance(boolean besselCorrection) {
			long divisor = besselCorrection ? n - 1 : n;
			if (divisor < 0) {
				return null;
			}
			if (divisor == 0) {
				return besselCorrection ? null : 0;
			}
			return M2 / divisor;
		}

		public Number getStddev(boolean besselCorrection) {
			Number variance = getVariance(besselCorrection);
			if (variance == null) {
				return null;
			}
			return Math.sqrt(variance.doubleValue());
		}

		@Override
		public void consume(IItem item) {
			IQuantity fieldValue = accessor.getMember(item);
			n++;
			if (fieldValue == null) {
				return;
			}
			if (unit == null) {
				unit = fieldValue.getUnit();
			}
			double x = fieldValue.doubleValueIn(unit);
			double delta = x - mean;
			mean = mean + delta / n;
			M2 = M2 + delta * (x - mean);
		}

		@Override
		public VarianceConsumer merge(VarianceConsumer other) {
			if (unit == null) {
				unit = other.unit;
			}
			if (unit != null) {
				if (other.unit != null) {
					/*
					 * Since the Knuth algorithm (Chan et. al) can be used with partitioned sets, we
					 * can combine the results from two aggregators with partial results like below.
					 */
					double otherMean = other.unit.valueTransformTo(unit).targetValue(other.mean);
					double otherM2 = other.unit.valueTransformTo(unit).targetValue(other.M2);
					double deltaMean = otherMean - mean;
					mean = mean + deltaMean * (other.n / (double) (other.n + n));
					M2 = otherM2 + M2 + deltaMean * deltaMean * (other.n * n / (double) (n + other.n));
					n += other.n;
				}
				return this;
			}
			return other;
		}
	}

	// FIXME: Would like to extend SumConsumer, but this currently causes generics problems
	public static class AvgConsumer extends QuantityConsumer<AvgConsumer> {

		public double sum = 0.0;
		public IUnit unit = null;
		public int count = 0;

		public AvgConsumer(IMemberAccessor<? extends IQuantity, IItem> accessor) {
			super(accessor);
		}

		@Override
		public void consume(IItem item) {
			IQuantity fieldValue = accessor.getMember(item);
			if (fieldValue == null) {
				count++;
				return;
			}
			if (unit == null) {
				unit = fieldValue.getUnit();
			}
			sum += fieldValue.doubleValueIn(unit);
			count++;
		}

		@Override
		public AvgConsumer merge(AvgConsumer other) {
			if (unit == null) {
				unit = other.unit;
			}
			if (unit != null) {
				if (other.unit != null) {
					sum += other.unit.valueTransformTo(unit).targetValue(other.sum);
					count += other.count;
				}
				return this;
			}
			return other;
		}

	}

	public static abstract class Avg extends FieldAggregatorBase<IQuantity, AvgConsumer> {

		public Avg(String name, String description, ContentType<IQuantity> ct) {
			super(name, description, ct);
		}

		@Override
		public AvgConsumer newItemConsumer(IType<IItem> type) {
			return new AvgConsumer(getAccessor(type));
		}

		@Override
		public IQuantity getValue(AvgConsumer consumer) {
			return consumer.unit == null ? null : consumer.unit.quantity(consumer.sum / consumer.count);
		}
	}

	public static class MinMaxConsumer<V extends Comparable<V>> implements IItemConsumer<MinMaxConsumer<V>> {

		private final IMemberAccessor<? extends V, IItem> accessor;
		private final boolean max;
		private V value;
		private IItem item;

		public MinMaxConsumer(IMemberAccessor<? extends V, IItem> accessor, boolean max) {
			this.accessor = accessor;
			this.max = max;
		}

		@Override
		public void consume(IItem item) {
			add(accessor.getMember(item), item);
		}

		// FIXME: "add" is not an ideal name for this method, rename to something better
		private void add(V newValue, IItem newItem) {
			if (newValue != null && (value == null || newValue.compareTo(value) > 0 == max)) {
				value = newValue;
				item = newItem;
			}
		}

		@Override
		public MinMaxConsumer<V> merge(MinMaxConsumer<V> other) {
			add(other.value, other.item);
			return this;
		}
	}

	public static abstract class MinMax<V extends Comparable<V>> extends FieldAggregatorBase<V, MinMaxConsumer<V>> {
		private final boolean max;

		MinMax(String name, String description, ContentType<V> ct, boolean max) {
			super(name, description, ct);
			this.max = max;
		}

		@Override
		public MinMaxConsumer<V> newItemConsumer(IType<IItem> type) {
			return new MinMaxConsumer<>(getAccessor(type), max);
		}

		@Override
		public V getValue(MinMaxConsumer<V> consumer) {
			return consumer.value;
		}
	}

	public static class CountConsumer implements IItemConsumer<CountConsumer> {

		private int count = 0;

		@Override
		public void consume(IItem item) {
			count++;
		}

		@Override
		public CountConsumer merge(CountConsumer other) {
			count += other.count;
			return this;
		}

		public int getCount() {
			return count;
		}

	}

	private static class FilterConsumer<C extends IItemConsumer<C>> implements IItemConsumer<FilterConsumer<C>> {

		private final IPredicate<IItem> p;
		private final C nestedConsumer;

		public FilterConsumer(IPredicate<IItem> p, C nestedConsumer) {
			this.p = p;
			this.nestedConsumer = nestedConsumer;
		}

		@Override
		public void consume(IItem item) {
			if (p.evaluate(item)) {
				nestedConsumer.consume(item);
			}
		}

		@Override
		public FilterConsumer<C> merge(FilterConsumer<C> other) {
			nestedConsumer.merge(other.nestedConsumer);
			return this;
		}
	}

	private static class Count extends MergingAggregator<IQuantity, CountConsumer> {

		Count(String name, String description) {
			super(name, description, UnitLookup.NUMBER);
		}

		@Override
		public boolean acceptType(IType<IItem> type) {
			return true;
		}

		@Override
		public CountConsumer newItemConsumer(IType<IItem> type) {
			return new CountConsumer();
		}

		@Override
		public IQuantity getValue(CountConsumer consumer) {
			return UnitLookup.NUMBER_UNITY.quantity(consumer.count);
		}
	}

	private static final Count COUNT = new Count(Messages.getString(Messages.ItemAggregate_COUNT), null);

	private static class AndOrConsumer implements IItemConsumer<AndOrConsumer> {
		boolean and;
		Boolean b;
		IMemberAccessor<? extends Boolean, IItem> accessor;

		public AndOrConsumer(IMemberAccessor<? extends Boolean, IItem> accessor, boolean and) {
			this.and = and;
			this.accessor = accessor;
		}

		@Override
		public void consume(IItem item) {
			evaluate(accessor.getMember(item));
		}

		void evaluate(Boolean value) {
			if (b == null) {
				b = value;
			} else if (value != null) {
				b = and ? b && value : b || value;
			}
		}

		@Override
		public AndOrConsumer merge(AndOrConsumer other) {
			evaluate(other.b);
			return this;
		}

	}

	private static abstract class AndOr extends FieldAggregatorBase<Boolean, AndOrConsumer> {

		public AndOr(String name, String description, IType<Boolean> ct) {
			super(name, description, ct);
		}

		@Override
		public Boolean getValue(AndOrConsumer consumer) {
			return consumer.b;
		}

	}

	private static <V extends Comparable<V>> IAggregator<IItem, ?> minMaxItem(
		String name, final IAttribute<V> attribute, boolean max) {
		return new MergingAggregator<IItem, MinMaxConsumer<V>>("Item with " + name, null, UnitLookup.UNKNOWN) { //$NON-NLS-1$

			@Override
			public boolean acceptType(IType<IItem> type) {
				return attribute.getAccessor(type) != null;
			}

			@Override
			public MinMaxConsumer<V> newItemConsumer(IType<IItem> type) {
				return new MinMaxConsumer<>(attribute.getAccessor(type), true);
			}

			@Override
			public IItem getValue(MinMaxConsumer<V> consumer) {
				return consumer.item;
			}

		};
	}

	public static <V extends Comparable<V>> IAggregator<IItem, ?> itemWithMin(IAttribute<V> attribute) {
		String name = getMinName(attribute.getName(), attribute.getContentType());
		return minMaxItem(name, attribute, false);
	}

	public static <V extends Comparable<V>> IAggregator<IItem, ?> itemWithMax(IAttribute<V> attribute) {
		String name = getMaxName(attribute.getName(), attribute.getContentType());
		return minMaxItem(name, attribute, true);
	}

	public static <V> IAggregator<V, ?> filter(IAggregator<V, ?> aggregator, IItemFilter filter) {
		return filter(aggregator.getName(), aggregator.getDescription(), aggregator, filter);
	}

	public static <V, C extends IItemConsumer<C>> IAggregator<V, ?> filter(
		String name, String description, final IAggregator<V, C> aggregator, final IItemFilter filter) {
		return new AggregatorBase<V, FilterConsumer<C>>(name, description, aggregator.getValueType()) {

			@Override
			public boolean acceptType(IType<IItem> type) {
				return aggregator.acceptType(type) && !PredicateToolkit.isFalseGuaranteed(filter.getPredicate(type));
			}

			@Override
			public FilterConsumer<C> newItemConsumer(IType<IItem> type) {
				return new FilterConsumer<>(filter.getPredicate(type), aggregator.newItemConsumer(type));
			}

			@Override
			public V getValue(final Iterator<FilterConsumer<C>> consumers) {
				return aggregator.getValue(new Iterator<C>() {

					@Override
					public boolean hasNext() {
						return consumers.hasNext();
					}

					@Override
					public C next() {
						return consumers.next().nestedConsumer;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				});
			}
		};
	}

	public static IAggregator<IQuantity, ?> sum(final IAttribute<IQuantity> attribute) {
		ContentType<?> contentType = attribute.getContentType();
		if (contentType instanceof LinearKindOfQuantity) {
			return new Sum(getSumName(attribute.getName()), attribute.getDescription(),
					(LinearKindOfQuantity) contentType) {

				@Override
				protected IMemberAccessor<IQuantity, IItem> doGetAccessor(IType<IItem> type) {
					return attribute.getAccessor(type);
				}

			};
		}
		throw new IllegalArgumentException("Can only use LinearKindOfQuantity"); //$NON-NLS-1$
	}

	/**
	 * Calculates the sample variance for a linear quantity attribute.
	 *
	 * @param attribute
	 *            the attribute to calculate the sample variance for
	 * @return the variance for the attribute
	 */
	public static IAggregator<IQuantity, ?> variance(final IAttribute<IQuantity> attribute) {
		return varianceInternal(attribute, true);
	}

	/**
	 * Calculates the population variance for a linear quantity attribute.
	 *
	 * @param attribute
	 *            the attribute to calculate the population variance for
	 * @return the variance for the attribute
	 */
	public static IAggregator<IQuantity, ?> variancep(final IAttribute<IQuantity> attribute) {
		return varianceInternal(attribute, false);
	}

	private static IAggregator<IQuantity, ?> varianceInternal(
		final IAttribute<IQuantity> attribute, boolean besselCorrection) {
		ContentType<?> contentType = attribute.getContentType();
		if (contentType instanceof LinearKindOfQuantity) {
			return new Variance(getVarianceName(attribute.getName(), besselCorrection), attribute.getDescription(),
					(LinearKindOfQuantity) contentType, besselCorrection) {
				@Override
				protected IMemberAccessor<IQuantity, IItem> doGetAccessor(IType<IItem> type) {
					return attribute.getAccessor(type);
				}
			};
		}
		throw new IllegalArgumentException("Can only use LinearKindOfQuantity"); //$NON-NLS-1$
	}

	/**
	 * Calculates the sample standard deviation for a linear quantity attribute.
	 *
	 * @param attribute
	 *            the attribute to calculate the sample standard deviation for
	 * @return the standard deviation for the attribute
	 */
	public static IAggregator<IQuantity, ?> stddev(final IAttribute<IQuantity> attribute) {
		return stddevInternal(attribute, true);
	}

	/**
	 * Calculates the sample standard deviation for a linear quantity attribute.
	 *
	 * @param name
	 *            aggregator name
	 * @param description
	 *            aggregator description
	 * @param attribute
	 *            the attribute to calculate the sample standard deviation for
	 * @return an aggregator that calculates the standard deviation for the attribute
	 */
	public static IAggregator<IQuantity, ?> stddev(
		String name, String description, final IAttribute<IQuantity> attribute) {
		return stddevInternal(name, description, attribute, true);
	}

	/**
	 * Calculates the population standard deviation for a linear quantity attribute.
	 *
	 * @param attribute
	 *            the attribute to calculate the population standard deviation for
	 * @return an aggregator that calculates the standard deviation for the attribute
	 */
	public static IAggregator<IQuantity, ?> stddevp(final IAttribute<IQuantity> attribute) {
		return stddevInternal(attribute, false);
	}

	/**
	 * Calculates the population standard deviation for a linear quantity attribute.
	 *
	 * @param name
	 *            aggregator name
	 * @param description
	 *            aggregator description
	 * @param attribute
	 *            the attribute to calculate the population standard deviation for
	 * @return an aggregator that calculates the standard deviation for the attribute
	 */
	public static IAggregator<IQuantity, ?> stddevp(
		String name, String description, final IAttribute<IQuantity> attribute) {
		return stddevInternal(name, description, attribute, false);
	}

	private static IAggregator<IQuantity, ?> stddevInternal(
		final IAttribute<IQuantity> attribute, boolean besselCorrection) {
		return stddevInternal(getStddevName(attribute.getName(), besselCorrection), attribute.getDescription(),
				attribute, besselCorrection);
	}

	private static IAggregator<IQuantity, ?> stddevInternal(
		String name, String description, final IAttribute<IQuantity> attribute, boolean besselCorrection) {
		ContentType<?> contentType = attribute.getContentType();
		if (contentType instanceof LinearKindOfQuantity) {
			return new Stddev(name, description, (LinearKindOfQuantity) contentType, besselCorrection) {
				@Override
				protected IMemberAccessor<IQuantity, IItem> doGetAccessor(IType<IItem> type) {
					return attribute.getAccessor(type);
				}
			};
		}
		throw new IllegalArgumentException("Can only use LinearKindOfQuantity"); //$NON-NLS-1$
	}

	public static IAggregator<IQuantity, ?> sum(final String typeId, final IAttribute<IQuantity> attribute) {
		return sum(getSumName(attribute.getName()), null, typeId, attribute);
	}

	public static IAggregator<IQuantity, ?> sum(String name, String description, IAttribute<IQuantity> attribute) {
		ContentType<IQuantity> contentType = attribute.getContentType();
		if (contentType instanceof LinearKindOfQuantity) {
			return sum(name, description, (LinearKindOfQuantity) contentType, attribute);
		}
		throw new IllegalArgumentException("Can only use LinearKindOfQuantity"); //$NON-NLS-1$
	}

	public static IAggregator<IQuantity, ?> sum(
		String name, String description, final String typeId, final IAttribute<IQuantity> attribute) {
		ContentType<IQuantity> contentType = attribute.getContentType();
		if (contentType instanceof LinearKindOfQuantity) {
			return new Sum(name, description, (LinearKindOfQuantity) contentType) {

				@Override
				protected IMemberAccessor<IQuantity, IItem> doGetAccessor(IType<IItem> type) {
					if (type.getIdentifier().equals(typeId)) {
						return attribute.getAccessor(type);
					}
					return null;
				}

			};
		}
		throw new IllegalArgumentException("Can only use LinearKindOfQuantity"); //$NON-NLS-1$
	}

	public static IAggregator<IQuantity, ?> sum(
		String name, String description, LinearKindOfQuantity ct, final IAccessorFactory<IQuantity> af) {
		return new Sum(name, description, ct) {

			@Override
			protected IMemberAccessor<? extends IQuantity, IItem> doGetAccessor(IType<IItem> type) {
				return af.getAccessor(type);
			}

		};
	}

	public static IAggregator<IQuantity, ?> avg(final IAttribute<IQuantity> attribute) {
		return new Avg(getAvgName(attribute.getName()), attribute.getDescription(), attribute.getContentType()) {

			@Override
			protected IMemberAccessor<IQuantity, IItem> doGetAccessor(IType<IItem> type) {
				return attribute.getAccessor(type);
			}

		};
	}

	public static IAggregator<IQuantity, ?> avg(final String typeId, final IAttribute<IQuantity> attribute) {
		return avg(getAvgName(attribute.getName()), null, typeId, attribute);
	}

	public static IAggregator<IQuantity, ?> avg(String name, String description, IAttribute<IQuantity> attribute) {
		ContentType<?> contentType = attribute.getContentType();
		if (contentType instanceof KindOfQuantity) {
			return avg(name, description, (KindOfQuantity<?>) contentType, attribute);
		}
		throw new IllegalArgumentException("Can only use KindOfQuantity"); //$NON-NLS-1$
	}

	public static IAggregator<IQuantity, ?> avg(
		String name, String description, final String typeId, final IAttribute<IQuantity> attribute) {
		ContentType<?> contentType = attribute.getContentType();
		if (contentType instanceof KindOfQuantity) {
			return new Avg(name, description, (KindOfQuantity<?>) contentType) {

				@Override
				protected IMemberAccessor<IQuantity, IItem> doGetAccessor(IType<IItem> type) {
					if (type.getIdentifier().equals(typeId)) {
						return attribute.getAccessor(type);
					}
					return null;
				}

			};
		}
		throw new IllegalArgumentException("Can only use KindOfQuantity"); //$NON-NLS-1$
	}

	public static IAggregator<IQuantity, ?> avg(
		String name, String description, KindOfQuantity<?> ct, final IAccessorFactory<IQuantity> af) {
		return new Avg(name, description, ct) {

			@Override
			protected IMemberAccessor<? extends IQuantity, IItem> doGetAccessor(IType<IItem> type) {
				return af.getAccessor(type);
			}

		};
	}

	public static <V extends Comparable<V>> IAggregator<V, ?> min(final IAttribute<V> attribute) {
		String name = getMinName(attribute.getName(), attribute.getContentType());
		return new MinMax<V>(name, attribute.getDescription(), attribute.getContentType(), false) {

			@Override
			protected IMemberAccessor<V, IItem> doGetAccessor(IType<IItem> type) {
				return attribute.getAccessor(type);
			}

		};
	}

	public static <V extends Comparable<V>> IAggregator<V, ?> min(final String typeId, final IAttribute<V> attribute) {
		String name = getMinName(attribute.getName(), attribute.getContentType());
		return min(name, null, typeId, attribute);
	}

	public static <V extends Comparable<V>> IAggregator<V, ?> min(
		String name, String description, final String typeId, final IAttribute<V> attribute) {
		ContentType<V> contentType = attribute.getContentType();
		return new MinMax<V>(name, description, contentType, false) {

			@Override
			protected IMemberAccessor<V, IItem> doGetAccessor(IType<IItem> type) {
				if (type.getIdentifier().equals(typeId)) {
					return attribute.getAccessor(type);
				}
				return null;
			}

		};
	}

	public static <V extends Comparable<V>> IAggregator<V, ?> max(final IAttribute<V> attribute) {
		String name = getMaxName(attribute.getName(), attribute.getContentType());
		return new MinMax<V>(name, attribute.getDescription(), attribute.getContentType(), true) {

			@Override
			protected IMemberAccessor<V, IItem> doGetAccessor(IType<IItem> type) {
				return attribute.getAccessor(type);
			}

		};
	}

	public static IAggregator<IQuantity, ?> max(final String typeId, final IAttribute<IQuantity> attribute) {
		String name = getMaxName(attribute.getName(), attribute.getContentType());
		return max(name, null, typeId, attribute);
	}

	public static <V extends Comparable<V>> IAggregator<V, ?> max(
		String name, String description, final IAttribute<V> attribute) {
		ContentType<V> contentType = attribute.getContentType();
		return new MinMax<V>(name, description, contentType, true) {

			@Override
			protected IMemberAccessor<V, IItem> doGetAccessor(IType<IItem> type) {
				return attribute.getAccessor(type);
			}
		};
	}

	public static <V extends Comparable<V>> IAggregator<V, ?> max(
		String name, String description, final String typeId, final IAttribute<V> attribute) {
		ContentType<V> contentType = attribute.getContentType();
		return new MinMax<V>(name, description, contentType, true) {

			@Override
			protected IMemberAccessor<V, IItem> doGetAccessor(IType<IItem> type) {
				if (type.getIdentifier().equals(typeId)) {
					return attribute.getAccessor(type);
				}
				return null;
			}
		};
	}

	public static IAggregator<IQuantity, CountConsumer> count() {
		return COUNT;
	}

	public static IAggregator<IQuantity, CountConsumer> count(String name, String description) {
		return new Count(name, description);
	}

	public static IAggregator<IQuantity, ?> count(IType<?> type) {
		return filter(getCountName(type), null, COUNT, ItemFilters.type(type.getIdentifier()));
	}

	public static IAggregator<IQuantity, ?> count(IItemFilter filter) {
		return filter(COUNT, filter);
	}

	public static IAggregator<IQuantity, ?> count(String name, String description, IItemFilter filter) {
		return filter(name, description, COUNT, filter);
	}

	public static IAggregator<Boolean, ?> and(final String typeId, final IAttribute<Boolean> attribute) {
		return new AndOr(attribute.getName(), attribute.getDescription(), UnitLookup.FLAG) {

			@Override
			public AndOrConsumer newItemConsumer(IType<IItem> type) {
				return new AndOrConsumer(attribute.getAccessor(type), true);
			}

			@Override
			public boolean acceptType(IType<IItem> type) {
				return type.getIdentifier().equals(typeId);
			}

			@Override
			protected IMemberAccessor<Boolean, IItem> doGetAccessor(IType<IItem> type) {
				if (type.getIdentifier().equals(typeId)) {
					return attribute.getAccessor(type);
				}
				return null;
			}
		};
	}

	public static IAggregator<Boolean, ?> or(final String typeId, final IAttribute<Boolean> attribute) {
		return new AndOr(attribute.getName(), attribute.getDescription(), UnitLookup.FLAG) {

			@Override
			public AndOrConsumer newItemConsumer(IType<IItem> type) {
				return new AndOrConsumer(attribute.getAccessor(type), false);
			}

			@Override
			public boolean acceptType(IType<IItem> type) {
				return type.getIdentifier().equals(typeId);
			}

			@Override
			protected IMemberAccessor<Boolean, IItem> doGetAccessor(IType<IItem> type) {
				if (type.getIdentifier().equals(typeId)) {
					return attribute.getAccessor(type);
				}
				return null;
			}
		};
	}

	public static class SetConsumer<T> implements IItemConsumer<SetConsumer<T>> {
		Set<T> distinct = new HashSet<>();
		private final IMemberAccessor<? extends T, IItem> accessor;

		public SetConsumer(IMemberAccessor<? extends T, IItem> accessor) {
			this.accessor = accessor;
		}

		@Override
		public void consume(IItem item) {
			T member = accessor.getMember(item);
			if (member != null) {
				distinct.add(member);
			}
		}

		@Override
		public SetConsumer<T> merge(SetConsumer<T> other) {
			distinct.addAll(other.distinct);
			return this;
		}
	}

	private abstract static class SetAggregator<V, T> extends MergingAggregator<V, SetConsumer<T>> {

		private final IAccessorFactory<T> attribute;

		public SetAggregator(String name, String description, IAccessorFactory<T> attribute, IType<? super V> type) {
			super(name, description, type);
			this.attribute = attribute;
		}

		@Override
		public boolean acceptType(IType<IItem> type) {
			return attribute.getAccessor(type) != null;
		}

		@Override
		public SetConsumer<T> newItemConsumer(IType<IItem> itemType) {
			return new SetConsumer<>(attribute.getAccessor(itemType));
		}
	};

	public static IAggregator<String, ?> distinctAsString(String typeId, IAttribute<String> attribute) {
		return filter(distinctAsString(attribute, ", "), ItemFilters.type(typeId)); //$NON-NLS-1$
	}

	public static IAggregator<String, ?> distinctAsString(IAttribute<String> attribute, final String delimiter) {
		return distinctAsString(attribute, delimiter, attribute.getName(), attribute.getDescription());
	}

	public static IAggregator<String, ?> distinctAsString(
		IAttribute<String> attribute, final String delimiter, String name, String description) {
		return Aggregators.valueBuilderAggregator(Aggregators.distinct(attribute),
				new IValueBuilder<String, Set<String>>() {

					@Override
					public String getValue(Set<String> source) {
						return source.isEmpty() ? null : StringToolkit.join(source, delimiter);
					}

					@Override
					public IType<? super String> getValueType() {
						return UnitLookup.PLAIN_TEXT;
					}
				}, name, description);
	}

	public static <V1, V2, C extends IItemConsumer<C>> IAggregator<V2, C> valueBuilderAggregator(
		final IAggregator<V1, C> aggregator, final IValueBuilder<V2, V1> valuebuilder, String name,
		String description) {

		return new AggregatorBase<V2, C>(name, description, valuebuilder.getValueType()) {

			@Override
			public boolean acceptType(IType<IItem> type) {
				return aggregator.acceptType(type);
			}

			@Override
			public C newItemConsumer(IType<IItem> type) {
				return aggregator.newItemConsumer(type);
			}

			@Override
			public V2 getValue(final Iterator<C> consumers) {
				V1 val1 = aggregator.getValue(consumers);
				return val1 != null ? valuebuilder.getValue(val1) : null;
			}
		};
	}

	public static <T> IAggregator<IQuantity, ?> countDistinct(
		String name, String description, IAccessorFactory<T> attribute) {
		return new SetAggregator<IQuantity, T>(name, description, attribute, UnitLookup.NUMBER) {

			@Override
			public IQuantity getValue(SetConsumer<T> consumer) {
				return UnitLookup.NUMBER_UNITY.quantity(consumer.distinct.size());
			}

		};
	}

	public static <T> IAggregator<Set<T>, ?> distinct(IAttribute<T> attribute) {
		return distinct(MessageFormat.format(Messages.getString(Messages.ItemAggregate_DISTINCT), attribute.getName()),
				attribute);
	}

	public static <T> IAggregator<Set<T>, ?> distinct(String name, IAccessorFactory<T> attribute) {
		return new SetAggregator<Set<T>, T>(name, null, attribute, UnitLookup.UNKNOWN) {

			@Override
			public Set<T> getValue(SetConsumer<T> consumer) {
				return consumer.distinct;
			}

		};
	}

	public static <C extends IItemConsumer<C>> IAggregator<C, C> forConsumer(IItemConsumerFactory<C> consumerFactory) {
		return forConsumer(consumerFactory, PredicateToolkit.<IType<IItem>> truePredicate());
	}

	public static <C extends IItemConsumer<C>> IAggregator<C, C> forConsumer(
		final IItemConsumerFactory<C> consumerFactory, final IPredicate<IType<IItem>> acceptType) {
		return new MergingAggregator<C, C>("", null, UnitLookup.UNKNOWN) { //$NON-NLS-1$

			@Override
			public boolean acceptType(IType<IItem> type) {
				return acceptType.evaluate(type);
			}

			@Override
			public C newItemConsumer(IType<IItem> type) {
				return consumerFactory.newItemConsumer(type);
			}

			@Override
			public C getValue(C consumer) {
				return consumer;
			}

		};
	}

	private static String getCountName(IType<?> type) {
		return Messages.getString(Messages.ItemAggregate_COUNT) + " " + type.getName(); //$NON-NLS-1$
	}

	static String getSumName(String name) {
		return Messages.getString(Messages.ItemAggregate_TOTAL) + " " + name; //$NON-NLS-1$
	}

	static String getVarianceName(String name, boolean besselCorrection) {
		return Messages.getString(besselCorrection ? Messages.ItemAggregate_VARIANCE : Messages.ItemAggregate_VARIANCEP)
				+ " " + name; //$NON-NLS-1$
	}

	static String getStddevName(String name, boolean besselCorrection) {
		return Messages.getString(besselCorrection ? Messages.ItemAggregate_STDDEV : Messages.ItemAggregate_STDDEVP)
				+ " " + name; //$NON-NLS-1$
	}

	static String getAvgName(String name) {
		return Messages.getString(Messages.ItemAggregate_AVERAGE) + " " + name; //$NON-NLS-1$
	}

	static String getMaxName(String name, ContentType<?> ct) {
		if (ct == UnitLookup.TIMESPAN) {
			return Messages.getString(Messages.ItemAggregate_LONGEST) + " " + name; //$NON-NLS-1$
		} else if (ct == UnitLookup.TIMESTAMP) {
			return Messages.getString(Messages.ItemAggregate_LAST) + " " + name; //$NON-NLS-1$
		} else {
			return Messages.getString(Messages.ItemAggregate_MAXIMUM) + " " + name; //$NON-NLS-1$
		}
	}

	static String getMinName(String name, ContentType<?> ct) {
		if (ct == UnitLookup.TIMESPAN) {
			return Messages.getString(Messages.ItemAggregate_SHORTEST) + " " + name; //$NON-NLS-1$
		} else if (ct == UnitLookup.TIMESTAMP) {
			return Messages.getString(Messages.ItemAggregate_FIRST) + " " + name; //$NON-NLS-1$
		} else {
			return Messages.getString(Messages.ItemAggregate_MINIMUM) + " " + name; //$NON-NLS-1$
		}
	}

	// FIXME: Translated strings are not good for persistence purposes. Maybe do something else.
	public static IAggregator<IQuantity, ?> getQuantityAggregator(String name, IAttribute<IQuantity> attribute) {
		if (name == null) {
			return null;
		}
		if (name.startsWith(Messages.getString(Messages.ItemAggregate_TOTAL))) {
			return sum(attribute);
		} else if (name.startsWith(Messages.getString(Messages.ItemAggregate_AVERAGE))) {
			return avg(attribute);
		} else if (name.startsWith(Messages.getString(Messages.ItemAggregate_LONGEST))) {
			return max(attribute);
		} else if (name.startsWith(Messages.getString(Messages.ItemAggregate_LAST))) {
			return max(attribute);
		} else if (name.startsWith(Messages.getString(Messages.ItemAggregate_MAXIMUM))) {
			return max(attribute);
		} else if (name.startsWith(Messages.getString(Messages.ItemAggregate_SHORTEST))) {
			return min(attribute);
		} else if (name.startsWith(Messages.getString(Messages.ItemAggregate_FIRST))) {
			return min(attribute);
		} else if (name.startsWith(Messages.getString(Messages.ItemAggregate_MINIMUM))) {
			return min(attribute);
		} else {
			return null;
		}
	}

	public static IAggregator<IQuantity, ?> getQuantityAggregator(String name, IType<?> type) {
		if (type != null) {
			if (name.startsWith(Messages.getString(Messages.ItemAggregate_COUNT))) {
				return count(type);
			}
		}
		if (name.startsWith(Messages.getString(Messages.ItemAggregate_COUNT))) {
			return count();
		}
		return null;
	}

	public static IAggregator<IQuantity, ?> getQuantityAggregator(String name) {
		if (name.startsWith(Messages.getString(Messages.ItemAggregate_COUNT))) {
			return count();
		}
		return null;
	}

	/**
	 * This consumer separates the attribute for which to do the ordering from the attribute to use
	 * for accessing the value. It is typically used within the AdvancedMinMaxAggregator for getting
	 * a specific value from the first or last event from a collection of events.
	 *
	 * @param <V>
	 *            the return value type, for example {@code java.lang.String}
	 * @param <T>
	 *            the value type for the ordering
	 */
	public static class AdvancedMinMaxConsumer<V, T extends Comparable<T>>
			implements IItemConsumer<AdvancedMinMaxConsumer<V, T>> {
		private final IMemberAccessor<? extends V, IItem> accessor;
		private final IMemberAccessor<T, IItem> comparatorAccessor;
		private final boolean max;
		private IItem item;

		/**
		 * @param valueAccessor
		 *            the accessor for retrieving the value
		 * @param comparatorAccessor
		 *            the accessor for retrieving the value to use for comparisons
		 * @param max
		 *            whether to use the smallest value, or the greatest
		 */
		public AdvancedMinMaxConsumer(IMemberAccessor<? extends V, IItem> valueAccessor,
				IMemberAccessor<T, IItem> comparatorAccessor, boolean max) {
			this.accessor = valueAccessor;
			this.max = max;
			this.comparatorAccessor = comparatorAccessor;
		}

		@Override
		public void consume(IItem newItem) {
			if (newItem == null) {
				return;
			}
			T newOrderingValue = comparatorAccessor.getMember(newItem);
			if (newOrderingValue == null) {
				return;
			} else if (item == null) {
				item = newItem;
			} else {
				T oldOrderingValue = comparatorAccessor.getMember(item);
				if (newOrderingValue.compareTo(oldOrderingValue) > 0 == max) {
					item = newItem;
				}
			}
		}

		@Override
		public AdvancedMinMaxConsumer<V, T> merge(AdvancedMinMaxConsumer<V, T> other) {
			consume(other.item);
			return this;
		}

		public V getValue() {
			if (item == null) {
				return null;
			}
			return accessor.getMember(item);
		}
	}

	/**
	 * This aggregator separates the attribute for which to do the ordering from the attribute to
	 * use for accessing the value. It is typically used for getting a specific value from the first
	 * or last event from a collection of events.
	 *
	 * @param <V>
	 *            the return value type, for example {@code java.lang.String}
	 * @param <T>
	 *            the value type for the ordering
	 */
	private static class AdvancedMinMaxAggregator<V, T extends Comparable<T>>
			extends FieldAggregatorBase<V, AdvancedMinMaxConsumer<V, T>> {
		private final boolean max;
		private final IAttribute<V> attribute;
		private final IAttribute<T> comparator;

		public AdvancedMinMaxAggregator(String name, String description, IAttribute<V> attribute,
				IAttribute<T> comparator, boolean max) {
			super(name, description, attribute.getContentType());
			this.attribute = attribute;
			this.comparator = comparator;
			this.max = max;
		}

		@Override
		public AdvancedMinMaxConsumer<V, T> newItemConsumer(IType<IItem> type) {
			return new AdvancedMinMaxConsumer<>(attribute.getAccessor(type), comparator.getAccessor(type), max);
		}

		@Override
		public V getValue(AdvancedMinMaxConsumer<V, T> consumer) {
			return consumer.getValue();
		}

		@Override
		protected IMemberAccessor<? extends V, IItem> doGetAccessor(IType<IItem> type) {
			return attribute.getAccessor(type);
		}
	}

	/**
	 * This aggregator separates the attribute for which to do the ordering from the attribute to
	 * use for accessing the value. It is typically used for getting a specific value from the first
	 * event from a collection of events.
	 * <p>
	 * For example:<br>
	 * AdvancedMinAggregator&lt;String, IQuantity&gt; aggregator = new
	 * AdvancedMinAggregator(myFavouriteAttribute, startTimeAttribute);
	 *
	 * @param <V>
	 *            the return value type, for example {@code java.lang.String}
	 * @param <T>
	 *            the value type for the ordering
	 */
	public static class AdvancedMinAggregator<V, T extends Comparable<T>> extends AdvancedMinMaxAggregator<V, T> {
		public AdvancedMinAggregator(String name, String description, IAttribute<V> attribute,
				IAttribute<T> comparator) {
			super(name, description, attribute, comparator, false);
		}
	}

	/**
	 * This aggregator separates the attribute for which to do the ordering from the attribute to
	 * use for accessing the value. It is typically used for getting a specific value from the last
	 * event from a collection of events.
	 * <p>
	 * For example: <br>
	 * AdvancedMaxAggregator&lt;String, IQuantity&gt; aggregator = new
	 * AdvancedMaxAggregator(myFavouriteAttribute, endTimeAttribute);
	 *
	 * @param <V>
	 *            the return value type, for example {@code java.lang.String}
	 * @param <T>
	 *            the value type for the ordering
	 */
	public static class AdvancedMaxAggregator<V, T extends Comparable<T>> extends AdvancedMinMaxAggregator<V, T> {
		public AdvancedMaxAggregator(String name, String description, IAttribute<V> attribute,
				IAttribute<T> comparator) {
			super(name, description, attribute, comparator, true);
		}
	}
}
