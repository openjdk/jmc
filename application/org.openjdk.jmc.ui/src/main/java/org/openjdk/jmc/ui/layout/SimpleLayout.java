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
package org.openjdk.jmc.ui.layout;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

/**
 * Layout manager that sizes a components children either vertically or horizontally. Each child
 * must have a {@link SimpleLayoutData} that hints to the layout manager how the component should be
 * laid out. See {@link SimpleLayoutData} for more information:
 */
public class SimpleLayout extends Layout {
	/**
	 * Constant that can be used with {@link SimpleLayoutData#setMaxSize()} to indicate that the
	 * control should not have a maximum size.
	 */
	public static final int INFINITE_SIZE = Integer.MAX_VALUE;

	/**
	 * Constant that can be used with {@link SimpleLayoutData#setMaxHorizontalRatio(double)} to
	 * indicate that the control should not use a maximum horizontal ratio .
	 */
	public static final float INFINITE_RATIO = Float.MAX_VALUE;

	/**
	 * Constant that can be used with {@link SimpleLayoutData#setWeight(double)} to indicate that
	 * that the control should grab as much excessive space as possible
	 */
	public static final float INIFINITE_WEIGHT = Float.MAX_VALUE;

	/**
	 * Constant that can be used with {@link SimpleLayoutData#setMinSize()} to indicate that the
	 * control should not have a minimum size.
	 */
	public static final int ZERO_SIZE = 0;

	/**
	 * Constant that can be used with {@link SimpleLayoutData#setWeight(double)} to indicate that
	 * that the control should not grab any excessive space. This is the same as setting the maximum
	 * size to the same as the minimum size.
	 */
	public static final double ZERO_WEIGHT = 0.0;

	/**
	 * Experimental. Constant that indicates that the minimum or maximum size should be the
	 * preferred size of the control.
	 */
	public static final int PREFERRED_SIZE = Integer.MIN_VALUE;

	/**
	 * Class that is used internally that is used when the controls are laid out.
	 */
	private static class CalculationData implements Comparable<CalculationData> {
		public int size;
		public float weight;
		public int maxSize;
		public int minSize;
		public int preferredSize;
		public int prePad;
		public int postPad;
		public double weightPriorityValue;

		@Override
		public int compareTo(CalculationData arg0) {
			return Double.compare(weightPriorityValue, arg0.weightPriorityValue);
		}
	}

	private int m_spacing = 6;
	private int m_margin = 6;
	private int m_style = SWT.FILL;
	private boolean m_horizontalOrientation = true;

	/**
	 * Factory method that creates a layout without margins.
	 *
	 * @return a margin free layout
	 */
	public static SimpleLayout createMarginFree() {
		SimpleLayout layout = new SimpleLayout();
		layout.setMargin(0);
		layout.setSpacing(0);
		return layout;
	}

	/**
	 * Sets if the layout manager should layout the child controls horizontally or vertically..
	 *
	 * @param horizontal
	 *            , true if the controls should be layout horizontally.
	 */
	public void setHorizontalOrientation(boolean horizontal) {
		m_horizontalOrientation = horizontal;
	}

	/**
	 * Flag that determines how the control should be aligned if there is space that no control
	 * wants to grab. Available flags are:
	 * <li>SWT.FILL<br>
	 * Control are spread out evenly
	 * <li>SWT.BEGINNING<br>
	 * Controls are aligned at the the top or at left depending on the orientation
	 * <li>SWT.END<br/>
	 * Controls align are at the right or at bottom depending on the orientation <br/>
	 * <br/>
	 * Default style is SWT.FILL
	 *
	 * @param style
	 *            flag indicating where controls should be place if there is excessive space no
	 *            control wants to use.
	 */
	public void setAlignment(int style) {
		m_style = style;
	}

	/**
	 * Returns the child controls should be laid out horizontally or not.
	 *
	 * @return true if the orientation is horizontal
	 */
	public boolean getHorizontalOrientation() {
		return m_horizontalOrientation;
	}

	/**
	 * Sets the margin that will be used on all edges.
	 *
	 * @param pixels
	 *            the margin size in pixels.
	 */
	public void setMargin(int pixels) {
		m_margin = pixels;
	}

