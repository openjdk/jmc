/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.joverflow.ui.swt;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class implements the Squarified algorithm for Treemaps. Using it, it is possible to
 * associate a rectangle to a {@link TreemapItem} element and its children.
 * <p>
 * 
 * @see TreemapItem
 */
class SquarifiedTreemap {

	/*
	 * The algorithm this implements is described in detail here:
	 *
	 * https://bitbucket.org/Ammirate/thermostat-treemap/src/tip/Treemap%20documentation.pdf
	 *
	 * Which is an improvement on:
	 *
	 * Mark Bruls, Kees Huizing and Jarke J. van Wijk, "Squarified Treemaps" in Data Visualization
	 * 2000: Proceedings of the Joint EUROGRAPHICS and IEEE TCVG Symposium on Visualization in
	 * Amsterdam, The Netherlands, May 29–30, 2000. Berlin, Germany: Springer Science & Business
	 * Media, 2012
	 *
	 * The paper itself is also available online at: https://www.win.tue.nl/~vanwijk/stm.pdf
	 */

	/**
	 * List of node to represent as TreeMap.
	 */
	private LinkedList<TreemapItem> elements;

	private double totalRealWeight;

	/**
	 * Represent the area in which draw nodes.
	 */
	private Rectangle2D.Double container;

	private enum DIRECTION {
		LEFT_RIGHT, TOP_BOTTOM
	}

	/**
	 * Indicates the drawing direction.
	 */
	private DIRECTION drawingDir;

	/**
	 * The rectangles area available for drawing.
	 */
	private Rectangle2D.Double availableRegion;

	private double initialArea;

	/**
	 * Maps nodes to their representative rectangles, for all nodes in rows that have been
	 * finalized.
	 */
	private Map<TreemapItem, Rectangle2D.Double> squarifiedMap;

	/**
	 * Coordinates on which to draw.
	 */
	private double lastX = 0;
	private double lastY = 0;

	/**
	 * Performs setup to facilitate the generation of squarified rectangles to represent the nodes
	 * in {@param elements}.
	 *
	 * @param region
	 *            the region that is available for placing rectangles.
	 * @param elements
	 *            the list of nodes to represent, which must be sorted in descending order by weight
	 */
	public SquarifiedTreemap(Rectangle2D.Double region, List<TreemapItem> elements) {
		this.elements = new LinkedList<>();
		this.elements.addAll(Objects.requireNonNull(elements));
		this.totalRealWeight = getRealSum(elements);
		this.container = Objects.requireNonNull(region);
		this.squarifiedMap = new HashMap<>();
	}

	/**
	 * This method prepares for and initiates the process of determining rectangles to represent
	 * nodes.
	 *
	 * @return a map that associates each node with the rectangle that represents it.
	 */
	public Map<TreemapItem, Rectangle2D.Double> squarify() {
		if (elements.isEmpty()) {
			return Collections.emptyMap();
		}

		initialArea = container.getWidth() * container.getHeight();
		availableRegion = new Rectangle2D.Double(container.getX(), container.getY(), container.getWidth(),
				container.getHeight());
		lastX = 0;
		lastY = 0;
		updateDirection();

		List<TreemapItem> row = new ArrayList<>();
		squarifyHelper(elements, row, 0, getPrincipalSide());
		return squarifiedMap;
	}

