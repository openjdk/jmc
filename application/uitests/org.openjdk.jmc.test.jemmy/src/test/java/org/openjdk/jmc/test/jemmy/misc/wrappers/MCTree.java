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
package org.openjdk.jmc.test.jemmy.misc.wrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.jemmy.Point;
import org.jemmy.control.Wrap;
import org.jemmy.input.StringPopupOwner;
import org.jemmy.input.StringPopupSelectableOwner;
import org.jemmy.input.StringTree;
import org.jemmy.interfaces.Keyboard.KeyboardButtons;
import org.jemmy.interfaces.Parent;
import org.jemmy.interfaces.Selectable;
import org.jemmy.resources.StringComparePolicy;
import org.jemmy.swt.ItemWrap;
import org.jemmy.swt.TreeWrap;
import org.jemmy.swt.lookup.ByItemLookup;
import org.jemmy.swt.lookup.ByName;
import org.junit.Assert;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.fetchers.Fetcher;

/**
 * The Jemmy wrapper for Trees
 */
public class MCTree extends MCJemmyBase {
	private StringComparePolicy policy;
	private StringComparePolicy savedPolicy;
	private static final Integer MAXIMUM_NUMBER_OF_NAVIGATIONAL_ATTEMPTS = 10;

	/**
	 * Returns all currently visible {@link MCTree}
	 *
	 * @param shell
	 *            the shell from where to start searching for the widgets
	 * @param waitForIdle
	 *            {@code true} if supposed to wait for the UI to be idle before performing the
	 *            lookup
	 * @return a {@link List} of {@link MCTree} that were found
	 */
	@SuppressWarnings("unchecked")
	public static List<MCTree> getAll(Wrap<? extends Shell> shell, boolean waitForIdle) {
		List<Wrap<? extends Tree>> list = getVisible(shell.as(Parent.class, Tree.class).lookup(Tree.class), waitForIdle,
				false);
		return list.stream().map(i -> new MCTree(i)).collect(Collectors.toList());
	}

	/**
	 * Returns all currently visible {@link MCTree}
	 *
	 * @param shell
	 *            the shell from where to start searching for the widgets
	 * @return a {@link List} of {@link MCTree} that were found
	 */
	public static List<MCTree> getAll(Wrap<? extends Shell> shell) {
		return getAll(shell, true);
	}

	/**
	 * Returns all currently visible {@link MCTree} (in the main Mission Control Window)
	 * 
	 * @return a {@link List} of {@link MCTree} that were found
	 */
	public static List<MCTree> getAll() {
		return getAll(getShell());
	}

	/**
	 * Finds a Tree (in the main Mission Control Window) by name and returns it
	 *
	 * @param name
	 *            the name of the widget
	 * @return a {@link MCTree}
	 */
	public static MCTree getByName(String name) {
		return getByName(getShell(), name);
	}

	/**
	 * Finds the first visible Tree (in the main Mission Control Window) by name
	 *
	 * @param name
	 *            the name of the widget
	 * @return a {@link MCTree}
	 */
	public static MCTree getFirstVisibleByName(String name) {
		return getFirstVisibleByName(getShell(), name);
	}

	/**
	 * Finds the first visible Tree (in the main Mission Control Window)
	 * 
	 * @return a {@link MCTree}
	 */
	public static MCTree getFirst() {
		return getFirst(getShell());
	}

	/**
	 * Finds the tree that contains a matching item
	 * 
	 * @param item
	 *            the desired item text
	 * @return a {@link MCTree}
	 */
	@SuppressWarnings("unchecked")
	public static MCTree getByItem(String item) {
		return new MCTree(
				getShell().as(Parent.class, Tree.class).lookup(Tree.class, new ByItemLookup<Tree>(item)).wrap());
	}

	/**
	 * Finds a tree by name
	 *
	 * @param shell
	 *            the shell from where to start searching for the widget
	 * @param name
	 *            the name of the widget
	 * @return a {@link MCTree}
	 */
	@SuppressWarnings("unchecked")
	static MCTree getByName(Wrap<? extends Shell> shell, String name) {
		return new MCTree(shell.as(Parent.class, Tree.class).lookup(Tree.class, new ByName<Tree>(name)).wrap());
	}

	/**
	 * Finds the first visible tree by name
	 *
	 * @param shell
	 *            the shell from where to start searching for the widget
	 * @param name
	 *            the name of the widget
	 * @return a {@link MCTree}
	 */
	static MCTree getFirstVisibleByName(Wrap<? extends Shell> shell, String name) {
		return getFirstVisibleByName(shell, name, true);
	}