	/**
	 * Returns the margin that is used for all edges(top, bottom, left, right)
	 *
	 * @return the margin size, in pixels.
	 */
	public int getMargin() {
		return m_margin;
	}

	/**
	 * Sets how much space should be between controls.
	 *
	 * @param spacing
	 *            the number of pixels between controls.
	 */
	public void setSpacing(int spacing) {
		m_spacing = spacing;
	}

	/**
	 * Returns how much space there should be between controls.
	 *
	 * @return the number of pixel there should be between control
	 */
	public int getSpacing() {
		return m_spacing;
	}

	/**
	 * Calculates minimum, maximum and weight for the constraints held by the
	 * {@link SimpleLayoutData} in an array of controls.
	 *
	 * @param controls
	 *            the control to calculate the calculation data for
	 * @param size
	 *            the size of the parent side that should be laid out.
	 * @param other
	 *            the size of the parent side that should NOT be laid out,
	 * @return an array of {@link CalculationData}
	 */
	protected CalculationData[] createCalculationData(Control[] controls, int size, int otherSize) {
		CalculationData[] cd = createCalculationDataArray(controls.length);
		SimpleLayoutData[] md = createLayoutDataArray(controls);

		fillPreferredSize(controls, cd, otherSize);
		fillMinAndMaxSize(md, cd);
		fillWeight(md, cd, size);
		if (getHorizontalOrientation()) {
			fillHorizontalRatio(md, cd, size, otherSize);
		}
		return cd;
	}

	/**
	 * Create an array of {@link CalculationData} and fills it with {@link CalculationData} objects.
	 *
	 * @param size
	 *            the size of the array to create.
	 * @return an array of {@link CalculationData}
	 */
	protected CalculationData[] createCalculationDataArray(int size) {
		CalculationData[] cd = new CalculationData[size];
		for (int n = 0; n < size; n++) {
			cd[n] = new CalculationData();
		}
		return cd;
	}

	/**
	 * Fills the an array of {@link CalculationData} with the preferred size for an array
	 * {@link Control}
	 *
	 * @param controls
	 *            the control to calculate the preferred size for
	 * @param cd
	 *            the {@link CalculationData} to store the preferred size in
	 */
	protected void fillPreferredSize(Control[] controls, CalculationData[] cd, int otherSize) {
		for (int n = 0; n < cd.length; n++) {
			if (getHorizontalOrientation()) {
				cd[n].preferredSize = controls[n].computeSize(SWT.DEFAULT, otherSize).x;
			} else {
				cd[n].preferredSize = controls[n].computeSize(otherSize, SWT.DEFAULT).y;
			}
		}
	}

	/**
	 * Sets the minimum and maximum size in the calculation {@link CalculationData} in such a way
	 * that it does not violate the horizontal aspect ratio set in the {@link SimpleLayoutData}.
	 *
	 * @param layoutData
	 * @param calculationData
	 * @param otherSize
	 */
	protected void fillHorizontalRatio(
		SimpleLayoutData[] layoutData, CalculationData[] calculationData, int horizontal, int verticalSize) {
		if (getHorizontalOrientation()) {
			for (int n = 0; n < layoutData.length; n++) {
				SimpleLayoutData ld = layoutData[n];
				CalculationData cd = calculationData[n];
				if (ld.getMinimumHorizontalRatio() > 0) {
					cd.minSize = Math.max(cd.minSize, (int) (ld.getMinimumHorizontalRatio() * verticalSize + .5));
				}
				if (ld.getMaximumHorizontalRatio() < SimpleLayout.INFINITE_RATIO) {
					cd.maxSize = Math.min(cd.maxSize, (int) (ld.getMaximumHorizontalRatio() * verticalSize + .5));
				}
				// Ensure cd.minSize <= cd.maxSize
				cd.minSize = Math.min(cd.minSize, cd.maxSize);
				cd.maxSize = Math.max(cd.minSize, cd.maxSize);
			}
		}
	}

