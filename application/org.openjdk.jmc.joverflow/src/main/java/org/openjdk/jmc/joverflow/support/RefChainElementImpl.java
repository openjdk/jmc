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
package org.openjdk.jmc.joverflow.support;

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaField;
import org.openjdk.jmc.joverflow.heap.model.Root;
import org.openjdk.jmc.joverflow.util.FastStack;
import org.openjdk.jmc.joverflow.util.IndexContainer;
import org.openjdk.jmc.joverflow.util.StringInterner;

/**
 * Concrete classes implementing RefChainElement that represent various cases. A ref chain element
 * is usually a "Clazz.field" combination, but for convenience we also provide compound and array
 * nodes. Compound nodes, in turn can represent collections with collapsed implementation details
 * (printed as e.g. "{HashMap}"), or collapsed custom linked lists (printed as e.g.
 * "{MyList.next}"). Array nodes represent aggregated references from arrays (printed as e.g.
 * "Object[]").
 * <p>
 * When we aggregate nodes that have the same class/field etc., we don't distinguish between class
 * versions, i.e. we consider two classes with the same name but different classloaders as a single
 * class. This fixed policy may need to be made flexible at some point
 */
public class RefChainElementImpl {

	private static final int MAX_STATIC_CHILDREN_LIST_SIZE = 100;

	/** Creates or returns an existing element for instance data field */
	public static InstanceFieldOrLinkedList getInstanceFieldElement(
		JavaClass clazz, int fieldIdx, RefChainElement refererElement) {
		ElementWithChildren referer = (ElementWithChildren) refererElement;
		ElementWithChildren[] children = (ElementWithChildren[]) referer.refererOrChildren;
		int clazzIdx = clazz.getClassListIdx();
		String clazzName = clazz.getName();
		for (ElementWithChildren child : children) {
			if (child instanceof InstanceFieldOrLinkedList) {
				InstanceFieldOrLinkedList ifChild = (InstanceFieldOrLinkedList) child;
				// Aggregation by class name, which treats different class versions
				// (classes with same name but different loaders) as one class.
				if (ifChild.isInstanceField() && ifChild.getFieldIdx() == fieldIdx
						&& (ifChild.getJavaClass().getClassListIdx() == clazzIdx
								|| ifChild.getJavaClass().getName().equals(clazzName))) {
					return ifChild;
				}
			}
		}

		InstanceFieldOrLinkedList result = new InstanceFieldOrLinkedList(clazz, fieldIdx, true);
		referer.addChild(result);
		return result;
	}

	/** Creates or returns an existing element for static data field */
	public static StaticField getStaticFieldElement(
		JavaClass clazz, int staticFieldIdx, RefChainElement refererElement) {
		ElementWithChildren referer = (ElementWithChildren) refererElement;
		ElementWithChildren[] children = (ElementWithChildren[]) referer.refererOrChildren;
		int clazzIdx = clazz.getClassListIdx();
		String clazzName = clazz.getName();

		// In applications with extremely large number of classes, data structures like
		// a Vector of classes, where each class references some problematic object via
		// a static field, we may end up with very long arrays of children. For example,
		// we may have a {Vector} with a few thousand children like SomeClazz:someStaticField.
		// Sequential lookup in such arrays can bring JOverflow to a crawl. Thus after a
		// certain threshold we switch to a hashtable representation of children. Since we
		// don't carry hashtable size, we have to take special measures during lookup and
		// element adding.
		int nChildren = children.length;
		if (nChildren <= MAX_STATIC_CHILDREN_LIST_SIZE) {
			// Unusual loop below is a performance optimization implemented after profiling.
			// In general, an element that we need is likely to have been added more recently,
			// so backward search is likely to hit it faster.
			for (int i = children.length - 1; i >= 0; i--) {
				ElementWithChildren child = children[i];
				if (child instanceof StaticField) {
					StaticField sfChild = (StaticField) child;
					// Aggregation by class name, no version distinguishing yet.
					// Looks like order of sub-statements below is important performance-wise:
					// cheaper checks should be done first.
					if (sfChild.getFieldIdx() == staticFieldIdx && (sfChild.getJavaClass().getClassListIdx() == clazzIdx
							|| sfChild.getJavaClass().getName().equals(clazzName))) {
						return sfChild;
					}
				}
			}
		} else { // Perform hash lookup
			// hash below should match AbstractElement. and AbstractField.shallowHashCode()!
			int hash = (clazz.getName().hashCode() << 4) + staticFieldIdx;
			int pos = (hash & 0x7FFFFFFF) % nChildren;
			int nIters = 0;
			while (children[pos] != null && nIters++ < nChildren) {
				ElementWithChildren child = children[pos];
				if (child instanceof StaticField) {
					StaticField sfChild = (StaticField) child;
					if (sfChild.getFieldIdx() == staticFieldIdx && (sfChild.getJavaClass().getClassListIdx() == clazzIdx
							|| sfChild.getJavaClass().getName().equals(clazzName))) {
						return sfChild;
					}
				}
				pos = (pos + 1) % nChildren;
			}
		}

		StaticField result = new StaticField(clazz, staticFieldIdx);

		if (nChildren <= MAX_STATIC_CHILDREN_LIST_SIZE) {
			referer.addChild(result);
			if (((ElementWithChildren[]) referer.refererOrChildren).length > 100) {
				referer.convertChildrenToHashFormat();
			}
		} else {
			referer.addChildToHashChildren(result);
		}

		return result;
	}

