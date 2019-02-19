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

import org.openjdk.jmc.common.IDisplayable;

/**
 * Visitor interface to gather information about values displayed in charts, typically in the
 * vicinity of some coordinates. Suitable for tooltips, highlighting or snapping.
 */
public interface IChartInfoVisitor {
	public abstract class Adapter implements IChartInfoVisitor {

		@Override
		public boolean enterScope(String context, boolean fullyShown) {
			return false;
		}

		@Override
		public void leaveScope() {
		}

		@Override
		public void visit(IBucket bucket) {
		}

		@Override
		public void visit(IPoint point) {
		}

		@Override
		public void visit(ISpan span) {
		}

		@Override
		public void visit(ITick tick) {
		}

		@Override
		public void visit(ILane lane) {
		}
	}

	public interface IBucket {
		/**
		 * Get a bucket equivalent to this bucket, but guaranteed to be unchanged at least until the
		 * chart changes state.
		 *
		 * @return
		 */
		IBucket keeper();

		String getName();

		Color getColor();

		Rectangle2D getTarget();

		IDisplayable getRange();

		IDisplayable getStartX();

		IDisplayable getEndX();

		IDisplayable getWidth();

		IDisplayable getY();

		Object getPayload();
	}

	public interface IPoint {
		/**
		 * Get a point equivalent to this point, but guaranteed to be unchanged at least until the
		 * chart changes state.
		 *
		 * @return
		 */
		IPoint keeper();

		String getName();

		Color getColor();

		Point2D getTarget();

		IDisplayable getX();

		IDisplayable getY();
	}

	public interface ISpan {
		/**
		 * Get a span equivalent to this span, but guaranteed to be unchanged at least until the
		 * chart changes state.
		 *
		 * @return
		 */
		ISpan keeper();

		Color getColor();

		Rectangle2D getTarget();

		IDisplayable getRange();

		IDisplayable getStartX();

		IDisplayable getEndX();

		IDisplayable getWidth();

		Object getPayload();

		String getDescription();
	}

	public interface ITick {
		Point2D getTarget();

		IDisplayable getValue();
	}

	public interface ILane {
		String getLaneName();

		String getLaneDescription();
	}

	/**
	 * Enter a context scope described by {@code context}. Scopes may be nested.
	 *
	 * @param context
	 * @param fullyShown
	 *            true if the entire {@code context} string is fully visible in the GUI
	 * @return true to receive a {@link #leaveScope()} when this context goes out of scope.
	 */
	boolean enterScope(String context, boolean fullyShown);

	void leaveScope();

	/**
	 * Visit a bucket in a histogram.
	 * <p>
	 * Note that the provided {@link IBucket} instance may be reused and thus cannot be directly
	 * saved by the visitor. Visitors wishing to delay processing of {@link IBucket}s, can do so by
	 * requesting an instance that will remain valid at least until the chart changes state, through
	 * the {@link IBucket#keeper()} method.
	 *
	 * @param bucket
	 */
	void visit(IBucket bucket);

	/**
	 * Visit a data point in a line chart or similar.
	 * <p>
	 * Note that the provided {@link IPoint} instance may be reused and thus cannot be directly
	 * saved by the visitor. Visitors wishing to delay processing of {@link IPoint}s, can do so by
	 * requesting an instance that will remain valid at least until the chart changes state, through
	 * the {@link IPoint#keeper()} method.
	 *
	 * @param point
	 */
	void visit(IPoint point);

	/**
	 * Visit a span in a Gantt chart or similar.
	 * <p>
	 * Note that the provided {@link ISpan} instance may be reused and thus cannot be directly saved
	 * by the visitor. Visitors wishing to delay processing of {@link ISpan}s, can do so by
	 * requesting an instance that will remain valid at least until the chart changes state, through
	 * the {@link ISpan#keeper()} method.
	 *
	 * @param span
	 */
	void visit(ISpan span);

	/**
	 * Visit a tick mark (or a bucket boundary/sub tick mark) on a chart axis.
	 * <p>
	 * The provided {@link ITick} instance may be directly saved by the visitor and will remain
	 * valid at least until the chart changes state.
	 *
	 * @param tick
	 */
	void visit(ITick tick);

	/**
	 * Visits the header part of a line chart, normally a caption in the form of a label.
	 *
	 * @param lane
	 */
	void visit(ILane lane);
}
