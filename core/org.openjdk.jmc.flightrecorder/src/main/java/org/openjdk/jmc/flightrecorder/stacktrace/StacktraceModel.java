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
package org.openjdk.jmc.flightrecorder.stacktrace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.collection.ArrayToolkit;
import org.openjdk.jmc.common.collection.SimpleArray;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.util.MCFrame;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

/**
 * A model for holding multiple stacktraces and their relations to each other.
 * <p>
 * The model is stateful in two ways. It uses lazy evaluation to calculate the model, and it
 * contains information about the currently selected path through the tree.
 * <p>
 * This class is not thread safe.
 * <p>
 * The typical way of using this class is to first decide on the {@link FrameSeparator} and then
 * create the model. This is done in constant time. After this you get the root fork and use the
 * {@link Fork} and {@link Branch} classes to traverse the tree of stacktraces. Getting the root
 * fork or the end fork of any branch is roughly O(n) to the number of items in the branch.
 * <p>
 * Opening a Java flight Recording and setting up the stacktrace model can be done like this:
 *
 * <pre>
 * IItemCollection items = JfrLoaderToolkit.loadEvents(file);
 * IItemCollection filteredItems = items.apply(JdkFilters.EXECUTION_SAMPLE);
 * FrameSeparator frameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);
 * StacktraceModel model = new StacktraceModel(true, frameSeparator, filteredItems);
 * Fork root = model.getRootFork();
 * </pre>
 * <p>
 * Traversing the stacktrace tree can be done like this:
 *
 * <pre>
 * void walkTree(Fork fork) {
 * 	for (Branch branch : fork.getBranches()) {
 * 		walkTree(branch.getEndFork());
 * 	}
 * }
 * </pre>
 * <p>
 * Examining the contents of a branch can be done by using {@link Branch#getFirstFrame()} and
 * {@link Branch#getTailFrames()}. These methods return {@link StacktraceFrame} entries that can be
 * queried for more information.
 */
public class StacktraceModel {

	private final IMemberAccessor<IMCStackTrace, IItem> accessor = ItemToolkit.accessor(JfrAttributes.EVENT_STACKTRACE);
	private final boolean threadRootAtTop;
	private final FrameSeparator frameSeparator;
	private final IItemCollection items;
	private Fork rootFork;

	/**
	 * @param threadRootAtTop
	 *            If true, present the thread roots on the first fork. If false, present top frames
	 *            on the first fork.
	 * @param frameSeparator
	 *            Determines how different two frames must be to motivate a fork in the model.
	 * @param items
	 *            Items containing stacktraces. Items in the collection that do not contain
	 *            stacktraces are silently ignored.
	 */
	public StacktraceModel(boolean threadRootAtTop, FrameSeparator frameSeparator, IItemCollection items) {
		this.threadRootAtTop = threadRootAtTop;
		this.frameSeparator = frameSeparator;
		this.items = items;
	}

