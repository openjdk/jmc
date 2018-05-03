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
package org.openjdk.jmc.joverflow.ui.viewers;

import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.ui.tabletree.TreeItem;

/**
 * A {@code TreeItem} for a {@code JavaThing}
 */
class JavaThingItem implements TreeItem {

	private Iterable<JavaThingItem> children;
	private boolean expanded;
	private final int level;
	private final JavaThing content;
	private final String name;
	private final String value;
	private final String size;

	public JavaThingItem(int level, String name, JavaThing content) {
		this(level, name, content == null ? "null" : content.valueAsString(), content == null ? 0 : content.getSize(), content);
	}

	public JavaThingItem(int level, String name, String value, int size, JavaThing content) {
		this.level = level;
		this.content = content;
		this.name = String.valueOf(name);
		this.value = String.valueOf(value);
		this.size = Integer.toString(size);
	}

	String getName() {
		return name;
	}

	String getValue() {
		return value;
	}

	String getSize() {
		return size;
	}

	public Iterable<JavaThingItem> getChildItems() {
		return children;
	}

	public void setChildItems(Iterable<JavaThingItem> children) {
		this.children = children;
	}

	@Override
	public void setExpended(boolean expanded) {
		this.expanded = expanded;
		if (!expanded && children != null) {
			// Collapse children with parent
			for (TreeItem c : children) {
				c.setExpended(false);
			}
		}
	}

	@Override
	public boolean isExpanded() {
		return expanded;
	}

	@Override
	public int getLevel() {
		return level;
	}

	public JavaThing getContent() {
		return content;
	}
}
