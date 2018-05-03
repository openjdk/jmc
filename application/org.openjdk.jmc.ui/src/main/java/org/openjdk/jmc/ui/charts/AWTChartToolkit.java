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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.openjdk.jmc.common.unit.IFormatter;
import org.openjdk.jmc.common.unit.IIncrementalFormatter;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.preferences.PreferenceConstants;

public class AWTChartToolkit {

	public static interface IColorProvider<T> {

		Color getColor(T o);
	}

	private static final int PLOT_RADIUS = 2;
	private static final int TICK_LINE = 3;
	private static final int TICK_SIZE = 6;
	private static final Stroke DASH_STROKE = new BasicStroke(.5f, 0, 0, 1.0f, new float[] {4, 3}, 0);
	private static final BasicStroke EXTRAPOLATION_STROKE = new BasicStroke(1f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, 1f, new float[] {3f, 2f}, 1f);
	private static final Paint EXTRAPOLATION_PAINT;
	// The amount of pixels at the top of the yAxis not to draw
	private static final int Y_AXIS_TOP_SPACE = 1;
	// The size of the arrow (real width/height will be ARROW_SIZE * 2 - 1)
	private static final int ARROW_SIZE = 3;

	private static boolean USE_AA = UIPlugin.getDefault().getPreferenceStore()
			.getBoolean(PreferenceConstants.P_ANTI_ALIASING);

	static {
		BufferedImage bi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
		Graphics2D big = bi.createGraphics();
		big.setColor(new Color(255, 255, 255));
		big.fillRect(0, 0, 5, 5);
		big.setColor(new Color(200, 200, 200));
		big.drawLine(0, 0, 5, 5);
		Rectangle rect = new Rectangle(0, 0, 5, 5);
		EXTRAPOLATION_PAINT = new TexturePaint(bi, rect);

		UIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(e -> USE_AA = (boolean) e.getNewValue());
	}

	public static <T> IColorProvider<T> staticColor(final Color color) {
		return new IColorProvider<T>() {

			@Override
			public Color getColor(T o) {
				return color;
			}

		};
	}

	private static Object getAntiAliasingHint() {
		return USE_AA ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
	}

	/**
	 * Draw a horizontal dashed extrapolation line and optionally a striped fill area below. Please
	 * observe that all the coordinates should be in the actual drawable area, at least roughly, to
	 * avoid <b>huge</b> performance issues on some machines, including some Macs.
	 *
	 * @param ctx
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param fill
	 */
	private static void drawExtrapolation(Graphics2D ctx, int x1, int y1, int x2, int y2, boolean fill) {
		int x = Math.min(x1, x2);
		int y = Math.min(y1, y2);
		int width = Math.abs(x2 - x1);
		int heigth = Math.abs(y2 - y1);
		if (fill) {
			Paint p = ctx.getPaint();
			ctx.setPaint(EXTRAPOLATION_PAINT);
			ctx.fillRect(x, y, width, heigth);
			ctx.setPaint(p);
		}
		Stroke oldStroke = ctx.getStroke();
		ctx.setStroke(EXTRAPOLATION_STROKE);
		/*
		 * On OS X 10.11, at least, these coordinates must be clamped to the visible bounds.
		 * Otherwise it may use huge resources and take time proportional to width (and possibly
		 * height). That is, _seconds_ with moderate zooming, and possibly much worse due to memory
		 * usage.
		 */
		ctx.drawLine(x, y + heigth, x + width, y + heigth);
		ctx.setStroke(oldStroke);
	}

