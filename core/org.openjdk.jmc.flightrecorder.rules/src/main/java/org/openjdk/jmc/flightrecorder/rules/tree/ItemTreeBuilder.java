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
package org.openjdk.jmc.flightrecorder.rules.tree;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

/**
 * Helper for building item trees.
 */
public class ItemTreeBuilder {

	/**
	 * Interface used to allow interrupting slow builds of encapsulation trees.
	 */
	public interface IItemTreeBuilderCallback {
		boolean shouldContinue();
	}

	private static IItemTreeBuilderCallback DEFAULT_CALLBACK = new IItemTreeBuilderCallback() {
		@Override
		public boolean shouldContinue() {
			return true;
		}
	};

	/**
	 * Builds a tree where events that wrap other events, time wise, are higher up in the hierarchy.
	 *
	 * @param items
	 *            the items to build a tree from
	 * @param allowInstants
	 *            {@code true} to allow instant events in the resulting tree
	 * @return the root node for the resulting tree
	 */
	public static ITreeNode<IItem> buildEncapsulationTree(IItemCollection items, boolean allowInstants) {
		return buildEncapsulationTree(items, allowInstants, false);
	}

	/**
	 * Builds a tree where events that wrap other events, time wise, are higher up in the hierarchy.
	 *
	 * @param items
	 *            the items to build a tree from
	 * @param allowInstants
	 *            {@code true} to allow instant events in the resulting tree
	 * @param ignoreThread
	 *            {@code true} to make the algorithm not care about event thread, can be used for VM
	 *            level events. It's up to the caller to make sure this is safe to do.
	 * @return the root node for the resulting tree.
	 */
	public static ITreeNode<IItem> buildEncapsulationTree(
		IItemCollection items, boolean allowInstants, boolean ignoreThread) {
		return buildEncapsulationTree(items, allowInstants, ignoreThread, DEFAULT_CALLBACK);
	}

	/**
	 * Builds a tree where events that wrap other events, time wise, are higher up in the hierarchy.
	 *
	 * @param items
	 *            the items to build a tree from
	 * @param allowInstants
	 *            {@code true} to allow instant events in the resulting tree
	 * @param ignoreThread
	 *            {@code true} to make the algorithm not care about event thread, can be used for VM
	 *            level events. It's up to the caller to make sure this is safe to do.
	 * @param callback
	 *            callback used to determine whether or not to continue building the encapsulation
	 *            tree
	 * @return the root node for the resulting tree.
	 */
	public static ITreeNode<IItem> buildEncapsulationTree(
		IItemCollection items, boolean allowInstants, boolean ignoreThread, IItemTreeBuilderCallback callback) {
		// FIXME: Consider introducing a maxdepth at which to stop adding nodes
		TreeNode<IItem> root = new TreeNode<>(null);
		for (IItemIterable itemIterable : items) {
			IMemberAccessor<IQuantity, IItem> durationAccessor = JfrAttributes.DURATION
					.getAccessor(itemIterable.getType());
			IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME
					.getAccessor(itemIterable.getType());
			IMemberAccessor<IQuantity, IItem> endTimeAccessor = JfrAttributes.END_TIME
					.getAccessor(itemIterable.getType());
			IMemberAccessor<IMCThread, IItem> threadAccessor = JfrAttributes.EVENT_THREAD
					.getAccessor(itemIterable.getType());
			for (IItem item : itemIterable) {
				if (!callback.shouldContinue()) {
					return root;
				}
				IQuantity duration = durationAccessor.getMember(item);
				boolean hasDuration = duration.longValue() != 0;
				IMCThread thread = threadAccessor == null ? null : threadAccessor.getMember(item);
				if (hasDuration || allowInstants) {
					addTimeSplitNode(root, item, hasDuration, startTimeAccessor.getMember(item),
							endTimeAccessor.getMember(item), thread, callback, ignoreThread);
				}
			}
		}
		return root;
	}

	private static void addTimeSplitNode(
		TreeNode<IItem> node, IItem item, boolean itemHasDuration, IQuantity itemStartTime, IQuantity itemEndTime,
		IMCThread itemThread, IItemTreeBuilderCallback callback, boolean ignoreThread) {
		TreeNode<IItem> addedNode = null;
		List<ITreeNode<IItem>> children = new ArrayList<>(node.getChildren());
		for (ITreeNode<IItem> child : children) {
			if (!callback.shouldContinue()) {
				return;
			}
			if (treeItemEncloses((TreeNode<IItem>) child, itemStartTime, itemEndTime, itemThread, ignoreThread)) {
				addTimeSplitNode((TreeNode<IItem>) child, item, itemHasDuration, itemStartTime, itemEndTime, itemThread,
						callback, ignoreThread);
				return;
			} else if (enclosesTreeItem(itemHasDuration, itemStartTime, itemEndTime, itemThread,
					(TreeNode<IItem>) child, ignoreThread)) {
				((TreeNode<IItem>) child).detach();
				if (addedNode == null) {
					addedNode = new TreeNode<>(item, itemHasDuration, itemStartTime, itemEndTime, itemThread);
					node.addChild(addedNode);
				}
				addedNode.addChild((TreeNode<IItem>) child);
			}
		}
		if (addedNode == null) {
			node.addChild(new TreeNode<>(item, itemHasDuration, itemStartTime, itemEndTime, itemThread));
		}
	}

	private static boolean enclosesTreeItem(
		boolean encloserHasDuration, IQuantity encloserStartTime, IQuantity encloserEndTime, IMCThread encloserThread,
		TreeNode<IItem> enclosee, boolean ignoreThread) {
		if (encloserHasDuration) {
			return encloserStartTime.compareTo(enclosee.getStartTime()) <= 0
					&& encloserEndTime.compareTo(enclosee.getEndTime()) >= 0
					&& (ignoreThread || encloserThread.equals(enclosee.getThread()));
		}
		return false;
	}

	private static boolean treeItemEncloses(
		TreeNode<IItem> encloser, IQuantity encloseeStartTime, IQuantity encloseeEndTime, IMCThread encloseeThread,
		boolean ignoreThread) {
		if (encloser.hasDuration()) {
			return encloser.getStartTime().compareTo(encloseeStartTime) <= 0
					&& encloser.getEndTime().compareTo(encloseeEndTime) >= 0
					&& (ignoreThread || encloser.getThread().equals(encloseeThread));
		}
		return false;
	}
}