	@Override
	public int hashCode() {
		return Objects.hash(frameSeparator, items, threadRootAtTop);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof StacktraceModel) {
			StacktraceModel other = (StacktraceModel) obj;
			return threadRootAtTop == other.threadRootAtTop && frameSeparator.equals(other.frameSeparator)
					&& items.equals(other.items);
		}
		return false;
	}

	/**
	 * Return the root fork which contains either top frames or thread roots, depending on the model
	 * configuration
	 * ({@link StacktraceModel#StacktraceModel(boolean, FrameSeparator, IItemCollection)
	 * threadRootAtTop}).
	 * <p>
	 * This is the entry point that you call when you want to access the model structure. After that
	 * you use the methods on the {@link Fork} and {@link Branch} classes to navigate the model.
	 * <p>
	 * The first call may take some time due to calculations, so it may be useful to call this in a
	 * background thread if used in a UI.
	 */
	public Fork getRootFork() {
		if (rootFork == null) {
			rootFork = new Fork(ItemToolkit.asIterable(items));
		}
		return rootFork;
	}

	private IMCFrame getFrame(IItem item, int frameIndex) {
		IMCStackTrace st = accessor.getMember(item);
		if (st != null) {
			if (threadRootAtTop && frameIndex == 0 && st.getTruncationState().isTruncated()) {
				return UNKNOWN_FRAME;
			}
			List<? extends IMCFrame> frames = st.getFrames();
			if (frames != null && frameIndex < frames.size()) {
				return frames.get(threadRootAtTop ? frames.size() - 1 - frameIndex : frameIndex);
			}
		}
		return null;
	}

	/**
	 * A special marker object that indicates a frame that cannot be determined.
	 * <p>
	 * A typical case is when a stacktrace is truncated due to to Flight Recorder settings. We know
	 * that there is a frame because of a truncation flag, but there is no information about it.
	 */
	public static final IMCFrame UNKNOWN_FRAME = new MCFrame(null, null, null, IMCFrame.Type.UNKNOWN);

	private static class FrameEntry {
		final SimpleArray<IItem> items = new SimpleArray<>(new IItem[3]);
		final IMCFrame frame;

		FrameEntry(IMCFrame frame) {
			this.frame = frame;
		}
	}

	/**
	 * Return a stream of frame entries that group the input items by distinct categories according
	 * to the frame separator.
	 */
	private List<FrameEntry> getDistinctFrames(int frameIndex, Iterable<? extends IItem> items) {
		Map<Object, SimpleArray<FrameEntry>> categories = new HashMap<>(2000);
		Object lastCategory = null; // Caching for speed
		SimpleArray<FrameEntry> lastCategoryEntries = null;
		for (IItem item : items) {
			IMCFrame frame = getFrame(item, frameIndex);
			if (frame != null) {
				// The category is only used to preliminarily group frame entries to speed up the linear findEntryForFrame method
				// FIXME: Clean up code so that it becomes more readable
				Object category = frameSeparator.getCategory(frame);
				if (!category.equals(lastCategory)) {
					lastCategoryEntries = categories.get(category);
					lastCategory = category;
					if (lastCategoryEntries == null) {
						lastCategoryEntries = new SimpleArray<>(new FrameEntry[1]);
						categories.put(category, lastCategoryEntries);
					}
				}
				findEntryForFrame(lastCategoryEntries, frame, frameSeparator).items.add(item);
			}
		}
		Collection<SimpleArray<FrameEntry>> feArrays = categories.values();
		// Avoid ArrayList resizing by precalculating size
		int nFrameEntries = 0;
		for (SimpleArray<FrameEntry> fes : feArrays) {
			nFrameEntries += fes.size();
		}
		List<FrameEntry> distinctFrames = new ArrayList<>(nFrameEntries);
		for (SimpleArray<FrameEntry> fes : feArrays) {
			for (FrameEntry fe : fes) {
				distinctFrames.add(fe);
			}
		}
		return distinctFrames;
	}

	/**
	 * Find or create a matching FrameEntry for a frame.
	 */
	private static FrameEntry findEntryForFrame(
		SimpleArray<FrameEntry> entries, IMCFrame frame, FrameSeparator frameSeparator) {
		for (FrameEntry e : entries) {
			if (frameSeparator.compareDetails(e.frame, frame)) {
				return e;
			}
		}
		FrameEntry newEntry = new FrameEntry(frame);
		entries.add(newEntry);
		return newEntry;
	}

	/**
	 * @return The number of frames in the selected branch and all its parent branches.
	 */
	private static int countFramesOnOrAbove(Branch branch) {
		if (branch != null) {
			return countFramesOnOrAbove(branch.getParentFork().getParentBranch()) + 1 + branch.getTailFrames().length;
		}
		return 0;
	}

	/**
	 * A branch is a sequence of frames without any forks. It is preceded by a {@link Fork} and ends
	 * with a fork.
	 * <p>
	 * When first constructed, the branch only has the first frame calculated. On demand, a list of
	 * non-branching "tail" frames and a fork after the branch can be calculated and retrieved.
	 * <p>
	 * Note that all frames within a branch do not necessarily have the same number of items. Stack
	 * traces that are similar in all aspects except for their sizes (e.g. [a, b] and [a, b, c]) can
	 * share a branch. Forks are only created if there are two or more different frames on the same
	 * level (e.g. [a, b, c] and [a, b, d]).
	 */
	public class Branch {
		private final Fork parentFork;
		private final StacktraceFrame firstFrame;
		private final int siblingIndex;
		// The sum of the number of items in all sibling branches preceding this one. A value between 0 and getParentFork().getItemsInFork().
		private final int itemOffsetInFork;
		private Boolean hasTail;
		private StacktraceFrame[] tailFrames;
		private Fork branchEnding;

		private Branch(Fork parent, SimpleArray<IItem> items, IMCFrame frame, int siblingIndex, int itemOffsetInFork) {
			this.parentFork = parent;
			this.siblingIndex = siblingIndex;
			this.itemOffsetInFork = itemOffsetInFork;
			firstFrame = new StacktraceFrame(items, frame, this, 0);
		}

		public int getItemOffsetInFork() {
			return itemOffsetInFork;
		}

		public Fork getParentFork() {
			return parentFork;
		}

		public boolean hasTail() {
			if (hasTail == null) {
				hasTail = calculateHasTail();
			}
			return hasTail;
		}

		/**
		 * Select a sibling branch. This sets the selection state on the parent forks.
		 *
		 * @param siblingOffset
		 *            Use 1 or -1 to select the next or previous sibling branch. If 0, then this
		 *            branch is selected. If null, clear branch selection.
		 * @return The newly selected branch. Null if branch selection was cleared.
		 * @deprecated Will eventually be moved to UI code
		 */
		@Deprecated
		public Branch selectSibling(Integer siblingOffset) {
			if (siblingOffset == null) {
				parentFork.selectBranch(null);
				return null;
			} else {
				Branch[] siblings = parentFork.branches;
				int selectedSibling = Math.max(0, Math.min(siblings.length - 1, (siblingIndex + siblingOffset)));
				parentFork.selectBranch(selectedSibling);
				return siblings[selectedSibling];
			}
		}

		public StacktraceFrame getFirstFrame() {
			return firstFrame;
		}

		/**
		 * @return The last frame in this branch. If the branch length is 1, then this will be equal
		 *         to the first frame.
		 */
		public StacktraceFrame getLastFrame() {
			StacktraceFrame[] tail = getTailFrames();
			return tail.length > 0 ? tail[tail.length - 1] : firstFrame;
		}

		/**
		 * @return Get non-branching tail frames in this branch. If you are building a UI where you
		 *         are not interested in non-branching frames, then you may want to ignore the tail.
		 */
		public StacktraceFrame[] getTailFrames() {
			if (tailFrames == null) {
				tailFrames = buildTail();
			}
			return tailFrames;
		}

		/**
		 * @return the fork with branches following this branch
		 */
		public Fork getEndFork() {
			if (branchEnding == null) {
				branchEnding = new Fork(this);
			}
			return branchEnding;
		}

		private boolean calculateHasTail() {
			int firstTailFrameIndex = countFramesOnOrAbove(parentFork.getParentBranch()) + 1;
			for (IItem item : firstFrame.getItems()) {
				IMCFrame frame = getFrame(item, firstTailFrameIndex);
				if (frame != null) {
					return true;
				}
			}
			return false;
		}

		private StacktraceFrame[] buildTail() {
			SimpleArray<StacktraceFrame> tail = new SimpleArray<>(new StacktraceFrame[5]);
			int nextIndex = countFramesOnOrAbove(parentFork.getParentBranch()) + 1; // first tail frame index
			StacktraceFrame node = firstFrame;
			while (true) {
				List<Integer> removeIndexes = new ArrayList<>();
				IMCFrame commonFrame = null;
				int itemIndex = 0;
				for (IItem item : node.getItems()) {
					IMCFrame frame = getFrame(item, nextIndex);
					if (frame == null) {
						// trace ended before branch
						removeIndexes.add(itemIndex);
					} else {
						if (commonFrame == null) {
							commonFrame = frame;
						} else if (frameSeparator.isSeparate(commonFrame, frame)) {
							// branch found
							return tail.elements();
						}
					}
					itemIndex++;
				}
				if (commonFrame == null) {
					// All branches match
					return tail.elements();
				} else if (removeIndexes.isEmpty()) {
					node = new StacktraceFrame(node.getItems(), commonFrame, this, tail.size() + 1);
				} else {
					IItem[] subset = ArrayToolkit.filter(node.getItems().elements(), removeIndexes);
					node = new StacktraceFrame(subset, commonFrame, this, tail.size() + 1);
				}
				tail.add(node);
				nextIndex++;
			}
		}

	}

	private final static Comparator<FrameEntry> COUNT_CMP = new Comparator<FrameEntry>() {

		@Override
		public int compare(FrameEntry o1, FrameEntry o2) {
			return o2.items.size() - o1.items.size();
		}
	};

	/**
	 * A fork is a collection of branches that share a common parent branch. The fork also keeps
	 * track of which one of its branches is currently selected.
	 * <p>
	 * This class might eventually be merged with the {@link Branch} class.
	 */
	public class Fork {
		private final Branch parentBranch;
		private final Branch[] branches;
		// The sum of the number of items in all forks preceding this one. A value between 0 and StacktraceModel.items.length.
		private final int itemOffset;
		private final int itemsInFork;
		private Integer selectedBranchIndex;

		private Fork(Branch parentBranch) {
			this(parentBranch.getLastFrame().getItems(),
					parentBranch.getParentFork().itemOffset + parentBranch.itemOffsetInFork, parentBranch);
		}

		private Fork(Iterable<? extends IItem> items) {
			this(items, 0, null);
		}

		/**
		 * Create a fork by grouping items by distinct head frames using the frame separator. If a
		 * parent branch is specified, then look for head frames after the parent branch.
		 */
		private Fork(Iterable<? extends IItem> items, int itemOffset, Branch parentBranch) {
			this.itemOffset = itemOffset;
			this.parentBranch = parentBranch;
			List<FrameEntry> branchHeadFrames = getDistinctFrames(countFramesOnOrAbove(parentBranch), items);
			Collections.sort(branchHeadFrames, COUNT_CMP);
			int itemsInFork = 0;
			SimpleArray<Branch> branches = new SimpleArray<>(new Branch[branchHeadFrames.size()]);
			for (FrameEntry fe : branchHeadFrames) {
				Branch b = new Branch(Fork.this, fe.items, fe.frame, branches.size(), itemsInFork);
				itemsInFork += fe.items.size();
				branches.add(b);
			}
			selectedBranchIndex = branches.size() > 0 ? 0 : null; // To disable default branch selection: always set null
			this.branches = branches.elements();
			this.itemsInFork = itemsInFork;
		}

		public int getItemOffset() {
			return itemOffset;
		}

		public int getItemsInFork() {
			return itemsInFork;
		}

		public Branch getParentBranch() {
			return parentBranch;
		}

		public int getBranchCount() {
			return branches.length;
		}

		/**
		 * @deprecated Will eventually be moved to UI code
		 */
		@Deprecated
		public Branch getSelectedBranch() {
			return selectedBranchIndex != null ? branches[selectedBranchIndex] : null;
		}

		public Branch getBranch(int branchIndex) {
			return branches[branchIndex];
		}

		public Branch[] getBranches() {
			return branches;
		}

		/**
		 * @return the first frame of each child branch to this fork
		 */
		public StacktraceFrame[] getFirstFrames() {
			StacktraceFrame[] firstFrames = new StacktraceFrame[branches.length];
			for (int i = 0; i < branches.length; i++) {
				firstFrames[i] = branches[i].getFirstFrame();
			}
			return firstFrames;
		}

		/**
		 * Select a child branch by its index.
		 *
		 * @param branchIndex
		 *            Index of branch to select. If null, then selection will be cleared.
		 * @deprecated Will eventually be moved to UI code
		 */
		@Deprecated
		public void selectBranch(Integer branchIndex) {
			if (parentBranch != null) {
				parentBranch.selectSibling(0);
			}
			// FIXME: Check input value range?
			selectedBranchIndex = branchIndex;
		}
	}

}