	/** Creates or returns an existing element for a compound collection */
	public static Collection getCompoundCollectionElement(JavaClass clazz, RefChainElement refererElement) {
		ElementWithChildren referer = (ElementWithChildren) refererElement;
		ElementWithChildren[] children = (ElementWithChildren[]) referer.refererOrChildren;
		int clazzIdx = clazz.getClassListIdx();
		String clazzName = clazz.getName();
		for (ElementWithChildren child : children) {
			if (child instanceof Collection &&
			// Aggregation by class name, no version distinguishing yet
					(child.getJavaClass().getClassListIdx() == clazzIdx
							|| child.getJavaClass().getName().equals(clazzName))) {
				return (Collection) child;
			}
		}
		Collection result = new Collection(clazz);
		referer.addChild(result);
		return result;
	}

	/** Creates or returns an existing element for compound linked list */
	public static InstanceFieldOrLinkedList getCompoundLinkedListElement(
		JavaClass clazz, int fieldIdx, RefChainElement refererElement) {
		ElementWithChildren referer = (ElementWithChildren) refererElement;
		ElementWithChildren[] children = (ElementWithChildren[]) referer.refererOrChildren;
		int clazzIdx = clazz.getClassListIdx();
		for (ElementWithChildren child : children) {
			if (child instanceof InstanceFieldOrLinkedList) {
				InstanceFieldOrLinkedList llChild = (InstanceFieldOrLinkedList) child;
				// Aggregation by class name, no version distinguishing yet
				if (!llChild.isInstanceField()
						&& (llChild.getJavaClass().getClassListIdx() == clazzIdx
								|| llChild.getJavaClass().getName().equals(clazz.getName()))
						&& llChild.getFieldIdx() == fieldIdx) {
					return llChild;
				}
			}
		}

		InstanceFieldOrLinkedList result = new InstanceFieldOrLinkedList(clazz, fieldIdx, false);
		referer.addChild(result);
		return result;
	}

	/** Creates or returns an existing element for a compound array */
	public static Array getCompoundArrayElement(JavaClass clazz, RefChainElement refererElement) {
		ElementWithChildren referer = (ElementWithChildren) refererElement;
		ElementWithChildren[] children = (ElementWithChildren[]) referer.refererOrChildren;
		for (ElementWithChildren child : children) {
			// Aggregation by class name, no version distinguishing yet
			if (child instanceof Array && child.getJavaClass().getName().equals(clazz.getName())) {
				return (Array) child;
			}
		}

		Array result = new Array(clazz);
		referer.addChild(result);
		return result;
	}

	// The following methods are currently used only from org.openjdk.jmc.joverflow.batch.ExtendedField
	// to create RefChainElements that are already in final form.

	/** Creates an element for instance data field in final form */
	public static InstanceFieldOrLinkedList createInstanceFieldOrLinkedListElementInFinalForm(
		JavaClass clazz, int fieldIdx, RefChainElement refererElement, boolean isInstanceField) {
		InstanceFieldOrLinkedList result = new InstanceFieldOrLinkedList(clazz, fieldIdx, isInstanceField);
		result.setReferer(refererElement);
		return result;
	}

