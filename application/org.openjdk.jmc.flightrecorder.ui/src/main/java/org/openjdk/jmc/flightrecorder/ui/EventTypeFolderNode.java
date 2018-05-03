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
package org.openjdk.jmc.flightrecorder.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.common.tree.IParent;

public class EventTypeFolderNode implements IParent<Object> {

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EventTypeFolderNode) {
			return Objects.equals(this.name, ((EventTypeFolderNode) obj).name);
		}
		return false;
	}

	static class TypeWithCategory {
		private final String[] category;
		private final IType<IItem> type;
		private final long count;

		TypeWithCategory(IType<IItem> type, String[] category, long count) {
			this.category = category;
			this.type = type;
			this.count = count;
		}

		IType<IItem> getType() {
			return type;
		}

		String[] getCategory() {
			return category;
		}

		long getCount() {
			return count;
		}
	}

	public static class EventTypeNode {
		private final IType<IItem> type;
		private final long count;

		EventTypeNode(IType<IItem> type, long count) {
			this.type = type;
			this.count = count;
		}

		@Override
		public String toString() {
			return "EventTypeNode: " + type.getName() + "(" + type.getIdentifier() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		public IType<IItem> getType() {
			return type;
		}

		public IQuantity getCount() {
			return UnitLookup.NUMBER_UNITY.quantity(count);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof EventTypeNode) {
				return Objects.equals(this.type, ((EventTypeNode) obj).type);
			}
			return false;
		}
	}

	private final String name;
	private final List<Object> children = new ArrayList<>();

	private EventTypeFolderNode(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return name;
	}

	@Override
	public boolean hasChildren() {
		return !children.isEmpty();
	}

	@Override
	public List<?> getChildren() {
		return children;
	}

	@Override
	public String toString() {
		return "EventTypeFolderNode: " + getName(); //$NON-NLS-1$
	}

	public IQuantity getCount() {
		return UnitLookup.NUMBER_UNITY.quantity(performCount());
	}

	private long performCount() {
		long childCount = 0;
		for (Object c : children) {
			if (c instanceof EventTypeFolderNode) {
				childCount += ((EventTypeFolderNode) c).performCount();
			} else if (c instanceof EventTypeNode) {
				childCount += ((EventTypeNode) c).count;
			}
		}
		return childCount;
	}

	private void sortChildren() {
		for (Object c : getChildren()) {
			if (c instanceof EventTypeFolderNode) {
				((EventTypeFolderNode) c).sortChildren();
			}
		}
		Collections.sort(getChildren(), new Comparator<Object>() {

			@Override
			public int compare(Object o1, Object o2) {
				if (o1 instanceof EventTypeFolderNode && o2 instanceof EventTypeFolderNode) {
					return ((EventTypeFolderNode) o1).getName().compareTo(((EventTypeFolderNode) o2).getName());
				} else if (o1 instanceof EventTypeNode && o2 instanceof EventTypeNode) {
					return ((EventTypeNode) o1).type.getName().compareTo(((EventTypeNode) o2).type.getName());
				} else {
					return o1 instanceof EventTypeFolderNode ? -1 : 1;
				}
			}
		});
	}

	private EventTypeFolderNode getOrCreateFolder(String label) {
		for (Object c : children) {
			if (c instanceof EventTypeFolderNode && ((EventTypeFolderNode) c).getName().equals(label)) {
				return (EventTypeFolderNode) c;
			}
		}
		EventTypeFolderNode folder = new EventTypeFolderNode(label);
		children.add(folder);
		return folder;
	}

	static EventTypeFolderNode buildRoot(Stream<TypeWithCategory> allEventTypes) {
		EventTypeFolderNode eventTypesRoot = new EventTypeFolderNode(Messages.EVENT_TYPE_FOLDER_NODE_EVENTS_BY_TYPE);
		allEventTypes.forEach(e -> addEventType(eventTypesRoot, e.getType(), e.getCategory(), e.getCount()));
		eventTypesRoot.sortChildren();
		return eventTypesRoot;
	}

	private static void addEventType(EventTypeFolderNode parentNode, IType<IItem> t, String[] category, long count) {
		if (category != null && category.length > 0) {
			for (String part : category) {
				parentNode = parentNode.getOrCreateFolder(part.trim());
			}
		} else {
			parentNode = parentNode.getOrCreateFolder(Messages.EVENT_TYPE_FOLDER_NODE_UNCATEGORIZED);
		}
		parentNode.children.add(new EventTypeNode(t, count));
	}
}
