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
package org.openjdk.jmc.rjmx.ui.internal;

import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmc.ui.common.tree.DefaultTreeNode;
import org.openjdk.jmc.ui.common.tree.ITreeNode;

public class TreeNodeBuilder {
	private Object value;
	private final Map<Object, TreeNodeBuilder> children = new HashMap<>(2);

	public TreeNodeBuilder get(Object childKey) {
		TreeNodeBuilder c = children.get(childKey);
		if (c == null) {
			c = new TreeNodeBuilder();
			children.put(childKey, c);
		}
		return c;
	}

	public TreeNodeBuilder get(Object childKey, Object childValue) {
		TreeNodeBuilder c = get(childKey);
		c.setValue(childValue);
		return c;
	}

	public TreeNodeBuilder getUniqueChild(Object child) {
		return get(child, child);
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

	public ITreeNode[] getChildren(ITreeNode parent) {
		ITreeNode[] nodes = new ITreeNode[children.size()];
		int i = 0;
		for (TreeNodeBuilder builder : children.values()) {
			DefaultTreeNode tn = new DefaultTreeNode(parent, builder.value);
			ITreeNode[] subNodes = builder.getChildren(tn);
			if (subNodes.length > 0) {
				tn.setChildren(subNodes);
			}
			nodes[i++] = tn;
		}
		return nodes;
	}
}