	/**
	 * Recursively determine the rectangles that represent the set of nodes.
	 *
	 * @param nodes
	 *            remaining nodes to be processed.
	 * @param row
	 *            the nodes that have been included in the row currently under construction.
	 * @param rowArea
	 *            the total area allocated to this row.
	 * @param side
	 *            the length of the side against which to calculate the the aspect ratio.
	 */
	private void squarifyHelper(LinkedList<TreemapItem> nodes, List<TreemapItem> row, double rowArea, double side) {

		if (nodes.isEmpty() && row.isEmpty()) {
			// nothing to do here, just return
			return;
		}
		if (nodes.isEmpty()) {
			// no more nodes to process, just finalize current row
			finalizeRow(row, rowArea);
			return;
		}
		if (row.isEmpty()) {
			// add the first element to the row and process any remaining nodes recursively
			row.add(nodes.getFirst());
			double realWeight = nodes.getFirst().getWeight();
			nodes.removeFirst();
			double nodeArea = (realWeight / totalRealWeight) * initialArea;
			squarifyHelper(nodes, row, nodeArea, side);
			return;
		}

		/*
		 * Determine if adding another rectangle to the current row improves the overall aspect
		 * ratio. If the current row is not (and therefore cannot be) improved then it is finalized,
		 * and the algorithm is run recursively on the remaining nodes that have not yet been placed
		 * in a row.
		 */
		List<TreemapItem> expandedRow = new ArrayList<>(row);
		expandedRow.add(nodes.getFirst());
		double realWeight = nodes.getFirst().getWeight();
		double nodeArea = (realWeight / totalRealWeight) * initialArea;
		double expandedRowArea = rowArea + nodeArea;

		double actualAspectRatio = maxAspectRatio(row, rowArea, side);
		double expandedAspectRatio = maxAspectRatio(expandedRow, expandedRowArea, side);

		if (!willImprove(actualAspectRatio, expandedAspectRatio)) {
			finalizeRow(row, rowArea);
			squarifyHelper(nodes, new ArrayList<>(), 0, getPrincipalSide());
		} else {
			nodes.removeFirst();
			squarifyHelper(nodes, expandedRow, expandedRowArea, side);
		}
	}

	public Map<TreemapItem, Rectangle2D.Double> getSquarifiedMap() {
		return squarifiedMap;
	}

	/**
	 * Recalculate the drawing direction.
	 */
	private void updateDirection() {
		drawingDir = availableRegion.getWidth() > availableRegion.getHeight() ? DIRECTION.TOP_BOTTOM
				: DIRECTION.LEFT_RIGHT;
	}

	/**
	 * Invert the drawing direction.
	 */
	private void invertDirection() {
		drawingDir = drawingDir == DIRECTION.LEFT_RIGHT ? DIRECTION.TOP_BOTTOM : DIRECTION.LEFT_RIGHT;
	}

	/**
	 * For each node in the row, this method creates a rectangle to represent it graphically.
	 *
	 * @param row
	 *            the set of nodes that constitute a row.
	 * @param rowArea
	 *            the area allocated to the row.
	 */
	private void finalizeRow(List<TreemapItem> row, double rowArea) {
		if (row == null || row.isEmpty()) {
			return;
		}

		// greedy optimization step: get the best aspect ratio for nodes drawn
		// on the longer and on the smaller side, to evaluate the best.
		double actualAR = maxAspectRatio(row, rowArea, getPrincipalSide());
		double alternativeAR = maxAspectRatio(row, rowArea, getSecondarySide());

		if (willImprove(actualAR, alternativeAR)) {
			invertDirection();
		}

		Rectangle2D.Double reference = null;
		for (TreemapItem node : row) {
			Rectangle2D.Double r = createRectangle(rowArea, node.getWeight() / getRealSum(row));

			// recalculate coordinates to draw next rectangle
			updateXY(r);

			squarifiedMap.put(node, r);

			if (reference == null) {
				reference = r;
			}
		}
		reduceAvailableArea(reference);
	}

	/**
	 * Create a rectangle that has a size determined by what fraction of the total row area is
	 * allocated to it.
	 *
	 * @param rowArea
	 *            the total area allocated to the row.
	 * @param fraction
	 *            the portion of the total area allocated to the rectangle being created.
	 * @return the created rectangle.
	 */
	private Rectangle2D.Double createRectangle(Double rowArea, Double fraction) {
		double side = getPrincipalSide();
		double w;
		double h;

		if (validate(fraction) == 0 || validate(rowArea) == 0 || validate(side) == 0) {
			return new Rectangle2D.Double(lastX, lastY, 0, 0);
		}

		if (drawingDir == DIRECTION.TOP_BOTTOM) {
			// the length of the secondary side (width here) of the rectangle is consistent between
			// rectangles in the row
			w = rowArea / side;

			// as the width is consistent, the length of the principal side (height here) of the
			// rectangle is proportional to the ratio rectangleArea / rowArea = fraction.
			h = fraction * side;
		} else {
			w = fraction * side;
			h = rowArea / side;
		}
		return new Rectangle2D.Double(lastX, lastY, w, h);
	}

