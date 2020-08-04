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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Instances of this class represent a selectable user interface object that represents a node in a
 * treemap widget.
 */
public class TreemapItem extends Item {
	private static final String ELLIPSIS = "..."; //$NON-NLS-1$
	private static final int HORIZONTAL_PADDING = 13;
	private static final int VERTICAL_PADDING = 13;
	private static final int MIN_SIZE = 1;

	private Treemap parent;
	private TreemapItem parentItem;
	private List<TreemapItem> children = new ArrayList<>();

	private Color background = null;
	private Color foreground = null;
	private Font font = null;

	private Rectangle bounds = null;
	private Rectangle textBounds = null;

	private double realWeight = 0; // the weight of the node
	// the cached sum of all direct children's apparent weights + realWeight. -1 indicates not yet cached
	private double apparentWeight = -1;

	private String toolTipText = ""; //$NON-NLS-1$

	// to be disposed
	private Color darkenBackground = null;

	/**
	 * Constructs a new instance of this class and inserts it into the parent treemap. The new item
	 * is inserted as a direct child of the root.
	 *
	 * @param parent
	 *            a treemap control which will be the parent of the new instance (cannot be null)
	 * @param style
	 *            the style of control to construct
	 */
	public TreemapItem(Treemap parent, int style) {
		this(Treemap.checkNull(parent), parent.getRootItem(), style);
	}

	/**
	 * Constructs TreeItem and inserts it into Tree. The new item is inserted as direct child of the
	 * specified item..
	 *
	 * @param parentItem
	 *            a treemap item which will be the parent of the new instance (cannot be null)
	 * @param style
	 *            the style of control to construct
	 */
	public TreemapItem(TreemapItem parentItem, int style) {
		this(checkNull(parentItem).parent, parentItem, style);
	}

	private TreemapItem(Treemap parent, TreemapItem parentItem, int style) {
		super(parent, style);

		if ((style & SWT.VIRTUAL) == SWT.VIRTUAL) {
			throw new UnsupportedOperationException("SWT.VIRTUAL is not support by TreemapItem"); //$NON-NLS-1$
		}

		this.parent = parent;
		this.parentItem = parentItem;

		if (parentItem != null) {
			// adding a 0 weighted node to the end of decreasingly sorted list preserves the sorted structure
			parentItem.children.add(this);
		}
	}