	/** Creates an element for static data field in final form */
	public static StaticField createStaticFieldElementInFinalForm(
		JavaClass clazz, int staticFieldIdx, RefChainElement refererElement) {
		StaticField result = new StaticField(clazz, staticFieldIdx);
		result.setReferer(refererElement);
		return result;
	}

	/** Creates an element for a compound collection in final form */
	public static Collection createCompoundCollectionElementInFinalForm(
		JavaClass clazz, RefChainElement refererElement) {
		Collection result = new Collection(clazz);
		result.setReferer(refererElement);
		return result;
	}

	/** Creates an element for a compound array in final form */
	public static Array createCompoundArrayElementInFinalForm(JavaClass clazz, RefChainElement refererElement) {
		Array result = new Array(clazz);
		result.setReferer(refererElement);
		return result;
	}

	/**
	 * Common abstract superclass of elements that contain Clazz and field. See concrete subclasses
	 * {@link org.openjdk.jmc.joverflow.support.RefChainElementImpl.InstanceFieldOrLinkedList} and
	 * {@link org.openjdk.jmc.joverflow.support.RefChainElementImpl.StaticField}.
	 */
	public static abstract class AbstractField extends AbstractElement {
		protected final char fieldIdx; // char instead of int to save memory

		/**
		 * Returns the index of the field within the instance fields or static fields of the
		 * corresponding JavaClass.
		 */
		public int getFieldIdx() {
			return fieldIdx;
		}

		protected AbstractField(JavaClass clazz, int fieldIdx) {
			super(clazz);
			this.fieldIdx = (char) fieldIdx;
		}

		@Override
		public boolean shallowEquals(Object otherObj) {
			if (!super.shallowEquals(otherObj)) {
				return false;
			}
			return this.fieldIdx == ((AbstractField) otherObj).fieldIdx;
		}

		@Override
		public int shallowHashCode() {
			return (super.shallowHashCode() << 4) + fieldIdx;
		}
	}

	/**
	 * Denotes either an instance field element of reference chain or a custom linked list (a
	 * repeating chain of class-field elements in aggregated form), e.g. Foo.bar, where bar is an
	 * instance (non-static) field defined in Foo, but possibly declared in some superclass of Foo.
	 */
	public static class InstanceFieldOrLinkedList extends AbstractField {

		private boolean isInstanceField;

		private InstanceFieldOrLinkedList(JavaClass clazz, int fieldIdx, boolean isInstanceField) {
			super(clazz, fieldIdx);
			this.isInstanceField = isInstanceField;
		}

		public boolean isInstanceField() {
			return isInstanceField;
		}

		/** Returns the name of this field, e.g. "bar" for Foo.bar */
		public String getFieldName() {
			return getJavaClass().getFieldForInstance(fieldIdx).getName();
		}

		/**
		 * Returns the class that declares this field, which may be either the same as returned by
		 * getJavaClass(), or one of its superclasses.
		 */
		public JavaClass getFieldDeclaringClass() {
			return getJavaClass().getDeclaringClassForField(fieldIdx);
		}

		public void switchToLinkedList() {
			isInstanceField = false;
			cachedToStringValue = null; // For debugging
		}

		@Override
		public String toString() {
			if (cachedToStringValue == null) {
				String clsName = getJavaClass().getHumanFriendlyName();
				if (isInstanceField) {
					cachedToStringValue = StringInterner.internString(clsName + '.' + getFieldName());
				} else {
					cachedToStringValue = StringInterner.internString(
							'{' + clsName + '.' + getJavaClass().getFieldForInstance(fieldIdx).getName() + '}');
				}
			}
			return cachedToStringValue;
		}

		@Override
		public boolean shallowEquals(Object otherObj) {
			if (!super.shallowEquals(otherObj)) {
				return false;
			}
			return this.isInstanceField == ((InstanceFieldOrLinkedList) otherObj).isInstanceField;
		}

		@Override
		public int shallowHashCode() {
			return (super.shallowHashCode() << 1) + (isInstanceField ? 1 : 0);
		}
	}

	/**
	 * Static field element of reference chain, e.g. Foo.baz, where baz is a static field declared
	 * in Foo.
	 */
	public static class StaticField extends AbstractField {

		private StaticField(JavaClass clazz, int fieldIdx) {
			super(clazz, fieldIdx);
		}

