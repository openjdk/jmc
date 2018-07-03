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

/**
 * A simple mutable tree node implementation, with a typed payload.
 *
 * @param <T>
 *            the type of the payload
 */
public class TreeNode<T> implements ITreeNode<T> {
	private TreeNode<T> parent;
	private List<ITreeNode<T>> children = new ArrayList<>();
	private T item;

	/**
	 * Create a new node.
	 *
	 * @param item
	 *            node payload
	 */
	public TreeNode(T item) {
		this.item = item;
	}

	@Override
	public ITreeNode<T> getParent() {
		return parent;
	}

	@Override
	public List<ITreeNode<T>> getChildren() {
		return children;
	}

	@Override
	public T getValue() {
		return item;
	}

	@Override
	public void accept(ITreeVisitor<T> visitor) {
		visitor.visit(this);
	}

	/**
	 * Add a child node.
	 *
	 * @param node
	 *            child node to add
	 */
	public void addChild(TreeNode<T> node) {
		children.add(node);
		node.parent = this;
	}

	/**
	 * Remove a child node.
	 *
	 * @param node
	 *            child node to remove
	 */
	void removeChild(TreeNode<T> node) {
		children.remove(node);
		node.parent = null;
	}

	/**
	 * Set the node payload.
	 *
	 * @param item
	 *            new node payload
	 */
	void setItem(T item) {
		this.item = item;
	}

	@Override
	public String toString() {
		return "TreeNode: " + String.valueOf(item); //$NON-NLS-1$
	}

	/**
	 * Detach this node from its parent.
	 */
	public void detach() {
		if (parent != null) {
			parent.removeChild(this);
		}
	}
}
