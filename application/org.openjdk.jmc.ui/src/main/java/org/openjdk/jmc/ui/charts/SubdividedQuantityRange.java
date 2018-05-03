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

import org.openjdk.jmc.common.unit.DecimalScaleFactor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.IScalarAffineTransform;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.SimpleAffineTransform;

/**
 * Representation of a quantity range, mapped to a numerical range starting at 0 and with a
 * configurable end (typically in pixels), with subdivisions aligned on "natural" quantity values.
 * Suitable for describing a graph axis with either tick marks or bucket boundaries, mapped to
 * pixels. For the time being, it might also be used with subdivisions that are aligned to the pixel
 * grid. But this usage should likely use a different implementation.
 */
public class SubdividedQuantityRange implements IRange<IQuantity> {
	// Essential state
	private final IQuantity rangeStart;
	private final IQuantity rangeEnd;
	private final int pixelExtent;

	private final IQuantity subdividerStart;
	private final IQuantity subdividerDelta;
	private final int numSubdividers;

	// Cache
	private final IQuantity quantityExtent;
	private final IUnit refUnit;
	private final IScalarAffineTransform refToSubdividerTransform;
	private final IScalarAffineTransform subdividerToRefTransform;
	private final IScalarAffineTransform refToPixelTransform;
	private final IScalarAffineTransform pixelToRefTransform;
	private final IScalarAffineTransform subdividerToPixelTransform;

	/**
	 * Create range with subdivisions aligned on "natural" quantities.
	 *
	 * @param rangeStart
	 * @param rangeEnd
	 * @param pixelExtent
	 * @param minPixelsPerSubdivider
	 */
	public SubdividedQuantityRange(IQuantity rangeStart, IQuantity rangeEnd, int pixelExtent,
			int minPixelsPerSubdivider) {
		this.rangeStart = rangeStart;
		this.rangeEnd = rangeEnd;
		quantityExtent = rangeEnd.subtract(rangeStart);
		this.pixelExtent = pixelExtent;

		IRange<IQuantity> firstBucket = rangeStart.getType().getFirstBucket(rangeStart, rangeEnd,
				((double) pixelExtent) / minPixelsPerSubdivider);
		subdividerStart = firstBucket.getStart();
		subdividerDelta = firstBucket.getExtent();
		numSubdividers = 2
				+ (int) (quantityExtent.doubleValue() / subdividerDelta.doubleValueIn(quantityExtent.getUnit()));

		refUnit = subdividerStart.getUnit();
		Number negOffsetInRef = subdividerStart.numberValueIn(refUnit);
		double divisor = subdividerDelta.doubleValueIn(refUnit.getDeltaUnit());
		refToSubdividerTransform = SimpleAffineTransform.createWithNegPreOffset(1 / divisor, negOffsetInRef);
		subdividerToRefTransform = refToSubdividerTransform.invert();
		negOffsetInRef = rangeStart.numberValueIn(refUnit);
		divisor = quantityExtent.doubleValueIn(refUnit.getDeltaUnit()) / pixelExtent;
		refToPixelTransform = SimpleAffineTransform.createWithNegPreOffset(1 / divisor, negOffsetInRef);
		pixelToRefTransform = refToPixelTransform.invert();
		subdividerToPixelTransform = refToPixelTransform.concat(subdividerToRefTransform);
	}

	/**
	 * Create range with fixed number of subdivisions, aligned to start and end. Not recommended for
	 * most usage. Should probably be moved to a simplified implementation.
	 *
	 * @param numSubdividers
	 * @param rangeStart
	 * @param rangeEnd
	 * @param pixelExtent
	 */
	public SubdividedQuantityRange(int numSubdividers, IQuantity rangeStart, IQuantity rangeEnd, int pixelExtent) {
		this.rangeStart = rangeStart;
		this.rangeEnd = rangeEnd;
		quantityExtent = rangeEnd.subtract(rangeStart);
		this.pixelExtent = pixelExtent;

		subdividerStart = rangeStart;
		subdividerDelta = quantityExtent.multiply(1.0 / numSubdividers);
		this.numSubdividers = numSubdividers;

		refUnit = subdividerStart.getUnit();
		Number negOffsetInRef = subdividerStart.numberValueIn(refUnit);
		double divisor = subdividerDelta.doubleValueIn(refUnit.getDeltaUnit());
		refToSubdividerTransform = SimpleAffineTransform.createWithNegPreOffset(1 / divisor, negOffsetInRef);
		subdividerToRefTransform = refToSubdividerTransform.invert();
		negOffsetInRef = rangeStart.numberValueIn(refUnit);
		divisor = quantityExtent.doubleValueIn(refUnit.getDeltaUnit()) / pixelExtent;
		refToPixelTransform = SimpleAffineTransform.createWithNegPreOffset(1 / divisor, negOffsetInRef);
		pixelToRefTransform = refToPixelTransform.invert();
		subdividerToPixelTransform = refToPixelTransform.concat(subdividerToRefTransform);
	}