		@Override
		public String toString() {
			if (cachedToStringValue == null) {
				String clsName = getJavaClass().getHumanFriendlyName();
				JavaField[] statics = getJavaClass().getStaticFields();
				cachedToStringValue = StringInterner.internString(clsName + ':' + statics[fieldIdx].getName());
			}
			return cachedToStringValue;
		}
	}

	/** Compound element of reference chain representing a known collection */
	public static class Collection extends AbstractElement {

		private Collection(JavaClass clazz) {
			super(clazz);
		}

		@Override
		public String toString() {
			if (cachedToStringValue == null) {
				String clsName = getJavaClass().getHumanFriendlyName();
				cachedToStringValue = StringInterner.internString('{' + clsName + '}');
			}
			return cachedToStringValue;
		}
	}

	/** Compound element of reference chain representing an array */
	public static class Array extends AbstractElement {

		private Array(JavaClass clazz) {
			super(clazz);
		}

		@Override
		public String toString() {
			if (cachedToStringValue == null) {
				cachedToStringValue = StringInterner.internString(getJavaClass().getHumanFriendlyName());
			}
			return cachedToStringValue;
		}
	}

	/**
	 * Intermediate abstract class supporting references to children, which are needed to build
	 * properly aggregated reference chains during data collection. When the full graph is built,
	 * switchToFinalFormat() should be called for all root elements, to convert all the elements
	 * into the final, more economical form, where we only store the reference to the parent element
	 * (referer).
	 */
	private static abstract class ElementWithChildren implements RefChainElement {

		protected static final int INIT_CHILDREN_SIZE = 2;

		/**
		 * During data collection, stores an ElementWithChildren[] array. After it's done and
		 * switchToFinalFormat() is called for all root elements, points at the parent
		 * ElementWithChildren instance.
		 */
		protected Object refererOrChildren = new ElementWithChildren[INIT_CHILDREN_SIZE];

		void addChild(ElementWithChildren child) {
			ElementWithChildren[] children = (ElementWithChildren[]) refererOrChildren;
			int curArraySize = children.length;
			if (children[curArraySize - 1] != null) {
				ElementWithChildren[] oldChildren = children;
				children = new ElementWithChildren[curArraySize + 4];
				System.arraycopy(oldChildren, 0, children, 0, curArraySize);
				children[curArraySize] = child;
				refererOrChildren = children;
			} else {
				int idx = curArraySize - 1;
				while (idx >= 0 && children[idx] == null) {
					idx--;
				}
				children[idx + 1] = child;
			}
		}

		void setReferer(RefChainElement referer) {
			this.refererOrChildren = referer;
		}

		/**
		 * Converts the internal representation from the intermediate "element-children" format into
		 * the final "element-parent" format. We use a loop and internal stack to avoid the (easier
		 * to write and understand) recursion, which can lead to StackOverflowError.
		 */
		static void switchSubtreeToFinalFormat(ElementWithChildren rootElement) {
			FastStack<Object> stack = new FastStack<>(80);
			stack.push(rootElement);
			stack.push(rootElement.refererOrChildren);
			stack.push(new IndexContainer());

			while (!stack.isEmpty()) {
				ElementWithChildren parent = (ElementWithChildren) stack.peek(2);
				ElementWithChildren[] children = (ElementWithChildren[]) stack.peek(1);
				IndexContainer index = (IndexContainer) stack.peek();
				int nextIdx = index.incrementAndGet();
				while (nextIdx < children.length && children[nextIdx] == null) {
					nextIdx = index.incrementAndGet();
				}
				if (nextIdx < children.length) {
					ElementWithChildren child = children[nextIdx];
					ElementWithChildren[] childsChildren = (ElementWithChildren[]) child.refererOrChildren;
					if (childsChildren[0] != null || childsChildren.length > INIT_CHILDREN_SIZE) {
						// Optimization to avoid pushing arrays that are guaranteed to be empty
						stack.push(child);
						stack.push(child.refererOrChildren);
						stack.push(new IndexContainer());
					}
					child.refererOrChildren = parent;
				} else {
					stack.pop(3);
				}
			}
		}

		void convertChildrenToHashFormat() {
			ElementWithChildren[] oldChildren = (ElementWithChildren[]) refererOrChildren;
			int capacity = (oldChildren.length * 2) | 1;
			createTable(oldChildren, capacity);
		}

