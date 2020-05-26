package org.openjdk.jmc.joverflow.ext.treemap.swt;

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

public class Breadcrumb extends Canvas {
	private static final int TRIM = 2;

	private Stack<BreadcrumbItem> items = new Stack<>();

	private Map<SelectionListener, TypedListener> selectionListeners = new HashMap<>();

	// the following members need to be disposed
	private Cursor cursor;

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

	/*package-private*/
	static Composite checkNull(Composite control) {
		if (control == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		return control;
	}

	/*package-private*/ void createItem(BreadcrumbItem item) {
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

		cursor = item == null ? new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW) :
				new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
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

	@Override
	public Rectangle getClientArea() {
		Rectangle bounds = super.getClientArea();
		bounds.x += TRIM;
		bounds.y += TRIM;
		bounds.width -= 2 * TRIM;
		bounds.height -= 2 * TRIM;

		return bounds;
	}

	public void popItem() {
		this.checkWidget();

		items.pop();

		redraw();
	}

	public BreadcrumbItem peekItem() {
		this.checkWidget();

		return items.peek();
	}

	public BreadcrumbItem getItem(int index) {
		this.checkWidget();

		return items.get(index);
	}

	public BreadcrumbItem getItem(Point point) {
		checkWidget();

		for (BreadcrumbItem item : items) {
			if (item.getBounds() != null && item.getBounds().contains(point)) {
				return item;
			}
		}

		return null;
	}

	public int getItemCount() {
		checkWidget();

		return items.size();
	}

	public BreadcrumbItem[] getItems() {
		checkWidget();

		return items.toArray(new BreadcrumbItem[0]);
	}

	/**
	 * Alias to #peekItem()
	 *
	 * @return the item currently selected
	 */
	public BreadcrumbItem getSelection() {
		checkWidget();

		return peekItem();
	}

	public void setSelection(int index) {
		checkWidget();

		removeFrom(index);

		Event e = createEventForItem(SWT.Selection, peekItem());
		notifyListeners(SWT.Selection, e);

		redraw();
	}

	public void setSelection(BreadcrumbItem item) {
		if (item != null && item.getParent() != this) {
			throw new IllegalArgumentException("the given TreemapItem does not belong to the receiver");
		}

		setSelection(items.indexOf(item));
	}

	public int indexOf(BreadcrumbItem item) {
		return items.indexOf(item);
	}

	public void removeFrom(int start) {
		while (items.size() > start + 1) {
			items.pop();
		}
	}

	public void removeAll() {
		items.clear();
	}
}
