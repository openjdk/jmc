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
package org.openjdk.jmc.ui.charts;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IScalarAffineTransform;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.ui.charts.IChartInfoVisitor.IBucket;
import org.openjdk.jmc.ui.charts.IChartInfoVisitor.IPoint;
import org.openjdk.jmc.ui.charts.IChartInfoVisitor.ISpan;

/**
 * A sequence of pairs of x and y quantities, ordered on x, with numerical mappings (typically to
 * pixels).
 *
 * @param <P>
 */
public abstract class XYQuantities<P> implements IXYDisplayableSet<P, IQuantity> {
	private final P payload;
	protected final SubdividedQuantityRange xRange;
	protected IQuantity maxY;
	protected IQuantity minY;
	protected SubdividedQuantityRange yRange;

	private static class XYStored<T> extends QuantityStored<T> {

		private final List<IQuantity> xValues;

		private XYStored(T payload, List<IQuantity> xValues, List<IQuantity> yValues, SubdividedQuantityRange xRange) {
			super(payload, yValues, xRange);
			this.xValues = xValues;
			assert xValues.size() == yValues.size();
		}

		@Override
		public int floorIndexAtX(double x) {
			// FIXME: Find samples also 'during' the pixel. Somewhat deviating from the method definition.
			// Redefine/rename method or fix this some other way.
			double nextX = Math.floor(x) + 1;
			int index = Collections.binarySearch(xValues, xRange.getQuantityAtPixel(nextX));
			if (index < 0) {
				// On inexact match
				index = (-index - 1);
			}
			// index will point to the sample exactly at, or the next after, nextX. The floor index is the index before.
			return index - 1;
		}

		@Override
		public double getPixelX(int index) {
			return xRange.getPixel(xValues.get(index));
		}

		@Override
		public IQuantity getDisplayableX(int index) {
			return xValues.get(index);
		}
	}

	private static class QuantityStored<T> extends XYQuantities<T> {
		private final List<IQuantity> yValues;

		private QuantityStored(T payload, List<IQuantity> yValues, SubdividedQuantityRange xRange) {
			super(payload, xRange);
			IQuantity maxY = null;
			IQuantity minY = null;
			for (IQuantity y : yValues) {
				if (y != null) {
					if (maxY == null) {
						maxY = y;
						minY = y;
					} else {
						if (y.compareTo(maxY) > 0) {
							maxY = y;
						}
						if (y.compareTo(minY) < 0) {
							minY = y;
						}
					}
				}
			}
			if (maxY == null) {
				this.yValues = Collections.emptyList();
				this.maxY = null;
				this.minY = null;
			} else {
				this.yValues = yValues;
				this.maxY = maxY;
				this.minY = minY;
			}
		}

		@Override
		public int getSize() {
			return yValues.size();
		}

		@Override
		public double getPixelY(int index) {
			return yRange.getPixel(yValues.get(index));
		}

		@Override
		public IQuantity getDisplayableY(int index) {
			return yValues.get(index);
		}
	}

	private static class DoubleStored<T> extends XYQuantities<T> {
		private final double[] yValues;
		private final IUnit yUnit;
		private IScalarAffineTransform yUnitToPixel;

		private DoubleStored(T payload, double[] yValues, IUnit yUnit, SubdividedQuantityRange xRange) {
			super(payload, xRange);
			this.yUnit = yUnit;
			double maxY = Double.NEGATIVE_INFINITY;
			double minY = Double.POSITIVE_INFINITY;
			for (double yValue : yValues) {
				double y = yValue;
				if (!Double.isNaN(y)) {
					if (y > maxY) {
						maxY = y;
					}
					if (y < minY) {
						minY = y;
					}
				}
			}
			if (yUnit == null || maxY < minY) {
				this.yValues = new double[0];
				this.maxY = null;
				this.minY = null;
			} else {
				this.yValues = yValues;
				this.maxY = yUnit.quantity(maxY);
				this.minY = yUnit.quantity(minY);
			}
		}