		void addChildToHashChildren(ElementWithChildren child) {
			ElementWithChildren[] children = (ElementWithChildren[]) refererOrChildren;
			int capacity = children.length;
			int pos = (child.shallowHashCode() & 0x7FFFFFFF) % capacity;
			// We don't store table size, so use "bad table behavior" as a signal that
			// it needs rehashing
			int seqLen = 0;
			while (children[pos] != null) {
				seqLen++;
				if (seqLen > capacity / 8) {
					createTable(children, (capacity * 3 / 2) | 1);
					addChildToHashChildren(child);
					return;
				}
				pos = (pos + 1) % capacity;
			}
			children[pos] = child;
		}

		void createTable(ElementWithChildren[] oldChildren, int capacity) {
			ElementWithChildren[] children = new ElementWithChildren[capacity];
			for (ElementWithChildren child : oldChildren) {
				if (child == null) {
					continue;
				}
				int pos = (child.shallowHashCode() & 0x7FFFFFFF) % capacity;
				while (children[pos] != null) {
					pos = (pos + 1) % capacity;
				}
				children[pos] = child;
			}
			refererOrChildren = children;
		}

		@Override
		public abstract boolean equals(Object other);

		@Override
		public abstract int hashCode();
	}

	private static abstract class AbstractElement extends ElementWithChildren {

		private final JavaClass clazz;
		protected String cachedToStringValue;

		@Override
		public JavaClass getJavaClass() {
			return clazz;
		}

		@Override
		public RefChainElement getReferer() {
			return (RefChainElement) refererOrChildren;
		}

		private AbstractElement(JavaClass clazz) {
			this.clazz = clazz;
		}

		@Override
		public boolean equals(Object other) {
			if (!shallowEquals(other)) {
				return false;
			}
			return referersEqual((AbstractElement) other);
		}

		@Override
		public int hashCode() {
			return (referersHashCode() << 4) + shallowHashCode();
		}

		/** Should be overridden in subclass that defines additional data fields */
		@Override
		public boolean shallowEquals(Object otherObj) {
			if (otherObj == this) {
				return true;
			}
			if (otherObj == null) {
				return false;
			}
			if (this.getClass() != otherObj.getClass()) {
				return false;
			}
			return (clazz.getClassListIdx() == ((AbstractElement) otherObj).clazz.getClassListIdx()
					|| clazz.getName().equals(((AbstractElement) otherObj).clazz.getName()));
		}

		private boolean referersEqual(AbstractElement other) {
			RefChainElement thisReferer = this.getReferer();
			RefChainElement otherReferer = other.getReferer();
			if (thisReferer == otherReferer) {
				// Probably won't happen, but just in case
				return true;
			}
			if (thisReferer == null || otherReferer == null) {
				return false;
			}
			return thisReferer.equals(otherReferer);
		}

		/** Should be overridden in subclass that defines additional data fields */
		@Override
		public int shallowHashCode() {
			return clazz.getName().hashCode();
		}

		private int referersHashCode() {
			RefChainElement referer = getReferer();
			if (referer == null) {
				return 0;
			}
			return referer.hashCode();
		}
	}

	public static class GCRoot extends ElementWithChildren {

		private final Root root;

		@Override
		public JavaClass getJavaClass() {
			return null;
		}

		@Override
		public RefChainElement getReferer() {
			return null;
		}

		public Root getRoot() {
			return root;
		}

		public GCRoot(Root root) {
			this.root = root;
		}

		/**
		 * This method should be called for each GCRoot instance after heap scanning is over, i.e.
		 * the ref chain graph is complete, but before making any calls to
		 * RefChainElement.getReferer(). It walks the ref chains hanging off this root all the way
		 * down, and converts the internal format of ref chain elements from "element-children" into
		 * the final "element-parent" format.
		 */
		public void switchTreeToFinalFormat() {
//			switchToFinalFormat(null);
			switchSubtreeToFinalFormat(this);
		}

		@Override
		public String toString() {
			return StringInterner.internString(root.getIdString());
		}

		@Override
		public boolean equals(Object otherObj) {
			if (otherObj == this) {
				return true;
			}
			if (!(otherObj instanceof GCRoot)) {
				return false;
			}
			GCRoot other = (GCRoot) otherObj;
			return this.root == other.root;
		}

		@Override
		public int hashCode() {
			return root.hashCode();
		}

		@Override
		public boolean shallowEquals(Object other) {
			return equals(other);
		}

		@Override
		public int shallowHashCode() {
			return hashCode();
		}
	}
}