	/**
	 * Finds the first visible tree by name
	 *
	 * @param shell
	 *            the shell from where to start searching for the widget
	 * @param name
	 *            the name of the widget
	 * @param waitForIdleUi
	 *            {@code true} if supposed to wait for the UI to be idle before performing the
	 *            lookup
	 * @return a {@link MCTree}
	 */
	@SuppressWarnings("unchecked")
	static MCTree getFirstVisibleByName(Wrap<? extends Shell> shell, String name, boolean waitForIdle) {
		return new MCTree((Wrap<? extends Tree>) getVisible(
				shell.as(Parent.class, Tree.class).lookup(Tree.class, new ByName<Tree>(name)), waitForIdle).get(0));
	}

	/**
	 * Finds the first tree in the SWT hierarchy for the given shell
	 *
	 * @param shell
	 *            the shell from where to start searching for the widget
	 * @return a {@link MCTree}
	 */
	@SuppressWarnings("unchecked")
	static MCTree getFirst(Wrap<? extends Shell> shell) {
		return new MCTree(shell.as(Parent.class, Tree.class).lookup(Tree.class).wrap());
	}

	/**
	 * Finds the first visible tree in the SWT hierarchy for the given shell
	 * 
	 * @param shell
	 *            the shell from where to start searching for the widget
	 * @param waitForIdleUi
	 *            {@code true} if supposed to wait for the UI to be idle before performing the
	 *            lookup
	 * @return a {@link MCTree}
	 */
	@SuppressWarnings("unchecked")
	static MCTree getFirstVisible(Wrap<? extends Shell> shell, boolean waitForIdleUi) {
		return new MCTree(
				(Wrap<? extends Tree>) getVisible(shell.as(Parent.class, Tree.class).lookup(Tree.class), waitForIdleUi)
						.get(0));
	}

	/**
	 * Finds the first visible tree in the SWT hierarchy for the given shell
	 *
	 * @param shell
	 *            the shell from where to start searching for the widget
	 * @return a {@link MCTree}
	 */
	static MCTree getFirstVisible(Wrap<? extends Shell> shell) {
		return getFirstVisible(shell, true);
	}

	/**
	 * Finds the first visible tree in the SWT hierarchy for the given {@link MCDialog}
	 *
	 * @param dialog
	 *            the {@link MCDialog} from where to start searching for the widget
	 * @return a {@link MCTree}
	 */
	public static MCTree getFirstVisible(MCDialog dialog) {
		return getFirstVisible(dialog.getDialogShell());
	}

	/**
	 * Finds the first tree in the SWT hierarchy for the given {@link MCDialog}
	 *
	 * @param dialog
	 *            the {@link MCDialog} from where to start searching for the widget
	 * @return a {@link MCTree}
	 */
	public static MCTree getFirst(MCDialog dialog) {
		return getFirst(dialog.getDialogShell());
	}

	/**
	 * Backwards compatibility. Currently only to be used old style Jemmy tests where more
	 * specialized lookups are used to find the tree
	 *
	 * @param treeWrap
	 *            the wrap of the tree
	 */
	public MCTree(Wrap<? extends Tree> treeWrap) {
		this.control = treeWrap;
		substringMatching();
	}

	/**
	 * Selects the tree item supplied
	 *
	 * @param path
	 *            the complete path, from the root, of the tree item to select
	 */
	@SuppressWarnings("unchecked")
	public void select(String ... path) {
		ensureFocus();
		StringTree<TreeItem> st = control.as(StringTree.class, TreeItem.class);
		st.setPolicy(policy);
		st.select(path);
	}

	/**
	 * Selects the item at the given index (if not -1))
	 *
	 * @param index
	 *            the index of the item
	 * @return {@code true} if selected index is the same as the provided. {@code false} otherwise
	 */
	public boolean selectRow(int index) {
		if (index != -1) {
			ensureFocus();
			int startIndex = getIndexOfSelectedItem();
			if (startIndex == -1) {
				control.keyboard().pushKey(KeyboardButtons.DOWN);
				control.keyboard().pushKey(KeyboardButtons.UP);
				startIndex = control.getProperty(Integer.class, Selectable.STATE_PROP_NAME);
			}
			if (startIndex != -1) {
				int steps = index - startIndex;
				KeyboardButtons stepButton = (index > startIndex) ? KeyboardButtons.DOWN : KeyboardButtons.UP;
				for (int i = 0; i < Math.abs(steps); i++) {
					control.keyboard().pushKey(stepButton);
				}
			}
			return (getIndexOfSelectedItem() == index && index != -1);
		} else {
			return false;
		}
	}