	/**
	 * Fill the {@link CalculationData} with the weights in {@link SimpleLayoutData}. The weight are
	 * normalized and converted to integers so they can be modulo the size that needs to
	 * distributed.
	 *
	 * @param layoutData
	 * @param calculationData
	 * @param size
	 */
	// FIXME: Implement a more efficient way to distribute pixels according to a weight factor.
	protected void fillWeight(SimpleLayoutData[] layoutData, CalculationData[] calculationData, int size) {
		for (int n = 0; n < calculationData.length; n++) {
			CalculationData cd = calculationData[n];
			cd.weight = layoutData[n].getWeight();
			cd.weightPriorityValue = cd.weight;
			ensurePositive(cd.weight, "Weight"); //$NON-NLS-1$
			if (cd.weight == 0.0) {
				cd.maxSize = cd.minSize;
			}
		}
	}

	/**
	 * Ensures that a value is positive, otherwise an {@link IllegalArgumentException} is thrown
	 *
	 * @param value
	 *            the value examine
	 * @param valueName
	 *            the name of the value to refer to in the exception.
	 */
	private void ensurePositive(double value, String valueName) {
		if (value < 0.0) {
			throw new IllegalArgumentException(valueName + " value must be a positive value."); //$NON-NLS-1$
		}
	}

	/**
	 * Fill {@link CalculationData} with minimums and maximums from the {@link SimpleLayoutData}
	 *
	 * @param layoutData
	 *            the source {@link SimpleLayoutData}
	 * @param calculationData
	 *            the destination {@link CalculationData}
	 */
	protected void fillMinAndMaxSize(SimpleLayoutData[] layoutData, CalculationData[] calculationData) {
		for (int n = 0; n < layoutData.length; n++) {
			CalculationData cd = calculationData[n];
			cd.minSize = layoutData[n].getMinimumSize();
			cd.maxSize = layoutData[n].getMaximumSize();

			if (cd.minSize == SimpleLayout.PREFERRED_SIZE) {
				cd.minSize = cd.preferredSize;
			}

			if (cd.maxSize == SimpleLayout.PREFERRED_SIZE) {
				cd.maxSize = cd.preferredSize;
			}
			ensurePositive(cd.minSize, "Minimum size"); //$NON-NLS-1$
			ensurePositive(cd.maxSize, "Maximum size"); //$NON-NLS-1$

			if (cd.maxSize < cd.minSize) {
				cd.minSize = cd.maxSize;
			}
		}
	}

	/* See {@link Layout#computeSize(Composite, int, int, boolean)} */
	@Override
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
		if (composite.getChildren().length != 0) {
			if (!getHorizontalOrientation()) {
				return new Point(computeMinimumWidth(composite, wHint, flushCache), calculateMinimumSize(composite));
			} else {
				return new Point(calculateMinimumSize(composite), computeMinimumHeight(composite, hHint, flushCache));
			}
		}