	/**
	 * Create range with pixel aligned subdivisions. Should probably be moved to a simplified
	 * implementation.
	 *
	 * @param rangeStart
	 * @param rangeEnd
	 * @param pixelExtent
	 */
	private SubdividedQuantityRange(IQuantity rangeStart, IQuantity rangeEnd, int pixelExtent) {
		this.rangeStart = rangeStart;
		this.rangeEnd = rangeEnd;
		quantityExtent = rangeEnd.subtract(rangeStart);
		this.pixelExtent = pixelExtent;

		subdividerStart = rangeStart;
		subdividerDelta = quantityExtent.multiply(1.0 / pixelExtent);
		numSubdividers = pixelExtent;

		refUnit = subdividerStart.getUnit();
		Number negOffsetInRef = subdividerStart.numberValueIn(refUnit);
		double divisor = subdividerDelta.doubleValueIn(refUnit.getDeltaUnit());
		refToSubdividerTransform = SimpleAffineTransform.createWithNegPreOffset(1 / divisor, negOffsetInRef);
		subdividerToRefTransform = refToSubdividerTransform.invert();
		refToPixelTransform = refToSubdividerTransform;
		pixelToRefTransform = subdividerToRefTransform;
		subdividerToPixelTransform = DecimalScaleFactor.get(0);
	}

	public SubdividedQuantityRange copyWithPixelSubdividers() {
		// FIXME: Use another implementation?
		return new SubdividedQuantityRange(rangeStart, rangeEnd, pixelExtent);
	}

	@Override
	public IQuantity getStart() {
		return rangeStart;
	}

	@Override
	public IQuantity getEnd() {
		return rangeEnd;
	}

	@Override
	public IQuantity getCenter() {
		return rangeStart.add(rangeEnd.subtract(rangeStart).multiply(0.5));
	}

	@Override
	public IQuantity getExtent() {
		return quantityExtent;
	}

	@Override
	public boolean isPoint() {
		return quantityExtent.doubleValue() == 0.0;
	}

	@Override
	public String displayUsing(String formatHint) {
		return rangeStart.getType().getRangeFormatter(formatHint).format(this);
	}

	public int getPixelExtent() {
		return pixelExtent;
	}

	public int getNumSubdividers() {
		return numSubdividers;
	}

	public IQuantity getSubdivider(int subdivider) {
		return refUnit.quantity(subdividerToRefTransform.targetNumber(subdivider));
	}

	public double getSubdividerPixel(int subdivider) {
		return subdividerToPixelTransform.targetValue((double) subdivider);
	}

	/**
	 * Get the closest lower subdivider index corresponding to {@code value}. Typically used to get
	 * a bucket index. Note that if you need to perform this on many values, all expressed in the
	 * same unit, it is more efficient to use {@link #toSubdividerTransform(IUnit)} once and use
	 * that to transform all numerical values using
	 * {@link IScalarAffineTransform#targetIntFloor(Number)} or so.
	 *
	 * @param value
	 * @return
	 */
	public int getFloorSubdivider(IQuantity value) {
		return toSubdividerTransform(value.getUnit()).targetIntFloor(value.numberValue());
	}

	/**
	 * Get the closest lower subdivider index corresponding to the pixel position
	 * {@code pixel}. Typically used to get a bucket index.
	 *
	 * @param pixel
	 * @return
	 */
	public int getFloorSubdividerAtPixel(Number pixel) {
		// FIXME: Add the inverted transform to the cache instead.
		return subdividerToPixelTransform.invert().targetIntFloor(pixel);
	}

	/**
	 * Get the closest subdivider index corresponding to the pixel position {@code pixel}.
	 * Typically used for snapping to ticks or bucket boundaries.
	 *
	 * @param pixel
	 * @return
	 */
	public int getClosestSubdividerAtPixel(Number pixel) {
		// FIXME: Add the inverted transform to the cache instead.
		// FIXME: Will not clamp correctly during the long to int conversion.
		return (int) Math.round(subdividerToPixelTransform.invert().targetNumber(pixel).doubleValue());
	}

	/**
	 * Get the pixel position corresponding to {@code value}. Note that if you need to perform
	 * this on many values, all expressed in the same unit, it is more efficient to use
	 * {@link #toPixelTransform(IUnit)} once and use that to transform all numerical values using
	 * {@link IScalarAffineTransform#targetValue(double)} or so.
	 *
	 * @param value
	 * @return
	 */
	public double getPixel(IQuantity value) {
		if (value == null) {
			return Double.NaN;
		}
		return toPixelTransform(value.getUnit()).targetNumber(value.numberValue()).doubleValue();
	}

	public IQuantity getQuantityAtPixel(Number pixel) {
		return refUnit.quantity(pixelToRefTransform.targetNumber(pixel));
	}

	public IScalarAffineTransform toSubdividerTransform(IUnit unit) {
		return refToSubdividerTransform.concat(unit.valueTransformTo(refUnit));
	}

	public IScalarAffineTransform toPixelTransform(IUnit unit) {
		return refToPixelTransform.concat(unit.valueTransformTo(refUnit));
	}
}
