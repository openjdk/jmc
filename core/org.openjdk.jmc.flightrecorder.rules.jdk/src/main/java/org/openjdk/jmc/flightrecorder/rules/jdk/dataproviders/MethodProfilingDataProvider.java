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
package org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders;

import java.util.List;

import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.GroupingAggregator;
import org.openjdk.jmc.common.item.GroupingAggregator.IQuantityListFinisher;
import org.openjdk.jmc.common.item.IAccessorFactory;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;

/**
 * Helper class for analyzing sets of method profiling samples.
 */
public class MethodProfilingDataProvider {

	public static final IItemFilter SAMPLES_OR_CPU_FILTER = ItemFilters.or(JdkFilters.EXECUTION_SAMPLE,
			JdkFilters.CPU_LOAD);
	public static final IAggregator<IQuantity, ?> MIN_ENDTIME = Aggregators.min(
			Messages.getString(Messages.MethodProfilingDataProvider_AGGR_MIN_ENDTIME), null, JdkTypeIDs.CPU_LOAD,
			JfrAttributes.END_TIME);
	public static final IAggregator<IQuantity, ?> MAX_ENDTIME = Aggregators.max(
			Messages.getString(Messages.MethodProfilingDataProvider_AGGR_MAX_ENDTIME), null, JdkTypeIDs.CPU_LOAD,
			JfrAttributes.END_TIME);

	/**
	 * A custom accessor used to get the top frame in a stack trace.
	 */
	public static final IAccessorFactory<IMCMethod> TOP_FRAME_ACCESSOR_FACTORY = new IAccessorFactory<IMCMethod>() {

		@Override
		public <T> IMemberAccessor<IMCMethod, T> getAccessor(IType<T> type) {
			final IMemberAccessor<IMCStackTrace, T> sta = JfrAttributes.EVENT_STACKTRACE.getAccessor(type);
			return new IMemberAccessor<IMCMethod, T>() {

				@Override
				public IMCMethod getMember(T inObject) {
					IMCStackTrace st = sta.getMember(inObject);
					if (st != null && !st.getFrames().isEmpty()) {
						return st.getFrames().get(0).getMethod();
					}
					return null;
				}
			};
		}

	};

	/**
	 * Function that calculates a value representing how balanced the set of top frames is as a
	 * number in the range [0,1]. A high number indicates that there are some frames occurring more
	 * frequently than others.
	 */
	public static final IQuantityListFinisher<IQuantity> topFrameBalanceFunction = new IQuantityListFinisher<IQuantity>() {

		@Override
		public IType<IQuantity> getValueType() {
			return UnitLookup.NUMBER;
		}

		@Override
		public IQuantity getValue(List<IQuantity> values, IQuantity total) {
			if (total != null && total.doubleValue() > 0) {
				IUnit totalUnit = total.getUnit();
				double totalValue = total.doubleValue();

				double score = 0;
				for (int i = values.size() - 1; i >= 0; i--) {
					int index = values.size() - i;
					score += values.get(i).doubleValueIn(totalUnit) / index;
				}
				return UnitLookup.NUMBER_UNITY.quantity(score / totalValue);
			}
			return null;
		}
	};

	/**
	 * A quota calculation of how large a part the most commonly occurring top frame is of the total
	 * amount of samples input.
	 */
	public static final IQuantityListFinisher<IQuantity> topFrameQuotaFunction = new IQuantityListFinisher<IQuantity>() {

		@Override
		public IType<IQuantity> getValueType() {
			return UnitLookup.NUMBER;
		}

		@Override
		public IQuantity getValue(List<IQuantity> values, IQuantity total) {
			if (total != null && total.doubleValue() > 0) {
				double score = values.get(values.size() - 1).ratioTo(total);
				return UnitLookup.NUMBER_UNITY.quantity(score);
			}
			return null;
		}
	};

	/**
	 * Ready to use aggregator using the
	 * {@link MethodProfilingDataProvider#topFrameBalanceFunction}.
	 */
	public static final IAggregator<IQuantity, ?> TOP_FRAME_BALANCE = GroupingAggregator.build(
			Messages.getString(Messages.MethodProfilingDataProvider_AGGR_TOP_FRAME_BALANCE),
			Messages.getString(Messages.MethodProfilingDataProvider_AGGR_TOP_FRAME_BALANCE_DESC),
			TOP_FRAME_ACCESSOR_FACTORY, JdkAggregators.EXECUTION_SAMPLE_COUNT, topFrameBalanceFunction);

	/**
	 * Ready to use aggregator using the {@link MethodProfilingDataProvider#topFrameQuotaFunction}.
	 */
	public static final IAggregator<IQuantity, ?> TOP_FRAME_QUOTA = GroupingAggregator.build(
			Messages.getString(Messages.MethodProfilingDataProvider_AGGR_AGGR_TOP_FRAME_QUOTA),
			Messages.getString(Messages.MethodProfilingDataProvider_AGGR_AGGR_TOP_FRAME_QUOTA_DESC),
			TOP_FRAME_ACCESSOR_FACTORY, JdkAggregators.EXECUTION_SAMPLE_COUNT, topFrameQuotaFunction);
}
