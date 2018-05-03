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
package org.openjdk.jmc.common.unit;

import org.openjdk.jmc.common.IDisplayable;

/**
 * A finite range of quantities.
 */
public abstract class QuantityRange<U extends TypedUnit<U>> implements IRange<IQuantity> {
	protected final ITypedQuantity<U> start;

	private QuantityRange(ITypedQuantity<U> start) {
		this.start = start;
	}

	private static class Point<U extends TypedUnit<U>> extends QuantityRange<U> {
		private Point(ITypedQuantity<U> start) {
			super(start);
		}

		@Override
		public ITypedQuantity<U> getEnd() {
			return start;
		}

		@Override
		public IQuantity getCenter() {
			return start;
		}

		@Override
		public IQuantity getExtent() {
			return start.getUnit().getDeltaUnit().quantity(0);
		}

		@Override
		public boolean isPoint() {
			return true;
		}

		@Override
		public boolean equals(Object obj) {
			return (obj instanceof Point) && start.equals(((Point<?>) obj).start);
		}

		@Override
		public int hashCode() {
			return start.hashCode();
		}
	}

	private static class WithEnd<U extends TypedUnit<U>> extends QuantityRange<U> {
		private final ITypedQuantity<U> end;

		private WithEnd(ITypedQuantity<U> start, ITypedQuantity<U> end) {
			super(start);
			this.end = end;
		}

		@Override
		public ITypedQuantity<U> getEnd() {
			return end;
		}

		@Override
		public IQuantity getCenter() {
			return start.add(end.subtract(start).multiply(0.5));
		}

		@Override
		public ITypedQuantity<LinearUnit> getExtent() {
			return end.subtract(start);
		}

		@Override
		public boolean isPoint() {
			return start.compareTo(end) == 0;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof WithEnd) {
				WithEnd<?> other = (WithEnd<?>) obj;
				return start.equals(other.start) && end.equals(other.end);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return start.hashCode() + end.hashCode();
		}
	}

	private static class WithExtent<U extends TypedUnit<U>> extends QuantityRange<U> {
		private final ITypedQuantity<LinearUnit> extent;

		private WithExtent(ITypedQuantity<U> start, ITypedQuantity<LinearUnit> extent) {
			super(start);
			this.extent = extent;
		}

		@Override
		public ITypedQuantity<U> getEnd() {
			return start.add(extent);
		}

		@Override
		public IQuantity getCenter() {
			return start.add(extent.multiply(0.5));
		}

		@Override
		public ITypedQuantity<LinearUnit> getExtent() {
			return extent;
		}

		@Override
		public boolean isPoint() {
			return extent.doubleValue() == 0.0;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof WithExtent) {
				WithExtent<?> other = (WithExtent<?>) obj;
				return start.equals(other.start) && extent.equals(other.extent);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return start.hashCode() + extent.hashCode();
		}
	}

	public static <U extends TypedUnit<U>> IRange<IQuantity> createPoint(IQuantity start) {
		@SuppressWarnings("unchecked")
		ITypedQuantity<U> typedStart = (ITypedQuantity<U>) start;
		return new Point<>(typedStart);
	}

	@SuppressWarnings("nls")
	public static <U extends TypedUnit<U>> IRange<IQuantity> createWithEnd(IQuantity start, IQuantity end) {
		if (start.getType() != end.getType()) {
			throw new IllegalArgumentException(
					start.persistableString() + " and " + end.persistableString() + " needs to be of the same kind");
		}
		@SuppressWarnings("unchecked")
		ITypedQuantity<U> typedStart = (ITypedQuantity<U>) start;
		@SuppressWarnings("unchecked")
		ITypedQuantity<U> typedEnd = (ITypedQuantity<U>) end;
		return new WithEnd<>(typedStart, typedEnd);
	}

	@SuppressWarnings("nls")
	public static <U extends TypedUnit<U>> IRange<IQuantity> createWithExtent(
		IQuantity start, ITypedQuantity<LinearUnit> extent) {
		@SuppressWarnings("unchecked")
		ITypedQuantity<U> typedStart = (ITypedQuantity<U>) start;
		if (typedStart.getUnit().getDeltaUnit().getContentType() != extent.getType()) {
			throw new IllegalArgumentException(start.persistableString() + " and " + extent.persistableString()
					+ " needs to be of compatible kinds");
		}
		return new WithExtent<>(typedStart, extent);
	}

	/**
	 * Create an {@link IRange IRange&lt;IQuantity&gt;} if {@code start} and {@code end} both are
	 * non-null. Otherwise, create an {@link IDisplayable} that looks like an infinite range, by
	 * treating null values as minus or plus infinity, respectively. This behavior is a consequence
	 * of the current state where {@link IQuantity} and thusly {@link IRange
	 * IRange&lt;IQuantity&gt;} both need to be finite. (A possible change to this state, when all
	 * kinds of quantities support being stored as doubles, is to allow
	 * {@link Double#NEGATIVE_INFINITY} and {@link Double#POSITIVE_INFINITY} as numerical
	 * quantities. In that case, this method can be replaced with
	 * {@link #createWithEnd(IQuantity, IQuantity)}).
	 */
	@SuppressWarnings("nls")
	public static IDisplayable createInfinite(final IQuantity start, final IQuantity end) {
		if (start != null) {
			if (end != null) {
				return createWithEnd(start, end);
			}
			return new IDisplayable() {
				@Override
				public String displayUsing(String formatHint) {
					return start.displayUsing(formatHint) + " \u2013 \u221e";
				}
			};
		} else if (end != null) {
			return new IDisplayable() {
				@Override
				public String displayUsing(String formatHint) {
					return " -\u221e \u2013" + end.displayUsing(formatHint);
				}
			};
		}
		return new IDisplayable() {
			@Override
			public String displayUsing(String formatHint) {
				return " -\u221e \u2013 \u221e";
			}
		};
	}

	public static IRange<IQuantity> intersection(IRange<IQuantity> a, IRange<IQuantity> b) {
		IQuantity maxStart = a.getStart().compareTo(b.getStart()) > 0 ? a.getStart() : b.getStart();
		IQuantity minEnd = a.getEnd().compareTo(b.getEnd()) > 0 ? b.getEnd() : a.getEnd();
		return minEnd.compareTo(maxStart) >= 0 ? createWithEnd(maxStart, minEnd) : null;
	}

	@Override
	public ITypedQuantity<U> getStart() {
		return start;
	}

	@Override
	public abstract ITypedQuantity<U> getEnd();

	@Override
	public String displayUsing(String formatHint) {
		return start.getType().getRangeFormatter(formatHint).format(this);
	}
}
