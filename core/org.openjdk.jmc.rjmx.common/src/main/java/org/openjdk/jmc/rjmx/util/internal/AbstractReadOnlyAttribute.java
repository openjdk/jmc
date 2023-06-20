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
package org.openjdk.jmc.rjmx.util.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;

import org.openjdk.jmc.rjmx.services.IAttribute;
import org.openjdk.jmc.rjmx.services.IAttributeChild;
import org.openjdk.jmc.rjmx.services.IAttributeInfo;
import org.openjdk.jmc.rjmx.services.IIndexedAttributeChild;
import org.openjdk.jmc.rjmx.services.IKeyedAttributeChild;
import org.openjdk.jmc.rjmx.services.IReadOnlyAttribute;
import org.openjdk.jmc.ui.common.tree.IParent;

public abstract class AbstractReadOnlyAttribute implements IReadOnlyAttribute, IParent<Object> {
	private final IAttributeInfo info;

	public AbstractReadOnlyAttribute(IAttributeInfo info) {
		this.info = info;
	}

	@Override
	public boolean hasChildren() {
		// The old implementation was to return "getChildren().size() > 0". The problem with this implementation is if
		// the number of children is large and/or complex to create.
		Object value = getValue();
		if (value == null) {
			return false;
		} else if (value.getClass().isArray()) {
			return Array.getLength(value) > 0;
		} else if (value instanceof CompositeData) {
			return !((CompositeData) value).getCompositeType().keySet().isEmpty();
		} else if (value instanceof TabularData) {
			return !((TabularData) value).values().isEmpty();
		} else if (value instanceof Collection<?>) {
			return !((Collection<?>) value).isEmpty();
		} else if (value instanceof Map<?, ?>) {
			return !((Map<?, ?>) value).isEmpty();
		} else if (value instanceof IParent<?>) {
			return ((IParent<?>) value).hasChildren();
		}
		return false;
	}

	@Override
	public Collection<?> getChildren() {
		// If this implementation is changed please also update the hasChildren method.
		Object value = getValue();
		if (value == null) {
			return Collections.emptyList();
		} else if (value.getClass().isArray()) {
			return getChildren(Array.getLength(value), value.getClass().getComponentType().getName());
		} else if (value instanceof CompositeData) {
			return getChildren((CompositeData) value);
		} else if (value instanceof TabularData) {
			return getChildren(((TabularData) value).values());
		} else if (value instanceof Collection<?>) {
			return getChildren(((Collection<?>) value));
		} else if (value instanceof Map<?, ?>) {
			return getChildren(((Map<?, ?>) value));
		} else if (value instanceof IParent<?>) {
			return ((IParent<?>) value).getChildren();
		} else {
			return Collections.emptyList();
		}
	}

	private List<?> getChildren(int arrayLength, String arrayComponentType) {
		List<IReadOnlyAttribute> children = new ArrayList<>(arrayLength);
		for (int i = 0; i < arrayLength; i++) {
			children.add(createArrayAttributeChild(arrayComponentType, i));
		}
		return PartitionedList.create(children);

	}

	private ReadOnlyArrayAttributeChild createArrayAttributeChild(String arrayComponentType, int i) {
		// FIXME: A more general handling of editing Object values would solve the problem of using the type of the array element.
		Object value = getValue();
		if (value != null && !value.getClass().getComponentType().isPrimitive()) {
			Object element = Array.get(value, i);
			if (element != null) {
				arrayComponentType = element.getClass().getName();
			}
		}
		if (this instanceof IAttribute) {
			return new ArrayAttributeChild(this, arrayComponentType, i);
		}
		return new ReadOnlyArrayAttributeChild(this, arrayComponentType, i);
	}

	private List<IReadOnlyAttribute> getChildren(CompositeData data) {
		CompositeType type = data.getCompositeType();
		List<IReadOnlyAttribute> children = new ArrayList<>(type.keySet().size());
		for (String childName : type.keySet()) {
			children.add(createCompositeDataChild(childName, type.getType(childName).getClassName()));
		}
		return children;
	}

	private List<IReadOnlyAttribute> getChildren(Map<?, ?> map) {
		List<IReadOnlyAttribute> children = new ArrayList<>(map.keySet().size());
		for (Object childName : map.keySet()) {
			children.add(new MapAttributeChild(this, childName));
		}
		return children;
	}

	private List<?> getChildren(Collection<?> data) {
		if (data instanceof List<?>) {
			return getChildren((List<?>) data);
		} else {
			int i = 0;
			List<IReadOnlyAttribute> children = new ArrayList<>();
			for (Object v : data) {
				children.add(new CollectionAttributeChild(this, v, i++));
			}
			return PartitionedList.create(children);
		}
	}