	static TreemapItem checkNull(TreemapItem item) {
		if (item == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		return item;
	}

	private void sortChildren() {
		children.sort(Comparator.comparingDouble(TreemapItem::getWeight).reversed());
	}

	void updateAncestor() {
		// update apparentWeight for all ancestors
		for (TreemapItem ancestor = parentItem; ancestor != null; ancestor = ancestor.parentItem) {
			ancestor.sortChildren();
			ancestor.cacheApparentWeight();
		}
	}

	private void clearThis() {
		this.realWeight = 0;
		this.apparentWeight = -1;
		this.foreground = null;
		this.background = null;
		this.font = null;

		if (darkenBackground != null && !darkenBackground.isDisposed()) {
			darkenBackground.dispose();
		}
		this.darkenBackground = null;

		this.setData(null);
		this.setText(""); //$NON-NLS-1$
		this.setToolTipText(""); //$NON-NLS-1$

		updateAncestor();
	}

	private void cacheApparentWeight() {
		double sum = 0;
		for (TreemapItem child : children) {
			sum += child.getWeight();
		}

		sum += realWeight;
		apparentWeight = sum;
	}

	void paintItem(GC gc, Rectangle bounds, boolean all) {
		this.bounds = bounds;

		Color bg = gc.getBackground();
		Color fg = gc.getForeground();
		Font font = gc.getFont();

		// fill background
		gc.setBackground(parent.getSelection() == this ? getDarkenBackground() : getBackground());
		gc.fillRectangle(bounds);

		if (getParent().getBordersVisible()) {
			gc.setForeground(getDarkenBackground());
			gc.drawRectangle(bounds);
		}

		gc.setFont(getFont());
		paintTextIfPossible(gc);
		if (all) {
			paintChildrenIfPossible(gc);
		} else {
			for (TreemapItem child : getItems()) {
				child.clearBounds(true);
			}
		}

		gc.setBackground(bg);
		gc.setForeground(fg);
		gc.setFont(font);
	}

	// add label to tile if space permits
	private void paintTextIfPossible(GC gc) {
		String text = getText();
		if (text == null || text.equals("")) { //$NON-NLS-1$
			return;
		}

		if (!tryPaintText(gc, text)) {
			tryPaintText(gc, ELLIPSIS);
		}
	}

	private boolean tryPaintText(GC gc, String text) {
		Rectangle textBounds;
		if (getParent().getBordersVisible()) {
			textBounds = new Rectangle(bounds.x, bounds.y, bounds.width - 2, bounds.height - 2); // -2 for the border
		} else {
			textBounds = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
		}

		Point textExtent = gc.textExtent(text);

		if (textExtent.x > textBounds.width || textExtent.y > textBounds.height) {
			this.textBounds = null;
			return false;
		}

		textBounds.width = textExtent.x;
		textBounds.height = textExtent.y;

		gc.setFont(getFont());
		gc.setForeground(getForeground());

		if (getParent().getBordersVisible()) {
			gc.drawText(text, bounds.x + 1, bounds.y + 1); // +1 so it doesn't overlap with the border
		} else {
			gc.drawText(text, bounds.x, bounds.y);
		}

		this.textBounds = textBounds;
		return true;
	}

	// add child tiles if space permits
	private void paintChildrenIfPossible(GC gc) {
		// calculate available sub region for child tiles
		Rectangle2D.Double availableRegion;
		{
			double w = Math.max(0, bounds.width - 2 * HORIZONTAL_PADDING);
			double h = Math.max(0, bounds.height - 2 * VERTICAL_PADDING);
			if (textBounds != null && textBounds.height > VERTICAL_PADDING) {
				h = h - textBounds.height + VERTICAL_PADDING;
			}
			availableRegion = new Rectangle2D.Double(0, 0, w, h);
		}

		if (availableRegion.width == 0 || availableRegion.height == 0) {
			return;
		}

		// calculate child rectangles
		List<TreemapItem> elements = Arrays.asList(getItems());
		SquarifiedTreemap algorithm = new SquarifiedTreemap(availableRegion, elements);
		Map<TreemapItem, Rectangle2D.Double> squarifiedMap = algorithm.squarify();

		for (TreemapItem item : elements) {
			Rectangle2D.Double childRect = squarifiedMap.get(item);

			if (childRect.width < MIN_SIZE || childRect.height < MIN_SIZE) {
				item.clearBounds(true);
				continue;
			}

			Rectangle2D.Double childBounds = squarifiedMap.get(item);

			int x = (int) childBounds.x + bounds.x + HORIZONTAL_PADDING;
			int y = (int) childBounds.y + bounds.y + VERTICAL_PADDING;
			if (textBounds != null && textBounds.height > VERTICAL_PADDING) {
				y = y + textBounds.height - VERTICAL_PADDING;
			}
			int w = (int) childBounds.width;
			int h = (int) childBounds.height;

			item.paintItem(gc, new Rectangle(x, y, w, h), true);
		}
	}

	private void clearBounds(boolean all) {
		bounds = null;
		textBounds = null;

		if (!all) {
			return;
		}

		for (TreemapItem child : getItems()) {
			child.clearBounds(true);
		}
	}

	/**
	 * Clears the item at the given zero-relative index, sorted in descending order by weight, in
	 * the receiver. The text, weight and other attributes of the item are set to the default value.
	 * again as needed.
	 *
	 * @param index
	 *            the index of the item to clear
	 * @param all
	 *            true if all child items of the indexed item should be cleared recursively, and
	 *            false otherwise
	 */
	public void clear(int index, boolean all) {
		checkWidget();

		TreemapItem target = children.get(index);
		target.clearThis();

		if (all) {
			target.clearAll(true);
		}
	}

	/**
	 * Clears all the items in the receiver. The text, weight and other attributes of the items are
	 * set to their default values.
	 *
	 * @param all
	 *            true if all child items should be cleared recursively, and false otherwise
	 */
	public void clearAll(boolean all) {
		checkWidget();

		children.forEach(item -> {
			item.clearThis();

			if (all) {
				item.clearAll(true);
			}
		});
	}

	@Override
	public void dispose() {
		if (darkenBackground != null && !darkenBackground.isDisposed()) {
			darkenBackground.dispose();
		}

		super.dispose();
	}

	/**
	 * Returns the receiver's background color.
	 *
	 * @return the background color
	 */
	public Color getBackground() {
		checkWidget();

		if (background != null) {
			return background;
		}

		if (parentItem != null) {
			return parentItem.getBackground();
		}

		return parent.getBackground();
	}

	private Color getDarkenBackground() {
		if (darkenBackground == null || darkenBackground.isDisposed()) {
			Color bg = getBackground();
			int r = (int) (bg.getRed() * 0.8);
			int g = (int) (bg.getGreen() * 0.8);
			int b = (int) (bg.getBlue() * 0.8);

			darkenBackground = new Color(Display.getCurrent(), r, g, b);
		}
		return darkenBackground;
	}

	/**
	 * Sets the receiver's background color to the color specified by the argument, or to the
	 * default system color for the item if the argument is null.
	 *
	 * @param color
	 *            the new color (or null)
	 */
	public void setBackground(Color color) {
		checkWidget();

		background = color;

		if (darkenBackground != null && !darkenBackground.isDisposed()) {
			darkenBackground.dispose();
		}
		darkenBackground = null;
	}

	/**
	 * Returns a rectangle describing the size and location of the receiver relative to its parent.
	 *
	 * @return the bounding rectangle of the receiver's text
	 */
	public Rectangle getBounds() {
		checkWidget();

		return bounds;
	}

	/**
	 * Returns the font that the receiver will use to paint textual information for this item.
	 *
	 * @return the receiver's font
	 */
	public Font getFont() {
		checkWidget();

		if (font != null) {
			return font;
		}

		if (parentItem != null) {
			return parentItem.getFont();
		}

		return parent.getFont();
	}

	/**
	 * Sets the font that the receiver will use to paint textual information for this item to the
	 * font specified by the argument, or to the default font for that kind of control if the
	 * argument is null.
	 *
	 * @param font
	 *            the new font (or null)
	 */
	public void setFont(Font font) {
		checkWidget();

		this.font = font;
	}

	/**
	 * Returns the foreground color that the receiver will use to draw.
	 *
	 * @return the receiver's foreground color
	 */
	public Color getForeground() {
		checkWidget();

		if (foreground != null) {
			return foreground;
		}

		if (parentItem != null) {
			return parentItem.getForeground();
		}

		return parent.getForeground();
	}

	/**
	 * Sets the foreground color at the given column index in the receiver to the color specified by
	 * the argument, or to the default system color for the item if the argument is null.
	 *
	 * @param color
	 *            the new color (or null)
	 */
	public void setForeground(Color color) {
		checkWidget();

		this.foreground = color;
	}

	/**
	 * Returns the item at the given, zero-relative index, sorted in descending order by weight, in
	 * the receiver. Throws an exception if the index is out of range.
	 *
	 * @param index
	 *            the index of the item to return
	 * @return the item at the given index
	 */
	public TreemapItem getItem(int index) {
		checkWidget();

		return children.get(index);
	}

	/**
	 * Returns the item at the given point in the receiver or null if no such item exists. The point
	 * is in the coordinate system of the receiver.
	 *
	 * @param point
	 *            the point used to locate the item
	 * @return the item at the given point, or null if the point is not in a selectable item
	 */
	public TreemapItem getItem(Point point) {
		checkWidget();

		if (getBounds() == null || !getBounds().contains(point)) {
			return null;
		}

		for (TreemapItem child : children) {
			if (child.getBounds() != null && child.getBounds().contains(point)) {
				return child.getItem(point);
			}
		}

		return this;
	}

	/**
	 * Returns the number of items contained in the receiver that are direct item children of the
	 * receiver.
	 *
	 * @return the number of items
	 */
	public int getItemCount() {
		checkWidget();

		return children.size();
	}

	/**
	 * Returns a (possibly empty) array of TreeItems which are the direct item children of the
	 * receiver. Note: This is not the actual structure used by the receiver to maintain its list of
	 * items, so modifying the array will not affect the receiver.
	 *
	 * @return the receiver's items
	 */
	public TreemapItem[] getItems() {
		checkWidget();

		return children.toArray(new TreemapItem[0]);
	}

	/**
	 * Returns the receiver's parent, which must be a Treemap.
	 *
	 * @return the receiver's parent
	 */
	public Treemap getParent() {
		checkWidget();

		return parent;
	}

	/**
	 * Returns the receiver's parent item, which must be a TreeItem or null when the receiver is a
	 * root.
	 *
	 * @return the receiver's parent item
	 */
	public TreemapItem getParentItem() {
		checkWidget();

		return parentItem;
	}

	/**
	 * Returns a rectangle describing the size and location relative to its parent of the text.
	 *
	 * @return the receiver's bounding text rectangle
	 */
	public Rectangle getTextBounds() {
		checkWidget();

		return textBounds;
	}

	/**
	 * Returns the receiver's weight, which is the sum of weights of all its direct children.
	 *
	 * @return the receiver's weight
	 */
	public double getWeight() {
		checkWidget();

		if (apparentWeight == -1) {
			cacheApparentWeight();
		}

		return apparentWeight;
	}

	/**
	 * Returns the widget's tool tip text indicating more information about this item.
	 *
	 * @return the widget tool tip text
	 */
	public String getToolTipText() {
		checkWidget();

		return toolTipText;
	}

	/**
	 * Sets the widget's tool tip text indicating more information about this item.
	 *
	 * @param text
	 *            the new tool tip text
	 */
	public void setToolTipText(String text) {
		checkWidget();

		Objects.requireNonNull(text);
		this.toolTipText = text;
	}

	/**
	 * Sets the receiver's weight, which must be a non-negative number.
	 *
	 * @param weight
	 *            the new weight
	 */
	public void setWeight(double weight) {
		checkWidget();

		if (weight < 0) {
			throw new IllegalArgumentException("weight must be positive"); //$NON-NLS-1$
		}

		realWeight = weight;
		apparentWeight = -1;

		updateAncestor();
	}

	/**
	 * Searches the receiver's list starting at the first item (index 0) until an item is found that
	 * is equal to the argument, and returns the index of that item. If no item is found, returns
	 * -1.
	 *
	 * @param item
	 *            the search item
	 * @return the index of the item
	 */
	public int indexOf(TreemapItem item) {
		checkWidget();

		return children.indexOf(item);
	}

	/**
	 * Removes the item at the given, zero-relative index, sorted in descending order by weight, in
	 * the receiver. Throws an exception if the index is out of range.
	 *
	 * @param index
	 *            index of the item to remove
	 */
	public void remove(int index) {
		checkWidget();

		TreemapItem item = getItem(index);
		children.remove(item);
	}

	/**
	 * Removes all of the items from the receiver.
	 */
	public void removeAll() {
		checkWidget();

		for (TreemapItem child : children) {
			child.removeAll();
		}

		children.clear();
	}
}
