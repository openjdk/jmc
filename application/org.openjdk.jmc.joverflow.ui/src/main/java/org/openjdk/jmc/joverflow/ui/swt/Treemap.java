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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TypedListener;
import org.openjdk.jmc.joverflow.ui.swt.events.TreemapEvent;
import org.openjdk.jmc.joverflow.ui.swt.events.TreemapListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Instances of this class represent a selectable user interface object that displays hierarchical
 * data using nested figures. A treemap node's size is in proportion to its size. This widget
 * implements the squarified treemap algorithm.
 */
public class Treemap extends Canvas {
	private static final int TRIM = 2;

	private TreemapItem rootItem = new TreemapItem(this, SWT.NONE);

	private Map<SelectionListener, TypedListener> selectionListeners = new HashMap<>();
	private Set<TreemapListener> treemapListeners = new HashSet<>();

	private TreemapItem topItem = rootItem;
	private TreemapItem selectedItem = null;

	private boolean borderVisible = true;
	private boolean toolTipEnabled = true;

	// the following members need to be disposed
	private Cursor cursor;
	private TreemapToolTip toolTip = new TreemapToolTip(this);

	/**
	 * Constructs a new instance of this class given its parent and a style value describing its
	 * behavior and appearance.
	 *
	 * @param parent
	 *            a composite control which will be the parent of the new instance (cannot be null)
	 * @param style
	 *            the style of control to construct
	 */
	public Treemap(Composite parent, int style) {
		super(checkNull(parent), style);

		if ((style & SWT.VIRTUAL) == SWT.VIRTUAL) {
			throw new UnsupportedOperationException("SWT.VIRTUAL is not support by Treemap"); //$NON-NLS-1$
		}

		addPaintListener(this::onPaintControl);
		addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent mouseEvent) {
				onMouseDoubleClick(mouseEvent);
			}

			@Override
			public void mouseDown(MouseEvent mouseEvent) {
				onMouseDown(mouseEvent);
			}

