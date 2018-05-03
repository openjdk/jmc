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
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.jemmy.interfaces.Keyboard.KeyboardButtons;
import org.junit.Assert;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;

/**
 * The Jemmy wrapper for the StackTrace part of the JFR GUI.
 */
public class JfrStackTrace extends MCJemmyBase {
	private MCTable table;
	private MCTree tree;
	private MCToolBar toolBar;
	private MCTabFolder tabFolder;

	private Image arrow_down;
	private Image arrow_up;
	private Image arrow_curved_down;
	private Image arrow_curved_up;
	private Image arrow_fork3_down;
	private Image arrow_fork3_up;

	private Image[] referenceIcons = null;

	private String FRAME_GROUP = "Choose Frame Group";
	private String PREVIOUS = "Previous Frame Group";
	private String NEXT = "Next Frame Group";
	private String TREE = "Show as Tree";
	private String GROUP_METHOD = "Group traces from last method";
	private String GROUP_ROOT = "Group traces from thread root";

	/**
	 * For FormattingOptions in the context menu
	 */
	public enum FormatOption {
		HIDDEN("Hidden"), CLASS_NAME("Class Name"), CLASS_AND_PACKAGE_NAME("Class and Package Name");

		private final String text;

		private FormatOption(final String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	/**
	 * For Distinguish Frames By in the context menu
	 */
	public enum LevelOption {
		PACKAGE("Package"),
		CLASS("Class"),
		METHOD("Method"),
		LINE_NUMBER("Line Number"),
		BYTE_CODE_INDEX("Byte Code Index");

		private final String text;

		private LevelOption(final String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	/**
	 * Note: This constructor has side effects: stack frames will be selected and buttons clicked.
	 * The reason for this is that icons need to be collected.
	 */
	public JfrStackTrace() {
		tabFolder = MCTabFolder.getByTabName("Stack Trace");

		tabFolder.select("Stack Trace");

		refreshTableAndTree();
		toolBar = MCToolBar.getByToolTip(FRAME_GROUP);
		collectAllIcons();
	}

	/**
	 * We need to get hold of all six different icons that should be used for the stack frames. This
	 * requires that we move about a bit in the table/tree.
	 */
	private void collectAllIcons() {

		/*
		 * FIXME: Assuming some state (table, not tree, and "Group traces from thread root" first).
		 * Need to make sure we have this state first.
		 */
		clickGroupTracesFromThreadRootButton();
		selectFirstFrame();

		// In the Group Traces from Thread Root mode, the top row
		// will always have a fork. Possible exception: if there is only one since event.
		JfrStackFrame topItem = getFrame(0);
		if (topItem.count > 1) {
			arrow_fork3_down = topItem.icon;
		} else {
			Assert.fail("Only one stack trace. Cannot test.");
		}

		// Walk down in the table until we have the same count as for the line above.
		JfrStackFrame thisLine = topItem;
		JfrStackFrame nextLine = getFrame(1);
		int index = 1;
		while (!thisLine.count.equals(nextLine.count)) {
			thisLine = nextLine;
			nextLine = getFrame(++index);
		}

		// Since the row above has the same count, this row
		// should have a straight down arrow.
		arrow_down = nextLine.icon;

		// Now, do the same with the "Group traces from last method" mode.
		clickGroupTracesFromLastMethodButton();
		clickFrameList();

		// We assume (falsely) that this is always a fork up.
		topItem = getFrame(0);
		if (topItem.count > 1) {
			arrow_fork3_up = topItem.icon;
		} else {
			Assert.fail("Only one stack trace. Cannot test.");
		}

		// Walk down in the table until we have the same count as for the line above.
		thisLine = topItem;
		nextLine = getFrame(1);
		index = 1;
		while (!thisLine.count.equals(nextLine.count)) {
			thisLine = nextLine;
			nextLine = getFrame(++index);
		}

		// Since the row above has the same count, this row
		// should have a straight up arrow.
		arrow_up = nextLine.icon;

		// Next icons to grab are the ones that appear in the "Choose Frame Group" mode.
		selectFirstFrame();
		clickChooseFrameGroupButton();
		thisLine = getFrame(0);
		selectFirstFrame();
		arrow_curved_up = thisLine.icon;

		// ... and we need to switch mode again.
		clickGroupTracesFromThreadRootButton();
		selectFirstFrame();
		clickChooseFrameGroupButton();
		thisLine = getFrame(0);
		arrow_curved_down = thisLine.icon;

		referenceIcons = new Image[6];
		referenceIcons[JfrStackFrame.ARROW_UP] = arrow_up;
		referenceIcons[JfrStackFrame.ARROW_DOWN] = arrow_down;
		referenceIcons[JfrStackFrame.ARROW_CURVED_DOWN] = arrow_curved_down;
		referenceIcons[JfrStackFrame.ARROW_CURVED_UP] = arrow_curved_up;
		referenceIcons[JfrStackFrame.ARROW_FORK3_DOWN] = arrow_fork3_down;
		referenceIcons[JfrStackFrame.ARROW_FORK3_UP] = arrow_fork3_up;
	}

	/**
	 * Gets the Table and Tree that is a child of this TabFolder
	 * <p>
	 * The user can switch between Table view and Tree view.
	 */
	private void refreshTableAndTree() {
		List<MCTable> tables = MCJemmyBase.getTables();
		if (tables == null || tables.size() == 0) {
			tables = null;
		} else {
			for (MCTable t : tables) {
				if (t.hasAsAncestor(tabFolder)) {
					table = t;
				}
			}
		}

		List<MCTree> trees = MCTree.getAll();
		if (trees == null || trees.size() == 0) {
			trees = null;
		} else {
			for (MCTree t : trees) {
				if (t.hasAsAncestor(tabFolder)) {
					tree = t;
				}
			}
		}

	}

	/**
	 * Method to determine if the current representation of the stack trace is a tree. Returns
	 * {@code false} if represented as a table.
	 *
	 * @return {@code true} if the current representation of the stack trace is a tree.
	 */
	private boolean isTree() {
		if (null == tree || tree.isDisposed()) {
			return false;
		}
		return true;
	}

	/**
	 * Six different images (arrow_up, arrow_down, etc) should be possible to find in the stack
	 * frames of the stack trace view.
	 *
	 * @return an array with six images
	 */
	public Image[] getStackFrameImages() {
		return referenceIcons;
	}

	/**
	 * Selects the first frame
	 *
	 * @return {@code true} if selected frame is the first frame, otherwise {@code false}
	 */
	public Boolean selectFirstFrame() {
		return selectFrame(0);
	}

	/**
	 * Selects the frame at the given index (if not -1))
	 *
	 * @param index
	 *            the index of the frame
	 * @return {@code true} if selected index is the same as the provided, otherwise {@code false}
	 */
	public Boolean selectFrame(int index) {
		if (isTree()) {
			return tree.selectRow(index);
		} else {
			return table.select(index);
		}
	}

	/**
	 * Gets the number of frames in the stack trace view
	 *
	 * @return the number of frames
	 */
	public List<JfrStackFrame> getAllFrames() {
		int max = getFrameCount();
		List<JfrStackFrame> allFrames = new ArrayList<>();
		for (int i = 0; i < max; i++) {
			allFrames.add(getFrame(i));
		}
		return allFrames;
	}

	/**
	 * Gets the number of frames in the stack trace view
	 *
	 * @return the number of frames
	 */
	public int getFrameCount() {
		if (isTree()) {
			return tree.getItemCount();
		} else {
			return table.getItemCount();
		}
	}

	/**
	 * Clicks somewhere on the list of frames.
	 */
	public void clickFrameList() {
		if (isTree()) {
			tree.click();
		} else {
			table.click();
		}
	}

	/**
	 * Context clicks the currently selected stack frame and chooses the supplied option
	 *
	 * @param desiredState
	 *            the selection state to which the context choice is to be to set to
	 * @param choice
	 *            the context menu path to the option
	 */
	public void contextChoose(boolean desiredState, String ... choice) {
		if (isTree()) {
			tree.contextChoose(desiredState, choice);
		} else {
			table.contextChoose(desiredState, choice);
		}
	}

	/**
	 * Gets the stack frame at the top. It does not matter if the stack frames are viewed as a tree
	 * or as a table.
	 *
	 * @return the frame at position 0 (the top)
	 */
	public JfrStackFrame getFirstFrame() {
		return getFrame(0);
	}

	/**
	 * Gets the stack frame at position index among the frames. It does not matter if the stack
	 * frames are viewed as a tree or as a table.
	 *
	 * @param index
	 *            the position to get the frame at.
	 * @return the frame at position index
	 */
	public JfrStackFrame getFrame(int index) {
		List<String> texts;
		Image icon;
		if (isTree()) {
			texts = tree.getItemTexts(index);
			icon = tree.getItemImage(index);
		} else {
			texts = table.getItemTexts(index);
			icon = table.getItemImage(index);
		}
		String text = texts.get(0);
		Integer count = Integer.valueOf(texts.get(1));
		JfrStackFrame stackFrame = new JfrStackFrame(icon, text, count, referenceIcons);
		return stackFrame;
	}

	/**
	 * Clicks the button "Choose Frame Group" in the toolbar
	 */
	public void clickChooseFrameGroupButton() {
		toolBar.clickToolItem(FRAME_GROUP);
		waitForIdle();
	}

	/**
	 * Clicks the button "Previous Frame Group" in the toolbar
	 */
	public void clickPreviousFrameGroupButton() {
		toolBar.clickToolItem(PREVIOUS);
		waitForIdle();
	}

	/**
	 * Clicks the button "Next Frame Group" in the toolbar
	 */
	public void clickNextFrameGroupButton() {
		toolBar.clickToolItem(NEXT);
		waitForIdle();
	}

	/**
	 * Clicks the button "Show as Tree" in the toolbar
	 */
	public void clickShowAsTreeButton() {
		toolBar.clickToolItem(TREE);
		refreshTableAndTree();
		waitForIdle();
	}

	/**
	 * Makes sure that the the state of the Choose Frame Group corresponds with the provided value.
	 * Note that if no frame is selected in the tree/table, the current state cannot be determined
	 * correctly.
	 *
	 * @param state
	 *            {@code true} if the stack trace should be in Choose Frame Group mode, otherwise
	 *            {@code false}
	 */
	public void setChooseFrameGroup(Boolean state) {
		// If we are in "Choose Frame Group" mode, the frameButton is enabled,
		// but the Previous and Next buttons are not
		Boolean currentState = frameButtonEnabled() && !nextButtonEnabled() && !previousButtonEnabled();
		if (currentState == state) {
			return;
		} else {
			clickChooseFrameGroupButton();
			currentState = frameButtonEnabled() && !nextButtonEnabled() && !previousButtonEnabled();
			if (currentState == state) {
				return;
			} else {
				clickChooseFrameGroupButton();
				currentState = frameButtonEnabled() && !nextButtonEnabled() && !previousButtonEnabled();
			}
		}
	}

	/**
	 * Makes sure that the the state of the Show as tree mode corresponds with the provided value.
	 *
	 * @param state
	 *            {@code true} if the stack trace should be displayed as a tree, {@code false} if it
	 *            should be displayed as a table
	 */
	public void setViewAsTree(Boolean state) {
		Boolean currentState = isTree();
		if (currentState == state) {
			return;
		} else {
			clickShowAsTreeButton();
		}
	}

	/**
	 * Checks if the "Show as Tree" button in the toolbar is enabled.
	 * 
	 * @return {@code true} if enabled, {@code false} if not
	 */
	public Boolean treeButtonEnabled() {
		return toolBar.toolItemEnabled(TREE);
	}

	/**
	 * Checks if the "Choose Frame Group" button in the toolbar is enabled.
	 * 
	 * @return {@code true} if enabled, {@code false} if not
	 */
	public Boolean frameButtonEnabled() {
		return toolBar.toolItemEnabled(FRAME_GROUP);
	}

	/**
	 * Checks if the "Next" button in the toolbar is enabled.
	 * 
	 * @return {@code true} if enabled, {@code false} if not
	 */
	public Boolean nextButtonEnabled() {
		return toolBar.toolItemEnabled(NEXT);
	}

	/**
	 * Checks if the "Previous" button in the toolbar is enabled.
	 * 
	 * @return {@code true} if enabled, {@code false} if not
	 */
	public Boolean previousButtonEnabled() {
		return toolBar.toolItemEnabled(PREVIOUS);
	}

	/**
	 * Clicks the "Group Traces From Last Method" button in the toolbar
	 */
	public void clickGroupTracesFromLastMethodButton() {
		toolBar.clickToolItem(GROUP_METHOD);
		waitForIdle();
	}

	/**
	 * Clicks the "Group Traces From Thread Root" button in the toolbar
	 */
	public void clickGroupTracesFromThreadRootButton() {
		toolBar.clickToolItem(GROUP_ROOT);
		waitForIdle();
	}

	// TODO: Should these be here? Or do it directly in the tests?
	public void keyboardEnter() {
		getShell().keyboard().pushKey(KeyboardButtons.ENTER);
		waitForIdle();
	}

	public void keyboardRight() {
		getShell().keyboard().pushKey(KeyboardButtons.RIGHT);
		waitForIdle();
	}

	public void keyboardLeft() {
		getShell().keyboard().pushKey(KeyboardButtons.LEFT);
		waitForIdle();
	}

	public void keyboardUp() {
		getShell().keyboard().pushKey(KeyboardButtons.UP);
		waitForIdle();
	}

	public void keyboardDown() {
		getShell().keyboard().pushKey(KeyboardButtons.DOWN);
		waitForIdle();
	}

	/**
	 * Stack frames can be distinguished by different levels through a setting in the context menu.
	 * This methods makes sure the setting in the context menu matches the desired state.
	 * <p>
	 * Note: A side effect of this method is that the items in the table or tree can change, so any
	 * selection there can be lost.
	 *
	 * @param level
	 *            the desired level that shall be used to distinguish frames
	 */
	public void setDistinguishFramesByLevel(LevelOption level) {
		String[] contextMenuPath = new String[] {"Distinguish Frames By", level.toString()};
		contextChoose(true, contextMenuPath);
		waitForIdle();
	}

	/**
	 * Stack frames can be distinguished by different levels of details through a setting in the
	 * context menu. This methods makes sure the setting in the context menu for Optimization Type
	 * matches the desired state.
	 *
	 * @param enabled
	 *            the desired state of whether optimization type shall be used to distinguish frames
	 *            or not
	 */
	public void setDistinguishFramesByOptimizationType(Boolean enabled) {
		String[] contextMenuPath = new String[] {"Distinguish Frames By", "Optimization Type"};
		contextChoose(enabled, contextMenuPath);
		waitForIdle();
	}

	/**
	 * Stack frames can be distinguished by different levels of details through a setting in the
	 * context menu. This methods makes sure the setting in the context menu for Optimization Type
	 * matches the desired states.
	 *
	 * @param returnValue
	 *            the format option for how return values should be formatted
	 * @param classValue
	 *            the format option for how class should be formatted
	 * @param parametersValue
	 *            the format option for how parameters should be formatted
	 */
	public void setMethodFormatting(FormatOption returnValue, FormatOption classValue, FormatOption parametersValue) {
		if (null != returnValue) {
			String[] returnFormat = new String[] {"Method Formatting Options", "Return Value", returnValue.text};
			contextChoose(true, returnFormat);
		}

		if (null != classValue) {
			String[] classFormat = new String[] {"Method Formatting Options", "Class", classValue.text};
			contextChoose(true, classFormat);
		}

		if (null != parametersValue) {
			String[] parametersFormat = new String[] {"Method Formatting Options", "Parameters", parametersValue.text};
			contextChoose(true, parametersFormat);
		}
	}

	/**
	 * Finds and selects a stack frame with branch. Searches from the top frame downwards, one frame
	 * at a time.
	 *
	 * @return an index to a stack frame which has branch (= has siblings) or -1 if no frame with
	 *         branch is found
	 */
	public int selectFrameWithBranch() {
		return selectFrameWithBranch(0);
	}

	public int selectFrameWithBranch(int startIndex) {
		return selectFrameWithBranch(startIndex, 1);
	}

	public int selectFrameWithBranch(int startIndex, int increment) {
		return selectFrameWithSiblings(startIndex, increment, true);
	}

	public int selectFrameWithoutBranch() {
		return selectFrameWithoutBranch(0);
	}

	public int selectFrameWithoutBranch(int startIndex) {
		return selectFrameWithoutBranch(startIndex, 1);
	}

	public int selectFrameWithoutBranch(int startIndex, int increment) {
		return selectFrameWithSiblings(startIndex, increment, false);
	}

	/**
	 * Steps through the list looking for a frame matching the siblings requirement
	 *
	 * @param startIndex
	 *            The index where the search should start
	 * @param increment
	 *            How the next index is picked. 1 means searching down, -1 means searching up
	 * @param hasSiblings
	 *            If the frame should have siblings or not
	 * @return The index of the first matching frame found
	 */

	private int selectFrameWithSiblings(int startIndex, int increment, Boolean hasSiblings) {
		int max = getFrameCount();
		for (int i = startIndex; i < max && i > -1; i = i + increment) {
			if (getFrame(i).hasSiblings() == hasSiblings) {
				selectFrame(i);
				return i;
			}
		}
		return -1;
	}

}
