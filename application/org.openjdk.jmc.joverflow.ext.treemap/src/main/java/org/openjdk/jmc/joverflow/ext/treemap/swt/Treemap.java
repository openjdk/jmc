package org.openjdk.jmc.joverflow.ext.treemap.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class Treemap extends Canvas implements PaintListener {

	private boolean borderVisible = true;

	private TreemapItem rootItem = new TreemapItem(this, SWT.NONE);

	// All created items of this receiver. Should recycle if possible.
//	private List<TreemapItem> items = new ArrayList<>();

	// TODO: checkWidget() when appropriate

	/**
	 * Constructs a new instance of this class given its parent and a style value describing its behavior and
	 * appearance.
	 * The style value is either one of the style constants defined in class SWT which is applicable to instances of
	 * this class, or must be built by bitwise OR'ing together (that is, using the int "|" operator) two or more of
	 * those SWT style constants. The class description lists the style constants that are applicable to the class.
	 * Style bits are also inherited from superclasses.
	 *
	 * @param parent a composite control which will be the parent of the new instance (cannot be null)
	 * @param style  the style of control to construct
	 */
	public Treemap(Composite parent, int style) {
		super(checkNull(parent), style);

		if ((style & SWT.VIRTUAL) == SWT.VIRTUAL) {
			throw new UnsupportedOperationException("SWT.VIRTUAL is not support by Treemap");
		}

		addPaintListener(this);
	}

	/*package-private*/ static Composite checkNull(Composite control) {
		if (control == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		return control;
	}

	/*package-private*/ static Treemap checkNull(Treemap treemap) {
		if (treemap == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		return treemap;
	}

	@Override
	public void paintControl(PaintEvent paintEvent) {
		System.out.println(getClientArea().toString());

		getTopItem().paintItem(paintEvent.gc, getClientArea());
	}

//	/*package-private*/ void createItem(TreemapItem item) {
//		items.add(item);
//	}

// TODO: call notifyListeners​(int eventType, Event event) instead
//	/**
//	 * Adds the listener to the collection of listeners who will be notified when an event of the given type occurs.
//	 * When the event does occur in the widget, the listener is notified by sending it the handleEvent() message. The
//	 * event type is one of the event constants defined in class SWT.
//	 *
//	 * @param eventType the type of event to listen for
//	 * @param listener  the listener which should be notified when the event occurs
//	 */
//	@Override
//	public void addListener(int eventType, Listener listener) {
//		super.addListener(eventType, listener);
//
//		// TODO: implement this if we want to support SWT.VIRTUAL
//	}
//
//	/**
//	 * Removes the listener from the collection of listeners who will be notified when an event of the given type 
//	 * occurs. The event type is one of the event constants defined in class SWT.
//	 * 
//	 * @param eventType the type of event to listen for
//	 * @param listener the listener which should no longer be notified
//	 */
//	@Override
//	public void removeListener(int eventType, Listener listener) {
//
//	}

	/**
	 * Adds the listener to the collection of listeners who will be notified when the user changes the receiver's
	 * selection, by sending it one of the messages defined in the SelectionListener interface.
	 * When widgetSelected is called, the item field of the event object is valid. If the receiver has the SWT.CHECK
	 * style and the check selection changes, the event object detail field contains the value SWT.CHECK.
	 * widgetDefaultSelected is typically called when an item is double-clicked. The item field of the event object is
	 * valid for default selection, but the detail field is not used.
	 *
	 * @param listener the listener which should be notified when the user changes the receiver's selection
	 */
	public void addSelectionListener(SelectionListener listener) {

	}

	/**
	 * Removes the listener from the collection of listeners who will be notified when the user changes the receiver's
	 * selection.
	 *
	 * @param listener the listener which should no longer be notified
	 */
	public void removeSelectionListener(SelectionListener listener) {

	}

	/**
	 * Adds the listener to the collection of listeners who will be notified when an item in the receiver is expanded or
	 * collapsed by sending it one of the messages defined in the TreeListener interface.
	 *
	 * @param listener the listener which should be notified
	 */
	public void addTreemapListener(TreemapListener listener) {

	}

	/**
	 * Removes the listener from the collection of listeners who will be notified when items in the receiver are
	 * expanded or collapsed.
	 *
	 * @param listener the listener which should no longer be notified
	 */
	public void removeTreemapListener(TreemapListener listener) {

	}

	/**
	 * Clears the item at the given zero-relative index, sorted in descending order by weight, in the receiver. The
	 * text, icon and other attributes of the item are set to the default value.
	 * 
	 * TODO: If the tree was created with the SWT.VIRTUAL style, these attributes are requested again as needed.
	 *
	 * @param index the index of the item to clear
	 * @param all   true if all child items of the indexed item should be cleared recursively, and false otherwise
	 */
	public void clear(int index, boolean all) {
		rootItem.clear(index, all);
	}

	/**
	 * Clears all the items in the receiver. The text, icon and other attributes of the items are set to their default
	 * values. If the tree was created with the SWT.VIRTUAL style, these attributes are requested again as needed.
	 *
	 * @param all true if all child items should be cleared recursively, and false otherwise
	 */
	public void clearAll(boolean all) {
		rootItem.clearAll(all);
	}

	/**
	 * Deselects an item in the receiver. If the item was already deselected, it remains deselected.
	 *
	 * @param item the item to be deselected
	 */
	public void deselect(TreemapItem item) {
		// TODO
	}

	/**
	 * Selects an item in the receiver. If the item was already selected, it remains selected.
	 *
	 * @param item the item to be selected
	 */
	public void select(TreemapItem item) {
		// TODO
	}

	/**
	 * Selects all of the items in the receiver.
	 */
	public void selectAll() {
		// TODO
	}

	/**
	 * Deselects all selected items in the receiver.
	 */
	public void deselectAll() {
		// TODO
	}

	/**
	 * Returns the item at the given, zero-relative index, sorted in descending order by weight, in the receiver. Throws
	 * an exception if the index is out of range.
	 *
	 * @param index the index of the item to return
	 * @return the item at the given index
	 */
	public TreemapItem getItem(int index) {
		return rootItem.getItem(index);
	}

	/**
	 * Returns the item at the given point in the receiver or null if no such item exists. The point is in the
	 * coordinate system of the receiver.
	 * The item that is returned represents an item that could be selected by the user. For example, if selection only
	 * occurs in items in the first column, then null is returned if the point is outside of the item. Note that the
	 * SWT.FULL_SELECTION style hint, which specifies the selection policy, determines the extent of the selection.
	 *
	 * @param point the point used to locate the item
	 * @return the item at the given point, or null if the point is not in a selectable item
	 */
	public TreemapItem getItem(Point point) {
		return rootItem.getItem(point);
	}

	/**
	 * Returns the number of items contained in the receiver that are direct item children of the receiver. The number
	 * that is returned is the number of roots in the tree.
	 *
	 * @return the number of items
	 */
	public int getItemCount() {
		return rootItem.getItemCount();
	}

	/**
	 * Sets the number of root-level items contained in the receiver.
	 *
	 * @param count the number of items
	 */
	public void setItemCount(int count) {
		// TODO: implement this if we want to support SWT.VIRTUAL
		throw new UnsupportedOperationException("SWT.VIRTUAL is not support by TreemapItem");
	}

	/**
	 * Returns a (possibly empty) array of items contained in the receiver that are direct item children of the
	 * receiver. These are the roots of the tree.
	 * Note: This is not the actual structure used by the receiver to maintain its list of items, so modifying the array
	 * will not affect the receiver.
	 *
	 * @return the items
	 */
	public TreemapItem[] getItems() {
		return rootItem.getItems();
	}

	/**
	 * Returns true if the receiver's borders are visible, and false otherwise.
	 * If one of the receiver's ancestors is not visible or some other condition makes the receiver not visible, this
	 * method may still indicate that it is considered visible even though it may not actually be showing.
	 *
	 * @return the visibility state of the borders
	 */
	public boolean getBordersVisible() {
		return borderVisible;
	}

	/**
	 * Marks the receiver's lines as visible if the argument is true, and marks it invisible otherwise.
	 * If one of the receiver's ancestors is not visible or some other condition makes the receiver not visible, marking
	 * it visible may not actually cause it to be displayed.
	 *
	 * @param show the new visibility state
	 */
	public void setBordersVisible(boolean show) {
		borderVisible = show;
	}

	/**
	 * Returns the receiver's root item, which must be a TreeItem.
	 *
	 * @return the receiver's parent item
	 */
	public TreemapItem getRootItem() {
		return rootItem;
	}

	/**
	 * Returns an array of TreeItems that are currently selected in the receiver. The order of the items is unspecified.
	 * An empty array indicates that no items are selected.
	 * 
	 * Note: This is not the actual structure used by the receiver to maintain its selection, so modifying the array
	 * will not affect the receiver.
	 *
	 * 
	 * @return an array representing the selection
	 */
	public TreemapItem[] getSelection() {
		return null;
	}

	/**
	 * Sets the receiver's selection to the given item. The current selection is cleared before the new item is
	 * selected, and if necessary the receiver is scrolled to make the new selection visible.
	 * If the item is not in the receiver, then it is ignored.
	 *
	 * @param item the item to select
	 */
	public void setSelection(TreemapItem item) {
		// TODO
	}

	public void setSelection(TreemapItem[] items) {
		// TODO
	}

	/**
	 * Returns the number of selected items contained in the receiver.
	 *
	 * @return the number of selected items
	 */
	public int getSelectionCount() {
		return 0;
	}

	/**
	 * Returns the item which is currently at the top of the receiver. This item can change when items are expanded, 
	 * collapsed, scrolled or new items are added or removed.
	 * 
	 * @return the item at the top of the receiver
	 */
	public TreemapItem getTopItem() {
		// TODO: track which item is on top
		return getRootItem();
	}

	/**
	 * Sets the item which is currently at the top of the receiver. This item can change when items are expanded, 
	 * collapsed, scrolled or new items are added or removed.
	 * 
	 * @param item
	 */
	public void setTopItem(TreemapItem item) {
		// TODO
	}
	
	/**
	 * Searches the receiver's list starting at the first item (index 0) until an item is found that is equal to the
	 * argument, and returns the index of that item. If no item is found, returns -1.
	 *
	 * @param item the search item
	 * @return the index of the item
	 */
	public int indexOf(TreemapItem item) {
		return rootItem.indexOf(item);
	}


	/**
	 * Removes the item at the given, zero-relative index, sorted in descending order by weight, in the receiver. Throws
	 * an exception if the index is out of range. 
	 *
	 * @param index index of the item to remove
	 */
	public void remove(int index) {
		rootItem.remove(index);
	}

	/**
	 * Searches the receiver's list starting at the first item (index 0) until an item is found that is equal to the
	 * argument, and remove that item.
	 *
	 * @param item the item to be removed
	 */
	public void remove(TreemapItem item) {
		item = TreemapItem.checkNull(item);

		if (item.getParent() != this) {
			throw new IllegalArgumentException("the given TreemapItem does not belong to the receiver");
		}

		item.updateAncestor();

		for (TreemapItem child : item.getItems()) {
			remove(child);
		}
	}

	/**
	 * Removes all of the items from the receiver.
	 */
	public void removeAll() {
		rootItem.removeAll();
	}

	/**
	 * Shows the item. If the item is already showing in the receiver, this method simply returns. Otherwise, the items
	 * are expanded until the item is visible.
	 *
	 * @param item the item to be shown
	 */
	public void showItem(TreemapItem item) {
		// TODO
	}

	/**
	 * Shows the first selection. If the selection is already showing in the receiver, this method simply returns. 
	 * Otherwise, the items are scrolled until the selection is visible.
	 */
	public void showSelection() {
		TreemapItem[] selection = getSelection();

		// TODO: find top most selection
		showItem(selection[0]);
	}
}














