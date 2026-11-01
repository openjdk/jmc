/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.joverflow.ui.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.joverflow.dominator.DominatorTree;
import org.openjdk.jmc.joverflow.heap.model.HeapObjectReader;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.ui.Messages;
import org.openjdk.jmc.joverflow.util.MiscUtils;

/**
 * Lazily creates labels and children, showing the {@link #MAX_CHILDREN} largest children by
 * retained size plus a placeholder for the rest. Labels use an independent reader and bounded
 * String previews.
 */
public class DominatorTreeItem {
	public static final int MAX_CHILDREN = 1000;
	private static final int MAX_STRING_PREVIEW_LENGTH = 256;
	private static final int MAX_LABEL_LENGTH = 1024;

	private final DominatorTree tree;
	private final HeapObjectReader reader;
	private final int node;
	private final int hiddenChildren;
	private String label;
	private List<DominatorTreeItem> children;

	public DominatorTreeItem(DominatorTree tree, int node, HeapObjectReader reader) {
		this(tree, node, reader, 0);
	}

	private DominatorTreeItem(DominatorTree tree, int node, HeapObjectReader reader, int hiddenChildren) {
		this.tree = tree;
		this.reader = reader;
		this.node = node;
		this.hiddenChildren = hiddenChildren;
	}

	public DominatorTree getTree() {
		return tree;
	}

	/**
	 * Returns the node id, or {@link DominatorTree#NO_NODE} for a placeholder item.
	 */
	public int getNode() {
		return node;
	}

	public boolean isPlaceholder() {
		return node == DominatorTree.NO_NODE;
	}

	public String getLabel() {
		if (label == null) {
			label = computeLabel();
			if (label.length() > MAX_LABEL_LENGTH) {
				label = label.substring(0, MAX_LABEL_LENGTH - 1) + '\u2026';
			}
		}
		return label;
	}

	/**
	 * Returns the shallow size, or -1 for a placeholder item.
	 */
	public long getShallowSize() {
		return isPlaceholder() ? -1 : tree.getShallowSize(node);
	}

	/**
	 * Returns the retained size, or -1 for a placeholder item.
	 */
	public long getRetainedSize() {
		return isPlaceholder() ? -1 : tree.getRetainedSize(node);
	}

	/**
	 * Retained size as a percentage of total heap size.
	 */
	public double getRetainedPercentage() {
		long total = tree.getTotalSize();
		return isPlaceholder() || total == 0 ? 0 : 100.0 * tree.getRetainedSize(node) / total;
	}

	public boolean hasChildren() {
		return !isPlaceholder() && tree.getChildCount(node) > 0;
	}

	public List<DominatorTreeItem> getChildren() {
		if (children == null) {
			children = computeChildren();
		}
		return children;
	}

	private List<DominatorTreeItem> computeChildren() {
		if (isPlaceholder()) {
			return Collections.emptyList();
		}
		int count = tree.getChildCount(node);
		int shown = Math.min(count, MAX_CHILDREN);
		List<DominatorTreeItem> result = new ArrayList<>(shown + 1);
		for (int i = 0; i < shown; i++) {
			result.add(new DominatorTreeItem(tree, tree.getChild(node, i), reader));
		}
		if (count > shown) {
			result.add(new DominatorTreeItem(tree, DominatorTree.NO_NODE, reader, count - shown));
		}
		return result;
	}

	private String computeLabel() {
		if (isPlaceholder()) {
			return NLS.bind(Messages.DominatorTreeItem_MORE_OBJECTS, String.format("%,d", hiddenChildren), //$NON-NLS-1$
					String.format("%,d", MAX_CHILDREN)); //$NON-NLS-1$
		}
		if (node == tree.getUnreachableObjectsNode()) {
			return Messages.DominatorTreeItem_UNREACHABLE_OBJECTS;
		}
		JavaHeapObject obj = tree.getObject(node);
		try {
			return describe(obj);
		} catch (IOException | RuntimeException e) {
			// Corrupt dumps may prevent reading object contents.
			return obj instanceof JavaClass ? "class " + ((JavaClass) obj).getName() //$NON-NLS-1$
					: obj.getClazz().getHumanFriendlyName();
		}
	}

	private String describe(JavaHeapObject obj) throws IOException {
		if (obj instanceof JavaClass) {
			JavaClass clazz = (JavaClass) obj;
			String name = "class " + clazz.getName(); //$NON-NLS-1$
			if (clazz.hasMultipleVersions()) {
				JavaThing loader = clazz.getLoader();
				name += " loader " + (loader instanceof JavaHeapObject //$NON-NLS-1$
						? MiscUtils.toHex(reader.readId((JavaHeapObject) loader)) : "null"); //$NON-NLS-1$
			}
			return name;
		}
		JavaClass clazz = obj.getClazz();
		String name = clazz.getHumanFriendlyName();
		if (obj instanceof JavaObjectArray) {
			name = insertLength(name, ((JavaObjectArray) obj).getLength());
		} else if (obj instanceof JavaValueArray) {
			name = insertLength(name, ((JavaValueArray) obj).getLength());
		}
		StringBuilder sb = new StringBuilder(name).append('@').append(MiscUtils.toHex(reader.readId(obj)));
		if (clazz.isString() && obj instanceof JavaObject) {
			String value = reader.readString((JavaObject) obj, MAX_STRING_PREVIEW_LENGTH);
			sb.append(' ').append(value == null ? "null" : MiscUtils.removeEndLinesAndAddQuotes(value, 0)); //$NON-NLS-1$
		}
		return sb.toString();
	}

	/**
	 * Turns e.g. "byte[]" into "byte[1024]"
	 */
	private static String insertLength(String arrayClassName, int length) {
		int idx = arrayClassName.indexOf('[');
		if (idx < 0) {
			return arrayClassName;
		}
		return arrayClassName.substring(0, idx + 1) + length + arrayClassName.substring(idx + 1);
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
