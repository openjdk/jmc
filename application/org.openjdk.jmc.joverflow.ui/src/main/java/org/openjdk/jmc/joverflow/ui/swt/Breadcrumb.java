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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TypedListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Instances of this class represent a selectable user interface object that displays the currently
 * location within programs, documents, or websites. The items of this receiver are kept in a stack
 * structure.
 * 
 * @see BreadcrumbItem
 */
public class Breadcrumb extends Canvas {
	private static final int TRIM = 2;

	private Stack<BreadcrumbItem> items = new Stack<>();

	private Map<SelectionListener, TypedListener> selectionListeners = new HashMap<>();

	// the following members need to be disposed
	private Cursor cursor;

	/**
	 * Constructs a new instance of this class given its parent and a style value describing its
	 * behavior and appearance.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new instance (cannot be null)
	 * @param style
	 *            the style of control to construct
	 */
	public Breadcrumb(Composite parent, int style) {
		super(checkNull(parent), style);

		addPaintListener(this::onPaintControl);
		addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent mouseEvent) {
				// noop
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

	void createItem(BreadcrumbItem item) {
		items.push(item);

		redraw();
	}

	private void onPaintControl(PaintEvent paintEvent) {
		Rectangle bounds = getClientArea();

		GC gc = paintEvent.gc;
		// clear background
		Color bg = gc.getBackground();
		gc.setBackground(getBackground());
		gc.fillRectangle(bounds);

		int dx = 0;
		for (BreadcrumbItem item : items) {
			item.paintItem(paintEvent.gc, new Rectangle(bounds.x + dx, bounds.y, bounds.width - dx, bounds.height));
			dx += item.getBounds().width;
		}

		gc.setBackground(bg);
	}

	private void onMouseDown(MouseEvent mouseEvent) {
		if (mouseEvent.button != 1) { // we care only about left button
			return;
		}

		BreadcrumbItem item = getItem(new Point(mouseEvent.x, mouseEvent.y));
		if (item == null) {
			return;
		}

		setSelection(item);
	}

	private void onMouseMove(MouseEvent mouseEvent) {
		BreadcrumbItem item = getItem(new Point(mouseEvent.x, mouseEvent.y));

		if (cursor != null && !cursor.isDisposed()) {
			cursor.dispose();
		}

		cursor = item == null ? new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW)
				: new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
		setCursor(cursor);
	}

	private Event createEventForItem(int type, BreadcrumbItem item) {
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
	public Point computeSize(int wHint, int hHint, boolean changed) {
		int width = 0;
		int height = 0;

		GC gc = new GC(this);
		for (BreadcrumbItem item : items) {
			Point dimension = item.getDimension(gc);

			width += dimension.x;
			height = Math.max(height, dimension.y);
		}
		return new Point(Math.max(width, wHint) + 2 * TRIM, Math.max(height, hHint) + 2 * TRIM);
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
		checkWidget();

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
		checkWidget();

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

	@Override
	public Rectangle getClientArea() {
		Rectangle bounds = super.getClientArea();
		bounds.x += TRIM;
		bounds.y += TRIM;
		bounds.width -= 2 * TRIM;
		bounds.height -= 2 * TRIM;

		return bounds;
	}

	/**
	 * Removes the last item from the receiver.
	 */
	public void popItem() {
		checkWidget();

		items.pop();

		redraw();
	}

	/**
	 * Return the last item from the receiver
	 * 
	 * @return the last item from the receiver
	 */
	public BreadcrumbItem peekItem() {
		checkWidget();

		return items.peek();
	}

	/**
	 * Returns the item at the given, zero-relative index in the receiver. Throws an exception if
	 * the index is out of range.
	 * 
	 * @param index
	 *            the index of the item to return
	 * @return the item at the given index
	 */
	public BreadcrumbItem getItem(int index) {
		checkWidget();

		return items.get(index);
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
	public BreadcrumbItem getItem(Point point) {
		checkWidget();

		for (BreadcrumbItem item : items) {
			if (item.getBounds() != null && item.getBounds().contains(point)) {
				return item;
			}
		}

		return null;
	}

	/**
	 * Returns the number of items contained in the receiver that are direct item children of the
	 * receiver.
	 *
	 * @return the number of items
	 */
	public int getItemCount() {
		checkWidget();

		return items.size();
	}

	/**
	 * Returns a (possibly empty) array of items contained in the receiver that are direct item
	 * children of the receiver. Note: This is not the actual structure used by the receiver to
	 * maintain its list of items, so modifying the array will not affect the receiver.
	 * 
	 * @return the items
	 */
	public BreadcrumbItem[] getItems() {
		checkWidget();

		return items.toArray(new BreadcrumbItem[0]);
	}

	/**
	 * An alias to #peekItem(). For breadcrumbs the selected item is always the last item.
	 *
	 * @return the item currently selected
	 */
	public BreadcrumbItem getSelection() {
		checkWidget();

		return peekItem();
	}

	/**
	 * Selects the item at the given zero-relative index in the receiver. If the item at the index
	 * was already selected, it remains selected. The current selection is first cleared, then the
	 * new item is selected. Indices that are out of range are ignored.
	 *
	 * @param index
	 *            the index of the item to select
	 */
	public void setSelection(int index) {
		checkWidget();

		removeFrom(index);

		Event e = createEventForItem(SWT.Selection, peekItem());
		notifyListeners(SWT.Selection, e);

		redraw();
	}

	/**
	 * Sets the receiver's selection to the given item. The current selection is cleared before the
	 * new item is selected. If the item is not in the receiver, then it is ignored.
	 *
	 * @param item
	 *            the item to select
	 */
	public void setSelection(BreadcrumbItem item) {
		checkWidget();

		if (item != null && item.getParent() != this) {
			return; // not in the receiver
		}

		setSelection(items.indexOf(item));
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
	public int indexOf(BreadcrumbItem item) {
		checkWidget();

		return items.indexOf(item);
	}

	/**
	 * Removes all items from the receiver with index equal or larger than this number. Indices that
	 * are out of range are ignored.
	 * 
	 * @param start
	 *            index of first element to be removed
	 */
	public void removeFrom(int start) {
		checkWidget();

		while (items.size() > start + 1) {
			items.pop();
		}
	}

	/**
	 * Removes all of the items from the receiver.
	 */
	public void removeAll() {
		checkWidget();

		items.clear();
	}
}