		return new Point(wHint == SWT.DEFAULT ? 2 * m_margin : wHint, hHint == SWT.DEFAULT ? 2 * m_margin : hHint);
	}

	private int computeMinimumWidth(Composite composite, int wHint, boolean flushCache) {
		int maxWidth = 0;
		for (Control child : composite.getChildren()) {
			Point size = child.computeSize(wHint, SWT.DEFAULT, flushCache);
			maxWidth = Math.max(maxWidth, size.x);
		}
		return maxWidth + 2 * m_margin;
	}

	private int computeMinimumHeight(Composite composite, int hHint, boolean flushCache) {
		int maxHeight = 0;
		for (Control child : composite.getChildren()) {
			Point size = child.computeSize(SWT.DEFAULT, hHint, flushCache);
			maxHeight = Math.max(maxHeight, size.y);
		}
		return maxHeight + 2 * m_margin;
	}

	/**
	 * calculates the minimum size for a control that has an {@link SimpleLayout}
	 *
	 * @param composite
	 *            the composite whose children will be part of the calculation
	 * @return the minimum size
	 */
	protected int calculateMinimumSize(Composite composite) {
		int totalSize = 2 * m_margin;
		Control[] children = composite.getChildren();
		for (Control element : children) {
			SimpleLayoutData sd = getSimpleLayoutData(element);
			int size = Math.min(sd.getMinimumSize(), sd.getMaximumSize());
			if (size > 0) {
				totalSize += size + m_spacing;
			}
		}
		return totalSize;
	}

	/**
	 * Retrieves the simple layout data object from the control. If none is present or of wrong
	 * class an exception is thrown.
	 *
	 * @param control
	 *            the control to operate on
	 * @return the {@link SimpleLayoutData} object for the control
	 * @throws IllegalStateException
	 *             if no layout data object is present or it is of wrong type
	 */
	private SimpleLayoutData getSimpleLayoutData(Control control) throws IllegalStateException {
		Object ld = control.getLayoutData();
		if (!(ld instanceof SimpleLayoutData)) {
			throw new IllegalStateException("SimpleLayout requires that all children have SimpleLayoutData set"); //$NON-NLS-1$
		}
		return (SimpleLayoutData) ld;
	}

	/**
	 * Creates an with {@link SimpleLayoutData} from extracted {@link SimpleLayoutData} that is
	 * available in an array of controls
	 *
	 * @param controls
	 * @return
	 */
	protected SimpleLayoutData[] createLayoutDataArray(Control[] controls) {
		SimpleLayoutData[] md = new SimpleLayoutData[controls.length];
		for (int n = 0; n < controls.length; n++) {
			md[n] = getSimpleLayoutData(controls[n]);
		}
		return md;
	}

	/* See {@link Composite#layout(boolean, boolean)} */
	@Override
	protected void layout(Composite composite, boolean flushCache) {
		Control[] controls = getVisibleChildren(composite);
		if (controls.length > 0) {
			Rectangle clientArea = composite.getClientArea();

			int fixedOrientation = getHorizontalOrientation() ? clientArea.height : clientArea.width;
			int dynamicOrientation = getHorizontalOrientation() ? clientArea.width : clientArea.height;

			int totalSpacing = (controls.length - 1) * m_spacing;
			int totalMargin = 2 * m_margin;

			fixedOrientation -= totalMargin;
			dynamicOrientation -= totalMargin + totalSpacing;

			if (dynamicOrientation > 0 && fixedOrientation > 0) {
				layoutControls(controls, dynamicOrientation, fixedOrientation);
			}
		}
	}

	/**
	 * Layouts controls
	 *
	 * @param controls
	 *            the controls to lay out
	 * @param dynamic
	 *            the size of the dynamic side
	 * @param fixed
	 *            size of the fixed side
	 */
	private void layoutControls(Control[] controls, int dynamic, int fixed) {
		CalculationData[] data = createCalculationData(controls, dynamic, fixed);
		int leftOver = distributeMinSize(data, dynamic);
		leftOver = distributeWeights(data, leftOver);

		switch (getAlignment()) {
		case SWT.BEGINNING:
			layoutOut(controls, data, fixed, 0);
			break;
		case SWT.FILL:
			distributeFillPad(data, leftOver);
			layoutOut(controls, data, fixed, 0);
			break;
		case SWT.END:
			layoutOut(controls, data, fixed, leftOver);
			break;
		default:
			throw new IllegalArgumentException(
					"Invalid alignment encountered. Only SWT.FIll, SWT.BEGINNING and SWT.END are allowed"); //$NON-NLS-1$
		}
	}

	/**
	 * make sure all controls have the minimum size.
	 *
	 * @param data
	 *            the {@link CalculationData}
	 * @param size
	 *            the amount of space available to distribute. Will be distribute even if space is
	 *            missing
	 * @return the amount space available after all the minimum sizes have been distributed.
	 */
	private int distributeMinSize(CalculationData[] data, int size) {
		for (int n = 0; n < data.length; n++) {
			if (data[n].minSize > data[n].size) {
				size -= (data[n].minSize - data[n].size);
				data[n].size = data[n].minSize;
			}
		}
		return size;
	}

	@Override
	protected boolean flushCache(Control control) {
		return true;
	}

	/**
	 * Returns all the visible children for a {@link Composite}
	 *
	 * @param composite
	 *            parent composite whose visible children will be returned.
	 * @return visible controls
	 */
	private Control[] getVisibleChildren(Composite composite) {
		Control[] children = composite.getChildren();
		ArrayList<Control> list = new ArrayList<>();
		for (Control element : children) {
			SimpleLayoutData md = getSimpleLayoutData(element);
			if (md.isVisible()) {
				list.add(element);
			}
		}
		return list.toArray(new Control[list.size()]);
	}

	/**
	 * Calculates the padding that is necessary if the SWT.FILL alignment is used.
	 *
	 * @param data
	 *            the {@link CalculationData}
	 * @param size
	 *            the size that needs to be filled up
	 * @param size
	 */
	private void distributeFillPad(CalculationData[] data, int size) {
		if (size > 0) {
			int count = data.length * 2;
			float averageSpill = (float) size / count;
			float spillDistributed = 0.0f;
			for (int n = 0; n < count; n++) {
				int spill = (int) ((n + 1) * averageSpill - spillDistributed);
				if (n % 2 == 0) {
					data[n / 2].prePad += spill;
				} else {
					data[n / 2].postPad += spill;
				}
				spillDistributed += spill;
			}
		}
	}

	/**
	 * Returns the align for the controls. See {@link #setAlignment(int)}
	 *
	 * @return the alignment (SWT.FILL, SWT.BEGINING or SWT.END)
	 */
	public int getAlignment() {
		return m_style;
	}

	/**
	 * Layouts the controls according the size in {@link CalculationData}
	 *
	 * @param controls
	 *            the controls that should be layed out
	 * @param data
	 *            the {@link CalculationData}
	 * @param fixedOrientationSize
	 *            the size of the side that is not control by the layout manager.
	 */
	protected void layoutOut(Control[] controls, CalculationData[] data, int fixedOrientationSize, int pixelPosition) {
		for (int n = 0; n < data.length; n++) {
			pixelPosition += data[n].prePad;
			if (getHorizontalOrientation()) {
				controls[n].setBounds(m_margin + pixelPosition, m_margin, data[n].size, fixedOrientationSize);
			} else {
				controls[n].setBounds(m_margin, m_margin + pixelPosition, fixedOrientationSize, data[n].size);
			}
			pixelPosition += data[n].size + data[n].postPad + m_spacing;
		}
	}

	/**
	 * Distributes pixels according to a weight factor. Precondition, size in
	 * {@link CalculationData} must be less or equal to maxSize.
	 *
	 * @param data
	 *            an array of {@link CalculationData}
	 * @param size
	 *            the size, or number of pixels, that should be distributed among the controls
	 *            represented by the {@link CalculationData}
	 */
	protected static int distributeWeights(CalculationData[] unSortedData, int size) {
		// Uses s poor mans priority heap. Adds a pixel to the data with the
		// lowest weightPriority value, increases the weightPriority and then
		// bubbles the top element down the heap, in linear time. Repeat until
		// all pixels have been distributed or when all controls have reached
		// their maximum value. Could be done more efficiently, but this is a
		// pretty robust implementation.
		if (size > 0) {
			int topElement = 0;
			CalculationData[] heap = unSortedData.clone();
			Arrays.sort(heap);
			do {
				if (heap[topElement].size < heap[topElement].maxSize) {
					heap[topElement].size++;
					heap[topElement].weightPriorityValue += heap[topElement].weight;
					bubbleTopElementDown(topElement, heap);
					size--;
				} else {
					topElement++;
				}
			} while (size > 0 && topElement < heap.length);
		}

		return size;
	}

	/**
	 * Bubble the top element down the heap so it's always in order
	 *
	 * @param heap
	 *            the {@link CalculationData} for the heap.
	 */
	private static void bubbleTopElementDown(int topElement, CalculationData[] heap) {
		for (int n = topElement; n < heap.length - 1
				&& heap[n].weightPriorityValue > heap[n + 1].weightPriorityValue; n++) {
			CalculationData tmp = heap[n];
			heap[n] = heap[n + 1];
			heap[n + 1] = tmp;
		}
	}
}