	private List<?> getChildren(List<?> list) {
		List<IReadOnlyAttribute> children = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			children.add(new ListAttributeChild(this, i));
		}
		return PartitionedList.create(children);
	}

	private IReadOnlyAttribute createCompositeDataChild(String name, String type) {
		return new AbstractReadOnlyAttribute(new SimpleAttributeInfo(name, type)) {
			@Override
			public Object getValue() {
				// Get the value from the parent
				return ((CompositeData) AbstractReadOnlyAttribute.this.getValue()).get(getInfo().getName());
			}
		};

	}

	@Override
	public IAttributeInfo getInfo() {
		return info;
	}

	@Override
	public int hashCode() {
		return getInfo().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof IReadOnlyAttribute)) {
			return false;
		}
		IReadOnlyAttribute other = (IReadOnlyAttribute) obj;
		return other.getInfo().equals(getInfo()) && equalValues(getValue(), other.getValue());
	}

	private boolean equalValues(Object thisValue, Object thatValue) {
		if (thisValue == null) {
			return thatValue == null;
		}
		return thisValue.equals(thatValue);
	}

	private static abstract class AbstractReadOnlyAttributeChild extends AbstractReadOnlyAttribute
			implements IAttributeChild {
		private final IReadOnlyAttribute parent;

		public AbstractReadOnlyAttributeChild(IReadOnlyAttribute parent, IAttributeInfo info) {
			super(info);
			this.parent = parent;
		}

		@Override
		public IReadOnlyAttribute getParent() {
			return parent;
		}
	}

	private static class CollectionAttributeChild extends AbstractReadOnlyAttributeChild
			implements Comparable<CollectionAttributeChild> {

		private final Object value;
		private final int index;

		public CollectionAttributeChild(IReadOnlyAttribute parent, Object value, int index) {
			super(parent, new SimpleAttributeInfo("[" + index + ']', "")); //$NON-NLS-1$ //$NON-NLS-2$
			this.value = value;
			this.index = index;
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public int compareTo(CollectionAttributeChild o) {
			return index - o.index;
		}

	}

	private static class MapAttributeChild extends AbstractReadOnlyAttributeChild implements IKeyedAttributeChild {

		private final Object key;

		public MapAttributeChild(IReadOnlyAttribute parent, Object key) {
			super(parent, new SimpleAttributeInfo(key.toString(), "")); //$NON-NLS-1$
			this.key = key;
		}

		@Override
		public Object getValue() {
			return ((Map<?, ?>) getParent().getValue()).get(key);
		}

		@Override
		public String getKey() {
			return getInfo().getName();
		}
	}

	private static class ListAttributeChild extends AbstractReadOnlyAttributeChild
			implements Comparable<ListAttributeChild>, IIndexedAttributeChild {

		private final int index;

		ListAttributeChild(IReadOnlyAttribute parent, int index) {
			super(parent, new SimpleAttributeInfo("[" + index + ']', "")); //$NON-NLS-1$ //$NON-NLS-2$
			this.index = index;
		}

		@Override
		public Object getValue() {
			return ((List<?>) getParent().getValue()).get(index);
		}

		@Override
		public int compareTo(ListAttributeChild o) {
			return index - o.index;
		}

		@Override
		public int getIndex() {
			return index;
		}
	}

	private static class ReadOnlyArrayAttributeChild extends AbstractReadOnlyAttributeChild
			implements Comparable<ReadOnlyArrayAttributeChild>, IIndexedAttributeChild {

		private final int index;

		ReadOnlyArrayAttributeChild(IReadOnlyAttribute parent, String typeName, int index) {
			super(parent, new SimpleAttributeInfo("[" + index + ']', typeName)); //$NON-NLS-1$
			this.index = index;
		}

		@Override
		public Object getValue() {
			return Array.get(getParent().getValue(), index);
		}

		@Override
		public int compareTo(ReadOnlyArrayAttributeChild o) {
			return index - o.index;
		}

		@Override
		public int getIndex() {
			return index;
		}
	}

	private static class ArrayAttributeChild extends ReadOnlyArrayAttributeChild implements IAttribute {

		// Since this object with given index might have been removed from the parent we must keep a record of the
		// current value. Otherwise we will crash with AIOOB during hash table lookup of entries in MBean browser tree.
		private Object cachedValue;

		ArrayAttributeChild(IReadOnlyAttribute parent, String typeName, int index) {
			super(parent, typeName, index);
			cachedValue = lookupValue();
		}

		private Object lookupValue() {
			return Array.get(getParent().getValue(), getIndex());
		}

		@Override
		public void setValue(Object value) {
			cachedValue = value;
			Object array = getParent().getValue();
			Array.set(array, getIndex(), value);
			((IAttribute) getParent()).setValue(array);
		}

		@Override
		public Object getValue() {
			return cachedValue;
		}
	}
}