	public static void drawPlot(Graphics2D ctx, IXYDisplayableSet<?, ?> points, int height, boolean fill) {
		Object oldAntiAliasing = ctx.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		ctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, getAntiAliasingHint());
		int diameter = PLOT_RADIUS * 2;
		for (int i = 0; i < points.getSize(); i++) {
			double yCoord = points.getPixelY(i);
			if (!Double.isNaN(yCoord)) {
				int x = (int) points.getPixelX(i) - PLOT_RADIUS;
				int y = height - 1 - (int) yCoord - PLOT_RADIUS;
				if (fill) {
					ctx.fillOval(x, y, diameter + 1, diameter + 1);
				} else {
					ctx.drawOval(x, y, diameter, diameter);
				}
			}
		}
		ctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntiAliasing);
	}

	public static void drawLineChart(
		Graphics2D ctx, IXYDisplayableSet<?, ?> points, int width, int height, boolean fill) {
		Object oldAntiAliasing = ctx.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		ctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, getAntiAliasingHint());

		AffineTransform oldTransform = ctx.getTransform();
		// Flipping integer coordinates 0 to (height - 1). May need to rethink for HiDPI.
		ctx.scale(1, -1);
		ctx.translate(0, 1 - height);

		Polygon p = getLineChart(points);
		int lastPoint = p.npoints - 1;
		/*
		 * On OS X 10.11, at least, these coordinates must be clamped to the visible bounds.
		 * Otherwise it may use huge resources and take time proportional to width (and possibly
		 * height). That is, _seconds_ with moderate zooming, and possibly much worse due to memory
		 * usage.
		 */
		if (p.xpoints[0] > 0) {
			drawExtrapolation(ctx, Math.min(p.xpoints[0], width), p.ypoints[0], 0, 0, fill);
		}
		if (p.xpoints[lastPoint] < width) {
			drawExtrapolation(ctx, Math.max(p.xpoints[lastPoint], 0), p.ypoints[lastPoint], width, 0, fill);
		}

		if (fill) {
			p.ypoints[0] = 0;
			p.ypoints[lastPoint] = 0;
			ctx.fillPolygon(p);
			ctx.setPaint(Color.BLACK);
			ctx.drawPolygon(p);
		} else {
			ctx.drawPolyline(p.xpoints, p.ypoints, p.npoints);
		}

		ctx.setTransform(oldTransform);
		ctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntiAliasing);
	}

	private static Polygon getLineChart(IXYDisplayableSet<?, ?> points) {
		int maxCoordinates = points.getSize() + 2;
		int[] xs = new int[maxCoordinates];
		int[] ys = new int[maxCoordinates];

		int index = 1;
		for (int i = 0; i < points.getSize(); i++) {
			double yCoord = points.getPixelY(i);
			if (!Double.isNaN(yCoord)) {
				xs[index] = (int) points.getPixelX(i);
				ys[index] = (int) yCoord;
				index++;
			}
		}
		xs[0] = xs[1];
		ys[0] = ys[1];
		xs[index] = xs[index - 1];
		ys[index] = ys[index - 1];
		return new Polygon(xs, ys, index > 1 ? ++index : 0);
	}

	private static Polygon getRightAngleChart(IXYDisplayableSet<?, ?> points, int width) {
		int maxCoordinates = points.getSize() * 2 + 2;
		int[] xs = new int[maxCoordinates];
		int[] ys = new int[maxCoordinates];

		int index = 0;
		int currentY = 0;
		for (int i = 0; i < points.getSize(); i++) {
			double yCoord = points.getPixelY(i);
			int nextY = Double.isNaN(yCoord) ? 0 : (int) yCoord;
			if (nextY != currentY) {
				int x = (int) points.getPixelX(i);
				xs[index] = x;
				ys[index] = currentY;
				index++;
				xs[index] = x;
				ys[index] = nextY;
				index++;
				currentY = nextY;
			}
		}
		if (index > 0) {
			xs[index] = width - 1;
			ys[index] = currentY;
			index++;
			xs[index] = width - 1;
			ys[index] = 0;
			index++;
		}
		return new Polygon(xs, ys, index);
	}

	public static void drawRightAngleChart(Graphics2D ctx, IXYDisplayableSet<?, ?> points, int width, int height) {
		Polygon p = getRightAngleChart(points, width);

		AffineTransform oldTransform = ctx.getTransform();
		ctx.scale(1, -1);
		ctx.translate(0, -height);

		ctx.fillPolygon(p);
		ctx.drawPolyline(p.xpoints, p.ypoints, p.npoints);

		ctx.setTransform(oldTransform);
	}

	public static <T> void drawBarChart(
		Graphics2D ctx, IXYDisplayableSet<T[], ?> points, IColorProvider<? super T> cp, int width, int height) {
		AffineTransform oldTransform = ctx.getTransform();
		ctx.scale(1, -1);
		ctx.translate(0, -height);

		Paint paint = ctx.getPaint();
		T[] payload = points.getPayload();

		for (int i = 0; i < points.getSize(); i++) {
			int barHeight = (int) points.getPixelY(i);
			int x1 = (int) points.getPixelX(i);
			int x2 = (int) points.getPixelX(i + 1);
			int barWidth = x2 - x1;
			if (barWidth > 10) {
				barWidth -= 4;
				x1 += 2;
			}
			// FIXME: Draw with gradient fill?
			ctx.setPaint(cp == null ? paint : cp.getColor((payload == null) ? null : payload[i]));
			ctx.fillRect(x1, 0, barWidth, barHeight);
			ctx.setPaint(Color.GRAY);
			ctx.drawRect(x1, 0, barWidth, barHeight);
		}
		ctx.setTransform(oldTransform);
	}

	public static void drawAxis(
		Graphics2D ctx, SubdividedQuantityRange range, int axisPos, boolean labelAhead, int labelLimit,
		boolean vertical) {
		int axisSize = range.getPixelExtent();
		FontMetrics fm = ctx.getFontMetrics();
		int textAscent = fm.getAscent();
		int textYadjust = textAscent / 2;
		int labelYPos = labelAhead ? axisPos - TICK_SIZE : axisPos + TICK_SIZE + textAscent;
		final int labelSpacing;

		if (vertical) {
			ctx.drawLine(axisPos, Y_AXIS_TOP_SPACE, axisPos, axisSize - 1);
			drawUpArrow(ctx, axisPos, Y_AXIS_TOP_SPACE, Math.min(ARROW_SIZE, axisSize - 2));
			labelSpacing = fm.getHeight() - textAscent;
		} else {
			ctx.drawLine(0, axisPos, axisSize - 1, axisPos);
			labelSpacing = fm.charWidth(' ') * 2;
		}

		IRange<IQuantity> firstBucket = QuantityRange.createWithEnd(range.getSubdivider(0), range.getSubdivider(1));
		IQuantity lastShownTick = null;
		final IFormatter<IQuantity> formatter = range.getStart().getType().getFormatterResolving(firstBucket);
		final IIncrementalFormatter changeFormatter;
		if (formatter instanceof IIncrementalFormatter) {
			changeFormatter = (IIncrementalFormatter) formatter;
			if (!vertical && (labelLimit < 0)) {
				lastShownTick = range.getSubdivider(0);
				if (lastShownTick.compareTo(range.getStart()) < 0) {
					lastShownTick = range.getSubdivider(1);
				}
				String label = changeFormatter.formatContext(lastShownTick);
				int labelWidth = fm.stringWidth(label);
				ctx.drawString(label, labelLimit, labelYPos);
				labelLimit += labelWidth + labelSpacing;
			}
		} else {
			changeFormatter = null;
		}

		int numTicks = range.getNumSubdividers();
		for (int i = 0; i < numTicks; i++) {
			int tickPos = (int) range.getSubdividerPixel(i);
			if (tickPos >= axisSize) {
				break;
			} else if (tickPos >= 0) {
				IQuantity currentTick = range.getSubdivider(i);
				final String label;
				if (vertical) {
					ctx.drawLine(axisPos - TICK_LINE, axisSize - 1 - tickPos, axisPos + TICK_LINE,
							axisSize - 1 - tickPos);
					if ((tickPos + textYadjust) >= axisSize) {
						break;
					} else if ((tickPos - textYadjust) >= labelLimit) {
						label = formatter.format(currentTick);
						int labelXPos = labelAhead ? axisPos - TICK_SIZE - fm.stringWidth(label) : axisPos + TICK_SIZE;
						ctx.drawString(label, labelXPos, axisSize - 1 - tickPos + textYadjust);
						labelLimit = tickPos + textYadjust + labelSpacing;
					}
				} else {
					if (changeFormatter != null) {
						label = changeFormatter.formatAdjacent(lastShownTick, range.getSubdivider(i));
					} else {
						label = formatter.format(currentTick);
					}
					ctx.drawLine(tickPos, axisPos - TICK_LINE, tickPos, axisPos + TICK_LINE);
					int textXadjust = fm.stringWidth(label) / 2;
					// FIXME: Decide if truncated labels should be shown
//					if ((tickPos + textXadjust) >= axisSize) {
					if (tickPos >= axisSize) {
						break;
					} else if ((tickPos - textXadjust) >= labelLimit) {
						ctx.drawString(label, tickPos - textXadjust, labelYPos);
						labelLimit = tickPos + textXadjust + labelSpacing;
						lastShownTick = currentTick;
					}
				}
			}
		}
	}

	private static void drawUpArrow(Graphics2D ctx, int axisX, int axisYTop, int size) {
		int yArrow = axisYTop + size;
		ctx.drawLine(axisX - size, yArrow, axisX, axisYTop);
		ctx.drawLine(axisX + size, yArrow, axisX, axisYTop);
	}

	public static void drawGrid(Graphics2D ctx, SubdividedQuantityRange range, int gridSize, boolean verticalAxis) {
		int axisSize = range.getPixelExtent();
		Stroke oldStroke = ctx.getStroke();
		ctx.setStroke(DASH_STROKE);
		int numTicks = range.getNumSubdividers();
		for (int i = 0; i < numTicks; i++) {
			int pos = (int) range.getSubdividerPixel(i);
			if (pos >= axisSize) {
				break;
			} else if (pos >= 0) {
				if (verticalAxis) {
					ctx.drawLine(0, axisSize - 1 - pos, gridSize - 1, axisSize - 1 - pos);
				} else {
					ctx.drawLine(pos, 0, pos, gridSize - 1);
				}

			}
		}
		ctx.setStroke(oldStroke);
	}

	/**
	 * Draw ranges by treating the coordinate pairs of {@code points} not as x and y, but as start
	 * and end on the x axis. As a consequence, {@link IXYDisplayableSet#getWidth()
	 * points.getWidth()} and {@link IXYDisplayableSet#getHeight() points.getHeight()} should return
	 * the same value. (Not to be confused with the {@code height} parameter, which is the actual
	 * number of y pixels that will be filled.)
	 *
	 * @param g2
	 * @param points
	 * @param height
	 * @param fill
	 */
	public static void drawRanges(Graphics2D g2, IXYDisplayableSet<?, ?> points, int height, boolean fill) {
		int width = points.getWidth();
		Shape oldClip = g2.getClip();
		g2.setClip(new Rectangle(width, height));
		for (int n = 0; n < points.getSize(); n++) {
			double x1 = points.getPixelX(n);
			double x2 = points.getPixelY(n);
			int start = x1 < 0 ? -1 : (int) x1;
			int end = x2 > width ? width + 1 : (int) x2;
			if (end > 0 && start < width) {
				if (fill) {
					g2.fillRect(start, 0, end - start, height);
				} else {
					g2.drawRect(start, 0, end - start, height - 1);
				}
			}
		}
		g2.setClip(oldClip);
	}

	/**
	 * Draw spans by treating the coordinate pairs of {@code points} not as x and y, but as start
	 * and end on the x axis. As a consequence, {@link IXYDisplayableSet#getWidth()
	 * points.getWidth()} and {@link IXYDisplayableSet#getHeight() points.getHeight()} should return
	 * the same value. (Not to be confused with the {@code height} parameter, which is the actual
	 * number of y pixels that will be filled.)
	 *
	 * @param g2
	 * @param points
	 * @param height
	 * @param markBoundaries
	 * @param cp
	 */
	public static <T> void drawSpan(
		Graphics2D g2, IXYDisplayableSet<T[], ?> points, int height, boolean markBoundaries,
		IColorProvider<? super T> cp) {
		int width = points.getWidth();
		int[] buffer = new int[width];
		int[] secondBuffer = markBoundaries ? new int[width] : buffer;
		T[] payload = points.getPayload();
		for (int n = 0; n < points.getSize(); n++) {
			T item = payload[n];
			if (item != null) {
				int x1 = (int) points.getPixelX(n);
				int x2 = (int) points.getPixelY(n);
				int start = Math.max(0, Math.min(x1, x2));
				int end = Math.min(width - 1, Math.max(x1, x2));
				int color = cp.getColor(item).getRGB();
				if (markBoundaries && (end - start) > 2) {
					double damp = 0.85 - 3.0 / (end - start);
					int shade = (int) (50 * damp * damp);
					for (int i = start; i <= end; i++) {
						if (shade > 0) {
							buffer[i] = shade(color, shade);
							shade = (int) (shade * damp);
						} else {
							buffer[i] = color;
						}
						secondBuffer[i] = i == start ? Color.BLACK.getRGB() : buffer[i];
					}
				} else {
					for (int i = start; i <= end; i++) {
						secondBuffer[i] = buffer[i] = color;
					}
				}
			}
		}
		BufferedImage image = new BufferedImage(width, 1, BufferedImage.TYPE_INT_ARGB);
		BufferedImage cpImage = markBoundaries ? new BufferedImage(width, 1, BufferedImage.TYPE_INT_ARGB) : image;
		image.setRGB(0, 0, width, 1, buffer, 0, width);
		cpImage.setRGB(0, 0, width, 1, secondBuffer, 0, width);

		for (int n = 0; n < height; n++) {
			if ((n & 2) == 0) {
				g2.drawImage(cpImage, 0, n, null, null);
			} else {
				g2.drawImage(image, 0, n, null, null);
			}
		}
	}

	private static int shade(int color, int shade) {
		return 0xff000000 & color | shift(color, shade, 16) | shift(color, shade, 8) | shift(color, shade, 0);
	}

	private static int shift(int color, int shade, int componentOffset) {
		int comp = ((color >>> componentOffset) & 0xff);
		return (comp > shade ? comp - shade : 0) << componentOffset;
	}

}
