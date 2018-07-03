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
package org.openjdk.jmc.flightrecorder.rules.tree.traversal;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.rules.tree.ITreeNode;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

/**
 * Iterates from the root to the leaf, always selecting the event with the longest duration in each
 * layer.
 */
public class LongestDurationIterator implements Iterator<IItem> {
	private ITreeNode<IItem> currentNode;

	public LongestDurationIterator(ITreeNode<IItem> node) {
		this.currentNode = getFirstNode(node);
	}

	// We allow providing a root node, without any items, having a child which is the longest lasting one.
	private ITreeNode<IItem> getFirstNode(ITreeNode<IItem> initNode) {
		if (initNode.getValue() == null) {
			return getLongestLastingChild(initNode.getChildren());
		}
		return initNode;
	}

	@Override
	public boolean hasNext() {
		return currentNode != null;
	}

	@Override
	public IItem next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		IItem value = currentNode.getValue();
		currentNode = getLongestLastingChild(currentNode.getChildren());
		return value;
	}

	private ITreeNode<IItem> getLongestLastingChild(List<ITreeNode<IItem>> children) {
		ITreeNode<IItem> longestLasting = null;
		IQuantity longestDuration = null;
		for (ITreeNode<IItem> node : children) {
			IItem nodeItem = node.getValue();
			if (nodeItem == null) {
				continue;
			}
			if (longestDuration == null || longestDuration.compareTo(RulesToolkit.getDuration(nodeItem)) < 0) {
				longestDuration = RulesToolkit.getDuration(nodeItem);
				longestLasting = node;
			}
		}
		return longestLasting;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("This iterator does not support removals!"); //$NON-NLS-1$
	}
}