	/**
	 * Ensure that a value is within an expected numeric range
	 *
	 * @param d
	 *            the value to check.
	 * @return 0 if the input is NaN, else the number
	 */
	double validate(double d) {
		if (Double.isNaN(d)) {
			return 0;
		}
		return d;
	}

	/**
	 * Check in which direction the rectangles have to be drawn.
	 *
	 * @return the side on which rectangles will be created.
	 */
	private double getPrincipalSide() {
		return drawingDir == DIRECTION.LEFT_RIGHT ? availableRegion.getWidth() : availableRegion.getHeight();
	}

	/**
	 * @return the secondary available area's side.
	 */
	private double getSecondarySide() {
		return drawingDir == DIRECTION.LEFT_RIGHT ? availableRegion.getHeight() : availableRegion.getWidth();
	}

	private double getRealSum(List<TreemapItem> nodes) {
		double sum = 0;
		for (TreemapItem node : nodes) {
			sum += node.getWeight();
		}
		return sum;
	}

	/**
	 * Recalculate the origin to draw next rectangles.
	 *
	 * @param r
	 *            the rectangle from which recalculate the origin.
	 */
	private void updateXY(Rectangle2D.Double r) {
		if (drawingDir == DIRECTION.LEFT_RIGHT) {
			//lastY doesn't change
			lastX += r.width;
		} else {
			//lastX doesn't change
			lastY += r.height;
		}
	}

	/**
	 * Initialize the origin at the rectangle's origin.
	 *
	 * @param r
	 *            the rectangle used as origin source.
	 */
	private void initializeXY(Rectangle2D.Double r) {
		lastX = r.x;
		lastY = r.y;
	}

	/**
	 * Determine the region that will be available upon finalization of this row.
	 */
	private void reduceAvailableArea(Rectangle2D.Double reference) {
		if (drawingDir == DIRECTION.LEFT_RIGHT) {
			// all rectangles inside the row have the same height
			availableRegion.height -= reference.height;
			availableRegion.y = lastY + reference.height;
			availableRegion.x = reference.x;
		} else {
			// all rectangles inside the row have the same width
			availableRegion.width -= reference.width;
			availableRegion.x = lastX + reference.width;
			availableRegion.y = reference.y;
		}
		updateDirection();
		initializeXY(availableRegion);
	}

	/**
	 * For each node in the row, determine the ratio longer side / shorter side of the rectangle
	 * that would represent it. Return the maximum ratio.
	 *
	 * @param row
	 *            the list of nodes in this row.
	 * @param rowArea
	 *            the area allocated to this row.
	 * @param side
	 *            the length of the side against which to calculate the the aspect ratio.
	 * @return the maximum ratio calculated for this row.
	 */
	private double maxAspectRatio(List<TreemapItem> row, double rowArea, double side) {
		if (row == null || row.isEmpty()) {
			return Double.MAX_VALUE;
		}

		double realSum = getRealSum(row);
		double maxRatio = 0;

		for (TreemapItem node : row) {
			double fraction = node.getWeight() / realSum;
			double length = rowArea / side;
			double width = fraction * side;
			double currentRatio = Math.max(length / width, width / length);

			if (currentRatio > maxRatio) {
				maxRatio = currentRatio;
			}
		}

		return maxRatio;
	}

	/**
	 * This method check which from the values in input, that represent rectangles' aspect ratio,
	 * produces more approximatively a square. It checks if one of the aspect ratio values gives a
	 * value nearest to 1 against the other, which means that width and height are similar.
	 *
	 * @param actualAR
	 *            the actual aspect ratio
	 * @param expandedAR
	 *            the aspect ratio to evaluate
	 * @return false if the actual aspect ratio is better than the new one, else true.
	 */
	private boolean willImprove(double actualAR, double expandedAR) {
		if (actualAR == 0) {
			return true;
		}
		if (expandedAR == 0) {
			return false;
		}
		// check which value is closer to 1, the square's aspect ratio
		double v1 = Math.abs(actualAR - 1);
		double v2 = Math.abs(expandedAR - 1);
		return v1 > v2;
	}
}
