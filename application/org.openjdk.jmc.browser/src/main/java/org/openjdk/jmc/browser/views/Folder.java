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
package org.openjdk.jmc.browser.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.ui.common.tree.IParent;
import org.openjdk.jmc.ui.common.util.ICopyable;

public class Folder implements IParent<Object>, ICopyable, IDescribable {

	private final static String[] EMPTY = {};
	private final static Pattern PATH_SEP_PATTERN = Pattern.compile("\\/"); //$NON-NLS-1$

	private Folder parent;
	private String name;
	private final List<Object> leafs = new ArrayList<>();
	private final Map<String, Folder> sub = new HashMap<>(5);

	public Folder(Folder parent, String name) {
		this.name = name;
		this.parent = parent;
		if (parent != null) {
			parent.sub.put(name, this);
		}
	}

	public void addLeaf(Object o) {
		leafs.add(o);
	}

	public void clearLeafs() {
		leafs.clear();
	}

	public Folder getFolder(String path) {
		return getFolder(this, path);
	}

	private Folder getFolder(Folder root, String path) {
		for (String segment : getSegments(path)) {
			Folder sf = root.sub.get(segment);
			if (sf == null) {
				sf = new Folder(root, segment);
			}
			root = sf;
		}
		return root;
	}

	private static String[] getSegments(String path) {
		return path == null ? EMPTY : PATH_SEP_PATTERN.split(path);
	}

	protected void setParent(Folder newParent) {
		if (newParent != parent) {
			if (newParent != null) {
				if (!newParent.isModifiable() || newParent.hasSubFolder(name) || newParent.isDescendentForm(this)) {
					throw new IllegalStateException(this + " can't accept parent " + newParent); //$NON-NLS-1$
				}
				newParent.sub.put(name, this);
			}
			if (parent != null) {
				parent.sub.remove(name);
			}

			parent = newParent;
		}
	}

	public void dispose() {
		performInsert(this, null);
	}

	public void insert(Object o) {
		performInsert(o, this);
	}

	protected void performInsert(Object o, Folder into) {
		// Delegate to parent to allow it to choose behavior
		if (parent != null) {
			parent.performInsert(o, into);
		}
	}

	public void setName(String name) {
		if (!isModifiable()) {
			throw new IllegalStateException(this + " can't be changed"); //$NON-NLS-1$
		}
		parent.sub.remove(this.name);
		this.name = name;
		parent.sub.put(this.name, this);
		performInsert(this, parent);
	}

	public List<Object> getLeafs() {
		return leafs;
	}

	public Folder getParent() {
		return parent;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return null;
	}

	public void accept(Consumer<Folder> visitor) {
		try {
			visitor.accept(this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		for (Folder sf : sub.values()) {
			sf.accept(visitor);
		}
	}

	public boolean isModifiable() {
		return parent == null ? true : parent.isModifiable();
	}

	public boolean hasSubFolder(String subfolderName) {
		return sub.containsKey(subfolderName);
	}

	public boolean isDescendentForm(Folder node) {
		return this == node || parent != null && parent.isDescendentForm(node);
	}

	public String getPath(boolean omitRoot) {
		if (parent == null) {
			return omitRoot ? null : name;
		} else {
			return omitRoot && parent.parent == null ? name : parent.getPath(omitRoot) + "/" + name; //$NON-NLS-1$
		}
	}

	@Override
	public boolean hasChildren() {
		return leafs.size() > 0 || sub.size() > 0;
	}

	@Override
	public Collection<Object> getChildren() {
		List<Object> children = new ArrayList<>();
		children.addAll(leafs);
		children.addAll(sub.values());
		return children;
	}

	@Override
	public String toString() {
		return getPath(false);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Folder && ((Folder) obj).getPath(false).equals(getPath(false));
	}

	@Override
	public int hashCode() {
		return getPath(false).hashCode();
	}

	@Override
	public boolean isCopyable() {
		for (Folder sf : sub.values()) {
			if (!sf.isCopyable()) {
				return false;
			}
		}
		for (Object l : leafs) {
			if (!(l instanceof ICopyable) || !((ICopyable) l).isCopyable()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Folder copy() {
		return fillCopy(new Folder(null, name));
	}

	private Folder fillCopy(Folder parent) {
		for (Folder sf : sub.values()) {
			Folder sfCopy = sf.fillCopy(new Folder(parent, sf.name));
			parent.sub.put(sfCopy.name, sfCopy);
		}
		for (Object l : leafs) {
			if ((l instanceof ICopyable)) {
				parent.leafs.add(((ICopyable) l).copy());
			}
		}
		return parent;
	}

}