		@Override
		public void setYRange(SubdividedQuantityRange yRange) {
			super.setYRange(yRange);
			yUnitToPixel = yRange.toPixelTransform(yUnit);
		}

		@Override
		public int getSize() {
			return yValues.length;
		}

		@Override
		public double getPixelY(int index) {
			double yValue = yValues[index];
			return yUnitToPixel.targetValue(Double.isNaN(yValue) ? 0.0 : yValue);
		}

		@Override
		public IQuantity getDisplayableY(int index) {
			double yValue = yValues[index];
			return Double.isNaN(yValue) ? null : yUnit.quantity(yValue);
		}
	}

	public static class Point implements IPoint {
		private final XYQuantities<?> set;
		protected int index;
		private final Point2D offset;
		private final String name;
		private final Color color;

		public Point(XYQuantities<?> set, int index, Point2D offset, String name, Color color) {
			this.set = set;
			this.index = index;
			this.offset = offset;
			this.name = name;
			this.color = color;
		}

		@Override
		public IPoint keeper() {
			return this;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Color getColor() {
			return color;
		}

		@Override
		public Point2D getTarget() {
			// Mimic integer truncations used during drawing in AWTChartToolkit?
			int y = (int) (offset.getY()) + set.getHeight() - 1 - ((int) set.getPixelY(index));
			return new Point2D.Double(offset.getX() + set.getPixelX(index), y);
		}

		@Override
		public IQuantity getX() {
			return set.getDisplayableX(index);
		}

		@Override
		public IQuantity getY() {
			return set.getDisplayableY(index);
		}
	}

	public static class Bucket implements IBucket {
		private final XYQuantities<?> set;
		protected int index;
		private final Point2D offset;
		private final String name;
		private final Color color;

		public Bucket(XYQuantities<?> set, int index, Point2D offset, String name, Color color) {
			this.set = set;
			this.index = index;
			this.offset = offset;
			this.name = name;
			this.color = color;
		}

		@Override
		public IBucket keeper() {
			return this;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Color getColor() {
			return color;
		}

		protected XYQuantities<?> getXYSet() {
			return set;
		}

		@Override
		public Rectangle2D getTarget() {
			// This will fail with XYStored, but those shouldn't be used for buckets.
			double x1 = set.getPixelX(index);
			// Mimic integer truncations used during drawing in AWTChartToolkit
			int w = ((int) set.getPixelX(index + 1)) - ((int) x1);
			int y = (int) set.getPixelY(index);
			// There are differences in how AWT Graphics and SWT GC interpret rectangle sizes.
			return new Rectangle2D.Double(offset.getX() + set.getPixelX(index), offset.getY() + set.getHeight() - 1 - y,
					w, y);
		}

		@Override
		public IDisplayable getRange() {
			return QuantityRange.createWithEnd(set.getDisplayableX(index), set.getDisplayableX(index + 1));
		}

		@Override
		public IQuantity getStartX() {
			return getXYSet().getDisplayableX(index);
		}

		@Override
		public IQuantity getEndX() {
			// This will fail with XYStored, but those shouldn't be used for buckets.
			return getXYSet().getDisplayableX(index + 1);
		}

		@Override
		public IQuantity getWidth() {
			return getEndX().subtract(getStartX());
		}

		@Override
		public IQuantity getY() {
			return getXYSet().getDisplayableY(index);
		}

		@Override
		public Object getPayload() {
			Object payload = getXYSet().getPayload();
			if (payload != null) {
				// Unpack if array or List.
				if (payload.getClass().isArray()) {
					return (index < Array.getLength(payload)) ? Array.get(payload, index) : null;
				}
				if (payload instanceof List) {
					List<?> list = (List<?>) payload;
					return (index < list.size()) ? list.get(index) : null;
				}
			}
			return payload;
		}
	}

	public static abstract class AbstractSpan implements ISpan, Cloneable {
		protected int index;
		private final Point2D offset;

		public AbstractSpan(int index, Point2D offset) {
			this.index = index;
			this.offset = offset;
		}

