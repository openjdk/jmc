package org.openjdk.jmc.joverflow.ext.treemap.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TreemapItem extends Item {
	/*package-private*/ private Treemap parent;
	/*package-private*/ private TreemapItem parentItem;
	/*package-private*/ private List<TreemapItem> children = new ArrayList<>();
	
	private int realWeight = 0; // the weight of a leaf node
	private int apparentWeight = 0; // the cached sum of all direct children's weights, or the realWeight if a leaf node
	private Color background = null;
	private Color foreground = null;
	private Font font = null;

	/**
	 * Constructs TreemapItem and inserts it into Treemap. Item is inserted as last direct child of the tree.
	 *
	 * @param parent a treemap control which will be the parent of the new instance (cannot be null)
	 * @param style  the style of control to construct
	 */
	public TreemapItem(Treemap parent, int style) {
		this(checkNull(parent), null, style);
	}

	/**
	 * Constructs TreeItem and inserts it into Tree. Item is inserted as last direct child of the specified TreeItem.
	 *
	 * @param parentItem a treemap control which will be the parent of the new instance (cannot be null)
	 * @param style      the style of control to construct
	 */
	public TreemapItem(TreemapItem parentItem, int style) {
		this(checkNull(parentItem).parent, parentItem, style);
	}

	private TreemapItem(Treemap parent, TreemapItem parentItem, int style) {
		super(parent, style);

		if ((style & SWT.VIRTUAL) == SWT.VIRTUAL) {
			// TODO: implement this if we want to support SWT.VIRTUAL
			throw new UnsupportedOperationException("SWT.VIRTUAL is not support by TreemapItem");
		}

		this.parent = parent;
		this.parentItem = parentItem;
		
		if (parentItem != null) {
			parentItem.children.add(this); // adding 0 weighted node to the end of decreasingly sorted list
		}
		
		parent.createItem(this);
	}

	private static Treemap checkNull(Treemap control) {
		if (control == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		return control;
	}

	private static TreemapItem checkNull(TreemapItem item) {
		if (item == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		return item;
	}

	private void sortChildren() {
		children.sort(Comparator.comparingInt(TreemapItem::getItemCount).reversed());
	}

	private void updateAncestor(int weightDelta) {
		// update apparentWeight for all ancestors
		for (TreemapItem ancestor = parentItem; ancestor !=null; ancestor = ancestor.parentItem) {
			ancestor.sortChildren();
			ancestor.apparentWeight += weightDelta;
		}
	}

	private void clearThis() {
		int delta = -this.apparentWeight;

		// TODO: clear more attributes
		this.realWeight = 0;
		this.apparentWeight = 0;
		this.setData(null);

		updateAncestor(delta);
	}

	/**
	 * Clears the item at the given zero-relative index, sorted in descending order by weight, in the receiver. The 
	 * text, weight and other attributes of the item are set to the default value.
	 * 
	 * TODO: If the tree was created with the SWT.VIRTUAL style, these attributes are requested again as needed.
	 *
	 * @param index the index of the item to clear
	 * @param all true if all child items of the indexed item should be cleared recursively, and false otherwise
	 */
	public void clear(int index, boolean all) {
		TreemapItem target = children.get(index);
		target.clearThis();

		if (all) {
			target.clearAll(true);
		}
	}

	/**
	 * Clears all the items in the receiver. The text, weight and other attributes of the items are set to their default 
	 * values. 
	 * 
	 * TODO: If the tree was created with the SWT.VIRTUAL style, these attributes are requested again as needed.
	 * 
	 * @param all true if all child items should be cleared recursively, and false otherwise
	 */
	public void clearAll(boolean all) {
		children.forEach(item -> {
			item.clearThis();
			
			if (all) {
				item.clearAll(true);
			}
		});
	}

	/**
	 * Returns the receiver's background color.
	 *
	 * @return the background color
	 */
	public Color getBackground() {
		if (background != null) {
			return background;
		}

		if (parentItem != null) {
			return parentItem.getBackground();
		}

		return parent.getBackground();
	}

	/**
	 * Sets the receiver's background color to the color specified by the argument, or to the default system color for
	 * the item if the argument is null.
	 *
	 * @param color the new color (or null)
	 */
	public void setBackground(Color color) {
		background = color;
	}

	/**
	 * Returns a rectangle describing the size and location of the receiver's text relative to its parent.
	 *
	 * @return the bounding rectangle of the receiver's text
	 */
	public Rectangle getBounds() {
		// TODO
		return null;
	}

	/**
	 * Returns the font that the receiver will use to paint textual information for this item.
	 *
	 * @return the receiver's font
	 */
	public Font getFont() {
		if (font != null) {
			return font;
		}
		
		if (parentItem != null) {
			return parentItem.getFont();
		}
		
		return parent.getFont();
	}

	/**
	 * Sets the font that the receiver will use to paint textual information for this item to the font specified by the
	 * argument, or to the default font for that kind of control if the argument is null.
	 *
	 * @param font the new font (or null)
	 */
	public void setFont(Font font) {
		 this.font = font;
	}

	/**
	 * Returns the foreground color that the receiver will use to draw.
	 *
	 * @return the receiver's foreground color
	 */
	public Color getForeground() {
		if (foreground != null) {
			return foreground;
		}

		if (parentItem != null) {
			return parentItem.getForeground();
		}

		return parent.getForeground();
	}

	/**
	 * Sets the foreground color at the given column index in the receiver to the color specified by the argument, or to
	 * the default system color for the item if the argument is null.
	 *
	 * @param color the new color (or null)
	 */
	public void setForeground(Color color) {
		this.foreground = color;
	}

	/**
	 * Returns the item at the given, zero-relative index, sorted in descending order by weight, in the receiver. Throws
	 * an exception if the index is out of range.
	 *
	 * @param index the index of the item to return
	 * @return the item at the given index
	 */
	public TreemapItem getItem(int index) {
		return children.get(index);
	}

	/**
	 * Returns the number of items contained in the receiver that are direct item children of the receiver.
	 *
	 * @return the number of items
	 */
	public int getItemCount() {
		return children.size();
	}

	/**
	 * Sets the number of child items contained in the receiver.
	 *
	 * @param count the number of items
	 */
	public void setItemCount(int count) {
		// TODO: implement this if we want to support SWT.VIRTUAL
		throw new UnsupportedOperationException("SWT.VIRTUAL is not support by TreemapItem");
	}

	/**
	 * Returns a (possibly empty) array of TreeItems which are the direct item children of the receiver.
	 * Note: This is not the actual structure used by the receiver to maintain its list of items, so modifying the array
	 * will not affect the receiver.
	 *
	 * @return the receiver's items
	 */
	public TreemapItem[] getItems() {
		return children.toArray(new TreemapItem[0]);
	}

	/**
	 * Returns the receiver's parent, which must be a Treemap.
	 *
	 * @return the receiver's parent
	 */
	public Treemap getParent() {
		return parent;
	}

	/**
	 * Returns the receiver's parent item, which must be a TreeItem or null when the receiver is a root.
	 *
	 * @return the receiver's parent item
	 */
	public TreemapItem getParentItem() {
		return parentItem;
	}

	/**
	 * Returns a rectangle describing the size and location relative to its parent of the text at a column in the table.
	 *
	 * @return the receiver's bounding text rectangle
	 */
	public Rectangle getTextBounds() {
		// TODO
		return null;
	}

	/**
	 * Returns the receiver's weight, which is the sum of weights of all its direct children. 
	 * 
	 * @return the receiver's weight
	 */
	public int getWeight() {
		if (apparentWeight != 0) {
			return apparentWeight; // for an internal node (with a child)
		}

		return realWeight; // for an external node (ie. leaf)
	}

	/**
	 * Sets the receiver's weight. Throws an exception if the receiver is not a leaf node..
	 * 
	 * @param weight the new weight
	 */
	public void setWeight(int weight) {
		if (!children.isEmpty()) {
			throw new IllegalStateException("Cannot set weight of a non-leaf node");
		}
		
		if (weight < 0) {
			throw new IllegalArgumentException("weight must be positive");
		}

		int delta = weight - realWeight;
		realWeight = weight;

		updateAncestor(delta);
	}
	
	/**
	 * Searches the receiver's list starting at the first item (index 0) until an item is found that is equal to the
	 * argument, and returns the index of that item. If no item is found, returns -1.
	 *
	 * @param item the search item
	 * @return the index of the item
	 */
	public int indexOf(TreemapItem item) {
		return 0;
	}

	/**
	 * 
	 * 
	 * @param index index of the item to remove
	 */
	public void remove(int index) {
		
	}

	/**
	 * Removes all of the items from the receiver.
	 */
	public void removeAll() {
	}
}
