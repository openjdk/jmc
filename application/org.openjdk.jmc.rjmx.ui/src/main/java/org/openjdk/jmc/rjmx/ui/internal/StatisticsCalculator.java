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
package org.openjdk.jmc.rjmx.ui.internal;

import org.eclipse.core.runtime.IAdaptable;

import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.rjmx.subscription.MRI;

public class StatisticsCalculator implements IAdaptable {

	private long count;
	private double last = Double.NaN;
	private double max = Double.NEGATIVE_INFINITY;
	private double min = Double.POSITIVE_INFINITY;
	private double mean;
	private double M2;
	private final MRI mri;
	private IUnit unit;

	public StatisticsCalculator(MRI mri) {
		this.mri = mri;
	}

	public void setUnit(IUnit unit) {
		this.unit = unit;
	}

	public void addValue(double value) {
		last = value;
		max = Math.max(max, value);
		min = Math.min(min, value);
		count++;
		double delta = value - mean;
		mean = mean + delta / count;
		M2 = M2 + delta * (value - mean);
	}

	public boolean reset() {
		if (count > 1) {
			min = last;
			max = last;
			mean = last;
			count = 1;
			M2 = 0;
			return true;
		}
		return false;
	}

	public IUnit getUnit() {
		return unit;
	}

	public MRI getAttribute() {
		return mri;
	}

	public Number getLast() {
		return last;
	}

	public Number getMax() {
		return count > 0 ? max : Double.NaN;
	}

	public Number getMin() {
		return count > 0 ? min : Double.NaN;
	}

	public Number getMean() {
		return count > 0 ? mean : Double.NaN;
	}

	public Number getSigma() {
		return count == 0 ? Double.NaN : count == 1 ? 0 : Math.sqrt(M2 / (count - 1));
	}

	private Object decode(Number o) {
		return unit == null || count == 0 ? o : unit.quantity(o);
	}

	@Override
	public int hashCode() {
		return mri.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof StatisticsCalculator && mri.equals(((StatisticsCalculator) obj).mri);
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return MRI.class.equals(adapter) ? adapter.cast(mri) : null;
	}

	public static final IMemberAccessor<MRI, StatisticsCalculator> GET_ATTRIBUTE = new IMemberAccessor<MRI, StatisticsCalculator>() {
		@Override
		public MRI getMember(StatisticsCalculator c) {
			return c.getAttribute();
		}
	};

	public static final IMemberAccessor<Object, StatisticsCalculator> GET_LAST = new IMemberAccessor<Object, StatisticsCalculator>() {
		@Override
		public Object getMember(StatisticsCalculator c) {
			return c.decode(c.getLast());
		}
	};

	public static final IMemberAccessor<Object, StatisticsCalculator> GET_MAX = new IMemberAccessor<Object, StatisticsCalculator>() {
		@Override
		public Object getMember(StatisticsCalculator c) {
			return c.decode(c.getMax());
		}
	};

	public static final IMemberAccessor<Object, StatisticsCalculator> GET_MIN = new IMemberAccessor<Object, StatisticsCalculator>() {
		@Override
		public Object getMember(StatisticsCalculator c) {
			return c.decode(c.getMin());
		}
	};

	public static final IMemberAccessor<Object, StatisticsCalculator> GET_AVERAGE = new IMemberAccessor<Object, StatisticsCalculator>() {
		@Override
		public Object getMember(StatisticsCalculator c) {
			return c.decode(c.getMean());
		}
	};

	public static final IMemberAccessor<Object, StatisticsCalculator> GET_SIGMA = new IMemberAccessor<Object, StatisticsCalculator>() {
		@Override
		public Object getMember(StatisticsCalculator c) {
			return c.decode(c.getSigma());
		}
	};
}