		@Override
		public ISpan keeper() {
			try {
				return (ISpan) clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				return this;
			}
		}

		protected abstract XYQuantities<?> getXYSet();

		protected abstract int getHeight();

		@Override
		public abstract Color getColor();

		@Override
		public Rectangle2D getTarget() {
			XYQuantities<?> set = getXYSet();
			// Mimic integer truncations used during drawing in AWTChartToolkit, but limit to avoid int overflows in SWT.
			int x1 = Math.max(-1000000, (int) set.getPixelX(index));
			int x2 = Math.min(1000000, (int) set.getPixelY(index));
			// There are differences in how AWT Graphics and SWT GC interpret rectangle sizes.
			return new Rectangle2D.Double(offset.getX() + x1, offset.getY(), x2 - x1, getHeight() - 1);
		}

		@Override
		public IDisplayable getRange() {
			return QuantityRange.createInfinite(getStartX(), getEndX());
		}

		@Override
		public IQuantity getStartX() {
			return getXYSet().getDisplayableX(index);
		}

		@Override
		public IQuantity getEndX() {
			return getXYSet().getDisplayableY(index);
		}

		@Override
		public IQuantity getWidth() {
			IQuantity start = getStartX();
			IQuantity end = getEndX();
			return ((start != null) && (end != null)) ? end.subtract(start) : null;
		}

		@Override
		public Object getPayload() {
			Object payload = getXYSet().getPayload();
			if (payload != null) {
				// Unpack if array or List.
				if (payload.getClass().isArray()) {
					return (index < Array.getLength(payload)) ? Array.get(payload, index) : null;
				}
				if (payload instanceof List) {
					List<?> list = (List<?>) payload;
					return (index < list.size()) ? list.get(index) : null;
				}
			}
			return payload;
		}
	}

	public static <T> XYQuantities<T> create(
		T payload, double[] numericalYValues, IUnit yUnit, SubdividedQuantityRange xRange) {
		return new DoubleStored<>(payload, numericalYValues, yUnit, xRange);
	}

	public static <T> XYQuantities<T> create(T payload, List<IQuantity> yValues, SubdividedQuantityRange xRange) {
		return new QuantityStored<>(payload, yValues, xRange);
	}

	public static <T> XYQuantities<T> create(
		T payload, List<IQuantity> xValues, List<IQuantity> yValues, SubdividedQuantityRange xRange) {
		return new XYStored<>(payload, xValues, yValues, xRange);
	}

	protected XYQuantities(P payload, SubdividedQuantityRange xRange) {
		this.payload = payload;
		this.xRange = xRange;
	}

	/**
	 * Set the y range so that the {@link #getPixelY(int)} and {@link #getHeight()} methods may be
	 * used.
	 *
	 * @param yRange
	 */
	// FIXME: Alternatively to this method, return an AWT 2D Affine Transform here.
	public void setYRange(SubdividedQuantityRange yRange) {
		this.yRange = yRange;
	}

	@Override
	public abstract int getSize();

	public int floorIndexAtX(double x) {
		return xRange.getFloorSubdividerAtPixel(x);
	}

	@Override
	public double getPixelX(int index) {
		return xRange.getSubdividerPixel(index);
	}

	@Override
	public abstract double getPixelY(int index);

	@Override
	public IQuantity getDisplayableX(int index) {
		return xRange.getSubdivider(index);
	}

	@Override
	public abstract IQuantity getDisplayableY(int index);

	public IQuantity getMaxY() {
		return maxY;
	}

	public IQuantity getMinY() {
		return minY;
	}

	@Override
	public int getWidth() {
		return xRange.getPixelExtent();
	}

	@Override
	public int getHeight() {
		return yRange.getPixelExtent();
	}

	public SubdividedQuantityRange getXRange() {
		return xRange;
	}

	public SubdividedQuantityRange getYRange() {
		return yRange;
	}

	@Override
	public P getPayload() {
		return payload;
	}
}
