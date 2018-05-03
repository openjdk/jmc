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
package org.openjdk.jmc.ui.common.tree;

import org.eclipse.core.runtime.IAdaptable;

/**
 * The default implementation of {@link ITreeNode}. It is the user responsibility to assert that the
 * tree model is kept in order. If a new node is assigned another node as parent it is the user
 * responsibility to insert the new node in the set of children of the parent node.
 */
public class DefaultTreeNode implements ITreeNode {

	private ITreeNode parent; // the parent node of this node or null if root
	private Object userData; // the user data stored in this node
	private ITreeNode[] children; // the children of this node or null if root

	/**
	 * Creates a new {@link DefaultTreeNode} with given user data and parent node.
	 *
	 * @param parent
	 *            the parent node of this node, or null if this node is a root
	 * @param userData
	 *            the user data to store in this node
	 */
	public DefaultTreeNode(ITreeNode parent, Object userData) {
		setParent(parent);
		setUserData(userData);
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object getAdapter(Class adapter) {
		if (adapter.isAssignableFrom(getUserData().getClass())) {
			return getUserData();
		} else if (getUserData() instanceof IAdaptable) {
			return ((IAdaptable) getUserData()).getAdapter(adapter);
		} else {
			return null;
		}
	}

	/**
	 * Sets the parent node of this node.
	 *
	 * @param parent
	 *            the parent node of this node, or null if this node is a root
	 */
	public void setParent(ITreeNode parent) {
		this.parent = parent;
	}

	@Override
	public ITreeNode getParent() {
		return parent;
	}

	/**
	 * Sets the user data of this node.
	 *
	 * @param userData
	 *            the user data to store in this node
	 */
	public void setUserData(Object userData) {
		this.userData = userData;
	}

	@Override
	public Object getUserData() {
		return userData;
	}

	/**
	 * Sets the children nodes of this node.
	 *
	 * @param children
	 *            the children of this node, or null if leaf
	 */
	public void setChildren(ITreeNode[] children) {
		this.children = children;
	}

	/**
	 * Returns the children of this node. The return value null indicates that this node is a leaf.
	 * The returned array is the actual array of children so any changes will alter the children of
	 * this node.
	 *
	 * @return the children of this node or null
	 */
	@Override
	public ITreeNode[] getChildren() {
		return children;
	}

	/**
	 * Returns a copy of the children of this node. The return value null indicates that this node
	 * is a leaf. The returned array can be changed without altering the children of this node.
	 *
	 * @return the children of this node or null
	 */
	public ITreeNode[] getCopyOfChildren() {
		ITreeNode[] copyOfChildren = null;
		if (children != null) {
			copyOfChildren = new ITreeNode[children.length];
			System.arraycopy(children, 0, copyOfChildren, 0, children.length);
		}
		return copyOfChildren;
	}

	/**
	 * Returns a string representation of this node, including it's children nodes. Will check to
	 * ensure that the parent node of the children nodes is this node.
	 *
	 * @return a textual representation of this node
	 * @throws IllegalStateException
	 *             if parent node of children is not this node
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());
		sb.append("[userData=["); //$NON-NLS-1$
		sb.append(userData.toString());
		if (children == null) {
			sb.append("]]"); //$NON-NLS-1$
		} else {
			sb.append("], children=["); //$NON-NLS-1$
			for (int i = 0; i < children.length; i += 1) {
				if (i > 0) {
					sb.append(", "); //$NON-NLS-1$
				}
				if (!equals(children[i].getParent())) {
					throw new IllegalStateException("Child has other parent than this!"); //$NON-NLS-1$
				}
				sb.append(children[i].toString());
			}
			sb.append("]]"); //$NON-NLS-1$
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + ((userData == null) ? 0 : userData.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DefaultTreeNode other = (DefaultTreeNode) obj;
		if (parent == null) {
			if (other.parent != null) {
				return false;
			}
		} else if (!parent.equals(other.parent)) {
			return false;
		}
		if (userData == null) {
			if (other.userData != null) {
				return false;
			}
		} else if (!userData.equals(other.userData)) {
			return false;
		}
		return true;
	}

}
