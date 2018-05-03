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
package org.openjdk.jmc.ui.misc;

import java.util.Collection;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeNode;

import org.openjdk.jmc.ui.common.tree.IArray;
import org.openjdk.jmc.ui.common.tree.IChild;
import org.openjdk.jmc.ui.common.tree.IParent;
import org.openjdk.jmc.ui.common.tree.ITreeNode;

public class TreeStructureContentProvider extends MCArrayContentProvider implements ITreeContentProvider {

	public static final TreeStructureContentProvider INSTANCE = new TreeStructureContentProvider();

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof IArray<?>) {
			return !((IArray<?>) element).isEmpty();
		} else if (element instanceof IParent) {
			return ((IParent<?>) element).hasChildren();
		} else if (element.getClass().isArray()) {
			return ((Object[]) element).length > 0;
		} else if (element instanceof Collection<?>) {
			return ((Collection<?>) element).size() > 0;
		} else if (element instanceof ITreeNode) {
			ITreeNode[] children = ((ITreeNode) element).getChildren();
			return children == null ? false : children.length > 0;
		} else if (element instanceof TreeNode) {
			return ((TreeNode) element).hasChildren();
		} else {
			return false;
		}
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof ITreeNode) {
			return ((ITreeNode) element).getParent();
		} else if (element instanceof TreeNode) {
			return ((TreeNode) element).getParent();
		} else if (element instanceof IChild<?>) {
			return ((IChild<?>) element).getParent();
		}
		return null;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IParent) {
			return ((IParent<?>) inputElement).getChildren().toArray();
		} else if (inputElement instanceof ITreeNode) {
			return ((ITreeNode) inputElement).getChildren();
		} else if (inputElement instanceof TreeNode) {
			return ((TreeNode) inputElement).getChildren();
		} else {
			return super.getElements(inputElement);
		}

	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return getElements(parentElement);
	}
}