	/**
	 * Returns the index of the first item currently selected in the tree. Note that this only
	 * returns the index of the first selected item if more than one is selected
	 *
	 * @return the index of the first item selected in the tree. -1 if no item is selected.
	 */
	public int getIndexOfSelectedItem() {
		TreeItem item = control.as(TreeWrap.class).getSelectedItem();
		Integer index = control.as(TreeWrap.class).index(item);
		return index;
	}

	/**
	 * Gets the number of items in the tree
	 *
	 * @return the number of items in the tree
	 */
	public int getItemCount() {
		final Tree tree = getWrap().getControl();
		Fetcher<Integer> fetcher = new Fetcher<Integer>() {
			@Override
			public void run() {
				int count = tree.getItemCount();
				setOutput(count);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput().intValue();
	}

	/**
	 * Selects the tree item supplied
	 *
	 * @param exactMatching
	 *            if {@code true} does an exact matching of each path element. Otherwise substring
	 *            matching is used
	 * @param path
	 *            the complete path, from the root, of the tree item to select
	 */
	public void select(boolean exactMatching, String ... path) {
		setMatching(exactMatching);
		try {
			select(path);
		} finally {
			resetMatching();
		}
	}

	/**
	 * Selects the tree item supplied and clicks {@code times} with the mouse on the item
	 *
	 * @param times
	 *            the number of times to click on the item (rapidly if more than once)
	 * @param path
	 *            the path to the tree item
	 */
	public void selectAndClick(int times, String ... path) {
		select(path);
		scrollbarSafeSelection();
		Wrap<TreeItem> itemWrap = new ItemWrap<>(control, control.as(TreeWrap.class).getSelectedItem());
		itemWrap.mouse().click(times);
	}

	/**
	 * Returns a list of the currently selected tree item's text values
	 * 
	 * @return a {@link List} of {@link String}
	 */
	public List<String> getSelectedItemTexts() {
		Fetcher<List<String>> fetcher = new Fetcher<List<String>>() {
			@Override
			public void run() {
				List<String> texts = new ArrayList<>();
				int columnCount = getColumnCount();
				TreeItem selectedItem = control.as(TreeWrap.class).getSelectedItem();
				if (columnCount > 0) {
					for (int i = 0; i < columnCount; i++) {
						texts.add(selectedItem.getText(i));
					}
				} else {
					texts.add(selectedItem.getText());
				}
				setOutput(texts);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Returns a list of the currently selected tree item's fonts, ordered
	 * by column
	 *
	 * @return a {@link List} of {@link Font}
	 */
	public List<Font> getSelectedItemFonts() {
		Fetcher<List<Font>> fetcher = new Fetcher<List<Font>>() {
			@Override
			public void run() {
				List<Font> fonts = new ArrayList<>();
				int columnCount = getColumnCount();
				TreeItem selectedItem = control.as(TreeWrap.class).getSelectedItem();
				if (columnCount > 0) {
					for (int i = 0; i < columnCount; i++) {
						fonts.add(selectedItem.getFont(i));
					}
				} else {
					fonts.add(selectedItem.getFont());
				}
				setOutput(fonts);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Get the currently selected item's direct child item texts
	 * 
	 * @return a {@link List} of {@link String}
	 */
	public List<String> getSelectedItemChildrenTexts() {
		Fetcher<List<String>> fetcher = new Fetcher<List<String>>() {
			@Override
			public void run() {
				List<String> texts = new ArrayList<>();
				TreeItem selectedItem = control.as(TreeWrap.class).getSelectedItem();
				for (TreeItem child : selectedItem.getItems()) {
					texts.add(child.getText());
				}
				setOutput(texts);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Returns a list of string lists containing the tree's "complete" tree item text values. Note:
	 * Do not expect to find all the data backed by the model as this seems to be (lazily) loaded
	 * into the tree (upon expansion of parent tree items). Expect to call this repeatedly when
	 * navigating and expanding/collapsing items to get a current picture of the tree contents.
	 *
	 * @return a {@link List} of {@link List} of {@link String}
	 */
	public List<List<String>> getAllItemTexts() {
		Fetcher<List<List<String>>> fetcher = new Fetcher<List<List<String>>>() {
			@Override
			public void run() {
				List<List<String>> output = new ArrayList<>();
				Tree tree = ((Tree) control.as(TreeWrap.class).getControl());
				TreeItem[] items = tree.getItems();
				int columnCount = tree.getColumnCount();
				addSubordinateItemTexts(output, new ArrayList<String>(), items, columnCount);
				setOutput(output);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Returns a list of strings for the tree item of the specified index.
	 *
	 * @param index
	 *            the index of the item to get the text for
	 * @return a {@link List} of {@link String}
	 */
	public List<String> getItemTexts(int index) {
		Fetcher<List<String>> fetcher = new Fetcher<List<String>>() {
			@Override
			public void run() {
				List<String> output = new ArrayList<>();
				Tree tree = ((Tree) control.as(TreeWrap.class).getControl());
				TreeItem[] items = tree.getItems();
				if (index >= items.length) {
					setOutput(null);
				} else {
					TreeItem item = items[index];
					items = new TreeItem[1];
					int columnCount = tree.getColumnCount();
					if (columnCount > 0) {
						for (int column = 0; column < columnCount; column++) {
							output.add(item.getText(column));
						}
					} else {
						// We're not adding null or empty Strings
						String thisText = item.getText();
						if (thisText != null && !"".equals(thisText)) {
							output.add(thisText);
						}
					}
					setOutput(output);
				}
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Gets an image for a specific row of the tree
	 *
	 * @param index
	 *            the index of the row to get
	 * @return an {@link Image}
	 */
	public Image getItemImage(int index) {
		final Tree tree = getWrap().getControl();
		Fetcher<Image> fetcher = new Fetcher<Image>() {
			@Override
			public void run() {
				TreeItem item = tree.getItem(index);
				Image icon = item.getImage();
				setOutput(icon);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	private void addSubordinateItemTexts(
		List<List<String>> totalList, List<String> list, TreeItem[] items, int columnCount) {
		for (TreeItem thisItem : items) {
			List<String> itemTexts = new ArrayList<>(list);
			if (columnCount > 0) {
				for (int column = 0; column < columnCount; column++) {
					itemTexts.add(thisItem.getText(column));
				}
			} else {
				// We're not adding null or empty Strings
				String thisText = thisItem.getText();
				if (thisText != null && !"".equals(thisText)) {
					itemTexts.add(thisText);
				}
			}
			if (itemTexts.size() > 0 && thisItem.getItemCount() > 0) {
				// Won't have to do deep-copying since we know that the list holds Strings (immutable)
				addSubordinateItemTexts(totalList, new ArrayList<>(itemTexts), thisItem.getItems(), columnCount);
			} else if (itemTexts.size() > 0) {
				totalList.add(itemTexts);
			}
		}
	}

	/**
	 * Returns the selected tree item text at the specified column index
	 *
	 * @param columnIndex
	 *            the column index
	 * @return a {@link String}
	 */
	public String getSelectedItemText(int columnIndex) {
		return getSelectedItemTexts().get(columnIndex);
	}

	/**
	 * Returns the selected tree item text of the column with the specified header
	 *
	 * @param columnHeader
	 *            the column header
	 * @return a {@link String}
	 */
	public String getSelectedItemText(String columnHeader) {
		return getSelectedItemText(getColumnIndex(columnHeader));
	}

	/**
	 * Finds the index of the column with the specified header
	 *
	 * @param columnHeader
	 *            the column header to match
	 * @return the index of the matching column header
	 */
	public int getColumnIndex(String columnHeader) {
		return getColumnIndex(columnHeader, true);
	}

	/**
	 * Finds the index of the column with the specified header
	 *
	 * @param columnHeader
	 *            the column header to match
	 * @param doAssert
	 *            asserts that the column really does exist
	 * @return The index of the matching column header. -1 if not found.
	 */
	public int getColumnIndex(String columnHeader, boolean doAssert) {
		Fetcher<Integer> fetcher = new Fetcher<Integer>() {
			@Override
			public void run() {
				boolean found = false;
				int index = 0;
				for (TreeColumn column : ((Tree) control.as(TreeWrap.class).getControl()).getColumns()) {
					if (column.getText().equals(columnHeader)) {
						found = true;
						break;
					}
					index++;
				}
				if (!found) {
					index = -1;
				}
				setOutput(index);
			}
		};
		Display.getDefault().syncExec(fetcher);
		int returnValue = fetcher.getOutput();
		if (doAssert) {
			Assert.assertTrue("Could not find the column with header \"" + columnHeader + "\"", returnValue != -1);
		}
		return returnValue;
	}

	private Wrap<? extends TreeItem> getSelectedItem() {
		return new ItemWrap<>(control, control.as(TreeWrap.class).getSelectedItem());
	}

	/**
	 * Selects the tree item specified by a list of string, using the mouse and scrolling with page
	 * up/down if necessary. Note that all nodes in the path to the treeitem will be clicked on by
	 * the mouse.
	 *
	 * @param path
	 *            the complete path, from the root, of the tree item to select
	 * @return {@code true} if the path was found, {@code false} otherwise
	 */
	public boolean selectByMouse(String ... path) {
		List<String> p = new ArrayList<>(Arrays.asList(path));
		return selectByMouse(null, p);
	}

	private boolean selectByMouse(ItemWrap<TreeItem> root, List<String> path) {
		if (path.isEmpty()) {
			return true;
		}

		String currentItem = path.get(0);
		TreeItem treeItem = getItem(root, currentItem);
		if (null == treeItem) {
			return false; // No such treeitem found.
		}

		ItemWrap<TreeItem> itemWrap = new ItemWrap<>(control, treeItem);
		if (!makeVisibleInTreeByScrolling(itemWrap)) {
			return false;
		}
		if (!makeSureItemIsExpanded(itemWrap)) {
			return false;
		}
		return selectByMouse(itemWrap, path.subList(1, path.size()));
	}

	// Currently we assume that it's enough with a mouse click to make sure that the tree item is expand.
	// However, this may not always be true.
	private boolean makeSureItemIsExpanded(ItemWrap<TreeItem> itemWrap) {
		if (!isItemExpanded(itemWrap)) {
			itemWrap.mouse().click();
		}
		return true;
	}

	private Boolean isItemExpanded(Wrap<? extends Item> itemWrap) {
		final Item item = itemWrap.getControl();
		Fetcher<Boolean> fetcher = new Fetcher<Boolean>() {
			@Override
			public void run() {
				setOutput(runMethod(Boolean.class, item, "getExpanded"));
			}
		};
		Display.getDefault().syncExec(fetcher);
		Boolean result = fetcher.getOutput();
		if (result == null) {
			return false;
		} else {
			return result;
		}
	}

	private boolean makeVisibleInTreeByScrolling(ItemWrap<TreeItem> itemWrap) {
		return makeVisibleInTreeByScrolling(itemWrap, 0);
	}

	// TODO: Need to make sure the whole tree is visible first.
	// TODO: Not checking if scrolling horizontally is necessary
	private boolean makeVisibleInTreeByScrolling(ItemWrap<TreeItem> itemWrap, Integer numberOfAttempts) {
		numberOfAttempts++;
		if (numberOfAttempts > MAXIMUM_NUMBER_OF_NAVIGATIONAL_ATTEMPTS) {
			System.out.println("The maximum number of scrolling attemps was reached.");
			return false;
		}
		org.jemmy.Rectangle itemRectangle = itemWrap.getScreenBounds();
		org.jemmy.Rectangle treeRectangle = control.getScreenBounds();
		Point itemPoint = itemRectangle.getLocation();
		Point treePoint = treeRectangle.getLocation();

		if (!treeRectangle.contains(itemPoint)) {
			if (itemPoint.y < treePoint.y) {
				itemWrap.keyboard().pushKey(KeyboardButtons.PAGE_UP);
			}
			if (itemPoint.y > (treePoint.y + treeRectangle.height)) {
				itemWrap.keyboard().pushKey(KeyboardButtons.PAGE_DOWN);
			}
			return makeVisibleInTreeByScrolling(itemWrap, numberOfAttempts);
		} else {
			return true;
		}
	}

	/**
	 * Context clicks the currently selected tree item and chooses the supplied option
	 *
	 * @param desiredState
	 *            the selection state to which the context choice is to be to set to
	 * @param choice
	 *            the context menu path to the option
	 */
	@SuppressWarnings("unchecked")
	public void contextChoose(boolean desiredState, String ... choice) {
		scrollbarSafeSelection();
		StringPopupSelectableOwner<Tree> spo = control.as(StringPopupSelectableOwner.class);
		spo.setPolicy(policy);
		spo.push(desiredState, getRelativeClickPoint(getSelectedItem()), choice);
	}

	/**
	 * Context clicks the currently selected tree item and finds out the selection status of the
	 * supplied option
	 *
	 * @param choice
	 *            the context menu path to the option
	 * @return the selection status of the item
	 */
	@SuppressWarnings("unchecked")
	public boolean getContextOptionState(String ... choice) {
		scrollbarSafeSelection();
		StringPopupSelectableOwner<Tree> spo = control.as(StringPopupSelectableOwner.class);
		spo.setPolicy(policy);
		return spo.getState(getRelativeClickPoint(getSelectedItem()), choice);
	}

	/**
	 * Context clicks the currently selected tree item and chooses the supplied option
	 *
	 * @param choice
	 *            the context menu path to the option
	 */
	@SuppressWarnings("unchecked")
	public void contextChoose(String ... choice) {
		scrollbarSafeSelection();
		Wrap<? extends TreeItem> selectedWrap = getSelectedItem();
		// workaround (needed on Mac OS X) to make sure that a yellow popup won't disturb during context clicking
		if (OS_NAME.contains("os x")) {
			selectedWrap.mouse().click();
		}
		StringPopupOwner<Tree> spo = control.as(StringPopupOwner.class);
		spo.setPolicy(policy);
		spo.push(getRelativeClickPoint(selectedWrap), choice);
	}

	/**
	 * Method that runs a recursive method in the UI-thread to find a tree item with the input path.
	 *
	 * @param path
	 *            the path to verify
	 * @return {@code true} if the path was found, otherwise {@code false}
	 */
	public boolean hasItem(final String ... path) {
		Fetcher<Boolean> fetcher = new Fetcher<Boolean>() {

			private boolean verifyPath(TreeItem root, String[] path) {
				if (root.getText().equals(path[0])) {
					if (path.length == 1) {
						return true;
					}
					for (TreeItem item : root.getItems()) {
						if (verifyPath(item, Arrays.copyOfRange(path, 1, path.length))) {
							return true;
						}
					}
				}
				return false;
			}

			@Override
			public void run() {
				setOutput(false);
				for (TreeItem item : getWrap().getControl().getItems()) {
					if (verifyPath(item, path)) {
						setOutput(true);
						break;
					}
				}
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	// If rootItem is null, we assume that we should start looking from the tree's root.
	private TreeItem getItem(final Wrap<? extends TreeItem> rootItem, final String itemToGet) {
		Fetcher<TreeItem> fetcher = new Fetcher<TreeItem>() {
			@Override
			public void run() {
				TreeItem[] listOfItems;
				if (null == rootItem) {
					listOfItems = getWrap().getControl().getItems();
				} else {
					listOfItems = rootItem.getControl().getItems();
				}
				setOutput(null);
				for (TreeItem item : listOfItems) {
					if (policy.equals(StringComparePolicy.EXACT)) {
						if (item.getText().equals(itemToGet)) {
							setOutput(item);
						}
					}
					if (policy.equals(StringComparePolicy.SUBSTRING)) {
						if (item.getText().indexOf(itemToGet) != -1) {
							setOutput(item);
						}
					}
				}
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Sets the text of the currently selected tree item
	 * 
	 * @param text
	 *            the text to set
	 */
	public void enterText(String text) {
		contextChoose("Change Value");
		for (int i = 0; i < text.length(); i++) {
			control.keyboard().typeChar(text.charAt(i));
		}
		// make sure that the text entered is "submitted" before moving focus elsewhere (necessary for Mac)
		control.keyboard().pushKey(KeyboardButtons.ENTER);
	}

	/**
	 * Expands the currently selected tree item
	 */
	public void expand() {
		Assert.assertTrue("Could not expand tree item with text " + getState(), setItemExpansionState(true));
	}

	/**
	 * Collapses the currently selected tree item
	 */
	public void collapse() {
		Assert.assertTrue("Could not collapse tree item with text " + getState(), setItemExpansionState(false));
	}

	private boolean setItemExpansionState(boolean desiredState) {
		int retries = MAXIMUM_NUMBER_OF_NAVIGATIONAL_ATTEMPTS;
		while (desiredState != isItemExpanded(getSelectedItem()) && retries > 0) {
			if (desiredState) {
				control.keyboard().pushKey(EXPAND_BUTTON);
			} else {
				control.keyboard().pushKey(COLLAPSE_BUTTON);
			}
			retries--;
		}
		return desiredState == isItemExpanded(getSelectedItem());
	}

	/**
	 * Sets the matching policy of this {@link MCTree} to exact string matching
	 */
	public void exactMatching() {
		policy = StringComparePolicy.EXACT;
	}

	/**
	 * Sets the matching policy of this {@link MCTree} to substring matching. This is the default
	 */
	public void substringMatching() {
		policy = StringComparePolicy.SUBSTRING;
	}

	private void setMatching(boolean exactMatching) {
		savedPolicy = policy;
		if (exactMatching) {
			exactMatching();
		} else {
			substringMatching();
		}
	}

	private void resetMatching() {
		policy = savedPolicy;
	}

	/**
	 * Calculates the click point of the child relative to the parent
	 *
	 * @param child
	 *            The wrapped child control
	 * @return the Point of the child relative to the parent
	 */
	private Point getRelativeClickPoint(final Wrap<? extends TreeItem> child) {
		Fetcher<Point> fetcher = new Fetcher<Point>() {
			@Override
			public void run() {
				Rectangle childRect = child.getControl().getBounds();
				Point clickPoint = (Point) child.getProperty(Wrap.CLICKPOINT_PROP_NAME);
				setOutput(new Point(childRect.x + clickPoint.x, childRect.y + clickPoint.y));
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Returns the number of direct child items in the tree
	 *
	 * @return the number of direct child items the tree currently contains
	 */
	public int getDirectChildItemsCount() {
		Fetcher<Integer> fetcher = new Fetcher<Integer>() {
			@Override
			public void run() {
				setOutput(getWrap().getControl().getItemCount());
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Returns a list with the strings of the direct child items in the tree
	 *
	 * @return a {@link List} of {@link String} with the strings of the direct child items in the
	 *         tree
	 */
	@SuppressWarnings("unchecked")
	public List<String> getItemsText() {
		return control.getProperty(List.class, Selectable.STATES_PROP_NAME);
	}

	/**
	 * @return the text of the currently selected TreeItem
	 */
	public String getState() {
		return control.getProperty(String.class, Selectable.STATE_PROP_NAME);
	}

	/**
	 * @return the {@link Image} of the selected tree item. {@code null} if no image has been
	 *         assigned to the item
	 */
	public Image fetchImageFromSelectedTreeItem() {
		Fetcher<Image> fetcher = new Fetcher<Image>() {
			@Override
			public void run() {
				setOutput(control.as(TreeWrap.class).getSelectedItem().getImage());
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	@SuppressWarnings("unchecked")
	private Wrap<? extends Tree> getWrap() {
		return control.as(TreeWrap.class);
	}

	/**
	 * Toggles selection state on the currently selected item
	 * 
	 * @param state
	 *            the state to set
	 */
	public void setSelectedItemState(boolean state) {
		if (selectedItemChecked() != state) {
			// Ensuring focus on the TreeItem
			getSelectedItem().mouse().click();
			// Special case for selecting MenuItem objects in the Export dialog. Linux requires two left keys to set
			// focus on the checkbox (within the MenuItem). Also, SPACE is the key to use in both Windows
			// and Linux.
			if (MCJemmyBase.OS_NAME.contains("linux")) {
				getShell().keyboard().pushKey(KeyboardButtons.LEFT);
				getShell().keyboard().pushKey(KeyboardButtons.LEFT);
			}
			getShell().keyboard().pushKey(KeyboardButtons.SPACE);
			if (state != selectedItemChecked()) {
				Assert.fail("Unable to set TreeItem state to: " + state);
			}
		}
	}

	private boolean selectedItemChecked() {
		Fetcher<Boolean> fetcher = new Fetcher<Boolean>() {
			@Override
			public void run() {
				setOutput(control.as(TreeWrap.class).getSelectedItem().getChecked());
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	private int getColumnCount() {
		Fetcher<Integer> fetcher = new Fetcher<Integer>() {
			@Override
			public void run() {
				int columnCount = getWrap().getControl().getColumnCount();
				setOutput(columnCount);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	private void scrollbarSafeSelection() {
		int currentSelection = getIndexOfSelectedItem();
		control.keyboard().pushKey(KeyboardButtons.DOWN);
		control.keyboard().pushKey(KeyboardButtons.UP);
		selectRow(currentSelection);
	}
}