			@Override
			public void mouseUp(MouseEvent mouseEvent) {
				// noop
			}
		});
		addMouseMoveListener(this::onMouseMove);
	}

	static Composite checkNull(Composite control) {
		if (control == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		return control;
	}

	static Treemap checkNull(Treemap treemap) {
		if (treemap == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		return treemap;
	}

	private void onPaintControl(PaintEvent paintEvent) {
		getTopItem().paintItem(paintEvent.gc, getClientArea(), true);
	}

	private void onMouseDoubleClick(MouseEvent mouseEvent) {
		if (mouseEvent.button != 1) { // we care only about left button
			return;
		}

		TreemapItem item = getItem(new Point(mouseEvent.x, mouseEvent.y));
		if (item == null) {
			return;
		}

		setTopItem(item);
	}

	private void onMouseDown(MouseEvent mouseEvent) {
		// left button: select (highlight) a node
		if (mouseEvent.button == 1) {
			TreemapItem item = getItem(new Point(mouseEvent.x, mouseEvent.y));
			if (item == null) {
				return;
			}

			setSelection(item);
			return;
		}

		// middle button: show the root node as top
		if (mouseEvent.button == 2) {
			setTopItem(getRootItem());
			return;
		}

		// right button: show the parent node as top
		if (mouseEvent.button == 3) {
			TreemapItem parentItem = getTopItem().getParentItem();
			if (parentItem == null) {
				return;
			}
			setTopItem(parentItem);
			return;
		}
	}

	private void onMouseMove(MouseEvent mouseEvent) {
		TreemapItem item = getItem(new Point(mouseEvent.x, mouseEvent.y));
		if (item == null) {
			return;
		}

		if (cursor != null && !cursor.isDisposed()) {
			cursor.dispose();
		}

		cursor = item.getItemCount() == 0 ? new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW)
				: new Cursor(Display.getCurrent(), SWT.CURSOR_CROSS);
		setCursor(cursor);

		if (toolTipEnabled) {
			toolTip.setItem(item);
		}
	}

	private Event createEventForItem(int type, TreemapItem item) {
		Event e = new Event();
		e.display = getDisplay();
		e.widget = this;
		e.type = type;
		e.item = item;
		e.index = indexOf(item);

		if (item != null) {
			e.data = item.getData();
		}

		if (item != null && item.getBounds() != null) {
			Rectangle bounds = item.getBounds();
			e.x = bounds.x;
			e.y = bounds.y;
			e.width = bounds.width;
			e.height = bounds.height;
		}

		return e;
	}

	@Override
	public Rectangle getClientArea() {
		Rectangle bounds = super.getClientArea();
		bounds.x += TRIM;
		bounds.y += TRIM;
		bounds.width -= 2 * TRIM;
		bounds.height -= 2 * TRIM;

		return bounds;
	}

	@Override
	public Rectangle computeTrim(int x, int y, int width, int height) {
		return new Rectangle(x - TRIM, y - TRIM, width + 2 * TRIM, height + 2 * TRIM);
	}

	/**
	 * Adds the listener to the collection of listeners who will be notified when the user changes
	 * the receiver's selection, by sending it one of the messages defined in the SelectionListener
	 * interface.
	 *
	 * @param listener
	 *            the listener which should be notified when the user changes the receiver's
	 *            selection
	 */
	public void addSelectionListener(SelectionListener listener) {
		this.checkWidget();

		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		selectionListeners.putIfAbsent(listener, new TypedListener(listener));
		TypedListener typedListener = selectionListeners.get(listener);

		addListener(SWT.Selection, typedListener);
		addListener(SWT.DefaultSelection, typedListener);
	}

	/**
	 * Removes the listener from the collection of listeners who will be notified when the user
	 * changes the receiver's selection.
	 *
	 * @param listener
	 *            the listener which should no longer be notified
	 */
	public void removeSelectionListener(SelectionListener listener) {
		this.checkWidget();

		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		TypedListener typedListener = selectionListeners.remove(listener);
		if (typedListener == null) {
			return;
		}

		removeListener(SWT.Selection, typedListener);
		removeListener(SWT.DefaultSelection, typedListener);
	}

	/**
	 * Adds the listener to the collection of listeners who will be notified when an item in the
	 * receiver becomes the new top by sending it one of the messages defined in the interface.
	 *
	 * @param listener
	 *            the listener which should be notified
	 */
	public void addTreemapListener(TreemapListener listener) {
		this.checkWidget();

		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		treemapListeners.add(listener);
	}

	/**
	 * Removes the listener from the collection of listeners who will be notified when items in the
	 * receiver becomes the new top
	 *
	 * @param listener
	 *            the listener which should no longer be notified
	 */
	public void removeTreemapListener(TreemapListener listener) {
		this.checkWidget();

		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		treemapListeners.remove(listener);
	}

	/**
	 * Clears the item at the given zero-relative index, sorted in descending order by weight, in
	 * the receiver. The text, icon and other attributes of the item are set to the default value
	 *
	 * @param index
	 *            the index of the item to clear
	 * @param all
	 *            true if all child items of the indexed item should be cleared recursively, and
	 *            false otherwise
	 */
	public void clear(int index, boolean all) {
		checkWidget();

		rootItem.clear(index, all);
	}

	/**
	 * Clears all the items in the receiver. The text, icon and other attributes of the items are
	 * set to their default values.
	 *
	 * @param all
	 *            true if all child items should be cleared recursively, and false otherwise
	 */
	public void clearAll(boolean all) {
		checkWidget();

		rootItem.clearAll(all);
	}

	@Override
	public void dispose() {
		if (cursor != null && !cursor.isDisposed()) {
			cursor.dispose();
		}

		super.dispose();
	}

	/**
	 * Deselects an item in the receiver. If the item was already deselected, it remains deselected.
	 * Indices that are out of range are ignored.
	 *
	 * @param index
	 *            the index of the item to deselect
	 */
	public void deselect(int index) {
		checkWidget();

		try {
			getItem(index);
			deselect();
		} catch (IndexOutOfBoundsException e) {
			// noop
		}
	}

	/**
	 * Deselects the item in the receive that is currently selected. It is ignore if there is no
	 * selection.
	 */
	public void deselect() {
		checkWidget();

		if (getSelection() != null) {
			setSelection(null);
		}
	}

	/**
	 * Selects an item in the receiver. If the item was already selected, it remains selected.
	 * Indices that are out of range are ignored.
	 *
	 * @param index
	 *            the index of the item to select
	 */
	public void select(int index) {
		checkWidget();

		try {
			setSelection(getItem(index));
		} catch (IndexOutOfBoundsException e) {
			// noop
		}
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

		return rootItem.getItem(index);
	}

	/**
	 * Returns the item at the given point in the receiver or null if no such item exists. The point
	 * is in the coordinate system of the receiver. The item that is returned represents an item
	 * that could be selected by the user.
	 *
	 * @param point
	 *            the point used to locate the item
	 * @return the item at the given point, or null if the point is not in a selectable item
	 */
	public TreemapItem getItem(Point point) {
		checkWidget();

		return topItem.getItem(point);
	}

	/**
	 * Returns the number of items contained in the receiver that are direct item children of the
	 * receiver. The number that is returned is the number of roots in the tree.
	 *
	 * @return the number of items
	 */
	public int getItemCount() {
		checkWidget();

		return rootItem.getItemCount();
	}

	/**
	 * Returns a (possibly empty) array of items contained in the receiver that are direct item
	 * children of the receiver. These are the roots of the tree. Note: This is not the actual
	 * structure used by the receiver to maintain its list of items, so modifying the array will not
	 * affect the receiver.
	 *
	 * @return the items
	 */
	public TreemapItem[] getItems() {
		checkWidget();

		return rootItem.getItems();
	}

	/**
	 * Returns true if the receiver's borders are visible, and false otherwise. If one of the
	 * receiver's ancestors is not visible or some other condition makes the receiver not visible,
	 * this method may still indicate that it is considered visible even though it may not actually
	 * be showing.
	 *
	 * @return the visibility state of the borders
	 */
	public boolean getBordersVisible() {
		checkWidget();

		return borderVisible;
	}

	/**
	 * Marks the receiver's lines as visible if the argument is true, and marks it invisible
	 * otherwise. If one of the receiver's ancestors is not visible or some other condition makes
	 * the receiver not visible, marking it visible may not actually cause it to be displayed.
	 *
	 * @param show
	 *            the new visibility state
	 */
	public void setBordersVisible(boolean show) {
		checkWidget();

		borderVisible = show;
	}

	/**
	 * Returns true if the receiver's tooltip is enabled, and false otherwise.
	 * 
	 * @return true of the tooltip is enabled, and false otherwise
	 * @see TreemapItem#setToolTipText(String)
	 */
	public boolean getToolTipEnabled() {
		checkWidget();

		return toolTipEnabled;
	}

	/**
	 * Marks the receiver's tooltip as enabled if the argument is true.
	 * 
	 * @param enabled
	 *            true of the tooltip is enabled, and false otherwise
	 */
	public void setToolTipEnabled(boolean enabled) {
		checkWidget();

		toolTipEnabled = enabled;
		if (enabled) {
			toolTip.activate();
		} else {
			toolTip.deactivate();
		}
	}

	/**
	 * Returns the receiver's root item, which must be a TreeItem.
	 *
	 * @return the receiver's parent item
	 */
	public TreemapItem getRootItem() {
		checkWidget();

		return rootItem;
	}

	/**
	 * Returns the TreeItems that are currently selected in the receiver. A null value indicates
	 * that no item is selected.
	 *
	 * @return the item currently selected (or null)
	 */
	public TreemapItem getSelection() {
		checkWidget();

		return selectedItem;
	}

	/**
	 * Sets the receiver's selection to the given item. The current selection is cleared before the
	 * new item is selected. If the item is not in the receiver, then it is ignored.
	 *
	 * @param item
	 *            the item to select
	 */
	public void setSelection(TreemapItem item) {
		checkWidget();

		if (item != null && item.getParent() != this) {
			return; // not in the receiver
		}

		selectedItem = item;

		Event e = createEventForItem(SWT.Selection, item);
		notifyListeners(SWT.Selection, e);
		redraw();
	}

	/**
	 * Sets the receiver's text. This is equivalent to getting text on the root item.
	 *
	 * @return the new text
	 */
	public String getText() {
		checkWidget();

		return rootItem.getText();
	}

	/**
	 * Returns the receiver's text, which will be an empty string if it has never been set. This is
	 * equivalent to setting text on the root item.
	 *
	 * @param message
	 *            the receiver's text
	 */
	public void setText(String message) {
		checkWidget();

		rootItem.setText(message);
	}

	/**
	 * Returns the item which is currently at the top of the receiver. This item can change when
	 * items new item is added or set as the top.
	 *
	 * @return the item at the top of the receiver
	 */
	public TreemapItem getTopItem() {
		checkWidget();

		return topItem;
	}

	/**
	 * Sets the item which is currently at the top of the receiver. This item can change when items
	 * are expanded, collapsed, scrolled or new items are added or removed. If the item is a leaf
	 * (ie. no child), then the parent item is set as top if not null.
	 *
	 * @param item
	 *            the item to be displayed as top
	 */
	public void setTopItem(TreemapItem item) {
		checkWidget();

		item = TreemapItem.checkNull(item);

		if (item.getParent() != this) {
			throw new IllegalArgumentException("the given TreemapItem does not belong to the receiver"); //$NON-NLS-1$
		}

		// if item is a leaf, then show it's parent item.
		if (item.getItemCount() == 0 && item.getParentItem() != null) {
			item = item.getParentItem();
		}

		TreemapItem oldItem = topItem;
		topItem = item;

		if (oldItem == topItem) {
			return;
		}

		Event e = createEventForItem(SWT.NONE, topItem);
		for (TreemapListener listener : treemapListeners) {
			listener.treemapTopChanged(new TreemapEvent(e));
		}

		redraw();
	}

	/**
	 * Returns the widget's tool tip text indicating more information about this item.
	 *
	 * @return the widget message
	 */
	public String getToolTip() {
		checkWidget();

		return rootItem.getToolTipText();
	}

	/**
	 * Sets the widget's tool tip text indicating more information about this item.
	 *
	 * @param message
	 *            the new message
	 */
	public void setToolTip(String message) {
		checkWidget();

		rootItem.setToolTipText(message);
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

		return rootItem.indexOf(item);
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

		rootItem.remove(index);
	}

	/**
	 * Removes all of the items from the receiver.
	 */
	public void removeAll() {
		checkWidget();

		rootItem.removeAll();
	}

	/**
	 * Shows the item. If the item is already showing in the receiver, this method simply returns.
	 * Otherwise, the items are expanded until the item is visible.
	 *
	 * @param item
	 *            the item to be shown
	 */
	public void showItem(TreemapItem item) {
		checkWidget();

		item = TreemapItem.checkNull(item);

		if (item.getParent() != this) {
			throw new IllegalArgumentException("the given TreemapItem does not belong to the receiver"); //$NON-NLS-1$
		}

		TreemapItem top = item.getParentItem();
		if (top == null) {
			top = item;
		}

		setTopItem(top);

		if (item.getBounds() == null) {
			setTopItem(top);
		}
	}

	/**
	 * Shows the selected item. If the selection is already showing in the receiver, this method
	 * simply returns. Otherwise, the items are scrolled until the selection is visible.
	 */
	public void showSelection() {
		checkWidget();

		TreemapItem selection = getSelection();
		if (selection == null) {
			return;
		}

		showItem(selection);
	}
}
