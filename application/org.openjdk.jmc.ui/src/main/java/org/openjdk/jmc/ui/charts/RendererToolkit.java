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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RendererToolkit {

	private static final IXDataRenderer EMPTY = new IXDataRenderer() {

		@Override
		public IRenderedRow render(Graphics2D context, SubdividedQuantityRange xRange, int height) {
			return new RenderedRowBase(height);

		}

	};

	private static class LayeredRenderer implements IXDataRenderer {
		private final List<IXDataRenderer> layers;

		LayeredRenderer(List<IXDataRenderer> layers) {
			this.layers = layers;
		}

		@Override
		public IRenderedRow render(Graphics2D context, SubdividedQuantityRange xRange, int height) {
			final List<IRenderedRow> results = new ArrayList<>(layers.size());
			String text = null;
			String description = null;
			Object payload = null;
			List<IRenderedRow> subdivision = Collections.emptyList();
			for (IXDataRenderer layer : layers) {
				IRenderedRow result = layer.render(context, xRange, height);
				results.add(result);
				// Beware that this picks the last text, payload and subdivision from its layers.
				// This could be confusing if more than one layer has those things.
				if (result.getName() != null && !result.getName().isEmpty()) {
					text = result.getName();
				}
				if (result.getDescription() != null && !result.getDescription().isEmpty()) {
					description = result.getDescription();
				}
				if (result.getPayload() != null) {
					payload = result.getPayload();
				}
				if (!result.getNestedRows().isEmpty()) {
					subdivision = result.getNestedRows();
				}
			}
			return new RenderedRowBase(subdivision, height, text, description, payload) {

				@Override
				public void infoAt(IChartInfoVisitor visitor, int x, int y, Point offset) {
					for (IRenderedRow rr : results) {
						rr.infoAt(visitor, x, y, offset);
					}
				}
			};
		}

	}

	private static class CompositeRenderer implements IXDataRenderer {

		private static final Color MISMATCH_CONTENT_BG = new Color(240, 240, 240, 190);
		private static final String NO_CONTENT_MSG = Messages
				.getString(Messages.RendererToolkit_NO_CONTENT);
		private static final String TOO_MUCH_CONTENT_MSG = Messages
				.getString(Messages.RendererToolkit_TOO_MUCH_CONTENT);
		private final List<IXDataRenderer> children;
		private final List<Double> weights;
		private final String text;
		private final double totalWeight;

		CompositeRenderer(List<IXDataRenderer> children, String text, List<Double> weights) {
			this.children = children;
			this.text = text;
			this.weights = weights;
			if (weights == null) {
				totalWeight = children.size();
			} else {
				double sum = 0;
				for (Double w : weights) {
					sum += w;
				}
				totalWeight = sum;
			}
		}

		@Override
		public IRenderedRow render(Graphics2D context, SubdividedQuantityRange xRange, int height) {
			List<IRenderedRow> result = new ArrayList<>(children.size());
			AffineTransform oldTransform = context.getTransform();
			int heightLeft = height;
			double weightLeft = totalWeight;
			for (int i = 0; i < children.size(); i++) {
				double rowWeight = weights == null ? 1 : weights.get(i);
				int rowHeight = (int) Math.round(heightLeft / weightLeft * rowWeight);
				weightLeft -= rowWeight;
				if (rowHeight > 0) {
					heightLeft -= rowHeight;
					result.add(children.get(i).render(context, xRange, rowHeight));
					context.translate(0, rowHeight);
				}
			}
			context.setTransform(oldTransform);
			if (result.size() != children.size()) {
				String displayMessage = result.size() == 0 ? NO_CONTENT_MSG : TOO_MUCH_CONTENT_MSG;
				result = Collections.emptyList();
				context.setPaint(MISMATCH_CONTENT_BG);
				context.fillRect(0, 0, xRange.getPixelExtent(), height);
				// FIXME: Draw something nice.
				Font orgFont = context.getFont();
				Font italicFont = orgFont.deriveFont(Font.ITALIC);
				FontMetrics fm = context.getFontMetrics(italicFont);
				int msgWidth = fm.stringWidth(displayMessage);
				if (height > fm.getHeight() && xRange.getPixelExtent() > msgWidth) {
					context.setFont(italicFont);
					context.setPaint(Color.BLACK);
					context.drawString(displayMessage, (xRange.getPixelExtent() - msgWidth) / 2,
							(height - fm.getHeight()) / 2 + fm.getAscent());
					context.setFont(orgFont);
				}
			}

			return new RenderedRowBase(result, height, text, null, null) {

				@Override
				public void infoAt(IChartInfoVisitor visitor, int x, int y, Point offset) {
					boolean notifyLeave = false;
					if (text != null) {
						// FIXME: Use stored state for fullyShown?
						notifyLeave = visitor.enterScope(text, (getHeight() > 20) && (text.length() <= 26));
					}
					int yRowStart = 0;
					for (IRenderedRow nestedRow : getNestedRows()) {
						int yRowEnd = yRowStart + nestedRow.getHeight();
						if (yRowStart > y) {
							break;
						} else if (yRowEnd > y) {
							Point newOffset = new Point(offset.x, offset.y + yRowStart);
							nestedRow.infoAt(visitor, x, y - yRowStart, newOffset);
						}
						yRowStart = yRowEnd;
					}
					if (notifyLeave) {
						visitor.leaveScope();
					}
				}
			};
		}
	}

	public static IXDataRenderer layers(IXDataRenderer ... layers) {
		return layers(Arrays.asList(layers));
	}

	public static IXDataRenderer layers(List<IXDataRenderer> layers) {
		return layers.isEmpty() ? empty() : new LayeredRenderer(layers);
	}

	public static IXDataRenderer uniformRows(List<IXDataRenderer> rows) {
		return uniformRows(rows, null);
	}

	public static IXDataRenderer uniformRows(List<IXDataRenderer> rows, String text) {
		return weightedRows(rows, text, null);
	}

	public static IXDataRenderer weightedRows(List<IXDataRenderer> rows, List<Double> weights) {
		return weightedRows(rows, null, weights);
	}

	public static IXDataRenderer weightedRows(List<IXDataRenderer> rows, String text, List<Double> weights) {
		return rows.isEmpty() ? empty() : new CompositeRenderer(rows, text, weights);
	}

	public static IXDataRenderer empty() {
		return EMPTY;
	}

}
